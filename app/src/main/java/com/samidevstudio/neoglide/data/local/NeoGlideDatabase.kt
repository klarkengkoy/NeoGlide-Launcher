package com.samidevstudio.neoglide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.samidevstudio.neoglide.data.local.dao.AppDao
import com.samidevstudio.neoglide.data.local.dao.FolderDao
import com.samidevstudio.neoglide.data.local.dao.HomeAppDao
import com.samidevstudio.neoglide.data.local.dao.WidgetDao
import com.samidevstudio.neoglide.data.local.entity.AppEntity
import com.samidevstudio.neoglide.data.local.entity.FolderAppEntity
import com.samidevstudio.neoglide.data.local.entity.FolderEntity
import com.samidevstudio.neoglide.data.local.entity.HomeAppEntity
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity

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
abstract class NeoGlideDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun widgetDao(): WidgetDao
    abstract fun homeAppDao(): HomeAppDao
    abstract fun folderDao(): FolderDao

    companion object {
        const val DATABASE_NAME = "neoglide_launcher_db"

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN category TEXT DEFAULT NULL")
            }
        }
    }
}
