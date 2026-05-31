package com.samidevstudio.pxllauncherneo.ui.components

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.roundToInt

private enum class Handle { NONE, TOP, BOTTOM, LEFT, RIGHT, MOVE }

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
    onLongClick: (DpOffset) -> Unit = {},
    onDragStart: () -> Unit = {},
    onResize: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> }
) {
    val widgetInfo = appWidgetManager.getAppWidgetInfo(widgetId)
    val density = LocalDensity.current
    val unitWidthPx = with(density) { unitWidth.toPx() }
    val unitHeightPx = with(density) { unitHeight.toPx() }

    // STABLE UPDATED STATE - These allow the pointerInput blocks to stay alive 
    // across state changes while still having access to the latest values.
    val currentOnResize by rememberUpdatedState(onResize)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentIsEditing by rememberUpdatedState(isEditing)
    val currentRow by rememberUpdatedState(row)
    val currentCol by rememberUpdatedState(column)
    val currentSpanX by rememberUpdatedState(spanX)
    val currentSpanY by rememberUpdatedState(spanY)

    // TRANSIENT DRAG STATE
    var activeHandle by remember { mutableStateOf(Handle.NONE) }
    var dragDeltaX by remember { mutableFloatStateOf(0f) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    
    // Snapshots at start of drag
    var initialSnapshot by remember { mutableStateOf<WidgetBounds?>(null) }

    // 1. Calculate visual drag bounds (fluid, based on initialSnapshot)
    val visualRect = remember(row, column, spanX, spanY, dragDeltaX, dragDeltaY, activeHandle, initialSnapshot) {
        val base = initialSnapshot ?: WidgetBounds(row, column, spanX, spanY)
        
        var r = base.row
        var c = base.col
        var sx = base.spanX
        var sy = base.spanY

        when (activeHandle) {
            Handle.TOP -> {
                val dy = dragDeltaY / unitHeightPx
                val maxDelta = base.spanY - 0.5f
                val clampedDy = dy.coerceAtMost(maxDelta)
                r = base.row + clampedDy
                sy = base.spanY - clampedDy
            }
            Handle.BOTTOM -> {
                val dy = dragDeltaY / unitHeightPx
                sy = (base.spanY + dy).coerceAtLeast(0.5f)
            }
            Handle.LEFT -> {
                val dx = dragDeltaX / unitWidthPx
                val maxDelta = base.spanX - 0.5f
                val clampedDx = dx.coerceAtMost(maxDelta)
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

    if (widgetInfo != null) {
        key(widgetId) {
            Box(
                modifier = modifier
                    .offset(
                        x = unitWidth * visualRect[1],
                        y = unitHeight * visualRect[0]
                    )
                    .width(unitWidth * visualRect[2])
                    .height(unitHeight * visualRect[3])
                    .padding(4.dp)
                    .then(
                        if (isEditing) {
                            Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    spotColor = Color.Black.copy(alpha = 0.5f)
                                )
                                .border(2.dp, Color.White, RoundedCornerShape(28.dp))
                        } else Modifier
                    )
                    .pointerInput(widgetId) {
                        // HIJACK GESTURE TO PREVENT APP OPENING ON LONG PRESS
                        // This block is stable and doesn't restart when isEditing changes.
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val down = event.changes.find { it.changedToDown() }
                                
                                // Only process long press if we aren't already editing
                                if (down != null && !currentIsEditing) {
                                    val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        while (true) {
                                            val nextEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
                                            if (nextEvent.changes.any { it.changedToUp() }) break
                                        }
                                    }
                                    if (result == null) {
                                        // LONG PRESS CONFIRMED
                                        currentOnLongClick(DpOffset(with(density) { down.position.x.toDp() }, with(density) { down.position.y.toDp() }))
                                        
                                        // DRAIN THE GESTURE: Consume everything until all fingers are up.
                                        // Because this block doesn't restart, the Up event is guaranteed to be consumed.
                                        var currentEvent = event
                                        while (currentEvent.changes.any { it.pressed }) {
                                            currentEvent.changes.forEach { it.consume() }
                                            currentEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                AndroidView(
                    factory = { context ->
                        appWidgetHost.createView(context, widgetId, widgetInfo).apply {
                            setAppWidget(widgetId, widgetInfo)
                        }
                    },
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                )

                if (isEditing) {
                    // Input blocker + Move handler
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .pointerInput(widgetId) {
                                // Block taps from reaching the widget below
                                detectTapGestures(onTap = { /* consume */ })
                            }
                            .pointerInput(widgetId) {
                                detectDragGestures(
                                    onDragStart = {
                                        currentOnDragStart()
                                        activeHandle = Handle.MOVE
                                        initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                    },
                                    onDragEnd = {
                                        val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                                        val finalRow = ((base.row + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                                        val finalCol = ((base.col + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f
                                        
                                        currentOnResize(
                                            finalRow.coerceAtLeast(0f),
                                            finalCol.coerceAtLeast(0f).coerceAtMost(4f - base.spanX),
                                            base.spanX,
                                            base.spanY
                                        )
                                        activeHandle = Handle.NONE
                                        dragDeltaX = 0f
                                        dragDeltaY = 0f
                                        initialSnapshot = null
                                    },
                                    onDragCancel = {
                                        activeHandle = Handle.NONE
                                        dragDeltaX = 0f
                                        dragDeltaY = 0f
                                        initialSnapshot = null
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDeltaX += dragAmount.x
                                        dragDeltaY += dragAmount.y
                                    }
                                )
                            }
                    )

                    // TOP HANDLE
                    ResizeHandle(
                        Alignment.TopCenter,
                        Modifier.fillMaxWidth().height(32.dp),
                        unitWidth,
                        unitHeight,
                        visualRect
                    ) { handle, delta, isEnd ->
                        if (isEnd) {
                            val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            val finalSpanY = ((base.spanY - dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                            val clampedSpanY = finalSpanY.coerceAtLeast(0.5f)
                            val initialBottom = base.row + base.spanY
                            val adjustedRow = (initialBottom - clampedSpanY).coerceAtLeast(0f)
                            currentOnResize(adjustedRow, base.col, base.spanX, clampedSpanY)
                            activeHandle = Handle.NONE
                            dragDeltaY = 0f
                            initialSnapshot = null
                        } else {
                            if (activeHandle == Handle.NONE) {
                                currentOnDragStart()
                                activeHandle = handle
                                initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            }
                            dragDeltaY += delta.y
                        }
                    }

                    // BOTTOM HANDLE
                    ResizeHandle(
                        Alignment.BottomCenter,
                        Modifier.fillMaxWidth().height(32.dp),
                        unitWidth,
                        unitHeight,
                        visualRect
                    ) { handle, delta, isEnd ->
                        if (isEnd) {
                            val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            val finalSpanY = ((base.spanY + dragDeltaY / unitHeightPx) * 2).roundToInt() / 2f
                            currentOnResize(base.row, base.col, base.spanX, finalSpanY.coerceAtLeast(0.5f))
                            activeHandle = Handle.NONE
                            dragDeltaY = 0f
                            initialSnapshot = null
                        } else {
                            if (activeHandle == Handle.NONE) {
                                currentOnDragStart()
                                activeHandle = handle
                                initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            }
                            dragDeltaY += delta.y
                        }
                    }

                    // LEFT HANDLE
                    ResizeHandle(
                        Alignment.CenterStart,
                        Modifier.fillMaxHeight().width(32.dp),
                        unitWidth,
                        unitHeight,
                        visualRect
                    ) { handle, delta, isEnd ->
                        if (isEnd) {
                            val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            val finalSpanX = ((base.spanX - dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f
                            val clampedSpanX = finalSpanX.coerceAtLeast(0.5f)
                            val initialRight = base.col + base.spanX
                            val adjustedCol = (initialRight - clampedSpanX).coerceAtLeast(0f)
                            currentOnResize(base.row, adjustedCol, clampedSpanX, base.spanY)
                            activeHandle = Handle.NONE
                            dragDeltaX = 0f
                            initialSnapshot = null
                        } else {
                            if (activeHandle == Handle.NONE) {
                                currentOnDragStart()
                                activeHandle = handle
                                initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            }
                            dragDeltaX += delta.x
                        }
                    }

                    // RIGHT HANDLE
                    ResizeHandle(
                        Alignment.CenterEnd,
                        Modifier.fillMaxHeight().width(32.dp),
                        unitWidth,
                        unitHeight,
                        visualRect
                    ) { handle, delta, isEnd ->
                        if (isEnd) {
                            val base = initialSnapshot ?: WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            val finalSpanX = ((base.spanX + dragDeltaX / unitWidthPx) * 2).roundToInt() / 2f
                            currentOnResize(base.row, base.col, finalSpanX.coerceAtLeast(0.5f), base.spanY)
                            activeHandle = Handle.NONE
                            dragDeltaX = 0f
                            initialSnapshot = null
                        } else {
                            if (activeHandle == Handle.NONE) {
                                currentOnDragStart()
                                activeHandle = handle
                                initialSnapshot = WidgetBounds(currentRow, currentCol, currentSpanX, currentSpanY)
                            }
                            dragDeltaX += delta.x
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ResizeHandle(
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
        
        // Calculate dynamic handle length as 30% of the edge, with a 48dp minimum
        val handleLength = if (isHorizontal) {
            (unitHeight * visualRect[3] * 0.3f).coerceAtLeast(48.dp)
        } else {
            (unitWidth * visualRect[2] * 0.3f).coerceAtLeast(48.dp)
        }

        // The "Thickened" part of the border
        Surface(
            modifier = Modifier
                .size(
                    width = if (isHorizontal) 6.dp else handleLength,
                    height = if (isHorizontal) handleLength else 6.dp
                ),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp
        ) {}
    }
}
