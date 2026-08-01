package com.orbit.browser.browser.preview

import android.graphics.Bitmap

/**
 * Unified PreviewProvider abstraction interface.
 * Implemented by both WebView pages and Compose screens (Home, Bookmarks, History, Settings, etc.).
 */
interface PreviewProvider {
    /** Unique source name identifier (e.g. "WebView", "ComposeHome", "Settings"). */
    val sourceName: String

    /** Captures a live bitmap snapshot. Returns null if capture fails or view is not ready. */
    suspend fun capturePreview(): Bitmap?
}
