package io.github.haratak.foldusage.domain

object SessionSplitter {
    fun summarize(
        sessions: List<ForegroundSession>,
        intervals: List<PostureInterval>,
    ): UsageSummary {
        val slices = sessions
            .flatMap { split(it, intervals) }
            .sortedWith(compareBy({ it.startMillis }, { it.packageName }))
        val folded = totals(slices, FoldPosture.FOLDED)
        val unfolded = totals(slices, FoldPosture.UNFOLDED)
        return UsageSummary(
            folded = folded,
            unfolded = unfolded,
            foldedTotalMillis = folded.sumOf { it.durationMillis },
            unfoldedTotalMillis = unfolded.sumOf { it.durationMillis },
            transitionCount = transitionCount(intervals),
            slices = slices,
        )
    }

    fun split(session: ForegroundSession, intervals: List<PostureInterval>): List<UsageSlice> {
        if (session.endMillis <= session.startMillis || session.packageName.isEmpty()) {
            return emptyList()
        }
        val relevant = intervals
            .asSequence()
            .filter { it.screenInteractive && it.posture != FoldPosture.UNKNOWN }
            .filter { it.endMillis > it.startMillis }
            .sortedWith(compareBy({ it.startMillis }, { it.endMillis }))
            .toList()
        val slices = mutableListOf<UsageSlice>()
        var cursor = session.startMillis
        for (interval in relevant) {
            if (interval.endMillis <= cursor) continue
            if (interval.startMillis >= session.endMillis) break
            val start = maxOf(cursor, interval.startMillis)
            val end = minOf(session.endMillis, interval.endMillis)
            if (end > start) {
                slices += UsageSlice(
                    packageName = session.packageName,
                    posture = interval.posture,
                    startMillis = start,
                    endMillis = end,
                )
                cursor = end
            }
        }
        return slices
    }

    fun transitionCount(intervals: List<PostureInterval>): Int {
        var count = 0
        var previous: FoldPosture? = null
        for (interval in intervals.sortedBy { it.startMillis }) {
            if (interval.posture == FoldPosture.UNKNOWN) continue
            if (previous != null && previous != interval.posture) count++
            previous = interval.posture
        }
        return count
    }

    private fun totals(slices: List<UsageSlice>, posture: FoldPosture): List<AppDuration> {
        return slices
            .filter { it.posture == posture }
            .groupBy { it.packageName }
            .map { (packageName, group) ->
                AppDuration(packageName, group.sumOf { it.durationMillis })
            }
            .sortedWith(compareByDescending<AppDuration> { it.durationMillis }.thenBy { it.packageName })
    }
}
