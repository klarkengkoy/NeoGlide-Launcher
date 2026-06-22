package com.samidevstudio.neoglide.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.toIcon
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun FolderExpansion(
    folderId: Int,
    label: String,
    apps: List<AppModel>,
    unitWidth: androidx.compose.ui.unit.Dp = 80.dp,
    unitHeight: androidx.compose.ui.unit.Dp = 96.dp,
    iconSize: androidx.compose.ui.unit.Dp = 56.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onDismiss: () -> Unit,
    onLabelChange: (String) -> Unit,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onDissolve: () -> Unit = {},
    onAddApps: () -> Unit = {},
    onMoveToCategory: (AppCategory) -> Unit = {},
    isDrawerFolder: Boolean = true,
    currentCategory: AppCategory? = null,
    allCategories: List<AppCategory?> = emptyList(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    refreshTrigger: Int = 0,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String) -> Unit = {},
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    isLocked: Boolean = false,
    autoFocusLabel: Boolean = false,
    onAppDragStart: (AppModel, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> },
    onAppDrag: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onAppDragOut: (AppModel, androidx.compose.ui.geometry.Offset, androidx.compose.ui.geometry.Offset) -> Unit = { _, _, _ -> },
    onAppDragEnd: () -> Unit = {},
    onAppDragCancel: () -> Unit = {}
) {
    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = label)) }
    var draggingApp by remember { mutableStateOf<AppModel?>(null) }
    var isDragConfirmed by remember { mutableStateOf(false) }
    var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var isDraggedOut by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var grabPoint by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rootCoords by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(label) {
        if (label != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = label)
        }
    }

    LaunchedEffect(autoFocusLabel) {
        if (autoFocusLabel) {
            textFieldValue = textFieldValue.copy(selection = TextRange(label.length))
            focusRequester.requestFocus()
        }
    }

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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row: Centered Title + Menu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Folder Label (Editable)
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = {
                                if (!isLocked) {
                                    textFieldValue = it
                                    onLabelChange(it.text)
                                }
                            },
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            enabled = !isLocked,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .then(if (isLocked) Modifier.clickable { 
                                    android.widget.Toast.makeText(context, "Locked from launcher settings", android.widget.Toast.LENGTH_SHORT).show()
                                } else Modifier)
                        )

                        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                            IconButton(onClick = { 
                                if (!isLocked) {
                                    showMenu = true 
                                } else {
                                    android.widget.Toast.makeText(context, "Locked from launcher settings", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Move apps here") },
                                    onClick = {
                                        showMenu = false
                                        onAddApps()
                                    }
                                )

                                if (isDrawerFolder) {
                                    var showCategoryMenu by remember { mutableStateOf(false) }
                                    
                                    DropdownMenuItem(
                                        text = { Text("Move folder to...") },
                                        onClick = { showCategoryMenu = true },
                                        trailingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                                    )

                                    if (showCategoryMenu) {
                                        DropdownMenu(
                                            expanded = showCategoryMenu,
                                            onDismissRequest = { showCategoryMenu = false }
                                        ) {
                                            val sortedCats = if (allCategories.isNotEmpty()) {
                                                allCategories.filterNotNull().filter { it != AppCategory.HIDDEN && it != currentCategory }
                                            } else {
                                                AppCategory.builtInValues.filter { it != AppCategory.FOLDER && it != AppCategory.HIDDEN && it != currentCategory }
                                            }

                                            sortedCats.forEach { category ->
                                                DropdownMenuItem(
                                                    text = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = category.toIcon(),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    },
                                                    onClick = {
                                                        showCategoryMenu = false
                                                        showMenu = false
                                                        onMoveToCategory(category)
                                                        onDismiss()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                DropdownMenuItem(
                                    text = { 
                                        Text(if (isDrawerFolder) "Dissolve folder" else "Remove from Home") 
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDissolve()
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }

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
                            var showAppMenu by remember { mutableStateOf(false) }
                            var shortcuts by remember { mutableStateOf<List<AppShortcut>>(emptyList()) }

                            LaunchedEffect(showAppMenu) {
                                if (showAppMenu) {
                                    shortcuts = getShortcuts(app.packageName)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .onGloballyPositioned { itemCoords = it }
                                    .graphicsLayer {
                                        alpha = if (isBeingDragged && isDragConfirmed) 0f else 1f
                                    }
                                    .pointerInput(app.packageName) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                if (!isLocked) {
                                                    onHapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                                    draggingApp = app
                                                    accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                                    isDragConfirmed = false
                                                    
                                                    // offset is the touch point relative to the item (0..size)
                                                    // This IS the grabPoint.
                                                    grabPoint = offset 
                                                    
                                                    // Standardize dragOffset to TOP-LEFT of the icon in Window coordinates
                                                    val touchWindow = itemCoords?.localToWindow(offset) ?: androidx.compose.ui.geometry.Offset.Zero
                                                    dragOffset = touchWindow - offset
                                                } else {
                                                    android.widget.Toast.makeText(context, "Locked from launcher settings", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            onDrag = { change, amount ->
                                                if (!isLocked) {
                                                    change.consume()
                                                    accumulatedDrag += amount
                                                    dragOffset += amount

                                                    if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                                        isDragConfirmed = true
                                                        onHapticFeedback(HapticEngine.HapticType.DRAG_START)
                                                        onAppDragStart(app, dragOffset, grabPoint)
                                                    }

                                                    if (isDragConfirmed) {
                                                        onAppDrag(amount)

                                                        // Introduce a slop/threshold before collapsing the folder
                                                        val movement = accumulatedDrag.getDistance()
                                                        val slop = with(density) { 24.dp.toPx() }

                                                        if (movement > slop && draggingApp != null && !isDraggedOut) {
                                                            // Collapse only after meaningful movement
                                                            isDraggedOut = true
                                                            onAppDragOut(app, dragOffset, amount)
                                                        }
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                if (!isLocked) {
                                                    if (isDragConfirmed) {
                                                        onHapticFeedback(HapticEngine.HapticType.DRAG_END)
                                                        // Only collapse if we actually dragged out
                                                        onAppDragEnd()
                                                    } else {
                                                        showAppMenu = true
                                                        // Don't call onAppDragEnd() here yet, let the menu show
                                                    }
                                                }
                                                draggingApp = null
                                                isDraggedOut = false
                                                isDragConfirmed = false
                                            },
                                            onDragCancel = {
                                                if (!isLocked) {
                                                    onAppDragCancel()
                                                }
                                                draggingApp = null
                                                isDraggedOut = false
                                                isDragConfirmed = false
                                            }
                                        )
                                    }
                            ) {
                                AppItem(
                                    app = app,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    iconSize = iconSize,
                                    fontSize = fontSize,
                                    useMonochrome = useMonochrome,
                                    iconPackPackageName = iconPackPackageName,
                                    sharedElementKeyPrefix = "folder-$folderId",
                                    showLabel = true,
                                    isLongClickEnabled = false, // Disable AppItem's internal long-click
                                    onLongClick = null,
                                    refreshTrigger = refreshTrigger,
                                    onClick = { options -> onAppClick(app.packageName, options) }
                                )

                                if (showAppMenu) {
                                    AppContextMenu(
                                        expanded = true,
                                        onDismissRequest = { showAppMenu = false },
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
        val isLifting = draggingApp != null && isDragConfirmed
        val liftScale by animateFloatAsState(
            targetValue = if (isLifting) 1.2f else 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "liftScale"
        )
        val liftShadow by animateDpAsState(
            targetValue = if (isLifting) 16.dp else 0.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "liftShadow"
        )

        if (isDragConfirmed && !isDraggedOut) {
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
                        .size(unitWidth, unitHeight)
                        .graphicsLayer {
                            scaleX = liftScale
                            scaleY = liftScale
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                                grabPoint.x / with(density) { unitWidth.toPx() },
                                grabPoint.y / with(density) { unitHeight.toPx() }
                            )
                            shadowElevation = liftShadow.toPx()
                            shape = RoundedCornerShape(16.dp)
                            clip = liftScale > 1f
                        }
                ) {
                    AppItem(
                        app = app,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        iconSize = iconSize,
                        fontSize = fontSize,
                        useMonochrome = useMonochrome,
                        iconPackPackageName = iconPackPackageName,
                        showLabel = false,
                        isLongClickEnabled = false,
                        refreshTrigger = refreshTrigger,
                        onClick = {}
                    )
                }
            }
        }
    }
}
