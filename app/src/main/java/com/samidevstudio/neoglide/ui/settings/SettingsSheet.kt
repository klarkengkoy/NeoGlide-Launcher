package com.samidevstudio.neoglide.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.samidevstudio.neoglide.data.billing.BillingManager
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.data.repository.BadgeStyle
import com.samidevstudio.neoglide.data.repository.CategoryBarType
import com.samidevstudio.neoglide.data.repository.GridSize
import com.samidevstudio.neoglide.data.repository.HorizontalAnchor
import com.samidevstudio.neoglide.data.repository.SearchProvider
import com.samidevstudio.neoglide.data.repository.SortingMode
import com.samidevstudio.neoglide.data.repository.VerticalAnchor
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.ui.components.AppIcon
import com.samidevstudio.neoglide.ui.components.MultiAppPickerDialog
import com.samidevstudio.neoglide.ui.components.category.AddCategoryDialogRefined
import com.samidevstudio.neoglide.ui.components.category.ManageCategoriesDialog
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
    val isPremium = preferences.isPremium
    val hapticFeedback = rememberHapticFeedback(preferences)
    val isNotifEnabled by viewModel.isNotificationServiceEnabled.collectAsStateWithLifecycle()
    val isAuthForHidden by viewModel.isUserAuthenticatedForHiddenApps.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val lifecycleOwner = LocalLifecycleOwner.current
    val restoreStatus by viewModel.restoreStatus.collectAsStateWithLifecycle()

    var activeDialog by remember { mutableStateOf<String?>(null) }
    var restoreError by remember { mutableStateOf<String?>(null) }
    val pendingResetAction = remember { mutableStateOf<ResetAction?>(null) }

    LaunchedEffect(restoreStatus) {
        when (restoreStatus) {
            is BillingManager.RestoreStatus.Success -> {
                android.widget.Toast.makeText(context, "Premium features restored successfully!", android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetRestoreStatus()
                activeDialog = null
            }
            is BillingManager.RestoreStatus.NoPurchase -> {
                restoreError = "We couldn't find a premium purchase for the account currently signed into your Google Play Store. If you have multiple accounts, please ensure the correct one is active in the Play Store app and try again."
                viewModel.resetRestoreStatus()
            }
            is BillingManager.RestoreStatus.NoNetwork -> {
                restoreError = "An internet connection is required to verify your purchases with the Google Play Store. Please check your connection and try again."
                viewModel.resetRestoreStatus()
            }
            is BillingManager.RestoreStatus.Error -> {
                restoreError = (restoreStatus as BillingManager.RestoreStatus.Error).message
                viewModel.resetRestoreStatus()
            }
            else -> {}
        }
    }

    // Observe lifecycle to refresh permissions when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkNotificationPermission()
                viewModel.checkDefaultLauncher()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkDefaultLauncher()
    }

    // Reset authentication when the settings sheet is closed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setUserAuthenticatedForHiddenApps(authenticated = false)
        }
    }

    val showHiddenAppsWithAuth = {
        if (!isPremium) {
            activeDialog = "premium"
        } else if (isAuthForHidden) {
            activeDialog = "hidden_apps"
        } else {
            BiometricHelper.showBiometricPrompt(
                activity = context as FragmentActivity,
                onSuccess = {
                    viewModel.setUserAuthenticatedForHiddenApps(authenticated = true)
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
                TextButton(
                    onClick = {
                        viewModel.setUserAuthenticatedForHiddenApps(authenticated = true)
                        activeDialog = "hidden_apps"
                    }
                ) { Text("Continue") }
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
        "category" -> {
            val drawerViewModel: com.samidevstudio.neoglide.ui.drawer.DrawerViewModel = hiltViewModel()
            val coroutineScope = rememberCoroutineScope()
            CategoryBarSettingsDialog(
                currentType = preferences.categoryBarType,
                onDismiss = { activeDialog = null },
                onSelect = { newType ->
                    coroutineScope.launch {
                        if (drawerViewModel.canSwitchToCategoryBarType(context, newType)) {
                            viewModel.setCategoryBarType(newType)
                        } else {
                            android.widget.Toast.makeText(context, "Cannot switch: Too many categories for this layout. Please remove some categories first.", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
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
                if ((!isPremium) && (it != SortingMode.ALPHABETICAL)) {
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
        "grid" -> GridSettingsDialog(
            currentSize = preferences.gridSize,
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setGridSize(it) }
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
            title = "Notification badges",
            options = listOf(
                DialogOption("Home Screen", BadgeStyle.COUNT, preferences.homeBadgeStyle == BadgeStyle.COUNT),
                DialogOption("App Drawer", BadgeStyle.COUNT, preferences.drawerBadgeStyle == BadgeStyle.COUNT),
                DialogOption("Category Rail", BadgeStyle.COUNT, preferences.railBadgeStyle == BadgeStyle.COUNT)
            ),
            onDismiss = { activeDialog = null },
            onSelect = { /* This was a simple dialog, now replaced by notif_settings */ }
        )
        "about" -> AboutDialog(onDismiss = { activeDialog = null })
        "notif_settings" -> NotificationSettingsDialog(
            isNotifEnabled = isNotifEnabled,
            homeStyle = preferences.homeBadgeStyle,
            drawerStyle = preferences.drawerBadgeStyle,
            railStyle = preferences.railBadgeStyle,
            onDismiss = { activeDialog = null },
            onSelectHomeStyle = { viewModel.setHomeBadgeStyle(it) },
            onSelectDrawerStyle = { viewModel.setDrawerBadgeStyle(it) },
            onSelectRailStyle = { viewModel.setRailBadgeStyle(it) }
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
        "add_category" -> {
            val drawerViewModel: com.samidevstudio.neoglide.ui.drawer.DrawerViewModel = hiltViewModel()
            AddCategoryDialogRefined(
                onDismiss = { activeDialog = null },
                drawerViewModel = drawerViewModel,
                onAddBuiltIn = { drawerViewModel.addBuiltInCategory(it) },
                onAddCustom = { name, icon -> 
                    drawerViewModel.addCustomCategory(name, icon)
                },
                onSwitchToVertical = { viewModel.setCategoryBarType(CategoryBarType.LEFT) }
            )
        }
        "manage_categories" -> {
            val drawerViewModel: com.samidevstudio.neoglide.ui.drawer.DrawerViewModel = hiltViewModel()
            ManageCategoriesDialog(
                onDismiss = { activeDialog = null },
                drawerViewModel = drawerViewModel,
                onRemove = { category -> drawerViewModel.removeCustomCategory(category) },
                onReorder = { order -> viewModel.updateCategoryOrder(order) },
                onUpdate = { old, new, icon -> viewModel.updateCategory(old, new, icon) }
            )
        }
        "add_folder" -> {
            val drawerViewModel: com.samidevstudio.neoglide.ui.drawer.DrawerViewModel = hiltViewModel()
            val allApps by viewModel.allApps.collectAsStateWithLifecycle()
            val preferences by viewModel.userPreferences.collectAsStateWithLifecycle()
            val selectedApps = remember { mutableStateListOf<String>() }
            
            val availableApps = remember(allApps, preferences.hiddenPackages) {
                allApps.filter { it.packageName !in preferences.hiddenPackages }
            }

            MultiAppPickerDialog(
                title = "Create New Folder",
                allApps = availableApps,
                memberPackageNames = selectedApps.toSet(),
                recentlyUsedApps = emptyList(),
                onToggleMember = { app, checked ->
                    if (checked) {
                        if (app.packageName !in selectedApps) selectedApps.add(app.packageName)
                    } else {
                        selectedApps.remove(app.packageName)
                    }
                },
                onDismissRequest = { 
                    if (selectedApps.size >= 2) {
                        val category = if (preferences.categoryBarType == CategoryBarType.NONE) null 
                                      else (drawerViewModel.selectedCategory.value ?: AppCategory.OTHER)
                        drawerViewModel.createFolderFromList(selectedApps.toList(), "Folder", category)
                        onDismiss()
                    }
                    activeDialog = null
                }
            )
        }
        "premium" -> {
            val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()
            val activity = LocalActivity.current
            
            PremiumFeaturesDialog(
                isPremium = isPremium,
                productDetailsMap = productDetails,
                restoreStatus = restoreStatus,
                onDismiss = { activeDialog = null },
                onRestore = { viewModel.restorePurchases() },
                onUpgrade = { productId ->
                    activity?.let { viewModel.buyPremium(it, productId) }
                }
            )
        }
    }
    
    if (restoreError != null) {
        AlertDialog(
            onDismissRequest = { restoreError = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Restoration Failed")
                }
            },
            text = { Text(restoreError!!) },
            confirmButton = {
                TextButton(onClick = { restoreError = null }) { Text("OK", fontWeight = FontWeight.Bold) }
            }
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
                            ResetAction.DISSOLVE_DRAWER_FOLDERS -> viewModel.dissolveAppDrawerFolders()
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

    // Removed redundant LaunchedEffect as LifecycleObserver handles ON_RESUME
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        val isDefaultLauncher by viewModel.isDefaultLauncher.collectAsStateWithLifecycle()

        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (!isDefaultLauncher) {
                    item {
                        Surface(
                            onClick = { viewModel.openDefaultLauncherSettings() },
                            modifier = Modifier.padding(16.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Set Default Launcher",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Experience NeoGlide as it's meant to be.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    SettingsGroup(title = "QUICK ACTIONS") {
                        SettingsItem(
                            icon = Icons.Default.CreateNewFolder,
                            title = "Add Folder",
                            onClick = {
                                if (!preferences.lockLayout) {
                                    activeDialog = "add_folder"
                                } else {
                                    android.widget.Toast.makeText(context, "Locked from launcher settings", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Category,
                            title = "Add Category",
                            onClick = {
                                if (!preferences.lockLayout) {
                                    activeDialog = "add_category"
                                } else {
                                    android.widget.Toast.makeText(context, "Locked from launcher settings", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        ToggleSettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Lock Layout",
                            checked = preferences.lockLayout,
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setLockLayout(it) }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "NAVIGATION") {
                        SettingsItem(
                            icon = Icons.Default.ViewStream,
                            title = "Category Bar",
                            onClick = { activeDialog = "category" },
                            trailing = {
                                val label = when(preferences.categoryBarType) {
                                    CategoryBarType.LEFT -> "Left"
                                    CategoryBarType.RIGHT -> "Right"
                                    CategoryBarType.BOTTOM -> "Bottom"
                                    CategoryBarType.NONE -> "None"
                                }
                                ValueLabel(label)
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Anchor,
                            title = "Alignment & Anchor",
                            onClick = { activeDialog = "anchor" },
                            trailing = { ValueLabel("${preferences.verticalAnchor.name.formatLabel()} / ${preferences.horizontalAnchor.name.formatLabel()}") }
                        )
                        SettingsItem(
                            icon = Icons.Default.SortByAlpha,
                            title = "Sorting Mode",
                            onClick = { activeDialog = "sorting" },
                            trailing = { ValueLabel(getSortingLabel(preferences.sortingMode)) }
                        )
                        SettingsItem(
                            icon = Icons.Default.FontDownload,
                            title = "App Labels",
                            onClick = { activeDialog = "label_settings" }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "HOME & APPEARANCE") {
                        SettingsItem(
                            icon = Icons.Default.GridView,
                            title = "Home Screen Layout",
                            onClick = { activeDialog = "grid" },
                            trailing = { ValueLabel(preferences.gridSize.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                        ToggleSettingsItem(
                            icon = Icons.Default.Vibration,
                            title = "Haptic Feedback",
                            checked = preferences.hapticsEnabled,
                            onHapticFeedback = hapticFeedback,
                            onCheckedChange = { viewModel.setHapticsEnabled(it) }
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
                    }
                }

                item {
                    SettingsGroup(title = "PRIVACY & SECURITY") {
                        SettingsItem(
                            icon = Icons.Default.VisibilityOff,
                            title = "Hidden Apps",
                            isPremium = !isPremium,
                            onClick = { showHiddenAppsWithAuth() }
                        )
                        SettingsItem(
                            icon = Icons.Default.Notifications,
                            title = "Notification Badges",
                            onClick = { activeDialog = "notif_settings" }
                        )
                    }
                }

                item {
                    SettingsGroup(title = "SEARCH") {
                        SettingsItem(
                            icon = Icons.Default.Search,
                            title = "Search Provider",
                            onClick = { activeDialog = "search" },
                            trailing = { ValueLabel(preferences.searchProvider.displayName) }
                        )
                    }
                }

                item {
                    if (!isPremium) {
                        SettingsGroup(title = "UPGRADE") {
                            SettingsItem(
                                icon = Icons.Default.WorkspacePremium,
                                title = "NeoGlide Premium",
                                onClick = { activeDialog = "premium" }
                            )
                        }
                    }
                }

                item {
                    SettingsGroup(title = "SYSTEM") {
                        SettingsItem(
                            icon = Icons.Default.Build,
                            title = "Troubleshooting",
                            onClick = { activeDialog = "trouble" }
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "About NeoGlide",
                            onClick = { activeDialog = "about" },
                            trailing = {
                                Text(
                                    "v${viewModel.getAppVersion()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
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
    homeStyle: BadgeStyle,
    drawerStyle: BadgeStyle,
    railStyle: BadgeStyle,
    onDismiss: () -> Unit,
    onSelectHomeStyle: (BadgeStyle) -> Unit,
    onSelectDrawerStyle: (BadgeStyle) -> Unit,
    onSelectRailStyle: (BadgeStyle) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Notification Badges")
            }
        },
        text = {
            Column {
                Text(
                    "Stay organized with notification badges. Choose your preferred style for different areas of the launcher.",
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
                                text = "Enable Badge Access",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isNotifEnabled) "Active • Reading notifications" else "Tap to grant permission",
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

                BadgeStyleSection("Home Screen", homeStyle, onSelectHomeStyle)
                Spacer(modifier = Modifier.height(16.dp))
                BadgeStyleSection("App Drawer", drawerStyle, onSelectDrawerStyle)
                Spacer(modifier = Modifier.height(16.dp))
                BadgeStyleSection("Category Rail", railStyle, onSelectRailStyle)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "None: No indicators.\nDot: Small dot for notifications.\nCount: Numeric count for each app.",
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
private fun BadgeStyleSection(
    title: String,
    currentStyle: BadgeStyle,
    onSelect: (BadgeStyle) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgeStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onSelect(style) },
                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    leadingIcon = if (currentStyle == style) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
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
                Text("App Labels")
            }
        },
        text = {
            Column {
                Text(
                    "Control the visibility of application labels across different areas of the launcher.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                val labelDescription = when (labelMode) {
                    AppLabelMode.BOTH -> "Labels are visible on both Home Screen and App Drawer for maximum clarity."
                    AppLabelMode.HOME_ONLY -> "Labels are only shown on the Home Screen. App Drawer remains clean and icon-focused."
                    AppLabelMode.DRAWER_ONLY -> "Labels are only shown in the App Drawer. Home Screen icons remain minimal without text."
                    AppLabelMode.NONE -> "All application labels are hidden for the most minimalist and clean experience."
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = labelDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Display labels on:",
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
                    text = "Keep your app list tidy by hiding apps you rarely use or want to keep private.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (preferences.showHiddenApps) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Show in Drawer Rail", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = preferences.showHiddenApps,
                            onCheckedChange = { viewModel.setShowHiddenApps(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

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
fun AboutDialog(onDismiss: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("About NeoGlide")
            }
        },
        text = {
            Column {
                Text(
                    "NeoGlide is an ergonomic, clean, and lightweight Android launcher optimized for fluidity and smart organization.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AboutInfoRow("Version", viewModel.getAppVersion())
                        AboutInfoRow("Build", viewModel.getAppBuild())
                        AboutInfoRow("Developer", "Samoyed Dev Studio")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    "This launcher is built with Jetpack Compose and prioritizes user privacy and performance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    var showPrivacyPolicy by remember { mutableStateOf(false) }
                    
                    if (showPrivacyPolicy) {
                        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
                    }

                    TextButton(
                        onClick = { showPrivacyPolicy = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Privacy Policy", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Privacy Policy")
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Effective Date: June 19, 2026",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Samoyed Dev Studio built the NeoGlide Launcher app as a Free app. This SERVICE is provided by Samoyed Dev Studio at no cost and is intended for use as is.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("1. Information Collection and Use", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Information requested is retained on your device and is not collected by us in any way. The app uses third-party services that may collect information used to identify you: Google Play Services, Firebase Analytics, and Firebase Crashlytics.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("2. Permissions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "• Query All Packages: Used to display and organize your apps.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("3. Data Safety", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "We do not sell or share your data with third parties. All app organization data is stored locally on your device.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("4. Contact Us", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "If you have any questions, contact us at smyddevstudio@gmail.com",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun AboutInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PremiumFeaturesDialog(
    isPremium: Boolean,
    productDetailsMap: Map<String, ProductDetails>,
    restoreStatus: BillingManager.RestoreStatus,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onUpgrade: (String) -> Unit
) {
    val isProcessing = restoreStatus is BillingManager.RestoreStatus.Processing

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
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
                           else "Unlock the full potential of NeoGlide Launcher with a choice of flexible plans.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (!isPremium) {
                    Text(
                        "Choose your plan:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Lifetime
                    PricingTier(
                        title = "Lifetime",
                        price = productDetailsMap[BillingManager.PRODUCT_LIFETIME]?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Loading...",
                        description = "Pay once, own forever. Best value.",
                        enabled = !isProcessing,
                        onClick = { onUpgrade(BillingManager.PRODUCT_LIFETIME) }
                    )
                    
                    // Yearly
                    PricingTier(
                        title = "Yearly",
                        price = productDetailsMap[BillingManager.PRODUCT_YEARLY]?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice?.let { "$it/year" } ?: "Loading...",
                        description = "Full access, billed annually.",
                        enabled = !isProcessing,
                        onClick = { onUpgrade(BillingManager.PRODUCT_YEARLY) }
                    )
                    
                    // Monthly
                    PricingTier(
                        title = "Monthly",
                        price = productDetailsMap[BillingManager.PRODUCT_MONTHLY]?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice?.let { "$it/month" } ?: "Loading...",
                        description = "Flexible access, cancel anytime.",
                        enabled = !isProcessing,
                        onClick = { onUpgrade(BillingManager.PRODUCT_MONTHLY) }
                    )
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Verifying with Google Play...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                PremiumFeatureItem(
                    title = "Privacy Vault",
                    description = "Hide sensitive apps and protect them with biometric security (Fingerprint/PIN).",
                    icon = Icons.Default.VisibilityOff
                )
                PremiumFeatureItem(
                    title = "Advanced Sorting",
                    description = "Sort your apps by Installation Time, Last Used, Icon Color, or Reverse the selected sort order.",
                    icon = Icons.Default.SortByAlpha
                )
                PremiumFeatureItem(
                    title = "Custom Search",
                    description = "Use preferred search providers.",
                    icon = Icons.Default.Search
                )
                PremiumFeatureItem(
                    title = "Multi-device Premium",
                    description = "Your premium status is tied to your Google Account and syncs automatically across all your devices.",
                    icon = Icons.Default.WorkspacePremium
                )
            }
        },
        confirmButton = {
            if (isPremium) {
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        },
        dismissButton = {
            if (!isPremium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    } else {
                        TextButton(onClick = onRestore) { Text("Restore") }
                    }
                    TextButton(onClick = onDismiss, enabled = !isProcessing) { Text("Maybe later") }
                }
            }
        }
    )
}

@Composable
private fun PricingTier(
    title: String,
    price: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.5f else 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    description, 
                    style = MaterialTheme.typography.bodySmall, 
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                price, 
                style = MaterialTheme.typography.titleMedium, 
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), 
                fontWeight = FontWeight.Bold
            )
        }
    }
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = getSortingDescription(currentMode, isReverse),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp).fillMaxWidth()
                    )
                }

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
                
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Vertical: $verticalDescription",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Horizontal: $horizontalDescription",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                
                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

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
                description?.let { desc ->
                    Text(
                        text = desc,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GridSettingsDialog(
    currentSize: GridSize,
    onDismiss: () -> Unit,
    onSelect: (GridSize) -> Unit
) {
    var selectedSize by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Home Screen Layout")
            }
        },
        text = {
            Column {
                Text(
                    "Choose how many apps and widgets fit on your home screen. Density and icon size adjust automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Density & Icon Size:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GridSize.entries.forEach { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { 
                                selectedSize = size
                            },
                            label = { Text(size.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            leadingIcon = if (selectedSize == size) {
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = when (selectedSize) {
                                GridSize.TINY -> "Maximum density with 48dp icons. Ideal for users who want to fit as much as possible on one screen."
                                GridSize.SMALL -> "Compact layout with 60dp icons. A balance between density and visibility."
                                GridSize.MEDIUM -> "Standard layout with 72dp icons. The recommended size for most modern displays."
                                GridSize.LARGE -> "Comfortable layout with 84dp icons and larger text. Ideal for maximum ease of use."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (selectedSize.ordinal > currentSize.ordinal) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning, 
                                null, 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Caution: Switching to a lower density (Larger icons) may remove items that no longer fit.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(selectedSize)
                    onDismiss()
                }
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
                Text(
                    "Advanced tools to restore default states, refresh cached assets, or access system-level application settings. Use these if you experience unexpected behavior.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                        icon = ResetAction.DISSOLVE_DRAWER_FOLDERS.icon,
                        title = ResetAction.DISSOLVE_DRAWER_FOLDERS.label,
                        description = ResetAction.DISSOLVE_DRAWER_FOLDERS.description,
                        showChevron = false,
                        onClick = { onAction(ResetAction.DISSOLVE_DRAWER_FOLDERS) }
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
    RESET_DRAWER("Reset App Drawer", "Clear drawer folders and reset all app categories to defaults.", Icons.Default.FolderDelete),
    DELETE_HOME_FOLDERS("Delete Home Folders", "Remove all folders from home screen. Apps inside will be removed from home screen.", Icons.Default.DeleteSweep),
    DISSOLVE_DRAWER_FOLDERS("Dissolve App Drawer Folders", "Remove all folders in the app drawer. Apps will return to their categories.", Icons.Default.FolderOff),
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

@Preview(showBackground = true)
@Composable
fun PremiumFeaturesDialogPreview() {
    com.samidevstudio.neoglide.ui.theme.NeoGlideLauncherTheme {
        PremiumFeaturesDialog(
            isPremium = false,
            productDetailsMap = emptyMap(),
            restoreStatus = BillingManager.RestoreStatus.Idle,
            onDismiss = {},
            onRestore = {},
            onUpgrade = {}
        )
    }
}


