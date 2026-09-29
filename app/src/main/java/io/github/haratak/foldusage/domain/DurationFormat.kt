package io.github.haratak.foldusage.domain

fun formatDurationJa(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    if (safe == 0L) return "0分"
    val minutes = safe / 60_000
    if (minutes == 0L) return "1分未満"
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}時間${mins}分"
        hours > 0 -> "${hours}時間"
        else -> "${minutes}分"
    }
}
