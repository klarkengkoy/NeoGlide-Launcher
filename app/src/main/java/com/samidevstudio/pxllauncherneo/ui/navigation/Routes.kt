package com.samidevstudio.pxllauncherneo.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface PxlRoute : NavKey

@Serializable
data object HomeRoute : PxlRoute

@Serializable
data class AppDetailRoute(val packageName: String) : PxlRoute
