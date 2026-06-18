package com.samidevstudio.neoglide.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal to provide information about whether the current wallpaper is considered "light".
 * Used for adjusting UI contrast (e.g., label colors on the home screen).
 */
val LocalWallpaperIsLight = staticCompositionLocalOf { false }
