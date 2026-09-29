package io.github.haratak.foldusage.domain

object UsageSessionReconstructor {
    const val MERGE_GAP_MILLIS = 1_500L

    fun reconstruct(events: List<RawUsageEvent>, rangeEndMillis: Long): List<ForegroundSession> {
        val sorted = events.sortedWith(compareBy({ it.timestampMillis }, { it.type.ordinal }))
        val raw = mutableListOf<ForegroundSession>()
        var packageName: String? = null
        var start: Long? = null

        fun close(at: Long) {
            val currentPackage = packageName
            val currentStart = start
            packageName = null
            start = null
            if (currentPackage.isNullOrEmpty() || currentStart == null) return
            if (at > currentStart) {
                raw += ForegroundSession(currentPackage, currentStart, at)
            }
        }

        for (event in sorted) {
            if (event.timestampMillis > rangeEndMillis) break
            when (event.type) {
                UsageEventType.ACTIVITY_RESUMED -> {
                    if (event.packageName.isEmpty() || packageName == event.packageName) continue
                    close(event.timestampMillis)
                    packageName = event.packageName
                    start = event.timestampMillis
                }
                UsageEventType.ACTIVITY_PAUSED,
                UsageEventType.ACTIVITY_STOPPED,
                -> {
                    if (packageName == event.packageName) close(event.timestampMillis)
                }
                UsageEventType.SCREEN_NON_INTERACTIVE -> close(event.timestampMillis)
                UsageEventType.SCREEN_INTERACTIVE,
                UsageEventType.OTHER,
                -> Unit
            }
        }
        close(rangeEndMillis)
        return mergeShortGaps(raw, events)
    }

    fun clip(
        sessions: List<ForegroundSession>,
        rangeStartMillis: Long,
        rangeEndMillis: Long,
    ): List<ForegroundSession> {
        return sessions.mapNotNull { session ->
            val start = maxOf(session.startMillis, rangeStartMillis)
            val end = minOf(session.endMillis, rangeEndMillis)
            if (end > start) session.copy(startMillis = start, endMillis = end) else null
        }
    }

    private fun mergeShortGaps(
        sessions: List<ForegroundSession>,
        events: List<RawUsageEvent>,
    ): List<ForegroundSession> {
        if (sessions.isEmpty()) return sessions
        val screenOffs = events
            .filter { it.type == UsageEventType.SCREEN_NON_INTERACTIVE }
            .map { it.timestampMillis }
        val merged = mutableListOf<ForegroundSession>()
        var current = sessions.first()
        for (next in sessions.drop(1)) {
            val gap = next.startMillis - current.endMillis
            val screenWentOff = screenOffs.any { it in current.endMillis..next.startMillis }
            if (next.packageName == current.packageName && gap <= MERGE_GAP_MILLIS && !screenWentOff) {
                current = current.copy(endMillis = maxOf(current.endMillis, next.endMillis))
            } else {
                merged += current
                current = next
            }
        }
        merged += current
        return merged
    }
}
