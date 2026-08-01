package com.orbit.browser.browser.preview

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewScheduler @Inject constructor() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val scheduledJobs = ConcurrentHashMap<String, Job>()
    private val lastExecutedTimeMap = ConcurrentHashMap<String, Long>()

    /**
     * Schedules a preview update request according to the specified [SchedulePolicy].
     */
    fun scheduleRequest(
        tabId: String,
        policy: SchedulePolicy,
        action: suspend () -> Unit
    ) {
        scheduledJobs[tabId]?.cancel()

        val job = scope.launch {
            when (policy) {
                is SchedulePolicy.Immediate -> {
                    action()
                    lastExecutedTimeMap[tabId] = System.currentTimeMillis()
                }
                is SchedulePolicy.Debounced -> {
                    delay(policy.delayMs)
                    if (isActive) {
                        action()
                        lastExecutedTimeMap[tabId] = System.currentTimeMillis()
                    }
                }
                is SchedulePolicy.Throttled -> {
                    val lastExec = lastExecutedTimeMap[tabId] ?: 0L
                    val elapsed = System.currentTimeMillis() - lastExec
                    if (elapsed < policy.minIntervalMs) {
                        delay(policy.minIntervalMs - elapsed)
                    }
                    if (isActive) {
                        action()
                        lastExecutedTimeMap[tabId] = System.currentTimeMillis()
                    }
                }
            }
        }
        scheduledJobs[tabId] = job
    }

    /**
     * Cancels any pending scheduled request for the specified tab.
     */
    fun cancelScheduledRequest(tabId: String) {
        scheduledJobs.remove(tabId)?.cancel()
        lastExecutedTimeMap.remove(tabId)
    }

    fun clearAll() {
        scheduledJobs.values.forEach { it.cancel() }
        scheduledJobs.clear()
        lastExecutedTimeMap.clear()
    }
}
