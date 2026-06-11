package com.samidevstudio.pxllauncherneo

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.samidevstudio.pxllauncherneo.ui.detail.AppDetailScreen
import com.samidevstudio.pxllauncherneo.ui.home.HomeScreen
import com.samidevstudio.pxllauncherneo.ui.navigation.AppDetailRoute
import com.samidevstudio.pxllauncherneo.ui.navigation.HomeRoute
import com.samidevstudio.pxllauncherneo.ui.navigation.Navigator
import com.samidevstudio.pxllauncherneo.ui.navigation.rememberNavigationState
import com.samidevstudio.pxllauncherneo.ui.navigation.toEntries
import com.samidevstudio.pxllauncherneo.ui.theme.PxlLauncherTheme
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine
import com.samidevstudio.pxllauncherneo.ui.utils.LocalHapticEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.samidevstudio.pxllauncherneo.data.repository.WidgetRepository
import androidx.compose.runtime.CompositionLocalProvider

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var widgetRepository: WidgetRepository
    @Inject lateinit var hapticEngine: HapticEngine

    override fun onStart() {
        super.onStart()
        widgetRepository.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetRepository.stopListening()
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalHapticEngine provides hapticEngine) {
                PxlLauncherTheme {
                    val navigationState = rememberNavigationState(
                        startRoute = HomeRoute,
                        topLevelRoutes = setOf(HomeRoute),
                    )
                    val navigator = remember { Navigator(navigationState) }
                    
                    SharedTransitionLayout {
                        val entryProvider = entryProvider<NavKey> {
                            entry<HomeRoute> {
                                HomeScreen(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                )
                            }
                            entry<AppDetailRoute> { key ->
                                AppDetailScreen(
                                    packageName = key.packageName,
                                    label = "App Name",
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                )
                            }
                        }

                        NavDisplay(
                            entries = navigationState.toEntries(entryProvider),
                            onBack = { navigator.goBack() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        val isHomeIntent = intent.action == android.content.Intent.ACTION_MAIN && 
                          intent.hasCategory(android.content.Intent.CATEGORY_HOME)
        if (isHomeIntent) {
            // Re-center or reset home view if needed
        }
    }
}
