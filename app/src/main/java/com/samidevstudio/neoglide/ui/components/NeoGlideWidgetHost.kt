package com.samidevstudio.neoglide.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import kotlin.math.roundToInt

enum class Handle { NONE, TOP, BOTTOM, LEFT, RIGHT, MOVE }

private data class WidgetBounds(
    val row: Float,
    val col: Float,
    val spanX: Float,
    val spanY: Float,
)

@Composable
fun NeoGlideWidgetHost(
    modifier: Modifier = Modifier,
    widgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    row: Float,
    column: Float,
    spanX: Float,
    spanY: Float,
    columns: Int = 5,
    maxRows: Int = 10,
    unitWidth: Dp,
    unitHeight: Dp,
    isEditing: Boolean,
    isBlocked: Boolean = false,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onDragStart: () -> Unit = {},
    onResizeStart: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onInteractionUpdate: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onResize: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    content: @Composable (() -> Unit)? = null
) {
    val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
    val density = LocalDensity.current
    val unitWidthPx = with(density) { unitWidth.toPx() }
    val unitHeightPx = with(density) { unitHeight.toPx() }

    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnResizeStart by rememberUpdatedState(onResizeStart)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnInteractionUpdate by rememberUpdatedState(onInteractionUpdate)
    val currentIsEditing by rememberUpdatedState(isEditing)
    val currentRow by rememberUpdatedState(row)
    val currentCol by rememberUpdatedState(column)
    val currentSpanX by rememberUpdatedState(spanX)
    val currentSpanY by rememberUpdatedState(spanY)

    var activeHandle by remember { mutableStateOf(Handle.NONE) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    var initialSnapshot by remember { mutableStateOf<WidgetBounds?>(null) }
    var grabPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    var lastSnappedRow by remember { mutableFloatStateOf(-1f) }
    var lastSnappedCol by remember { mutableFloatStateOf(-1f) }
    var lastSnappedSpanX by remember { mutableFloatStateOf(-1f) }
    var lastSnappedSpanY by remember { mutableFloatStateOf(-1f) }

    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (activeHandle != Handle.NONE) 16.dp else if (isEditing) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "widgetElevation"
    )

    val liftScale by animateFloatAsState(
        targetValue = if (activeHandle == Handle.MOVE) {
            1.0f + (0.2f / kotlin.math.sqrt((spanX * spanY).toDouble()).toFloat()).coerceAtMost(0.2f)
        } else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "widgetLiftScale"
    )

    val visualRect = remember(row, column, spanX, spanY, dragDeltaX, dragDeltaY, activeHandle, initialSnapshot) {
        val base = initialSnapshot ?: WidgetBounds(row, column, spanX, spanY)

        var r = base.row
        var c = base.col
        var sx = base.spanX
        var sy = base.spanY

        when (activeHandle) {
            Handle.TOP -> {
                val dy = dragDeltaY / unitHeightPx
                val clampedDy = dy.coerceAtMost(base.spanY - 0.5f)
                r = base.row + clampedDy
                sy = base.spanY - clampedDy
            }
            Handle.BOTTOM -> {
                val dy = dragDeltaY / unitHeightPx
                sy = (base.spanY + dy).coerceAtLeast(0.5f)
            }
            Handle.LEFT -> {
                val dx = dragDeltaX / unitWidthPx
                val clampedDx = dx.coerceAtMost(base.spanX - 0.5f)
                c = base.col + clampedDx
                sx = base.spanX - clampedDx
            }
            Handle.RIGHT -> {
                val dx = dragDeltaX / unitWidthPx
                sx = (base.spanX + dx).coerceAtLeast(0.5f)
            }
            Handle.MOVE -> {
                val dx = dragDeltaX / unitWidthPx
                val dy = dragDeltaY / unitHeightPx
                c = (base.col + dx).coerceIn(0f, (columns.toFloat() - base.spanX).coerceAtLeast(0f))
                r = (base.row + dy).coerceIn(0f, (maxRows.toFloat() - base.spanY).coerceAtLeast(0f))
            }
            Handle.NONE -> {}
        }
        floatArrayOf(r, c, sx, sy)
    }

    LaunchedEffect(visualRect, activeHandle) {
        if (activeHandle != Handle.NONE) {
            val finalRow = ((visualRect[0]) * 2).roundToInt() / 2f
            val finalCol = ((visualRect[1]) * 2).roundToInt() / 2f
            val finalSpanX = (visualRect[2] * 2).roundToInt() / 2f
            val finalSpanY = (visualRect[3] * 2).roundToInt() / 2f

            if (finalRow != lastSnappedRow || finalCol != lastSnappedCol ||
                finalSpanX != lastSnappedSpanX || finalSpanY != lastSnappedSpanY) {
                onHapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                lastSnappedRow = finalRow
                lastSnappedCol = finalCol
                lastSnappedSpanX = finalSpanX
                lastSnappedSpanY = finalSpanY
            }

            currentOnInteractionUpdate(finalRow, finalCol, finalSpanX, finalSpanY)
        }
    }

    Box(
        modifier = modifier
            .offset(
                x = unitWidth * visualRect[1],
                y = unitHeight * visualRect[0]
            )
            .width(unitWidth * visualRect[2])
            .height(unitHeight * visualRect[3])
            .zIndex(if (isEditing || liftScale > 1f) 10f else 0f)
            .graphicsLayer {
                scaleX = liftScale
                scaleY = liftScale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    grabPoint.x / (unitWidthPx * visualRect[2]),
                    grabPoint.y / (unitHeightPx * visualRect[3])
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .shadow(
                    elevation = elevation,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .then(
                    if (isEditing) {
                        Modifier.border(2.dp, Color.White, RoundedCornerShape(28.dp))
                    } else if (isBlocked) {
                        Modifier.border(2.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                    } else Modifier
                )
                .pointerInput(widgetId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            onHapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                            accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                            isDragConfirmed = false
                            grabPoint = offset
                            initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        },
                        onDragEnd = {
                            if (isDragConfirmed) {
                                onHapticFeedback(HapticEngine.HapticType.DRAG_END)
                                val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                val finalRow = ((base.row + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                                val finalCol = ((base.col + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f

                                currentOnResize(
                                    finalRow.coerceAtLeast(0f),
                                    finalCol.coerceAtLeast(0f).coerceAtMost(columns.toFloat() - base.spanX),
                                    base.spanX,
                                    base.spanY
                                )
                            } else {
                                currentOnLongClick()
                            }
                            activeHandle = Handle.NONE
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                            initialSnapshot = null
                            isDragConfirmed = false
                        },
                        onDragCancel = {
                            activeHandle = Handle.NONE
                            dragDeltaX = 0f
                            dragDeltaY = 0f
                            initialSnapshot = null
                            isDragConfirmed = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                            dragDeltaX += dragAmount.x
                            dragDeltaY += dragAmount.y

                            if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                isDragConfirmed = true
                                onHapticFeedback(HapticEngine.HapticType.DRAG_START)
                                if (!currentIsEditing) {
                                    currentOnDragStart()
                                }
                                activeHandle = Handle.MOVE
                            }
                        }
                    )
                }
        ) {
            if (widgetInfo != null) {
                AndroidView(
                    factory = { context ->
                        appWidgetHost.createView(context, widgetId, widgetInfo).apply {
                            setAppWidget(widgetId, widgetInfo)
                            // REMOVE SYSTEM PADDING: Ensures launcher control over margins
                            setPadding(0, 0, 0, 0)
                            
                            // Initial size update (minus 8dp container padding)
                            val w = ((unitWidth * visualRect[2]) - 8.dp).value.toInt()
                            val h = ((unitHeight * visualRect[3]) - 8.dp).value.toInt()
                            updateAppWidgetSize(null, w, h, w, h)
                        }
                    },
                    update = { view ->
                        // Flickering Fix: Only update size when we snap to half-grid
                        // This prevents requesting the widget to redraw for every pixel of drag
                        val snapW = (visualRect[2] * 2).roundToInt() / 2f
                        val snapH = (visualRect[3] * 2).roundToInt() / 2f
                        
                        val w = ((unitWidth * snapW) - 8.dp).value.toInt()
                        val h = ((unitHeight * snapH) - 8.dp).value.toInt()
                        view.updateAppWidgetSize(null, w, h, w, h)
                    },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                )
            }
else if (content != null) {
                content()
            }

            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                )
            }

            if (isEditing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .pointerInput(widgetId) {
                            detectTapGestures(onTap = { /* consume */ })
                        }
                        .pointerInput(widgetId) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                    isDragConfirmed = false
                                    grabPoint = offset
                                    initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                },
                                onDragEnd = {
                                    if (isDragConfirmed) {
                                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                        val finalRow = ((base.row + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                                        val finalCol = ((base.col + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f

                                        currentOnResize(
                                            finalRow.coerceAtLeast(0f),
                                            finalCol.coerceAtLeast(0f).coerceAtMost(columns.toFloat() - base.spanX),
                                            base.spanX,
                                            base.spanY
                                        )
                                    }
                                    activeHandle = Handle.NONE
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    initialSnapshot = null
                                    isDragConfirmed = false
                                },
                                onDragCancel = {
                                    activeHandle = Handle.NONE
                                    dragDeltaX = 0f
                                    dragDeltaY = 0f
                                    initialSnapshot = null
                                    isDragConfirmed = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount
                                    dragDeltaX += dragAmount.x
                                    dragDeltaY += dragAmount.y

                                    if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 5.dp.toPx() }) {
                                        isDragConfirmed = true
                                        onHapticFeedback(HapticEngine.HapticType.DRAG_START)
                                        activeHandle = Handle.MOVE
                                    }
                                }
                            )
                        }
                )

                // TOP HANDLE
                ResizeHandle(Alignment.TopCenter, Modifier.align(Alignment.TopCenter).zIndex(2f), unitWidth, unitHeight, visualRect, activeHandle == Handle.TOP) { h, d, e ->
                    if (e) {
                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        val finalSpanY = ((base.spanY - dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                        val clampedSpanY = finalSpanY.coerceAtLeast(0.5f)
                        val adjustedRow = (base.row + base.spanY - clampedSpanY).coerceAtLeast(0f)
                        currentOnResize(adjustedRow, base.col, base.spanX, clampedSpanY)
                        activeHandle = Handle.NONE; dragDeltaY = 0f; initialSnapshot = null
                    } else {
                        if (activeHandle == Handle.NONE) { 
                            currentOnResizeStart()
                            activeHandle = h
                            initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        }
                        dragDeltaY += d.y
                    }
                }

                // BOTTOM HANDLE
                ResizeHandle(Alignment.BottomCenter, Modifier.align(Alignment.BottomCenter).zIndex(2f), unitWidth, unitHeight, visualRect, activeHandle == Handle.BOTTOM) { h, d, e ->
                    if (e) {
                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        val finalSpanY = ((base.spanY + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                        currentOnResize(base.row, base.col, base.spanX, finalSpanY.coerceAtLeast(0.5f))
                        activeHandle = Handle.NONE; dragDeltaY = 0f; initialSnapshot = null
                    } else {
                        if (activeHandle == Handle.NONE) {
                            currentOnResizeStart()
                            activeHandle = h
                            initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        }
                        dragDeltaY += d.y
                    }
                }

                // LEFT HANDLE
                ResizeHandle(Alignment.CenterStart, Modifier.align(Alignment.CenterStart).zIndex(2f), unitWidth, unitHeight, visualRect, activeHandle == Handle.LEFT) { h, d, e ->
                    if (e) {
                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        val finalSpanX = ((base.spanX - dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f
                        val clampedSpanX = finalSpanX.coerceAtLeast(0.5f)
                        val adjustedCol = (base.col + base.spanX - clampedSpanX).coerceAtLeast(0f)
                        currentOnResize(base.row, adjustedCol, clampedSpanX, base.spanY)
                        activeHandle = Handle.NONE; dragDeltaX = 0f; initialSnapshot = null
                    } else {
                        if (activeHandle == Handle.NONE) {
                            currentOnResizeStart()
                            activeHandle = h
                            initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        }
                        dragDeltaX += d.x
                    }
                }

                // RIGHT HANDLE
                ResizeHandle(Alignment.CenterEnd, Modifier.align(Alignment.CenterEnd).zIndex(2f), unitWidth, unitHeight, visualRect, activeHandle == Handle.RIGHT) { h, d, e ->
                    if (e) {
                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        val finalSpanX = ((base.spanX + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f
                        currentOnResize(base.row, base.col, finalSpanX.coerceAtLeast(0.5f), base.spanY)
                        activeHandle = Handle.NONE; dragDeltaX = 0f; initialSnapshot = null
                    } else {
                        if (activeHandle == Handle.NONE) {
                            currentOnResizeStart()
                            activeHandle = h
                            initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                        }
                        dragDeltaX += d.x
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.ResizeHandle(
    alignment: Alignment,
    modifier: Modifier,
    unitWidth: Dp,
    unitHeight: Dp,
    visualRect: FloatArray,
    isActive: Boolean,
    onDrag: (Handle, androidx.compose.ui.geometry.Offset, Boolean) -> Unit
) {
    val handle = when(alignment) {
        Alignment.TopCenter -> Handle.TOP
        Alignment.BottomCenter -> Handle.BOTTOM
        Alignment.CenterStart -> Handle.LEFT
        Alignment.CenterEnd -> Handle.RIGHT
        else -> Handle.NONE
    }

    val isHorizontal = handle == Handle.LEFT || handle == Handle.RIGHT
    // Constrain handle length to 40% of the span or 48dp minimum, ensuring corner space
    val handleLength = if (isHorizontal) {
        (unitHeight * visualRect[3] * 0.4f).coerceAtLeast(48.dp)
    } else {
        (unitWidth * visualRect[2] * 0.4f).coerceAtLeast(48.dp)
    }

    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = modifier
            .size(
                width = if (isHorizontal) 32.dp else handleLength,
                height = if (isHorizontal) handleLength else 32.dp
            )
            .pointerInput(handle) {
                detectDragGestures(
                    onDragEnd = { currentOnDrag(handle, androidx.compose.ui.geometry.Offset.Zero, true) },
                    onDragCancel = { currentOnDrag(handle, androidx.compose.ui.geometry.Offset.Zero, true) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentOnDrag(handle, dragAmount, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(width = if (isHorizontal) 6.dp else handleLength, height = if (isHorizontal) handleLength else 6.dp),
            shape = CircleShape,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            shadowElevation = 2.dp
        ) {}
    }
}
