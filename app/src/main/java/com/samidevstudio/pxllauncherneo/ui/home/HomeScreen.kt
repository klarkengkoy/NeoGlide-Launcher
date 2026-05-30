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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel as hiltViewModelV2
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
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
    onNavigateToDetail: (String) -> Unit,
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
        androidx.compose.animation.AnimatedVisibility(
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
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    editingWidgetId = -1
                                    showWidgetMenu = false
                                    focusManager.clearFocus()
                                },
                                onLongPress = { offset ->
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
                    val unitWidth = maxWidth / 4
                    val unitHeight = 96.dp

                    // GRID OVERLAY
                    if (editingWidgetId != -1) {
                        val isDark = isSystemInDarkTheme()
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val mainGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f)
                            val subGridColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f)
                            
                            val totalRows = (size.height / unitHeight.toPx()).toInt()
                            for (i in 0..totalRows) {
                                val y = i * unitHeight.toPx()
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 2f)
                                val midY = y + (unitHeight.toPx() / 2f)
                                if (midY <= size.height) {
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(0f, midY), androidx.compose.ui.geometry.Offset(size.width, midY), strokeWidth = 1f)
                                }
                            }
                            
                            for (i in 0..4) {
                                val x = i * unitWidth.toPx()
                                drawLine(mainGridColor, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 2f)
                                if (i < 4) {
                                    val midX = x + (unitWidth.toPx() / 2f)
                                    drawLine(subGridColor, androidx.compose.ui.geometry.Offset(midX, 0f), androidx.compose.ui.geometry.Offset(midX, size.height), strokeWidth = 1f)
                                }
                            }
                        }
                    }

                    widgets.forEach { widget ->
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
                            isEditing = editingWidgetId == widget.widgetId,
                            onLongClick = { offset ->
                                if (!preferences.lockLayout) {
                                    editingWidgetId = widget.widgetId
                                    val widgetTop = unitHeight * widget.row
                                    val widgetBottom = unitHeight * (widget.row + widget.spanY)
                                    val menuHeightEstimate = 100.dp
                                    val menuY = if (widgetTop > menuHeightEstimate) {
                                        widgetTop - menuHeightEstimate
                                    } else {
                                        widgetBottom + 8.dp
                                    }
                                    contextMenuOffset = DpOffset(
                                        x = (unitWidth * widget.column) + offset.x,
                                        y = menuY
                                    )
                                    showWidgetMenu = true
                                } else {
                                    Toast.makeText(context, "Layout is locked", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onResize = { newRow, newCol, newSpanX, newSpanY ->
                                viewModel.updateWidgetBounds(
                                    widget.widgetId,
                                    newRow,
                                    newCol,
                                    newSpanX,
                                    newSpanY
                                )
                            }
                        )
                    }
                }

                // Dock Widget
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.wrapContentSize()
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
                                                 app.packageName in activeNotifications,
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                DrawerScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    onAppClick = { packageName, options ->
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
