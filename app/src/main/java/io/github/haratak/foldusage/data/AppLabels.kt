package io.github.haratak.foldusage.data

import android.content.Context
import android.content.pm.PackageManager

object AppLabels {
    fun label(context: Context, packageName: String): String {
        val pm = context.packageManager
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info)?.toString().orEmpty().ifBlank { packageName }
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}
