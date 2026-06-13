package com.samidevstudio.pxllauncherneo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samidevstudio.pxllauncherneo.data.local.dao.AppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.FolderDao
import com.samidevstudio.pxllauncherneo.data.local.dao.HomeAppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.AppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity

@Database(
    entities = [
        AppEntity::class,
        WidgetEntity::class,
        HomeAppEntity::class,
        FolderEntity::class,
        FolderAppEntity::class,
    ],
    version = 7,
    exportSchema = false
)
abstract class PxlDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun widgetDao(): WidgetDao
    abstract fun homeAppDao(): HomeAppDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "pxl_launcher_db"

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN category TEXT DEFAULT NULL")
            }
        }
    }
}
