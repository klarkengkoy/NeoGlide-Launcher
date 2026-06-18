package com.samidevstudio.neoglide.ui.components.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.ui.drawer.DrawerViewModel
import com.samidevstudio.neoglide.ui.utils.resolveIcon
import com.samidevstudio.neoglide.ui.utils.toIcon

@Composable
fun ManageCategoriesDialog(
    onDismiss: () -> Unit,
    drawerViewModel: DrawerViewModel,
    onRemove: (AppCategory) -> Unit,
    onReorder: (List<String>) -> Unit,
    onUpdate: (String, String, String?) -> Unit
) {
    val categorizedApps by drawerViewModel.categorizedApps.collectAsState()
    val initialCategories = remember(categorizedApps) {
        categorizedApps.keys.filterNotNull()
            .filter { it != AppCategory.HIDDEN && it != AppCategory.OTHER }
    }
    
    var orderedList by remember(initialCategories) { mutableStateOf(initialCategories) }
    var categoryToConfirmDeletion by remember { mutableStateOf<AppCategory?>(null) }
    var categoryToEdit by remember { mutableStateOf<AppCategory?>(null) }

    if (categoryToEdit != null) {
        EditCategoryDialog(
            category = categoryToEdit!!,
            onDismiss = { categoryToEdit = null },
            onConfirm = { newName, newIcon ->
                onUpdate(categoryToEdit!!.name, newName, newIcon)
                categoryToEdit = null
            }
        )
    }

    if (categoryToConfirmDeletion != null) {
        AlertDialog(
            onDismissRequest = { categoryToConfirmDeletion = null },
            title = { Text("Remove Category") },
            text = { Text("Are you sure you want to remove '${categoryToConfirmDeletion?.name}'? Apps will be redistributed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove(categoryToConfirmDeletion!!)
                        categoryToConfirmDeletion = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { categoryToConfirmDeletion = null }) { Text("Cancel") }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Reorder, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Manage Categories")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Long-press the handle to drag and reorder. Top items have higher priority.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.heightIn(max = 450.dp)) {
                    if (orderedList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No categories enabled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        ReorderableCategoryList(
                            list = orderedList,
                            onMove = { from, to ->
                                val newList = orderedList.toMutableList()
                                val item = newList.removeAt(from)
                                newList.add(to, item)
                                orderedList = newList
                                onReorder(newList.map { it.name })
                            },
                            onRemove = { categoryToConfirmDeletion = it },
                            onEdit = { categoryToEdit = it }
                        )
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
fun EditCategoryDialog(
    category: AppCategory,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.iconName) }
    
    val availableIcons = AppCategory.builtInValues.mapNotNull { it.iconName }.distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("Change Icon", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                Box(modifier = Modifier.height(200.dp)) {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableIcons.size) { index ->
                            val iconName = availableIcons[index]
                            val icon = resolveIcon(iconName)
                            Surface(
                                onClick = { selectedIcon = iconName },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedIcon == iconName) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (selectedIcon == iconName) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (selectedIcon == iconName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedIcon) },
                enabled = name.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReorderableCategoryList(
    list: List<AppCategory>,
    onMove: (Int, Int) -> Unit,
    onRemove: (AppCategory) -> Unit,
    onEdit: (AppCategory) -> Unit
) {
    val lazyListState = rememberLazyListState()
    
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(list, key = { _, item -> item.name }) { index, category ->
            val isBeingDragged = draggedIndex == index
            
            val currentOnMove by rememberUpdatedState(onMove)
            val currentListSize by rememberUpdatedState(list.size)
            val currentIndex by rememberUpdatedState(index)

            ReorderableItem(
                category = category,
                onRemove = { onRemove(category) },
                onEdit = { onEdit(category) },
                isDragging = isBeingDragged,
                dragOffset = dragOffset,
                onDragStart = { draggedIndex = currentIndex },
                onDragEnd = { 
                    draggedIndex = null
                    dragOffset = 0f
                },
                onDrag = { amount ->
                    dragOffset += amount
                    
                    val threshold = 72f // Approximate item height + spacing
                    if (dragOffset > threshold && currentIndex < currentListSize - 1) {
                        currentOnMove(currentIndex, currentIndex + 1)
                        draggedIndex = currentIndex + 1
                        dragOffset -= threshold
                    } else if (dragOffset < -threshold && currentIndex > 0) {
                        currentOnMove(currentIndex, currentIndex - 1)
                        draggedIndex = currentIndex - 1
                        dragOffset += threshold
                    }
                }
            )
        }
    }
}

@Composable
fun ReorderableItem(
    category: AppCategory,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDragging) 0.8f else 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = dragOffset
                    scaleX = 1.05f
                    scaleY = 1.05f
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    }
            ) {
                Icon(
                    Icons.Default.DragHandle, 
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onEdit() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.toIcon(), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
            }
        }
    }
}
