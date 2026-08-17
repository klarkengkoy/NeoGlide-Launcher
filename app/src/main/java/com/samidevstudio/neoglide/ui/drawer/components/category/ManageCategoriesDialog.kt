package com.samidevstudio.neoglide.ui.drawer.components.category

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.ui.drawer.DrawerViewModel
import com.samidevstudio.neoglide.ui.utils.icons.resolveIcon
import com.samidevstudio.neoglide.ui.utils.icons.toIcon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sign

@Composable
fun ManageCategoriesDialog(
    onDismiss: () -> Unit,
    drawerViewModel: DrawerViewModel,
    onRemove: (AppCategory) -> Unit,
    onReorder: (List<String>) -> Unit,
    onUpdate: (AppCategory, String, String?) -> Unit
) {
    val categorizedApps by drawerViewModel.categorizedApps.collectAsState()
    var localCategories by remember { mutableStateOf<List<AppCategory>>(emptyList()) }

    LaunchedEffect(categorizedApps) {
        val newCategories = categorizedApps.map { it.first }.filterNotNull()
            .filter { it != AppCategory.HIDDEN && it != AppCategory.OTHER }
        if (localCategories != newCategories) {
            localCategories = newCategories
        }
    }

    val slideStates = remember { mutableStateMapOf<AppCategory, SlideState>() }
    var itemHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    var categoryToEdit by remember { mutableStateOf<AppCategory?>(null) }
    var showAddCategory by remember { mutableStateOf(false) }

    if (showAddCategory) {
        AddCategoryDialogRefined(
            onDismiss = { showAddCategory = false },
            drawerViewModel = drawerViewModel,
            onAddBuiltIn = { name, selected ->
                drawerViewModel.addBuiltInCategory(name, selected)
                showAddCategory = false
            },
            onAddCustom = { name, icon, selected ->
                drawerViewModel.addCustomCategory(name, icon, selected)
                showAddCategory = false
            },
            onSwitchToVertical = { showAddCategory = false }
        )
    }

    categoryToEdit?.let { category ->
        EditCategoryDialog(
            category = category,
            onDismiss = { categoryToEdit = null },
            onConfirm = { newLabel, newIcon ->
                onUpdate(category, newLabel, newIcon)
                categoryToEdit = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Manage Categories", modifier = Modifier.weight(1f))
                IconButton(onClick = { showAddCategory = true }) {
                    Icon(Icons.Default.Add, "Add Category", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Add, remove, rename, or reorder categories. Long press and drag to change the order in your category rail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    if (localCategories.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No categories enabled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                    items(localCategories, key = { it.name }) { category ->
                        val slideState = slideStates[category] ?: SlideState.NONE
                        val itemOffset by animateIntOffsetAsState(
                            targetValue = when (slideState) {
                                SlideState.UP -> IntOffset(0, -itemHeightPx)
                                SlideState.DOWN -> IntOffset(0, itemHeightPx)
                                SlideState.NONE -> IntOffset(0, 0)
                            },
                            label = "slide"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .onGloballyPositioned {
                                    if (itemHeightPx == 0) {
                                        itemHeightPx = it.size.height + with(density) { 8.dp.roundToPx() }
                                    }
                                }
                                .offset { itemOffset }
                                .dragToReorder(
                                    item = category,
                                    itemList = localCategories,
                                    itemHeight = itemHeightPx,
                                    updateSlideState = { item, state -> slideStates[item] = state },
                                    onStopDrag = { oldIndex, newIndex ->
                                        if (oldIndex != newIndex) {
                                            val newList = localCategories.toMutableList()
                                            val movedItem = newList.removeAt(oldIndex)
                                            newList.add(newIndex, movedItem)
                                            localCategories = newList
                                            onReorder(newList.map { it.name })
                                        }
                                        slideStates.clear()
                                    }
                                )
                        ) {
                            CategoryItem(
                                category = category,
                                onRemove = { onRemove(category) },
                                onEdit = { categoryToEdit = category }
                            )
                        }
                    }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun CategoryItem(
    category: AppCategory,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Reorder,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEdit() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.toIcon(),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                category.displayName,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EditCategoryDialog(
    category: AppCategory,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(category.displayName) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }
    val availableIcons = remember { AppCategory.builtInValues.mapNotNull { it.iconName }.distinct() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableIcons.size) { index ->
                            val iconName = availableIcons[index]
                            val isSelected = selectedIcon == iconName
                            Surface(
                                onClick = { selectedIcon = iconName },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        resolveIcon(iconName),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, selectedIcon) }, enabled = name.isNotBlank()) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

enum class SlideState { NONE, UP, DOWN }

fun <T> Modifier.dragToReorder(
    item: T,
    itemList: List<T>,
    itemHeight: Int,
    updateSlideState: (item: T, slideState: SlideState) -> Unit,
    onStartDrag: (currIndex: Int) -> Unit = {},
    onStopDrag: (currIndex: Int, destIndex: Int) -> Unit
): Modifier = composed {
    val offsetY = remember { Animatable(0f) }

    pointerInput(item, itemList) {
        coroutineScope {
            val itemIndex = itemList.indexOf(item)
            if (itemIndex == -1) return@coroutineScope

            val offsetToSlide = itemHeight / 2
            var numberOfSlidItems = 0
            var currentOffsetSign = 0
            var listOffset = 0

            val onDragStart = {
                launch {
                    offsetY.stop()
                }
                onStartDrag(itemIndex)
            }

            val onDragging = { change: PointerInputChange ->
                val verticalDragOffset = offsetY.value + change.positionChange().y

                launch {
                    offsetY.snapTo(verticalDragOffset)

                    val newOffsetSign = offsetY.value.sign.toInt()

                    if (newOffsetSign != currentOffsetSign && currentOffsetSign != 0) {
                        for (i in 1..numberOfSlidItems) {
                            val targetIndex = itemIndex + i * currentOffsetSign
                            if (targetIndex in itemList.indices) {
                                updateSlideState(itemList[targetIndex], SlideState.NONE)
                            }
                        }
                        numberOfSlidItems = 0
                    }
                    currentOffsetSign = newOffsetSign

                    val newNumberOfSlidItems = calculateNumberOfSlidItems(
                        offsetY.value * currentOffsetSign,
                        itemHeight,
                        offsetToSlide,
                        numberOfSlidItems
                    )

                    if (newNumberOfSlidItems != numberOfSlidItems) {
                        val maxRange = maxOf(numberOfSlidItems, newNumberOfSlidItems)
                        for (i in 1..maxRange) {
                            val targetIndex = itemIndex + i * currentOffsetSign
                            if (targetIndex in itemList.indices) {
                                updateSlideState(
                                    itemList[targetIndex],
                                    if (i <= newNumberOfSlidItems) {
                                        if (currentOffsetSign == 1) SlideState.UP else SlideState.DOWN
                                    } else {
                                        SlideState.NONE
                                    }
                                )
                            }
                        }
                        numberOfSlidItems = newNumberOfSlidItems
                    }

                    listOffset = numberOfSlidItems * currentOffsetSign
                }
                if (change.positionChange() != Offset.Zero) change.consume()
            }

            val onDragEnd = {
                launch {
                    onStopDrag(itemIndex, itemIndex + listOffset)
                    offsetY.snapTo(0f)
                }
            }

            detectDragGesturesAfterLongPress(
                onDragStart = { onDragStart() },
                onDrag = { change, _ -> onDragging(change) },
                onDragEnd = { onDragEnd() }
            )
        }
    }.offset {
        IntOffset(0, offsetY.value.roundToInt())
    }
}

private const val NO_ITEMS_MOVED = 0

private fun calculateNumberOfSlidItems(
    offsetY: Float,
    itemHeight: Int,
    offsetToSlide: Int,
    previousNumberOfItems: Int
): Int {
    if (itemHeight <= 0) return NO_ITEMS_MOVED
    val numberOfItemsInOffset = (offsetY / itemHeight).toInt()
    val numberOfItemsPlusOffset = ((offsetY + offsetToSlide) / itemHeight).toInt()
    val numberOfItemsMinusOffset = ((offsetY - offsetToSlide - 1) / itemHeight).toInt()

    return when {
        offsetY - offsetToSlide - 1 < 0 -> NO_ITEMS_MOVED
        numberOfItemsPlusOffset > numberOfItemsInOffset -> numberOfItemsPlusOffset
        numberOfItemsMinusOffset < numberOfItemsInOffset -> numberOfItemsInOffset
        else -> previousNumberOfItems
    }
}
