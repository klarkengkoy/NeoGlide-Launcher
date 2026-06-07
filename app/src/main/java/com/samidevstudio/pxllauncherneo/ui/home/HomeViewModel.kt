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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val widgetRepository: WidgetRepository,
    private val homeRepository: HomeRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    val appWidgetManager: AppWidgetManager,
    val appWidgetHost: AppWidgetHost
) : ViewModel() {

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
                userPreferencesRepository.setFirstInstallRun(false)
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

    fun updateItemPosition(item: HomeItem, newRow: Float, newCol: Float) {
        viewModelScope.launch {
            // Check for collisions and rearrange
            rearrangeItems(item, newRow, newCol)
            
            when (item) {
                is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, newRow, newCol)
                is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, newRow, newCol, item.spanX, item.spanY)
            }
        }
    }

    private suspend fun rearrangeItems(draggedItem: HomeItem, newRow: Float, newCol: Float) {
        val currentItems = homeItems.value
        val draggedRect = android.graphics.RectF(newCol, newRow, newCol + draggedItem.spanX, newRow + draggedItem.spanY)
        
        currentItems.forEach { item ->
            if (item.id == draggedItem.id && ((item is HomeItem.App && draggedItem is HomeItem.App) || (item is HomeItem.Widget && draggedItem is HomeItem.Widget))) return@forEach
            
            val itemRect = android.graphics.RectF(item.column, item.row, item.column + item.spanX, item.row + item.spanY)
            
            if (android.graphics.RectF.intersects(draggedRect, itemRect)) {
                // Collision detected! Push the item down for now.
                // Simple implementation: move item to the next available row
                val pushToRow = newRow + draggedItem.spanY
                when (item) {
                    is HomeItem.App -> homeRepository.updateHomeAppPosition(item.id, pushToRow, item.column)
                    is HomeItem.Widget -> homeRepository.updateWidgetBounds(item.id, pushToRow, item.column, item.spanX, item.spanY)
                }
            }
        }
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

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    fun onDragStart() {
        // Handle drag start global state if needed
    }

    fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float) {
        viewModelScope.launch {
            widgetRepository.updateWidgetBounds(widgetId, row, col, spanX, spanY)
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
