package com.samidevstudio.pxllauncherneo.ui.home

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as hiltViewModelV2
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import com.samidevstudio.pxllauncherneo.data.repository.AppLabelMode
import com.samidevstudio.pxllauncherneo.data.repository.NotificationDotMode
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.ui.components.*
import com.samidevstudio.pxllauncherneo.ui.drawer.DrawerScreen
import com.samidevstudio.pxllauncherneo.ui.settings.SettingsSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModelV2(),
    settingsViewModel: com.samidevstudio.pxllauncherneo.ui.settings.SettingsViewModel = hiltViewModelV2(),
    sharedTransitionScope: SharedTransitionScope,
    drawerViewModel: com.samidevstudio.pxllauncherneo.ui.drawer.DrawerViewModel = hiltViewModelV2(),
    onNavigateToDetail: (String) -> Unit = {},
) {
    val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val shouldShowDefaultPrompt by viewModel.shouldShowDefaultPrompt.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showWidgetMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var editingWidgetId by remember { mutableIntStateOf(-1) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val widgetConfigLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val info = viewModel.appWidgetManager.getAppWidgetInfo(appWidgetId)
                viewModel.addWidget(WidgetEntity(
                    widgetId = appWidgetId,
                    providerPackage = info?.provider?.packageName ?: "",
                    providerClass = info?.provider?.className ?: "",
                    label = info?.loadLabel(context.packageManager) ?: "Widget",
                    spanX = 4f,
                    spanY = 2f,
                    row = 0f,
                    column = 0f
                ))
            }
        }
    }

    val widgetPickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val info = viewModel.appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (info?.configure != null) {
                    try {
                        val intent = android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                            component = info.configure
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        widgetConfigLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Cannot configure this widget", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    viewModel.addWidget(WidgetEntity(
                        widgetId = appWidgetId,
                        providerPackage = info?.provider?.packageName ?: "",
                        providerClass = info?.provider?.className ?: "",
                        label = info?.loadLabel(context.packageManager) ?: "Widget",
                        spanX = 4f,
                        spanY = 2f,
                        row = 0f,
                        column = 0f
                    ))
                }
            }
        }
    }
    
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current
    val homeAlpha by animateFloatAsState(
        targetValue = if (showDrawer) 0f else 1f,
        animationSpec = tween(300),
        label = "homeAlpha"
    )

    LaunchedEffect(Unit) {
        viewModel.checkDefaultLauncher()
    }

    BackHandler(enabled = showDrawer || showSettings || showContextMenu || showWidgetMenu || editingWidgetId != -1) {
        if (showWidgetMenu) {
            showWidgetMenu = false
        } else if (editingWidgetId != -1) {
            editingWidgetId = -1
        } else if (showSettings) {
            showSettings = false
        } else if (showContextMenu) {
            showContextMenu = false
        } else {
            drawerViewModel.resetState()
            showDrawer = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    if (dragAmount < -20) {
                        showDrawer = true
                    }
                }
            }
    ) {
        // FULL SCREEN FROSTED GLASS
        AnimatedVisibility(
            visible = editingWidgetId != -1,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FrostedGlass(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 20.dp
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homeAlpha },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    editingWidgetId = -1
                                    showWidgetMenu = false
                                    focusManager.clearFocus()
                                },
                                onLongPress = { offset ->
                                    // Disable home screen menu when editing a widget
                                    if (editingWidgetId == -1 && !preferences.lockLayout) {
                                        contextMenuOffset = DpOffset(
                                            x = with(density) { offset.x.toDp() },
                                            y = with(density) { offset.y.toDp() }
                                        )
                                        showContextMenu = true
                                    } else if (preferences.lockLayout) {
                                        Toast.makeText(context, "Layout is locked", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                ) {
                    val unitWidth = maxWidth / 4f
                    val topOffset = 0.dp // Use padding from Scaffold
                    val bottomPadding = 0.dp // Use padding from Scaffold
                    val topOffsetPx = with(density) { topOffset.toPx() }
                    
                    // Adaptive Grid: Calculate nearest row count and stretch to fill height minus bottom padding
                    val availableHeight = maxHeight
                    val maxRows = (availableHeight / 96.dp).let { 
                        if (it % 1 > 0.7f) it.toInt() + 1 else it.toInt()
                    }.coerceAtLeast(1)
                    val unitHeight = availableHeight / maxRows


                    // GRID OVERLAY
                    if (editingWidgetId != -1) {
                        val isDark = isSystemInDarkTheme()
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val mainGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f)
                            val subGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                            
                            // Draw exactly maxRows lines, starting from topOffset
                            for (i in 0..maxRows) {
                                val y = topOffsetPx + (i * unitHeight.toPx())
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2f)
                                if (i < maxRows) {
                                    val midY = y + (unitHeight.toPx() / 2f)
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(0f, midY), androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = 1f)
                                }
                            }
                            
                            val gridBottomY = topOffsetPx + (maxRows * unitHeight.toPx())
                            for (i in 0..4) {
                                val x = i * unitWidth.toPx()
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(x, topOffsetPx), androidx.compose.ui.geometry.Offset(x, gridBottomY), strokeWidth = 2f)
                                if (i < 4) {
                                    val midX = x + (unitWidth.toPx() / 2f)
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(midX, topOffsetPx), androidx.compose.ui.geometry.Offset(midX, gridBottomY), strokeWidth = 1f)
                                }
                            }
                        }
                    }

                    // Dock Widget (Placed here to be covered by the scrim)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 48.dp)
                            .zIndex(0f),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            dockApps.forEach { app ->
                                AppItem(
                                    app = app,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    useMonochrome = preferences.useMonochromeIcons,
                                    iconPackPackageName = preferences.iconPackPackageName,
                                    isHidden = app.packageName in preferences.hiddenPackages,
                                    hasNotification = (preferences.notificationDotMode == NotificationDotMode.APP_ICON || 
                                                     preferences.notificationDotMode == NotificationDotMode.BOTH) &&
                                                     app.packageName in activeNotifications.keys,
                                    notificationCount = activeNotifications[app.packageName] ?: 0,
                                    showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                    sharedElementKeyPrefix = "dock",
                                    getShortcuts = { viewModel.getShortcuts(it) },
                                    onShortcutClick = { viewModel.launchShortcut(it) },
                                    onHideToggle = { 
                                        if (app.packageName in preferences.hiddenPackages) {
                                            viewModel.unhideApp(app.packageName)
                                        } else {
                                            viewModel.hideApp(app.packageName)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { options ->
                                    viewModel.launchApp(app.packageName, options)
                                }
                            }
                        }
                    }

                    // TOUCH BLOCKER (Scrim) - placed below the active widget but above everything else in the grid (including Dock)
                    if (editingWidgetId != -1) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(0.5f)
                                .pointerInput(Unit) {
                                    detectTapGestures(onTap = { 
                                        editingWidgetId = -1 
                                        showWidgetMenu = false
                                    })
                                }
                        )
                    }

                    val homeApps by viewModel.homeItems.collectAsStateWithLifecycle()
                    
                    homeApps.forEach { item ->
                        if (item is HomeItem.App) {
                            val app = item.appModel
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = unitWidth * item.column,
                                        y = topOffset + (unitHeight * item.row)
                                    )
                                    .size(unitWidth, unitHeight)
                            ) {
                                AppItem(
                                    app = app,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    useMonochrome = preferences.useMonochromeIcons,
                                    iconPackPackageName = preferences.iconPackPackageName,
                                    isHidden = app.packageName in preferences.hiddenPackages,
                                    hasNotification = (preferences.notificationDotMode == NotificationDotMode.APP_ICON || 
                                                     preferences.notificationDotMode == NotificationDotMode.BOTH) &&
                                                     app.packageName in activeNotifications.keys,
                                    notificationCount = activeNotifications[app.packageName] ?: 0,
                                    showLabel = preferences.appLabelMode == AppLabelMode.HOME_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                    sharedElementKeyPrefix = "home",
                                    getShortcuts = { viewModel.getShortcuts(it) },
                                    onShortcutClick = { viewModel.launchShortcut(it) },
                                    onHideToggle = { 
                                        if (app.packageName in preferences.hiddenPackages) {
                                            viewModel.unhideApp(app.packageName)
                                        } else {
                                            viewModel.hideApp(app.packageName)
                                        }
                                    }
                                ) { options ->
                                    viewModel.launchApp(app.packageName, options)
                                }
                            }
                        }
                    }

                    widgets.forEach { widget ->
                        val isCurrentEditing = editingWidgetId == widget.widgetId
                        PxlWidgetHost(
                            widgetId = widget.widgetId,
                            appWidgetHost = viewModel.appWidgetHost,
                            appWidgetManager = viewModel.appWidgetManager,
                            row = widget.row,
                            column = widget.column,
                            spanX = widget.spanX,
                            spanY = widget.spanY,
                            unitWidth = unitWidth,
                            unitHeight = unitHeight,
                            isEditing = isCurrentEditing,
                            modifier = Modifier
                                .offset(y = topOffset)
                                .zIndex(if (isCurrentEditing) 1f else 0f), // Lift active widget above scrim
                            onDragStart = { showWidgetMenu = false },
                            onLongClick = {
                                if (!preferences.lockLayout) {
                                    editingWidgetId = widget.widgetId
                                    
                                    val widgetLeft = (unitWidth * widget.column) + 4.dp
                                    val widgetTop = topOffset + (unitHeight * widget.row) + 4.dp
                                    val widgetWidth = (unitWidth * widget.spanX) - 8.dp
                                    val widgetHeight = (unitHeight * widget.spanY) - 8.dp
                                    val widgetBottom = widgetTop + widgetHeight
                                    val widgetRight = widgetLeft + widgetWidth
                                    
                                    // Estimated menu size (Our Dynamic Popup is now compact)
                                    val menuH = 100.dp 
                                    val menuW = 150.dp
                                    val gap = 12.dp
                                    
                                    var finalX = widgetLeft
                                    var finalY = widgetBottom + gap
                                    
                                    // gridW/H are derived from maxWidth/maxHeight which are Dp in BoxWithConstraints
                                    val gridW = maxWidth
                                    val gridH = maxHeight
                                    
                                    if (widgetBottom + menuH + gap > gridH) {
                                        if (widgetTop > menuH + gap) {
                                            finalY = widgetTop - menuH - gap
                                        } else {
                                            finalY = widgetTop
                                            if (widgetRight + menuW + gap <= gridW) {
                                                finalX = widgetRight + gap
                                            } else if (widgetLeft > menuW + gap) {
                                                finalX = widgetLeft - menuW - gap
                                            } else {
                                                finalX = (gridW - menuW) / 2f
                                                finalY = (gridH - menuH) / 2f
                                            }
                                        }
                                    }
                                    
                                    contextMenuOffset = DpOffset(x = finalX, y = finalY)
                                    showWidgetMenu = true
                                } else {
                                    Toast.makeText(context, "Layout is locked", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onResize = { newRow, newCol, newSpanX, newSpanY ->
                                // Constrain widgets to the visible maxRows
                                viewModel.updateWidgetBounds(
                                    widget.widgetId,
                                    newRow.coerceIn(0f, (maxRows - newSpanY).coerceAtLeast(0f)),
                                    newCol.coerceIn(0f, (4f - newSpanX).coerceAtLeast(0f)),
                                    newSpanX,
                                    newSpanY
                                )
                            }
                        )
                    }

                    HomeContextMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false },
                        offset = contextMenuOffset,
                        onOpenWidgets = {
                            val appWidgetId = viewModel.allocateWidgetId()
                            val intent = android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            widgetPickLauncher.launch(intent)
                        },
                        onOpenLauncherSettings = { showSettings = true }
                    )

                    WidgetContextMenu(
                        expanded = showWidgetMenu,
                        onDismissRequest = { showWidgetMenu = false },
                        onRemove = {
                            if (editingWidgetId != -1) {
                                viewModel.removeWidget(editingWidgetId)
                                editingWidgetId = -1
                            }
                            showWidgetMenu = false
                        },
                        onOpenApp = {
                            if (editingWidgetId != -1) {
                                val info = viewModel.appWidgetManager.getAppWidgetInfo(editingWidgetId)
                                info?.provider?.packageName?.let { pkg ->
                                    viewModel.launchApp(pkg)
                                }
                                editingWidgetId = -1
                            }
                            showWidgetMenu = false
                        },
                        offset = contextMenuOffset
                    )
                }
            }
        }

        if (shouldShowDefaultPrompt) {
            DefaultLauncherDialog(
                onSetDefault = { 
                    viewModel.openDefaultLauncherSettings()
                },
                onDismiss = { viewModel.dismissDefaultPrompt() }
            )
        }

        if (showSettings) {
            SettingsSheet(
                onDismiss = { showSettings = false },
                viewModel = settingsViewModel
            )
        }

        AnimatedVisibility(
            visible = showDrawer,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                DrawerScreen(
                    viewModel = drawerViewModel,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onAppClick = { packageName, options ->
                        drawerViewModel.resetState()
                        viewModel.launchApp(packageName, options)
                        showDrawer = false
                    }
                )
            }
        }
    }
}

@Composable
fun DefaultLauncherDialog(
    onSetDefault: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Set PxlLauncher as default?") },
        text = { 
            Text("PxlLauncher is not your default launcher. Set it as default for the best experience. You can always change it back in settings.") 
        },
        confirmButton = {
            TextButton(onClick = onSetDefault) {
                Text("Set as default")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No thanks")
            }
        }
    )
}
