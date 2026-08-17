package com.samidevstudio.neoglide.ui.utils.icons

import android.graphics.drawable.Drawable
import android.util.LruCache
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IconCache @Inject constructor() {
    // Stores Pair<Icon, isMonochrome> - Balanced for memory efficiency (approx 200 icons)
    private val memoryCache = object : LruCache<String, Pair<Drawable?, Boolean>>(200) {
        override fun sizeOf(key: String, value: Pair<Drawable?, Boolean>): Int {
            return 1 
        }
    }

    // Tracks if a package has dynamic metadata (Calendar/Clock)
    private val dynamicStatus = ConcurrentHashMap<String, Boolean>()
    
    // Prevents concurrent redundant loads
    private val loadingMutexes = ConcurrentHashMap<String, Mutex>()
    private val globalMutex = Mutex()

    fun get(key: String): Pair<Drawable?, Boolean>? = memoryCache.get(key)

    fun put(key: String, value: Pair<Drawable?, Boolean>) {
        memoryCache.put(key, value)
    }

    fun isDynamic(packageName: String): Boolean = dynamicStatus[packageName] ?: false

    fun setDynamic(packageName: String, dynamic: Boolean) {
        dynamicStatus[packageName] = dynamic
    }

    suspend fun <T> withLoadLock(key: String, block: suspend () -> T): T {
        val mutex = globalMutex.withLock {
            loadingMutexes.getOrPut(key) { Mutex() }
        }
        return mutex.withLock {
            try {
                block()
            } finally {
                globalMutex.withLock {
                    // Only remove if it's the same mutex to avoid race conditions
                    if (loadingMutexes[key] === mutex) {
                        loadingMutexes.remove(key)
                    }
                }
            }
        }
    }
    
    fun clear() {
        memoryCache.evictAll()
        dynamicStatus.clear()
        loadingMutexes.clear()
    }
}
