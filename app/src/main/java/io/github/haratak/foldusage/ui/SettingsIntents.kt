package io.github.haratak.foldusage.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

fun openUsageAccessSettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { data = packageUri }
    val plain = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    if (!tryStart(context, withPackage)) {
        tryStart(context, plain)
    }
}

@SuppressLint("BatteryLife")
fun openBatteryExemption(context: Context) {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    if (!tryStart(context, request)) {
        tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

private fun tryStart(context: Context, intent: Intent): Boolean {
    return try {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
