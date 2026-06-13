package com.samidevstudio.neoglide.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NeoGlideRoute : NavKey

@Serializable
data object HomeRoute : NeoGlideRoute

@Serializable
data class AppDetailRoute(val packageName: String) : NeoGlideRoute
