package com.samidevstudio.neoglide.data.repository

import android.appwidget.AppWidgetHost
import com.samidevstudio.neoglide.data.local.dao.WidgetDao
import com.samidevstudio.neoglide.data.local.entity.WidgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao,
    val appWidgetHost: AppWidgetHost,
) {

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
