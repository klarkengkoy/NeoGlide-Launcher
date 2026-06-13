package com.samidevstudio.pxllauncherneo.ui.drawer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import com.samidevstudio.pxllauncherneo.data.repository.AppRepository
import com.samidevstudio.pxllauncherneo.data.repository.HomeRepository
import com.samidevstudio.pxllauncherneo.data.repository.SearchRepository
import com.samidevstudio.pxllauncherneo.data.repository.UserPreferencesRepository
import com.samidevstudio.pxllauncherneo.domain.model.AppCategory
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut
import com.samidevstudio.pxllauncherneo.service.PxlNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DrawerItem {
    abstract val label: String

    data class App(val appModel: AppModel) : DrawerItem() {
        override val label: String get() = appModel.label
    }

    data class Folder(val id: Int, override val label: String, val apps: List<AppModel>) : DrawerItem()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val searchRepository: SearchRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val homeRepository: HomeRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _searchQuery = savedStateHandle.getStateFlow("search_query", "")
    val searchQuery = _searchQuery

    private val _webSuggestions = MutableStateFlow<List<String>>(emptyList())
    val webSuggestions = _webSuggestions.asStateFlow()

    private val preferences = userPreferencesRepository.userPreferencesFlow

    val allApps: StateFlow<List<AppModel>> = appRepository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotifications: StateFlow<Map<String, Int>> = PxlNotificationListener.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentlyUsedApps: StateFlow<List<AppModel>> = appRepository.allApps
        .map { apps ->
            apps.filter { it.lastUsedTime > 0 }
                .sortedByDescending { it.lastUsedTime }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorizedApps: StateFlow<Map<AppCategory, List<DrawerItem>>> = combine(
        appRepository.allApps,
        homeRepository.allDrawerFolders,
        preferences
    ) { apps, folders, prefs ->
        val filteredApps = apps.filter { it.packageName !in prefs.hiddenPackages || prefs.showHiddenApps }
        
        val appItems = filteredApps
            .filter { it.category != AppCategory.FOLDER }
            .map { app ->
            val category = if (app.packageName in prefs.hiddenPackages) AppCategory.HIDDEN else app.category
            category to DrawerItem.App(app)
        }
        
        val folderItems = folders.filter { it.folder.category != null }.map { folderWithApps ->
            val category = AppCategory.fromString(folderWithApps.folder.category!!)
            val folderApps = folderWithApps.apps.mapNotNull { folderApp ->
                apps.find { it.packageName == folderApp.packageName }
            }
            category to DrawerItem.Folder(folderWithApps.folder.id, folderWithApps.folder.label, folderApps)
        }
        
        (appItems + folderItems).groupBy({ it.first }, { it.second })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val filteredApps: StateFlow<List<AppModel>> = combine(
        appRepository.allApps,
        _searchQuery,
        preferences
    ) { apps, query, prefs ->
        if (query.isBlank()) {
            emptyList()
        } else {
            apps.filter { it.packageName !in prefs.hiddenPackages || prefs.showHiddenApps }
                .filter { it.label.contains(query, ignoreCase = true) }
                .sortedWith(compareByDescending<AppModel> { 
                    it.label.startsWith(query, ignoreCase = true) 
                }.thenBy { it.label.length }.thenBy { it.label })
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        _searchQuery
            .debounce(300)
            .filter { it.isNotBlank() }
            .onEach { query ->
                val suggestions = searchRepository.getWebSuggestions(query)
                _webSuggestions.value = suggestions
            }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        savedStateHandle["search_query"] = query
        if (query.isBlank()) {
            _webSuggestions.value = emptyList()
        }
    }

    fun resetState() {
        onSearchQueryChanged("")
        _webSuggestions.value = emptyList()
    }

    fun launchFirstResult(options: android.os.Bundle? = null) {
        filteredApps.value.firstOrNull()?.let { app ->
            launchApp(app.packageName, options)
        }
    }

    fun launchApp(packageName: String, options: android.os.Bundle? = null) {
        onSearchQueryChanged("")
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

    fun moveAppToCategory(packageName: String, category: AppCategory) {
        viewModelScope.launch {
            homeRepository.cleanupDrawerMembership(packageName)
            appRepository.updateAppCategory(packageName, category)
        }
    }

    fun createFolder(appA: AppModel, appB: AppModel, category: AppCategory) {
        viewModelScope.launch {
            homeRepository.cleanupDrawerMembership(appA.packageName)
            homeRepository.cleanupDrawerMembership(appB.packageName)
            
            homeRepository.createFolderFromApps(
                appA = HomeAppEntity(id = 0, packageName = appA.packageName, row = 0f, column = 0f),
                appB = HomeAppEntity(id = 0, packageName = appB.packageName, row = 0f, column = 0f),
                label = "Folder",
                category = category.name
            )
            appRepository.markAppAsInFolder(appA.packageName)
            appRepository.markAppAsInFolder(appB.packageName)
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

    fun moveFolderToCategory(folderId: Int, category: AppCategory) {
        viewModelScope.launch {
            homeRepository.updateFolderCategory(folderId, category.name)
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
}
