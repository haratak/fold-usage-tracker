package io.github.haratak.foldusage.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvFormatter {
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")

    fun format(
        slices: List<UsageSlice>,
        labels: Map<String, String>,
        zone: ZoneId,
    ): String {
        val body = buildString {
            append("start,end,package,app_label,fold_state\n")
            for (slice in slices.sortedWith(compareBy({ it.startMillis }, { it.packageName }))) {
                if (slice.posture != FoldPosture.FOLDED && slice.posture != FoldPosture.UNFOLDED) continue
                if (slice.durationMillis <= 0L) continue
                append(field(formatTime(slice.startMillis, zone)))
                append(',')
                append(field(formatTime(slice.endMillis, zone)))
                append(',')
                append(field(slice.packageName))
                append(',')
                append(field(labels[slice.packageName] ?: slice.packageName))
                append(',')
                append(field(slice.posture.csvValue()))
                append('\n')
            }
        }
        return "\uFEFF$body"
    }

    fun formatTime(millis: Long, zone: ZoneId): String =
        Instant.ofEpochMilli(millis).atZone(zone).format(timestampFormatter)

    private fun field(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuote) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
