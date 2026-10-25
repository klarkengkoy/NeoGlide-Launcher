package com.smyddevstudio.neoglidelauncher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smyddevstudio.neoglidelauncher.data.local.dao.AppDao
import com.smyddevstudio.neoglidelauncher.data.local.dao.CategoryDao
import com.smyddevstudio.neoglidelauncher.data.local.dao.FolderDao
import com.smyddevstudio.neoglidelauncher.data.local.dao.HomeAppDao
import com.smyddevstudio.neoglidelauncher.data.local.dao.WidgetDao
import com.smyddevstudio.neoglidelauncher.data.local.entity.AppEntity
import com.smyddevstudio.neoglidelauncher.data.local.entity.CategoryEntity
import com.smyddevstudio.neoglidelauncher.data.local.entity.FolderAppEntity
import com.smyddevstudio.neoglidelauncher.data.local.entity.FolderEntity
import com.smyddevstudio.neoglidelauncher.data.local.entity.HomeAppEntity
import com.smyddevstudio.neoglidelauncher.data.local.entity.WidgetEntity

@Database(
    entities = [
        AppEntity::class,
        WidgetEntity::class,
        HomeAppEntity::class,
        FolderEntity::class,
        FolderAppEntity::class,
        CategoryEntity::class,
    ],
    version = 4,
    exportSchema = false
)
abstract class NeoGlideDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun widgetDao(): WidgetDao
    abstract fun homeAppDao(): HomeAppDao
    abstract fun folderDao(): FolderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "neoglide_launcher_db"
    }
}
