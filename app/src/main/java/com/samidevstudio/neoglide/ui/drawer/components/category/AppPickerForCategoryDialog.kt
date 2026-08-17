package com.samidevstudio.neoglide.ui.drawer.components.category

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.components.MultiAppPickerItem
import com.samidevstudio.neoglide.ui.components.PickerSortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerForCategoryDialog(
    category: AppCategory,
    allApps: List<AppModel>,
    recommendedPackageNames: List<String>,
    recentlyUsedApps: List<AppModel>,
    onConfirm: (List<String>) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(PickerSortMode.ALPHABETICAL) }
    val selectedPackages = remember { mutableStateOf(recommendedPackageNames.toSet()) }

    val filteredApps = remember(allApps, searchQuery, sortMode, recentlyUsedApps) {
        val baseList = if (sortMode == PickerSortMode.RECENT) {
            val availablePackageNames = allApps.map { it.packageName }.toSet()
            recentlyUsedApps.filter { it.packageName in availablePackageNames }
        } else {
            allApps
        }

        if (searchQuery.isBlank()) baseList
        else baseList.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    Dialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Add Category",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                Text(
                    text = "Choose applications to move into this category. Applications already in other categories will be relocated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PickerSortMode.entries.forEach { mode ->
                        FilterChip(
                            selected = sortMode == mode,
                            onClick = { sortMode = mode },
                            label = { Text(mode.label) },
                            leadingIcon = if (sortMode == mode) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    val (selectedApps, otherApps) = filteredApps.partition { it.packageName in selectedPackages.value }

                    if (selectedApps.isNotEmpty()) {
                        item(key = "header_selected") {
                            Text(
                                text = "${category.displayName} Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(selectedApps.sortedBy { it.label }, key = { "selected_${it.packageName}" }) { app ->
                            MultiAppPickerItem(
                                app = app,
                                isChecked = true,
                                onToggle = { isSelected ->
                                    if (!isSelected) {
                                        selectedPackages.value -= app.packageName
                                    }
                                }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) }
                    }

                    if (sortMode == PickerSortMode.CATEGORY) {
                        val grouped = otherApps.groupBy { it.category }.toSortedMap(compareBy { it.name })
                        grouped.forEach { (cat, categoryApps) ->
                            item(key = cat.name) {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                                )
                            }
                            items(categoryApps.sortedBy { it.label }, key = { "other_${it.packageName}" }) { app ->
                                MultiAppPickerItem(
                                    app = app,
                                    isChecked = false,
                                    onToggle = { isSelected ->
                                        if (isSelected) {
                                            selectedPackages.value += app.packageName
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        val sortedList = if (sortMode == PickerSortMode.ALPHABETICAL) otherApps.sortedBy { it.label } else otherApps
                        items(sortedList, key = { "other_${it.packageName}" }) { app ->
                            MultiAppPickerItem(
                                app = app,
                                isChecked = false,
                                onToggle = { isSelected ->
                                    if (isSelected) {
                                        selectedPackages.value += app.packageName
                                    }
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismissRequest() }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            onConfirm(selectedPackages.value.toList())
                            onDismissRequest()
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Confirm (${selectedPackages.value.size})")
                    }
                }
            }
        }
    }
}
