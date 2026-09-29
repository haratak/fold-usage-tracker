package io.github.haratak.foldusage.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import io.github.haratak.foldusage.domain.RawUsageEvent
import io.github.haratak.foldusage.domain.UsageEventType

class UsageStatsSource(
    context: Context,
) {
    private val usageStatsManager =
        context.applicationContext.getSystemService(UsageStatsManager::class.java)

    fun query(startMillis: Long, endMillis: Long): List<RawUsageEvent> {
        if (endMillis <= startMillis) return emptyList()
        val events = usageStatsManager.queryEvents(startMillis, endMillis)
        val out = ArrayList<RawUsageEvent>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventType.ACTIVITY_RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventType.ACTIVITY_PAUSED
                UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventType.ACTIVITY_STOPPED
                UsageEvents.Event.SCREEN_INTERACTIVE -> UsageEventType.SCREEN_INTERACTIVE
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventType.SCREEN_NON_INTERACTIVE
                else -> continue
            }
            out += RawUsageEvent(
                timestampMillis = event.timeStamp,
                packageName = event.packageName.orEmpty(),
                type = type,
            )
        }
        return out
    }
}
