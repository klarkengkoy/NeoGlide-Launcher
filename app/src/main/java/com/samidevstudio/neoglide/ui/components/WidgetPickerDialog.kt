package com.samidevstudio.neoglide.ui.components

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.samidevstudio.neoglide.domain.model.AppModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerDialog(
    appsWithWidgets: List<AppModel>,
    allWidgetProviders: List<AppWidgetProviderInfo>,
    onGetWidgets: (String) -> List<AppWidgetProviderInfo>,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAppPackage by remember { mutableStateOf<String?>(null) }
    
    val filteredApps = remember(appsWithWidgets, searchQuery) {
        if (searchQuery.isBlank()) appsWithWidgets
        else appsWithWidgets.filter { it.label.contains(searchQuery, ignoreCase = true) }
    }

    // Direct widget search across all apps
    val context = LocalContext.current
    val appLabelMap = remember(appsWithWidgets) { appsWithWidgets.associate { it.packageName to it.label } }
    
    val allWidgetsWithMetadata = remember(allWidgetProviders, appLabelMap) {
        allWidgetProviders.map { info ->
            Triple(info, info.loadLabel(context.packageManager), appLabelMap[info.provider.packageName] ?: "")
        }
    }
    
    val searchedWidgets = remember(allWidgetsWithMetadata, searchQuery) {
        if (searchQuery.isBlank()) emptyList<Triple<AppWidgetProviderInfo, String, String>>()
        else {
            allWidgetsWithMetadata.filter { (info, label, appName) ->
                label.contains(searchQuery, ignoreCase = true) || appName.contains(searchQuery, ignoreCase = true)
            }
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
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (selectedAppPackage != null) {
                        IconButton(onClick = { 
                            selectedAppPackage = null 
                            searchQuery = "" // Clear search when going back
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    Text(
                        text = if (selectedAppPackage == null) {
                            if (searchQuery.isEmpty()) "Select Application" else "Search Results"
                        } else "Select Widget",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (selectedAppPackage == null) {
                    // Search and App List Page
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        placeholder = { Text("Search widgets...") },
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

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        if (searchQuery.isEmpty()) {
                            items(filteredApps, key = { it.packageName }) { app ->
                                val widgetCount = remember(app.packageName) { onGetWidgets(app.packageName).size }
                                AppPickerItem(app, widgetCount) { selectedAppPackage = app.packageName }
                            }
                        } else {
                            items(searchedWidgets) { (info, _, appName) ->
                                WidgetProviderItem(info, appName = appName) { onWidgetSelected(info) }
                            }
                        }
                    }
                } else {
                    // Widget List Page (Filtered by App)
                    val widgets = remember(selectedAppPackage) { onGetWidgets(selectedAppPackage!!) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(widgets) { info ->
                            WidgetProviderItem(info) { onWidgetSelected(info) }
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

@Composable
private fun WidgetProviderItem(
    info: AppWidgetProviderInfo,
    appName: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val label = info.loadLabel(context.packageManager)
    
    val spanX = ((info.minWidth + 30) / 70).coerceIn(1, 5)
    val spanY = ((info.minHeight + 30) / 70).coerceIn(1, 6)

    val previewDrawable = remember(info) {
        try {
            // Priority 1: loadPreviewImage
            info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
                ?: info.loadIcon(context, context.resources.displayMetrics.densityDpi)
        } catch (_: Exception) {
            null
        }
    }

    // Priority 2: previewLayout (Android 12+)
    val hasPreviewLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.previewLayout != 0

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Widget Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 220.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (hasPreviewLayout && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Use key to prevent view recycling issues with RemoteViews
                    key(info.provider.flattenToString()) {
                        AndroidView(
                            factory = { ctx ->
                                try {
                                    val rv = RemoteViews(info.provider.packageName, info.previewLayout)
                                    rv.apply(ctx, null)
                                } catch (_: Exception) {
                                    android.widget.ImageView(ctx).apply {
                                        previewDrawable?.let { setImageBitmap(it.toBitmap()) }
                                    }
                                }
                            },
                            modifier = Modifier.wrapContentSize()
                        )
                    }
                } else if (previewDrawable != null) {
                    Image(
                        bitmap = previewDrawable.toBitmap().asImageBitmap(),
                        contentDescription = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(packageName = info.provider.packageName, contentDescription = null, modifier = Modifier.size(40.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (appName != null) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${spanX} × ${spanY}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AppPickerItem(app: AppModel, widgetCount: Int, onClick: () -> Unit) {
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
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = widgetCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
