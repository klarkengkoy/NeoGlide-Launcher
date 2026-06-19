package com.samidevstudio.neoglide.ui.components

import android.appwidget.AppWidgetProviderInfo
import android.os.Build
import android.widget.RemoteViews
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.ui.utils.WidgetUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerDialog(
    appsWithWidgets: List<AppModel>,
    allWidgetProviders: List<AppWidgetProviderInfo>,
    unitWidthDp: Float,
    unitHeightDp: Float,
    maxColumns: Int,
    onGetWidgets: (String) -> List<AppWidgetProviderInfo>,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit,
    onDismissRequest: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedAppPackage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

    // Combined search logic: search for apps and widgets
    val appsToDisplay = remember(appsWithWidgets, allWidgetProviders, searchQuery) {
        if (searchQuery.isBlank()) {
            appsWithWidgets.sortedBy { it.label }
        } else {
            // Find apps whose label matches OR who have a widget whose label matches
            val matchingWidgetPackages = allWidgetProviders.filter { info ->
                info.loadLabel(context.packageManager).contains(searchQuery, ignoreCase = true)
            }.map { it.provider.packageName }.toSet()

            appsWithWidgets.filter { app ->
                app.label.contains(searchQuery, ignoreCase = true) || 
                matchingWidgetPackages.contains(app.packageName)
            }.sortedBy { it.label }
        }
    }

    // Auto-expand if searching and only few results, or if specific widget match
    val finalExpandedPackage = remember(expandedAppPackage, searchQuery, appsToDisplay) {
        if (searchQuery.isNotEmpty() && appsToDisplay.size == 1) {
            appsToDisplay.first().packageName
        } else expandedAppPackage
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header - Centered as in screenshot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Widgets",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it 
                        if (it.isEmpty()) expandedAppPackage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    items(appsToDisplay, key = { it.packageName }) { app ->
                        val isExpanded = finalExpandedPackage == app.packageName
                        val widgets = remember(app.packageName) { onGetWidgets(app.packageName) }
                        
                        // Filter widgets if searching
                        val filteredWidgets = remember(widgets, searchQuery) {
                            if (searchQuery.isBlank()) widgets
                            else widgets.filter { info ->
                                info.loadLabel(context.packageManager).contains(searchQuery, ignoreCase = true) ||
                                app.label.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        if (filteredWidgets.isNotEmpty()) {
                            AppAccordionItem(
                                app = app,
                                widgetCount = widgets.size,
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedAppPackage = if (isExpanded) null else app.packageName
                                }
                            )

                            if (isExpanded) {
                                // Intelligent row packing
                                val widgetRows = remember(filteredWidgets, unitWidthDp, unitHeightDp) {
                                    val result = mutableListOf<List<AppWidgetProviderInfo>>()
                                    var currentPair = mutableListOf<AppWidgetProviderInfo>()
                                    
                                    filteredWidgets.forEach { info ->
                                        val (spanX, spanY) = WidgetUtils.calculateProjectedWidgetSpan(context, info, unitWidthDp, unitHeightDp, maxColumns)
                                        val isWide = spanX / spanY >= 1.5f
                                        
                                        if (isWide) {
                                            if (currentPair.isNotEmpty()) {
                                                result.add(currentPair)
                                                currentPair = mutableListOf()
                                            }
                                            result.add(listOf(info))
                                        } else {
                                            currentPair.add(info)
                                            if (currentPair.size == 2) {
                                                result.add(currentPair)
                                                currentPair = mutableListOf()
                                            }
                                        }
                                    }
                                    if (currentPair.isNotEmpty()) result.add(currentPair)
                                    result
                                }

                                widgetRows.forEach { rowWidgets ->
                                    val isSingleItem = rowWidgets.size == 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalArrangement = if (isSingleItem) Arrangement.Center else Arrangement.spacedBy(16.dp)
                                    ) {
                                        rowWidgets.forEach { info ->
                                            Box(
                                                modifier = Modifier.then(
                                                    if (isSingleItem) Modifier.fillMaxWidth(0.9f) // Slight margin for centered single items
                                                    else Modifier.weight(1f)
                                                )
                                            ) {
                                                WidgetProviderItem(
                                                    info = info,
                                                    unitWidthDp = unitWidthDp,
                                                    unitHeightDp = unitHeightDp,
                                                    maxColumns = maxColumns
                                                ) { onWidgetSelected(info) }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderItem(
    info: AppWidgetProviderInfo,
    unitWidthDp: Float,
    unitHeightDp: Float,
    maxColumns: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val label = info.loadLabel(context.packageManager)
    
    val (spanX, spanY) = remember(info, unitWidthDp, unitHeightDp, maxColumns) { 
        WidgetUtils.calculateProjectedWidgetSpan(context, info, unitWidthDp, unitHeightDp, maxColumns) 
    }

    val previewDrawable = remember(info) {
        try {
            info.loadPreviewImage(context, context.resources.displayMetrics.densityDpi)
                ?: info.loadIcon(context, context.resources.displayMetrics.densityDpi)
        } catch (_: Exception) {
            null
        }
    }

    val hasPreviewLayout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.previewLayout != 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Widget Preview Container
        Box(
            modifier = Modifier
                .height(100.dp)
                .aspectRatio(spanX / spanY, matchHeightConstraintsFirst = true)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (hasPreviewLayout && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                AppIcon(packageName = info.provider.packageName, contentDescription = null, modifier = Modifier.size(40.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "${if (spanX % 1f == 0f) spanX.toInt() else spanX} × ${if (spanY % 1f == 0f) spanY.toInt() else spanY}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AppAccordionItem(
    app: AppModel, 
    widgetCount: Int, 
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$widgetCount ${if (widgetCount == 1) "widget" else "widgets"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
