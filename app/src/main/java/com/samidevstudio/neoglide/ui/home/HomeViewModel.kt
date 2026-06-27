package com.samidevstudio.neoglide.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.data.repository.HomeRepository
import com.samidevstudio.neoglide.data.repository.UserPreferencesRepository
import com.samidevstudio.neoglide.data.repository.WidgetRepository
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.service.NeoGlideNotificationListener
import com.samidevstudio.neoglide.ui.utils.LayoutManager
import com.samidevstudio.neoglide.ui.utils.WidgetUtils
import androidx.compose.ui.unit.dp
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    ) : HomeItem()

    data class Folder(
        override val id: Int,
        val label: String,
        val apps: List<AppModel>,
        override val row: Float,
        override val column: Float,
        override val spanX: Float = 1f,
        override val spanY: Float = 1f,
    ) : HomeItem()
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class FolderCreated(val folderId: Int) : UiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val widgetRepository: WidgetRepository,
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val appWidgetManager: AppWidgetManager,
    val appWidgetHost: AppWidgetHost,
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    private val preferences = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.samidevstudio.neoglide.data.repository.UserPreferences())

    fun triggerIconRefresh() {
        _refreshTrigger.value += 1
    }

    private val _shouldShowDefaultPrompt = MutableStateFlow(value = false)
    val shouldShowDefaultPrompt = _shouldShowDefaultPrompt.asStateFlow()

    private val _isSplashScreenFinished = MutableStateFlow(value = false)
    val isSplashScreenFinished = _isSplashScreenFinished.asStateFlow()

    fun onSplashScreenFinished() {
        _isSplashScreenFinished.value = true
    }

    private var lastAppLaunchFromDrawerTimestamp: Long = 0

    fun recordDrawerAppLaunch() {
        lastAppLaunchFromDrawerTimestamp = System.currentTimeMillis()
    }

    fun shouldCloseDrawerOnReturn(): Boolean {
        if (lastAppLaunchFromDrawerTimestamp == 0L) return false
        val elapsed = System.currentTimeMillis() - lastAppLaunchFromDrawerTimestamp
        // Reset timestamp after check to prevent repeated triggers from same launch
        lastAppLaunchFromDrawerTimestamp = 0L
        return elapsed > 10000L // 10 seconds
    }

    fun isWithinPeekWindow(): Boolean {
        if (lastAppLaunchFromDrawerTimestamp == 0L) return false
        val elapsed = System.currentTimeMillis() - lastAppLaunchFromDrawerTimestamp
        return elapsed <= 10000L
    }

    init {
        viewModelScope.launch {
            // Check for first install provisioning
            userPreferencesRepository.userPreferencesFlow
                .map { it.isFirstInstallRun }
                .distinctUntilChanged()
                .collect { isFirst ->
                    if (isFirst) {
                        // Wait for database to be ready before provisioning
                        appRepository.isDatabaseReady.first { it }
                        provisionDefaultHomeApps()
                    }
                }
        }

        var lastColumns = -1
        var lastMaxRows = -1

        viewModelScope.launch {
            // Monitor grid size and label mode for sanitization
            combine(
                userPreferencesRepository.userPreferencesFlow.map { it.gridSize }.distinctUntilChanged(),
                userPreferencesRepository.userPreferencesFlow.map { it.appLabelMode }.distinctUntilChanged(),
            ) { size, labelMode -> size to labelMode }
                .collect { (size, labelMode) ->
                    val screenWidthDp = context.resources.configuration.screenWidthDp
                    val screenHeightDp = context.resources.configuration.screenHeightDp

                    // VM STABILITY: Use estimated standard insets to match UI grid capacity
                    val layoutConfig = LayoutManager.calculateConfig(
                        screenWidthDp = screenWidthDp.dp, 
                        screenHeightDp = screenHeightDp.dp, 
                        densitySetting = size,
                        topInset = 80.dp,
                        bottomInset = 48.dp
                    )
                    val columns = layoutConfig.totalColumns
                    val maxRows = layoutConfig.totalRows

                    // Only sanitize if the grid actually shrank
                    if ((lastColumns != -1) && ((columns < lastColumns) || (maxRows < lastMaxRows))) {
                        sanitizeGridItems(columns, maxRows)
                    }

                    lastColumns = columns.toInt()
                    lastMaxRows = maxRows.toInt()
                }
        }
    }

    private suspend fun provisionDefaultHomeApps() {
        val coreApps = appRepository.getCoreAppsForProvisioning()
        if (coreApps.isNotEmpty()) {
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val screenWidthDp = context.resources.configuration.screenWidthDp
            val screenHeightDp = context.resources.configuration.screenHeightDp
            val layoutConfig = LayoutManager.calculateConfig(
                screenWidthDp = screenWidthDp.dp, 
                screenHeightDp = screenHeightDp.dp, 
                densitySetting = prefs.gridSize,
                topInset = 80.dp,
                bottomInset = 48.dp
            )
            val snapFactor = LayoutManager.SNAP_FACTOR

            val appCount = coreApps.size
            val totalCols = layoutConfig.totalColumns

            // Symmetric Balanced Math:
            // 1. Calculate a unified gap by distributing space across apps AND margins
            val rawGap = if (appCount > 1) (totalCols - appCount) / appCount else 0f
            val snappedGap = Math.floor(rawGap.toDouble() * snapFactor).toFloat() / snapFactor

            // 2. Center the group based on this gap and snap the start position
            val groupWidth = appCount + (appCount - 1) * snappedGap
            val startCol = Math.round(((totalCols - groupWidth) / 2f) * snapFactor) / snapFactor

            coreApps.forEachIndexed { index, packageName ->
                // Calculate position using snapped parameters to ensure grid alignment and symmetry
                val rawCol = startCol + index * (1f + snappedGap)
                val rawRow = 99f // Use Dock Marker for dynamic positioning

                homeRepository.addHomeApp(
                    com.samidevstudio.neoglide.data.local.entity.HomeAppEntity(
                        packageName = packageName,
                        row = rawRow,
                        column = rawCol
                    )
                )
            }
        }
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
                spanY = widget.spanY
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
        apps.asSequence()
            .filter { it.packageName !in homePackages }
            .sortedBy { it.label }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // WIDGET PICKER STATE
    private val _pendingWidgetRow = MutableStateFlow(0f)
    private val _pendingWidgetCol = MutableStateFlow(0f)
    private val _pendingWidgetInfo = MutableStateFlow<AppWidgetProviderInfo?>(null)

    fun setPendingWidgetPosition(row: Float, col: Float) {
        _pendingWidgetRow.value = row
        _pendingWidgetCol.value = col
    }

    fun setPendingWidgetInfo(info: AppWidgetProviderInfo?) {
        _pendingWidgetInfo.value = info
    }

    val appsWithWidgets: StateFlow<List<AppModel>> = allApps.map { apps ->
        val providers = appWidgetManager.installedProviders
        val packagesWithWidgets = providers.asSequence().map { it.provider.packageName }.toSet()
        apps.asSequence()
            .filter { it.packageName in packagesWithWidgets }
            .sortedBy { it.label }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getWidgetsForApp(packageName: String): List<AppWidgetProviderInfo> {
        return appWidgetManager.installedProviders.filter { it.provider.packageName == packageName }
    }

    fun updateItemPosition(item: HomeItem, newRow: Float, newCol: Float, maxRows: Float = 10f) {
        viewModelScope.launch {
            // Check for collisions and potential merges
            when (val collisionResult = checkCollision(item, newRow, newCol, maxRows = maxRows, ignoreUniqueKey = item.uniqueKey)) {
                is CollisionResult.None -> {
                    when (item) {
                        is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, newRow, newCol)
                        is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, newRow, newCol, item.spanX, item.spanY)
                        is HomeItem.Folder -> homeRepository.updateFolderPosition(item.id, newRow, newCol)
                    }
                }
                is CollisionResult.MergeApps -> {
                    if (item is HomeItem.App) {
                        val newFolderId = homeRepository.createFolderFromApps(
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
                        if (newFolderId != -1) {
                            _uiEvent.emit(UiEvent.FolderCreated(newFolderId))
                        }
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

    fun sanitizeGridItems(columns: Float, maxRows: Float) {
        viewModelScope.launch {
            val items = homeItems.value
            val removedItems = mutableListOf<String>()

            // Keep track of spots we've already "booked" during this sanitization pass
            // We use a local list to prevent stacking before the repository updates the Flow
            val bookedRects = items.mapNotNull { item ->
                val isAppOrFolder = (item is HomeItem.App) || (item is HomeItem.Folder)
                val showLabels = userPreferencesRepository.userPreferencesFlow.first().let {
                    (it.appLabelMode == AppLabelMode.HOME_ONLY) || (it.appLabelMode == AppLabelMode.BOTH)
                }
                val effectiveSpanY = if (isAppOrFolder && showLabels) 1.5f else item.spanY

                // If it's already inside, book it. If outside, don't book yet.
                if (item.column + item.spanX <= columns && item.row + effectiveSpanY <= maxRows) {
                    android.graphics.RectF(item.column, item.row, item.column + item.spanX, item.row + effectiveSpanY)
                } else null
            }.toMutableList()

            items.forEach { item ->
                val isAppOrFolder = (item is HomeItem.App) || (item is HomeItem.Folder)
                val showLabels = userPreferencesRepository.userPreferencesFlow.first().let {
                    (it.appLabelMode == AppLabelMode.HOME_ONLY) || (it.appLabelMode == AppLabelMode.BOTH)
                }
                val effectiveSpanY = if (isAppOrFolder && showLabels) 1.5f else item.spanY

                val isOutside = item.column + item.spanX > columns || item.row + effectiveSpanY > maxRows

                if (isOutside) {
                    var foundRow = -1f
                    var foundCol = -1f

                    val snapFactor = LayoutManager.SNAP_FACTOR
                    val step = 1f / snapFactor

                    outer@for (rowIdx in 0 until (maxRows * snapFactor).toInt()) {
                        for (colIdx in 0 until (columns * snapFactor).toInt()) {
                            val row = rowIdx * step
                            val col = colIdx * step

                            if (col + item.spanX > columns || row + effectiveSpanY > maxRows) continue

                            val targetRect = android.graphics.RectF(col, row, col + item.spanX, row + effectiveSpanY)

                            // Check if this spot is booked by any other item
                            val isOccupied = bookedRects.any { android.graphics.RectF.intersects(it, targetRect) }
                            if (isOccupied) continue

                            foundRow = row
                            foundCol = col
                            break@outer
                        }
                    }

                    if (foundRow != -1f) {
                        bookedRects.add(android.graphics.RectF(foundCol, foundRow, foundCol + item.spanX, foundRow + effectiveSpanY))
                        when (item) {
                            is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, foundRow, foundCol)
                            is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, foundRow, foundCol, item.spanX, item.spanY)
                            is HomeItem.Folder -> homeRepository.updateFolderPosition(item.id, foundRow, foundCol)
                        }
                    } else {
                        val label = when (item) {
                            is HomeItem.App -> item.appModel.label
                            is HomeItem.Widget -> item.widgetEntity.label
                            is HomeItem.Folder -> item.label
                        }
                        removedItems.add(label)

                        when (item) {
                            is HomeItem.App -> homeRepository.removeHomeAppById(item.id)
                            is HomeItem.Widget -> widgetRepository.removeWidget(item.id)
                            is HomeItem.Folder -> homeRepository.removeFolder(item.id)
                        }
                    }
                }
            }

            if (removedItems.isNotEmpty()) {
                _uiEvent.emit(UiEvent.ShowToast("Removed ${removedItems.joinToString(", ")} (No space in new grid)"))
            }
        }
    }

    private sealed class CollisionResult {
        object None : CollisionResult()
        object Blocked : CollisionResult()
        data class MergeApps(val targetApp: HomeItem.App) : CollisionResult()
        data class AddToFolder(val targetFolder: HomeItem.Folder) : CollisionResult()
    }

    private fun findAvailableSpace(
        prefRow: Float,
        prefCol: Float,
        spanX: Float,
        spanY: Float,
        maxRows: Float,
        columns: Float
    ): Pair<Float, Float>? {
        // 1. Check if preferred spot is available
        val tempItem = HomeItem.App(-1, AppModel("", "", com.samidevstudio.neoglide.domain.model.AppCategory.OTHER), prefRow, prefCol, spanX, spanY)
        if (checkCollision(tempItem, prefRow, prefCol, maxRows) is CollisionResult.None) {
            return prefRow to prefCol
        }

        // 2. Scan entire grid with fractional steps
        val snapFactor = LayoutManager.SNAP_FACTOR
        val step = 1f / snapFactor

        for (rowIdx in 0 until (maxRows * snapFactor).toInt()) {
            for (colIdx in 0 until (columns * snapFactor).toInt()) {
                val row = rowIdx * step
                val col = colIdx * step

                // Stay within grid bounds
                if (col + spanX > columns || row + spanY > maxRows) continue

                if (checkCollision(tempItem, row, col, maxRows) is CollisionResult.None) {
                    return row to col
                }
            }
        }
        return null
    }

    private fun checkCollision(
        draggedItem: HomeItem,
        newRow: Float,
        newCol: Float,
        maxRows: Float,
        ignoreUniqueKey: String? = null
    ): CollisionResult {
        val currentItems = homeItems.value
        val draggedRect = android.graphics.RectF(newCol, newRow, newCol + draggedItem.spanX, newRow + draggedItem.spanY)

        currentItems.forEach { item ->
            // Skip self or ignored item via uniqueKey
            if (item.uniqueKey == ignoreUniqueKey) return@forEach

            val effectiveRow = when {
                item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                else -> item.row
            }

            val visualCol = item.column

            val itemRect = android.graphics.RectF(visualCol, effectiveRow, visualCol + item.spanX, effectiveRow + item.spanY)

            if (android.graphics.RectF.intersects(draggedRect, itemRect)) {
                // Potential merge if both are apps and overlap is significant (e.g. centers are close)
                if (draggedItem is HomeItem.App) {
                    if (item is HomeItem.App) {
                        val distSq = (newRow - effectiveRow) * (newRow - effectiveRow) + (newCol - visualCol) * (newCol - visualCol)
                        // Tighter threshold for finer grid: 0.25 radius (distSq < 0.0625)
                        if (distSq < 0.0625f) {
                            return CollisionResult.MergeApps(item)
                        }
                    } else if (item is HomeItem.Folder) {
                        val distSq = (newRow - effectiveRow) * (newRow - effectiveRow) + (newCol - visualCol) * (newCol - visualCol)
                        if (distSq < 0.0625f) {
                            return CollisionResult.AddToFolder(item)
                        }
                    }
                }
                return CollisionResult.Blocked
            }
        }
        return CollisionResult.None
    }

    fun isSpaceOccupied(row: Float, col: Float, spanX: Float, spanY: Float, maxRows: Float, ignoreUniqueKey: String?): Boolean {
        val rect = android.graphics.RectF(col, row, col + spanX, row + spanY)
        return homeItems.value.any { item ->
            if (item.uniqueKey == ignoreUniqueKey) return@any false
            
            val effectiveRow = when {
                item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                else -> item.row
            }
            val itemRect = android.graphics.RectF(item.column, effectiveRow, item.column + item.spanX, effectiveRow + item.spanY)
            android.graphics.RectF.intersects(rect, itemRect)
        }
    }

    fun launchApp(packageName: String, options: android.os.Bundle? = null) {
        appRepository.launchApp(packageName, options)
    }

    suspend fun getShortcuts(packageName: String): List<AppShortcut> {
        val shortcuts = appRepository.getShortcuts(packageName)
        return shortcuts
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
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val screenWidthDp = context.resources.configuration.screenWidthDp
            val screenHeightDp = context.resources.configuration.screenHeightDp

            val layoutConfig = LayoutManager.calculateConfig(
                screenWidthDp = screenWidthDp.dp, 
                screenHeightDp = screenHeightDp.dp, 
                densitySetting = prefs.gridSize,
                topInset = 80.dp,
                bottomInset = 48.dp
            )
            val columns = layoutConfig.totalColumns
            val effectiveMaxRows = layoutConfig.totalRows

            val (finalRow, finalCol) = findAvailableSpace(row, col, 1f, 1f, effectiveMaxRows, columns) ?: run {
                _uiEvent.emit(UiEvent.ShowToast("Home screen is full"))
                return@launch
            }

            homeRepository.addHomeApp(
                com.samidevstudio.neoglide.data.local.entity.HomeAppEntity(
                    packageName = packageName,
                    row = finalRow,
                    column = finalCol
                )
            )
        }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun completeWidgetConfiguration(widgetId: Int) {
        viewModelScope.launch {
            val info = try {
                appWidgetManager.getAppWidgetInfo(widgetId)
            } catch (_: Exception) {
                null
            } ?: return@launch

            // Use current grid preferences for span calculation
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            val screenWidthDp = context.resources.configuration.screenWidthDp
            val screenHeightDp = context.resources.configuration.screenHeightDp

            val layoutConfig = LayoutManager.calculateConfig(
                screenWidthDp = screenWidthDp.dp, 
                screenHeightDp = screenHeightDp.dp, 
                densitySetting = prefs.gridSize,
                topInset = 80.dp,
                bottomInset = 48.dp
            )
            val columns = layoutConfig.totalColumns
            val effectiveMaxRows = layoutConfig.totalRows
            // Widget spans are based on the full cell size
            val cellWidthDp = layoutConfig.unitWidth.value
            val cellHeightDp = layoutConfig.unitHeight.value

            val (spanX, spanY) = WidgetUtils.calculateProjectedWidgetSpan(
                context = context, 
                info = info, 
                unitWidthDp = cellWidthDp, 
                unitHeightDp = cellHeightDp, 
                maxColumns = columns.toInt()
            )

            // Find available space if preferred spot is taken
            val preferredRow = _pendingWidgetRow.value
            val preferredCol = _pendingWidgetCol.value

            val (finalRow, finalCol) = findAvailableSpace(
                preferredRow, preferredCol, spanX, spanY, effectiveMaxRows, columns
            ) ?: run {
                _uiEvent.emit(UiEvent.ShowToast("Home screen is full"))
                appWidgetHost.deleteAppWidgetId(widgetId)
                return@launch
            }

            widgetRepository.addWidget(
                WidgetEntity(
                    widgetId = widgetId,
                    providerPackage = info.provider.packageName,
                    providerClass = info.provider.className,
                    label = info.loadLabel(context.packageManager),
                    spanX = spanX,
                    spanY = spanY,
                    row = finalRow,
                    column = finalCol
                )
            )
        }
    }

    fun cancelWidgetConfiguration(widgetId: Int) {
        viewModelScope.launch {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    // Removed unused onDragStart

    fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float, maxRows: Float = 10f) {
        viewModelScope.launch {
            val currentWidget = homeItems.value.find { it.id == widgetId && it is HomeItem.Widget } as? HomeItem.Widget ?: return@launch
            val tempWidget = currentWidget.copy(row = row, column = col, spanX = spanX, spanY = spanY)

            val collisionResult = checkCollision(tempWidget, row, col, maxRows = maxRows, ignoreUniqueKey = tempWidget.uniqueKey)
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

    fun dissolveFolder(folderId: Int) {
        viewModelScope.launch {
            homeRepository.dissolveFolder(folderId)
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

    fun removeAppFromFolder(folderId: Int, packageName: String, targetRow: Float, targetCol: Float) {
        viewModelScope.launch {
            // Find AppModel across all current apps to ensure we have valid data on rerun
            val appModel = allApps.value.find { it.packageName == packageName } ?: run {
                // If it's not in allApps, try the database directly as a fallback
                appRepository.allApps.first().find { it.packageName == packageName }
            } ?: return@launch

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
            // Use default maxRows (10) as a fallback for folder removal if not provided,
            // though ideally this should also be passed from UI.
            val collisionResult = checkCollision(
                tempApp,
                targetRow,
                targetCol,
                maxRows = 10f,
                ignoreUniqueKey = sourceFolder?.uniqueKey
            )

            if (collisionResult is CollisionResult.None) {
                homeRepository.removeAppFromFolder(folderId, packageName, targetRow, targetCol)
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
            }
        }
    }
}
