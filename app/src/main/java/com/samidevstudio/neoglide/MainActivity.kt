package com.samidevstudio.neoglide

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.viewModels
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
import com.samidevstudio.neoglide.ui.detail.AppDetailScreen
import com.samidevstudio.neoglide.ui.home.HomeScreen
import com.samidevstudio.neoglide.ui.home.HomeViewModel
import com.samidevstudio.neoglide.ui.navigation.AppDetailRoute
import com.samidevstudio.neoglide.ui.navigation.HomeRoute
import com.samidevstudio.neoglide.ui.navigation.Navigator
import com.samidevstudio.neoglide.ui.navigation.rememberNavigationState
import com.samidevstudio.neoglide.ui.navigation.toEntries
import com.samidevstudio.neoglide.ui.theme.NeoGlideLauncherTheme
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.LocalHapticEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.samidevstudio.neoglide.data.repository.WidgetRepository
import androidx.compose.runtime.CompositionLocalProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import com.samidevstudio.neoglide.ui.drawer.DrawerViewModel

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val REQUEST_WIDGET_CONFIG = 100
        private const val REQUEST_BIND_WIDGET = 101
    }

    @Inject lateinit var widgetRepository: WidgetRepository
    @Inject lateinit var hapticEngine: HapticEngine
    
    private val homeViewModel: HomeViewModel by viewModels()
    private val drawerViewModel: DrawerViewModel by viewModels()

    private val iconRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            homeViewModel.triggerIconRefresh()
            drawerViewModel.triggerIconRefresh()
        }
    }

    override fun onStart() {
        super.onStart()
        widgetRepository.startListening()
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        registerReceiver(iconRefreshReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        widgetRepository.stopListening()
        unregisterReceiver(iconRefreshReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WIDGET_CONFIG) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == RESULT_OK && widgetId != -1) {
                homeViewModel.completeWidgetConfiguration(widgetId)
            } else if (widgetId != -1) {
                homeViewModel.cancelWidgetConfiguration(widgetId)
            }
        } else if (requestCode == REQUEST_BIND_WIDGET) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (resultCode == RESULT_OK && widgetId != -1) {
                val info = homeViewModel.appWidgetManager.getAppWidgetInfo(widgetId)
                if (info?.configure != null) {
                    startWidgetConfig(widgetId)
                } else {
                    homeViewModel.completeWidgetConfiguration(widgetId)
                }
            } else if (widgetId != -1) {
                homeViewModel.cancelWidgetConfiguration(widgetId)
            }
        }
    }

    fun startWidgetBind(widgetId: Int, provider: android.content.ComponentName) {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        startActivityForResult(intent, REQUEST_BIND_WIDGET)
    }

    fun startWidgetConfig(widgetId: Int) {
        try {
            widgetRepository.appWidgetHost.startAppWidgetConfigureActivityForResult(
                this,
                widgetId,
                0,
                REQUEST_WIDGET_CONFIG,
                null
            )
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start widget config", e)
            homeViewModel.cancelWidgetConfiguration(widgetId)
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Removed forced delay
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalHapticEngine provides hapticEngine) {
                NeoGlideLauncherTheme {
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isHomeIntent = intent.action == Intent.ACTION_MAIN && 
                          intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHomeIntent) {
            // Re-center or reset home view if needed
        }
    }
}
