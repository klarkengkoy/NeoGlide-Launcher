package com.samidevstudio.neoglide.di

import android.content.Context
import androidx.room.Room
import com.samidevstudio.neoglide.data.local.NeoGlideDatabase
import com.samidevstudio.neoglide.data.local.dao.AppDao
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
    fun provideDatabase(@ApplicationContext context: Context): NeoGlideDatabase {
        return Room.databaseBuilder(
            context,
            NeoGlideDatabase::class.java,
            NeoGlideDatabase.DATABASE_NAME
        )
            .addMigrations(NeoGlideDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideAppDao(database: NeoGlideDatabase): AppDao {
        return database.appDao()
    }

    @Provides
    fun provideWidgetDao(database: NeoGlideDatabase): com.samidevstudio.neoglide.data.local.dao.WidgetDao {
        return database.widgetDao()
    }

    @Provides
    fun provideHomeAppDao(database: NeoGlideDatabase): com.samidevstudio.neoglide.data.local.dao.HomeAppDao {
        return database.homeAppDao()
    }

    @Provides
    fun provideFolderDao(database: NeoGlideDatabase): com.samidevstudio.neoglide.data.local.dao.FolderDao {
        return database.folderDao()
    }
}
