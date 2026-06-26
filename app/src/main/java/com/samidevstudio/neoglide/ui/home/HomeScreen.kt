package com.samidevstudio.neoglide.ui.home

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.data.repository.BadgeStyle
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.components.AppContextMenu
import com.samidevstudio.neoglide.ui.components.AppItem
import com.samidevstudio.neoglide.ui.components.AppPickerDialog
import com.samidevstudio.neoglide.ui.components.FolderContextMenu
import com.samidevstudio.neoglide.ui.components.FolderExpansion
import com.samidevstudio.neoglide.ui.components.FolderItem
import com.samidevstudio.neoglide.ui.components.FrostedGlass
import com.samidevstudio.neoglide.ui.components.HomeContextMenu
import com.samidevstudio.neoglide.ui.components.NeoGlideWidgetHost
import com.samidevstudio.neoglide.ui.components.WidgetContextMenu
import com.samidevstudio.neoglide.ui.components.WidgetPickerDialog
import com.samidevstudio.neoglide.ui.drawer.DrawerScreen
import com.samidevstudio.neoglide.ui.settings.SettingsSheet
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.LayoutManager
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as hiltViewModelV2

data class RectBounds(
    val row: Float,
    val col: Float,
    val spanX: Float,
    val spanY: Float,
)

private fun getLiftScale(spanX: Float, spanY: Float): Float {
    val area = spanX * spanY
    return 1.0f + (0.2f / kotlin.math.sqrt(area.toDouble()).toFloat()).coerceAtMost(0.2f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModelV2(),
    settingsViewModel: com.samidevstudio.neoglide.ui.settings.SettingsViewModel = hiltViewModelV2(),
    sharedTransitionScope: SharedTransitionScope,
    drawerViewModel: com.samidevstudio.neoglide.ui.drawer.DrawerViewModel = hiltViewModelV2(),
) {
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val hapticFeedback = rememberHapticFeedback(preferences)
    val shouldShowDefaultPrompt by viewModel.shouldShowDefaultPrompt.collectAsStateWithLifecycle()
    val isSplashScreenFinished by viewModel.isSplashScreenFinished.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDrawer by remember { mutableStateOf(value = false) }
    var showSettings by remember { mutableStateOf(value = false) }
    var showContextMenu by remember { mutableStateOf(value = false) }
    var showWidgetMenu by remember { mutableStateOf(value = false) }
    var showFolderMenu by remember { mutableStateOf(value = false) }
    var showFolderMenuId by remember { mutableIntStateOf(-1) }
    var showFolderMenuLabel by remember { mutableStateOf("") }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var editingWidgetId by remember { mutableIntStateOf(-1) }
    var showAppPicker by remember { mutableStateOf(value = false) }
    var showWidgetPicker by remember { mutableStateOf(value = false) }
    var pendingAddAppRow by remember { mutableFloatStateOf(0f) }
    var pendingAddAppCol by remember { mutableFloatStateOf(0f) }
    var pendingFolderIdForAdd by remember { mutableIntStateOf(-1) }
    var gridCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var meshCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }

    var expandedFolderId by remember { mutableIntStateOf(-1) }
    var autoFocusFolderName by remember { mutableStateOf(value = false) }
    var draggingAppFromFolder by remember { mutableStateOf<AppModel?>(null) }

    // TRACK ORIGINAL POSITION FOR DRAG-FIRST LOGIC
    var originalRow by remember { mutableFloatStateOf(-1f) }
    var originalCol by remember { mutableFloatStateOf(-1f) }

    var showAppMenuPackage by remember { mutableStateOf<String?>(null) }
    var showAppMenuId by remember { mutableIntStateOf(-1) }
    var appMenuShortcuts by remember { mutableStateOf<List<com.samidevstudio.neoglide.domain.model.AppShortcut>>(emptyList()) }
    var appMenuLabel by remember { mutableStateOf("") }

    // DRAG STATE FOR ICON REPOSITIONING
    var draggingUniqueKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var grabPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragTargetBounds by remember { mutableStateOf<RectBounds?>(null) }
    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(value = false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val refreshTrigger by viewModel.refreshTrigger.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (showDrawer && viewModel.shouldCloseDrawerOnReturn()) {
                    drawerViewModel.resetState()
                    showDrawer = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    val homeAlpha by animateFloatAsState(
        targetValue = if (showDrawer) 0f else 1f,
        animationSpec = tween(300),
        label = "homeAlpha",
    )

    LaunchedEffect(isSplashScreenFinished) {
        if (isSplashScreenFinished) {
            viewModel.checkDefaultLauncher()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is UiEvent.FolderCreated -> {
                    expandedFolderId = event.folderId
                    autoFocusFolderName = true
                }
            }
        }
    }

    BackHandler(enabled = (showDrawer || showSettings || showContextMenu || showFolderMenu || (editingWidgetId != -1) || (showAppMenuPackage != null) || (expandedFolderId != -1))) {
        if (showWidgetMenu) {
            showWidgetMenu = false
        } else if (showFolderMenu) {
            showFolderMenu = false
            showFolderMenuId = -1
        } else if (expandedFolderId != -1) {
            expandedFolderId = -1
            autoFocusFolderName = false
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

    val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
    val containerSize = windowInfo.containerSize
    val screenWidthDp = with(density) { containerSize.width.toDp() }
    val screenHeightDp = with(density) { containerSize.height.toDp() }

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
            visible = (editingWidgetId != -1) || showFolderMenu || (draggingUniqueKey != null) || (expandedFolderId != -1),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
                        FrostedGlass(
                            modifier = Modifier.fillMaxSize(),
                            blurRadius = 40.dp,
                            tintColor = Color.White.copy(alpha = 0.5f)
                        )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues -> 
            @Suppress("UNUSED_VARIABLE")
            val _ignore = paddingValues 
            
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { gridCoords = it }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    editingWidgetId = -1
                                    showWidgetMenu = false
                                    showAppMenuPackage = null
                                    focusManager.clearFocus()
                                }
                            )
                        }
                ) {
                    val layoutConfig = LayoutManager.calculateConfig(screenWidthDp, screenHeightDp, preferences.gridSize)
                    val columns = layoutConfig.columns
                    val maxRows = layoutConfig.rows
                    val totalColumns = layoutConfig.totalColumns
                    val totalRows = layoutConfig.totalRows
                    val unitWidth = layoutConfig.unitWidth
                    val unitHeight = layoutConfig.unitHeight
                    val iconSize = layoutConfig.iconSize
                    val fontSize = layoutConfig.fontSize
                    
                    val unitWidthPx = with(density) { unitWidth.toPx() }
                    val unitHeightPx = with(density) { unitHeight.toPx() }

                    val dockRow = LayoutManager.getDockRow(totalRows)
                    val showLabels = (preferences.appLabelMode == AppLabelMode.HOME_ONLY) || (preferences.appLabelMode == AppLabelMode.BOTH)

                    val draggingItem = remember(draggingUniqueKey, homeItems) {
                        homeItems.find { it.uniqueKey == draggingUniqueKey }
                    }
                    val isLifting = (draggingUniqueKey != null || draggingAppFromFolder != null)

                    // 1. THE TRAY (Offset for Optical Balance: 48dp Top, 16dp Bottom)
                    Box(
                        modifier = Modifier
                            .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxSize()
                    ) {
                        if (isLifting || editingWidgetId != -1) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f))
                            ) {}
                        }

                        // 2. THE MESH BOX (Centered inside Tray)
                        Box(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .size(width = layoutConfig.actualGridWidth, height = layoutConfig.actualGridHeight)
                                .onGloballyPositioned { meshCoords = it }
                                .graphicsLayer { clip = true }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { offset ->
                                            if ((editingWidgetId == -1) && (!preferences.lockLayout)) {
                                                hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                val meshInWindow = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                                                contextMenuOffset = DpOffset(
                                                    x = with(density) { (meshInWindow.x + offset.x).toDp() },
                                                    y = with(density) { (meshInWindow.y + offset.y).toDp() }
                                                )
                                                showContextMenu = true
                                            } else if (preferences.lockLayout) {
                                                Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // LOCAL COORDINATES (0,0 is top-left of this Mesh Box)
                            
                            fun calculateTargetBounds(fingerPosition: androidx.compose.ui.geometry.Offset, spanX: Float = 1f, spanY: Float = 1f): RectBounds? {
                                val meshOffset = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                                val localX = fingerPosition.x - meshOffset.x
                                val localY = fingerPosition.y - meshOffset.y
                                
                                val meshWidthPx = with(density) { layoutConfig.actualGridWidth.toPx() }
                                val meshHeightPx = with(density) { layoutConfig.actualGridHeight.toPx() }

                                if (localX < 0 || localX > meshWidthPx || localY < 0 || localY > meshHeightPx) {
                                    return null
                                }

                                val snapFactor = LayoutManager.SNAP_FACTOR
                                val offsetW = layoutConfig.expansionOffsetW
                                val offsetH = layoutConfig.expansionOffsetH
                                
                                val rawColRelative = (localX / unitWidthPx) - (spanX / 2f)
                                val rawRowRelative = (localY / unitHeightPx) - (spanY / 2f)
                                
                                val snappedCol = kotlin.math.round((rawColRelative - offsetW) * snapFactor) / snapFactor
                                val snappedRow = kotlin.math.round((rawRowRelative - offsetH) * snapFactor) / snapFactor

                                return RectBounds(
                                    snappedRow.coerceIn(-offsetH, (totalRows - spanY - offsetH).coerceAtLeast(-offsetH)),
                                    snappedCol.coerceIn(-offsetW, (totalColumns - spanX - offsetW).coerceAtLeast(-offsetW)),
                                    spanX,
                                    spanY
                                )
                            }

                            // 3. THE CORE BOX (Aligned to bold lines)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(
                                        x = layoutConfig.unitWidth * layoutConfig.expansionOffsetW,
                                        y = layoutConfig.unitHeight * layoutConfig.expansionOffsetH
                                    )
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().offset(x = -layoutConfig.unitWidth * layoutConfig.expansionOffsetW, y = -layoutConfig.unitHeight * layoutConfig.expansionOffsetH)) {
                                    val snapFactor = LayoutManager.SNAP_FACTOR
                                    val gridWidthPx = size.width
                                    val gridHeightPx = size.height
                                    val minorStepXPx = unitWidthPx / snapFactor
                                    val minorStepYPx = unitHeightPx / snapFactor
                                    
                                    for (i in 0..(totalColumns * snapFactor).roundToInt()) {
                                        val x = i * minorStepXPx
                                        if (x > gridWidthPx + 0.5f) continue
                                        drawLine(Color.Black.copy(alpha = 0.12f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, gridHeightPx), 1f)
                                    }
                                    for (i in 0..(totalRows * snapFactor).roundToInt()) {
                                        val y = i * minorStepYPx
                                        if (y > gridHeightPx + 0.5f) continue
                                        drawLine(Color.Black.copy(alpha = 0.12f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(gridWidthPx, y), 1f)
                                    }

                                    val offX = layoutConfig.expansionOffsetW * unitWidthPx
                                    val offY = layoutConfig.expansionOffsetH * unitHeightPx
                                    for (i in 0..5) {
                                        val x = offX + (i * unitWidthPx)
                                        if (x > gridWidthPx + 0.5f) continue
                                        drawLine(Color.Black.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, gridHeightPx), 2f)
                                    }
                                    for (i in 0..floor(totalRows).toInt()) {
                                        val y = offY + (i * unitHeightPx)
                                        if (y > gridHeightPx + 0.5f) continue
                                        drawLine(Color.Black.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(gridWidthPx, y), 2f)
                                    }

                                    drawLine(Color.Black.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(gridWidthPx, 0f), androidx.compose.ui.geometry.Offset(gridWidthPx, gridHeightPx), 2f)
                                    drawLine(Color.Black.copy(alpha = 0.35f), androidx.compose.ui.geometry.Offset(0f, gridHeightPx), androidx.compose.ui.geometry.Offset(gridWidthPx, gridHeightPx), 2f)
                                }

                                val ghostTargetX by animateDpAsState(targetValue = dragTargetBounds?.let { unitWidth * it.col } ?: 0.dp, label = "ghostX")
                                val ghostTargetY by animateDpAsState(targetValue = dragTargetBounds?.let { unitHeight * it.row } ?: 0.dp, label = "ghostY")
                                val ghostTargetWidth by animateDpAsState(targetValue = dragTargetBounds?.let { unitWidth * it.spanX } ?: 0.dp, label = "ghostW")
                                val ghostTargetHeight by animateDpAsState(targetValue = dragTargetBounds?.let { unitHeight * it.spanY } ?: 0.dp, label = "ghostH")

                                if (draggingUniqueKey != null || draggingAppFromFolder != null || (editingWidgetId != -1 && dragTargetBounds != null)) {
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(ghostTargetX.toPx().roundToInt(), ghostTargetY.toPx().roundToInt()) }
                                            .size(ghostTargetWidth, ghostTargetHeight)
                                            .padding(4.dp)
                                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    )
                                }

                                homeItems.forEach { item ->
                                    key(item.uniqueKey) {
                                        val isDragging = draggingUniqueKey == item.uniqueKey
                                        val rowToUse = if (item.row >= 99f) dockRow else item.row
                                        Box(
                                            modifier = Modifier
                                                .offset(x = unitWidth * item.column, y = unitHeight * rowToUse)
                                                .size(unitWidth * item.spanX, unitHeight * item.spanY)
                                                .graphicsLayer { alpha = if (isDragging) 0f else 1f }
                                                .pointerInput(item.id, item.row, item.column) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            if (!preferences.lockLayout) {
                                                                hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                                isDragConfirmed = false
                                                                accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                                showAppMenuPackage = null
                                                                originalRow = item.row
                                                                originalCol = item.column
                                                                grabPoint = offset
                                                                val meshInWindow = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                                                                dragOffset = androidx.compose.ui.geometry.Offset(
                                                                    meshInWindow.x + (layoutConfig.expansionOffsetW + item.column) * unitWidthPx,
                                                                    meshInWindow.y + (layoutConfig.expansionOffsetH + rowToUse) * unitHeightPx
                                                                )
                                                                dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, item.spanX, item.spanY)
                                                            }
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            if (!preferences.lockLayout) {
                                                                change.consume()
                                                                accumulatedDrag += dragAmount
                                                                dragOffset += dragAmount
                                                                if (!isDragConfirmed && accumulatedDrag.getDistance() > 10.dp.toPx()) {
                                                                    isDragConfirmed = true
                                                                    hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                                    draggingUniqueKey = item.uniqueKey
                                                                }
                                                                dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, item.spanX, item.spanY)
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (isDragConfirmed) {
                                                                hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                                dragTargetBounds?.let { viewModel.updateItemPosition(item, it.row, it.col, maxRows) }
                                                            } else {
                                                                appMenuLabel = if (item is HomeItem.App) item.appModel.label else ""
                                                                showAppMenuId = item.id
                                                                val meshInWindow = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                                                                contextMenuOffset = DpOffset(x = with(density) { (meshInWindow.x + (layoutConfig.expansionOffsetW + item.column) * unitWidthPx).toDp() }, y = with(density) { (meshInWindow.y + (layoutConfig.expansionOffsetH + rowToUse) * unitHeightPx + unitHeightPx).toDp() })
                                                                if (item is HomeItem.App) coroutineScope.launch {
                                                                    appMenuShortcuts = viewModel.getShortcuts(item.appModel.packageName)
                                                                    showAppMenuPackage = item.appModel.packageName
                                                                }
                                                            }
                                                            draggingUniqueKey = null
                                                            dragTargetBounds = null
                                                        }
                                                    )
                                                }
                                        ) {
                                            when (item) {
                                                is HomeItem.App -> AppItem(app = item.appModel, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, refreshTrigger = refreshTrigger, onClick = { if (draggingUniqueKey == null) viewModel.launchApp(item.appModel.packageName) })
                                                is HomeItem.Folder -> FolderItem(label = item.label, apps = item.apps, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, onClick = { if (draggingUniqueKey == null) expandedFolderId = item.id })
                                                is HomeItem.Widget -> NeoGlideWidgetHost(widgetId = item.id, appWidgetHost = viewModel.appWidgetHost, appWidgetManager = viewModel.appWidgetManager, row = rowToUse, column = item.column, spanX = item.spanX, spanY = item.spanY, columns = columns, maxRows = maxRows, unitWidth = unitWidth, unitHeight = unitHeight, isEditing = editingWidgetId == item.id, modifier = Modifier.zIndex(if (editingWidgetId == item.id) 1f else 0f), onDragStart = { editingWidgetId = item.id }, onInteractionUpdate = { r, c, sx, sy -> dragTargetBounds = RectBounds(r, c, sx, sy) }, onResize = { nr, nc, nsx, nsy -> viewModel.updateWidgetBounds(item.id, nr, nc, nsx, nsy, maxRows); editingWidgetId = -1; dragTargetBounds = null })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isLifting && draggingItem != null) {
                        val liftScaleOverlay by animateFloatAsState(targetValue = getLiftScale(draggingItem.spanX, draggingItem.spanY), animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "liftScale")
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                                .size(unitWidth * draggingItem.spanX, unitHeight * draggingItem.spanY)
                                .zIndex(110f)
                                .graphicsLayer {
                                    alpha = 0.8f
                                    scaleX = liftScaleOverlay
                                    scaleY = liftScaleOverlay
                                    shadowElevation = 16.dp.toPx()
                                    shape = RoundedCornerShape(16.dp)
                                    clip = true
                                }
                        ) {
                            when (draggingItem) {
                                is HomeItem.App -> AppItem(app = draggingItem.appModel, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, refreshTrigger = refreshTrigger, onClick = {})
                                is HomeItem.Folder -> FolderItem(label = draggingItem.label, apps = draggingItem.apps, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, onClick = {})
                                else -> {}
                            }
                        }
                    }

                    HomeContextMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }, offset = contextMenuOffset, onOpenWidgets = { showWidgetPicker = true }, onAddApp = { showAppPicker = true }, onOpenLauncherSettings = { showSettings = true })
                    if (showAppMenuPackage != null) AppContextMenu(expanded = true, onDismissRequest = { showAppMenuPackage = null }, packageName = showAppMenuPackage!!, label = appMenuLabel, shortcuts = appMenuShortcuts, offset = contextMenuOffset, onShortcutClick = { viewModel.launchShortcut(it) }, onHideToggle = { viewModel.hideApp(showAppMenuPackage!!) }, onRemove = { viewModel.removeHomeApp(showAppMenuId) })
                    
                    val expandedFolder = remember(homeItems, expandedFolderId) { homeItems.find { it.id == expandedFolderId && it is HomeItem.Folder } as? HomeItem.Folder }
                    if (expandedFolder != null) FolderExpansion(folderId = expandedFolder.id, label = expandedFolder.label, apps = expandedFolder.apps, unitWidth = unitWidth, unitHeight = unitHeight, iconSize = iconSize, fontSize = fontSize, onDismiss = { expandedFolderId = -1 }, onDissolve = { viewModel.removeFolder(expandedFolder.id) }, onAddApps = { pendingFolderIdForAdd = expandedFolder.id; showAppPicker = true }, onLabelChange = { viewModel.updateFolderLabel(expandedFolder.id, it) }, onAppClick = { pkg, opts -> viewModel.launchApp(pkg, opts); expandedFolderId = -1 }, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, useMonochrome = preferences.useMonochromeIcons, refreshTrigger = refreshTrigger, onHapticFeedback = hapticFeedback, getShortcuts = { viewModel.getShortcuts(it) }, onShortcutClick = { viewModel.launchShortcut(it) }, onHideToggle = { viewModel.hideApp(it) }, onAppDragStart = { app, startPos, grab -> draggingAppFromFolder = app; dragOffset = startPos; grabPoint = grab; isDragConfirmed = true }, onAppDrag = { dragOffset += it }, onAppDragEnd = { if (isDragConfirmed) { dragTargetBounds?.let { viewModel.removeAppFromFolder(expandedFolder.id, draggingAppFromFolder!!.packageName, it.row, it.col) } }; draggingAppFromFolder = null; dragTargetBounds = null; expandedFolderId = -1 }, onAppDragCancel = { draggingAppFromFolder = null; expandedFolderId = -1 })

                    if (showAppPicker) AppPickerDialog(title = "Add Application", apps = viewModel.availableAppsForPicker.collectAsStateWithLifecycle().value, recentlyUsedApps = viewModel.recentlyUsedApps.collectAsStateWithLifecycle().value, onAppSelected = { viewModel.addHomeApp(it.packageName, pendingAddAppRow, pendingAddAppCol); showAppPicker = false }, onDismissRequest = { showAppPicker = false })
                    if (showWidgetPicker) WidgetPickerDialog(appsWithWidgets = viewModel.appsWithWidgets.collectAsStateWithLifecycle().value, allWidgetProviders = viewModel.appWidgetManager.installedProviders, unitWidthDp = unitWidth.value, unitHeightDp = unitHeight.value, maxColumns = columns, onGetWidgets = { viewModel.getWidgetsForApp(it) }, onWidgetSelected = { info -> showWidgetPicker = false; val id = viewModel.allocateWidgetId(); if (viewModel.appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider)) viewModel.completeWidgetConfiguration(id) }, onDismissRequest = { showWidgetPicker = false })
                }
            }
        }

        if (shouldShowDefaultPrompt) DefaultLauncherDialog(onSetDefault = { viewModel.openDefaultLauncherSettings() }, onDismiss = { viewModel.dismissDefaultPrompt() })
        if (showSettings) SettingsSheet(onDismiss = { showSettings = false }, viewModel = settingsViewModel)

        AnimatedVisibility(visible = showDrawer, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                DrawerScreen(viewModel = drawerViewModel, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = this@AnimatedVisibility, onAppClick = { pkg, opts -> viewModel.recordDrawerAppLaunch(); viewModel.launchApp(pkg, opts) }, onShortcutClick = { viewModel.recordDrawerAppLaunch(); viewModel.launchShortcut(it) })
            }
        }
    }
}

@Composable
fun DefaultLauncherDialog(onSetDefault: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), title = { Text("Set NeoGlide as default?") }, text = { Text("NeoGlide is not your default launcher. Set it as default for the best experience.") }, confirmButton = { TextButton(onClick = onSetDefault) { Text("Set as default") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("No thanks") } })
}

private fun androidx.compose.ui.layout.LayoutCoordinates.positionInWindow(): androidx.compose.ui.geometry.Offset = this.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
