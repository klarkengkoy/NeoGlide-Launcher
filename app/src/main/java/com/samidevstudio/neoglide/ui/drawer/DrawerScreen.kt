package com.samidevstudio.neoglide.ui.drawer

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samidevstudio.neoglide.data.repository.AppLabelMode
import com.samidevstudio.neoglide.data.repository.CategoryBarType
import com.samidevstudio.neoglide.data.repository.NotificationDotMode
import com.samidevstudio.neoglide.data.repository.SearchProvider
import com.samidevstudio.neoglide.data.repository.UserPreferences
import com.samidevstudio.neoglide.data.repository.VerticalAnchor
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.ui.components.AppContextMenu
import com.samidevstudio.neoglide.ui.components.AppItem
import com.samidevstudio.neoglide.ui.components.FolderExpansion
import com.samidevstudio.neoglide.ui.components.FolderItem
import com.samidevstudio.neoglide.ui.components.SearchAppItem
import com.samidevstudio.neoglide.ui.settings.SettingsSheet
import com.samidevstudio.neoglide.ui.settings.SettingsViewModel
import com.samidevstudio.neoglide.ui.utils.HapticEngine
import com.samidevstudio.neoglide.ui.utils.rememberHapticFeedback
import com.samidevstudio.neoglide.ui.utils.toIcon
import kotlinx.coroutines.launch

internal class DragTargetInfo {
    var isDragging: Boolean by mutableStateOf(false)
    var dragPosition by mutableStateOf(Offset.Zero)
    var dragOffset by mutableStateOf(Offset.Zero)
    var grabOffset by mutableStateOf(Offset.Zero)
    var hoveredCategory by mutableStateOf<AppCategory?>(null)
    var hoveredApp by mutableStateOf<AppModel?>(null)
    var hoveredFolderId by mutableIntStateOf(-1)
    var draggableItem by mutableStateOf<AppModel?>(null)
    var draggableFolderId by mutableIntStateOf(-1)
    var draggableFolderApps by mutableStateOf<List<AppModel>>(emptyList())
    var draggableFolderName by mutableStateOf("")
    var sourceFolderPosition by mutableStateOf(Offset.Zero)
}

internal class BoxedOffset {
    var value: Offset = Offset.Zero
}

internal val LocalDragTargetInfo = compositionLocalOf { DragTargetInfo() }

enum class CategoryOrientation {
    HORIZONTAL_BOTTOM,
    VERTICAL_LEFT,
    VERTICAL_RIGHT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DrawerScreen(
    modifier: Modifier = Modifier,
    viewModel: DrawerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
) {
    val dragInfo = remember { DragTargetInfo() }
    
    CompositionLocalProvider(LocalDragTargetInfo provides dragInfo) {
        DrawerContent(
            modifier = modifier,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onAppClick = onAppClick,
            onShortcutClick = onShortcutClick,
            dragInfo = dragInfo
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun DrawerContent(
    modifier: Modifier,
    viewModel: DrawerViewModel,
    settingsViewModel: SettingsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    dragInfo: DragTargetInfo
) {
    val categorizedApps by viewModel.categorizedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val recentlyUsedApps by viewModel.recentlyUsedApps.collectAsStateWithLifecycle()
    val webSuggestions by viewModel.webSuggestions.collectAsStateWithLifecycle()
    val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
    val refreshTrigger by viewModel.refreshTrigger.collectAsStateWithLifecycle()
    val preferences by settingsViewModel.userPreferences.collectAsStateWithLifecycle()
    val hapticFeedback = rememberHapticFeedback(preferences)
    
    val context = LocalContext.current
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val gridItems by viewModel.gridItems.collectAsStateWithLifecycle()
    
    var showSettings by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var expandedFolderId by remember { mutableIntStateOf(-1) }
    var isFolderInvisibleByDrag by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var pendingFolderIdForAdd by remember { mutableIntStateOf(-1) }
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var drawerRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val categories = remember(categorizedApps) {
        categorizedApps.keys.sortedWith(
            compareBy<AppCategory?> { it == AppCategory.HIDDEN }
                .thenBy { it?.name ?: "" }
        )
    }

    val showCategoryBar = preferences.categoryBarType != CategoryBarType.NONE
    val orientation = remember(preferences.categoryBarType) {
        when (preferences.categoryBarType) {
            CategoryBarType.LEFT -> CategoryOrientation.VERTICAL_LEFT
            CategoryBarType.RIGHT -> CategoryOrientation.VERTICAL_RIGHT
            CategoryBarType.BOTTOM -> CategoryOrientation.HORIZONTAL_BOTTOM
            CategoryBarType.NONE -> CategoryOrientation.VERTICAL_LEFT
        }
    }

    LaunchedEffect(categories, showCategoryBar) {
        if (!showCategoryBar) {
            viewModel.selectCategory(null)
        } else if (selectedCategory == null && categories.isNotEmpty()) {
            viewModel.selectCategory(categories.first())
        }
    }

    LaunchedEffect(animatedVisibilityScope.transition.currentState) {
        if (animatedVisibilityScope.transition.currentState == EnterExitState.PostExit) {
            isSearchActive = false
            viewModel.resetState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is DrawerUiEvent.ShowToast) {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(isSearchActive || showSettings || expandedFolderId != -1 || showAppPicker) {
        when {
            showAppPicker -> {
                showAppPicker = false
                pendingFolderIdForAdd = -1
            }
            expandedFolderId != -1 -> expandedFolderId = -1
            showSettings -> showSettings = false
            isSearchActive -> {
                isSearchActive = false
                viewModel.onSearchQueryChanged("")
                focusManager.clearFocus()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { drawerRootCoords = it }
    ) {
        DrawerMainLayout(
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            focusRequester = focusRequester,
            focusManager = focusManager,
            viewModel = viewModel,
            filteredApps = filteredApps,
            recentlyUsedApps = recentlyUsedApps,
            webSuggestions = webSuggestions,
            preferences = preferences,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onAppClick = onAppClick,
            onShortcutClick = onShortcutClick,
            activeNotifications = activeNotifications,
            gridItems = gridItems,
            categories = categories,
            showCategoryBar = showCategoryBar,
            orientation = orientation,
            refreshTrigger = refreshTrigger,
            hapticFeedback = hapticFeedback,
            drawerRootCoords = drawerRootCoords,
            onSearchActiveChange = { isSearchActive = it },
            onSettingsClick = { showSettings = true },
            onFolderClick = { expandedFolderId = it }
        )

        DrawerOverlays(
            showSettings = showSettings,
            expandedFolderId = expandedFolderId,
            showAppPicker = showAppPicker,
            isFolderInvisibleByDrag = isFolderInvisibleByDrag,
            pendingFolderIdForAdd = pendingFolderIdForAdd,
            categorizedApps = categorizedApps,
            selectedCategory = selectedCategory,
            categories = categories,
            preferences = preferences,
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onAppClick = onAppClick,
            onShortcutClick = onShortcutClick,
            hapticFeedback = hapticFeedback,
            dragInfo = dragInfo,
            drawerRootCoords = drawerRootCoords,
            refreshTrigger = refreshTrigger,
            onDismissSettings = { showSettings = false },
            onDismissFolder = { expandedFolderId = -1 },
            onSetFolderInvisible = { isFolderInvisibleByDrag = it },
            onShowAppPicker = { showAppPicker = it },
            onSetPendingFolder = { pendingFolderIdForAdd = it }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DrawerMainLayout(
    selectedCategory: AppCategory?,
    searchQuery: String,
    isSearchActive: Boolean,
    focusRequester: FocusRequester,
    focusManager: androidx.compose.ui.focus.FocusManager,
    viewModel: DrawerViewModel,
    filteredApps: List<AppModel>,
    recentlyUsedApps: List<AppModel>,
    webSuggestions: List<String>,
    preferences: UserPreferences,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    activeNotifications: Map<String, Int>,
    gridItems: Map<AppCategory?, List<DrawerItem?>>,
    categories: List<AppCategory?>,
    showCategoryBar: Boolean,
    orientation: CategoryOrientation,
    refreshTrigger: Int,
    hapticFeedback: (HapticEngine.HapticType) -> Unit,
    drawerRootCoords: LayoutCoordinates?,
    onSearchActiveChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onFolderClick: (Int) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .padding(top = 16.dp),
    ) {
        DrawerHeader(
            selectedCategory = selectedCategory,
            searchQuery = searchQuery,
            isSearchActive = isSearchActive,
            focusRequester = focusRequester,
            onSearchActiveChange = onSearchActiveChange,
            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
            onSettingsClick = onSettingsClick,
            onLaunchFirstResult = { 
                filteredApps.firstOrNull()?.let { app ->
                    onAppClick(app.packageName, null)
                }
            },
            onClearFocus = { focusManager.clearFocus() }
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            if (searchQuery.isNotBlank() || isSearchActive) {
                DrawerSearchResults(
                    filteredApps = filteredApps,
                    recentlyUsedApps = if (searchQuery.isBlank()) recentlyUsedApps else emptyList(),
                    webSuggestions = webSuggestions,
                    searchProvider = preferences.searchProvider,
                    useMonochrome = preferences.useMonochromeIcons,
                    iconPackPackageName = preferences.iconPackPackageName,
                    hiddenPackages = preferences.hiddenPackages,
                    activeNotifications = activeNotifications,
                    notificationDotMode = preferences.notificationDotMode,
                    appLabelMode = preferences.appLabelMode,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onAppClick = onAppClick,
                    onShortcutClick = onShortcutClick,
                    getShortcuts = { viewModel.getShortcuts(it) },
                    onHideToggle = { packageName, isHidden ->
                        if (isHidden) viewModel.unhideApp(packageName) else viewModel.hideApp(packageName)
                    },
                    onWebSearch = { query ->
                        val provider = preferences.searchProvider
                        if (provider != SearchProvider.LOCAL_ONLY) {
                            val intent = Intent(Intent.ACTION_VIEW, "${provider.searchUrl}$query".toUri())
                            context.startActivity(intent)
                        }
                    }
                )
            } else {
                val categoryNotifications by viewModel.categoryNotifications.collectAsStateWithLifecycle()

                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    val isVertical = orientation != CategoryOrientation.HORIZONTAL_BOTTOM
                    val barWidth = if (!showCategoryBar) 0.dp else if (isVertical) 56.dp else 0.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (showCategoryBar && orientation == CategoryOrientation.VERTICAL_LEFT) {
                            DrawerCategoryRail(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                orientation = orientation,
                                barWidth = barWidth,
                                categoryNotifications = categoryNotifications,
                                showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH),
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onHapticFeedback = hapticFeedback
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            val currentGridItems = remember(gridItems, selectedCategory) {
                                gridItems[selectedCategory] ?: emptyList()
                            }
                            AppGrid(
                                items = currentGridItems,
                                columns = 4,
                                onFolderClick = onFolderClick,
                                bottomPadding = if (showCategoryBar && orientation == CategoryOrientation.HORIZONTAL_BOTTOM) 8.dp else 20.dp,
                                modifier = Modifier.weight(1f),
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = preferences.useMonochromeIcons,
                                iconPackPackageName = preferences.iconPackPackageName,
                                hiddenPackages = preferences.hiddenPackages,
                                isLocked = preferences.lockLayout,
                                verticalAnchor = preferences.verticalAnchor,
                                activeNotifications = activeNotifications,
                                showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.APP_ICON, NotificationDotMode.BOTH),
                                showLabel = preferences.appLabelMode == AppLabelMode.DRAWER_ONLY || preferences.appLabelMode == AppLabelMode.BOTH,
                                refreshTrigger = refreshTrigger,
                                rootCoords = drawerRootCoords,
                                getShortcuts = { viewModel.getShortcuts(it) },
                                onShortcutClick = onShortcutClick,
                                onHideToggle = { packageName, isHidden ->
                                    if (isHidden) viewModel.unhideApp(packageName) else viewModel.hideApp(packageName)
                                },
                                onHapticFeedback = hapticFeedback,
                                onDrop = { app, category -> 
                                    category?.let { viewModel.moveAppToCategory(app.packageName, it) }
                                },
                                onMerge = { appA, appB ->
                                    viewModel.createFolder(appA, appB, selectedCategory)
                                },
                                onFolderMerge = { app, folderId ->
                                    viewModel.addAppToFolder(folderId, app.packageName)
                                },
                                onFolderMove = { folderId, category ->
                                    viewModel.moveFolderToCategory(folderId, category)
                                },
                                onAppClick = onAppClick
                            )

                            if (showCategoryBar && orientation == CategoryOrientation.HORIZONTAL_BOTTOM) {
                                CategorySelector(
                                    allCategories = categories,
                                    selectedCategory = selectedCategory,
                                    orientation = orientation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    onCategorySelected = { viewModel.selectCategory(it) },
                                    categoryNotifications = categoryNotifications,
                                    showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH),
                                    onHapticFeedback = hapticFeedback
                                )
                            }
                        }

                        if (showCategoryBar && orientation == CategoryOrientation.VERTICAL_RIGHT) {
                            DrawerCategoryRail(
                                categories = categories,
                                selectedCategory = selectedCategory,
                                orientation = orientation,
                                barWidth = barWidth,
                                categoryNotifications = categoryNotifications,
                                showNotificationDots = preferences.notificationDotMode in listOf(NotificationDotMode.CATEGORY_BAR, NotificationDotMode.BOTH),
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onHapticFeedback = hapticFeedback
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DrawerOverlays(
    showSettings: Boolean,
    expandedFolderId: Int,
    showAppPicker: Boolean,
    isFolderInvisibleByDrag: Boolean,
    pendingFolderIdForAdd: Int,
    categorizedApps: Map<AppCategory?, List<DrawerItem>>,
    selectedCategory: AppCategory?,
    categories: List<AppCategory?>,
    preferences: UserPreferences,
    viewModel: DrawerViewModel,
    settingsViewModel: SettingsViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    hapticFeedback: (HapticEngine.HapticType) -> Unit,
    dragInfo: DragTargetInfo,
    drawerRootCoords: LayoutCoordinates?,
    refreshTrigger: Int,
    onDismissSettings: () -> Unit,
    onDismissFolder: () -> Unit,
    onSetFolderInvisible: (Boolean) -> Unit,
    onShowAppPicker: (Boolean) -> Unit,
    onSetPendingFolder: (Int) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current

    if (showSettings) {
        SettingsSheet(
            onDismiss = onDismissSettings,
            viewModel = settingsViewModel
        )
    }

    val currentExpandedFolder = remember(expandedFolderId, categorizedApps, selectedCategory) {
        if (expandedFolderId == -1) null
        else {
            categorizedApps[selectedCategory]?.find { it is DrawerItem.Folder && it.id == expandedFolderId } as? DrawerItem.Folder
        }
    }

    AnimatedVisibility(
        visible = expandedFolderId != -1 && currentExpandedFolder != null,
        enter = fadeIn() + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut() + scaleOut(targetScale = 0.8f, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.samidevstudio.neoglide.ui.components.FrostedGlass(
                modifier = Modifier.fillMaxSize(),
                blurRadius = 24.dp,
                tintColor = Color.Black.copy(alpha = 0.4f)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (isFolderInvisibleByDrag) 0f else 1f }
            ) {
                currentExpandedFolder?.let { folder ->
                    FolderExpansion(
                        folderId = folder.id,
                        label = folder.label,
                        apps = folder.apps,
                        onDismiss = onDismissFolder,
                        onDissolve = { viewModel.dissolveFolder(folder.id) },
                        onAddApps = {
                            onSetPendingFolder(folder.id)
                            onShowAppPicker(true)
                        },
                        onMoveToCategory = { viewModel.moveFolderToCategory(folder.id, it) },
                        isDrawerFolder = true,
                        isLocked = preferences.lockLayout,
                        currentCategory = selectedCategory,
                        allCategories = categories,
                        onLabelChange = { viewModel.updateFolderLabel(folder.id, it) },
                        onAppClick = onAppClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        useMonochrome = preferences.useMonochromeIcons,
                        iconPackPackageName = preferences.iconPackPackageName,
                        refreshTrigger = refreshTrigger,
                        getShortcuts = { viewModel.getShortcuts(it) },
                        onShortcutClick = onShortcutClick,
                        onHideToggle = { viewModel.hideApp(it) },
                        onHapticFeedback = hapticFeedback,
                        onAppDragStart = { app, windowOffset, grabPoint ->
                            hapticFeedback(HapticEngine.HapticType.DRAG_START)
                            dragInfo.isDragging = true
                            dragInfo.draggableItem = app
                            dragInfo.dragPosition = windowOffset + grabPoint
                            dragInfo.dragOffset = Offset.Zero
                            dragInfo.grabOffset = grabPoint
                            dragInfo.sourceFolderPosition = drawerRootCoords?.windowToLocal(windowOffset + grabPoint) ?: (windowOffset + grabPoint)
                        },
                        onAppDrag = { amount -> dragInfo.dragOffset += amount },
                        onAppDragOut = { _, _, _ -> onSetFolderInvisible(true) },
                        onAppDragEnd = {
                            hapticFeedback(HapticEngine.HapticType.DRAG_END)
                            val appToDrop = dragInfo.draggableItem
                            val categoryToDrop = dragInfo.hoveredCategory
                            val targetApp = dragInfo.hoveredApp
                            val targetFolderId = dragInfo.hoveredFolderId
                            
                            if (appToDrop != null) {
                                val finalTouchPos = dragInfo.dragPosition + dragInfo.dragOffset
                                val localTouch = drawerRootCoords?.windowToLocal(finalTouchPos) ?: finalTouchPos
                                val itemSizePx = with(density) { 80.dp.toPx() }
                                val sourceRect = android.graphics.RectF(
                                    dragInfo.sourceFolderPosition.x - itemSizePx / 2f,
                                    dragInfo.sourceFolderPosition.y - itemSizePx / 2f,
                                    dragInfo.sourceFolderPosition.x + itemSizePx / 2f,
                                    dragInfo.sourceFolderPosition.y + itemSizePx / 2f
                                )
                                val isBackOnParent = sourceRect.contains(localTouch.x, localTouch.y)

                                if (targetFolderId == folder.id || (isFolderInvisibleByDrag && isBackOnParent)) {
                                    viewModel.showToast("Item already in this folder")
                                } else if (targetFolderId != -1) {
                                    viewModel.addAppToFolder(targetFolderId, appToDrop.packageName)
                                } else if (targetApp != null && appToDrop.packageName != targetApp.packageName) {
                                    viewModel.createFolder(appA = appToDrop, appB = targetApp, category = selectedCategory)
                                } else if (categoryToDrop != null) {
                                    viewModel.moveAppToCategory(appToDrop.packageName, categoryToDrop)
                                } else if (isFolderInvisibleByDrag) {
                                    viewModel.removeAppFromFolder(folder.id, appToDrop.packageName)
                                }
                            }
                            
                            dragInfo.isDragging = false
                            dragInfo.draggableItem = null
                            dragInfo.hoveredCategory = null
                            dragInfo.hoveredApp = null
                            dragInfo.hoveredFolderId = -1
                            onDismissFolder()
                            onSetFolderInvisible(false)
                        },
                        onAppDragCancel = {
                            dragInfo.isDragging = false
                            dragInfo.draggableItem = null
                            dragInfo.hoveredCategory = null
                            dragInfo.hoveredApp = null
                            dragInfo.hoveredFolderId = -1
                            onDismissFolder()
                            onSetFolderInvisible(false)
                        }
                    )
                }
            }
        }
    }

    DrawerFloatingDragIcon(dragInfo = dragInfo, useMonochrome = preferences.useMonochromeIcons)

    if (showAppPicker) {
        val allApps by viewModel.allApps.collectAsStateWithLifecycle()
        val recentApps by viewModel.recentlyUsedApps.collectAsStateWithLifecycle()
        
        val memberPackageNames = remember(pendingFolderIdForAdd, categorizedApps) {
            categorizedApps.values.flatten()
                .filterIsInstance<DrawerItem.Folder>()
                .find { it.id == pendingFolderIdForAdd }
                ?.apps?.map { it.packageName }?.toSet() ?: emptySet()
        }
        
        com.samidevstudio.neoglide.ui.components.MultiAppPickerDialog(
            title = "Move apps here",
            allApps = allApps,
            memberPackageNames = memberPackageNames,
            recentlyUsedApps = recentApps,
            onToggleMember = { app, isChecked ->
                if (isChecked) {
                    viewModel.addAppToFolder(pendingFolderIdForAdd, app.packageName)
                } else {
                    viewModel.removeAppFromFolder(pendingFolderIdForAdd, app.packageName)
                }
            },
            onDismissRequest = {
                onShowAppPicker(false)
                onSetPendingFolder(-1)
            }
        )
    }
}

@Composable
private fun DrawerFloatingDragIcon(
    dragInfo: DragTargetInfo,
    useMonochrome: Boolean
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val isLifting = dragInfo.isDragging && (dragInfo.draggableItem != null || dragInfo.draggableFolderId != -1)
    val liftScale by animateFloatAsState(
        targetValue = if (isLifting) 1.2f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "liftScaleDrawer"
    )

    if (dragInfo.isDragging && (dragInfo.draggableItem != null || dragInfo.draggableFolderId != -1)) {
        Box(
            modifier = Modifier
                .offset { 
                    androidx.compose.ui.unit.IntOffset(
                        (dragInfo.dragPosition.x + dragInfo.dragOffset.x).toInt(),
                        (dragInfo.dragPosition.y + dragInfo.dragOffset.y).toInt()
                    )
                }
                .size(80.dp)
                .graphicsLayer {
                    alpha = 1f
                    scaleX = liftScale
                    scaleY = liftScale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                        dragInfo.grabOffset.x / with(density) { 80.dp.toPx() },
                        dragInfo.grabOffset.y / with(density) { 80.dp.toPx() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (dragInfo.draggableItem != null) {
                com.samidevstudio.neoglide.ui.components.AppIcon(
                    packageName = dragInfo.draggableItem!!.packageName,
                    contentDescription = dragInfo.draggableItem!!.label,
                    useMonochrome = useMonochrome
                )
            } else if (dragInfo.draggableFolderId != -1) {
                FolderItem(
                    label = dragInfo.draggableFolderName,
                    apps = dragInfo.draggableFolderApps,
                    useMonochrome = useMonochrome,
                    showLabel = false,
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    selectedCategory: AppCategory?,
    searchQuery: String,
    isSearchActive: Boolean,
    focusRequester: FocusRequester,
    onSearchActiveChange: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onLaunchFirstResult: () -> Unit,
    onClearFocus: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedCategory?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            
            IconButton(
                onClick = { onSearchActiveChange(true) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = "Search", 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(
                onClick = { 
                    val intent = context.packageManager.getLaunchIntentForPackage("com.android.vending")
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Storefront, 
                    contentDescription = "Play Store", 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Tune, 
                    contentDescription = "NeoGlide Settings", 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(
                onClick = { 
                    context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Settings, 
                    contentDescription = "System Settings", 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = scaleIn(
                animationSpec = tween(400),
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.8f, 0.5f)
            ) + fadeIn(animationSpec = tween(400)),
            exit = scaleOut(
                animationSpec = tween(400),
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.8f, 0.5f)
            ) + fadeOut(animationSpec = tween(400))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { 
                            onLaunchFirstResult()
                            onSearchActiveChange(false)
                            onSearchQueryChange("")
                            onClearFocus()
                        }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = { 
                        onSearchActiveChange(false)
                        onSearchQueryChange("")
                        onClearFocus()
                    }) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Cancel",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            LaunchedEffect(isSearchActive) {
                if (isSearchActive) {
                    focusRequester.requestFocus()
                }
            }
        }
    }
}

@Composable
private fun DrawerCategoryRail(
    categories: List<AppCategory?>,
    selectedCategory: AppCategory?,
    orientation: CategoryOrientation,
    barWidth: androidx.compose.ui.unit.Dp,
    categoryNotifications: Map<AppCategory?, Pair<Boolean, Int>>,
    showNotificationDots: Boolean,
    onCategorySelected: (AppCategory?) -> Unit,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit
) {
    Box(
        modifier = Modifier
            .width(barWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        CategorySelector(
            allCategories = categories,
            selectedCategory = selectedCategory,
            orientation = orientation,
            onCategorySelected = onCategorySelected,
            categoryNotifications = categoryNotifications,
            showNotificationDots = showNotificationDots,
            onHapticFeedback = onHapticFeedback
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DrawerSearchResults(
    filteredApps: List<AppModel>,
    recentlyUsedApps: List<AppModel>,
    webSuggestions: List<String>,
    searchProvider: SearchProvider,
    useMonochrome: Boolean,
    iconPackPackageName: String?,
    hiddenPackages: Set<String>,
    activeNotifications: Map<String, Int>,
    notificationDotMode: NotificationDotMode,
    appLabelMode: AppLabelMode,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    onShortcutClick: (AppShortcut) -> Unit,
    getShortcuts: suspend (String) -> List<AppShortcut>,
    onHideToggle: (String, Boolean) -> Unit,
    onWebSearch: (String) -> Unit
) {
    SearchResults(
        filteredApps = filteredApps,
        recentlyUsedApps = recentlyUsedApps,
        webSuggestions = webSuggestions,
        searchProviderName = searchProvider.displayName,
        modifier = Modifier.fillMaxSize(),
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onAppClick = onAppClick,
        useMonochrome = useMonochrome,
        iconPackPackageName = iconPackPackageName,
        hiddenPackages = hiddenPackages,
        activeNotifications = activeNotifications,
        showNotificationDots = notificationDotMode in listOf(NotificationDotMode.APP_ICON, NotificationDotMode.BOTH),
        showLabel = appLabelMode == AppLabelMode.DRAWER_ONLY || appLabelMode == AppLabelMode.BOTH,
        getShortcuts = getShortcuts,
        onShortcutClick = onShortcutClick,
        onHideToggle = onHideToggle,
        onWebSearch = onWebSearch
    )
}

@Composable
fun CategorySelector(
    allCategories: List<AppCategory?>,
    selectedCategory: AppCategory?,
    orientation: CategoryOrientation,
    modifier: Modifier = Modifier,
    onCategorySelected: (AppCategory?) -> Unit,
    categoryNotifications: Map<AppCategory?, Pair<Boolean, Int>> = emptyMap(),
    showNotificationDots: Boolean = true,
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {}
) {
    val isVertical = orientation != CategoryOrientation.HORIZONTAL_BOTTOM
    val density = androidx.compose.ui.platform.LocalDensity.current
    val dragInfo = LocalDragTargetInfo.current
    
    val iconSize = 40.dp
    val defaultSpacing = 8.dp
    
    var containerSize by remember { mutableFloatStateOf(0f) }
    var containerOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var lastHoveredIndex by remember { mutableIntStateOf(-1) }

    val currentSelectedCategory by rememberUpdatedState(selectedCategory)
    val currentAllCategories by rememberUpdatedState(allCategories)

    val handleGesture = remember(containerSize, isVertical) {
        { offset: androidx.compose.ui.geometry.Offset ->
            val count = currentAllCategories.size
            if (count > 0 && containerSize > 0) {
                val iconSizePx = with(density) { iconSize.toPx() }
                val spacingPx = with(density) { defaultSpacing.toPx() }
                val totalContentSize = (iconSizePx * count) + (spacingPx * (count - 1))
                val startOffset = (containerSize - totalContentSize) / 2
                val touchPos = if (isVertical) offset.y else offset.x
                val relativeTouch = touchPos - startOffset
                val itemSizeWithSpacing = iconSizePx + spacingPx
                
                val index = when {
                    relativeTouch < 0 -> 0
                    relativeTouch >= totalContentSize -> count - 1
                    else -> (relativeTouch / itemSizeWithSpacing).toInt().coerceIn(0, count - 1)
                }

                val newCategory = currentAllCategories[index]
                if (newCategory != currentSelectedCategory) {
                    onHapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                    onCategorySelected(newCategory)
                }
            }
        }
    }

    val content = @Composable {
        allCategories.forEachIndexed { index, category ->
            val itemSizePx = with(density) { iconSize.toPx() }
            val spacingPx = with(density) { defaultSpacing.toPx() }
            val totalContentSize = (itemSizePx * allCategories.size) + (spacingPx * (allCategories.size - 1))
            val centeringOffset = (containerSize - totalContentSize) / 2
            val itemStart = centeringOffset + index * (itemSizePx + spacingPx)
            val itemEnd = itemStart + itemSizePx
            
            var isHovered = false
            if (dragInfo.isDragging) {
                val globalTouchPos = dragInfo.dragPosition + dragInfo.dragOffset
                val localTouchPos = if (isVertical) globalTouchPos.y - containerOffset.y else globalTouchPos.x - containerOffset.x
                val crossAxisTouchPos = if (isVertical) globalTouchPos.x - containerOffset.x else globalTouchPos.y - containerOffset.y
                
                val railWidthPx = with(density) { 56.dp.toPx() }
                isHovered = crossAxisTouchPos >= 0 && crossAxisTouchPos <= railWidthPx &&
                                localTouchPos >= itemStart && localTouchPos <= itemEnd
                
                if (isHovered) {
                    dragInfo.hoveredCategory = category
                    if (lastHoveredIndex != index) {
                        onHapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                        lastHoveredIndex = index
                    }
                } else if (lastHoveredIndex == index) {
                    if (dragInfo.hoveredCategory == category) {
                        dragInfo.hoveredCategory = null
                    }
                    lastHoveredIndex = -1
                }
            }
            
            val categoryNotifs = categoryNotifications[category] ?: (false to 0)

            CategoryIconItem(
                isSelected = selectedCategory == category,
                icon = category?.toIcon() ?: Icons.Default.AllInclusive,
                isHovered = isHovered,
                hasNotification = categoryNotifs.first && showNotificationDots,
                notificationCount = categoryNotifs.second,
                size = iconSize
            )
            
            if (index < allCategories.size - 1) {
                if (isVertical) Spacer(modifier = Modifier.height(defaultSpacing))
                else Spacer(modifier = Modifier.width(defaultSpacing))
            }
        }
    }

    if (isVertical) {
        Column(
            modifier = modifier
                .fillMaxHeight()
                .onGloballyPositioned { 
                    containerSize = it.size.height.toFloat()
                    containerOffset = it.positionInWindow()
                }
                .pointerInput(containerSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handleGesture(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            handleGesture(change.position)
                        }
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .onGloballyPositioned { 
                    containerSize = it.size.width.toFloat()
                    containerOffset = it.positionInWindow()
                }
                .pointerInput(containerSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        handleGesture(down.position)
                        drag(down.id) { change ->
                            change.consume()
                            handleGesture(change.position)
                        }
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
private fun CategoryIconItem(
    isSelected: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isHovered: Boolean = false,
    hasNotification: Boolean = false,
    notificationCount: Int = 0,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    val scale by animateFloatAsState(if (isHovered) 1.2f else 1f)
    val color = if (isHovered) MaterialTheme.colorScheme.secondaryContainer else if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .then(if (isSelected && !isHovered) Modifier.padding(4.dp) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = color,
            contentColor = if (isHovered) MaterialTheme.colorScheme.onSecondaryContainer else if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
        }
        
        if (hasNotification) {
            if (notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(1.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(1.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppGrid(
    items: List<DrawerItem?>,
    columns: Int,
    onFolderClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: androidx.compose.ui.unit.Dp = 20.dp,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    hiddenPackages: Set<String> = emptySet(),
    isLocked: Boolean = false,
    verticalAnchor: VerticalAnchor = VerticalAnchor.TOP,
    activeNotifications: Map<String, Int> = emptyMap(),
    showNotificationDots: Boolean = true,
    showLabel: Boolean = true,
    refreshTrigger: Int = 0,
    rootCoords: androidx.compose.ui.layout.LayoutCoordinates? = null,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String, Boolean) -> Unit = { _, _ -> },
    onHapticFeedback: (HapticEngine.HapticType) -> Unit = {},
    onDrop: (AppModel, AppCategory?) -> Unit = { _, _ -> },
    onMerge: (AppModel, AppModel) -> Unit = { _, _ -> },
    onFolderMerge: (AppModel, Int) -> Unit = { _, _ -> },
    onFolderMove: (Int, AppCategory) -> Unit = { _, _ -> },
    onAppClick: (String, android.os.Bundle?) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        reverseLayout = false,
        contentPadding = PaddingValues(bottom = bottomPadding, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = if (verticalAnchor == VerticalAnchor.BOTTOM) {
            Arrangement.spacedBy(16.dp, Alignment.Bottom)
        } else {
            Arrangement.spacedBy(16.dp, Alignment.Top)
        },
        modifier = modifier
    ) {
        items(
            count = items.size,
            key = { index ->
                val it = items[index]
                if (it == null) "placeholder_$index"
                else {
                    when (it) {
                        is DrawerItem.App -> it.appModel.packageName
                        is DrawerItem.Folder -> "folder_${it.id}"
                    }
                }
            }
        ) { index ->
            val drawerItem = items[index]
            if (drawerItem == null) {
                Spacer(modifier = Modifier.fillMaxWidth().height(80.dp))
            } else {
                val context = LocalContext.current
                val dragInfo = LocalDragTargetInfo.current
                val density = androidx.compose.ui.platform.LocalDensity.current
                val coroutineScope = rememberCoroutineScope()
                
                val itemPosition = remember { BoxedOffset() }
                var accumulatedDrag by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                var isDragConfirmed by remember { mutableStateOf(false) }
                var showMenu by remember { mutableStateOf(false) }
                var shortcuts by remember { mutableStateOf<List<com.samidevstudio.neoglide.domain.model.AppShortcut>>(emptyList()) }

                Box(
                    modifier = Modifier
                        .onGloballyPositioned { 
                            itemPosition.value = it.positionInWindow()
                        }
                        .pointerInput(drawerItem) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    if (!isLocked) {
                                        onHapticFeedback(HapticEngine.HapticType.LONG_PRESS)
                                        accumulatedDrag = androidx.compose.ui.geometry.Offset.Zero
                                        isDragConfirmed = false
                                        val currentPos = itemPosition.value
                                        dragInfo.dragPosition = rootCoords?.windowToLocal(currentPos) ?: currentPos
                                        dragInfo.grabOffset = offset
                                        if (drawerItem is DrawerItem.App) {
                                            dragInfo.draggableItem = drawerItem.appModel
                                        } else if (drawerItem is DrawerItem.Folder) {
                                            dragInfo.draggableFolderId = drawerItem.id
                                            dragInfo.draggableFolderApps = drawerItem.apps
                                            dragInfo.draggableFolderName = drawerItem.label
                                        }
                                    } else {
                                        Toast.makeText(context, "Locked from launcher settings", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    if (!isLocked) {
                                        change.consume()
                                        accumulatedDrag += dragAmount
                                        if (!isDragConfirmed && accumulatedDrag.getDistance() > with(density) { 10.dp.toPx() }) {
                                            isDragConfirmed = true
                                            onHapticFeedback(HapticEngine.HapticType.DRAG_START)
                                            dragInfo.isDragging = true
                                            dragInfo.dragOffset = accumulatedDrag
                                        }
                                        if (isDragConfirmed) {
                                            dragInfo.dragOffset += dragAmount
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (!isLocked) {
                                        if (isDragConfirmed) {
                                            onHapticFeedback(HapticEngine.HapticType.DRAG_END)
                                            val appToDrop = dragInfo.draggableItem
                                            val folderToDrop = dragInfo.draggableFolderId
                                            val categoryToDrop = dragInfo.hoveredCategory
                                            val targetApp = dragInfo.hoveredApp
                                            val targetFolderId = dragInfo.hoveredFolderId
                                            if (appToDrop != null) {
                                                if (targetFolderId != -1) {
                                                    onFolderMerge(appToDrop, targetFolderId)
                                                } else if (targetApp != null && appToDrop.packageName != targetApp.packageName) {
                                                    onMerge(appToDrop, targetApp)
                                                } else if (categoryToDrop != null) {
                                                    onDrop(appToDrop, categoryToDrop)
                                                }
                                            } else if (folderToDrop != -1) {
                                                if (categoryToDrop != null) {
                                                    onFolderMove(folderToDrop, categoryToDrop)
                                                }
                                            }
                                        } else {
                                            if (drawerItem is DrawerItem.App) {
                                                coroutineScope.launch {
                                                    shortcuts = getShortcuts(drawerItem.appModel.packageName)
                                                    showMenu = true
                                                }
                                            }
                                        }
                                    }
                                    dragInfo.isDragging = false
                                    dragInfo.draggableItem = null
                                    dragInfo.draggableFolderId = -1
                                    dragInfo.hoveredCategory = null
                                    dragInfo.hoveredApp = null
                                    dragInfo.hoveredFolderId = -1
                                    isDragConfirmed = false
                                },
                                onDragCancel = {
                                    if (!isLocked) {
                                        dragInfo.isDragging = false
                                        dragInfo.draggableItem = null
                                        dragInfo.draggableFolderId = -1
                                        dragInfo.hoveredCategory = null
                                        dragInfo.hoveredApp = null
                                        dragInfo.hoveredFolderId = -1
                                        isDragConfirmed = false
                                    }
                                }
                            )
                        }
                ) {
                    if (dragInfo.isDragging) {
                        val dragTopLeft = dragInfo.dragPosition + dragInfo.dragOffset
                        val currentPos = itemPosition.value
                        val targetTopLeft = rootCoords?.windowToLocal(currentPos) ?: currentPos
                        val distSq = (dragTopLeft.x - targetTopLeft.x) * (dragTopLeft.x - targetTopLeft.x) +
                                   (dragTopLeft.y - targetTopLeft.y) * (dragTopLeft.y - targetTopLeft.y)
                        val thresholdPx = with(density) { 40.dp.toPx() }
                        val isHovered = distSq < (thresholdPx * thresholdPx)
                        if (isHovered) {
                            if (drawerItem is DrawerItem.App && dragInfo.draggableItem?.packageName != drawerItem.appModel.packageName) {
                                if (dragInfo.hoveredApp?.packageName != drawerItem.appModel.packageName) {
                                    onHapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                }
                                dragInfo.hoveredApp = drawerItem.appModel
                            } else if (drawerItem is DrawerItem.Folder && dragInfo.draggableFolderId != drawerItem.id) {
                                if (dragInfo.hoveredFolderId != drawerItem.id) {
                                    onHapticFeedback(HapticEngine.HapticType.GRID_SNAP)
                                }
                                dragInfo.hoveredFolderId = drawerItem.id
                            }
                        } else {
                            if (drawerItem is DrawerItem.App && dragInfo.hoveredApp?.packageName == drawerItem.appModel.packageName) {
                                dragInfo.hoveredApp = null
                            } else if (drawerItem is DrawerItem.Folder && dragInfo.hoveredFolderId == drawerItem.id) {
                                dragInfo.hoveredFolderId = -1
                            }
                        }
                    }

                    val isHoveredByDrag = dragInfo.isDragging && (
                        dragInfo.hoveredApp?.packageName == (drawerItem as? DrawerItem.App)?.appModel?.packageName ||
                        dragInfo.hoveredFolderId == (drawerItem as? DrawerItem.Folder)?.id
                    )

                    when (drawerItem) {
                        is DrawerItem.App -> {
                            val isBeingDragged = dragInfo.isDragging && dragInfo.draggableItem?.packageName == drawerItem.appModel.packageName
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = if (isHoveredByDrag) 0.8f else 1f
                                    scaleY = if (isHoveredByDrag) 0.8f else 1f
                                    alpha = if (isBeingDragged) 0f else 1f
                                }
                            ) {
                                AppItem(
                                    app = drawerItem.appModel,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    useMonochrome = useMonochrome,
                                    iconPackPackageName = iconPackPackageName,
                                    isHidden = drawerItem.appModel.packageName in hiddenPackages,
                                    hasNotification = showNotificationDots && drawerItem.appModel.packageName in activeNotifications.keys,
                                    notificationCount = activeNotifications[drawerItem.appModel.packageName] ?: 0,
                                    showLabel = showLabel,
                                    sharedElementKeyPrefix = "drawer",
                                    isLongClickEnabled = false,
                                    onLongClick = null,
                                    refreshTrigger = refreshTrigger,
                                    getShortcuts = getShortcuts,
                                    onShortcutClick = onShortcutClick,
                                    onHideToggle = { onHideToggle(drawerItem.appModel.packageName, drawerItem.appModel.packageName in hiddenPackages) }
                                ) { options ->
                                    onAppClick(drawerItem.appModel.packageName, options)
                                }

                                if (showMenu) {
                                    AppContextMenu(
                                        expanded = true,
                                        onDismissRequest = { showMenu = false },
                                        packageName = drawerItem.appModel.packageName,
                                        label = drawerItem.appModel.label,
                                        shortcuts = shortcuts,
                                        onShortcutClick = onShortcutClick,
                                        onHideToggle = { onHideToggle(drawerItem.appModel.packageName, drawerItem.appModel.packageName in hiddenPackages) }
                                    )
                                }
                            }
                        }
                        is DrawerItem.Folder -> {
                            val isBeingDragged = dragInfo.isDragging && dragInfo.draggableFolderId == drawerItem.id
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (isBeingDragged) 0f else 1f
                                }
                            ) {
                                FolderItem(
                                    label = drawerItem.label,
                                    apps = drawerItem.apps,
                                    useMonochrome = useMonochrome,
                                    showLabel = showLabel,
                                    isHovered = isHoveredByDrag,
                                    onHapticFeedback = onHapticFeedback,
                                    onClick = { 
                                        if (dragInfo.draggableItem == null && dragInfo.draggableFolderId == -1) {
                                            onFolderClick(drawerItem.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SearchResults(
    filteredApps: List<AppModel>,
    webSuggestions: List<String>,
    searchProviderName: String,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAppClick: (String, android.os.Bundle?) -> Unit,
    modifier: Modifier = Modifier,
    recentlyUsedApps: List<AppModel> = emptyList(),
    useMonochrome: Boolean = false,
    iconPackPackageName: String? = null,
    hiddenPackages: Set<String> = emptySet(),
    activeNotifications: Map<String, Int> = emptyMap(),
    showNotificationDots: Boolean = true,
    showLabel: Boolean = true,
    refreshTrigger: Int = 0,
    getShortcuts: suspend (String) -> List<AppShortcut> = { emptyList() },
    onShortcutClick: (AppShortcut) -> Unit = {},
    onHideToggle: (String, Boolean) -> Unit = { _, _ -> },
    onWebSearch: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (recentlyUsedApps.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Used",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    recentlyUsedApps.forEach { app ->
                                Box(modifier = Modifier.weight(1f)) {
                            AppItem(
                                app = app,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                useMonochrome = useMonochrome,
                                iconPackPackageName = iconPackPackageName,
                                isHidden = app.packageName in hiddenPackages,
                                hasNotification = showNotificationDots && app.packageName in activeNotifications.keys,
                                notificationCount = activeNotifications[app.packageName] ?: 0,
                                showLabel = showLabel,
                                sharedElementKeyPrefix = "recent",
                                refreshTrigger = refreshTrigger,
                                getShortcuts = getShortcuts,
                                onShortcutClick = onShortcutClick,
                                onHideToggle = { onHideToggle(app.packageName, app.packageName in hiddenPackages) }
                            ) { options ->
                                onAppClick(app.packageName, options)
                            }
                        }
                    }
                    repeat(5 - recentlyUsedApps.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (filteredApps.isNotEmpty()) {
            item {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(filteredApps) { app ->
                SearchAppItem(
                    app = app,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    useMonochrome = useMonochrome,
                    iconPackPackageName = iconPackPackageName,
                    isHidden = app.packageName in hiddenPackages,
                    hasNotification = showNotificationDots && app.packageName in activeNotifications.keys,
                    notificationCount = activeNotifications[app.packageName] ?: 0,
                    showLabel = showLabel,
                    sharedElementKeyPrefix = "search",
                    refreshTrigger = refreshTrigger,
                    getShortcuts = getShortcuts,
                    onShortcutClick = onShortcutClick,
                    onHideToggle = { onHideToggle(app.packageName, app.packageName in hiddenPackages) },
                    onClick = { options -> onAppClick(app.packageName, options) }
                )
            }
        }

        if (webSuggestions.isNotEmpty()) {
            item {
                Text(
                    text = "$searchProviderName Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    webSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onWebSearch(suggestion) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = suggestion,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
