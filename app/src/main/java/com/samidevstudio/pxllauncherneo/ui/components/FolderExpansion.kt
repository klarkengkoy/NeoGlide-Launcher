package com.samidevstudio.pxllauncherneo.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut
import com.samidevstudio.pxllauncherneo.ui.utils.HapticEngine
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun FolderExpansion(
    folderId: Int,
    label: String,
    apps: List<AppModel>,
    onDismiss: () -> Unit,
    onLabelChange: (String) -> Unit,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String) -> Unit = {},
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onAppDragStart: (AppModel, androidx.compose.ui.geometry.Offset) -> Unit = { _, _ -> },
    onAppDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onAppDragOut: (AppModel, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> },
    onAppDragEnd: () -> Unit = {},
    onAppDragCancel: () -> Unit = {}
) {
    var currentLabel by remember(label) { mutableStateOf(label) }
    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var isDraggedOut by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rootCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootCoords = it }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
                .clickable(enabled = false) {}, // Consume clicks
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Folder Label (Editable)
                    BasicTextField(
                        value = currentLabel,
                        onValueChange = {
                            currentLabel = it
                            onLabelChange(it)
                        },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )

                    // Apps Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            var itemCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
                            val isBeingDragged = draggingApp?.packageName == app.packageName
                            var showMenu by remember { mutableStateOf(false) }
                            var shortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }
                            var initialDragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

                            LaunchedEffect(showMenu) {
                                if (showMenu) {
                                    shortcuts = getShortcuts(app.packageName)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { itemCoords = it }
                                    .graphicsLayer {
                                        alpha = if (isBeingDragged) 0f else 1f
                                    }
                                    .pointerInput(app.packageName) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                onHapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                draggingApp = app
                                                // Standardize dragOffset to TOP-LEFT of the icon
                                                val touchWindow = itemCoords?.localToWindow(offset) ?: androidx.compose.ui.geometry.Offset.Zero
                                                val iconSizePx = with(density) { 80.dp.toPx() }
                                                initialDragOffset = touchWindow - androidx.compose.ui.geometry.Offset(iconSizePx / 2, iconSizePx / 2)
                                                dragOffset = initialDragOffset
                                                onAppDragStart(app, initialDragOffset)
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                                onAppDrag(amount)

                                                // Introduce a slop/threshold before collapsing the folder
                                                // Check movement of the touch point (center of icon)
                                                val iconSizePx = with(density) { 80.dp.toPx() }
                                                val currentTouch = dragOffset + androidx.compose.ui.geometry.Offset(iconSizePx / 2, iconSizePx / 2)
                                                val initialTouch = initialDragOffset + androidx.compose.ui.geometry.Offset(iconSizePx / 2, iconSizePx / 2)
                                                val movement = (currentTouch - initialTouch).getDistance()
                                                val slop = with(density) { 24.dp.toPx() }

                                                if (movement > slop && draggingApp != null && !isDraggedOut) {
                                                    // Collapse only after meaningful movement
                                                    isDraggedOut = true
                                                    // CRITICAL: Pass the current standardized dragOffset (Top-Left), not the touch position
                                                    onAppDragOut(app, dragOffset, amount)
                                                }
                                            },
                                            onDragEnd = {
                                                onHapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                // Trigger Menu if dropped on same spot (minimal movement)
                                                val iconSizePx = with(density) { 80.dp.toPx() }
                                                val currentTouch = dragOffset + androidx.compose.ui.geometry.Offset(iconSizePx / 2, iconSizePx / 2)
                                                val initialTouch = initialDragOffset + androidx.compose.ui.geometry.Offset(iconSizePx / 2, iconSizePx / 2)
                                                val movement = (currentTouch - initialTouch).getDistance()
                                                val slop = with(density) { 10.dp.toPx() }
                                                if (movement < slop && !isDraggedOut) {
                                                    showMenu = true
                                                }
                                                // Ensure we tell the Home screen that the drag has ended
                                                onAppDragEnd()
                                                draggingApp = null
                                                isDraggedOut = false
                                            },
                                            onDragCancel = {
                                                onAppDragCancel()
                                                draggingApp = null
                                                isDraggedOut = false
                                            }
                                        )
                                    }
                            ) {
                                AppItem(
                                    app = app,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    useMonochrome = useMonochrome,
                                    iconPackPackageName = iconPackPackageName,
                                    sharedElementKeyPrefix = "folder-$folderId",
                                    showLabel = true,
                                    isLongClickEnabled = false, // Disable AppItem's internal long-click
                                    onClick = { options -> onAppClick(app.packageName, options) }
                                )

                                if (showMenu) {
                                    AppContextMenu(
                                        expanded = true,
                                        onDismissRequest = { showMenu = false },
                                        packageName = app.packageName,
                                        label = app.label,
                                        shortcuts = shortcuts,
                                        onShortcutClick = onShortcutClick,
                                        onHideToggle = { onHideToggle(app.packageName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Drag Icon
        if (!isDraggedOut) {
            draggingApp?.let { app ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            // Convert window-relative dragOffset (top-left) to local coordinates of root Box
                            val localTopLeft = rootCoords?.windowToLocal(dragOffset) ?: dragOffset

                            androidx.compose.ui.unit.IntOffset(
                                localTopLeft.x.roundToInt(),
                                localTopLeft.y.roundToInt()
                            )
                        }
                        .size(80.dp) // Approximate size for drag feedback
                        .graphicsLayer {
                            scaleX = 1.2f
                            scaleY = 1.2f
                            shadowElevation = with(density) { 16.dp.toPx() }
                            shape = RoundedCornerShape(16.dp)
                            clip = true
                        }
                ) {
                    AppItem(
                        app = app,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        useMonochrome = useMonochrome,
                        iconPackPackageName = iconPackPackageName,
                        showLabel = false,
                        isLongClickEnabled = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}
