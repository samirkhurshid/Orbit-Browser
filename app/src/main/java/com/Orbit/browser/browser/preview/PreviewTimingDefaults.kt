package com.orbit.browser.browser.preview

/**
 * Timing constants for Preview Subsystem scheduling policies.
 */
object PreviewTimingDefaults {
    /** Debounce delay for WebView scroll settling before triggering preview capture. */
    const val SCROLL_SETTLE_DELAY_MS = 400L

    /** Debounce delay for Compose screen layout composition settling before triggering preview capture. */
    const val COMPOSE_SETTLE_DELAY_MS = 300L
}
