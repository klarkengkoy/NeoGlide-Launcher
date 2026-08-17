package com.samidevstudio.neoglide.ui.home

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.components.AppContextMenu
import com.samidevstudio.neoglide.ui.components.AppIcon
import com.samidevstudio.neoglide.ui.components.AppItem
import com.samidevstudio.neoglide.ui.components.AppPickerDialog
import com.samidevstudio.neoglide.ui.components.folder.FolderContextMenu
import com.samidevstudio.neoglide.ui.components.folder.FolderExpansion
import com.samidevstudio.neoglide.ui.components.folder.FolderItem
import com.samidevstudio.neoglide.ui.components.FrostedGlass
import com.samidevstudio.neoglide.ui.home.components.HomeContextMenu
import com.samidevstudio.neoglide.ui.home.components.NeoGlideWidgetHost
import com.samidevstudio.neoglide.ui.home.components.WidgetContextMenu
import com.samidevstudio.neoglide.ui.home.components.WidgetPickerDialog
import com.samidevstudio.neoglide.ui.drawer.DrawerScreen
import com.samidevstudio.neoglide.ui.settings.SettingsSheet
import com.samidevstudio.neoglide.ui.utils.system.HapticEngine
import com.samidevstudio.neoglide.ui.layout.LayoutManager
import com.samidevstudio.neoglide.ui.utils.system.rememberHapticFeedback
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
    var isRightAligned by remember { mutableStateOf(false) }
    var isBottomAligned by remember { mutableStateOf(false) }
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
    var hoveredUniqueKey by remember { mutableStateOf<String?>(null) }
    var blockedUniqueKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var grabPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var dragTargetBounds by remember { mutableStateOf<RectBounds?>(null) }
    var isDragBlocked by remember { mutableStateOf(false) }
    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(value = false) }
    var isCurrentDragBlocked by remember { mutableStateOf(false) }

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
                else -> {}
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

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

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
            val ignore = paddingValues
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
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
                    val statusBars = WindowInsets.statusBars.asPaddingValues()
                    val navigationBars = WindowInsets.navigationBars.asPaddingValues()

                    val topInset = statusBars.calculateTopPadding()
                    val bottomInset = navigationBars.calculateBottomPadding()

                    val layoutConfig = LayoutManager.calculateConfig(
                        screenWidthDp = screenWidthDp,
                        screenHeightDp = screenHeightDp,
                        densitySetting = preferences.gridSize,
                        topInset = topInset,
                        bottomInset = bottomInset,
                        showLabels = (preferences.appLabelMode == AppLabelMode.HOME_ONLY) || (preferences.appLabelMode == AppLabelMode.BOTH)
                    )

                    val columns = layoutConfig.columns
                    val totalColumns = layoutConfig.totalColumns
                    val totalRows = layoutConfig.totalRows
                    val unitWidth = layoutConfig.unitWidth
                    val unitHeight = layoutConfig.unitHeight
                    val iconSize = layoutConfig.iconSize
                    val fontSize = layoutConfig.fontSize

                    val unitWidthPx = with(density) { unitWidth.toPx() }
                    val unitHeightPx = with(density) { unitHeight.toPx() }

                    fun calculateTargetBounds(fingerPosition: androidx.compose.ui.geometry.Offset, spanX: Float = 1f, spanY: Float = 1f): RectBounds? {
                        val meshOffset = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                        val localX = fingerPosition.x - meshOffset.x
                        val localY = fingerPosition.y - meshOffset.y

                        val meshWidthPx = with(density) { layoutConfig.actualGridWidth.toPx() }
                        val meshHeightPx = with(density) { layoutConfig.actualGridHeight.toPx() }

                        if (localX !in 0f..meshWidthPx || localY !in 0f..meshHeightPx) {
                            return null
                        }

                        val snapFactor = LayoutManager.SNAP_FACTOR

                        val rawColRelative = (localX / unitWidthPx) - (spanX / 2f)
                        val rawRowRelative = (localY / unitHeightPx) - (spanY / 2f)

                        val snappedCol = kotlin.math.round(rawColRelative * snapFactor) / snapFactor
                        val snappedRow = kotlin.math.round(rawRowRelative * snapFactor) / snapFactor

                        return RectBounds(
                            snappedRow.coerceIn(0f, (totalRows - spanY).coerceAtLeast(0f)),
                            snappedCol.coerceIn(0f, (totalColumns - spanX).coerceAtLeast(0f)),
                            spanX,
                            spanY
                        )
                    }

                    val dockRow = LayoutManager.getDockRow(totalRows)
                    val showLabels = (preferences.appLabelMode == AppLabelMode.HOME_ONLY) || (preferences.appLabelMode == AppLabelMode.BOTH)
                    val isPremium = preferences.isPremium

                    /**
                     * Calculates the optimal anchor and alignment for the context menu.
                     * Uses a "Double-Anchor" strategy to let the system handle actual width/height.
                     */
                    fun calculateSmartMenuOffset(
                        itemRow: Float,
                        itemCol: Float,
                        spanX: Float,
                        spanY: Float
                    ): Triple<DpOffset, Boolean, Boolean> {
                        val meshOffset = meshCoords?.positionInWindow() ?: androidx.compose.ui.geometry.Offset.Zero
                        val meshLeft = with(density) { meshOffset.x.toDp() }.value
                        val meshTop = with(density) { meshOffset.y.toDp() }.value

                        val unitW = unitWidth.value
                        val unitH = unitHeight.value

                        val itemTop = meshTop + itemRow * unitH
                        val itemBottom = meshTop + (itemRow + spanY) * unitH
                        val itemLeft = meshLeft + itemCol * unitW
                        val itemRight = meshLeft + (itemCol + spanX) * unitW

                        val itemCenterX = itemLeft + (spanX * unitW) / 2f
                        val screenWidth = screenWidthDp.value
                        val meshCenterX = meshLeft + (totalColumns * unitW) / 2f

                        val screenHeight = screenHeightDp.value
                        val itemCenterY = itemTop + (spanY * unitH) / 2f
                        val screenCenterY = screenHeight / 2f

                        // X: Anchor to the edge that gives more room
                        val isRightAligned = itemCenterX > meshCenterX
                        val x = if (isRightAligned) (itemRight - screenWidth).dp else itemLeft.dp

                        // Y: Anchor to Top edge if flipping UP, Bottom edge if flipping DOWN
                        val isBottomAligned = itemCenterY > screenCenterY
                        val y = if (isBottomAligned) {
                            // Anchor BOTTOM of menu to TOP of icon (with 8dp gap)
                            (itemTop - 8 - screenHeight).dp
                        } else {
                            // Anchor TOP of menu to BOTTOM of icon (with 8dp gap)
                            (itemBottom + 8).dp
                        }

                        return Triple(DpOffset(x, y), isRightAligned, isBottomAligned)
                    }

                    val draggingItem = remember(draggingUniqueKey, homeItems) {
                        homeItems.find { it.uniqueKey == draggingUniqueKey }
                    }
                    val isLifting = (draggingUniqueKey != null || draggingAppFromFolder != null)

                    // 1. THE TRAY (Full Screen, Padding from LayoutManager)
                    Box(
                        modifier = Modifier
                            .padding(
                                top = layoutConfig.topPadding, 
                                bottom = layoutConfig.bottomPadding, 
                                start = layoutConfig.sidePadding, 
                                end = layoutConfig.sidePadding
                            )
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

                        // 2. THE MESH BOX
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(width = layoutConfig.actualGridWidth, height = layoutConfig.actualGridHeight)
                                .onGloballyPositioned { meshCoords = it }
                                .graphicsLayer { clip = false }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            editingWidgetId = -1
                                            showWidgetMenu = false
                                            showAppMenuPackage = null
                                            focusManager.clearFocus()
                                        },
                                        onLongPress = { offset ->
                                            if ((editingWidgetId == -1) && (!preferences.lockLayout)) {
                                                hapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                val row = offset.y / unitHeightPx
                                                val col = offset.x / unitWidthPx
                                                pendingAddAppRow = row
                                                pendingAddAppCol = col
                                                
                                                val (smartOffset, right, bottom) = calculateSmartMenuOffset(
                                                    itemRow = row,
                                                    itemCol = col,
                                                    spanX = 0f,
                                                    spanY = 0f
                                                )
                                                contextMenuOffset = smartOffset
                                                isRightAligned = right
                                                isBottomAligned = bottom
                                                showContextMenu = true
                                            }
                                            else if (preferences.lockLayout) {
                                                Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // LOCAL COORDINATES (0,0 is top-left of this Mesh Box)

                            // 3. THE CORE BOX (Aligned to bold lines)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                // GHOST TARGET / PLACEHOLDER
                                val ghostTargetX by animateDpAsState(targetValue = dragTargetBounds?.let { unitWidth * it.col } ?: 0.dp, label = "ghostX")
                                val ghostTargetY by animateDpAsState(targetValue = dragTargetBounds?.let { unitHeight * it.row } ?: 0.dp, label = "ghostY")
                                val ghostTargetWidth by animateDpAsState(targetValue = dragTargetBounds?.let { unitWidth * it.spanX } ?: 0.dp, label = "ghostW")
                                val ghostTargetHeight by animateDpAsState(targetValue = dragTargetBounds?.let { unitHeight * it.spanY } ?: 0.dp, label = "ghostH")

                                val ghostColor = Color.Black.copy(alpha = 0.15f)

                                if (editingWidgetId != -1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .zIndex(90f)
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onTap = {
                                                        editingWidgetId = -1
                                                    }
                                                )
                                            }
                                    )
                                }

                                if (draggingUniqueKey != null || draggingAppFromFolder != null || (editingWidgetId != -1 && dragTargetBounds != null)) {
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(ghostTargetX.toPx().roundToInt(), ghostTargetY.toPx().roundToInt()) }
                                            .size(ghostTargetWidth, ghostTargetHeight)
                                            .padding(4.dp)
                                            .background(ghostColor, RoundedCornerShape(12.dp))
                                    )
                                }

                                homeItems.forEach { item ->
                                    key(item.uniqueKey) {
                                        val isDragging = draggingUniqueKey == item.uniqueKey
                                        val isHovered = hoveredUniqueKey == item.uniqueKey
                                        val isBlocked = item.uniqueKey in blockedUniqueKeys
                                        val rowToUse = if (item.row >= 99f) dockRow else item.row
                                        val isDockItem = rowToUse == dockRow
                                        Box(
                                            modifier = Modifier
                                                .then(
                                                    if (item !is HomeItem.Widget) {
                                                        Modifier
                                                            .offset(x = unitWidth * item.column, y = unitHeight * rowToUse)
                                                            .size(unitWidth * item.spanX, unitHeight * item.spanY)
                                                    } else Modifier.fillMaxSize()
                                                )
                                                .zIndex(if (item.id == editingWidgetId || draggingUniqueKey == item.uniqueKey) 100f else 0f)
                                                .graphicsLayer { alpha = if (isDragging && item !is HomeItem.Widget) 0f else 1f }
                                                .then(
                                                    if (item !is HomeItem.Widget) {
                                                        Modifier.pointerInput(item.id, item.row, item.column) {
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
                                                                            meshInWindow.x + item.column * unitWidthPx,
                                                                            meshInWindow.y + rowToUse * unitHeightPx
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

                                                                        // Real-time hover detection
                                                                        val newHoveredKey = if (isDragConfirmed) {
                                                                            dragTargetBounds?.let { b ->
                                                                                homeItems.find { other ->
                                                                                    if (other.uniqueKey == item.uniqueKey) return@find false
                                                                                    val otherRow = if (other.row >= 99f) dockRow else other.row
                                                                                    val distSq = (b.row - otherRow) * (b.row - otherRow) + (b.col - other.column) * (b.col - other.column)
                                                                                    distSq < 0.0625f // 0.25 unit radius
                                                                                }?.uniqueKey
                                                                            }
                                                                        } else null

                                                                        if (newHoveredKey != hoveredUniqueKey) {
                                                                            if (newHoveredKey != null) hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                                                            hoveredUniqueKey = newHoveredKey
                                                                        }

                                                                        blockedUniqueKeys = if (isDragConfirmed && dragTargetBounds != null) {
                                                                            val epsilon = 0.05f
                                                                            val targetRect = android.graphics.RectF(
                                                                                dragTargetBounds!!.col + epsilon,
                                                                                dragTargetBounds!!.row + epsilon,
                                                                                dragTargetBounds!!.col + item.spanX - epsilon,
                                                                                dragTargetBounds!!.row + item.spanY - epsilon
                                                                            )
                                                                            homeItems.filter { other ->
                                                                                if (other.uniqueKey == item.uniqueKey || other.uniqueKey == newHoveredKey) return@filter false
                                                                                val otherRow = if (other.row >= 99f) dockRow else other.row
                                                                                val otherRect = android.graphics.RectF(other.column + epsilon, otherRow + epsilon, other.column + other.spanX - epsilon, otherRow + other.spanY - epsilon)
                                                                                android.graphics.RectF.intersects(targetRect, otherRect)
                                                                            }.map { it.uniqueKey }.toSet()
                                                                        } else emptySet()

                                                                        isCurrentDragBlocked = blockedUniqueKeys.isNotEmpty()
                                                                    }
                                                                },
                                                                onDragEnd = {
                                                                    if (isDragConfirmed) {
                                                                        hapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                                        dragTargetBounds?.let { viewModel.updateItemPosition(item, it.row, it.col, totalRows) }
                                                                    } else {
                                                                        showAppMenuId = item.id
                                                                        if (item is HomeItem.App) {
                                                                            coroutineScope.launch {
                                                                                val shortcuts = viewModel.getShortcuts(item.appModel.packageName)
                                                                                appMenuShortcuts = shortcuts
                                                                                val (offset, right, bottom) = calculateSmartMenuOffset(rowToUse, item.column, 1f, 1f)
                                                                                contextMenuOffset = offset
                                                                                isRightAligned = right
                                                                                isBottomAligned = bottom
                                                                                appMenuLabel = item.appModel.label
                                                                                showAppMenuPackage = item.appModel.packageName
                                                                            }
                                                                        } else if (item is HomeItem.Folder) {
                                                                            val (offset, right, bottom) = calculateSmartMenuOffset(rowToUse, item.column, 1f, 1f)
                                                                            contextMenuOffset = offset
                                                                            isRightAligned = right
                                                                            isBottomAligned = bottom
                                                                            showFolderMenuId = item.id
                                                                            showFolderMenu = true
                                                                        }
                                                                    }
                                                                    draggingUniqueKey = null
                                                                    hoveredUniqueKey = null
                                                                    blockedUniqueKeys = emptySet()
                                                                    dragTargetBounds = null
                                                                }
                                                            )
                                                        }
                                                    } else Modifier
                                                ),
                                            contentAlignment = if (item is HomeItem.Widget) {
                                                Alignment.TopStart
                                            } else if (showLabels) {
                                                if (isDockItem) Alignment.BottomCenter else Alignment.TopCenter
                                            } else {
                                                Alignment.Center
                                            }
                                        ) {
                                            when (item) {
                                                is HomeItem.App -> AppItem(app = item.appModel, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, refreshTrigger = refreshTrigger, isHovered = isHovered, isBlocked = isBlocked, onClick = {
                                                    if (editingWidgetId != -1) {
                                                        editingWidgetId = -1
                                                        return@AppItem
                                                    }
                                                    if (draggingUniqueKey == null) {
                                                        hapticFeedback(HapticEngine.HapticType.CLICK)
                                                        viewModel.launchApp(item.appModel.packageName)
                                                    }
                                                })
                                                is HomeItem.Folder -> FolderItem(label = item.label, apps = item.apps, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = showLabels, isHovered = isHovered, isBlocked = isBlocked, onClick = {
                                                    if (editingWidgetId != -1) {
                                                        editingWidgetId = -1
                                                        return@FolderItem
                                                    }
                                                    if (draggingUniqueKey == null) {
                                                        hapticFeedback(HapticEngine.HapticType.CLICK)
                                                        expandedFolderId = item.id
                                                    }
                                                })
                                                is HomeItem.Widget -> NeoGlideWidgetHost(
                                                    widgetId = item.id,
                                                    appWidgetHost = viewModel.appWidgetHost,
                                                    appWidgetManager = viewModel.appWidgetManager,
                                                    row = rowToUse,
                                                    column = item.column,
                                                    spanX = item.spanX,
                                                    spanY = item.spanY,
                                                    totalColumns = totalColumns,
                                                    totalRows = totalRows,
                                                    unitWidth = unitWidth,
                                                    unitHeight = unitHeight,
                                                    isEditing = editingWidgetId == item.id,
                                                    isBlocked = isBlocked,
                                                    modifier = Modifier,
                                                    onHapticFeedback = { hapticFeedback(it) },
                                                    onDragStart = {
                                                        draggingUniqueKey = item.uniqueKey
                                                        showWidgetMenu = false
                                                    },
                                                    onResizeStart = {
                                                        draggingUniqueKey = item.uniqueKey
                                                        showWidgetMenu = false
                                                    },
                                                    onLongClick = {
                                                        if (editingWidgetId != -1) {
                                                            editingWidgetId = -1
                                                        } else {
                                                            coroutineScope.launch {
                                                                appMenuShortcuts = viewModel.getShortcuts(item.widgetEntity.providerPackage)
                                                                editingWidgetId = item.id
                                                                showAppMenuId = item.id
                                                                appMenuLabel = item.widgetEntity.label
                                                                val (offset, right, bottom) = calculateSmartMenuOffset(rowToUse, item.column, item.spanX, item.spanY)
                                                                contextMenuOffset = offset
                                                                isRightAligned = right
                                                                isBottomAligned = bottom
                                                                showWidgetMenu = true
                                                            }
                                                        }
                                                    },
                                                    onClick = {
                                                        if (editingWidgetId != -1) {
                                                            editingWidgetId = -1
                                                        }
                                                    },
                                                    onInteractionUpdate = { r, c, sx, sy ->
                                                        dragTargetBounds = RectBounds(r, c, sx, sy)
                                                        val targetRect = android.graphics.RectF(c, r, c + sx, r + sy)
                                                        blockedUniqueKeys = homeItems.filter { other ->
                                                            if (other.uniqueKey == item.uniqueKey) return@filter false
                                                            val otherRow = if (other.row >= 99f) dockRow else other.row
                                                            val otherRect = android.graphics.RectF(other.column, otherRow, other.column + other.spanX, otherRow + other.spanY)
                                                            android.graphics.RectF.intersects(targetRect, otherRect)
                                                        }.map { it.uniqueKey }.toSet()
                                                        isCurrentDragBlocked = viewModel.isSpaceOccupied(r, c, sx, sy, totalRows, item.uniqueKey)
                                                    },
                                                    onInteractionEnd = {
                                                        draggingUniqueKey = null
                                                        dragTargetBounds = null
                                                        isCurrentDragBlocked = false
                                                        blockedUniqueKeys = emptySet()
                                                    },
                                                    onResize = { nr, nc, nsx, nsy ->
                                                        viewModel.updateWidgetBounds(item.id, nr, nc, nsx, nsy, totalRows)
                                                        dragTargetBounds = null
                                                        draggingUniqueKey = null
                                                        isCurrentDragBlocked = false
                                                        blockedUniqueKeys = emptySet()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isLifting && (draggingItem != null || draggingAppFromFolder != null)) {
                        val isWidget = draggingItem is HomeItem.Widget
                        if (!isWidget) {
                            val spanX = draggingItem?.spanX ?: 1f
                            val spanY = draggingItem?.spanY ?: 1f
                            val liftScaleOverlay by animateFloatAsState(targetValue = getLiftScale(spanX, spanY), animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), label = "liftScale")
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                                    .size(unitWidth * spanX, unitHeight * spanY)
                                    .zIndex(110f)
                                    .graphicsLayer {
                                        alpha = 1.0f
                                        scaleX = liftScaleOverlay
                                        scaleY = liftScaleOverlay
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (draggingItem != null && draggingItem !is HomeItem.Widget) {
                                    when (draggingItem) {
                                        is HomeItem.App -> AppIcon(packageName = draggingItem.appModel.packageName, contentDescription = null, size = iconSize, useMonochrome = preferences.useMonochromeIcons)
                                        is HomeItem.Folder -> FolderItem(label = draggingItem.label, apps = draggingItem.apps, iconSize = iconSize, fontSize = fontSize, useMonochrome = preferences.useMonochromeIcons, showLabel = false, onClick = null)
                                        is HomeItem.Widget -> {}
                                    }
                                } else if (draggingAppFromFolder != null) {
                                    AppIcon(packageName = draggingAppFromFolder!!.packageName, contentDescription = null, size = iconSize, useMonochrome = preferences.useMonochromeIcons)
                                }
                            }
                        }
                    }

                    HomeContextMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        offset = contextMenuOffset,
                        isRightAligned = isRightAligned,
                        isBottomAligned = isBottomAligned,
                        onOpenWidgets = { showWidgetPicker = true },
                        onAddApp = { showAppPicker = true },
                        onOpenLauncherSettings = { showSettings = true }
                    )

                    if (showFolderMenu) {
                        val folderItem = homeItems.find { it.id == showFolderMenuId && it is HomeItem.Folder } as? HomeItem.Folder
                        FolderContextMenu(
                            expanded = true,
                            onDismissRequest = { showFolderMenu = false; showFolderMenuId = -1 },
                            label = folderItem?.label ?: "Folder",
                            offset = contextMenuOffset,
                            onEditName = {
                                expandedFolderId = showFolderMenuId
                                autoFocusFolderName = true
                            },
                            onRemove = {
                                viewModel.removeFolder(showFolderMenuId)
                                showFolderMenu = false
                                showFolderMenuId = -1
                            }
                        )
                    }

                    if (showAppMenuPackage != null) AppContextMenu(
                        expanded = true,
                        onDismissRequest = { showAppMenuPackage = null },
                        packageName = showAppMenuPackage!!,
                        label = appMenuLabel,
                        shortcuts = appMenuShortcuts,
                        offset = contextMenuOffset,
                        isRightAligned = isRightAligned,
                        isBottomAligned = isBottomAligned,
                        isPremium = isPremium,
                        onShortcutClick = { viewModel.launchShortcut(it) },
                        onHideToggle = { viewModel.hideApp(showAppMenuPackage!!) },
                        onRemove = { viewModel.removeHomeApp(showAppMenuId) },
                        onShowPaywall = { /* Trigger paywall logic */ }
                    )
                    if (showWidgetMenu) {
                        val widgetItem = homeItems.find { it.id == showAppMenuId && it is HomeItem.Widget } as? HomeItem.Widget
                        WidgetContextMenu(
                            expanded = true,
                            onDismissRequest = { showWidgetMenu = false },
                            onRemove = {
                                viewModel.removeWidget(showAppMenuId)
                                showWidgetMenu = false
                                editingWidgetId = -1
                            },
                            onOpenApp = {
                                widgetItem?.let { viewModel.launchApp(it.widgetEntity.providerPackage) }
                                showWidgetMenu = false
                                editingWidgetId = -1
                            },
                            onAppInfo = {
                                widgetItem?.let {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", it.widgetEntity.providerPackage, null)
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                                showWidgetMenu = false
                                editingWidgetId = -1
                            },
                            label = appMenuLabel,
                            shortcuts = appMenuShortcuts,
                            onShortcutClick = {
                                viewModel.launchShortcut(it)
                                editingWidgetId = -1
                            },
                            offset = contextMenuOffset,
                            isRightAligned = isRightAligned,
                            isBottomAligned = isBottomAligned
                        )
                    }

                    val expandedFolder = remember(homeItems, expandedFolderId) { homeItems.find { it.id == expandedFolderId && it is HomeItem.Folder } as? HomeItem.Folder }
                    if (expandedFolder != null) FolderExpansion(folderId = expandedFolder.id, label = expandedFolder.label, apps = expandedFolder.apps, unitWidth = unitWidth, unitHeight = unitHeight, iconSize = iconSize, fontSize = fontSize, spacing = layoutConfig.spacing, columns = columns, onDismiss = { expandedFolderId = -1 }, onRemove = { viewModel.removeFolder(expandedFolder.id) }, onAddApps = { pendingFolderIdForAdd = expandedFolder.id; showAppPicker = true }, onLabelChange = { viewModel.updateFolderLabel(expandedFolder.id, it) }, onAppClick = { pkg, opts ->
                        if (editingWidgetId != -1) {
                            editingWidgetId = -1
                            expandedFolderId = -1
                            return@FolderExpansion
                        }
                        hapticFeedback(HapticEngine.HapticType.CLICK)
                        viewModel.launchApp(pkg, opts)
                        expandedFolderId = -1
                    }, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope, useMonochrome = preferences.useMonochromeIcons, refreshTrigger = refreshTrigger, onHapticFeedback = hapticFeedback, getShortcuts = { viewModel.getShortcuts(it) }, onShortcutClick = { viewModel.launchShortcut(it) }, onHideToggle = { viewModel.hideApp(it) }, onAddToHome = { viewModel.addHomeApp(it, 0f, 0f) }, isDrawerFolder = false, onAppDragStart = { app, startPos, grab ->
                        hapticFeedback(HapticEngine.HapticType.DRAG_START)
                        draggingAppFromFolder = app
                        dragOffset = startPos
                        grabPoint = grab
                        isDragConfirmed = true
                        draggingUniqueKey = "APP_FOLDER_DRAG" // Sentinel to trigger tray/mesh state
                    }, onAppDrag = { dragAmount ->
                        dragOffset += dragAmount
                        dragTargetBounds = calculateTargetBounds(dragOffset + grabPoint, 1f, 1f)

                        // Hover/Collision check for dragging OUT of folder
                        val newHoveredKey = dragTargetBounds?.let { b ->
                            homeItems.find { other ->
                                // Skip the folder we are coming from if possible, or just use center distance
                                if (other.id == expandedFolderId) return@find false
                                val otherRow = if (other.row >= 99f) dockRow else other.row
                                val distSq = (b.row - otherRow) * (b.row - otherRow) + (b.col - other.column) * (b.col - other.column)
                                distSq < 0.0625f
                            }?.uniqueKey
                        }
                        if (newHoveredKey != hoveredUniqueKey) {
                            if (newHoveredKey != null) hapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                            hoveredUniqueKey = newHoveredKey
                        }

                        blockedUniqueKeys = dragTargetBounds?.let { b ->
                            val targetRect = android.graphics.RectF(b.col, b.row, b.col + 1f, b.row + 1f)
                            homeItems.filter { other ->
                                if (other.id == expandedFolderId || other.uniqueKey == newHoveredKey) return@filter false
                                val otherRow = if (other.row >= 99f) dockRow else other.row
                                val otherRect = android.graphics.RectF(other.column, otherRow, other.column + other.spanX, otherRow + other.spanY)
                                android.graphics.RectF.intersects(targetRect, otherRect)
                            }.map { it.uniqueKey }.toSet()
                        } ?: emptySet()

                        isCurrentDragBlocked = blockedUniqueKeys.isNotEmpty()
                    }, onAppDragIn = {
                        // Optional: trigger re-expansion animation if needed
                    }, onAppDragEnd = {
                        if (isDragConfirmed) {
                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                            dragTargetBounds?.let { viewModel.removeAppFromFolder(expandedFolder.id, draggingAppFromFolder!!.packageName, it.row, it.col) }
                        }
                        draggingAppFromFolder = null
                        draggingUniqueKey = null
                        hoveredUniqueKey = null
                        blockedUniqueKeys = emptySet()
                        dragTargetBounds = null
                        expandedFolderId = -1
                    }, onAppDragCancel = {
                        draggingAppFromFolder = null
                        draggingUniqueKey = null
                        hoveredUniqueKey = null
                        blockedUniqueKeys = emptySet()
                        dragTargetBounds = null
                        expandedFolderId = -1
                    })

                    if (showAppPicker) AppPickerDialog(title = "Add Application", apps = viewModel.availableAppsForPicker.collectAsStateWithLifecycle().value, recentlyUsedApps = viewModel.recentlyUsedApps.collectAsStateWithLifecycle().value, onAppSelected = { viewModel.addHomeApp(it.packageName, pendingAddAppRow, pendingAddAppCol); showAppPicker = false }, onDismissRequest = { showAppPicker = false })
                    if (showWidgetPicker) WidgetPickerDialog(
                        appsWithWidgets = viewModel.appsWithWidgets.collectAsStateWithLifecycle().value, 
                        allWidgetProviders = viewModel.appWidgetManager.installedProviders, 
                        unitWidthDp = unitWidth.value, 
                        unitHeightDp = unitHeight.value, 
                        maxColumns = columns, 
                        onGetWidgets = { viewModel.getWidgetsForApp(it) }, 
                        onWidgetSelected = { info -> 
                            showWidgetPicker = false
                            viewModel.addNewWidget(info, pendingAddAppRow, pendingAddAppCol)
                        }, 
                        onDismissRequest = { showWidgetPicker = false }
                    )
                }
            }
        }

        if (shouldShowDefaultPrompt) DefaultLauncherDialog(onSetDefault = { viewModel.openDefaultLauncherSettings() }, onDismiss = { viewModel.dismissDefaultPrompt() })
        if (showSettings) SettingsSheet(
            onDismiss = { showSettings = false }, 
            viewModel = settingsViewModel,
            onMigrateLabels = { viewModel.migrateLayoutForLabelMode(it) }
        )

        AnimatedVisibility(visible = showDrawer, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Surface(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                DrawerScreen(
                    viewModel = drawerViewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onAppClick = { pkg, opts ->
                        viewModel.recordDrawerAppLaunch()
                        viewModel.launchApp(pkg, opts)
                    },
                    onShortcutClick = {
                        viewModel.recordDrawerAppLaunch()
                        viewModel.launchShortcut(it)
                    },
                    onAddToHome = { pkg ->
                        viewModel.addHomeApp(pkg, 0f, 0f) // Add to home logic
                    },
                    onMigrateLabels = { viewModel.migrateLayoutForLabelMode(it) }
                )
            }
        }
    }
}

@Composable
fun DefaultLauncherDialog(onSetDefault: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), title = { Text("Set NeoGlide as default?") }, text = { Text("NeoGlide is not your default launcher. Set it as default for the best experience.") }, confirmButton = { TextButton(onClick = onSetDefault) { Text("Set as default") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("No thanks") } })
}

private fun androidx.compose.ui.layout.LayoutCoordinates.positionInWindow(): androidx.compose.ui.geometry.Offset = this.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
