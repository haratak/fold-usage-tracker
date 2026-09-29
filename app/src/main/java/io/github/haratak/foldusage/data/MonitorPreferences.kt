package io.github.haratak.foldusage.data

import android.annotation.SuppressLint
import android.content.Context
import io.github.haratak.foldusage.domain.FoldPosture
import io.github.haratak.foldusage.domain.FoldReading

class MonitorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    @SuppressLint("ApplySharedPref")
    fun setEnabled(enabled: Boolean) {
        // commit so a process death cannot drop the flag before the service starts.
        prefs.edit().putBoolean(KEY_ENABLED, enabled).commit()
    }

    fun saveLatest(reading: FoldReading) {
        prefs.edit()
            .putString(KEY_POSTURE, reading.posture.name)
            .putBoolean(KEY_SCREEN, reading.screenInteractive)
            .putInt(KEY_WIDTH, reading.widthPx)
            .putInt(KEY_HEIGHT, reading.heightPx)
            .putLong(KEY_AT, reading.atMillis)
            .apply()
    }

    fun latest(): FoldReading? {
        val at = prefs.getLong(KEY_AT, 0L)
        if (at == 0L) return null
        return FoldReading(
            posture = FoldPosture.fromStored(prefs.getString(KEY_POSTURE, null).orEmpty()),
            screenInteractive = prefs.getBoolean(KEY_SCREEN, false),
            widthPx = prefs.getInt(KEY_WIDTH, 0),
            heightPx = prefs.getInt(KEY_HEIGHT, 0),
            atMillis = at,
        )
    }

    companion object {
        private const val PREFS = "fold_usage"
        private const val KEY_ENABLED = "monitoring_enabled"
        private const val KEY_POSTURE = "latest_posture"
        private const val KEY_SCREEN = "latest_screen"
        private const val KEY_WIDTH = "latest_width"
        private const val KEY_HEIGHT = "latest_height"
        private const val KEY_AT = "latest_at"
    }
}
