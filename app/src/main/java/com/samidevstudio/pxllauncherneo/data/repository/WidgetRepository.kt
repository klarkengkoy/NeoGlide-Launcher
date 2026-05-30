package com.samidevstudio.pxllauncherneo.data.repository

import android.appwidget.AppWidgetHost
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao,
    val appWidgetHost: AppWidgetHost
) {
    val allWidgets: Flow<List<WidgetEntity>> = widgetDao.getAllWidgets()

    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    suspend fun addWidget(widget: WidgetEntity) = withContext(Dispatchers.IO) {
        widgetDao.insertWidget(widget)
    }

    suspend fun removeWidget(widgetId: Int) = withContext(Dispatchers.IO) {
        widgetDao.deleteWidgetById(widgetId)
        appWidgetHost.deleteAppWidgetId(widgetId)
    }

    suspend fun updateWidgetBounds(widgetId: Int, row: Float, column: Float, spanX: Float, spanY: Float) = withContext(Dispatchers.IO) {
        widgetDao.updateWidgetBounds(widgetId, row, column, spanX, spanY)
    }

    fun startListening() {
        appWidgetHost.startListening()
    }

    fun stopListening() {
        appWidgetHost.stopListening()
    }
}
