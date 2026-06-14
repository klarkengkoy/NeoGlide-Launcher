package com.samidevstudio.neoglide.ui.utils

import androidx.compose.runtime.staticCompositionLocalOf

val LocalIconCache = staticCompositionLocalOf<IconCache> {
    error("No IconCache provided")
}
