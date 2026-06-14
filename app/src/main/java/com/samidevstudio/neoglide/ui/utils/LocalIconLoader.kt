package com.samidevstudio.neoglide.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf

val LocalIconLoader = staticCompositionLocalOf<IconLoader> {
    error("No IconLoader provided")
}
