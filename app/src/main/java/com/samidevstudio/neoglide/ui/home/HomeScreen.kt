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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback
import kotlinx.coroutines.launch
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

    var expandedFolderId by remember { mutableIntStateOf(-1) }
    var autoFocusFolderName by remember { mutableStateOf(value = false) }
    var draggingAppFromFolder by remember { mutableStateOf<AppModel?>(null) }
    var sourceFolderId by remember { mutableIntStateOf(-1) }
    var isInvisibleByDrag by remember { mutableStateOf(value = false) }

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

    BackHandler(enabled = (showDrawer || showSettings || showContextMenu || showWidgetMenu || showFolderMenu || (editingWidgetId != -1) || (showAppMenuPackage != null) || (expandedFolderId != -1))) {
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
                                    if ((editingWidgetId == -1) && (!preferences.lockLayout)) {
                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                        contextMenuOffset = DpOffset(
                                            x = with(density) { offset.x.toDp() },
                                            y = with(density) { offset.y.toDp() }
                                        )
                                        showContextMenu = true
                                    } else if (preferences.lockLayout) {
                                        Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                ) {
                    val columns = preferences.gridSize.getColumnCount(maxWidth.value)
                    val unitWidth = maxWidth / columns.toFloat()
                    val unitHeight = unitWidth // SQUARE GRID!
                    
                    val availableHeight = maxHeight
                    val maxRows = (availableHeight / unitHeight).toInt().coerceAtLeast(1)
                    
                    // Center the grid vertically if there's extra space
                    val totalGridHeight = unitHeight * maxRows
                    val verticalPadding = (availableHeight - totalGridHeight) / 2f
                    val topOffsetPx = with(density) { verticalPadding.toPx() }

                    val unitWidthPx = with(density) { unitWidth.toPx() }
                    val unitHeightPx = with(density) { unitHeight.toPx() }

                    val showLabels = (preferences.appLabelMode == AppLabelMode.HOME_ONLY) || (preferences.appLabelMode == AppLabelMode.BOTH)

                    // DRAG LOGIC REFINEMENT: Update bounds in response to dragOffset changes
                    fun calculateTargetBounds(fingerPosition: androidx.compose.ui.geometry.Offset, spanX: Float = 1f, spanY: Float = 1f): RectBounds {
                        // Use finger position as the logical center of the snap target for better "under finger" feel
                        // This makes the ghost target follow the finger's center regardless of grab point
                        val centerX = fingerPosition.x
                        val centerY = fingerPosition.y - topOffsetPx
                        
                        // SNAP TO SUBGRID: Universal half-unit snapping (factor 2)
                        val snapFactor = 2f
                        
                        val rawCol = (((centerX / unitWidthPx) - (spanX / 2f)) * snapFactor).roundToInt() / snapFactor
                        val rawRow = (((centerY / unitHeightPx) - (spanY / 2f)) * snapFactor).roundToInt() / snapFactor
                        
                        return RectBounds(
                            rawRow.coerceIn(0f, (maxRows.toFloat() - spanY).coerceAtLeast(0f)),
                            rawCol.coerceIn(0f, (columns.toFloat() - spanX).coerceAtLeast(0f)),
                            spanX,
                            spanY
                        )
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
                                
                                // WIDGET UX FIX: Widgets cannot merge with apps/folders.
                                // Only allow hoveredUniqueKey if we are NOT dragging a widget.
                                val isDraggingWidget = draggingUniqueKey?.startsWith("WIDGET_") == true || editingWidgetId != -1
                                if (isDraggingWidget) return@find false

                                val effectiveRow = when {
                                    item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                                    item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                                    else -> item.row
                                }

                                // Center-to-center distance check
                                val targetCenterX = item.column + item.spanX / 2f
                                val targetCenterY = effectiveRow + item.spanY / 2f
                                
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
                    LaunchedEffect(dragTargetBounds, draggingUniqueKey, editingWidgetId, hoveredUniqueKey, maxRows) {
                        val bounds = dragTargetBounds
                        if (bounds != null) {
                            val activeUniqueKey = draggingUniqueKey ?: if (editingWidgetId != -1) "WIDGET_$editingWidgetId" else null
                            val sourceFolderKey = if (sourceFolderId != -1) "FOLDER_$sourceFolderId" else null

                            blockedUniqueKeys = homeItems.asSequence()
                                .filter { it.uniqueKey != activeUniqueKey && it.uniqueKey != sourceFolderKey }
                                .filter { item ->
                                    val effectiveRow = when {
                                        item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                                        item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                                        else -> item.row
                                    }

                                    // Check if item's rect intersects with dragTargetBounds
                                    val itemRect = android.graphics.RectF(item.column, effectiveRow, item.column + item.spanX, effectiveRow + item.spanY)
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

                    // RAISED CONTAINER FOR EDIT MODE
                    if (isLifting || editingWidgetId != -1) {
                        Surface(
                            modifier = Modifier
                                .offset(y = verticalPadding)
                                .fillMaxWidth()
                                .height(unitHeight * maxRows + 5.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), // Lightened alpha
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.15f)) // Added subtle black outline
                        ) {}
                    }

                    if (isLifting) {
                        val spanX = draggingItem?.spanX ?: 1f
                        val spanY = draggingItem?.spanY ?: 1f
                        
                        val overlayWidth = unitWidth * spanX
                        val overlayHeight = unitHeight * spanY
                        
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
                                }
                                .size(overlayWidth, overlayHeight)
                                .zIndex(110f)
                                .graphicsLayer {
                                    alpha = 0.8f
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
                                    onClick = { },
                                )
                            } else draggingItem?.let { item ->
                                when (item) {
                                    is HomeItem.App -> {
                                        AppItem(
                                            app = item.appModel,
                                            sharedTransitionScope = sharedTransitionScope,
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            iconSize = preferences.gridSize.iconSizeDp.dp,
                                            fontSize = preferences.gridSize.fontSizeSp.sp,
                                            useMonochrome = preferences.useMonochromeIcons,
                                            iconPackPackageName = preferences.iconPackPackageName,
                                            showLabel = showLabels,
                                            sharedElementKeyPrefix = "dragging-home",
                                            isLongClickEnabled = false,
                                            refreshTrigger = refreshTrigger,
                                            onClick = { },
                                        )
                                    }
                                    is HomeItem.Folder -> {
                                        FolderItem(
                                            label = item.label,
                                            apps = item.apps,
                                            iconSize = preferences.gridSize.iconSizeDp.dp,
                                            fontSize = preferences.gridSize.fontSizeSp.sp,
                                            useMonochrome = preferences.useMonochromeIcons,
                                            showLabel = showLabels,
                                            onClick = { },
                                        )
                                    }
                                    is HomeItem.Widget -> {
                                        // Widgets handle their own drag state visually
                                    }
                                }
                            }
                        }
                    }



                    // TOUCH BLOCKER (Scrim) - placed below the active widget but above everything else in the grid
                    if (editingWidgetId != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0.5f)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            editingWidgetId = -1
                                            showWidgetMenu = false
                                        }
                                    )
                                }
                        )
                    }

                    // GHOST TARGET VISUAL
                    val ghostTargetX by animateDpAsState(
                        targetValue = dragTargetBounds?.let { unitWidth * it.col } ?: 0.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        label = "ghostX"
                    )
                    val ghostTargetY by animateDpAsState(
                        targetValue = dragTargetBounds?.let { verticalPadding + (unitHeight * it.row) } ?: 0.dp,
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
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        )
                    }

                    homeItems.forEach { item ->
                        key(item.uniqueKey) {
                            var itemCoords by remember(item.uniqueKey) { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                            
                            val effectiveSpanY = item.spanY

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
                                            .size(unitWidth * item.spanX, unitHeight * effectiveSpanY)
                                            .graphicsLayer {
                                                alpha = if (isDragging) 0f else 1f
                                            }
                                            .pointerInput(item.id, item.row, item.column) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        if (!preferences.lockLayout) {
                                                            hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                            accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                            isDragConfirmed = false
                                                            
                                                            showAppMenuPackage = null // Hide menu on drag start
                                                            originalRow = item.row
                                                            originalCol = item.column

                                                            grabPoint = offset
                                                            val initialAdaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                                            dragOffset = androidx.compose.ui.geometry.Offset(
                                                                unitWidthPx * item.column,
                                                                topOffsetPx + unitHeightPx * initialAdaptiveRow
                                                            )
                                                            
                                                            dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, item.spanX, effectiveSpanY)
                                                        } else {
                                                            Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                onDrag = { change, dragAmount ->
                                                    if (!preferences.lockLayout) {
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingUniqueKey = item.uniqueKey
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (!preferences.lockLayout) {
                                                        if (isDragConfirmed) {
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                            dragTargetBounds?.let { bounds ->
                                                                viewModel.updateItemPosition(item, bounds.row, bounds.col, maxRows)
                                                            }
                                                        } else {
                                                            appMenuLabel = app.label
                                                            showAppMenuId = item.id
                                                            val iconLeft = (unitWidth * item.column)
                                                            val iconTop = verticalPadding + (unitHeight * item.row)

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
                                                    }
                                                },
                                                onDragCancel = {
                                                    if (!preferences.lockLayout) {
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    AppItem(
                                        app = app,
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope,
                                        iconSize = preferences.gridSize.iconSizeDp.dp,
                                        fontSize = preferences.gridSize.fontSizeSp.sp,
                                        useMonochrome = preferences.useMonochromeIcons,
                                        iconPackPackageName = preferences.iconPackPackageName,
                                        isHidden = app.packageName in preferences.hiddenPackages,
                                        hasNotification = preferences.homeBadgeStyle != BadgeStyle.NONE &&
                                                app.packageName in activeNotifications.keys,
                                        notificationCount = if (preferences.homeBadgeStyle == BadgeStyle.COUNT) {
                                            activeNotifications[app.packageName] ?: 0
                                        } else 0,
                                        showLabel = showLabels,
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
                                
                                val folderNotifCount = item.apps.sumOf { activeNotifications[it.packageName] ?: 0 }
                                val folderHasNotif = preferences.homeBadgeStyle != BadgeStyle.NONE &&
                                        item.apps.any { it.packageName in activeNotifications.keys }

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
                                        .size(unitWidth * item.spanX, unitHeight * effectiveSpanY)
                                        .graphicsLayer {
                                            alpha = if (isDragging) 0f else 1f
                                        }
                                        .pointerInput(item.id, item.row, item.column) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    if (!preferences.lockLayout) {
                                                        hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                        isDragConfirmed = false
                                                        
                                                        originalRow = item.row
                                                        originalCol = item.column

                                                        grabPoint = offset
                                                        val initialAdaptiveRow = if (item.row >= 99f) (maxRows - 1).toFloat() else item.row
                                                        dragOffset = androidx.compose.ui.geometry.Offset(
                                                            unitWidthPx * item.column,
                                                            topOffsetPx + unitHeightPx * initialAdaptiveRow
                                                        )

                                                        dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, item.spanX, effectiveSpanY)
                                                    } else {
                                                        Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    if (!preferences.lockLayout) {
                                                        change.consume()
                                                        accumulatedDrag += dragAmount
                                                        dragOffset += dragAmount

                                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                            isDragConfirmed = true
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                            draggingUniqueKey = item.uniqueKey
                                                        }
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (!preferences.lockLayout) {
                                                        if (isDragConfirmed) {
                                                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                            dragTargetBounds?.let { bounds ->
                                                                viewModel.updateItemPosition(item, bounds.row, bounds.col, maxRows)
                                                            }
                                                        } else {
                                                            showFolderMenuLabel = item.label
                                                            showFolderMenuId = item.id
                                                            val iconLeft = (unitWidth * item.column)
                                                            val iconTop = verticalPadding + (unitHeight * item.row)

                                                            val menuH = 100.dp
                                                            val gap = 8.dp

                                                            var finalY = iconTop + unitHeight + gap
                                                            if (finalY + menuH > availableHeight) {
                                                                finalY = iconTop - menuH - gap
                                                            }

                                                            contextMenuOffset = DpOffset(x = iconLeft, y = finalY)
                                                            showFolderMenu = true
                                                        }
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    }
                                                },
                                                onDragCancel = {
                                                    if (!preferences.lockLayout) {
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isDragConfirmed = false
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    FolderItem(
                                        label = item.label,
                                        apps = item.apps,
                                        iconSize = preferences.gridSize.iconSizeDp.dp,
                                        fontSize = preferences.gridSize.fontSizeSp.sp,
                                        useMonochrome = preferences.useMonochromeIcons,
                                        showLabel = showLabels,
                                        isHovered = hoveredUniqueKey == item.uniqueKey,
                                        isBlocked = item.uniqueKey in blockedUniqueKeys,
                                        hasNotification = folderHasNotif,
                                        notificationCount = if (preferences.homeBadgeStyle == BadgeStyle.COUNT) folderNotifCount else 0,
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
                                    row = when {
                                        item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                                        item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                                        else -> item.row.coerceIn(0f, (maxRows - item.spanY).coerceAtLeast(0f))
                                    },
                                    column = item.column.coerceIn(0f, (columns.toFloat() - item.spanX).coerceAtLeast(0f)),
                                    spanX = item.spanX,
                                    spanY = item.spanY,
                                    columns = columns,
                                    maxRows = maxRows,
                                    unitWidth = unitWidth,
                                    unitHeight = unitHeight,
                                    isEditing = isCurrentEditing,
                                    isBlocked = item.uniqueKey in blockedUniqueKeys,
                                    onHapticFeedback = hapticFeedback,
                                    modifier = Modifier
                                        .offset(y = verticalPadding)
                                        .zIndex(if (isCurrentEditing) 1f else 0f),
                                    onDragStart = {
                                        if (!preferences.lockLayout) {
                                            showWidgetMenu = false
                                            editingWidgetId = item.id
                                            originalRow = item.row
                                            originalCol = item.column
                                        } else {
                                            Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onResizeStart = {
                                        if (!preferences.lockLayout) {
                                            showWidgetMenu = false
                                        } else {
                                            Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onLongClick = {
                                        if (!preferences.lockLayout) {
                                            val widgetLeft = (unitWidth * item.column) + 4.dp
                                            val widgetTop = verticalPadding + (unitHeight * item.row) + 4.dp
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
                                            editingWidgetId = item.id
                                        } else {
                                            Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onInteractionUpdate = { r, c, sx, sy ->
                                        dragTargetBounds = RectBounds(r, c, sx, sy)
                                    },
                                    onResize = { newRow, newCol, newSpanX, newSpanY ->
                                        viewModel.updateWidgetBounds(
                                            item.id,
                                            newRow.coerceIn(0f, (maxRows - newSpanY).coerceAtLeast(0f)),
                                            newCol.coerceIn(0f, (columns.toFloat() - newSpanX).coerceAtLeast(0f)),
                                            newSpanX,
                                            newSpanY,
                                            maxRows
                                        )
                                        editingWidgetId = item.id
                                        dragTargetBounds = null
                                    }
                                )

                                val targetRow = when {
                                    item.row >= 99.5f -> (maxRows - 1.5f).coerceAtLeast(0f)
                                    item.row >= 99f -> (maxRows - 1f).coerceAtLeast(0f)
                                    else -> null
                                }

                                targetRow?.let { tRow ->
                                    LaunchedEffect(item.id, tRow) {
                                        viewModel.updateWidgetBounds(item.id, tRow, item.column, item.spanX, item.spanY, maxRows)
                                    }
                                }
                            }
                        }
                    }
                }

                    HomeContextMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        offset = contextMenuOffset,
                        onOpenWidgets = {
                            val xPx = with(density) { contextMenuOffset.x.toPx() }
                            val yPx = with(density) { (contextMenuOffset.y - verticalPadding).toPx() }
                            val unitWidthPx = with(density) { unitWidth.toPx() }
                            val unitHeightPx = with(density) { unitHeight.toPx() }
                            
                            val col = (xPx / unitWidthPx).toInt().toFloat().coerceIn(0f, columns.toFloat() - 1f)
                            val row = (yPx / unitHeightPx).toInt().toFloat().coerceIn(0f, maxRows.toFloat() - 1f)
                            
                            viewModel.setPendingWidgetPosition(row, col)
                            showWidgetPicker = true
                        },
                        onAddApp = {
                            val xPx = with(density) { contextMenuOffset.x.toPx() }
                            val yPx = with(density) { (contextMenuOffset.y - verticalPadding).toPx() }
                            val unitWidthPx = with(density) { unitWidth.toPx() }
                            val unitHeightPx = with(density) { unitHeight.toPx() }

                            pendingAddAppCol = (xPx / unitWidthPx).toInt().toFloat().coerceIn(0f, columns.toFloat() - 1f)
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
                                onDismiss = { 
                                    expandedFolderId = -1
                                    autoFocusFolderName = false
                                },
                                isDrawerFolder = false,
                                isLocked = preferences.lockLayout,
                                autoFocusLabel = autoFocusFolderName,
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
                                    isDragConfirmed = true

                                    val localTopLeft = gridCoords?.windowToLocal(initialTopLeft) ?: initialTopLeft
                                    dragOffset = localTopLeft
                                    grabPoint = initialGrabPoint
                                    dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint)
                                },
                                onAppDrag = { amount ->
                                    dragOffset += amount
                                },
                                onAppDragOut = { _, _, _ ->
                                    isInvisibleByDrag = true
                                },
                                onAppDragEnd = {
                                    hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                    if (isInvisibleByDrag) {
                                        dragTargetBounds?.let { bounds ->
                                            draggingAppFromFolder?.let { app ->
                                                viewModel.removeAppFromFolder(sourceFolderId, app.packageName, bounds.row, bounds.col)
                                            }
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
                                    viewModel.addHomeApp(app.packageName, pendingAddAppRow, pendingAddAppCol, maxRows)
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
                            unitWidthDp = unitWidth.value,
                            unitHeightDp = unitHeight.value,
                            maxColumns = columns,
                            onGetWidgets = { pkg -> viewModel.getWidgetsForApp(pkg) },
                            onWidgetSelected = { info ->
                                showWidgetPicker = false
                                val widgetId = viewModel.allocateWidgetId()
                                val isBound = viewModel.appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, info.provider)
                                
                                if (isBound) {
                                    val isConfigOptional = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
                                    } else false

                                    if (info.configure != null && !isConfigOptional) {
                                        mainActivity?.startWidgetConfig(widgetId)
                                    } else {
                                        viewModel.completeWidgetConfiguration(widgetId, maxRows)
                                    }
                                } else {
                                    viewModel.setPendingWidgetInfo(info)
                                    mainActivity?.startWidgetBind(widgetId, info.provider)
                                }
                            },
                            onDismissRequest = { showWidgetPicker = false }
                        )
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
                        viewModel.recordDrawerAppLaunch()
                        viewModel.launchApp(packageName, options)
                    },
                    onShortcutClick = { shortcut ->
                        viewModel.recordDrawerAppLaunch()
                        viewModel.launchShortcut(shortcut)
                    }
                )
            }
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
