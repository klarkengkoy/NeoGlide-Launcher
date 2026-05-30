package com.samidevstudio.pxllauncherneo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samidevstudio.pxllauncherneo.data.local.dao.AppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.AppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity

@Database(entities = [AppEntity::class, WidgetEntity::class], version = 4, exportSchema = false)
abstract class PxlDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        const val DATABASE_NAME = "pxl_launcher_db"
    }
}
