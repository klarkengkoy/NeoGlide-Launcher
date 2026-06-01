package com.samidevstudio.pxllauncherneo.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class CategoryBarType {
    LEFT, RIGHT, BOTTOM, NONE
}

enum class SortingMode {
    ALPHABETICAL, INSTALL_TIME, LAST_USED, ICON_COLOR
}

enum class GridSize {
    GRID_4X5, GRID_5X5, GRID_6X6
}

enum class SearchProvider {
    GOOGLE, DUCKDUCKGO, LOCAL_ONLY
}

enum class NotificationDotMode {
    APP_ICON, CATEGORY_BAR, BOTH, NONE
}

data class UserPreferences(
    val categoryBarType: CategoryBarType,
    val useMonochromeIcons: Boolean = false,
    val hiddenPackages: Set<String> = emptySet(),
    val showHiddenApps: Boolean = false,
    val lastDefaultPromptTime: Long = 0L,
    val sortingMode: SortingMode = SortingMode.ALPHABETICAL,
    val gridSize: GridSize = GridSize.GRID_5X5,
    val showIconLabels: Boolean = true,
    val searchProvider: SearchProvider = SearchProvider.GOOGLE,
    val notificationDotMode: NotificationDotMode = NotificationDotMode.BOTH,
    val lockLayout: Boolean = false,
    val doubleTapToSleep: Boolean = false,
    val swipeDownForNotifications: Boolean = true,
    val isBottomAnchored: Boolean = true,
    val iconPackPackageName: String? = null
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
        val SHOW_ICON_LABELS = booleanPreferencesKey("show_icon_labels")
        val SEARCH_PROVIDER = stringPreferencesKey("search_provider")
        val NOTIFICATION_DOT_MODE = stringPreferencesKey("notification_dot_mode")
        val LOCK_LAYOUT = booleanPreferencesKey("lock_layout")
        val DOUBLE_TAP_TO_SLEEP = booleanPreferencesKey("double_tap_to_sleep")
        val SWIPE_DOWN_FOR_NOTIFICATIONS = booleanPreferencesKey("swipe_down_for_notifications")
        val IS_BOTTOM_ANCHORED = booleanPreferencesKey("is_bottom_anchored")
        val ICON_PACK_PACKAGE_NAME = stringPreferencesKey("icon_pack_package_name")
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
            val categoryBarTypeStr = preferences[PreferencesKeys.CATEGORY_BAR_TYPE] ?: CategoryBarType.RIGHT.name
            val categoryBarType = try { CategoryBarType.valueOf(categoryBarTypeStr) } catch (_: Exception) { CategoryBarType.RIGHT }
            
            val sortingModeStr = preferences[PreferencesKeys.SORTING_MODE] ?: SortingMode.ALPHABETICAL.name
            val sortingMode = try { SortingMode.valueOf(sortingModeStr) } catch (_: Exception) { SortingMode.ALPHABETICAL }

            val gridSizeStr = preferences[PreferencesKeys.GRID_SIZE] ?: GridSize.GRID_5X5.name
            val gridSize = try { GridSize.valueOf(gridSizeStr) } catch (_: Exception) { GridSize.GRID_5X5 }

            val searchProviderStr = preferences[PreferencesKeys.SEARCH_PROVIDER] ?: SearchProvider.GOOGLE.name
            val searchProvider = try { SearchProvider.valueOf(searchProviderStr) } catch (_: Exception) { SearchProvider.GOOGLE }

            val dotModeStr = preferences[PreferencesKeys.NOTIFICATION_DOT_MODE] ?: NotificationDotMode.BOTH.name
            val dotMode = try { NotificationDotMode.valueOf(dotModeStr) } catch (_: Exception) { NotificationDotMode.BOTH }

            UserPreferences(
                categoryBarType = categoryBarType,
                useMonochromeIcons = preferences[PreferencesKeys.USE_MONOCHROME_ICONS] ?: false,
                hiddenPackages = preferences[PreferencesKeys.HIDDEN_PACKAGES] ?: emptySet(),
                showHiddenApps = preferences[PreferencesKeys.SHOW_HIDDEN_APPS] ?: false,
                lastDefaultPromptTime = preferences[PreferencesKeys.LAST_DEFAULT_PROMPT_TIME] ?: 0L,
                sortingMode = sortingMode,
                gridSize = gridSize,
                showIconLabels = preferences[PreferencesKeys.SHOW_ICON_LABELS] ?: true,
                searchProvider = searchProvider,
                notificationDotMode = dotMode,
                lockLayout = preferences[PreferencesKeys.LOCK_LAYOUT] ?: false,
                doubleTapToSleep = preferences[PreferencesKeys.DOUBLE_TAP_TO_SLEEP] ?: false,
                swipeDownForNotifications = preferences[PreferencesKeys.SWIPE_DOWN_FOR_NOTIFICATIONS] ?: true,
                isBottomAnchored = preferences[PreferencesKeys.IS_BOTTOM_ANCHORED] ?: true,
                iconPackPackageName = preferences[PreferencesKeys.ICON_PACK_PACKAGE_NAME]
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

    suspend fun updateShowIconLabels(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SHOW_ICON_LABELS] = show }
    }

    suspend fun updateSearchProvider(provider: SearchProvider) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SEARCH_PROVIDER] = provider.name }
    }

    suspend fun updateNotificationDotMode(mode: NotificationDotMode) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.NOTIFICATION_DOT_MODE] = mode.name }
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

    suspend fun updateDoubleTapToSleep(enable: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.DOUBLE_TAP_TO_SLEEP] = enable }
    }

    suspend fun updateSwipeDownForNotifications(enable: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.SWIPE_DOWN_FOR_NOTIFICATIONS] = enable }
    }

    suspend fun updateIsBottomAnchored(anchored: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_BOTTOM_ANCHORED] = anchored }
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
}
