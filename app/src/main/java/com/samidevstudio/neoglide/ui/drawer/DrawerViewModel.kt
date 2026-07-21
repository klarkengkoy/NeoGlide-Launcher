package com.samidevstudio.neoglide.ui.drawer

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.neoglide.data.local.entity.HomeAppEntity
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.data.repository.CategoryBarType
import com.samidevstudio.neoglide.data.repository.CategoryRepository
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    data class FolderCreated(val folderId: Int) : DrawerUiEvent()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class DrawerViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val searchRepository: SearchRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val homeRepository: HomeRepository,
    private val categoryRepository: CategoryRepository,
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

    val userPreferences: StateFlow<com.samidevstudio.neoglide.data.repository.UserPreferences> = userPreferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.samidevstudio.neoglide.data.repository.UserPreferences())

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

    private val _drawerColumns = MutableStateFlow(5)
    val drawerColumns = _drawerColumns.asStateFlow()

    fun updateDrawerColumns(columns: Int) {
        if (_drawerColumns.value != columns) {
            _drawerColumns.value = columns
        }
    }

    val allApps: StateFlow<List<AppModel>> = combine(
        appRepository.allApps,
        categoryRepository.allCategories
    ) { apps, overrides ->
        val overrideMap = overrides.associateBy { it.name }
        apps.map { app ->
            val override = overrideMap[app.category.name]
            if (override != null) {
                app.copy(category = app.category.copy(
                    label = override.label ?: app.category.label,
                    iconName = override.iconName ?: app.category.iconName
                ))
            } else app
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotifications: StateFlow<Map<String, Int>> = NeoGlideNotificationListener.activeNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val recentlyUsedApps: StateFlow<List<AppModel>> = allApps
        .map { apps ->
            apps.filter { it.lastUsedTime > 0 }
                .sortedByDescending { it.lastUsedTime }
                .take(5)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categorizedApps: StateFlow<List<Pair<AppCategory?, List<DrawerItem>>>> = combine(
        allApps,
        homeRepository.allDrawerFolders.distinctUntilChanged(),
        categoryRepository.allCategories.distinctUntilChanged(),
        preferences.distinctUntilChanged()
    ) { apps, folders, customCatEntities, prefs ->
        val filteredApps = apps.filter { it.packageName !in prefs.hiddenPackages || prefs.showHiddenApps }
        val appsMap = filteredApps.associateBy { it.packageName }
        
        val result: List<Pair<AppCategory?, List<DrawerItem>>> = if (prefs.categoryBarType == CategoryBarType.NONE) {
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
            listOf(null to (unifiedFolders + appItems))
        } else {
            // CATEGORIZED MODE
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
            
            val baseGrouped = (appItems + folderItems).groupBy({ it.first as AppCategory? }, { it.second })
            
            // Map custom entities to AppCategory instances with correct icons and labels
            val dbCategoryOverrides = customCatEntities.associate { 
                it.name to AppCategory(it.name, isCustom = true, iconName = it.iconName, label = it.label)
            }

            val finalGrouped = mutableMapOf<AppCategory?, List<DrawerItem>>()
            
            // 1. Populate and Apply Overrides (Built-ins and Custom)
            baseGrouped.forEach { (cat, items) ->
                if (cat != null) {
                    val override = dbCategoryOverrides[cat.name]
                    val finalCat = if (override != null) {
                        // Use override for custom categories OR for built-in renaming
                        cat.copy(iconName = override.iconName ?: cat.iconName, label = override.label ?: cat.label)
                    } else cat
                    finalGrouped[finalCat] = items
                } else {
                    finalGrouped[null] = items
                }
            }

            // 2. Add remaining DB custom categories (empty ones)
            dbCategoryOverrides.forEach { (name, dbCat) ->
                if (!finalGrouped.keys.any { it?.name == name }) {
                    finalGrouped[dbCat] = emptyList()
                }
            }

            // 3. Add enabled built-in categories that are empty
            AppCategory.builtInValues.forEach { builtIn ->
                if (builtIn.name in prefs.orderedCategories && !finalGrouped.keys.any { it?.name == builtIn.name }) {
                    val override = dbCategoryOverrides[builtIn.name]
                    val finalBuiltIn = if (override != null) {
                        builtIn.copy(iconName = override.iconName ?: builtIn.iconName, label = override.label ?: builtIn.label)
                    } else builtIn
                    finalGrouped[finalBuiltIn] = emptyList()
                }
            }
            
            // 4. Handle HIDDEN category explicitly based on prefs
            if (prefs.showHiddenApps && !finalGrouped.containsKey(AppCategory.HIDDEN)) {
                finalGrouped[AppCategory.HIDDEN] = emptyList()
            } else if (!prefs.showHiddenApps) {
                finalGrouped.remove(AppCategory.HIDDEN)
            }

            // 5. SORT based on prefs.orderedCategories
            val sortedKeys = finalGrouped.keys.sortedWith(
                compareBy<AppCategory?> { it?.name == AppCategory.OTHER.name } // OTHER always last
                    .thenBy { cat ->
                        val index = prefs.orderedCategories.indexOf(cat?.name)
                        if (index == -1) Int.MAX_VALUE else index
                    }
                    .thenBy { it?.name ?: "" }
            )
            
            sortedKeys.map { key ->
                key to (finalGrouped[key] ?: emptyList())
            }
        }
        result
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categoryNotifications: StateFlow<Map<AppCategory?, Pair<Boolean, Int>>> = combine(
        categorizedApps,
        activeNotifications
    ) { categorizedList, notifications ->
        categorizedList.associate { (category, items) ->
            val result = if (category == null) {
                notifications.isNotEmpty() to notifications.values.sum()
            } else {
                val appPackages = items.flatMap { item ->
                    when (item) {
                        is DrawerItem.App -> listOf(item.appModel.packageName)
                        is DrawerItem.Folder -> item.apps.map { it.packageName }
                    }
                }.toSet()
                val hasNotif = appPackages.any { it in notifications.keys }
                val count = notifications.filter { it.key in appPackages }.values.sum()
                hasNotif to count
            }
            category to result
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val filteredApps: StateFlow<List<AppModel>> = combine(
        allApps,
        _searchQuery,
        preferences.distinctUntilChanged()
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

    val gridItems: StateFlow<List<Pair<AppCategory?, List<DrawerItem?>>>> = combine(
        categorizedApps,
        _drawerColumns,
        preferences
    ) { categorizedList, columns, prefs ->
        val sortingMode = prefs.sortingMode
        val isReverse = prefs.isSortReverse
        val verticalAnchor = prefs.verticalAnchor
        val horizontalAnchor = prefs.horizontalAnchor

        val baseComparator = when (sortingMode) {
            com.samidevstudio.neoglide.data.repository.SortingMode.ALPHABETICAL -> compareBy<AppModel> { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.INSTALL_TIME -> compareByDescending<AppModel> { it.installTime }.thenBy { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.LAST_USED -> compareByDescending<AppModel> { it.lastUsedTime }.thenBy { it.label.lowercase() }
            com.samidevstudio.neoglide.data.repository.SortingMode.ICON_COLOR -> compareBy<AppModel> { it.dominantHue }.thenBy { it.label.lowercase() }
        }
        val finalComparator = if (isReverse) baseComparator.reversed() else baseComparator

        categorizedList.map { (cat, items) ->
            if (items.isEmpty()) return@map cat to emptyList<DrawerItem?>()

            val folders = items.filterIsInstance<DrawerItem.Folder>().sortedBy { it.label }
            val apps = items.filterIsInstance<DrawerItem.App>()
                .map { it.appModel }
                .sortedWith(finalComparator)
                .map { DrawerItem.App(it) }

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
            cat to result
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        combine(_searchQuery, preferences) { query, prefs ->
            query to prefs.searchProvider
        }
        .debounce(300)
        .filter { (query, provider) -> query.isNotBlank() && provider != com.samidevstudio.neoglide.data.repository.SearchProvider.LOCAL_ONLY }
        .onEach { (query, _) ->
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
        createFolderFromList(listOf(appA.packageName, appB.packageName), "Folder", category)
    }

    fun createFolderFromList(packageNames: List<String>, label: String, category: AppCategory?) {
        viewModelScope.launch {
            if (packageNames.size < 2) return@launch
            val prefs = preferences.first()
            val folderCategory = if (prefs.categoryBarType == CategoryBarType.NONE) "CATEGORYLESS" else category?.name ?: "OTHER"
            
            packageNames.forEach { homeRepository.cleanupDrawerMembership(it) }
            
            val apps = packageNames.map { pkg ->
                HomeAppEntity(id = 0, packageName = pkg, row = 0f, column = 0f)
            }
            
            val newFolderId = homeRepository.createFolderWithApps(
                apps = apps,
                label = label,
                category = folderCategory
            )
            packageNames.forEach { appRepository.markAppAsInFolder(it) }
            
            if (newFolderId != -1) {
                _uiEvent.emit(DrawerUiEvent.FolderCreated(newFolderId))
            }
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

    suspend fun checkCategoryCapacity(context: Context): Boolean {
        val prefs = preferences.first()
        val count = categorizedApps.value.size + 1
        return canFitCategories(context, prefs.categoryBarType, count)
    }

    suspend fun canSwitchToCategoryBarType(context: Context, newType: CategoryBarType): Boolean {
        val count = categorizedApps.value.size
        return canFitCategories(context, newType, count)
    }

    private fun canFitCategories(context: Context, type: CategoryBarType, count: Int): Boolean {
        if (type == CategoryBarType.NONE) return true
        
        val displayMetrics = context.resources.displayMetrics
        val isVertical = type == CategoryBarType.LEFT || type == CategoryBarType.RIGHT
        
        val totalAvailable = if (isVertical) {
            (displayMetrics.heightPixels / displayMetrics.density) * 0.9f // 90% of height
        } else {
            (displayMetrics.widthPixels / displayMetrics.density) - 32 // full width minus padding
        }

        if (count <= 0) return true
        
        // Match shrinking logic in CategorySelector:
        // Min icon size: 24dp, Min spacing: 2dp
        val minRequired = (count * 24) + ((count - 1) * 2)
        return minRequired <= totalAvailable
    }

    fun addBuiltInCategory(name: String, packageNames: List<String>) {
        viewModelScope.launch {
            userPreferencesRepository.toggleCategoryEnabled(name)
            _selectedCategory.value = AppCategory.fromString(name)
            appRepository.moveAppsToCategory(packageNames, name)
            showToast("Category added with ${packageNames.size} apps.")
        }
    }

    fun addCustomCategory(name: String, iconName: String?, packageNames: List<String>) {
        viewModelScope.launch {
            categoryRepository.addCategory(name, iconName)
            userPreferencesRepository.toggleCategoryEnabled(name)
            _selectedCategory.value = AppCategory(name, isCustom = true, iconName = iconName)
            appRepository.moveAppsToCategory(packageNames, name)
            showToast("Category added with ${packageNames.size} apps.")
        }
    }

    fun removeCustomCategory(category: AppCategory) {
        viewModelScope.launch {
            if (category.isCustom) {
                categoryRepository.removeCategory(category.name)
                userPreferencesRepository.removeCategoryFromOrder(category.name)
            } else {
                userPreferencesRepository.toggleCategoryEnabled(category.name)
            }
            appRepository.reclassifyAppsFromDeletedCategory(category.name)
            showToast("Category removed. Apps redistributed.")
        }
    }

    suspend fun getRecommendedApps(category: AppCategory): List<String> {
        return appRepository.getRecommendedAppsForCategory(category)
    }
}
