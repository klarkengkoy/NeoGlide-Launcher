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
import com.samidevstudio.neoglide.ui.utils.IconCache
import com.samidevstudio.neoglide.ui.utils.LocalIconCache
import com.samidevstudio.neoglide.ui.utils.IconLoader
import com.samidevstudio.neoglide.ui.utils.LocalIconLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.samidevstudio.neoglide.data.repository.WidgetRepository
import com.samidevstudio.neoglide.data.repository.AppRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
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
    @Inject lateinit var iconCache: IconCache
    @Inject lateinit var iconLoader: IconLoader
    @Inject lateinit var appRepository: AppRepository
    
    private val homeViewModel: HomeViewModel by viewModels()
    private val drawerViewModel: DrawerViewModel by viewModels()

    private var warmUpTriggerJob: Job? = null

    private val iconRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            homeViewModel.triggerIconRefresh()
            drawerViewModel.triggerIconRefresh()
        }
    }

    override fun onResume() {
        super.onResume()
        // Deduplicate warmup triggers. Ensures if onResume flutters, we only run once.
        warmUpTriggerJob?.cancel()
        warmUpTriggerJob = lifecycleScope.launch {
            delay(300)
            appRepository.warmUpIcons(iconLoader, lifecycleScope)
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
        
        // Stop warm-up and clear cache to be a "Good Neighbor"
        appRepository.stopWarmUp()
        iconCache.clear()
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
                val info = try {
                    homeViewModel.appWidgetManager.getAppWidgetInfo(widgetId)
                } catch (_: Exception) {
                    null
                }
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
        } catch (_: Exception) {
            homeViewModel.cancelWidgetConfiguration(widgetId)
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val startTime = System.currentTimeMillis()
        val splashScreen = installSplashScreen()
        
        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            val stillShowing = elapsed < 2500
            stillShowing
        }
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start app refresh immediately on cold start
        lifecycleScope.launch {
            appRepository.refreshApps()
        }

        setContent {
            CompositionLocalProvider(
                LocalHapticEngine provides hapticEngine,
                LocalIconCache provides iconCache,
                LocalIconLoader provides iconLoader
            ) {
                NeoGlideLauncherTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}
