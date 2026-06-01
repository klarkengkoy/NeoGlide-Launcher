package com.samidevstudio.pxllauncherneo.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.samidevstudio.pxllauncherneo.data.local.dao.AppDao
import com.samidevstudio.pxllauncherneo.data.local.entity.AppEntity
import com.samidevstudio.pxllauncherneo.domain.model.AppCategory
import com.samidevstudio.pxllauncherneo.domain.model.AppModel
import com.samidevstudio.pxllauncherneo.domain.model.AppShortcut
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDao: AppDao,
) {
    private val packageManager: PackageManager = context.packageManager

    val allApps: Flow<List<AppModel>> = appDao.getAllApps().map { entities ->
        entities.map { entity ->
            entity.toDomainModel()
        }
    }

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent, 
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }
        
        val myPackageName = context.packageName
        val existingApps = appDao.getAllAppsList().associateBy { it.packageName }
        
        val appEntities = resolveInfos
            .map { it.activityInfo }
            .filter { it.packageName != myPackageName }
            .map { activityInfo ->
                val existing = existingApps[activityInfo.packageName]
                createAppEntity(
                    app = activityInfo.applicationInfo,
                    existingLastUsedTime = existing?.lastUsedTime ?: 0L,
                    existingCategory = existing?.category
                )
            }
            .distinctBy { it.packageName }
            
        // Use a more surgical approach: delete only what's gone, insert the rest
        val newPackageNames = appEntities.map { it.packageName }.toSet()
        existingApps.keys.forEach { pkg ->
            if (pkg !in newPackageNames) {
                appDao.deleteAppByPackageName(pkg)
            }
        }
        appDao.insertApps(appEntities)
    }

    suspend fun updatePackage(packageName: String) = withContext(Dispatchers.IO) {
        if (packageName == context.packageName) return@withContext
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                val existing = appDao.getAppByPackageName(packageName)
                appDao.insertApp(createAppEntity(
                    app = appInfo,
                    existingLastUsedTime = existing?.lastUsedTime ?: 0L,
                    existingCategory = existing?.category
                ))
            } else {
                appDao.deleteAppByPackageName(packageName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            appDao.deleteAppByPackageName(packageName)
        }
    }

    suspend fun removePackage(packageName: String) = withContext(Dispatchers.IO) {
        appDao.deleteAppByPackageName(packageName)
    }

    suspend fun updateAppCategory(packageName: String, category: AppCategory) = withContext(Dispatchers.IO) {
        appDao.updateAppCategory(packageName, category.name)
    }

    private fun createAppEntity(
        app: ApplicationInfo, 
        existingLastUsedTime: Long = 0L,
        existingCategory: String? = null
    ): AppEntity {
        val installTime = try {
            packageManager.getPackageInfo(app.packageName, 0).firstInstallTime
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        return AppEntity(
            packageName = app.packageName,
            label = packageManager.getApplicationLabel(app).toString(),
            category = existingCategory ?: categorizeApp(app).name,
            installTime = installTime,
            lastUsedTime = existingLastUsedTime
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

    suspend fun getDefaultDockApps(): List<String> = withContext(Dispatchers.IO) {
        val dockPackages = mutableListOf<String>()

        // 1. Browser
        val browserIntent = Intent(Intent.ACTION_VIEW, "https://www.google.com".toUri())
        findDefaultPackage(browserIntent)?.let { if (it !in dockPackages) dockPackages.add(it) }

        // 2. Phone
        val phoneIntent = Intent(Intent.ACTION_DIAL)
        findDefaultPackage(phoneIntent)?.let { if (it !in dockPackages) dockPackages.add(it) }

        // 3. Messages
        val messagesIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) }
        findDefaultPackage(messagesIntent)?.let { if (it !in dockPackages) dockPackages.add(it) }

        // 4. Camera
        val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        findDefaultPackage(cameraIntent)?.let { if (it !in dockPackages) dockPackages.add(it) }

        // Fallbacks if we still have less than 4 apps (rare on Pixel)
        if (dockPackages.size < 4) {
            val fallbacks = listOf(
                "com.android.chrome",
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.google.android.GoogleCamera",
                "com.brave.browser"
            )
            for (pkg in fallbacks) {
                if (dockPackages.size >= 4) break
                if (pkg !in dockPackages && packageManager.getLaunchIntentForPackage(pkg) != null) {
                    dockPackages.add(pkg)
                }
            }
        }

        dockPackages.take(4)
    }

    private fun findDefaultPackage(intent: Intent): String? {
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = resolveInfo?.activityInfo?.packageName
        return if (pkg != null && pkg != "android" && packageManager.getLaunchIntentForPackage(pkg) != null) {
            pkg
        } else {
            // If resolveActivity fails, try querying all activities and pick the first non-system one
            val resolved = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolved.firstOrNull { it.activityInfo.packageName != "android" && packageManager.getLaunchIntentForPackage(it.activityInfo.packageName) != null }
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

    private fun categorizeApp(app: ApplicationInfo): AppCategory {
        val knownCategories = mapOf(
            "com.whatsapp" to AppCategory.COMMUNICATION,
            "com.facebook.orca" to AppCategory.COMMUNICATION,
            "com.telegram.messenger" to AppCategory.COMMUNICATION,
            "com.google.android.apps.messaging" to AppCategory.COMMUNICATION,
            "com.google.android.dialer" to AppCategory.COMMUNICATION,
            "com.google.android.apps.photos" to AppCategory.MEDIA,
            "com.instagram.android" to AppCategory.SOCIAL,
            "com.facebook.katana" to AppCategory.SOCIAL,
            "com.twitter.android" to AppCategory.SOCIAL,
            "com.zhiliaoapp.musically" to AppCategory.SOCIAL,
            "com.google.android.youtube" to AppCategory.MEDIA,
            "com.spotify.music" to AppCategory.MEDIA,
            "com.netflix.mediaclient" to AppCategory.MEDIA,
            "com.amazon.mShop.android.shopping" to AppCategory.SHOPPING,
            "com.ebay.mobile" to AppCategory.SHOPPING,
            "com.google.android.apps.maps" to AppCategory.UTILITIES,
            "com.google.android.gm" to AppCategory.COMMUNICATION,
            "com.android.chrome" to AppCategory.UTILITIES,
            "com.google.android.calendar" to AppCategory.UTILITIES,
            "com.google.android.apps.docs" to AppCategory.UTILITIES,
            "com.google.android.calculator" to AppCategory.UTILITIES,
            "com.google.android.deskclock" to AppCategory.UTILITIES,
            "com.android.settings" to AppCategory.SYSTEM,
            "com.google.android.apps.nbu.files" to AppCategory.UTILITIES,
            "com.google.android.apps.walletnfcrel" to AppCategory.UTILITIES
        )

        knownCategories[app.packageName]?.let { return it }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (app.category) {
                ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
                ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_MAPS -> AppCategory.UTILITIES
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.UTILITIES
                else -> AppCategory.OTHER
            }
        } else {
            AppCategory.OTHER
        }
    }

    private fun AppEntity.toDomainModel(): AppModel {
        return AppModel(
            packageName = packageName,
            label = label,
            category = AppCategory.fromString(category),
            isFavorite = isFavorite,
            lastUsedTime = lastUsedTime
        )
    }
}
