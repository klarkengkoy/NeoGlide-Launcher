package com.samidevstudio.pxllauncherneo.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine

enum class Handle { NONE, TOP, BOTTOM, LEFT, RIGHT, MOVE }

private data class WidgetBounds(
    val row: Float,
    val col: Float,
    val spanX: Float,
    val spanY: Float
)

@Composable
fun PxlWidgetHost(
    widgetId: Int,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    row: Float,
    column: Float,
    spanX: Float,
    spanY: Float,
    unitWidth: Dp,
    unitHeight: Dp,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
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

    var lastSnappedRow by remember { mutableFloatStateOf(-1f) }
    var lastSnappedCol by remember { mutableFloatStateOf(-1f) }
    var lastSnappedSpanX by remember { mutableFloatStateOf(-1f) }
    var lastSnappedSpanY by remember { mutableFloatStateOf(-1f) }

    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDragConfirmed by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (activeHandle != Handle.NONE) 16.dp else if (isEditing) 8.dp else 0.dp,
        label = "widgetElevation"
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
                c = base.col + (dragDeltaX / unitWidthPx)
                r = base.row + (dragDeltaY / unitHeightPx)
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
            .zIndex(if (isEditing) 10f else 0f)
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
                    } else Modifier
                )
                .pointerInput(widgetId) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            onHapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                            accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                            isDragConfirmed = false
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
                                    finalCol.coerceAtLeast(0f).coerceAtMost(4f - base.spanX),
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
                        }
                    },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                )
            } else if (content != null) {
                content()
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
                                onDragStart = {
                                    accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                    isDragConfirmed = false
                                    initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                },
                                onDragEnd = {
                                    if (isDragConfirmed) {
                                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                        val finalRow = ((base.row + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                                        val finalCol = ((base.col + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f

                                        currentOnResize(
                                            finalRow.coerceAtLeast(0f),
                                            finalCol.coerceAtLeast(0f).coerceAtMost(4f - base.spanX),
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
                                        currentOnDragStart()
                                        activeHandle = Handle.MOVE
                                    }
                                }
                            )
                        }
                )

                // TOP HANDLE
                ResizeHandle(Alignment.TopCenter, Modifier.fillMaxWidth().height(32.dp), unitWidth, unitHeight, visualRect) { h, d, e ->
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
                ResizeHandle(Alignment.BottomCenter, Modifier.fillMaxWidth().height(32.dp), unitWidth, unitHeight, visualRect) { h, d, e ->
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
                ResizeHandle(Alignment.CenterStart, Modifier.fillMaxHeight().width(32.dp), unitWidth, unitHeight, visualRect) { h, d, e ->
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
                ResizeHandle(Alignment.CenterEnd, Modifier.fillMaxHeight().width(32.dp), unitWidth, unitHeight, visualRect) { h, d, e ->
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
    onDrag: (Handle, androidx.compose.ui.geometry.Offset, Boolean) -> Unit
) {
    val handle = when(alignment) {
        Alignment.TopCenter -> Handle.TOP
        Alignment.BottomCenter -> Handle.BOTTOM
        Alignment.CenterStart -> Handle.LEFT
        Alignment.CenterEnd -> Handle.RIGHT
        else -> Handle.NONE
    }

    val currentOnDrag by rememberUpdatedState(onDrag)

    Box(
        modifier = modifier
            .align(alignment)
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
        contentAlignment = when(alignment) {
            Alignment.TopCenter -> Alignment.TopCenter
            Alignment.BottomCenter -> Alignment.BottomCenter
            Alignment.CenterStart -> Alignment.CenterStart
            Alignment.CenterEnd -> Alignment.CenterEnd
            else -> Alignment.Center
        }
    ) {
        val isHorizontal = handle == Handle.LEFT || handle == Handle.RIGHT
        val handleLength = if (isHorizontal) (unitHeight * visualRect[3] * 0.3f).coerceAtLeast(48.dp) else (unitWidth * visualRect[2] * 0.3f).coerceAtLeast(48.dp)

        Surface(
            modifier = Modifier.size(width = if (isHorizontal) 6.dp else handleLength, height = if (isHorizontal) handleLength else 6.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp
        ) {}
    }
}
