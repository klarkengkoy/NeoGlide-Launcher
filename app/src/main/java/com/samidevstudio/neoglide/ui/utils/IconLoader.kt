package com.samidevstudio.neoglide.ui.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val iconCache: IconCache
) {
    private val TAG = "IconLoader"
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val pm = context.packageManager

    suspend fun loadIcon(
        packageName: String,
        useMonochrome: Boolean,
        iconPackPackageName: String? = null,
        refreshTrigger: Int = 0
    ): Pair<Drawable?, Boolean> = withContext(Dispatchers.IO) {
        val cacheKey = "$packageName-$useMonochrome-$iconPackPackageName"
        
        iconCache.withLoadLock(cacheKey) {
            // Check cache again inside lock
            val cached = iconCache.get(cacheKey)
            if (cached != null && !iconCache.isDynamic(packageName)) {
                return@withLoadLock cached
            }

            try {
                val userHandle = Process.myUserHandle()
                val activities = launcherApps.getActivityList(packageName, userHandle)
                
                if (activities.isNotEmpty()) {
                    val info = activities[0]
                    var icon = info.getIcon(0)

                    // Metadata keys for dynamic icons
                    val CALENDAR_METADATA = "com.google.android.calendar.dynamic_icons"
                    val CALENDAR_LEVEL_METADATA = "com.android.launcher3.LEVEL_PER_DAY"
                    val CLOCK_METADATA = "com.google.android.apps.deskclock.dynamic_icons"
                    
                    val activityInfo = pm.getActivityInfo(info.componentName, PackageManager.GET_META_DATA)
                    val metaData = activityInfo.metaData

                    var isDynamic = false

                    if (!useMonochrome && metaData != null) {
                        if (metaData.containsKey(CALENDAR_METADATA)) {
                            isDynamic = true
                            val arrayResId = metaData.getInt(CALENDAR_METADATA)
                            if (arrayResId != 0) {
                                try {
                                    val resources = pm.getResourcesForApplication(packageName)
                                    val dayArray = resources.obtainTypedArray(arrayResId)
                                    val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                    val iconResId = dayArray.getResourceId(day - 1, 0)
                                    dayArray.recycle()
                                    if (iconResId != 0) {
                                        @Suppress("DEPRECATION")
                                        icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            resources.getDrawable(iconResId, null)
                                        } else {
                                            resources.getDrawable(iconResId)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error loading calendar icon for $packageName", e)
                                }
                            }
                        } else if (metaData.containsKey(CALENDAR_LEVEL_METADATA)) {
                            isDynamic = true
                            val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                            icon?.level = day
                        } else if (metaData.containsKey(CLOCK_METADATA)) {
                            isDynamic = true
                        }
                    }

                    val finalIcon: Drawable?
                    val isMonochromeResult: Boolean

                    if (useMonochrome && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) && 
                        (icon is AdaptiveIconDrawable) && (icon.monochrome != null)) {
                        finalIcon = icon.monochrome
                        isMonochromeResult = true
                    } else {
                        finalIcon = icon
                        isMonochromeResult = false
                    }

                    val result = finalIcon to isMonochromeResult
                    
                    iconCache.setDynamic(packageName, isDynamic)
                    if (!isDynamic) {
                        iconCache.put(cacheKey, result)
                    }
                    
                    result
                } else {
                    null to false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading icon for $packageName", e)
                null to false
            }
        }
    }
}
