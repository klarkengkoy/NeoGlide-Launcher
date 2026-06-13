package com.samidevstudio.pxllauncherneo.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.widget.Toast
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as hiltViewModelV2
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlinx.coroutines.launch
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import com.samidevstudio.pxllauncherneo.data.repository.AppLabelMode
import com.samidevstudio.pxllauncherneo.data.repository.NotificationDotMode
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.ui.components.*
import com.samidevstudio.pxllauncherneo.ui.drawer.DrawerScreen
import com.samidevstudio.pxllauncherneo.ui.settings.SettingsSheet
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine
import com.samidevstudio.pxllauncherneo.ui.utils.rememberHapticFeedback

data class RectBounds(
    val row: Float,
    val col: Float,
    val spanX: Float,
    val spanY: Float
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModelV2(),
    settingsViewModel: com.samidevstudio.pxllauncherneo.ui.settings.SettingsViewModel = hiltViewModelV2(),
    sharedTransitionScope: SharedTransitionScope,
    drawerViewModel: com.samidevstudio.pxllauncherneo.ui.drawer.DrawerViewModel = hiltViewModelV2(),
) {
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val recentlyUsedApps by viewModel.recentlyUsedApps.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val hapticFeedback = rememberHapticFeedback(preferences)
    val shouldShowDefaultPrompt by viewModel.shouldShowDefaultPrompt.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showWidgetMenu by remember { mutableStateOf(false) }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showFolderMenuId by remember { mutableIntStateOf(-1) }
    var showFolderMenuLabel by remember { mutableStateOf("") }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var editingWidgetId by remember { mutableIntStateOf(-1) }
    var showAppPicker by remember { mutableStateOf(false) }
    var pendingAddAppRow by remember { mutableFloatStateOf(0f) }
    var pendingAddAppCol by remember { mutableFloatStateOf(0f) }
    var gridCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    var expandedFolderId by remember { mutableIntStateOf(-1) }
    var draggingAppFromFolder by remember { mutableStateOf<AppModel?>(null) }
    var sourceFolderId by remember { mutableIntStateOf(-1) }
    var isInvisibleByDrag by remember { mutableStateOf(false) }

    // TRACK ORIGINAL POSITION FOR DRAG-FIRST LOGIC
    var originalRow by remember { mutableFloatStateOf(-1f) }
    var originalCol by remember { mutableFloatStateOf(-1f) }

    var showAppMenuPackage by remember { mutableStateOf<String?>(null) }
    var showAppMenuId by remember { mutableIntStateOf(-1) }
    var appMenuShortcuts by remember { mutableStateOf<List<com.samidevstudio.pxllauncherneo.domain.model.AppShortcut>>(emptyList()) }
    var appMenuLabel by remember { mutableStateOf("") }

    // DRAG STATE FOR ICON REPOSITIONING
    var draggingItemId by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragTargetBounds by remember { mutableStateOf<RectBounds?>(null) }
    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()

    val widgetConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val info = viewModel.appWidgetManager.getAppWidgetInfo(appWidgetId)
                viewModel.addWidget(WidgetEntity(
                    widgetId = appWidgetId,
                    providerPackage = info?.provider?.packageName ?: "",
                    providerClass = info?.provider?.className ?: "",
                    label = info?.loadLabel(context.packageManager) ?: "Widget",
                    spanX = 4f,
                    spanY = 2f,
                    row = 0f,
                    column = 0f
                ))
            }
        }
    }

    val widgetPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val info = viewModel.appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (info?.configure != null) {
                    try {
                        val intent = android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = info.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        widgetConfigLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Cannot configure this widget", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.addWidget(WidgetEntity(
                        widgetId = appWidgetId,
                        providerPackage = info?.provider?.packageName ?: "",
                        providerClass = info?.provider?.className ?: "",
                        label = info?.loadLabel(context.packageManager) ?: "Widget",
                        spanX = 4f,
                        spanY = 2f,
                        row = 0f,
                        column = 0f
                    ))
                }
            }
        }
    }

    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    val homeAlpha by animateFloatAsState(
        targetValue = if (showDrawer) 0f else 1f,
        animationSpec = tween(300),
        label = "homeAlpha"
    )

    LaunchedEffect(Unit) {
        viewModel.checkDefaultLauncher()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = showDrawer || showSettings || showContextMenu || showWidgetMenu || showFolderMenu || editingWidgetId != -1 || showAppMenuPackage != null || expandedFolderId != -1) {
        if (showWidgetMenu) {
            showWidgetMenu = false
        } else if (showFolderMenu) {
            showFolderMenu = false
            showFolderMenuId = -1
        } else if (expandedFolderId != -1) {
            expandedFolderId = -1
        } else if (showAppMenuPackage != null) {
            showAppMenuPackage = null
        } else if (editingWidgetId != -1) {
            editingWidgetId = -1
        } else if (showSettings) {
            showSettings = false
        } else if (showContextMenu) {
            showContextMenu = false
        } else {
            drawerViewModel.resetState()
            showDrawer = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -20) {
                        showDrawer = true
                    }
                }
            }
    ) {
        // FULL SCREEN FROSTED GLASS
        AnimatedVisibility(
            visible = editingWidgetId != -1 || showFolderMenu || draggingItemId != -1 || expandedFolderId != -1,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FrostedGlass(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 20.dp
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .onGloballyPositioned { gridCoords = it }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    editingWidgetId = -1
                                    showWidgetMenu = false
                                    showAppMenuPackage = null
                                    focusManager.clearFocus()
                                },
                                onLongPress = { offset ->
                                    // Disable home screen menu when editing a widget
                                    if (editingWidgetId == -1 && !preferences.lockLayout) {
                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                        contextMenuOffset = DpOffset(
                                            x = with(density) { offset.x.toDp() },
                                            y = with(density) { offset.y.toDp() }
                                        )
                                        showContextMenu = true
                                    } else if (preferences.lockLayout) {
                                        Toast.makeText(context, "Layout is locked", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                ) {
                    val unitWidth = maxWidth / 4f
                    val topOffset = 0.dp // Use padding from Scaffold
                    val bottomPadding = 0.dp // Use padding from Scaffold
                    val topOffsetPx = with(density) { topOffset.toPx() }

                    // Adaptive Grid: Calculate nearest row count and stretch to fill height minus bottom padding
                    val availableHeight = maxHeight
                    val maxRows = (availableHeight / 96.dp).let {
                        if (it % 1 > 0.7f) it.toInt() + 1 else it.toInt()
                    }.coerceAtLeast(1)
                    val unitHeight = availableHeight / maxRows
                    val unitWidthPx = with(density) { unitWidth.toPx() }
                    val unitHeightPx = with(density) { unitHeight.toPx() }

                    // DRAG LOGIC REFINEMENT: Update bounds in response to dragOffset changes
                    fun calculateTargetBounds(offset: androidx.compose.ui.geometry.Offset): RectBounds {
                        val iconSizePx = with(density) { 80.dp.toPx() }
                        // Offset is standardized as the TOP-LEFT of the dragging 80dp icon
                        val centerX = offset.x + iconSizePx / 2
                        val centerY = offset.y + iconSizePx / 2

                        // Find the grid cell index (0.5 steps) that contains the center point
                        val targetRow = (((centerY - topOffsetPx) / unitHeightPx) * 2).toInt() / 2f
                            .coerceIn(0f, maxRows.toFloat() - 1f)
                        val targetCol = ((centerX / unitWidthPx) * 2).toInt() / 2f
                            .coerceIn(0f, 3f)
                        return RectBounds(targetRow, targetCol, 1f, 1f)
                    }

                    LaunchedEffect(dragOffset, draggingItemId, draggingAppFromFolder) {
                        if (draggingItemId != -1 || draggingAppFromFolder != null) {
                            dragTargetBounds = calculateTargetBounds(dragOffset)
                        }
                    }

                    // RENDER DRAGGING APP FROM FOLDER
                    draggingAppFromFolder?.let { app ->
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                }
                                .size(80.dp) // Render with 80dp to match FolderExpansion
                                .zIndex(100f)
                                .graphicsLayer {
                                    scaleX = 1.2f
                                    scaleY = 1.2f
                                    alpha = 0.9f
                                    shadowElevation = 16.dp.toPx()
                                    shape = RoundedCornerShape(16.dp)
                                }
                        ) {
                            AppItem(
                                app = app,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = preferences.useMonochromeIcons,
                                iconPackPackageName = preferences.iconPackPackageName,
                                showLabel = false, // Keep label hidden during drag out
                                sharedElementKeyPrefix = "dragging-folder",
                                isLongClickEnabled = false,
                                onClick = {}
                            )
                        }
                    }


                    // GRID OVERLAY
                    if (editingWidgetId != -1 || draggingItemId != -1 || draggingAppFromFolder != null) {
                        val isDark = isSystemInDarkTheme()
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val mainGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f)
                            val subGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)

                            // Draw exactly maxRows lines, starting from topOffset
                            for (i in 0..maxRows) {
                                val y = topOffsetPx + (i * unitHeight.toPx())
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2f)
                                if (i < maxRows) {
                                    val midY = y + (unitHeight.toPx() / 2f)
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(0f, midY), androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = 1f)
                                }
                            }

                            val gridBottomY = topOffsetPx + (maxRows * unitHeight.toPx())
                            for (i in 0..4) {
                                val x = i * unitWidth.toPx()
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(x, topOffsetPx), androidx.compose.ui.geometry.Offset(x, gridBottomY), strokeWidth = 2f)
                                if (i < 4) {
                                    val midX = x + (unitWidth.toPx() / 2f)
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(midX, topOffsetPx), androidx.compose.ui.geometry.Offset(midX, gridBottomY), strokeWidth = 1f)
                                }
                            }
                        }
                    }

                    // TOUCH BLOCKER (Scrim) - placed below the active widget but above everything else in the grid (including Dock)
                    if (editingWidgetId != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0.5f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = {
                                        editingWidgetId = -1
                                        showWidgetMenu = false
                                    })
                                }
                        )
                    }

                    // No longer rendering static Dock here, it's now in homeItems

                    // GHOST TARGET VISUAL
                    if (draggingItemId != -1 || draggingAppFromFolder != null || (editingWidgetId != -1 && dragTargetBounds != null)) {
                        dragTargetBounds?.let { bounds ->
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = unitWidth * bounds.col,
                                        y = topOffset + (unitHeight * bounds.row)
                                    )
                                    .size(unitWidth * bounds.spanX, unitHeight * bounds.spanY)
                                    .padding(8.dp)
                                    .background((if (isSystemInDarkTheme()) Color.White else Color.Black).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            )
                        }
                    }

                    homeItems.forEach { item ->
                        when (item) {
                            is HomeItem.App -> {
                                val app = item.appModel
                                val isDragging = draggingItemId == item.id
                                val unitWidthPx = with(density) { unitWidth.toPx() }
                                val unitHeightPx = with(density) { unitHeight.toPx() }
                                val topOffsetPx = with(density) { topOffset.toPx() }

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            if (isDragging) {
                                                androidx.compose.ui.unit.IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                            } else {
                                                androidx.compose.ui.unit.IntOffset(
                                                    (unitWidth * item.column).toPx().roundToInt(),
                                                    (topOffset + (unitHeight * item.row)).toPx().roundToInt()
                                                )
                                            }
                                        }
                                        .size(unitWidth, unitHeight)
                                        .zIndex(if (isDragging) 100f else 0f)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                scaleX = 1.2f
                                                scaleY = 1.2f
                                                alpha = 0.9f
                                                shadowElevation = 16.dp.toPx()
                                                shape = RoundedCornerShape(16.dp)
                                            }
                                        }
                                        .pointerInput(item.id, item.row, item.column) {
                                            if (!preferences.lockLayout) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { _ ->
                                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                        isDragConfirmed = false
                                                        
                                                        showAppMenuPackage = null // Hide menu on drag start
                                                        originalRow = item.row
                                                        originalCol = item.column

                                                        // Calculate initial dragOffset based on current position
                                                        val startX = unitWidthPx * item.column
                                                        val startY = topOffsetPx + (unitHeightPx * item.row)
                                                        dragOffset = androidx.compose.ui.geometry.Offset(startX, startY)
                                                        dragTargetBounds = RectBounds(item.row, item.column, 1f, 1f)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingItemId = item.id
                                                        }

                                                        if (isDragConfirmed) {
                                                            // Calculate nearest grid cell with 0.5f snapping
                                                            val targetRow = (((dragOffset.y - topOffsetPx) / unitHeightPx) * 2).roundToInt() / 2f
                                                                .coerceIn(0f, maxRows.toFloat() - 1f)
                                                            val targetCol = (((dragOffset.x) / unitWidthPx) * 2).roundToInt() / 2f
                                                                .coerceIn(0f, 3f)

                                                            if (targetRow != dragTargetBounds?.row || targetCol != dragTargetBounds?.col) {
                                                                hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                                            }

                                                            dragTargetBounds = RectBounds(targetRow, targetCol, 1f, 1f)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (isDragConfirmed) {
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                            dragTargetBounds?.let { bounds ->
                                                                viewModel.updateItemPosition(item, bounds.row, bounds.col)
                                                            }
                                                        } else {
                                                            // Trigger Menu (not dragged enough)
                                                            appMenuLabel = app.label
                                                            showAppMenuId = item.id
                                                            val iconLeft = (unitWidth * item.column)
                                                            val iconTop = topOffset + (unitHeight * item.row)

                                                            val menuH = 150.dp
                                                            val gap = 8.dp

                                                            var finalY = iconTop + unitHeight + gap
                                                            if (finalY + menuH > availableHeight) {
                                                                finalY = iconTop - menuH - gap
                                                            }

                                                            contextMenuOffset = DpOffset(x = iconLeft, y = finalY)
                                                            coroutineScope.launch {
                                                                appMenuShortcuts = viewModel.getShortcuts(app.packageName)
                                                                showAppMenuPackage = app.packageName
                                                            }
                                                        }
                                                        draggingItemId = -1
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    },
                                                    onDragCancel = {
                                                        if (isDragConfirmed) {
                                                            // If we were dragging, just reset
                                                        } else {
                                                            // Maybe show menu on cancel too if it was a long press? 
                                                            // But usually cancel means something else interrupted.
                                                        }
                                                        draggingItemId = -1
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    }
                                                )
                                            }
                                        }
                                ) {
                                    AppItem(
                                        app = app,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        useMonochrome = preferences.useMonochromeIcons,
                                        iconPackPackageName = preferences.iconPackPackageName,
                                        isHidden = app.packageName in preferences.hiddenPackages,
                                        hasNotification = (preferences.notificationDotMode == NotificationDotMode.APP_ICON ||
                                                preferences.notificationDotMode == NotificationDotMode.BOTH) &&
                                                app.packageName in activeNotifications.keys,
                                        notificationCount = activeNotifications[app.packageName] ?: 0,
                                        showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                        sharedElementKeyPrefix = "home",
                                        isLongClickEnabled = false,
                                        getShortcuts = { viewModel.getShortcuts(it) },
                                        onShortcutClick = { viewModel.launchShortcut(it) },
                                        onHideToggle = {
                                            if (app.packageName in preferences.hiddenPackages) {
                                                viewModel.unhideApp(app.packageName)
                                            } else {
                                                viewModel.hideApp(app.packageName)
                                            }
                                        },
                                        onLongClick = null
                                    ) { options ->
                                        if (draggingItemId == -1) {
                                            viewModel.launchApp(app.packageName, options)
                                        }
                                    }
                                }
                            }
                            is HomeItem.Folder -> {
                                val isDragging = draggingItemId == item.id
                                val unitWidthPx = with(density) { unitWidth.toPx() }
                                val unitHeightPx = with(density) { unitHeight.toPx() }
                                val topOffsetPx = with(density) { topOffset.toPx() }

                                Box(
                                    modifier = Modifier
                                        .offset {
                                            if (isDragging) {
                                                androidx.compose.ui.unit.IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                            } else {
                                                androidx.compose.ui.unit.IntOffset(
                                                    (unitWidth * item.column).toPx().roundToInt(),
                                                    (topOffset + (unitHeight * item.row)).toPx().roundToInt()
                                                )
                                            }
                                        }
                                        .size(unitWidth, unitHeight)
                                        .zIndex(if (isDragging) 100f else 0f)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                scaleX = 1.2f
                                                scaleY = 1.2f
                                                alpha = 0.9f
                                                shadowElevation = 16.dp.toPx()
                                                shape = RoundedCornerShape(16.dp)
                                            }
                                        }
                                        .pointerInput(item.id, item.row, item.column) {
                                            if (!preferences.lockLayout) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { _ ->
                                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                        isDragConfirmed = false
                                                        
                                                        originalRow = item.row
                                                        originalCol = item.column

                                                        val startX = unitWidthPx * item.column
                                                        val startY = topOffsetPx + (unitHeightPx * item.row)
                                                        dragOffset = androidx.compose.ui.geometry.Offset(startX, startY)
                                                        dragTargetBounds = RectBounds(item.row, item.column, 1f, 1f)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingItemId = item.id
                                                        }

                                                        if (isDragConfirmed) {
                                                            val targetRow = (((dragOffset.y - topOffsetPx) / unitHeightPx) * 2).roundToInt() / 2f
                                                                .coerceIn(0f, maxRows.toFloat() - 1f)
                                                            val targetCol = (((dragOffset.x) / unitWidthPx) * 2).roundToInt() / 2f
                                                                .coerceIn(0f, 3f)

                                                            if (targetRow != dragTargetBounds?.row || targetCol != dragTargetBounds?.col) {
                                                                hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                                            }

                                                            dragTargetBounds = RectBounds(targetRow, targetCol, 1f, 1f)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (isDragConfirmed) {
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                            dragTargetBounds?.let { bounds ->
                                                                viewModel.updateItemPosition(item, bounds.row, bounds.col)
                                                            }
                                                        } else {
                                                            // Trigger Menu
                                                            showFolderMenuLabel = item.label
                                                            showFolderMenuId = item.id
                                                            val iconLeft = (unitWidth * item.column)
                                                            val iconTop = topOffset + (unitHeight * item.row)
                                                            val iconHeight = unitHeight

                                                            val menuH = 100.dp
                                                            val gap = 8.dp

                                                            var finalY = iconTop + iconHeight + gap
                                                            if (finalY + menuH > availableHeight) {
                                                                finalY = iconTop - menuH - gap
                                                            }

                                                            contextMenuOffset = DpOffset(x = iconLeft, y = finalY)
                                                            showFolderMenu = true
                                                        }
                                                        draggingItemId = -1
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    },
                                                    onDragCancel = {
                                                        draggingItemId = -1
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    }
                                                )
                                            }
                                        }
                                ) {
                                    FolderItem(
                                        label = item.label,
                                        apps = item.apps,
                                        useMonochrome = preferences.useMonochromeIcons,
                                        showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                        onHapticFeedback = hapticFeedback,
                                        onClick = {
                                            if (draggingItemId == -1) {
                                                expandedFolderId = item.id
                                            }
                                        }
                                    )
                                }
                            }
                            is HomeItem.Widget -> {
                                val isCurrentEditing = editingWidgetId == item.id
                                val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()

                                PxlWidgetHost(
                                    widgetId = item.id,
                                    appWidgetHost = viewModel.appWidgetHost,
                                    appWidgetManager = viewModel.appWidgetManager,
                                    // Adaptive Snap: handle special placeholder rows
                                    row = when {
                                        item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                                        item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                                        else -> item.row.coerceIn(0f, (maxRows - item.spanY).coerceAtLeast(0f))
                                    },
                                    column = item.column.coerceIn(0f, (4f - item.spanX).coerceAtLeast(0f)),
                                    spanX = item.spanX,
                                    spanY = item.spanY,
                                    unitWidth = unitWidth,
                                    unitHeight = unitHeight,
                                    isEditing = isCurrentEditing,
                                    onHapticFeedback = hapticFeedback,
                                    modifier = Modifier
                                        .offset(y = topOffset)
                                        .zIndex(if (isCurrentEditing) 1f else 0f),
                                    onDragStart = {
                                        showWidgetMenu = false
                                        editingWidgetId = item.id
                                        originalRow = item.row
                                        originalCol = item.column
                                    },
                                    onResizeStart = {
                                        showWidgetMenu = false
                                    },
                                    onLongClick = {
                                        // Trigger Menu (dropped on same spot)
                                        val widgetLeft = (unitWidth * item.column) + 4.dp
                                        val widgetTop = topOffset + (unitHeight * item.row) + 4.dp
                                        val widgetWidth = (unitWidth * item.spanX) - 8.dp
                                        val widgetHeight = (unitHeight * item.spanY) - 8.dp
                                        val widgetBottom = widgetTop + widgetHeight
                                        val widgetRight = widgetLeft + widgetWidth

                                        val menuH = 100.dp
                                        val menuW = 150.dp
                                        val gap = 12.dp

                                        var finalX = widgetLeft
                                        var finalY = widgetBottom + gap

                                        val gridW = maxWidth
                                        val gridH = maxHeight

                                        if (widgetBottom + menuH + gap > gridH) {
                                            if (widgetTop > menuH + gap) {
                                                finalY = widgetTop - menuH - gap
                                            } else {
                                                finalY = widgetTop
                                                if (widgetRight + menuW + gap <= gridW) {
                                                    finalX = widgetRight + gap
                                                } else if (widgetLeft > menuW + gap) {
                                                    finalX = widgetLeft - menuW - gap
                                                } else {
                                                    finalX = (gridW - menuW) / 2f
                                                    finalY = (gridH - menuH) / 2f
                                                }
                                            }
                                        }

                                        contextMenuOffset = DpOffset(x = finalX, y = finalY)
                                        showWidgetMenu = true
                                        // RETAIN editingWidgetId as requested
                                        editingWidgetId = item.id
                                    },
                                    onInteractionUpdate = { r, c, sx, sy ->
                                        dragTargetBounds = RectBounds(r, c, sx, sy)
                                    },
                                    onResize = { newRow, newCol, newSpanX, newSpanY ->
                                        // Normal move
                                        if (item.isCustom) {
                                            // Handle fixed-dimension widget (Internal Widgets like Dock)
                                            val widthChanged = newSpanX != item.spanX
                                            val heightChanged = newSpanY != item.spanY
                                            if (widthChanged || heightChanged) {
                                                val message = when {
                                                    widthChanged && heightChanged -> "This widget has a fixed size"
                                                    widthChanged -> "This widget has a fixed width"
                                                    else -> "This widget has a fixed height"
                                                }
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                viewModel.updateWidgetBounds(item.id, item.row, item.column, item.spanX, item.spanY)
                                            } else {
                                                viewModel.updateWidgetBounds(item.id, newRow.coerceAtLeast(0f), newCol, newSpanX, newSpanY)
                                            }
                                        } else {
                                            viewModel.updateWidgetBounds(
                                                item.id,
                                                newRow.coerceIn(0f, (maxRows - newSpanY).coerceAtLeast(0f)),
                                                newCol.coerceIn(0f, (4f - newSpanX).coerceAtLeast(0f)),
                                                newSpanX,
                                                newSpanY
                                            )
                                        }
                                        // RETAIN editingWidgetId as requested
                                        editingWidgetId = item.id
                                        dragTargetBounds = null
                                    }
                                ) {
                                    val currentMaxRows = maxRows
                                    val targetRow = when {
                                        item.row >= 99.5f -> (currentMaxRows - 1.5f).coerceAtLeast(0f)
                                        item.row >= 99f -> (currentMaxRows - 1f).coerceAtLeast(0f)
                                        else -> null
                                    }

                                    if (targetRow != null) {
                                        LaunchedEffect(item.id, targetRow) {
                                            viewModel.updateWidgetBounds(item.id, targetRow, item.column, item.spanX, item.spanY)
                                        }
                                    }

                                    if (item.isCustom) {
                                        // RENDER INTERNAL WIDGET CONTENT
                                        val widget = item.widgetEntity
                                        if (widget.providerClass == "dock") {
                                            // RENDER DOCK CONTENT
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Visual Background
                                                Surface(
                                                    modifier = Modifier.fillMaxSize(),
                                                    shape = RoundedCornerShape(24.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ) {}

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                                ) {
                                                    dockApps.forEach { app ->
                                                        AppItem(
                                                            app = app,
                                                            sharedTransitionScope = sharedTransitionScope,
                                                            animatedVisibilityScope = animatedVisibilityScope,
                                                            useMonochrome = preferences.useMonochromeIcons,
                                                            iconPackPackageName = preferences.iconPackPackageName,
                                                            isHidden = app.packageName in preferences.hiddenPackages,
                                                            hasNotification = (preferences.notificationDotMode == NotificationDotMode.APP_ICON ||
                                                                    preferences.notificationDotMode == NotificationDotMode.BOTH) &&
                                                                    app.packageName in activeNotifications.keys,
                                                            notificationCount = activeNotifications[app.packageName] ?: 0,
                                                            showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                                            sharedElementKeyPrefix = "dock",
                                                            getShortcuts = { viewModel.getShortcuts(it) },
                                                            onShortcutClick = { viewModel.launchShortcut(it) },
                                                            onHideToggle = {
                                                                if (app.packageName in preferences.hiddenPackages) {
                                                                    viewModel.unhideApp(app.packageName)
                                                                } else {
                                                                    viewModel.hideApp(app.packageName)
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        ) { options ->
                                                            viewModel.launchApp(app.packageName, options)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Remove the separate widgets loop as they are now handled in homeItems

                    HomeContextMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        offset = contextMenuOffset,
                        onOpenWidgets = {
                            val appWidgetId = viewModel.allocateWidgetId()
                            val intent = android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            widgetPickLauncher.launch(intent)
                        },
                        onAddApp = {
                            // Convert the contextMenuOffset (which is in Dp) to grid coordinates
                            val xPx = with(density) { contextMenuOffset.x.toPx() }
                            val yPx = with(density) { (contextMenuOffset.y - topOffset).toPx() }

                            val unitWidthPx = with(density) { unitWidth.toPx() }
                            val unitHeightPx = with(density) { unitHeight.toPx() }

                            pendingAddAppCol = (xPx / unitWidthPx).toInt().toFloat().coerceIn(0f, 3f)
                            pendingAddAppRow = (yPx / unitHeightPx).toInt().toFloat().coerceIn(0f, maxRows.toFloat() - 1f)

                            showAppPicker = true
                        },
                        onOpenLauncherSettings = { showSettings = true }
                    )

                    showAppMenuPackage?.let { pkg ->
                        AppContextMenu(
                            expanded = true,
                            onDismissRequest = {
                                showAppMenuPackage = null
                                showAppMenuId = -1
                            },
                            packageName = pkg,
                            label = appMenuLabel,
                            shortcuts = appMenuShortcuts,
                            offset = contextMenuOffset,
                            onShortcutClick = { viewModel.launchShortcut(it) },
                            onHideToggle = {
                                viewModel.hideApp(pkg)
                                if (showAppMenuId != -1) {
                                    viewModel.removeHomeApp(showAppMenuId)
                                }
                            },
                            onRemove = if (showAppMenuId != -1) {
                                { viewModel.removeHomeApp(showAppMenuId) }
                            } else null
                        )
                    }

                    WidgetContextMenu(
                        expanded = showWidgetMenu,
                        onDismissRequest = { showWidgetMenu = false },
                        onRemove = {
                            if (editingWidgetId != -1) {
                                viewModel.removeWidget(editingWidgetId)
                                editingWidgetId = -1
                            }
                            showWidgetMenu = false
                        },
                        onOpenApp = {
                            if (editingWidgetId != -1) {
                                val info = viewModel.appWidgetManager.getAppWidgetInfo(editingWidgetId)
                                info?.provider?.packageName?.let { pkg ->
                                    viewModel.launchApp(pkg)
                                }
                                editingWidgetId = -1
                            }
                            showWidgetMenu = false
                        },
                        offset = contextMenuOffset
                    )

                    FolderContextMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = {
                            showFolderMenu = false
                            showFolderMenuId = -1
                        },
                        label = showFolderMenuLabel,
                        offset = contextMenuOffset,
                        onEditName = {
                            if (showFolderMenuId != -1) {
                                expandedFolderId = showFolderMenuId
                            }
                        },
                        onRemove = {
                            if (showFolderMenuId != -1) {
                                viewModel.removeFolder(showFolderMenuId)
                            }
                        }
                    )

                    val expandedFolder = remember(homeItems, expandedFolderId) {
                        homeItems.find { it.id == expandedFolderId && it is HomeItem.Folder } as? HomeItem.Folder
                    }

                    if (expandedFolder != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { alpha = if (isInvisibleByDrag) 0f else 1f }
                        ) {
                            FolderExpansion(
                                folderId = expandedFolder.id,
                                label = expandedFolder.label,
                                apps = expandedFolder.apps,
                                onDismiss = { expandedFolderId = -1 },
                                onLabelChange = { newLabel -> viewModel.updateFolderLabel(expandedFolder.id, newLabel) },
                                onAppClick = { pkg, options ->
                                    viewModel.launchApp(pkg, options)
                                    expandedFolderId = -1
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = preferences.useMonochromeIcons,
                                iconPackPackageName = preferences.iconPackPackageName,
                                onHapticFeedback = hapticFeedback,
                                getShortcuts = { viewModel.getShortcuts(it) },
                                onShortcutClick = { viewModel.launchShortcut(it) },
                                onHideToggle = { pkg ->
                                    viewModel.hideApp(pkg)
                                },
                                onAppDragStart = { app, initialTopLeft ->
                                    hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                    draggingAppFromFolder = app
                                    sourceFolderId = expandedFolder.id

                                    // initialTopLeft is window-relative Top-Left of the 80dp icon
                                    val localTopLeft = gridCoords?.windowToLocal(initialTopLeft) ?: initialTopLeft
                                    dragOffset = localTopLeft
                                    dragTargetBounds = calculateTargetBounds(dragOffset)
                                },
                                onAppDrag = { amount ->
                                    dragOffset += amount
                                    val newBounds = calculateTargetBounds(dragOffset)
                                    if (newBounds.row != dragTargetBounds?.row || newBounds.col != dragTargetBounds?.col) {
                                        hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                    }
                                    dragTargetBounds = newBounds
                                },
                                onAppDragOut = { _, _, _ ->
                                    isInvisibleByDrag = true
                                },
                                onAppDragEnd = {
                                    hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                    if (isInvisibleByDrag) {
                                        val finalBounds = calculateTargetBounds(dragOffset)
                                        draggingAppFromFolder?.let { app ->
                                            viewModel.removeAppFromFolder(sourceFolderId, app.packageName, finalBounds.row, finalBounds.col)
                                        }
                                    }
                                    draggingAppFromFolder = null
                                    dragTargetBounds = null
                                    expandedFolderId = -1
                                    isInvisibleByDrag = false
                                },
                                onAppDragCancel = {
                                    draggingAppFromFolder = null
                                    dragTargetBounds = null
                                    expandedFolderId = -1
                                    isInvisibleByDrag = false
                                }
                            )
                        }
                    } else if (expandedFolderId != -1) {
                        expandedFolderId = -1
                    }
                }
            }
        }

        if (shouldShowDefaultPrompt) {
            DefaultLauncherDialog(
                onSetDefault = {
                    viewModel.openDefaultLauncherSettings()
                },
                onDismiss = { viewModel.dismissDefaultPrompt() }
            )
        }

        if (showSettings) {
            SettingsSheet(
                onDismiss = { showSettings = false },
                viewModel = settingsViewModel
            )
        }

        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                DrawerScreen(
                    viewModel = drawerViewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onAppClick = { packageName, options ->
                        drawerViewModel.resetState()
                        viewModel.launchApp(packageName, options)
                        showDrawer = false
                    }
                )
            }
        }

        if (showAppPicker) {
            val availableApps by viewModel.availableAppsForPicker.collectAsStateWithLifecycle()
            val recentApps by viewModel.recentlyUsedApps.collectAsStateWithLifecycle()
            AppPickerDialog(
                apps = availableApps,
                recentlyUsedApps = recentApps,
                onAppSelected = { app ->
                    viewModel.addHomeApp(app.packageName, pendingAddAppRow, pendingAddAppCol)
                    showAppPicker = false
                },
                onDismissRequest = { showAppPicker = false }
            )
        }

    }
}

@Composable
fun DefaultLauncherDialog(
    onSetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Set PxlLauncher as default?") },
        text = {
            Text("PxlLauncher is not your default launcher. Set it as default for the best experience. You can always change it back in settings.")
        },
        confirmButton = {
            TextButton(onClick = onSetDefault) {
                Text("Set as default")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No thanks")
            }
        }
    )
}
