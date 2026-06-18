package com.samidevstudio.neoglide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.samidevstudio.neoglide.data.local.dao.AppDao
import com.samidevstudio.neoglide.data.local.dao.CategoryDao
import com.samidevstudio.neoglide.data.local.dao.FolderDao
import com.samidevstudio.neoglide.data.local.dao.HomeAppDao
import com.samidevstudio.neoglide.data.local.dao.WidgetDao
import com.samidevstudio.neoglide.data.local.entity.AppEntity
import com.samidevstudio.neoglide.data.local.entity.CategoryEntity
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
        CategoryEntity::class,
    ],
    version = 1,
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
