package io.github.haratak.foldusage.domain

enum class FoldPosture {
    FOLDED,
    UNFOLDED,
    UNKNOWN,
    ;

    fun csvValue(): String = when (this) {
        FOLDED -> "folded"
        UNFOLDED -> "unfolded"
        UNKNOWN -> "unknown"
    }

    companion object {
        fun fromStored(value: String): FoldPosture =
            entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class DisplayPower {
    ON,
    OFF,
    OTHER,
}

data class DisplaySample(
    val id: Int,
    val isInternal: Boolean,
    val power: DisplayPower,
    val widthPx: Int,
    val heightPx: Int,
)

data class FoldReading(
    val posture: FoldPosture,
    val screenInteractive: Boolean,
    val widthPx: Int,
    val heightPx: Int,
    val atMillis: Long,
)

data class ForegroundSession(
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long,
)

data class PostureInterval(
    val posture: FoldPosture,
    val screenInteractive: Boolean,
    val startMillis: Long,
    val endMillis: Long,
)

data class UsageSlice(
    val packageName: String,
    val posture: FoldPosture,
    val startMillis: Long,
    val endMillis: Long,
) {
    val durationMillis: Long
        get() = (endMillis - startMillis).coerceAtLeast(0)
}

data class AppDuration(
    val packageName: String,
    val durationMillis: Long,
)

data class UsageSummary(
    val folded: List<AppDuration>,
    val unfolded: List<AppDuration>,
    val foldedTotalMillis: Long,
    val unfoldedTotalMillis: Long,
    val transitionCount: Int,
    val slices: List<UsageSlice>,
) {
    companion object {
        val EMPTY = UsageSummary(
            folded = emptyList(),
            unfolded = emptyList(),
            foldedTotalMillis = 0,
            unfoldedTotalMillis = 0,
            transitionCount = 0,
            slices = emptyList(),
        )
    }
}

enum class UsageEventType {
    SCREEN_NON_INTERACTIVE,
    ACTIVITY_PAUSED,
    ACTIVITY_STOPPED,
    ACTIVITY_RESUMED,
    SCREEN_INTERACTIVE,
    OTHER,
}

data class RawUsageEvent(
    val timestampMillis: Long,
    val packageName: String,
    val type: UsageEventType,
)

enum class TimeRange {
    TODAY,
    DAYS_7,
    DAYS_30,
}

data class StoredInterval(
    val id: Long,
    val posture: FoldPosture,
    val screenInteractive: Boolean,
    val startMillis: Long,
    val endMillis: Long?,
    val lastSeenMillis: Long,
    val widthPx: Int,
    val heightPx: Int,
)
