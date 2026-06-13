package com.samidevstudio.pxllauncherneo.di

import android.content.Context
import androidx.room.Room
import com.samidevstudio.pxllauncherneo.data.local.PxlDatabase
import com.samidevstudio.pxllauncherneo.data.local.dao.AppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PxlDatabase {
        return Room.databaseBuilder(
            context,
            PxlDatabase::class.java,
            PxlDatabase.DATABASE_NAME
        )
            .addMigrations(PxlDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideAppDao(database: PxlDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    fun provideWidgetDao(database: PxlDatabase): com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao {
        return database.widgetDao()
    }

    @Provides
    fun provideHomeAppDao(database: PxlDatabase): com.samidevstudio.pxllauncherneo.data.local.dao.HomeAppDao {
        return database.homeAppDao()
    }

    @Provides
    fun provideFolderDao(database: PxlDatabase): com.samidevstudio.pxllauncherneo.data.local.dao.FolderDao {
        return database.folderDao()
    }
}
