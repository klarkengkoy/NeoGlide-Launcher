package com.samidevstudio.neoglide.data.repository

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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "neo_user_prefs_v1")

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
    val gridSize: GridSize = GridSize.GRID_5X5,
    val appLabelMode: AppLabelMode = AppLabelMode.BOTH,
    val searchProvider: SearchProvider = SearchProvider.GOOGLE,
    val notificationDotMode: NotificationDotMode = NotificationDotMode.BOTH,
    val lockLayout: Boolean = false,
    val doubleTapToSleep: Boolean = false,
    val swipeDownForNotifications: Boolean = true,
    val verticalAnchor: VerticalAnchor = VerticalAnchor.TOP,
    val horizontalAnchor: HorizontalAnchor = HorizontalAnchor.LEFT,
    val iconPackPackageName: String? = null,
    val hapticsEnabled: Boolean = true,
    val isSortReverse: Boolean = false,
    val isFirstInstallRun: Boolean = true
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
        val LOCK_LAYOUT = booleanPreferencesKey("lock_layout")
        val DOUBLE_TAP_TO_SLEEP = booleanPreferencesKey("double_tap_to_sleep")
        val SWIPE_DOWN_FOR_NOTIFICATIONS = booleanPreferencesKey("swipe_down_for_notifications")
        val VERTICAL_ANCHOR = stringPreferencesKey("vertical_anchor")
        val HORIZONTAL_ANCHOR = stringPreferencesKey("horizontal_anchor")
        val ICON_PACK_PACKAGE_NAME = stringPreferencesKey("icon_pack_package_name")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val IS_SORT_REVERSE = booleanPreferencesKey("is_sort_reverse")
        val IS_FIRST_INSTALL_RUN = booleanPreferencesKey("is_first_install_run")
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

            val gridSizeStr = preferences[PreferencesKeys.GRID_SIZE] ?: GridSize.GRID_5X5.name
            val gridSize = try { GridSize.valueOf(gridSizeStr) } catch (_: Exception) { GridSize.GRID_5X5 }

            val appLabelModeStr = preferences[PreferencesKeys.APP_LABEL_MODE] ?: AppLabelMode.BOTH.name
            val appLabelMode = try { AppLabelMode.valueOf(appLabelModeStr) } catch (_: Exception) { AppLabelMode.BOTH }

            val searchProviderStr = preferences[PreferencesKeys.SEARCH_PROVIDER] ?: SearchProvider.GOOGLE.name
            val searchProvider = try { SearchProvider.valueOf(searchProviderStr) } catch (_: Exception) { SearchProvider.GOOGLE }

            val dotModeStr = preferences[PreferencesKeys.NOTIFICATION_DOT_MODE] ?: NotificationDotMode.BOTH.name
            val dotMode = try { NotificationDotMode.valueOf(dotModeStr) } catch (_: Exception) { NotificationDotMode.BOTH }

            val verticalAnchorStr = preferences[PreferencesKeys.VERTICAL_ANCHOR] ?: VerticalAnchor.TOP.name
            val verticalAnchor = try { VerticalAnchor.valueOf(verticalAnchorStr) } catch (_: Exception) { VerticalAnchor.TOP }

            val horizontalAnchorStr = preferences[PreferencesKeys.HORIZONTAL_ANCHOR] ?: HorizontalAnchor.LEFT.name
            val horizontalAnchor = try { HorizontalAnchor.valueOf(horizontalAnchorStr) } catch (_: Exception) { HorizontalAnchor.LEFT }

            android.util.Log.d("NeoGlidePrefs", "Loaded Prefs (v1): categoryBar=$categoryBarType, vAnchor=$verticalAnchor, hAnchor=$horizontalAnchor")

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
                notificationDotMode = dotMode,
                lockLayout = preferences[PreferencesKeys.LOCK_LAYOUT] ?: false,
                doubleTapToSleep = preferences[PreferencesKeys.DOUBLE_TAP_TO_SLEEP] ?: false,
                swipeDownForNotifications = preferences[PreferencesKeys.SWIPE_DOWN_FOR_NOTIFICATIONS] ?: true,
                verticalAnchor = verticalAnchor,
                horizontalAnchor = horizontalAnchor,
                iconPackPackageName = preferences[PreferencesKeys.ICON_PACK_PACKAGE_NAME],
                hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
                isSortReverse = preferences[PreferencesKeys.IS_SORT_REVERSE] ?: false,
                isFirstInstallRun = preferences[PreferencesKeys.IS_FIRST_INSTALL_RUN] ?: true
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

    suspend fun setFirstInstallRun(isFirst: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_FIRST_INSTALL_RUN] = isFirst }
    }
}
