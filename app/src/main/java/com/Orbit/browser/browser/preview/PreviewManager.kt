package com.orbit.browser.browser.preview

import android.graphics.Bitmap
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import com.orbit.browser.browser.tabs.TabManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewManager @Inject constructor(
    val thumbnailCache: ThumbnailCache,
    val scheduler: PreviewScheduler,
    val tabManager: TabManager,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeCaptureLocks = ConcurrentHashMap<String, Job>()
    private val versionTracker = ConcurrentHashMap<String, Int>()

    /**
     * Requests a preview update for the specified [tabId] using [provider] under [policy].
     */
    fun requestPreview(
        tabId: String,
        provider: PreviewProvider,
        policy: SchedulePolicy = SchedulePolicy.Debounced()
    ) {
        scheduler.scheduleRequest(tabId, policy) {
            executeCapture(tabId, provider)
        }
    }

    /**
     * Executes the off-main-thread capture pipeline with concurrency locking,
     * job cancellation, and failure recovery.
     */
    private suspend fun executeCapture(tabId: String, provider: PreviewProvider) {
        // Enforce Capture Lock: If a capture is currently running for tabId, return
        if (activeCaptureLocks.containsKey(tabId)) {
            return
        }

        val captureJob = scope.launch(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            try {
                thumbnailCache.updateState(tabId, PreviewState.Capturing)

                val rawBitmap = provider.capturePreview()
                if (rawBitmap != null && !rawBitmap.isRecycled) {
                    val duration = System.currentTimeMillis() - startTime
                    val nextVersion = (versionTracker[tabId] ?: 0) + 1
                    versionTracker[tabId] = nextVersion

                    val preview = Preview(
                        bitmap = rawBitmap,
                        timestamp = System.currentTimeMillis(),
                        captureDurationMs = duration,
                        version = nextVersion,
                        source = provider.sourceName,
                        width = rawBitmap.width,
                        height = rawBitmap.height,
                        memorySizeBytes = rawBitmap.allocationByteCount.toLong()
                    )

                    withContext(Dispatchers.Main) {
                        thumbnailCache.put(tabId, preview)
                        tabManager.updateTab(tabId) { it.copy(thumbnail = preview.bitmap) }
                    }
                } else {
                    handleCaptureFailure(tabId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleCaptureFailure(tabId)
            } finally {
                activeCaptureLocks.remove(tabId)
            }
        }

        activeCaptureLocks[tabId] = captureJob
    }

    /**
     * Failure recovery: Keeps previous preview if valid, or falls back to Unavailable.
     */
    private suspend fun handleCaptureFailure(tabId: String) {
        withContext(Dispatchers.Main) {
            val existing = thumbnailCache.get(tabId)
            if (existing !is PreviewState.Ready) {
                thumbnailCache.updateState(tabId, PreviewState.Unavailable)
            }
        }
    }

    /**
     * Cancels any active capture and removes cache entries when a tab is closed.
     */
    fun cancelAndEvict(tabId: String) {
        scheduler.cancelScheduledRequest(tabId)
        activeCaptureLocks.remove(tabId)?.cancel()
        versionTracker.remove(tabId)
        thumbnailCache.evict(tabId)
        tabManager.updateTab(tabId) { it.copy(thumbnail = null) }
    }

    fun clearAll() {
        scheduler.clearAll()
        activeCaptureLocks.values.forEach { it.cancel() }
        activeCaptureLocks.clear()
        versionTracker.clear()
        thumbnailCache.clear()
    }
}
