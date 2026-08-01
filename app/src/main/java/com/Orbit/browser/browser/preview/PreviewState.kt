package com.orbit.browser.browser.preview

import android.graphics.Bitmap

/**
 * Metadata model holding thumbnail bitmap and execution metrics.
 */
data class Preview(
    val bitmap: Bitmap,
    val timestamp: Long = System.currentTimeMillis(),
    val captureDurationMs: Long = 0,
    val version: Int = 1,
    val source: String = "",
    val width: Int = bitmap.width,
    val height: Int = bitmap.height,
    val memorySizeBytes: Long = bitmap.allocationByteCount.toLong(),
)

/**
 * Explicit state model for tab previews.
 */
sealed class PreviewState {
    object Idle : PreviewState()
    object Queued : PreviewState()
    object Capturing : PreviewState()
    data class Ready(val preview: Preview) : PreviewState()
    object Invalid : PreviewState()
    object Unavailable : PreviewState()
}
