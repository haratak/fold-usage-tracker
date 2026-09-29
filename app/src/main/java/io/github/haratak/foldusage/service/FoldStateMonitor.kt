package io.github.haratak.foldusage.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Display
import androidx.core.content.ContextCompat
import io.github.haratak.foldusage.domain.DisplayPower
import io.github.haratak.foldusage.domain.DisplaySample
import io.github.haratak.foldusage.domain.FoldPosture
import io.github.haratak.foldusage.domain.FoldReading
import io.github.haratak.foldusage.domain.FoldReadingResolver

/**
 * Watches fold posture from a foreground service, without an Activity window.
 *
 * DisplayManager is the signal: on a Pixel Fold the cover and the inner panel
 * are separate internal displays, and only the one in use is STATE_ON. The
 * lit panel's aspect ratio distinguishes 閉じてる (tall cover) from 開いてる
 * (nearly square inner display). Jetpack WindowManager is tied to an Activity
 * window and stops updating in the background. DeviceStateManager is still a
 * hidden API on API 36, so a sideloaded app cannot rely on it.
 *
 * Screen-off time is tracked separately via ACTION_SCREEN_OFF / ON and
 * PowerManager.isInteractive(), and is excluded from usage totals.
 */
class FoldStateMonitor(
    private val context: Context,
    private val onReading: (FoldReading) -> Unit,
) {
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var lastPosture = FoldPosture.UNKNOWN
    private var started = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = emit()
        override fun onDisplayRemoved(displayId: Int) = emit()
        override fun onDisplayChanged(displayId: Int) = emit()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            emit()
        }
    }

    fun start(initialPosture: FoldPosture) {
        if (started) return
        started = true
        lastPosture = initialPosture
        displayManager.registerDisplayListener(displayListener, handler)
        ContextCompat.registerReceiver(
            context,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        emit()
    }

    fun stop() {
        if (!started) return
        started = false
        displayManager.unregisterDisplayListener(displayListener)
        context.unregisterReceiver(screenReceiver)
    }

    fun emit() {
        if (!started) return
        val screenInteractive = powerManager.isInteractive
        val reading = FoldReadingResolver.resolve(
            displays = readDisplays(),
            screenInteractive = screenInteractive,
            lastPosture = lastPosture,
            nowMillis = System.currentTimeMillis(),
        )
        lastPosture = reading.posture
        onReading(reading)
    }

    private fun readDisplays(): List<DisplaySample> {
        return displayManager.displays.map { display ->
            val mode = display.mode
            DisplaySample(
                id = display.displayId,
                isInternal = display.isBuiltin(),
                power = when (display.state) {
                    Display.STATE_ON, Display.STATE_VR -> DisplayPower.ON
                    Display.STATE_OFF -> DisplayPower.OFF
                    else -> DisplayPower.OTHER
                },
                widthPx = mode.physicalWidth,
                heightPx = mode.physicalHeight,
            )
        }
    }
}

private fun Display.isBuiltin(): Boolean {
    // Display.getType() / TYPE_INTERNAL left the public SDK. API 37 adds isInternal().
    if (Build.VERSION.SDK_INT >= 37) {
        return isInternal
    }
    val externalFlags = Display.FLAG_PRESENTATION or Display.FLAG_PRIVATE
    return (flags and externalFlags) == 0
}
