package com.samidevstudio.pxllauncherneo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samidevstudio.pxllauncherneo.data.local.dao.AppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.HomeAppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.AppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity

@Database(entities = [AppEntity::class, WidgetEntity::class, HomeAppEntity::class], version = 5, exportSchema = false)
abstract class PxlDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun widgetDao(): WidgetDao
    abstract fun homeAppDao(): HomeAppDao

    companion object {
        const val DATABASE_NAME = "pxl_launcher_db"
    }
}
