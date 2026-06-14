package com.samidevstudio.neoglide.ui.drawer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.neoglide.data.local.entity.HomeAppEntity
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.data.repository.CategoryBarType
import com.samidevstudio.neoglide.data.repository.HomeRepository
import com.samidevstudio.neoglide.data.repository.HorizontalAnchor
import com.samidevstudio.neoglide.data.repository.SearchRepository
import com.samidevstudio.neoglide.data.repository.UserPreferencesRepository
import com.samidevstudio.neoglide.data.repository.VerticalAnchor
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.service.NeoGlideNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

sealed class DrawerUiEvent {
    data class ShowToast(val message: String) : DrawerUiEvent()
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

    private val _uiEvent = MutableSharedFlow<DrawerUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun showToast(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(DrawerUiEvent.ShowToast(message))
        }
    }

    private val _searchQuery = savedStateHandle.getStateFlow("search_query", "")
    val searchQuery = _searchQuery

    private val _webSuggestions = MutableStateFlow<List<String>>(emptyList())
    val webSuggestions = _webSuggestions.asStateFlow()

    private val preferences = userPreferencesRepository.userPreferencesFlow

    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    fun selectCategory(category: AppCategory?) {
        _selectedCategory.value = category
    }

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger = _refreshTrigger.asStateFlow()

    fun triggerIconRefresh() {
        _refreshTrigger.value += 1
    }

    val allApps: StateFlow<List<AppModel>> = appRepository.allApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotifications: StateFlow<Map<String, Int>> = NeoGlideNotificationListener.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentlyUsedApps: StateFlow<List<AppModel>> = appRepository.allApps
        .map { apps ->
            apps.filter { it.lastUsedTime > 0 }
                .sortedByDescending { it.lastUsedTime }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorizedApps: StateFlow<Map<AppCategory?, List<DrawerItem>>> = combine(
        appRepository.allApps,
        homeRepository.allDrawerFolders,
        preferences
    ) { apps, folders, prefs ->
        val filteredApps = apps.filter { it.packageName !in prefs.hiddenPackages || prefs.showHiddenApps }
        val appsMap = filteredApps.associateBy { it.packageName }
        
        val result: Map<AppCategory?, List<DrawerItem>> = if (prefs.categoryBarType == CategoryBarType.NONE) {
            // CATEGORYLESS MODE: Show all apps and folders in one unified list
            val unifiedFolders = folders.filter { it.folder.category == "CATEGORYLESS" }.map { folderWithApps ->
                val folderApps = folderWithApps.apps.mapNotNull { folderApp ->
                    appsMap[folderApp.packageName]
                }
                DrawerItem.Folder(folderWithApps.folder.id, folderWithApps.folder.label, folderApps)
            }
            
            val appItems = filteredApps
                .filter { it.category != AppCategory.FOLDER && it.packageName !in folders.flatMap { f -> f.apps.map { a -> a.packageName } }.toSet() }
                .map { DrawerItem.App(it) }
            
            // Map to null key for consistency with selectedCategory being null
            mapOf(null to (unifiedFolders + appItems))
        } else {
            // CATEGORIZED MODE: Only show folders belonging to standard categories
            val appItems = filteredApps
                .filter { it.category != AppCategory.FOLDER }
                .map { app ->
                    val category = if (app.packageName in prefs.hiddenPackages) AppCategory.HIDDEN else app.category
                    category to DrawerItem.App(app)
                }
            
            val folderItems = folders
                .filter { it.folder.category != null && it.folder.category != "CATEGORYLESS" }
                .map { folderWithApps ->
                    val category = AppCategory.fromString(folderWithApps.folder.category!!)
                    val folderApps = folderWithApps.apps.mapNotNull { folderApp ->
                        appsMap[folderApp.packageName]
                    }
                    category to DrawerItem.Folder(folderWithApps.folder.id, folderWithApps.folder.label, folderApps)
                }
            
            val grouped = (appItems + folderItems).groupBy({ it.first as AppCategory? }, { it.second })
            grouped
        }
        result
    }.flowOn(Dispatchers.Default)
    .stateIn(
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
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val gridItems: StateFlow<List<DrawerItem?>> = combine(
        categorizedApps,
        _selectedCategory,
        preferences
    ) { categorized, selected, prefs ->
        val items = categorized[selected] ?: emptyList()
        if (items.isEmpty()) return@combine emptyList<DrawerItem?>()

        val columns = 4 
        
        val sortingMode = prefs.sortingMode
        val isReverse = prefs.isSortReverse

        val baseComparator = when (sortingMode) {
            com.samidevstudio.neoglide.data.repository.SortingMode.ALPHABETICAL -> compareBy<AppModel> { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.INSTALL_TIME -> compareByDescending<AppModel> { it.installTime }.thenBy { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.LAST_USED -> compareByDescending<AppModel> { it.lastUsedTime }.thenBy { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.ICON_COLOR -> compareBy<AppModel> { it.dominantHue }.thenBy { it.label.lowercase() }
        }

        val finalComparator = if (isReverse) baseComparator.reversed() else baseComparator

        val folders = items.filterIsInstance<DrawerItem.Folder>().sortedBy { it.label }
        val apps = items.filterIsInstance<DrawerItem.App>()
            .map { it.appModel }
            .sortedWith(finalComparator)
            .map { DrawerItem.App(it) }
        
        val verticalAnchor = prefs.verticalAnchor
        val horizontalAnchor = prefs.horizontalAnchor

        android.util.Log.d("NeoGlideDrawer", "Calculating Grid: vAnchor=$verticalAnchor, hAnchor=$horizontalAnchor, items=${items.size}")

        val totalContentCount = items.size
        val numRows = (totalContentCount + columns - 1) / columns
        val rem = totalContentCount % columns
        val placeholdersCount = if (rem > 0) columns - rem else 0

        val contentSlots = mutableListOf<Pair<Int, Int>>()
        val totalGridSlots = numRows * columns

        for (index in 0 until totalGridSlots) {
            val isP = when {
                verticalAnchor == VerticalAnchor.TOP && horizontalAnchor == HorizontalAnchor.LEFT -> index >= totalContentCount
                verticalAnchor == VerticalAnchor.TOP && horizontalAnchor == HorizontalAnchor.RIGHT -> {
                    val lastRowStart = (numRows - 1) * columns
                    index >= lastRowStart && index < lastRowStart + placeholdersCount
                }
                verticalAnchor == VerticalAnchor.BOTTOM && horizontalAnchor == HorizontalAnchor.LEFT -> {
                    index >= rem && index < rem + placeholdersCount
                }
                verticalAnchor == VerticalAnchor.BOTTOM && horizontalAnchor == HorizontalAnchor.RIGHT -> {
                    index < placeholdersCount
                }
                else -> false
            }
            if (isP) {
                android.util.Log.v("NeoGlideDrawer", "Index $index is Placeholder")
            }
            if (!isP) {
                contentSlots.add(index / columns to index % columns)
            }
        }
        
        val sortedContentSlots = contentSlots.sortedWith(compareBy { (r, c) ->
            val rowDist = if (verticalAnchor == VerticalAnchor.TOP) r else (numRows - 1 - r)
            val colDist = if (horizontalAnchor == HorizontalAnchor.LEFT) c else (columns - 1 - c)
            rowDist * 100 + colDist
        })
        
        val folderAssignedSlots = sortedContentSlots.take(folders.size).sortedWith(compareBy({ it.first }, { it.second }))
        val appAssignedSlots = sortedContentSlots.drop(folders.size).sortedWith(compareBy({ it.first }, { it.second }))
        
        val slotToItem = mutableMapOf<Pair<Int, Int>, DrawerItem>()
        folderAssignedSlots.forEachIndexed { i, slot -> slotToItem[slot] = folders[i] }
        appAssignedSlots.forEachIndexed { i, slot -> slotToItem[slot] = apps[i] }
        
        val result = mutableListOf<DrawerItem?>()
        for (index in 0 until totalGridSlots) {
            val isP = when {
                verticalAnchor == VerticalAnchor.TOP && horizontalAnchor == HorizontalAnchor.LEFT -> index >= totalContentCount
                verticalAnchor == VerticalAnchor.TOP && horizontalAnchor == HorizontalAnchor.RIGHT -> {
                    val lastRowStart = (numRows - 1) * columns
                    index >= lastRowStart && index < lastRowStart + placeholdersCount
                }
                verticalAnchor == VerticalAnchor.BOTTOM && horizontalAnchor == HorizontalAnchor.LEFT -> {
                    index >= rem && index < rem + placeholdersCount
                }
                verticalAnchor == VerticalAnchor.BOTTOM && horizontalAnchor == HorizontalAnchor.RIGHT -> {
                    index < placeholdersCount
                }
                else -> false
            }
            if (isP) {
                result.add(null)
            } else {
                result.add(slotToItem[index / columns to index % columns])
            }
        }
        result
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun createFolder(appA: AppModel, appB: AppModel, category: AppCategory?) {
        viewModelScope.launch {
            val prefs = preferences.first()
            val folderCategory = if (prefs.categoryBarType == CategoryBarType.NONE) "CATEGORYLESS" else category?.name ?: "OTHER"
            
            homeRepository.cleanupDrawerMembership(appA.packageName)
            homeRepository.cleanupDrawerMembership(appB.packageName)
            
            homeRepository.createFolderFromApps(
                appA = HomeAppEntity(id = 0, packageName = appA.packageName, row = 0f, column = 0f),
                appB = HomeAppEntity(id = 0, packageName = appB.packageName, row = 0f, column = 0f),
                label = "Folder",
                category = folderCategory
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
