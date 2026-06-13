package com.samidevstudio.pxllauncherneo.data.repository

import com.samidevstudio.pxllauncherneo.data.local.dao.FolderDao
import com.samidevstudio.pxllauncherneo.data.local.dao.HomeAppDao
import com.samidevstudio.pxllauncherneo.data.local.dao.WidgetDao
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.FolderEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.HomeAppEntity
import com.samidevstudio.pxllauncherneo.data.local.entity.WidgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val homeAppDao: HomeAppDao,
    private val widgetDao: WidgetDao,
    private val folderDao: FolderDao,
    private val appRepositoryProvider: Provider<AppRepository>,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val appRepository get() = appRepositoryProvider.get()
    val allHomeApps: Flow<List<HomeAppEntity>> = homeAppDao.getAllHomeApps()
    val allWidgets: Flow<List<WidgetEntity>> = widgetDao.getAllWidgets()
    val allFolders = folderDao.getHomeScreenFoldersWithApps()
    val allDrawerFolders = folderDao.getAllFoldersWithApps()

    suspend fun addHomeApp(homeApp: HomeAppEntity) = withContext(Dispatchers.IO) {
        homeAppDao.insertHomeApp(homeApp)
    }

    suspend fun removeHomeApp(packageName: String) = withContext(Dispatchers.IO) {
        homeAppDao.deleteHomeAppByPackageName(packageName)
    }

    suspend fun removeHomeAppById(id: Int) = withContext(Dispatchers.IO) {
        homeAppDao.deleteHomeAppById(id)
    }

    suspend fun updateHomeAppPosition(id: Int, row: Float, col: Float) = withContext(Dispatchers.IO) {
        homeAppDao.updateHomeAppPosition(id, row, col)
    }

    suspend fun updateWidgetBounds(widgetId: Int, row: Float, col: Float, spanX: Float, spanY: Float) = withContext(Dispatchers.IO) {
        widgetDao.updateWidgetBounds(widgetId, row, col, spanX, spanY)
    }

    suspend fun createFolderFromApps(appA: HomeAppEntity, appB: HomeAppEntity, label: String = "Folder", category: String? = null): Int = withContext(Dispatchers.IO) {
        // 1. Create Folder
        val folderId = folderDao.insertFolder(FolderEntity(
            label = label,
            row = appB.row,
            column = appB.column,
            category = category
        )).toInt()

        // 2. Add apps to folder
        folderDao.insertFolderApp(FolderAppEntity(folderId, appA.packageName, 0))
        folderDao.insertFolderApp(FolderAppEntity(folderId, appB.packageName, 1))

        // 3. Remove apps from home screen
        homeAppDao.deleteHomeAppById(appA.id)
        homeAppDao.deleteHomeAppById(appB.id)

        folderId
    }

    suspend fun updateFolderPosition(folderId: Int, row: Float, col: Float) = withContext(Dispatchers.IO) {
        folderDao.updateFolderPosition(folderId, row, col)
    }

    suspend fun updateFolderLabel(folderId: Int, label: String) = withContext(Dispatchers.IO) {
        folderDao.updateFolderLabel(folderId, label)
    }

    suspend fun removeAppFromFolders(packageName: String) = withContext(Dispatchers.IO) {
        folderDao.removeAppFromAllFolders(packageName)
    }

    suspend fun addAppToFolder(folderId: Int, packageName: String) = withContext(Dispatchers.IO) {
        val nextOrder = (folderDao.getMaxDisplayOrder(folderId) ?: -1) + 1
        folderDao.insertFolderApp(FolderAppEntity(folderId, packageName, nextOrder))
    }

    suspend fun removeFolder(folderId: Int) = withContext(Dispatchers.IO) {
        folderDao.deleteFolderById(folderId)
        appRepository.refreshApps()
    }

    suspend fun removeAppFromFolder(folderId: Int, packageName: String, targetRow: Float, targetCol: Float) = withContext(Dispatchers.IO) {
        // 1. Remove from folder_apps
        folderDao.removeAppFromFolder(folderId, packageName)

        // 2. Add back to home_apps
        homeAppDao.insertHomeApp(HomeAppEntity(
            packageName = packageName,
            row = targetRow,
            column = targetCol
        ))

        // 3. Clean up folder if needed
        dissolveFolderIfNeeded(folderId)
    }

    suspend fun cleanupPackage(packageName: String) = withContext(Dispatchers.IO) {
        // 1. Find folders containing this app before removing
        val folderIds = folderDao.getFoldersContainingApp(packageName)
        
        // 2. Remove app from all folders
        folderDao.removeAppFromAllFolders(packageName)
        
        // 3. Remove from home screen
        homeAppDao.deleteHomeAppByPackageName(packageName)
        
        // 4. Dissolve folders if they now have < 2 apps
        folderIds.forEach { dissolveFolderIfNeeded(it) }
    }

    suspend fun dissolveFolderIfNeeded(folderId: Int) = withContext(Dispatchers.IO) {
        val folderWithApps = folderDao.getFolderWithAppsById(folderId) ?: return@withContext

        if (folderWithApps.apps.isEmpty()) {
            folderDao.deleteFolderById(folderId)
        } else if (folderWithApps.apps.size == 1) {
            val lastApp = folderWithApps.apps.first()
            val folder = folderWithApps.folder

            // Move last app to home at folder's position
            homeAppDao.insertHomeApp(HomeAppEntity(
                packageName = lastApp.packageName,
                row = folder.row,
                column = folder.column
            ))

            // Delete folder (cascades to folder_apps)
            folderDao.deleteFolderById(folderId)
        }
    }

    suspend fun resetHome() = withContext(Dispatchers.IO) {
        homeAppDao.deleteAllHomeApps()
        widgetDao.deleteAllWidgets()
        folderDao.deleteHomeScreenFolders()
        userPreferencesRepository.setFirstInstallRun(true)
        appRepository.refreshApps()
    }

    suspend fun deleteHomeFolders() = withContext(Dispatchers.IO) {
        folderDao.deleteHomeScreenFolders()
        appRepository.refreshApps()
    }
}
