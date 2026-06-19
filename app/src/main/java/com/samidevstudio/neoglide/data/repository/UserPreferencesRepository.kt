package com.samidevstudio.neoglide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "neo_user_prefs_v1")

enum class CategoryBarType {
    LEFT, RIGHT, BOTTOM, NONE
}

enum class SortingMode {
    ALPHABETICAL, INSTALL_TIME, LAST_USED, ICON_COLOR
}

enum class GridSize {
    SMALL, MEDIUM, LARGE;

    val targetCellWidthDp: Int
        get() = when (this) {
            SMALL -> 66
            MEDIUM -> 80
            LARGE -> 95
        }

    val iconSizeDp: Int
        get() = when (this) {
            SMALL -> 48
            MEDIUM -> 56
            LARGE -> 64
        }

    val fontSizeSp: Int
        get() = when (this) {
            SMALL -> 11
            MEDIUM -> 13
            LARGE -> 15
        }

    fun getColumnCount(availableWidthDp: Float): Int {
        return (availableWidthDp / targetCellWidthDp).toInt().coerceAtLeast(3)
    }
}

enum class SearchProvider(val searchUrl: String, val displayName: String) {
    GOOGLE("https://www.google.com/search?q=", "Google"),
    DUCKDUCKGO("https://duckduckgo.com/?q=", "DuckDuckGo"),
    BRAVE("https://search.brave.com/search?q=", "Brave"),
    ECOSIA("https://www.ecosia.org/search?q=", "Ecosia"),
    LOCAL_ONLY("", "Local Only")
}

enum class BadgeStyle {
    NONE, DOT, COUNT
}

enum class AppLabelMode {
    HOME_ONLY, DRAWER_ONLY, BOTH, NONE
}

enum class VerticalAnchor {
    BOTTOM, TOP
}

enum class HorizontalAnchor {
    LEFT, RIGHT
}

data class UserPreferences(
    val categoryBarType: CategoryBarType = CategoryBarType.BOTTOM,
    val useMonochromeIcons: Boolean = false,
    val hiddenPackages: Set<String> = emptySet(),
    val showHiddenApps: Boolean = false,
    val lastDefaultPromptTime: Long = 0L,
    val sortingMode: SortingMode = SortingMode.ALPHABETICAL,
    val gridSize: GridSize = GridSize.MEDIUM,
    val appLabelMode: AppLabelMode = AppLabelMode.BOTH,
    val searchProvider: SearchProvider = SearchProvider.GOOGLE,
    val homeBadgeStyle: BadgeStyle = BadgeStyle.COUNT,
    val drawerBadgeStyle: BadgeStyle = BadgeStyle.COUNT,
    val railBadgeStyle: BadgeStyle = BadgeStyle.COUNT,
    val lockLayout: Boolean = false,
    val swipeDownForNotifications: Boolean = true,
    val verticalAnchor: VerticalAnchor = VerticalAnchor.TOP,
    val horizontalAnchor: HorizontalAnchor = HorizontalAnchor.LEFT,
    val iconPackPackageName: String? = null,
    val hapticsEnabled: Boolean = true,
    val isSortReverse: Boolean = false,
    val isPremium: Boolean = false,
    val isFirstInstallRun: Boolean = true,
    val orderedCategories: List<String> = emptyList()
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val CATEGORY_BAR_TYPE = stringPreferencesKey("category_bar_type")
        val USE_MONOCHROME_ICONS = booleanPreferencesKey("use_monochrome_icons")
        val HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
        val SHOW_HIDDEN_APPS = booleanPreferencesKey("show_hidden_apps")
        val LAST_DEFAULT_PROMPT_TIME = longPreferencesKey("last_default_prompt_time")
        val SORTING_MODE = stringPreferencesKey("sorting_mode")
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val APP_LABEL_MODE = stringPreferencesKey("app_label_mode")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val NOTIFICATION_DOT_MODE = stringPreferencesKey("notification_dot_mode")
        val HOME_BADGE_STYLE = stringPreferencesKey("home_badge_style")
        val DRAWER_BADGE_STYLE = stringPreferencesKey("drawer_badge_style")
        val RAIL_BADGE_STYLE = stringPreferencesKey("rail_badge_style")
        val LOCK_LAYOUT = booleanPreferencesKey("lock_layout")
        val SWIPE_DOWN_FOR_NOTIFICATIONS = booleanPreferencesKey("swipe_down_for_notifications")
        val VERTICAL_ANCHOR = stringPreferencesKey("vertical_anchor")
        val HORIZONTAL_ANCHOR = stringPreferencesKey("horizontal_anchor")
        val ICON_PACK_PACKAGE_NAME = stringPreferencesKey("icon_pack_package_name")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val IS_SORT_REVERSE = booleanPreferencesKey("is_sort_reverse")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val IS_FIRST_INSTALL_RUN = booleanPreferencesKey("is_first_install_run")
        val ORDERED_CATEGORIES = stringPreferencesKey("ordered_categories")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val categoryBarTypeStr = preferences[PreferencesKeys.CATEGORY_BAR_TYPE] ?: CategoryBarType.BOTTOM.name
            val categoryBarType = try { CategoryBarType.valueOf(categoryBarTypeStr) } catch (_: Exception) { CategoryBarType.BOTTOM }
            
            val sortingModeStr = preferences[PreferencesKeys.SORTING_MODE] ?: SortingMode.ALPHABETICAL.name
            val sortingMode = try { SortingMode.valueOf(sortingModeStr) } catch (_: Exception) { SortingMode.ALPHABETICAL }

            val gridSizeStr = preferences[PreferencesKeys.GRID_SIZE] ?: GridSize.MEDIUM.name
            val gridSize = when (gridSizeStr) {
                "GRID_4X5" -> GridSize.LARGE
                "GRID_5X5" -> GridSize.MEDIUM
                "GRID_6X6" -> GridSize.SMALL
                else -> try { GridSize.valueOf(gridSizeStr) } catch (_: Exception) { GridSize.MEDIUM }
            }

            val appLabelModeStr = preferences[PreferencesKeys.APP_LABEL_MODE] ?: AppLabelMode.BOTH.name
            val appLabelMode = try { AppLabelMode.valueOf(appLabelModeStr) } catch (_: Exception) { AppLabelMode.BOTH }

            val searchProviderStr = preferences[PreferencesKeys.SEARCH_PROVIDER] ?: SearchProvider.GOOGLE.name
            val searchProvider = try { SearchProvider.valueOf(searchProviderStr) } catch (_: Exception) { SearchProvider.GOOGLE }

            val legacyDotModeStr = preferences[PreferencesKeys.NOTIFICATION_DOT_MODE] ?: "BOTH"

            val homeBadgeStyleStr = preferences[PreferencesKeys.HOME_BADGE_STYLE]
            val homeBadgeStyle = if (homeBadgeStyleStr == null) {
                if (legacyDotModeStr == "APP_ICON" || legacyDotModeStr == "BOTH") BadgeStyle.COUNT else BadgeStyle.NONE
            } else {
                try { BadgeStyle.valueOf(homeBadgeStyleStr) } catch (_: Exception) { BadgeStyle.COUNT }
            }

            val drawerBadgeStyleStr = preferences[PreferencesKeys.DRAWER_BADGE_STYLE]
            val drawerBadgeStyle = if (drawerBadgeStyleStr == null) {
                if (legacyDotModeStr == "APP_ICON" || legacyDotModeStr == "BOTH") BadgeStyle.COUNT else BadgeStyle.NONE
            } else {
                try { BadgeStyle.valueOf(drawerBadgeStyleStr) } catch (_: Exception) { BadgeStyle.COUNT }
            }

            val railBadgeStyleStr = preferences[PreferencesKeys.RAIL_BADGE_STYLE]
            val railBadgeStyle = if (railBadgeStyleStr == null) {
                if (legacyDotModeStr == "CATEGORY_BAR" || legacyDotModeStr == "BOTH") BadgeStyle.COUNT else BadgeStyle.NONE
            } else {
                try { BadgeStyle.valueOf(railBadgeStyleStr) } catch (_: Exception) { BadgeStyle.COUNT }
            }

            val verticalAnchorStr = preferences[PreferencesKeys.VERTICAL_ANCHOR] ?: VerticalAnchor.TOP.name
            val verticalAnchor = try { VerticalAnchor.valueOf(verticalAnchorStr) } catch (_: Exception) { VerticalAnchor.TOP }

            val horizontalAnchorStr = preferences[PreferencesKeys.HORIZONTAL_ANCHOR] ?: HorizontalAnchor.LEFT.name
            val horizontalAnchor = try { HorizontalAnchor.valueOf(horizontalAnchorStr) } catch (_: Exception) { HorizontalAnchor.LEFT }

            val orderedCategoriesStr = preferences[PreferencesKeys.ORDERED_CATEGORIES]
            val orderedCategories = orderedCategoriesStr?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()

            UserPreferences(
                categoryBarType = categoryBarType,
                useMonochromeIcons = preferences[PreferencesKeys.USE_MONOCHROME_ICONS] ?: false,
                hiddenPackages = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet(),
                showHiddenApps = preferences[PreferencesKeys.SHOW_HIDDEN_APPS] ?: false,
                lastDefaultPromptTime = preferences[PreferencesKeys.LAST_DEFAULT_PROMPT_TIME] ?: 0L,
                sortingMode = sortingMode,
                gridSize = gridSize,
                appLabelMode = appLabelMode,
                searchProvider = searchProvider,
                homeBadgeStyle = homeBadgeStyle,
                drawerBadgeStyle = drawerBadgeStyle,
                railBadgeStyle = railBadgeStyle,
                lockLayout = preferences[PreferencesKeys.LOCK_LAYOUT] ?: false,
                swipeDownForNotifications = preferences[PreferencesKeys.SWIPE_DOWN_FOR_NOTIFICATIONS] ?: true,
                verticalAnchor = verticalAnchor,
                horizontalAnchor = horizontalAnchor,
                iconPackPackageName = preferences[PreferencesKeys.ICON_PACK_PACKAGE_NAME],
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                isSortReverse = preferences[PreferencesKeys.IS_SORT_REVERSE] ?: false,
                isPremium = preferences[PreferencesKeys.IS_PREMIUM] ?: false,
                isFirstInstallRun = preferences[PreferencesKeys.IS_FIRST_INSTALL_RUN] ?: true,
                orderedCategories = orderedCategories
            )
        }

    suspend fun updateCategoryBarType(type: CategoryBarType) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.CATEGORY_BAR_TYPE] = type.name }
    }

    suspend fun updateSortingMode(mode: SortingMode) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SORTING_MODE] = mode.name }
    }

    suspend fun updateGridSize(size: GridSize) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.GRID_SIZE] = size.name }
    }

    suspend fun updateAppLabelMode(mode: AppLabelMode) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.APP_LABEL_MODE] = mode.name }
    }

    suspend fun updateSearchProvider(provider: SearchProvider) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SEARCH_PROVIDER] = provider.name }
    }

    suspend fun updateHomeBadgeStyle(style: BadgeStyle) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.HOME_BADGE_STYLE] = style.name }
    }

    suspend fun updateDrawerBadgeStyle(style: BadgeStyle) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DRAWER_BADGE_STYLE] = style.name }
    }

    suspend fun updateRailBadgeStyle(style: BadgeStyle) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.RAIL_BADGE_STYLE] = style.name }
    }

    suspend fun updateUseMonochromeIcons(useMonochrome: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USE_MONOCHROME_ICONS] = useMonochrome }
    }

    suspend fun hideApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet()
            preferences[PreferencesKeys.HIDDEN_PACKAGES] = current + packageName
        }
    }

    suspend fun unhideApp(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet()
            preferences[PreferencesKeys.HIDDEN_PACKAGES] = current - packageName
        }
    }

    suspend fun setShowHiddenApps(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_HIDDEN_APPS] = show }
    }

    suspend fun updateLastDefaultPromptTime(time: Long) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.LAST_DEFAULT_PROMPT_TIME] = time }
    }

    suspend fun updateLockLayout(lock: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.LOCK_LAYOUT] = lock }
    }

    suspend fun updateSwipeDownForNotifications(enable: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SWIPE_DOWN_FOR_NOTIFICATIONS] = enable }
    }

    suspend fun updateVerticalAnchor(anchor: VerticalAnchor) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.VERTICAL_ANCHOR] = anchor.name }
    }

    suspend fun updateHorizontalAnchor(anchor: HorizontalAnchor) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.HORIZONTAL_ANCHOR] = anchor.name }
    }

    suspend fun updateIconPack(packageName: String?) {
        context.dataStore.edit { preferences -> 
            if (packageName == null) {
                preferences.remove(PreferencesKeys.ICON_PACK_PACKAGE_NAME)
            } else {
                preferences[PreferencesKeys.ICON_PACK_PACKAGE_NAME] = packageName
            }
        }
    }

    suspend fun updateHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateIsSortReverse(reverse: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_SORT_REVERSE] = reverse }
    }

    suspend fun updateIsPremium(isPremium: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_PREMIUM] = isPremium }
    }

    suspend fun setFirstInstallRun(isFirst: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_FIRST_INSTALL_RUN] = isFirst }
    }

    suspend fun updateCategoryOrder(categories: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ORDERED_CATEGORIES] = categories.joinToString("|")
        }
    }

    suspend fun updateCategoryNameInOrder(oldName: String, newName: String) {
        context.dataStore.edit { preferences ->
            val orderedStr = preferences[PreferencesKeys.ORDERED_CATEGORIES] ?: ""
            val current = orderedStr.split("|").filter { it.isNotEmpty() }.toMutableList()
            val index = current.indexOf(oldName)
            if (index != -1) {
                current[index] = newName
                preferences[PreferencesKeys.ORDERED_CATEGORIES] = current.joinToString("|")
            }
        }
    }

    suspend fun toggleCategoryEnabled(categoryName: String) {
        context.dataStore.edit { preferences ->
            val orderedStr = preferences[PreferencesKeys.ORDERED_CATEGORIES] ?: ""
            val current = orderedStr.split("|").filter { it.isNotEmpty() }.toMutableList()
            
            if (categoryName in current) {
                current.remove(categoryName)
            } else {
                current.add(categoryName)
            }
            preferences[PreferencesKeys.ORDERED_CATEGORIES] = current.joinToString("|")
        }
    }

    suspend fun removeCategoryFromOrder(categoryName: String) {
        context.dataStore.edit { preferences ->
            val orderedStr = preferences[PreferencesKeys.ORDERED_CATEGORIES] ?: ""
            val current = orderedStr.split("|").filter { it.isNotEmpty() }.toMutableList()
            if (current.remove(categoryName)) {
                preferences[PreferencesKeys.ORDERED_CATEGORIES] = current.joinToString("|")
            }
        }
    }
}
