package com.samidevstudio.neoglide.ui.settings

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.neoglide.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val appRepository: AppRepository,
    private val homeRepository: HomeRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    val allApps: StateFlow<List<com.samidevstudio.neoglide.domain.model.AppModel>> = appRepository.allApps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(categoryBarType = CategoryBarType.BOTTOM)
        )

    private val _isNotificationServiceEnabled = MutableStateFlow(value = false)
    val isNotificationServiceEnabled = _isNotificationServiceEnabled.asStateFlow()

    private val _isUserAuthenticatedForHiddenApps = MutableStateFlow(false)
    val isUserAuthenticatedForHiddenApps = _isUserAuthenticatedForHiddenApps.asStateFlow()

    fun setUserAuthenticatedForHiddenApps(authenticated: Boolean) {
        _isUserAuthenticatedForHiddenApps.value = authenticated
    }

    fun launchApp(packageName: String) {
        appRepository.launchApp(packageName)
    }

    fun checkNotificationPermission() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        _isNotificationServiceEnabled.value = enabledPackages.contains(context.packageName)
    }

    fun setCategoryBarType(type: CategoryBarType) {
        viewModelScope.launch { preferencesRepository.updateCategoryBarType(type) }
    }

    fun setSortingMode(mode: SortingMode) {
        viewModelScope.launch { preferencesRepository.updateSortingMode(mode) }
    }

    fun setGridSize(size: GridSize) {
        viewModelScope.launch { preferencesRepository.updateGridSize(size) }
    }

    fun setAppLabelMode(mode: AppLabelMode) {
        viewModelScope.launch { preferencesRepository.updateAppLabelMode(mode) }
    }

    fun setSearchProvider(provider: SearchProvider) {
        viewModelScope.launch { preferencesRepository.updateSearchProvider(provider) }
    }

    fun setNotificationDotMode(mode: NotificationDotMode) {
        viewModelScope.launch { preferencesRepository.updateNotificationDotMode(mode) }
    }

    fun setUseMonochromeIcons(useMonochrome: Boolean) {
        viewModelScope.launch { preferencesRepository.updateUseMonochromeIcons(useMonochrome) }
    }

    fun setShowHiddenApps(show: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowHiddenApps(show) }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { 
            preferencesRepository.hideApp(packageName)
            homeRepository.removeHomeApp(packageName)
            homeRepository.removeAppFromFolders(packageName)
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { preferencesRepository.unhideApp(packageName) }
    }

    fun updateLastDefaultPromptTime(time: Long) {
        viewModelScope.launch { preferencesRepository.updateLastDefaultPromptTime(time) }
    }

    fun setLockLayout(lock: Boolean) {
        viewModelScope.launch { preferencesRepository.updateLockLayout(lock) }
    }

    fun setDoubleTapToSleep(enable: Boolean) {
        viewModelScope.launch { preferencesRepository.updateDoubleTapToSleep(enable) }
    }

    fun setSwipeDownForNotifications(enable: Boolean) {
        viewModelScope.launch { preferencesRepository.updateSwipeDownForNotifications(enable) }
    }

    fun setVerticalAnchor(anchor: VerticalAnchor) {
        viewModelScope.launch { preferencesRepository.updateVerticalAnchor(anchor) }
    }

    fun setHorizontalAnchor(anchor: HorizontalAnchor) {
        viewModelScope.launch { preferencesRepository.updateHorizontalAnchor(anchor) }
    }

    fun setIconPack(packageName: String?) {
        viewModelScope.launch { preferencesRepository.updateIconPack(packageName) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.updateHapticsEnabled(enabled) }
    }

    fun setIsSortReverse(reverse: Boolean) {
        viewModelScope.launch { preferencesRepository.updateIsSortReverse(reverse) }
    }

    fun setIsPremium(isPremium: Boolean) {
        viewModelScope.launch { preferencesRepository.updateIsPremium(isPremium) }
    }

    fun refreshAppIcons() {
        viewModelScope.launch {
            appRepository.refreshApps(forceRecalculateColors = true)
        }
    }

    fun resetHomeScreen() {
        viewModelScope.launch {
            homeRepository.resetHome()
        }
    }

    fun resetAppDrawer() {
        viewModelScope.launch {
            appRepository.resetDrawer()
        }
    }

    fun deleteHomeFolders() {
        viewModelScope.launch {
            homeRepository.deleteHomeFolders()
        }
    }

    fun deleteAppDrawerFolders() {
        viewModelScope.launch {
            appRepository.deleteDrawerFolders()
        }
    }

    fun openAppInfo() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openWallpaperSettings() {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback for some devices
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openDefaultLauncherSettings() {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openAppearanceSettings() {
        try {
            // Android 12+ Wallpaper & Style intent
            val intent = android.content.Intent("android.service.wallpaper.WallpaperService").apply {
                component = android.content.ComponentName("com.google.android.apps.wallpaper", "com.google.android.apps.wallpaper.picker.CategoryPickerActivity")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to general wallpaper settings
            openWallpaperSettings()
        }
    }

    fun resetLayout() {
        // Deprecated by granular reset methods
    }
}
