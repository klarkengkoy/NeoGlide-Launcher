package com.samidevstudio.pxllauncherneo.ui.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import com.samidevstudio.pxllauncherneo.data.repository.AppRepository
import com.samidevstudio.pxllauncherneo.data.repository.HomeRepository
import com.samidevstudio.pxllauncherneo.data.repository.UserPreferencesRepository
import com.samidevstudio.pxllauncherneo.data.repository.WidgetRepository
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut
import com.samidevstudio.pxllauncherneo.service.PxlNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeItem {
    abstract val id: Int
    abstract val row: Float
    abstract val column: Float
    abstract val spanX: Float
    abstract val spanY: Float

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
        val isCustom: Boolean = false // New flag for internal widgets like Dock
    ) : HomeItem()
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val widgetRepository: WidgetRepository,
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val appWidgetManager: AppWidgetManager,
    val appWidgetHost: AppWidgetHost
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _shouldShowDefaultPrompt = MutableStateFlow(value = false)
    val shouldShowDefaultPrompt = _shouldShowDefaultPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            // 1. Provision the internal Dock widget into the standard database strictly on FIRST INSTALL
            val prefs = userPreferencesRepository.userPreferencesFlow.first()
            if (prefs.isFirstInstallRun) {
                // Double check DB to prevent accidental duplicates
                val existing = widgetRepository.allWidgets.first()
                if (existing.none { it.providerPackage == "internal" && it.providerClass == "dock" }) {
                    // Provision internal Dock (Floating placeholder: 99.5f)
                    val dockId = widgetRepository.allocateWidgetId()
                    widgetRepository.addWidget(WidgetEntity(
                        widgetId = dockId,
                        providerPackage = "internal",
                        providerClass = "dock",
                        label = "Dock",
                        row = 99.5f,
                        column = 0f,
                        spanX = 4f,
                        spanY = 1f
                    ))
                }
                userPreferencesRepository.setFirstInstallRun(isFirst = false)
            }

            // 2. Refresh apps after provisioning
            appRepository.refreshApps()
        }
    }

    val activeNotifications: StateFlow<Map<String, Int>> = PxlNotificationListener.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val homeItems: StateFlow<List<HomeItem>> = combine(
        appRepository.allApps,
        homeRepository.allHomeApps,
        homeRepository.allWidgets
    ) { apps, homeApps, widgets ->
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
        
        appItems + widgetItems
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

    fun updateItemPosition(item: HomeItem, newRow: Float, newCol: Float) {
        viewModelScope.launch {
            // Check for collisions
            val canMove = rearrangeItems(item, newRow, newCol)
            
            if (canMove) {
                when (item) {
                    is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, newRow, newCol)
                    is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, newRow, newCol, item.spanX, item.spanY)
                }
            } else {
                _uiEvent.emit(UiEvent.ShowToast("Space already occupied"))
            }
        }
    }

    private fun rearrangeItems(draggedItem: HomeItem, newRow: Float, newCol: Float): Boolean {
        val currentItems = homeItems.value
        val draggedRect = android.graphics.RectF(newCol, newRow, newCol + draggedItem.spanX, newRow + draggedItem.spanY)
        
        currentItems.forEach { item ->
            // Skip the item itself (but check ID and type to be safe)
            if (item.id == draggedItem.id && ((item is HomeItem.App && draggedItem is HomeItem.App) || (item is HomeItem.Widget && draggedItem is HomeItem.Widget))) return@forEach
            
            val itemRect = android.graphics.RectF(item.column, item.row, item.column + item.spanX, item.row + item.spanY)
            
            if (android.graphics.RectF.intersects(draggedRect, itemRect)) {
                // Universal collision: no displacement allowed
                return false
            }
        }
        return true
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

    fun addHomeApp(packageName: String, row: Float, col: Float) {
        viewModelScope.launch {
            homeRepository.addHomeApp(com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity(
                packageName = packageName,
                row = row,
                column = col
            ))
        }
    }

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    // Removed unused onDragStart

    fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float) {
        viewModelScope.launch {
            val currentWidget = homeItems.value.find { it.id == widgetId && it is HomeItem.Widget } as? HomeItem.Widget ?: return@launch
            val tempWidget = currentWidget.copy(row = row, column = col, spanX = spanX, spanY = spanY)
            
            val canMove = rearrangeItems(tempWidget, row, col)
            if (canMove) {
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
}
