package com.samidevstudio.neoglide.ui.home

import android.widget.Toast
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as hiltViewModelV2
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import kotlinx.coroutines.launch
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.data.repository.NotificationDotMode
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.components.*
import com.samidevstudio.neoglide.ui.drawer.DrawerScreen
import com.samidevstudio.neoglide.ui.settings.SettingsSheet
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback

data class RectBounds(
    val row: Float,
    val col: Float,
    val spanX: Float,
    val spanY: Float
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
    var showWidgetPicker by remember { mutableStateOf(false) }
    var pendingAddAppRow by remember { mutableFloatStateOf(0f) }
    var pendingAddAppCol by remember { mutableFloatStateOf(0f) }
    var pendingFolderIdForAdd by remember { mutableIntStateOf(-1) }
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
    var appMenuShortcuts by remember { mutableStateOf<List<com.samidevstudio.neoglide.domain.model.AppShortcut>>(emptyList()) }
    var appMenuLabel by remember { mutableStateOf("") }

    // DRAG STATE FOR ICON REPOSITIONING
    var draggingUniqueKey by remember { mutableStateOf<String?>(null) }
    var hoveredUniqueKey by remember { mutableStateOf<String?>(null) }
    var blockedUniqueKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var grabPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragTargetBounds by remember { mutableStateOf<RectBounds?>(null) }
    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val homeItems by viewModel.homeItems.collectAsStateWithLifecycle()
    val refreshTrigger by viewModel.refreshTrigger.collectAsStateWithLifecycle()

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
            visible = editingWidgetId != -1 || showFolderMenu || draggingUniqueKey != null || expandedFolderId != -1,
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
                    val unitWidth = maxWidth / 5f
                    val topOffset = 0.dp // Use padding from Scaffold
                    // val bottomPadding = 0.dp // Use padding from Scaffold
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
                    fun calculateTargetBounds(fingerPosition: androidx.compose.ui.geometry.Offset, spanX: Float = 1f, spanY: Float = 1f): RectBounds {
                        // Use item's logical top-left (finger - grabPoint) for grid mapping to avoid "lean"
                        val itemTopLeft = fingerPosition - grabPoint
                        
                        val rawRow = (((itemTopLeft.y - topOffsetPx) / unitHeightPx) * 2).roundToInt() / 2f
                            .coerceIn(0f, (maxRows - spanY).coerceAtLeast(0f))
                        val rawCol = ((itemTopLeft.x / unitWidthPx) * 2).roundToInt() / 2f
                            .coerceIn(0f, (5f - spanX).coerceAtLeast(0f))
                        
                        return RectBounds(rawRow, rawCol, spanX, spanY)
                    }

                    LaunchedEffect(dragOffset, grabPoint, draggingUniqueKey, draggingAppFromFolder, isDragConfirmed) {
                        if ((draggingUniqueKey != null || draggingAppFromFolder != null) && isDragConfirmed) {
                            val fingerPosition = dragOffset + grabPoint
                            
                            val rawRow = (fingerPosition.y - topOffsetPx) / unitHeightPx
                            val rawCol = fingerPosition.x / unitWidthPx

                            // Find dragging item's span to calculate bounds correctly
                            val draggingItem = homeItems.find { it.uniqueKey == draggingUniqueKey }
                            val spanX = draggingItem?.spanX ?: 1f
                            val spanY = draggingItem?.spanY ?: 1f

                            val bounds = calculateTargetBounds(fingerPosition, spanX, spanY)
                            val isCellJump = bounds.row != dragTargetBounds?.row || bounds.col != dragTargetBounds?.col
                            
                            val sourceFolderKey = if (sourceFolderId != -1) "FOLDER_$sourceFolderId" else null

                            val target = homeItems.find { item ->
                                // Skip self/dragging item
                                if (item.uniqueKey == draggingUniqueKey) return@find false
                                // Skip source folder
                                if (item.uniqueKey == sourceFolderKey) return@find false
                                
                                // Center-to-center distance check
                                val targetCenterX = item.column + item.spanX / 2f
                                val targetCenterY = item.row + item.spanY / 2f
                                
                                val distSq = (rawRow - targetCenterY) * (rawRow - targetCenterY) + 
                                           (rawCol - targetCenterX) * (rawCol - targetCenterX)
                                           
                                // Tighter radius for standard items (1x1), slightly larger for widgets
                                val radiusSq = if (item is HomeItem.Widget) 0.36f else 0.16f
                                distSq < radiusSq
                            }
                            
                            val isHoverChange = target?.uniqueKey != hoveredUniqueKey
                            
                            if (isHoverChange) {
                                if (target != null) {
                                    hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                }
                                hoveredUniqueKey = target?.uniqueKey
                            } else if (isCellJump && hoveredUniqueKey == null) {
                                // Only tick for grid snap if we are NOT currently hovering over a merge target
                                hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                            }
                            
                            dragTargetBounds = bounds
                        } else {
                            hoveredUniqueKey = null
                            if (draggingUniqueKey == null && draggingAppFromFolder == null) {
                                dragTargetBounds = null
                            }
                        }
                    }

                    // OBSTRUCTION DETECTION (Soft Red Indicator)
                    LaunchedEffect(dragTargetBounds, draggingUniqueKey, editingWidgetId, hoveredUniqueKey) {
                        val bounds = dragTargetBounds
                        if (bounds != null) {
                            val activeUniqueKey = draggingUniqueKey ?: if (editingWidgetId != -1) "WIDGET_$editingWidgetId" else null
                            val sourceFolderKey = if (sourceFolderId != -1) "FOLDER_$sourceFolderId" else null

                            blockedUniqueKeys = homeItems.asSequence()
                                .filter { it.uniqueKey != activeUniqueKey && it.uniqueKey != sourceFolderKey }
                                .filter { item ->
                                    // Check if item's rect intersects with dragTargetBounds
                                    val itemRect = android.graphics.RectF(item.column, item.row, item.column + item.spanX, item.row + item.spanY)
                                    val shadowRect = android.graphics.RectF(bounds.col, bounds.row, bounds.col + bounds.spanX, bounds.row + bounds.spanY)
                                    
                                    android.graphics.RectF.intersects(itemRect, shadowRect)
                                }
                                .map { it.uniqueKey }
                                .filter { it != hoveredUniqueKey } // Don't turn the merge target red
                                .toSet()
                        } else {
                            blockedUniqueKeys = emptySet()
                        }
                    }

                    // UNIFIED DRAGGING OVERLAY
                    val draggingItem = remember(draggingUniqueKey, homeItems) {
                        homeItems.find { it.uniqueKey == draggingUniqueKey }
                    }
                    val isLifting = draggingUniqueKey != null || draggingAppFromFolder != null
                    val liftScaleOverlay by animateFloatAsState(
                        targetValue = if (isLifting) {
                            val spanX = draggingItem?.spanX ?: 1f
                            val spanY = draggingItem?.spanY ?: 1f
                            getLiftScale(spanX, spanY)
                        } else 1f,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "liftScaleOverlay"
                    )
                    val liftShadowOverlay by animateDpAsState(
                        targetValue = if (isLifting) 16.dp else 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "liftShadowOverlay"
                    )

                    if (isLifting) {
                        val overlayWidth = draggingItem?.let { unitWidth * it.spanX } ?: unitWidth
                        val overlayHeight = draggingItem?.let { unitHeight * it.spanY } ?: unitHeight
                        
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                }
                                .size(overlayWidth, overlayHeight)
                                .zIndex(110f)
                                .graphicsLayer {
                                    scaleX = liftScaleOverlay
                                    scaleY = liftScaleOverlay
                                    val sizeX = with(density) { overlayWidth.toPx() }
                                    val sizeY = with(density) { overlayHeight.toPx() }
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                        grabPoint.x / sizeX,
                                        grabPoint.y / sizeY
                                    )
                                    shadowElevation = liftShadowOverlay.toPx()
                                    shape = RoundedCornerShape(16.dp)
                                    clip = true
                                }
                        ) {
                            if (draggingAppFromFolder != null) {
                                AppItem(
                                    app = draggingAppFromFolder!!,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    useMonochrome = preferences.useMonochromeIcons,
                                    iconPackPackageName = preferences.iconPackPackageName,
                                    showLabel = false,
                                    sharedElementKeyPrefix = "dragging-folder",
                                    isLongClickEnabled = false,
                                    refreshTrigger = refreshTrigger,
                                    onClick = {}
                                )
                            } else if (draggingItem != null) {
                                when (draggingItem) {
                                    is HomeItem.App -> {
                                        AppItem(
                                            app = draggingItem.appModel,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            useMonochrome = preferences.useMonochromeIcons,
                                            iconPackPackageName = preferences.iconPackPackageName,
                                            showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                            sharedElementKeyPrefix = "dragging-home",
                                            isLongClickEnabled = false,
                                            refreshTrigger = refreshTrigger,
                                            onClick = {}
                                        )
                                    }
                                    is HomeItem.Folder -> {
                                        FolderItem(
                                            label = draggingItem.label,
                                            apps = draggingItem.apps,
                                            useMonochrome = preferences.useMonochromeIcons,
                                            showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                            onClick = {}
                                        )
                                    }
                                    is HomeItem.Widget -> {
                                        // Widgets currently handle their own drag state visually via NeoGlideWidgetHost, 
                                        // but we could unify them here too in future if needed.
                                        // For now, we skip them in the overlay to avoid double rendering.
                                    }
                                }
                            }
                        }
                    }


                    // GRID OVERLAY
                    if (editingWidgetId != -1 || draggingUniqueKey != null || draggingAppFromFolder != null) {
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
                    val ghostTargetX by animateDpAsState(
                        targetValue = dragTargetBounds?.let { unitWidth * it.col } ?: 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "ghostX"
                    )
                    val ghostTargetY by animateDpAsState(
                        targetValue = dragTargetBounds?.let { topOffset + (unitHeight * it.row) } ?: 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "ghostY"
                    )
                    val ghostTargetWidth by animateDpAsState(
                        targetValue = dragTargetBounds?.let { unitWidth * it.spanX } ?: 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "ghostWidth"
                    )
                    val ghostTargetHeight by animateDpAsState(
                        targetValue = dragTargetBounds?.let { unitHeight * it.spanY } ?: 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "ghostHeight"
                    )

                    if (draggingUniqueKey != null || draggingAppFromFolder != null || (editingWidgetId != -1 && dragTargetBounds != null)) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        ghostTargetX.toPx().roundToInt(),
                                        ghostTargetY.toPx().roundToInt()
                                    )
                                }
                                .size(ghostTargetWidth, ghostTargetHeight)
                                .padding(8.dp)
                                .background((if (isSystemInDarkTheme()) Color.White else Color.Black).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        )
                    }

                    homeItems.forEach { item ->
                        var itemCoords by remember(item.uniqueKey) { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                        
                        when (item) {
                            is HomeItem.App -> {
                                val app = item.appModel
                                val isDragging = draggingUniqueKey == item.uniqueKey
                                
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { itemCoords = it }
                                        .offset {
                                            val adaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                            androidx.compose.ui.unit.IntOffset(
                                                (unitWidthPx * item.column).roundToInt(),
                                                (topOffsetPx + unitHeightPx * adaptiveRow).roundToInt()
                                            )
                                        }
                                        .size(unitWidth, unitHeight)
                                        .graphicsLayer {
                                            alpha = if (isDragging) 0f else 1f
                                        }
                                        .pointerInput(item.id, item.row, item.column) {
                                            if (!preferences.lockLayout) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                        isDragConfirmed = false
                                                        
                                                        showAppMenuPackage = null // Hide menu on drag start
                                                        originalRow = item.row
                                                        originalCol = item.column

                                                        // Use Bit-Perfect Initialization for Zero-Jump
                                                        grabPoint = offset
                                                        val initialAdaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                                            unitWidthPx * item.column,
                                                            topOffsetPx + unitHeightPx * initialAdaptiveRow
                                                        )
                                                        
                                                        dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, 1f, 1f)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingUniqueKey = item.uniqueKey
                                                        }

                                                        // Logic handled by LaunchedEffect
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
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    },
                                                    onDragCancel = {
                                                        draggingUniqueKey = null
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
                                        isHovered = hoveredUniqueKey == item.uniqueKey,
                                        isBlocked = item.uniqueKey in blockedUniqueKeys,
                                        sharedElementKeyPrefix = "home",
                                        isLongClickEnabled = false,
                                        refreshTrigger = refreshTrigger,
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
                                        if (draggingUniqueKey == null) {
                                            viewModel.launchApp(app.packageName, options)
                                        }
                                    }
                                }
                            }
                            is HomeItem.Folder -> {
                                val isDragging = draggingUniqueKey == item.uniqueKey
                                
                                Box(
                                    modifier = Modifier
                                        .onGloballyPositioned { itemCoords = it }
                                        .offset {
                                            val adaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                            androidx.compose.ui.unit.IntOffset(
                                                (unitWidthPx * item.column).roundToInt(),
                                                (topOffsetPx + unitHeightPx * adaptiveRow).roundToInt()
                                            )
                                        }
                                        .size(unitWidth, unitHeight)
                                        .graphicsLayer {
                                            alpha = if (isDragging) 0f else 1f
                                        }
                                        .pointerInput(item.id, item.row, item.column) {
                                            if (!preferences.lockLayout) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                        isDragConfirmed = false
                                                        
                                                        originalRow = item.row
                                                        originalCol = item.column

                                                        // Use Bit-Perfect Initialization for Zero-Jump
                                                        grabPoint = offset
                                                        val initialAdaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                                            unitWidthPx * item.column,
                                                            topOffsetPx + unitHeightPx * initialAdaptiveRow
                                                        )

                                                        dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, 1f, 1f)
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingUniqueKey = item.uniqueKey
                                                        }

                                                        // Logic handled by LaunchedEffect
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
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    },
                                                    onDragCancel = {
                                                        draggingUniqueKey = null
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
                                        isHovered = hoveredUniqueKey == item.uniqueKey,
                                        isBlocked = item.uniqueKey in blockedUniqueKeys,
                                        onHapticFeedback = hapticFeedback,
                                        onClick = {
                                            if (draggingUniqueKey == null) {
                                                expandedFolderId = item.id
                                            }
                                        }
                                    )
                                }
                            }
                            is HomeItem.Widget -> {
                                val isCurrentEditing = editingWidgetId == item.id
                            

                                NeoGlideWidgetHost(
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
                                    isBlocked = item.uniqueKey in blockedUniqueKeys,
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
                                        viewModel.updateWidgetBounds(
                                            item.id,
                                            newRow.coerceIn(0f, (maxRows - newSpanY).coerceAtLeast(0f)),
                                            newCol.coerceIn(0f, (4f - newSpanX).coerceAtLeast(0f)),
                                            newSpanX,
                                            newSpanY
                                        )
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
                            // Convert contextMenuOffset to grid coordinates
                            val xPx = with(density) { contextMenuOffset.x.toPx() }
                            val yPx = with(density) { (contextMenuOffset.y - topOffset).toPx() }
                            val unitWidthPx = with(density) { unitWidth.toPx() }
                            val unitHeightPx = with(density) { unitHeight.toPx() }
                            
                            val col = (xPx / unitWidthPx).toInt().toFloat().coerceIn(0f, 4f)
                            val row = (yPx / unitHeightPx).toInt().toFloat().coerceIn(0f, maxRows.toFloat() - 1f)
                            
                            viewModel.setPendingWidgetPosition(row, col)
                            showWidgetPicker = true
                        },
                        onAddApp = {
                            // Convert the contextMenuOffset (which is in Dp) to grid coordinates
                            val xPx = with(density) { contextMenuOffset.x.toPx() }
                            val yPx = with(density) { (contextMenuOffset.y - topOffset).toPx() }

                            val unitWidthPx = with(density) { unitWidth.toPx() }
                            val unitHeightPx = with(density) { unitHeight.toPx() }

                            pendingAddAppCol = (xPx / unitWidthPx).toInt().toFloat().coerceIn(0f, 4f)
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
                                unitWidth = unitWidth,
                                unitHeight = unitHeight,
                                onDismiss = { expandedFolderId = -1 },
                                isDrawerFolder = false,
                                onDissolve = { viewModel.removeFolder(expandedFolder.id) },
                                onAddApps = {
                                    pendingFolderIdForAdd = expandedFolder.id
                                    showAppPicker = true
                                },
                                onLabelChange = { newLabel -> viewModel.updateFolderLabel(expandedFolder.id, newLabel) },
                                onAppClick = { pkg, options ->
                                    viewModel.launchApp(pkg, options)
                                    expandedFolderId = -1
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = preferences.useMonochromeIcons,
                                iconPackPackageName = preferences.iconPackPackageName,
                                refreshTrigger = refreshTrigger,
                                onHapticFeedback = hapticFeedback,
                                getShortcuts = { viewModel.getShortcuts(it) },
                                onShortcutClick = { viewModel.launchShortcut(it) },
                                onHideToggle = { pkg ->
                                    viewModel.hideApp(pkg)
                                },
                                onAppDragStart = { app, initialTopLeft, initialGrabPoint ->
                                    hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                    draggingAppFromFolder = app
                                    sourceFolderId = expandedFolder.id
                                    isDragConfirmed = true // Trigger shadow guide immediately

                                    // initialTopLeft is window-relative Top-Left of the icon
                                    val localTopLeft = gridCoords?.windowToLocal(initialTopLeft) ?: initialTopLeft
                                    dragOffset = localTopLeft
                                    grabPoint = initialGrabPoint
                                    dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint)
                                },
                                onAppDrag = { amount ->
                                    dragOffset += amount
                                    // Logic moved to LaunchedEffect(dragOffset)
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
                title = if (pendingFolderIdForAdd != -1) "Add to folder" else "Add Application",
                apps = availableApps,
                recentlyUsedApps = recentApps,
                onAppSelected = { app ->
                    if (pendingFolderIdForAdd != -1) {
                        viewModel.addAppToFolder(pendingFolderIdForAdd, app.packageName)
                    } else {
                        viewModel.addHomeApp(app.packageName, pendingAddAppRow, pendingAddAppCol)
                    }
                    showAppPicker = false
                    pendingFolderIdForAdd = -1
                },
                onDismissRequest = {
                    showAppPicker = false
                    pendingFolderIdForAdd = -1
                }
            )
        }

        if (showWidgetPicker) {
            val appsWithWidgets by viewModel.appsWithWidgets.collectAsStateWithLifecycle()
            val mainActivity = context as? com.samidevstudio.neoglide.MainActivity
            
            WidgetPickerDialog(
                appsWithWidgets = appsWithWidgets,
                allWidgetProviders = viewModel.appWidgetManager.installedProviders,
                onGetWidgets = { pkg -> viewModel.getWidgetsForApp(pkg) },
                onWidgetSelected = { info ->
                    showWidgetPicker = false
                    val widgetId = viewModel.allocateWidgetId()
                    val isBound = viewModel.appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
                    
                    if (isBound) {
                        if (info.configure != null) {
                            mainActivity?.startWidgetConfig(widgetId)
                        } else {
                            viewModel.completeWidgetConfiguration(widgetId)
                        }
                    } else {
                        // Need to request bind permission from system
                        viewModel.setPendingWidgetInfo(info)
                        mainActivity?.startWidgetBind(widgetId, info.provider)
                    }
                },
                onDismissRequest = { showWidgetPicker = false }
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
        title = { Text("Set NeoGlide as default?") },
        text = {
            Text("NeoGlide is not your default launcher. Set it as default for the best experience. You can always change it back in settings.")
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
