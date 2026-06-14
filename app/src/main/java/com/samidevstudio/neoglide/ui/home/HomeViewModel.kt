package com.samidevstudio.neoglide.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.data.repository.HomeRepository
import com.samidevstudio.neoglide.data.repository.UserPreferencesRepository
import com.samidevstudio.neoglide.data.repository.WidgetRepository
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.service.NeoGlideNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeItem {
    abstract val id: Int
    abstract val row: Float
    abstract val column: Float
    abstract val spanX: Float
    abstract val spanY: Float
    
    val uniqueKey: String get() = when(this) {
        is App -> "APP_$id"
        is Folder -> "FOLDER_$id"
        is Widget -> "WIDGET_$id"
    }

    data class App(
        override val id: Int,
        val appModel: AppModel,
        override val row: Float,
        override val column: Float,
        override val spanX: Float = 1f,
        override val spanY: Float = 1f,
    ) : HomeItem()

    data class Widget(
        override val id: Int, // uses widgetId
        val widgetEntity: WidgetEntity,
        override val row: Float,
        override val column: Float,
        override val spanX: Float,
        override val spanY: Float,
        val isCustom: Boolean = false, // New flag for internal widgets like Dock
    ) : HomeItem()

    data class Folder(
        override val id: Int,
        val label: String,
        val apps: List<AppModel>,
        override val row: Float,
        override val column: Float,
        override val spanX: Float = 1f,
        override val spanY: Float = 1f
    ) : HomeItem()
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val widgetRepository: WidgetRepository,
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val appWidgetManager: AppWidgetManager,
    val appWidgetHost: AppWidgetHost
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    fun triggerIconRefresh() {
        _refreshTrigger.value += 1
    }

    private val _shouldShowDefaultPrompt = MutableStateFlow(value = false)
    val shouldShowDefaultPrompt = _shouldShowDefaultPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe preferences to handle first run provisioning (and resets)
            userPreferencesRepository.userPreferencesFlow.collect { prefs ->
                if (prefs.isFirstInstallRun) {
                    provisionDefaultDock()
                }
            }
        }
        viewModelScope.launch {
            // Refresh apps on startup
            appRepository.refreshApps()
        }
    }

    private suspend fun provisionDefaultDock() {
        // Double check DB to prevent accidental duplicates
        val existing = widgetRepository.allWidgets.first()
        if (existing.none { (it.providerPackage == "internal") && (it.providerClass == "dock") }) {
            // Provision internal Dock (Floating placeholder: 99.5f)
            val dockId = widgetRepository.allocateWidgetId()
            widgetRepository.addWidget(
                WidgetEntity(
                    widgetId = dockId,
                    providerPackage = "internal",
                    providerClass = "dock",
                    label = "Dock",
                    row = 99.5f,
                    column = 0f,
                    spanX = 5f,
                    spanY = 1f
                )
            )
        }
        // Mark as done so we don't repeat this until next reset
        userPreferencesRepository.setFirstInstallRun(isFirst = false)
    }

    val activeNotifications: StateFlow<Map<String, Int>> = NeoGlideNotificationListener.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val homeItems: StateFlow<List<HomeItem>> = combine(
        appRepository.allApps,
        homeRepository.allHomeApps,
        homeRepository.allWidgets,
        homeRepository.allFolders
    ) { apps, homeApps, widgets, folders ->
        // Avoid emitting items if apps list is empty but we expect to have home items
        // This prevents folders appearing empty for a split second on rerun
        if (apps.isEmpty() && (homeApps.isNotEmpty() || folders.isNotEmpty())) {
            return@combine emptyList()
        }

        val appItems = homeApps.mapNotNull { homeApp ->
            apps.find { it.packageName == homeApp.packageName }?.let { appModel ->
                HomeItem.App(
                    id = homeApp.id,
                    appModel = appModel,
                    row = homeApp.row,
                    column = homeApp.column,
                    spanX = homeApp.spanX,
                    spanY = homeApp.spanY
                )
            }
        }
        val widgetItems = widgets.map { widget ->
            HomeItem.Widget(
                id = widget.widgetId,
                widgetEntity = widget,
                row = widget.row,
                column = widget.column,
                spanX = widget.spanX,
                spanY = widget.spanY,
                isCustom = widget.providerPackage == "internal"
            )
        }
        
        val folderItems = folders.map { folderWithApps ->
            HomeItem.Folder(
                id = folderWithApps.folder.id,
                label = folderWithApps.folder.label,
                apps = folderWithApps.apps.mapNotNull { folderApp ->
                    apps.find { it.packageName == folderApp.packageName }
                },
                row = folderWithApps.folder.row,
                column = folderWithApps.folder.column
            )
        }
        
        appItems + widgetItems + folderItems
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dockApps: StateFlow<List<AppModel>> = combine(
        appRepository.allApps,
        appRepository.allApps.map { appRepository.getDefaultDockApps() }
    ) { apps, defaultDockPkgs ->
        apps.asSequence()
            .filter { it.packageName in defaultDockPkgs }
            .sortedBy { defaultDockPkgs.indexOf(it.packageName) }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val widgets: StateFlow<List<WidgetEntity>> = widgetRepository.allWidgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApps: StateFlow<List<AppModel>> = appRepository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyUsedApps: StateFlow<List<AppModel>> = appRepository.allApps
        .map { apps ->
            apps.asSequence()
                .filter { it.lastUsedTime > 0 }
                .sortedByDescending { it.lastUsedTime }
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableAppsForPicker: StateFlow<List<AppModel>> = combine(
        allApps,
        homeItems
    ) { apps, items ->
        val homePackages = mutableSetOf<String>()
        items.forEach { item ->
            when (item) {
                is HomeItem.App -> homePackages.add(item.appModel.packageName)
                is HomeItem.Folder -> item.apps.forEach { homePackages.add(it.packageName) }
                else -> {}
            }
        }
        apps.filter { it.packageName !in homePackages }.sortedBy { it.label }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // WIDGET PICKER STATE
    private val _pendingWidgetRow = MutableStateFlow(0f)
    val pendingWidgetRow = _pendingWidgetRow.asStateFlow()

    private val _pendingWidgetCol = MutableStateFlow(0f)
    val pendingWidgetCol = _pendingWidgetCol.asStateFlow()

    private val _pendingWidgetInfo = MutableStateFlow<android.appwidget.AppWidgetProviderInfo?>(null)
    val pendingWidgetInfo = _pendingWidgetInfo.asStateFlow()

    fun setPendingWidgetPosition(row: Float, col: Float) {
        _pendingWidgetRow.value = row
        _pendingWidgetCol.value = col
    }

    fun setPendingWidgetInfo(info: android.appwidget.AppWidgetProviderInfo?) {
        _pendingWidgetInfo.value = info
    }

    val appsWithWidgets: StateFlow<List<AppModel>> = allApps.map { apps ->
        val providers = appWidgetManager.installedProviders
        val packagesWithWidgets = providers.map { it.provider.packageName }.toSet()
        apps.filter { it.packageName in packagesWithWidgets }.sortedBy { it.label }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getWidgetsForApp(packageName: String): List<android.appwidget.AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders.filter { it.provider.packageName == packageName }
    }

    fun updateItemPosition(item: HomeItem, newRow: Float, newCol: Float) {
        viewModelScope.launch {
            // Check for collisions and potential merges
            when (val collisionResult = checkCollision(item, newRow, newCol, ignoreUniqueKey = item.uniqueKey)) {
                is CollisionResult.None -> {
                    when (item) {
                        is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, newRow, newCol)
                        is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, newRow, newCol, item.spanX, item.spanY)
                        is HomeItem.Folder -> homeRepository.updateFolderPosition(item.id, newRow, newCol)
                    }
                }
                is CollisionResult.MergeApps -> {
                    if (item is HomeItem.App) {
                        homeRepository.createFolderFromApps(
                            appA = com.samidevstudio.neoglide.data.local.entity.HomeAppEntity(
                                id = item.id,
                                packageName = item.appModel.packageName,
                                row = item.row,
                                column = item.column
                            ),
                            appB = com.samidevstudio.neoglide.data.local.entity.HomeAppEntity(
                                id = collisionResult.targetApp.id,
                                packageName = collisionResult.targetApp.appModel.packageName,
                                row = collisionResult.targetApp.row,
                                column = collisionResult.targetApp.column
                            )
                        )
                        _uiEvent.emit(UiEvent.ShowToast("Folder Created"))
                    } else {
                        _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
                    }
                }
                is CollisionResult.AddToFolder -> {
                    if (item is HomeItem.App) {
                        homeRepository.addAppToFolder(collisionResult.targetFolder.id, item.appModel.packageName)
                        homeRepository.removeHomeAppById(item.id)
                        _uiEvent.emit(UiEvent.ShowToast("Added to ${collisionResult.targetFolder.label}"))
                    } else {
                        _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
                    }
                }
                is CollisionResult.Blocked -> {
                    _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
                }
            }
        }
    }

    private sealed class CollisionResult {
        object None : CollisionResult()
        object Blocked : CollisionResult()
        data class MergeApps(val targetApp: HomeItem.App) : CollisionResult()
        data class AddToFolder(val targetFolder: HomeItem.Folder) : CollisionResult()
    }

    private fun checkCollision(
        draggedItem: HomeItem, 
        newRow: Float, 
        newCol: Float,
        ignoreUniqueKey: String? = null
    ): CollisionResult {
        val currentItems = homeItems.value
        val draggedRect = android.graphics.RectF(newCol, newRow, newCol + draggedItem.spanX, newRow + draggedItem.spanY)
        
        currentItems.forEach { item ->
            // Skip self or ignored item via uniqueKey
            if (item.uniqueKey == ignoreUniqueKey) return@forEach
            
            val itemRect = android.graphics.RectF(item.column, item.row, item.column + item.spanX, item.row + item.spanY)
            
            if (android.graphics.RectF.intersects(draggedRect, itemRect)) {
                // Potential merge if both are apps and overlap is significant (e.g. centers are close)
                if (draggedItem is HomeItem.App) {
                    if (item is HomeItem.App) {
                        val distSq = (newRow - item.row) * (newRow - item.row) + (newCol - item.column) * (newCol - item.column)
                        if (distSq < 0.25f) { // roughly 0.5 unit distance
                            return CollisionResult.MergeApps(item)
                        }
                    } else if (item is HomeItem.Folder) {
                        val distSq = (newRow - item.row) * (newRow - item.row) + (newCol - item.column) * (newCol - item.column)
                        if (distSq < 0.25f) {
                            return CollisionResult.AddToFolder(item)
                        }
                    }
                }
                return CollisionResult.Blocked
            }
        }
        return CollisionResult.None
    }

    fun launchApp(packageName: String, options: android.os.Bundle? = null) {
        appRepository.launchApp(packageName, options)
    }

    suspend fun getShortcuts(packageName: String): List<AppShortcut> {
        return appRepository.getShortcuts(packageName)
    }

    fun launchShortcut(shortcut: AppShortcut) {
        appRepository.launchShortcut(shortcut.packageName, shortcut.id)
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            userPreferencesRepository.hideApp(packageName)
            homeRepository.cleanupPackage(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch {
            userPreferencesRepository.unhideApp(packageName)
        }
    }

    fun addWidget(widget: WidgetEntity) {
        viewModelScope.launch {
            widgetRepository.addWidget(widget)
        }
    }

    fun removeWidget(widgetId: Int) {
        viewModelScope.launch {
            widgetRepository.removeWidget(widgetId)
        }
    }

    fun removeHomeApp(id: Int) {
        viewModelScope.launch {
            homeRepository.removeHomeAppById(id)
        }
    }

    fun addHomeApp(packageName: String, row: Float, col: Float) {
        viewModelScope.launch {
            homeRepository.addHomeApp(com.samidevstudio.neoglide.data.local.entity.HomeAppEntity(
                packageName = packageName,
                row = row,
                column = col
            ))
        }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun completeWidgetConfiguration(widgetId: Int) {
        viewModelScope.launch {
            val info = appWidgetManager.getAppWidgetInfo(widgetId) ?: return@launch
            
            // Refined span calculation using standard 70dp cell formula (supports up to 5 cols)
            val spanX = ((info.minWidth + 30) / 70).coerceIn(1, 5).toFloat()
            val spanY = ((info.minHeight + 30) / 70).coerceIn(1, 6).toFloat()

            widgetRepository.addWidget(WidgetEntity(
                widgetId = widgetId,
                providerPackage = info.provider.packageName,
                providerClass = info.provider.className,
                label = info.loadLabel(context.packageManager),
                spanX = spanX,
                spanY = spanY,
                row = _pendingWidgetRow.value,
                column = _pendingWidgetCol.value
            ))
        }
    }

    fun cancelWidgetConfiguration(widgetId: Int) {
        viewModelScope.launch {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    // Removed unused onDragStart

    fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float) {
        viewModelScope.launch {
            val currentWidget = homeItems.value.find { it.id == widgetId && it is HomeItem.Widget } as? HomeItem.Widget ?: return@launch
            val tempWidget = currentWidget.copy(row = row, column = col, spanX = spanX, spanY = spanY)
            
            val collisionResult = checkCollision(tempWidget, row, col, ignoreUniqueKey = tempWidget.uniqueKey)
            if (collisionResult is CollisionResult.None) {
                widgetRepository.updateWidgetBounds(widgetId, row, col, spanX, spanY)
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
            }
        }
    }

    fun checkDefaultLauncher() {
        viewModelScope.launch {
            val isDefault = appRepository.isDefaultLauncher()
            val lastPromptTime = userPreferencesRepository.userPreferencesFlow.first().lastDefaultPromptTime
            val currentTime = System.currentTimeMillis()
            val oneDayInMillis = 24 * 60 * 60 * 1000L
            
            if (!isDefault && (currentTime - lastPromptTime > oneDayInMillis)) {
                _shouldShowDefaultPrompt.value = true
            }
        }
    }

    fun openDefaultLauncherSettings() {
        appRepository.openDefaultLauncherSettings()
        _shouldShowDefaultPrompt.value = false
    }

    fun dismissDefaultPrompt() {
        _shouldShowDefaultPrompt.value = false
        viewModelScope.launch {
            userPreferencesRepository.updateLastDefaultPromptTime(System.currentTimeMillis())
        }
    }

    fun updateFolderLabel(folderId: Int, label: String) {
        viewModelScope.launch {
            homeRepository.updateFolderLabel(folderId, label)
        }
    }

    fun removeFolder(folderId: Int) {
        viewModelScope.launch {
            homeRepository.removeFolder(folderId)
        }
    }

    fun addAppToFolder(folderId: Int, packageName: String) {
        viewModelScope.launch {
            homeRepository.addAppToFolder(folderId, packageName)
        }
    }

    fun removeAppFromFolder(folderId: Int, packageName: String) {
        viewModelScope.launch {
            homeRepository.removeAppFromFolder(folderId, packageName)
        }
    }

    fun removeAppFromFolder(folderId: Int, packageName: String, targetRow: Float, targetCol: Float) {
        viewModelScope.launch {
            // Find AppModel across all current apps to ensure we have valid data on rerun
            val appModel = allApps.value.find { it.packageName == packageName } ?: run {
                android.util.Log.e("HomeViewModel", "removeAppFromFolder FAILED: AppModel not found for $packageName. allApps size: ${allApps.value.size}")
                // If it's not in allApps, try the database directly as a fallback
                appRepository.allApps.first().find { it.packageName == packageName }
            } ?: run {
                android.util.Log.e("HomeViewModel", "removeAppFromFolder CRITICAL: App not found in database for $packageName")
                return@launch
            }

            // PREVENT dropping back into the same folder
            val sourceFolder = homeItems.value.find { it.id == folderId && it is HomeItem.Folder }
            if (sourceFolder != null) {
                val folderRect = android.graphics.RectF(sourceFolder.column, sourceFolder.row, sourceFolder.column + 1f, sourceFolder.row + 1f)
                val dropRect = android.graphics.RectF(targetCol, targetRow, targetCol + 1f, targetRow + 1f)
                if (android.graphics.RectF.intersects(folderRect, dropRect)) {
                    _uiEvent.emit(UiEvent.ShowToast("Item already in this folder"))
                    return@launch
                }
            }

            val tempApp = HomeItem.App(
                id = -1,
                appModel = appModel,
                row = targetRow,
                column = targetCol
            )
            
            // Ignore the folder we are coming from during collision check
            val collisionResult = checkCollision(
                tempApp, 
                targetRow, 
                targetCol, 
                ignoreUniqueKey = sourceFolder?.uniqueKey
            )
            
            android.util.Log.d("HomeViewModel", "removeAppFromFolder: pkg=$packageName, target=($targetRow, $targetCol), collision=$collisionResult")

            if (collisionResult is CollisionResult.None) {
                homeRepository.removeAppFromFolder(folderId, packageName, targetRow, targetCol)
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
            }
        }
    }
}
