package com.samidevstudio.neoglide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.samidevstudio.neoglide.domain.model.AppModel

enum class PickerSortMode(val label: String) {
    ALPHABETICAL("A-Z"),
    CATEGORY("Category"),
    RECENT("Recent")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    title: String = "Add Application",
    apps: List<AppModel>,
    recentlyUsedApps: List<AppModel>,
    onAppSelected: (AppModel) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(PickerSortMode.ALPHABETICAL) }
    
    val filteredApps = remember(apps, searchQuery, sortMode, recentlyUsedApps) {
        val baseList = if (sortMode == PickerSortMode.RECENT) {
            // Intersect apps (available for picker) with recentlyUsedApps to maintain filtering
            val availablePackageNames = apps.map { it.packageName }.toSet()
            recentlyUsedApps.filter { it.packageName in availablePackageNames }
        } else {
            apps
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // SORTING CHIPS
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
                    if (sortMode == PickerSortMode.CATEGORY) {
                        val grouped = filteredApps.groupBy { it.category }.toSortedMap(compareBy { it.name })
                        grouped.forEach { (category, categoryApps) ->
                            item(key = category.name) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                                )
                            }
                            items(categoryApps.sortedBy { it.label }, key = { "cat_${it.packageName}" }) { app ->
                                AppPickerItem(app) { onAppSelected(app) }
                            }
                        }
                    } else {
                        val finalSortedList = if (sortMode == PickerSortMode.ALPHABETICAL) {
                            filteredApps.sortedBy { it.label }
                        } else {
                            filteredApps // RECENT is already sorted
                        }
                        
                        items(finalSortedList, key = { it.packageName }) { app ->
                            AppPickerItem(app) { onAppSelected(app) }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAppPickerDialog(
    title: String,
    allApps: List<AppModel>,
    memberPackageNames: Set<String>,
    recentlyUsedApps: List<AppModel>,
    onToggleMember: (AppModel, Boolean) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(PickerSortMode.ALPHABETICAL) }

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

    val (members, others) = remember(filteredApps, memberPackageNames) {
        filteredApps.partition { it.packageName in memberPackageNames }
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )

                Text(
                    text = "Select two or more apps to group them into a folder. Selected apps will be moved into this new folder regardless of which category or folder they are currently in.",
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
                    if (members.isNotEmpty()) {
                        item(key = "header_members") {
                            Text(
                                text = "Folder Members",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(members, key = { "member_${it.packageName}" }) { app ->
                            MultiAppPickerItem(app, true) { onToggleMember(app, it) }
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) }
                    }

                    if (sortMode == PickerSortMode.CATEGORY) {
                        val grouped = others.groupBy { it.category }.toSortedMap(compareBy { it.name })
                        grouped.forEach { (category, categoryApps) ->
                            item(key = category.name) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp)
                                )
                            }
                            items(categoryApps.sortedBy { it.label }, key = { "cat_${it.packageName}" }) { app ->
                                MultiAppPickerItem(app, false) { onToggleMember(app, it) }
                            }
                        }
                    } else {
                        val finalSortedList = if (sortMode == PickerSortMode.ALPHABETICAL) others.sortedBy { it.label } else others
                        items(finalSortedList, key = { "other_${it.packageName}" }) { app ->
                            MultiAppPickerItem(app, false) { onToggleMember(app, it) }
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
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MultiAppPickerItem(
    app: AppModel,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!isChecked) },
        shape = RoundedCornerShape(12.dp),
        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(6.dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = app.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = isChecked,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun AppPickerItem(app: AppModel, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
