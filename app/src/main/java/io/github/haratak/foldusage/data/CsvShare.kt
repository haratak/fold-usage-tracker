package io.github.haratak.foldusage.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import io.github.haratak.foldusage.R
import java.io.File

object CsvShare {
    fun share(context: Context, csv: String) {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, "fold-usage.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
            clipData = ClipData.newRawUri("fold-usage.csv", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_chooser)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
