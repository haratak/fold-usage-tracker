package io.github.haratak.foldusage.data

import android.content.Context
import io.github.haratak.foldusage.domain.SessionSplitter
import io.github.haratak.foldusage.domain.TimeRange
import io.github.haratak.foldusage.domain.UsageSessionReconstructor
import io.github.haratak.foldusage.domain.UsageSummary
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class BuiltReport(
    val summary: UsageSummary,
    val labels: Map<String, String>,
    val rangeStartMillis: Long,
    val rangeEndMillis: Long,
    val hasUsageAccess: Boolean,
)

class UsageReportBuilder(
    private val context: Context,
    private val repository: PostureRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun build(range: TimeRange): BuiltReport {
        val now = clock()
        val zone = ZoneId.systemDefault()
        val start = range.startMillis(now, zone)
        val intervals = repository.intervalsOverlapping(start, now, now)
        var hasUsage = PermissionChecks.hasUsageAccess(context)
        val events = if (!hasUsage) {
            emptyList()
        } else {
            try {
                UsageStatsSource(context).query(start - LOOKBACK_MILLIS, now)
            } catch (_: SecurityException) {
                hasUsage = false
                emptyList()
            }
        }
        val sessions = UsageSessionReconstructor.clip(
            UsageSessionReconstructor.reconstruct(events, now),
            start,
            now,
        )
        val summary = SessionSplitter.summarize(sessions, intervals)
        val labels = summary.slices
            .map { it.packageName }
            .distinct()
            .associateWith { AppLabels.label(context, it) }
        return BuiltReport(
            summary = summary,
            labels = labels,
            rangeStartMillis = start,
            rangeEndMillis = now,
            hasUsageAccess = hasUsage,
        )
    }

    companion object {
        private const val LOOKBACK_MILLIS = 24L * 60L * 60L * 1000L
    }
}

fun TimeRange.startMillis(nowMillis: Long, zone: ZoneId): Long {
    val now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone)
    return when (this) {
        TimeRange.TODAY -> now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        TimeRange.DAYS_7 -> nowMillis - 7L * 24L * 60L * 60L * 1000L
        TimeRange.DAYS_30 -> nowMillis - 30L * 24L * 60L * 60L * 1000L
    }
}
