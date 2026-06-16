package com.samidevstudio.neoglide

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.samidevstudio.neoglide.data.repository.AppRepository
import com.samidevstudio.neoglide.data.repository.WidgetRepository
import com.samidevstudio.neoglide.ui.detail.AppDetailScreen
import com.samidevstudio.neoglide.ui.drawer.DrawerViewModel
import com.samidevstudio.neoglide.ui.home.HomeScreen
import com.samidevstudio.neoglide.ui.home.HomeViewModel
import com.samidevstudio.neoglide.ui.navigation.AppDetailRoute
import com.samidevstudio.neoglide.ui.navigation.HomeRoute
import com.samidevstudio.neoglide.ui.navigation.Navigator
import com.samidevstudio.neoglide.ui.navigation.rememberNavigationState
import com.samidevstudio.neoglide.ui.navigation.toEntries
import com.samidevstudio.neoglide.ui.theme.NeoGlideLauncherTheme
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.IconCache
import com.samidevstudio.neoglide.ui.utils.IconLoader
import com.samidevstudio.neoglide.ui.utils.LocalHapticEngine
import com.samidevstudio.neoglide.ui.utils.LocalIconCache
import com.samidevstudio.neoglide.ui.utils.LocalIconLoader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        private const val REQUEST_WIDGET_CONFIG = 100
    }

    @Inject lateinit var widgetRepository: WidgetRepository
    @Inject lateinit var hapticEngine: HapticEngine
    @Inject lateinit var iconCache: IconCache
    @Inject lateinit var iconLoader: IconLoader
    @Inject lateinit var appRepository: AppRepository

    private val homeViewModel: HomeViewModel by viewModels()
    private val drawerViewModel: DrawerViewModel by viewModels()

    private var pendingWidgetId: Int = -1

    private val bindWidgetLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1).takeIf { it != -1 } ?: pendingWidgetId
        
        if ((result.resultCode == RESULT_OK) && (widgetId != -1)) {
            val info = try {
                homeViewModel.appWidgetManager.getAppWidgetInfo(widgetId)
            } catch (_: Exception) {
                null
            }
            if (info?.configure != null) {
                startWidgetConfig(widgetId)
            } else {
                homeViewModel.completeWidgetConfiguration(widgetId)
                pendingWidgetId = -1
            }
        } else if (widgetId != -1) {
            homeViewModel.cancelWidgetConfiguration(widgetId)
            pendingWidgetId = -1
        }
    }

    private var warmUpTriggerJob: Job? = null
    private var delayedCleanupJob: Job? = null

    private val iconRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            homeViewModel.triggerIconRefresh()
            drawerViewModel.triggerIconRefresh()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("NeoGlideInit", "Step 5: Main Activity Resumed.")
        // Cancel any pending cleanup as user returned
        delayedCleanupJob?.cancel()

        // Deduplicate warmup triggers. Ensures if onResume flutters, we only run once.
        warmUpTriggerJob?.cancel()
        warmUpTriggerJob = lifecycleScope.launch {
            delay(300.milliseconds)
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

        // Use Delayed Cleanup if we are in the "Peek" window (10s)
        if (homeViewModel.isWithinPeekWindow()) {
            delayedCleanupJob?.cancel()
            delayedCleanupJob = lifecycleScope.launch {
                delay(10.seconds)
                appRepository.stopWarmUp()
                iconCache.clear()
            }
        } else {
            // Immediate cleanup for standard launches
            appRepository.stopWarmUp()
            iconCache.clear()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WIDGET_CONFIG) {
            val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1).takeIf { it != -1 } ?: pendingWidgetId
            if (resultCode == RESULT_OK && widgetId != -1) {
                homeViewModel.completeWidgetConfiguration(widgetId)
            } else if (widgetId != -1) {
                homeViewModel.cancelWidgetConfiguration(widgetId)
            }
            pendingWidgetId = -1
        }
    }

    fun startWidgetBind(widgetId: Int, provider: android.content.ComponentName) {
        pendingWidgetId = widgetId
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
        }
        bindWidgetLauncher.launch(intent)
    }

    fun startWidgetConfig(widgetId: Int) {
        pendingWidgetId = widgetId
        try {
            widgetRepository.appWidgetHost.startAppWidgetConfigureActivityForResult(
                this,
                widgetId,
                0,
                REQUEST_WIDGET_CONFIG,
                null,
            )
        } catch (_: Exception) {
            homeViewModel.cancelWidgetConfiguration(widgetId)
            pendingWidgetId = -1
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("NeoGlideInit", "Step 2: Activity onCreate started.")
        val startTime = System.currentTimeMillis()

        Log.d("NeoGlideInit", "Step 2: Installing SplashScreen API.")
        val splashScreen = installSplashScreen()

        var hasLoggedDismissal = false
        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            val isDatabaseReady = appRepository.isDatabaseReady.value

            val shouldStay = !isDatabaseReady && elapsed < 5000
            if (!shouldStay && !hasLoggedDismissal) {
                hasLoggedDismissal = true
                Log.d("NeoGlideInit", "Step 2: Splash screen condition met. Ready to transition.")
            }
            shouldStay
        }

        splashScreen.setOnExitAnimationListener { vp ->
            Log.d("NeoGlideInit", "Step 4: Starting Unified Overlay Animation (Erase then Un-Scribble Reveal).")
            val iconView = vp.iconView
            val iconWidth = iconView.width
            val iconHeight = iconView.height
            val iconLeft = iconView.left
            val iconTop = iconView.top

            // Unified Overlay for both phases
            val overlayView = object : android.view.View(this) {
                val scribblePaint = Paint().apply {
                    val bg = vp.view.background as? android.graphics.drawable.ColorDrawable
                    color = bg?.color ?: android.graphics.Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 120f // Slightly thicker for guaranteed coverage
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    isAntiAlias = true
                }
                
                val clearPaint = Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                val scribblePath = Path()
                val drawPath = Path()
                val pathMeasure = PathMeasure()
                
                val wiperPath = Path()
                val wiperRectF = RectF()
                
                var eraseProgress = 0f
                var revealProgress = 0f

                init {
                    // Build the Zig-Zag path for Erase
                    val steps = 10
                    val stepWidth = iconWidth.toFloat() / steps
                    scribblePath.moveTo(0f, 0f)
                    for (i in 0..steps) {
                        val x = i * stepWidth
                        val y = if (i % 2 == 1) iconHeight.toFloat() else 0f
                        scribblePath.lineTo(x, y)
                    }
                    pathMeasure.setPath(scribblePath, false)
                }

                override fun onDraw(canvas: android.graphics.Canvas) {
                    // Use a layer to allow PorterDuff.Mode.CLEAR to work on the overlay's content
                    val checkpoint = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

                    // 1. Draw the scribble (Erase Phase)
                    drawPath.reset()
                    pathMeasure.getSegment(0f, eraseProgress * pathMeasure.length, drawPath, true)
                    canvas.drawPath(drawPath, scribblePaint)

                    // 2. Clear the scribble (Reveal Phase)
                    if (revealProgress > 0f) {
                        wiperPath.reset()
                        val pivotX = width / 2f
                        val pivotY = height * 1.3f
                        val r = height * 2.5f
                        
                        val startAngle = -45f
                        val endAngle = -135f
                        val sweep = (endAngle - startAngle) * revealProgress
                        
                        wiperPath.moveTo(pivotX, pivotY)
                        wiperRectF.set(pivotX - r, pivotY - r, pivotX + r, pivotY + r)
                        wiperPath.arcTo(wiperRectF, startAngle, sweep, false)
                        wiperPath.close()
                        
                        canvas.drawPath(wiperPath, clearPaint)
                    }

                    canvas.restoreToCount(checkpoint)
                }
            }

            // Position overlay exactly over the icon
            val params = android.widget.FrameLayout.LayoutParams(iconWidth, iconHeight).apply {
                leftMargin = iconLeft
                topMargin = iconTop
            }
            (vp.view as android.view.ViewGroup).addView(overlayView, params)

            // Animators
            val eraseAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1500L
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener {
                    overlayView.eraseProgress = it.animatedValue as Float
                    overlayView.invalidate()
                }
            }

            val revealAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1500L
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    overlayView.revealProgress = it.animatedValue as Float
                    overlayView.invalidate()
                }
            }

            val fadeAnimator = android.animation.ObjectAnimator.ofFloat(vp.view, "alpha", 1f, 0f).apply {
                duration = 300L
            }

            // Sequence: Erase -> Reveal -> Fade
            eraseAnimator.addListener(
                object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        revealAnimator.start()
                    }
                }
            )

            revealAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    fadeAnimator.start()
                }
            })

            fadeAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    vp.remove()
                }
            })

            eraseAnimator.start()
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start app refresh immediately on cold start
        lifecycleScope.launch {
            appRepository.refreshApps()
        }

        Log.d("NeoGlideInit", "Step 3: Setting up Compose UI content...")
        setContent {
            CompositionLocalProvider(
                LocalHapticEngine provides hapticEngine,
                LocalIconCache provides iconCache,
                LocalIconLoader provides iconLoader
            ) {
                Log.d("NeoGlideInit", "Step 3: UI Composition entered.")
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
                                entries = navigationState.toEntries(entryProvider)
                            ) {
                                navigator.goBack()
                            }
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
