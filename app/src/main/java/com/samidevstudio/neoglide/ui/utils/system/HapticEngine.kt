package com.samidevstudio.neoglide.ui.utils.system

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import com.samidevstudio.neoglide.data.repository.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

val LocalHapticEngine = staticCompositionLocalOf<HapticEngine?> { null }

@Composable
fun rememberHapticFeedback(preferences: UserPreferences): (HapticEngine.HapticType) -> Unit {
    val hapticEngine = LocalHapticEngine.current ?: return {}
    val view = LocalView.current
    return { type ->
        if (preferences.hapticsEnabled) {
            hapticEngine.performHapticFeedback(view, type)
        }
    }
}

@Singleton
class HapticEngine @Inject constructor() {

    fun performHapticFeedback(view: View, type: HapticType) {
        val constant = when (type) {
            HapticType.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
            HapticType.DRAG_START -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            HapticType.DRAG_END -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_END
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            HapticType.GRID_SNAP -> HapticFeedbackConstants.CLOCK_TICK
            HapticType.FOLDER_OPEN -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticType.DRAWER_OPEN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            HapticType.TOGGLE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                HapticFeedbackConstants.TOGGLE_ON
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            HapticType.REJECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            HapticType.CLICK -> HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.isHapticFeedbackEnabled = true
        view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
    }

    enum class HapticType {
        LONG_PRESS,
        DRAG_START,
        DRAG_END,
        GRID_SNAP,
        FOLDER_OPEN,
        DRAWER_OPEN,
        TOGGLE,
        REJECT,
        CLICK
    }
}
