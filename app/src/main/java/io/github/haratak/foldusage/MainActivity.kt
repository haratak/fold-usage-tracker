package io.github.haratak.foldusage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.haratak.foldusage.data.MonitorPreferences
import io.github.haratak.foldusage.service.FoldMonitorService
import io.github.haratak.foldusage.ui.FoldUsageNav
import io.github.haratak.foldusage.ui.theme.FoldUsageTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (MonitorPreferences(this).isEnabled()) {
            try {
                FoldMonitorService.start(this)
            } catch (_: RuntimeException) {
                // The foreground service can be blocked briefly after boot. The user can start it again.
            }
        }
        enableEdgeToEdge()
        setContent {
            FoldUsageTheme {
                FoldUsageNav()
            }
        }
    }
}
