package com.samidevstudio.neoglide.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import com.samidevstudio.neoglide.data.local.dao.AppDao
import com.samidevstudio.neoglide.data.local.dao.FolderDao
import com.samidevstudio.neoglide.data.local.entity.AppEntity
import com.samidevstudio.neoglide.domain.classifier.AppCategoryClassifier
import com.samidevstudio.neoglide.domain.model.AppCategory
import com.samidevstudio.neoglide.domain.model.AppModel
import com.samidevstudio.neoglide.domain.model.AppShortcut
import com.samidevstudio.neoglide.ui.utils.IconLoader
import com.samidevstudio.neoglide.ui.utils.PaletteUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val folderDao: FolderDao,
    private val homeRepository: HomeRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val packageManager: PackageManager = context.packageManager
    private val launcherApps: LauncherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private var warmUpJob: Job? = null
    
    private val _isDatabaseReady = MutableStateFlow(value = false)
    val isDatabaseReady: StateFlow<Boolean> = _isDatabaseReady.asStateFlow()

    fun warmUpIcons(iconLoader: IconLoader, scope: CoroutineScope) {
        warmUpJob?.cancel()
        warmUpJob = scope.launch(Dispatchers.Default) {
            Log.d("NeoGlideInit", "Step 5: Starting icon warm-up sequence...")
            // Wait for the first database refresh to complete if it hasn't already
            if (!_isDatabaseReady.value) {
                _isDatabaseReady.first { it }
            }

            val apps = appDao.getAllAppsList()
            
            if (apps.isEmpty()) {
                return@launch
            }
            
            // Priority 1: First 12 apps (likely what's visible on screen)
            apps.take(12).forEach { app ->
                if (!isActive) {
                    return@launch
                }
                iconLoader.loadIcon(app.packageName, useMonochrome = false)
                delay(20.milliseconds) 
            }
            yield()

            // Priority 2: Next 36 apps (immediate glide range)
            val priority2Apps = apps.asSequence().drop(12).take(36).toList()
            priority2Apps.chunked(4).forEach { chunk ->
                if (!isActive) {
                    return@launch
                }
                chunk.forEach { app ->
                    launch { 
                        iconLoader.loadIcon(app.packageName, useMonochrome = false)
                    }
                }
                delay(150.milliseconds) 
            }
            
            // Critical warm-up (what user sees immediately) is done
            Log.d("NeoGlideInit", "Step 5: Critical warm-up complete (First 12 icons).")
            yield()

            // Priority 3: The rest (deeper storage)
            if (apps.size > 48) {
                apps.asSequence().drop(48).chunked(10).forEach { chunk ->
                    if (!isActive) {
                        return@launch
                    }
                    chunk.forEach { app ->
                        launch { 
                            iconLoader.loadIcon(app.packageName, useMonochrome = false)
                        }
                    }
                    delay(400.milliseconds) 
                }
            }
            Log.d("NeoGlideInit", "Step 5: Full icon warm-up finished (Processed ${apps.size} apps).")
        }
    }

    fun stopWarmUp() {
        warmUpJob?.cancel()
        warmUpJob = null
    }

    val allApps: Flow<List<AppModel>> = appDao.getAllApps().map { entities ->
        entities.map { entity ->
            entity.toDomainModel()
        }
    }

    suspend fun refreshApps(
        forceRecategorize: Boolean = false,
        forceRecalculateColors: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        Log.d("NeoGlideInit", "Step 3: Starting app database refresh...")
        val userHandle = android.os.Process.myUserHandle()
        val activityList = launcherApps.getActivityList(null, userHandle)
        
        val myPackageName = context.packageName
        val existingApps = appDao.getAllAppsList().associateBy { it.packageName }
        
        val prefsFlow = userPreferencesRepository.userPreferencesFlow.first()
        val classifier = AppCategoryClassifier(prefsFlow.orderedCategories.toSet())

        val appEntities = activityList
            .asSequence()
            .filter { it.applicationInfo.packageName != myPackageName }
            .map { info ->
                val packageName = info.applicationInfo.packageName
                val existing = existingApps[packageName]
                createAppEntity(
                    app = info.applicationInfo,
                    classifier = classifier,
                    existingLastUsedTime = existing?.lastUsedTime ?: 0L,
                    existingCategory = if (forceRecategorize) null else existing?.category,
                    existingHue = if (forceRecalculateColors) null else existing?.dominantHue,
                )
            }
            .distinctBy { it.packageName }
            .toList()
            
        // Use a more surgical approach: delete only what's gone, insert the rest
        val newPackageNames = appEntities.asSequence().map { it.packageName }.toSet()
        existingApps.keys.forEach { pkg ->
            if (pkg !in newPackageNames) {
                appDao.deleteAppByPackageName(pkg)
                homeRepository.cleanupPackage(pkg)
            }
        }
        appDao.insertApps(appEntities)
        _isDatabaseReady.value = true
        Log.d("NeoGlideInit", "Step 3: App database refresh complete (Total: ${appEntities.size} apps).")
    }

    suspend fun updatePackage(packageName: String) = withContext(Dispatchers.IO) {
        if (packageName == context.packageName) return@withContext
        try {
            val userHandle = android.os.Process.myUserHandle()
            val activities = launcherApps.getActivityList(packageName, userHandle)
            
            if (activities.isNotEmpty()) {
                val info = activities[0]
                val existing = appDao.getAppByPackageName(packageName)
                val prefsFlow = userPreferencesRepository.userPreferencesFlow.first()
                val classifier = AppCategoryClassifier(prefsFlow.orderedCategories.toSet())
                appDao.insertApp(
                    createAppEntity(
                        app = info.applicationInfo,
                        classifier = classifier,
                        existingLastUsedTime = existing?.lastUsedTime ?: 0L,
                        existingCategory = existing?.category,
                        existingHue = existing?.dominantHue
                    )
                )
            } else {
                appDao.deleteAppByPackageName(packageName)
                homeRepository.cleanupPackage(packageName)
            }
        } catch (_: Exception) {
            appDao.deleteAppByPackageName(packageName)
            homeRepository.cleanupPackage(packageName)
        }
    }

    suspend fun removePackage(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteAppByPackageName(packageName)
        homeRepository.cleanupPackage(packageName)
    }

    suspend fun updateAppCategory(packageName: String, category: AppCategory) = withContext(Dispatchers.IO) {
        appDao.updateAppCategory(packageName, category.name)
    }

    suspend fun updateAllAppsInCategory(oldCategory: String, newCategory: String) = withContext(Dispatchers.IO) {
        appDao.updateAllAppsInCategory(oldCategory, newCategory)
    }

    suspend fun markAppAsInFolder(packageName: String) = withContext(Dispatchers.IO) {
        appDao.markAppAsInFolder(packageName)
    }

    private fun createAppEntity(
        app: ApplicationInfo, 
        classifier: AppCategoryClassifier,
        existingLastUsedTime: Long = 0L,
        existingCategory: String? = null,
        existingHue: Float? = null
    ): AppEntity {
        val installTime = try {
            packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        
        val hue = existingHue ?: try {
            val icon = packageManager.getApplicationIcon(app)
            PaletteUtils.extractDominantHue(icon)
        } catch (_: Exception) {
            0f
        }

        return AppEntity(
            packageName = app.packageName,
            label = packageManager.getApplicationLabel(app).toString(),
            category = existingCategory ?: classifier.classify(app.packageName, context).name,
            installTime = installTime,
            lastUsedTime = existingLastUsedTime,
            dominantHue = hue,
        )
    }

    fun launchApp(packageName: String, options: android.os.Bundle? = null) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent, options)
            updateLastUsedTime(packageName)
        }
    }

    private fun updateLastUsedTime(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            appDao.updateLastUsedTime(packageName, System.currentTimeMillis())
        }
    }

    suspend fun getShortcuts(packageName: String): List<AppShortcut> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return@withContext emptyList()
        
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        }

        try {
            val shortcuts = launcherApps.getShortcuts(query, android.os.Process.myUserHandle()) ?: emptyList()
            shortcuts.map { shortcut ->
                val icon = try {
                    launcherApps.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi)
                } catch (_: Exception) {
                    null
                }
                AppShortcut(
                    id = shortcut.id,
                    label = shortcut.shortLabel?.toString() ?: shortcut.longLabel?.toString() ?: "",
                    packageName = packageName,
                    icon = icon
                )
            }.take(5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun launchShortcut(packageName: String, shortcutId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        try {
            launcherApps.startShortcut(packageName, shortcutId, null, null, android.os.Process.myUserHandle())
            updateLastUsedTime(packageName)
        } catch (_: Exception) {
            launchApp(packageName)
        }
    }





    suspend fun getCoreAppsForProvisioning(): List<String> = withContext(Dispatchers.IO) {
        val packages = mutableListOf<String>()

        // 1. Browser
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())
        findDefaultPackage(browserIntent)?.let { if (it !in packages) packages.add(it) }

        // 2. Phone
        val phoneIntent = Intent(Intent.ACTION_DIAL)
        findDefaultPackage(phoneIntent)?.let { if (it !in packages) packages.add(it) }

        // 3. Messages
        val messagesIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
        findDefaultPackage(messagesIntent)?.let { if (it !in packages) packages.add(it) }

        // 4. Camera
        val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        findDefaultPackage(cameraIntent)?.let { if (it !in packages) packages.add(it) }

        // 5. AI Slot Logic
        val googleApp = "com.google.android.googlequicksearchbox"
        val geminiApp = "com.google.android.apps.bard"
        
        val defaultAssistant = findDefaultPackage(Intent(Intent.ACTION_ASSIST))
        
        val aiPackage = when {
            // Case 1: Default is Google App -> Try upgrade to Gemini
            defaultAssistant == googleApp -> {
                if (packageManager.getLaunchIntentForPackage(geminiApp) != null) geminiApp else googleApp
            }
            // Case 2: Default is something else (Alexa, ChatGPT set as default, etc.)
            defaultAssistant != null -> defaultAssistant
            // Case 3: No default set -> Look for Gemini, then Google
            else -> {
                if (packageManager.getLaunchIntentForPackage(geminiApp) != null) {
                    geminiApp
                } else if (packageManager.getLaunchIntentForPackage(googleApp) != null) {
                    googleApp
                } else {
                    null
                }
            }
        }

        aiPackage?.let { if (it !in packages) packages.add(it) }

        // Final sanity check for core 4 apps if any were missed by intents
        if (packages.size < 4) {
            val coreFallbacks = listOf(
                "com.android.chrome",
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera"
            )
            for (pkg in coreFallbacks) {
                if (packages.size >= 4) break
                if ((pkg !in packages) && (packageManager.getLaunchIntentForPackage(pkg) != null)) {
                    packages.add(pkg)
                }
            }
        }
        
        packages
    }

    private fun findDefaultPackage(intent: Intent): String? {
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = resolveInfo?.activityInfo?.packageName
        return if ((pkg != null) && (pkg != "android") && (packageManager.getLaunchIntentForPackage(pkg) != null)) {
            pkg
        } else {
            val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolved.firstOrNull { (it.activityInfo.packageName != "android") && (packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) != null) }
                ?.activityInfo?.packageName
        }
    }

    suspend fun isDefaultLauncher(): Boolean = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun openDefaultLauncherSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                try {
                    val lastIntent = Intent(Settings.ACTION_SETTINGS)
                    lastIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(lastIntent)
                } catch (_: Exception) {
                    android.widget.Toast.makeText(context, "Could not open settings. Please set as default manually.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }



    private fun AppEntity.toDomainModel(): AppModel {
        return AppModel(
            packageName = packageName,
            label = label,
            category = AppCategory.fromString(category),
            isFavorite = isFavorite,
            lastUsedTime = lastUsedTime,
            installTime = installTime,
            dominantHue = dominantHue ?: 0f
        )
    }

    suspend fun resetDrawer() = withContext(Dispatchers.IO) {
        folderDao.deleteAppDrawerFolders()
        refreshApps(forceRecategorize = true)
    }

    suspend fun reclassifyAll(): Map<AppCategory, List<String>> = withContext(Dispatchers.IO) {
        val apps = appDao.getAllAppsList()
        val customCategoriesMap = categoryRepository.getCustomCategoriesMap()
        val customCategoryNames = customCategoriesMap.keys.map { it.name }.toSet()
        val prefsFlow = userPreferencesRepository.userPreferencesFlow.first()
        val enabledCategories = prefsFlow.orderedCategories.toSet()
        val classifier = AppCategoryClassifier(enabledCategories)
        
        val movements = mutableMapOf<AppCategory, MutableList<String>>()
        val categoryCounts = apps.groupingBy { it.category }.eachCount().toMutableMap()
        
        for (app in apps) {
            val currentCategoryName = app.category
            
            // Manual/Custom categories never steal and are never stolen from automatically
            if (currentCategoryName in customCategoryNames) continue

            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                } else {
                    packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                }
            } catch (_: Exception) { null } ?: continue

            val permissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val appInfoCategory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                packageInfo.applicationInfo?.category ?: -1
            } else -1
            
            val detailed = classifier.classifyDetailed(app.packageName, app.label, appInfoCategory, permissions)
            
            val targetCategory = detailed.natural
                ?: if ((detailed.heuristic != null) && (detailed.heuristic.name in enabledCategories)) {
                    detailed.heuristic
                } else {
                    AppCategory.OTHER
                }

            if (targetCategory.name != currentCategoryName) {
                val sourceCount = categoryCounts[currentCategoryName] ?: 0
                
                // Stealing rule: Keep at least 1 app in the source category if it's a heuristic steal.
                // Natural (Manifest) matches always move to their rightful place.
                val isHeuristicSteal = detailed.natural == null && detailed.heuristic != null
                val canSteal = if (isHeuristicSteal) sourceCount > 1 else true

                if (canSteal) {
                    appDao.updateAppCategory(app.packageName, targetCategory.name)
                    movements.getOrPut(targetCategory) { mutableListOf() }.add(app.label)
                    
                    categoryCounts[currentCategoryName] = sourceCount - 1
                    categoryCounts[targetCategory.name] = (categoryCounts[targetCategory.name] ?: 0) + 1
                }
            }
        }
        movements
    }

    suspend fun dissolveDrawerFolders() = withContext(Dispatchers.IO) {
        val drawerFolders = folderDao.getAppDrawerFoldersWithAppsList()
        drawerFolders.forEach { folderWithApps ->
            homeRepository.dissolveFolder(folderWithApps.folder.id)
        }
    }
}
