package com.samidevstudio.pxllauncherneo.data.repository

import com.samidevstudio.pxllauncherneo.data.local.dao.HomeAppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val homeAppDao: HomeAppDao,
    private val widgetDao: WidgetDao
) {
    val allHomeApps: Flow<List<HomeAppEntity>> = homeAppDao.getAllHomeApps()
    val allWidgets: Flow<List<WidgetEntity>> = widgetDao.getAllWidgets()

    suspend fun addHomeApp(homeApp: HomeAppEntity) = withContext(Dispatchers.IO) {
        homeAppDao.insertHomeApp(homeApp)
    }

    suspend fun removeHomeApp(packageName: String) = withContext(Dispatchers.IO) {
        homeAppDao.deleteHomeAppByPackageName(packageName)
    }

    suspend fun updateHomeAppPosition(id: Int, row: Float, col: Float) = withContext(Dispatchers.IO) {
        homeAppDao.updateHomeAppPosition(id, row, col)
    }

    suspend fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float) = withContext(Dispatchers.IO) {
        widgetDao.updateWidgetBounds(widgetId, row, col, spanX, spanY)
    }
}
