package com.samidevstudio.pxllauncherneo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samidevstudio.pxllauncherneo.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(categoryBarType = CategoryBarType.RIGHT)
        )

    fun setCategoryBarType(type: CategoryBarType) {
        viewModelScope.launch { preferencesRepository.updateCategoryBarType(type) }
    }

    fun setSortingMode(mode: SortingMode) {
        viewModelScope.launch { preferencesRepository.updateSortingMode(mode) }
    }

    fun setGridSize(size: GridSize) {
        viewModelScope.launch { preferencesRepository.updateGridSize(size) }
    }

    fun setShowIconLabels(show: Boolean) {
        viewModelScope.launch { preferencesRepository.updateShowIconLabels(show) }
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
        viewModelScope.launch { preferencesRepository.hideApp(packageName) }
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

    fun setIconPack(packageName: String?) {
        viewModelScope.launch { preferencesRepository.updateIconPack(packageName) }
    }

    fun clearIconCache() { }

    fun resetLayout() { }
}
