package com.samidevstudio.neoglide.di

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {
    private const val APP_WIDGET_HOST_ID = 1024

    @Provides
    @Singleton
    fun provideAppWidgetManager(@ApplicationContext context: Context): AppWidgetManager {
        return AppWidgetManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAppWidgetHost(@ApplicationContext context: Context): AppWidgetHost {
        return AppWidgetHost(context, APP_WIDGET_HOST_ID)
    }
}
