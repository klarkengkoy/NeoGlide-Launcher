package com.samidevstudio.neoglide.ui.components.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.ui.drawer.DrawerViewModel
import com.samidevstudio.neoglide.ui.utils.toIcon

@Composable
fun AddCategoryDialogRefined(
    onDismiss: () -> Unit,
    drawerViewModel: DrawerViewModel,
    onAddCustom: (String, String?) -> Unit,
    onAddBuiltIn: (String) -> Unit,
    onSwitchToVertical: () -> Unit
) {
    val context = LocalContext.current
    val categorizedApps by drawerViewModel.categorizedApps.collectAsState()
    val preferences by drawerViewModel.userPreferences.collectAsState()
    val isBottomRail = preferences.categoryBarType == com.samidevstudio.neoglide.data.repository.CategoryBarType.BOTTOM
    
    val enabledCategories = categorizedApps.keys.mapNotNull { it?.name }.toSet()
    val unusedBuiltIn = AppCategory.builtInValues.filter { it.name !in enabledCategories && it != AppCategory.OTHER }
    
    var showCustomFlow by remember { mutableStateOf(false) }
    var capacityExceeded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        capacityExceeded = !drawerViewModel.checkCategoryCapacity(context)
    }

    if (showCustomFlow) {
        AddCategoryDialog(
            onDismiss = { showCustomFlow = false },
            onConfirm = { name, icon ->
                onAddCustom(name, icon)
                onDismiss()
            },
            showCapacityWarning = capacityExceeded,
            onSwitchToVertical = onSwitchToVertical
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (capacityExceeded) {
                    CapacityWarning(
                        isBottomRail = isBottomRail,
                        onSwitchToVertical = onSwitchToVertical
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    "Select a built-in category to enable it, or create a custom one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Surface(
                                onClick = { if (!capacityExceeded) showCustomFlow = true },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (capacityExceeded) 0.5f else 1f),
                                enabled = !capacityExceeded
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Create Custom Category", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Built-in Categories", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        items(unusedBuiltIn.chunked(2)) { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { category ->
                                    BuiltInSelectionItem(
                                        category = category,
                                        enabled = !capacityExceeded,
                                        modifier = Modifier.weight(1f),
                                        onClick = { 
                                            onAddBuiltIn(category.name)
                                            onDismiss()
                                        }
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CapacityWarning(isBottomRail: Boolean, onSwitchToVertical: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "Category rail is full.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                if (isBottomRail) {
                    Text(
                        "You cannot add more categories. Try switching to a vertical rail for more space.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(
                        onClick = onSwitchToVertical,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Switch to Vertical Rail", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Text(
                        "You have reached the maximum number of categories supported by the vertical rail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun BuiltInSelectionItem(
    category: AppCategory,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                category.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                category.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}
