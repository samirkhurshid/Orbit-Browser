package com.orbit.browser.browser.preview

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailCache @Inject constructor(
    context: Context
) : ComponentCallbacks2 {

    // Cap cache to 1/8th of available JVM heap size (e.g. ~32MB - 48MB)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceAtLeast(1024)

    private val lruCache = object : LruCache<String, Preview>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Preview): Int {
            return (value.memorySizeBytes / 1024).toInt()
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Preview, newValue: Preview?) {
            if (evicted && oldValue != newValue && !oldValue.bitmap.isRecycled) {
                // Recycle bitmap if evicted from cache
                try {
                    oldValue.bitmap.recycle()
                } catch (e: Exception) {
                    // Ignore recycling exceptions
                }
            }
        }
    }

    private val _previewStates = MutableStateFlow<Map<String, PreviewState>>(emptyMap())
    val previewStates: StateFlow<Map<String, PreviewState>> = _previewStates.asStateFlow()

    private var activeTabId: String? = null

    init {
        try {
            context.applicationContext.registerComponentCallbacks(this)
        } catch (e: Exception) {
            // Context register fallback
        }
    }

    fun setActiveTabId(tabId: String?) {
        activeTabId = tabId
    }

    fun get(tabId: String): PreviewState {
        val cached = lruCache.get(tabId)
        return if (cached != null && !cached.bitmap.isRecycled) {
            PreviewState.Ready(cached)
        } else {
            _previewStates.value[tabId] ?: PreviewState.Unavailable
        }
    }

    fun put(tabId: String, preview: Preview) {
        lruCache.put(tabId, preview)
        _previewStates.value = _previewStates.value + (tabId to PreviewState.Ready(preview))
    }

    fun updateState(tabId: String, state: PreviewState) {
        _previewStates.value = _previewStates.value + (tabId to state)
    }

    fun evict(tabId: String) {
        val removed = lruCache.remove(tabId)
        if (removed != null && !removed.bitmap.isRecycled) {
            try { removed.bitmap.recycle() } catch (e: Exception) {}
        }
        _previewStates.value = _previewStates.value - tabId
    }

    fun clear() {
        lruCache.evictAll()
        _previewStates.value = emptyMap()
    }

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL || level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            // Trim background cache under critical memory pressure, preserving active tab
            val currentActive = activeTabId
            val activePreview = if (currentActive != null) lruCache.get(currentActive) else null
            lruCache.evictAll()
            if (currentActive != null && activePreview != null && !activePreview.bitmap.isRecycled) {
                lruCache.put(currentActive, activePreview)
            }
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            lruCache.trimToSize(cacheSizeKb / 2)
        }
    }

    override fun onLowMemory() {
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
}
