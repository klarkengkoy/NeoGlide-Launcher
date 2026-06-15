package com.samidevstudio.neoglide.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samidevstudio.neoglide.data.repository.*
import com.samidevstudio.neoglide.ui.components.AppIcon
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val isPremium = preferences.isPremium
    val hapticFeedback = rememberHapticFeedback(preferences)
    val isNotifEnabled by viewModel.isNotificationServiceEnabled.collectAsStateWithLifecycle()
    val isAuthForHidden by viewModel.isUserAuthenticatedForHiddenApps.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Reset authentication when the settings sheet is closed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setUserAuthenticatedForHiddenApps(authenticated = false)
        }
    }

    var activeDialog by remember { mutableStateOf<String?>(null) }
    val pendingResetAction = remember { mutableStateOf<ResetAction?>(null) }

    val showHiddenAppsWithAuth = {
        if (!isPremium) {
            activeDialog = "premium"
        } else if (isAuthForHidden) {
            activeDialog = "hidden_apps"
        } else {
            BiometricHelper.showBiometricPrompt(
                activity = context as FragmentActivity,
                onSuccess = {
                    viewModel.setUserAuthenticatedForHiddenApps(true)
                    activeDialog = "hidden_apps"
                },
                onNoSecurityEnrolled = {
                    activeDialog = "security_warning"
                }
            )
        }
    }

    // DIALOG DISPATCHER
    when (activeDialog) {
        "security_warning" -> AlertDialog(
            onDismissRequest = { activeDialog = null },
            title = { Text("Unprotected Vault") },
            text = { Text("Your device has no lock set. Anyone can access this vault. Are you sure you want to continue without security?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.setUserAuthenticatedForHiddenApps(true)
                    activeDialog = "hidden_apps" 
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    try {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                    activeDialog = null
                }) { Text("Set up Lock") }
            }
        )
        "category" -> CategoryBarSettingsDialog(
            currentType = preferences.categoryBarType,
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setCategoryBarType(it) }
        )
        "anchor" -> AnchorSettingsDialog(
            verticalAnchor = preferences.verticalAnchor,
            horizontalAnchor = preferences.horizontalAnchor,
            onDismiss = { activeDialog = null },
            onSelectVertical = { viewModel.setVerticalAnchor(it) },
            onSelectHorizontal = { viewModel.setHorizontalAnchor(it) }
        )
        "sorting" -> SortingSettingsDialog(
            currentMode = preferences.sortingMode,
            isReverse = preferences.isSortReverse,
            isPremium = isPremium,
            onDismiss = { activeDialog = null },
            onSelectMode = { 
                if (!isPremium && it != SortingMode.ALPHABETICAL) {
                    activeDialog = "premium"
                } else {
                    viewModel.setSortingMode(it)
                }
            },
            onToggleReverse = { 
                if (!isPremium && it) {
                    activeDialog = "premium"
                } else {
                    viewModel.setIsSortReverse(it) 
                }
            }
        )
        "grid" -> SelectionDialog(
            title = "Home screen grid",
            description = "Customize the number of rows and columns on your home screen.",
            options = listOf(
                DialogOption("4 x 5 (Standard)", GridSize.GRID_4X5, preferences.gridSize == GridSize.GRID_4X5),
                DialogOption("5 x 5 (Dense)", GridSize.GRID_5X5, preferences.gridSize == GridSize.GRID_5X5),
                DialogOption("6 x 6 (Expert)", GridSize.GRID_6X6, preferences.gridSize == GridSize.GRID_6X6)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setGridSize(it as GridSize) }
        )
        "trouble" -> TroubleshootingDialog(
            onDismiss = { activeDialog = null },
            onAction = { pendingResetAction.value = it }
        )
        "search" -> SearchSettingsDialog(
            currentProvider = preferences.searchProvider,
            isPremium = isPremium,
            onDismiss = { activeDialog = null },
            onSelect = { provider ->
                if (!isPremium && (provider == SearchProvider.DUCKDUCKGO || provider == SearchProvider.BRAVE || provider == SearchProvider.ECOSIA)) {
                    activeDialog = "premium"
                } else {
                    viewModel.setSearchProvider(provider)
                }
            }
        )
        "notif" -> SelectionDialog(
            title = "Notification dots",
            options = listOf(
                DialogOption("App Icon", NotificationDotMode.APP_ICON, preferences.notificationDotMode == NotificationDotMode.APP_ICON),
                DialogOption("Category Bar", NotificationDotMode.CATEGORY_BAR, preferences.notificationDotMode == NotificationDotMode.CATEGORY_BAR),
                DialogOption("Both", NotificationDotMode.BOTH, preferences.notificationDotMode == NotificationDotMode.BOTH),
                DialogOption("None", NotificationDotMode.NONE, preferences.notificationDotMode == NotificationDotMode.NONE)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setNotificationDotMode(it as NotificationDotMode) }
        )
        "about" -> AboutDialog(onDismiss = { activeDialog = null })
        "notif_settings" -> NotificationSettingsDialog(
            isNotifEnabled = isNotifEnabled,
            dotMode = preferences.notificationDotMode,
            onDismiss = { activeDialog = null },
            onSelectDotMode = { viewModel.setNotificationDotMode(it) }
        )
        "label_settings" -> AppLabelSettingsDialog(
            labelMode = preferences.appLabelMode,
            onDismiss = { activeDialog = null },
            onSelectLabelMode = { viewModel.setAppLabelMode(it) }
        )
        "hidden_apps" -> HiddenAppsDialog(
            onDismiss = { activeDialog = null },
            viewModel = viewModel
        )
        "premium" -> PremiumFeaturesDialog(
            isPremium = isPremium,
            onDismiss = { activeDialog = null },
            onUpgrade = { viewModel.setIsPremium(true) }
        )
    }
    
    if (pendingResetAction.value != null) {
        AlertDialog(
            onDismissRequest = { pendingResetAction.value = null },
            title = { Text("Confirm Action") },
            text = { 
                val message = if (pendingResetAction.value == ResetAction.OPEN_APP_INFO) {
                    "You will be taken to the system settings for NeoGlide where you can manually clear cache or storage."
                } else {
                    "Are you sure you want to ${pendingResetAction.value?.label?.lowercase()}? This action cannot be undone."
                }
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (pendingResetAction.value) {
                            ResetAction.REFRESH_APP_ICONS -> viewModel.refreshAppIcons()
                            ResetAction.RESET_HOME -> viewModel.resetHomeScreen()
                            ResetAction.RESET_DRAWER -> viewModel.resetAppDrawer()
                            ResetAction.DELETE_HOME_FOLDERS -> viewModel.deleteHomeFolders()
                            ResetAction.DELETE_DRAWER_FOLDERS -> viewModel.deleteAppDrawerFolders()
                            ResetAction.OPEN_APP_INFO -> viewModel.openAppInfo()
                            null -> {}
                        }
                        pendingResetAction.value = null
                        activeDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (pendingResetAction.value == ResetAction.OPEN_APP_INFO) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error
                    )
                ) { Text(if (pendingResetAction.value == ResetAction.OPEN_APP_INFO) "Open Settings" else "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetAction.value = null }) { Text("Cancel") }
            }
        )
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.checkNotificationPermission()
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        QuickActionItem(Icons.Default.Add, "Shortcut") { }
                        QuickActionItem(Icons.Default.CreateNewFolder, "Folder") { }
                        QuickActionItem(Icons.Default.Category, "Category") { }
                    }
                }

                item {
                    SettingsGroup(title = "DEVELOPER TOOLS (TEMPORARY)") {
                        ToggleSettingsItem(
                            icon = Icons.Default.BugReport,
                            title = "Premium Toggle",
                            checked = isPremium,
                            onCheckedChange = { viewModel.setIsPremium(it) }
                        )
                        SettingsItem(
                            icon = Icons.Default.Home,
                            title = "Set Default Launcher",
                            onClick = { viewModel.openDefaultLauncherSettings() }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "DRAWER & NAVIGATION") {
                        SettingsItem(
                            icon = Icons.Default.ViewStream,
                            title = "Category bar",
                            onClick = { activeDialog = "category" },
                            trailing = { 
                                val label = when(preferences.categoryBarType) {
                                    CategoryBarType.LEFT -> "Left"
                                    CategoryBarType.RIGHT -> "Right"
                                    CategoryBarType.BOTTOM -> "Bottom"
                                    CategoryBarType.NONE -> "Categoryless"
                                }
                                ValueLabel(label) 
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Anchor,
                            title = "Anchor",
                            onClick = { activeDialog = "anchor" },
                            trailing = { ValueLabel("${preferences.verticalAnchor.name.formatLabel()} / ${preferences.horizontalAnchor.name.formatLabel()}") }
                        )
                        SettingsItem(
                            icon = Icons.Default.SortByAlpha,
                            title = "Sorting",
                            isPremium = !isPremium,
                            onClick = { activeDialog = "sorting" },
                            trailing = { ValueLabel(getSortingLabel(preferences.sortingMode)) }
                        )
                        SettingsItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Hidden apps",
                            isPremium = !isPremium,
                            onClick = { showHiddenAppsWithAuth() }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "INTERACTION & LAYOUT") {
                        SettingsItem(
                            icon = Icons.Default.Smartphone, 
                            title = "Home grid", 
                            onClick = { activeDialog = "grid" },
                            trailing = { ValueLabel(preferences.gridSize.name.split("_").last()) }
                        ) 
                        ToggleSettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Prevent changes",
                            checked = preferences.lockLayout,
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setLockLayout(it) }
                        )
                        ToggleSettingsItem(
                            icon = Icons.Default.Vibration,
                            title = "Haptic feedback",
                            checked = preferences.hapticsEnabled,
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setHapticsEnabled(it) }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "ADVANCED") {
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            onClick = { activeDialog = "notif_settings" }
                        )
                        SettingsItem(
                            icon = Icons.Default.Search, 
                            title = "Search provider", 
                            isPremium = !isPremium,
                            onClick = { activeDialog = "search" },
                            trailing = { ValueLabel(preferences.searchProvider.displayName) }
                        )
                        SettingsItem(
                            icon = Icons.Default.FontDownload,
                            title = "App labels",
                            onClick = { activeDialog = "label_settings" }
                        )
                        SettingsItem(
                            icon = Icons.Default.Wallpaper, 
                            title = "Wallpaper", 
                            onClick = { viewModel.openWallpaperSettings() }
                        )
                        SettingsItem(
                            icon = Icons.Default.Palette, 
                            title = "Appearance", 
                            onClick = { viewModel.openAppearanceSettings() }
                        )
                        SettingsItem(
                            icon = Icons.Default.Build, 
                            title = "Troubleshooting", 
                            onClick = { activeDialog = "trouble" }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "SUPPORT & INFO") {
                        SettingsItem(
                            icon = Icons.Default.WorkspacePremium, 
                            title = "Premium features", 
                            onClick = { activeDialog = "premium" }, 
                            trailing = { 
                                if (isPremium) {
                                    ValueLabel("Active")
                                } else {
                                    PremiumBadge()
                                }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "About NeoGlide Launcher",
                            onClick = { activeDialog = "about" },
                            trailing = { Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

private fun String.formatLabel(): String = this.replace("_", " ")
    .lowercase(Locale.getDefault())
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private fun getSortingLabel(mode: SortingMode): String = when(mode) {
    SortingMode.ALPHABETICAL -> "Alphabetical"
    SortingMode.INSTALL_TIME -> "Install Time"
    SortingMode.LAST_USED -> "Last Used"
    SortingMode.ICON_COLOR -> "Icon Color"
}

private fun getSortingDescription(mode: SortingMode, isReverse: Boolean): String = when(mode) {
    SortingMode.ALPHABETICAL -> if (isReverse) "Reverse Z to A organization." else "Standard A to Z organization."
    SortingMode.INSTALL_TIME -> if (isReverse) "Oldest applications first." else "Newest applications first."
    SortingMode.LAST_USED -> if (isReverse) "Least used apps first. Defaults to Z-A." else "Most used apps first. Defaults to A-Z."
    SortingMode.ICON_COLOR -> if (isReverse) "White icons first, then black, grayscale, and reverse spectrum." else "Vibrant colors first, then grayscale, black, and white icons."
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsDialog(
    isNotifEnabled: Boolean,
    dotMode: NotificationDotMode,
    onDismiss: () -> Unit,
    onSelectDotMode: (NotificationDotMode) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Notifications")
            }
        },
        text = {
            Column {
                Text(
                    "Stay organized with notification badges. Enable access to see counts on your icons and in the app drawer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Permission Item
                Surface(
                    onClick = {
                        try {
                            val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Cannot open settings", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = if (!isNotifEnabled) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)) else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isNotifEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isNotifEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Notification Badges",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotifEnabled) "Active • Counting notifications" else "Tap to enable numeric badges",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNotifEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Display badges on:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // CHIP GROUP FOR MODERN SELECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val showOnIcons = dotMode == NotificationDotMode.APP_ICON || dotMode == NotificationDotMode.BOTH
                    val showOnRail = dotMode == NotificationDotMode.CATEGORY_BAR || dotMode == NotificationDotMode.BOTH

                    FilterChip(
                        selected = showOnIcons,
                        onClick = {
                            val newMode = when {
                                !showOnIcons && showOnRail -> NotificationDotMode.BOTH
                                !showOnIcons && !showOnRail -> NotificationDotMode.APP_ICON
                                showOnIcons && showOnRail -> NotificationDotMode.CATEGORY_BAR
                                else -> NotificationDotMode.NONE
                            }
                            onSelectDotMode(newMode)
                        },
                        label = { Text("App Icons") },
                        leadingIcon = if (showOnIcons) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilterChip(
                        selected = showOnRail,
                        onClick = {
                            val newMode = when {
                                !showOnRail && showOnIcons -> NotificationDotMode.BOTH
                                !showOnRail && !showOnIcons -> NotificationDotMode.CATEGORY_BAR
                                showOnRail && showOnIcons -> NotificationDotMode.APP_ICON
                                else -> NotificationDotMode.NONE
                            }
                            onSelectDotMode(newMode)
                        },
                        label = { Text("Category Rail") },
                        leadingIcon = if (showOnRail) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Choose where you want to see notification counts. Changes apply instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLabelSettingsDialog(
    labelMode: AppLabelMode,
    onDismiss: () -> Unit,
    onSelectLabelMode: (AppLabelMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FontDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("App Names")
            }
        },
        text = {
            Column {
                Text(
                    "Control the visibility of application labels across different areas of the launcher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Display names on:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // CHIP GROUP FOR MODERN SELECTION
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val showOnHome = labelMode == AppLabelMode.HOME_ONLY || labelMode == AppLabelMode.BOTH
                            val showOnDrawer = labelMode == AppLabelMode.DRAWER_ONLY || labelMode == AppLabelMode.BOTH

                            FilterChip(
                                selected = showOnHome,
                                onClick = {
                                    val newMode = when {
                                        !showOnHome && showOnDrawer -> AppLabelMode.BOTH
                                        !showOnHome && !showOnDrawer -> AppLabelMode.HOME_ONLY
                                        showOnHome && showOnDrawer -> AppLabelMode.DRAWER_ONLY
                                        else -> AppLabelMode.NONE
                                    }
                                    onSelectLabelMode(newMode)
                                },
                                label = { Text("Home Screen") },
                                leadingIcon = if (showOnHome) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            FilterChip(
                                selected = showOnDrawer,
                                onClick = {
                                    val newMode = when {
                                        !showOnDrawer && showOnHome -> AppLabelMode.BOTH
                                        !showOnDrawer && !showOnHome -> AppLabelMode.DRAWER_ONLY
                                        showOnDrawer && showOnHome -> AppLabelMode.HOME_ONLY
                                        else -> AppLabelMode.NONE
                                    }
                                    onSelectLabelMode(newMode)
                                },
                                label = { Text("App Drawer") },
                                leadingIcon = if (showOnDrawer) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Toggling these will instantly hide or show labels on your icons. You can mix and match for a cleaner workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenAppsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(com.samidevstudio.neoglide.ui.components.PickerSortMode.ALPHABETICAL) }

    val filteredApps = remember(allApps, searchQuery, sortMode, preferences.hiddenPackages) {
        val baseList = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        
        when (sortMode) {
            com.samidevstudio.neoglide.ui.components.PickerSortMode.ALPHABETICAL -> {
                baseList.sortedBy { it.label }
            }
            com.samidevstudio.neoglide.ui.components.PickerSortMode.RECENT -> {
                baseList.sortedByDescending { it.lastUsedTime }
            }
            else -> baseList.sortedBy { it.label }
        }
    }

    val (hiddenInFiltered, unhiddenInFiltered) = remember(filteredApps, preferences.hiddenPackages) {
        filteredApps.partition { it.packageName in preferences.hiddenPackages }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Manage Hidden Apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage apps that are hidden from the main drawer view.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps to hide...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        com.samidevstudio.neoglide.ui.components.PickerSortMode.ALPHABETICAL,
                        com.samidevstudio.neoglide.ui.components.PickerSortMode.RECENT
                    ).forEach { mode ->
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
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 450.dp)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (hiddenInFiltered.isNotEmpty()) {
                        item(key = "header_hidden") {
                            Text(
                                text = "Hidden Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(
                            items = hiddenInFiltered,
                            key = { "hidden_${it.packageName}" }
                        ) { app ->
                            HiddenAppPickerItem(
                                app = app,
                                isHidden = true,
                                onToggle = { viewModel.unhideApp(app.packageName) }
                            )
                        }
                        
                        if (unhiddenInFiltered.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    if (unhiddenInFiltered.isNotEmpty()) {
                        item(key = "header_available") {
                            Text(
                                text = "Available Apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp, start = 8.dp)
                            )
                        }
                        items(
                            items = unhiddenInFiltered,
                            key = { "available_${it.packageName}" }
                        ) { app ->
                            HiddenAppPickerItem(
                                app = app,
                                isHidden = false,
                                onToggle = { viewModel.hideApp(app.packageName) }
                            )
                        }
                    }

                    if (hiddenInFiltered.isEmpty() && unhiddenInFiltered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No apps found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
private fun HiddenAppPickerItem(
    app: com.samidevstudio.neoglide.domain.model.AppModel,
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!isHidden) },
        shape = RoundedCornerShape(12.dp),
        color = if (isHidden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
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
                        if (isHidden) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
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
                fontWeight = if (isHidden) FontWeight.Bold else FontWeight.Normal,
                color = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Checkbox(
                checked = isHidden,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("About NeoGlide Launcher")
            }
        },
        text = {
            Column {
                Text(
                    "NeoGlide Launcher is a minimalist, clean, and lightweight Android launcher optimized for responsiveness and intelligent organization.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Version: 1.0.0-neo", fontWeight = FontWeight.Bold)
                Text("Developer: SamiDev Studio")
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "This project is built with Jetpack Compose and modern Android architecture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                // TODO: Add proper Legal & Compliance links and info (Privacy Policy, Licenses, etc.)
                TextButton(
                    onClick = { /* TODO: Open Privacy Policy URL */ },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Privacy Policy", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PremiumFeaturesDialog(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("NeoGlide Premium")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = if (isPremium) "Thank you for supporting NeoGlide! You have unlocked all premium features." 
                           else "Unlock the full potential of NeoGlide Launcher with a one-time premium purchase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PremiumFeatureItem(
                    title = "Privacy Vault",
                    description = "Hide sensitive apps and protect them with biometric security (Fingerprint/PIN).",
                    icon = Icons.Default.VisibilityOff
                )
                PremiumFeatureItem(
                    title = "Advanced Sorting",
                    description = "Sort your apps by Installation Time, Last Used, Icon Color, or Reverse Alphabetical.",
                    icon = Icons.Default.SortByAlpha
                )
                PremiumFeatureItem(
                    title = "Custom Search",
                    description = "Use DuckDuckGo or other privacy-focused search providers.",
                    icon = Icons.Default.Search
                )
                PremiumFeatureItem(
                    title = "Backup & Sync (Planned)",
                    description = "Save your layout to Google Drive using Android's Handoff API.",
                    icon = Icons.Default.CloudSync
                )
                PremiumFeatureItem(
                    title = "Infinite Customization (V2)",
                    description = "Custom grid sizes, widget stacks, monochrome icons, and custom icon shapes.",
                    icon = Icons.Default.Palette
                )
            }
        },
        confirmButton = {
            if (!isPremium) {
                Button(
                    onClick = { 
                        onUpgrade()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Unlock Everything")
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (!isPremium) {
                TextButton(onClick = onDismiss) { Text("Maybe later") }
            }
        }
    )
}

@Composable
private fun PremiumFeatureItem(
    title: String,
    description: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                text = description, 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortingSettingsDialog(
    currentMode: SortingMode,
    isReverse: Boolean,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelectMode: (SortingMode) -> Unit,
    onToggleReverse: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SortByAlpha, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sorting Settings")
            }
        },
        text = {
            Column {
                Text(
                    "Change the order in which apps appear in the drawer. Settings apply instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // CHIP GROUP FOR MODERN SELECTION
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortingMode.entries.forEach { mode ->
                        val label = getSortingLabel(mode)
                        val isPremiumMode = mode != SortingMode.ALPHABETICAL
                        FilterChip(
                            selected = currentMode == mode,
                            onClick = { onSelectMode(mode) },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label)
                                    if (isPremiumMode && !isPremium) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        PremiumBadge()
                                    }
                                }
                            },
                            leadingIcon = if (currentMode == mode) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleReverse(!isReverse) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SwapVert, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Reverse Order",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (!isPremium) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PremiumBadge()
                                }
                            }
                        }
                        Switch(
                            checked = isReverse,
                            onCheckedChange = { onToggleReverse(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = getSortingDescription(currentMode, isReverse),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorSettingsDialog(
    verticalAnchor: VerticalAnchor,
    horizontalAnchor: HorizontalAnchor,
    onDismiss: () -> Unit,
    onSelectVertical: (VerticalAnchor) -> Unit,
    onSelectHorizontal: (HorizontalAnchor) -> Unit
) {
    var selectedVertical by remember { mutableStateOf(verticalAnchor) }
    var selectedHorizontal by remember { mutableStateOf(horizontalAnchor) }

    val verticalDescription = when (selectedVertical) {
        VerticalAnchor.TOP -> "Standard familiar look when paired with Left Anchor. Apps align to the top. Folders are placed at the top for consistent organization."
        VerticalAnchor.BOTTOM -> "Ergonomic layout. Apps align to the bottom for easier one-handed reach. Folders are placed at the bottom to keep your most frequent actions within the natural range of your thumb."
    }

    val horizontalDescription = when (selectedHorizontal) {
        HorizontalAnchor.LEFT -> "Left-hand ergonomics. Partial rows and folders gravitate to the left edge."
        HorizontalAnchor.RIGHT -> "Right-hand ergonomics. Partial rows and folders gravitate to the right edge, making them more accessible for one-handed use with your right thumb."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Anchor, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Anchor Settings")
            }
        },
        text = {
            Column {
                Text(
                    "Control how your app grid fills the space. Anchoring shifts the stack and alignment for better ergonomics.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Vertical Anchor:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VerticalAnchor.entries.reversed().forEach { anchor ->
                        FilterChip(
                            selected = selectedVertical == anchor,
                            onClick = { selectedVertical = anchor },
                            label = { Text(anchor.name.formatLabel()) },
                            leadingIcon = if (selectedVertical == anchor) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = verticalDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Horizontal Anchor:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalAnchor.entries.forEach { anchor ->
                        FilterChip(
                            selected = selectedHorizontal == anchor,
                            onClick = { selectedHorizontal = anchor },
                            label = { Text(anchor.name.formatLabel()) },
                            leadingIcon = if (selectedHorizontal == anchor) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = horizontalDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onSelectVertical(selectedVertical)
                    onSelectHorizontal(selectedHorizontal)
                    onDismiss()
                }
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryBarSettingsDialog(
    currentType: CategoryBarType,
    onDismiss: () -> Unit,
    onSelect: (CategoryBarType) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }

    val description = when (selectedType) {
        CategoryBarType.LEFT -> "Optimized for left-handed ergonomics. Scrubber rail stays on the left edge."
        CategoryBarType.RIGHT -> "Optimized for right-handed ergonomics. Scrubber rail stays on the right edge."
        CategoryBarType.BOTTOM -> "Balanced bottom layout. Perfect for quick thumb gliding across categories."
        CategoryBarType.NONE -> "All apps will appear in a single unified list without category divisions. Rapid gliding is disabled."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ViewStream, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Category Bar")
            }
        },
        text = {
            Column {
                Text(
                    "Choose the position or visibility of the category selection rail.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Position:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryBarType.entries.forEach { type ->
                        val label = when(type) {
                            CategoryBarType.LEFT -> "Left"
                            CategoryBarType.RIGHT -> "Right"
                            CategoryBarType.BOTTOM -> "Bottom"
                            CategoryBarType.NONE -> "Categoryless"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) },
                            leadingIcon = if (selectedType == type) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedType == CategoryBarType.NONE) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    onSelect(selectedType)
                    onDismiss()
                }
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchSettingsDialog(
    currentProvider: SearchProvider,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSelect: (SearchProvider) -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }

    val description = when (selectedProvider) {
        SearchProvider.GOOGLE -> "Unmatched speed and personalized results integrated into the Google ecosystem."
        SearchProvider.DUCKDUCKGO -> "Search without being followed by ads. Total privacy, no profiles, and no search bubbles."
        SearchProvider.BRAVE -> "True independence. Uses its own index to avoid Big Tech bias and algorithmic filtering."
        SearchProvider.ECOSIA -> "Plant trees while you search. 100% of profits go to climate action and environmental impact."
        SearchProvider.LOCAL_ONLY -> "Fastest performance. Web search is disabled to keep your focus entirely on local apps."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Search Provider")
            }
        },
        text = {
            Column {
                Text(
                    "Choose which engine to use for web search suggestions and routing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Engine:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchProvider.entries.forEach { provider ->
                        val isPremiumRequired = provider == SearchProvider.DUCKDUCKGO || 
                                               provider == SearchProvider.BRAVE || 
                                               provider == SearchProvider.ECOSIA
                        
                        FilterChip(
                            selected = selectedProvider == provider,
                            onClick = { selectedProvider = provider },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(provider.displayName)
                                    if (isPremiumRequired && !isPremium) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        PremiumBadge()
                                    }
                                }
                            },
                            leadingIcon = if (selectedProvider == provider) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    ) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedProvider == SearchProvider.LOCAL_ONLY)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(selectedProvider)
                    if (isPremium || !(selectedProvider == SearchProvider.DUCKDUCKGO || selectedProvider == SearchProvider.BRAVE || selectedProvider == SearchProvider.ECOSIA)) {
                        onDismiss()
                    }
                }
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class DialogOption(
    val label: String, 
    val value: Any, 
    val isSelected: Boolean,
    val isPremiumRequired: Boolean = false
)

@Composable
fun SelectionDialog(
    title: String,
    description: String? = null,
    options: List<DialogOption>,
    isPremium: Boolean = true,
    onDismiss: () -> Unit,
    autoDismiss: Boolean = true,
    onSelect: (Any) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(
                                selected = option.isSelected,
                                onClick = { 
                                    onSelect(option.value)
                                    if (autoDismiss) onDismiss()
                                }
                            )
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option.isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = option.label, style = MaterialTheme.typography.bodyLarge)
                            if (option.isPremiumRequired && !isPremium) {
                                Spacer(modifier = Modifier.width(8.dp))
                                PremiumBadge()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun TroubleshootingDialog(
    onDismiss: () -> Unit,
    onAction: (ResetAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Troubleshooting") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                SettingsGroup(title = "HOME SCREEN") {
                    SettingsItem(
                        icon = ResetAction.RESET_HOME.icon,
                        title = ResetAction.RESET_HOME.label,
                        description = ResetAction.RESET_HOME.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.RESET_HOME) }
                    )
                    SettingsItem(
                        icon = ResetAction.DELETE_HOME_FOLDERS.icon,
                        title = ResetAction.DELETE_HOME_FOLDERS.label,
                        description = ResetAction.DELETE_HOME_FOLDERS.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.DELETE_HOME_FOLDERS) }
                    )
                }

                SettingsGroup(title = "APP DRAWER") {
                    SettingsItem(
                        icon = ResetAction.RESET_DRAWER.icon,
                        title = ResetAction.RESET_DRAWER.label,
                        description = ResetAction.RESET_DRAWER.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.RESET_DRAWER) }
                    )
                    SettingsItem(
                        icon = ResetAction.DELETE_DRAWER_FOLDERS.icon,
                        title = ResetAction.DELETE_DRAWER_FOLDERS.label,
                        description = ResetAction.DELETE_DRAWER_FOLDERS.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.DELETE_DRAWER_FOLDERS) }
                    )
                }

                SettingsGroup(title = "CACHE & PERFORMANCE") {
                    SettingsItem(
                        icon = ResetAction.REFRESH_APP_ICONS.icon,
                        title = ResetAction.REFRESH_APP_ICONS.label,
                        description = ResetAction.REFRESH_APP_ICONS.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.REFRESH_APP_ICONS) }
                    )
                }

                SettingsGroup(title = "SYSTEM & STORAGE") {
                    SettingsItem(
                        icon = ResetAction.OPEN_APP_INFO.icon,
                        title = ResetAction.OPEN_APP_INFO.label,
                        description = ResetAction.OPEN_APP_INFO.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.OPEN_APP_INFO) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

enum class ResetAction(val label: String, val description: String, val icon: ImageVector) {
    REFRESH_APP_ICONS("Refresh App Icons", "Re-extract colors and icons for all apps. Useful if icons appear outdated or incorrect.", Icons.Default.Refresh),
    RESET_HOME("Reset Home Screen", "Restore the default home screen layout and dock.", Icons.Default.LayersClear),
    RESET_DRAWER("Reset App Drawer", "Clear drawer folders and reset categories.", Icons.Default.FolderDelete),
    DELETE_HOME_FOLDERS("Delete Home Folders", "Dissolve folders on home screen.", Icons.Default.DeleteSweep),
    DELETE_DRAWER_FOLDERS("Delete App Drawer Folders", "Dissolve folders in app drawer.", Icons.Default.FolderOff),
    OPEN_APP_INFO("App Info", "Open system settings to clear cache or storage.", Icons.Default.Settings)
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionHeader(title)
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun PremiumBadge() {
    Icon(
        Icons.Default.WorkspacePremium, 
        null, 
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
private fun ValueLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ToggleSettingsItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    isPremium: Boolean = false,
    description: String? = null,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                onHapticFeedback(HapticEngine.HapticType.TOGGLE)
                onCheckedChange(!checked) 
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (isPremium) {
                    Spacer(modifier = Modifier.width(6.dp))
                    PremiumBadge()
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked, 
            onCheckedChange = {
                onHapticFeedback(HapticEngine.HapticType.TOGGLE)
                onCheckedChange(it)
            },
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    title: String, 
    isPremium: Boolean = false,
    description: String? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null, 
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (isPremium) {
                    Spacer(modifier = Modifier.width(6.dp))
                    PremiumBadge()
                }
            }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) trailing()
        else if (onClick != null && showChevron) {
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}
