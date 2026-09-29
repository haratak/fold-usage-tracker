package io.github.haratak.foldusage.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.haratak.foldusage.data.MonitorPreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        if (!MonitorPreferences(context).isEnabled()) return
        try {
            FoldMonitorService.start(context)
        } catch (_: RuntimeException) {
            // Boot can race the foreground-service allowance. The next unlock or app open starts it.
        }
    }
}
