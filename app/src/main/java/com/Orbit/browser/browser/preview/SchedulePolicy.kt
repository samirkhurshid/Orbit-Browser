package com.orbit.browser.browser.preview

/**
 * Configurable scheduling policy rules for preview generation.
 */
sealed class SchedulePolicy {
    /** Execute capture immediately on next tick. */
    object Immediate : SchedulePolicy()

    /** Debounce capture request by specified delay in milliseconds. */
    data class Debounced(val delayMs: Long = 300L) : SchedulePolicy()

    /** Throttle capture request to at most once per minimum interval in milliseconds. */
    data class Throttled(val minIntervalMs: Long = 500L) : SchedulePolicy()
}
