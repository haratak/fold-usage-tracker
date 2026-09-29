package io.github.haratak.foldusage.ui

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.haratak.foldusage.R
import io.github.haratak.foldusage.data.db.AppDatabase
import io.github.haratak.foldusage.data.CsvShare
import io.github.haratak.foldusage.data.MonitorPreferences
import io.github.haratak.foldusage.data.PermissionChecks
import io.github.haratak.foldusage.data.PostureRepository
import io.github.haratak.foldusage.data.UsageReportBuilder
import io.github.haratak.foldusage.domain.CsvFormatter
import io.github.haratak.foldusage.domain.FoldPosture
import io.github.haratak.foldusage.domain.FoldReading
import io.github.haratak.foldusage.domain.TimeRange
import io.github.haratak.foldusage.domain.UsageSummary
import io.github.haratak.foldusage.service.FoldMonitorService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId

data class PermissionSnapshot(
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
    val batteryExempt: Boolean = false,
    val monitoring: Boolean = false,
    val readingText: String = "",
    val versionName: String = "",
)

data class UsageUiState(
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val range: TimeRange = TimeRange.TODAY,
    val summary: UsageSummary = UsageSummary.EMPTY,
    val labels: Map<String, String> = emptyMap(),
    val hasUsageAccess: Boolean = false,
    val permissions: PermissionSnapshot = PermissionSnapshot(),
)

class UsageViewModel(app: Application) : AndroidViewModel(app) {
    private val preferences = MonitorPreferences(app)
    private val builder = UsageReportBuilder(
        app,
        PostureRepository(AppDatabase.get(app).postureDao()),
    )
    private val _state = MutableStateFlow(UsageUiState())
    val state = _state.asStateFlow()
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()
    private var loadJob: Job? = null

    init {
        refresh()
    }

    fun setRange(range: TimeRange) {
        if (_state.value.range == range) return
        _state.update { it.copy(range = range) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val range = _state.value.range
            _state.update { it.copy(loading = true, permissions = readPermissions()) }
            try {
                val report = builder.build(range)
                _state.update {
                    it.copy(
                        loaded = true,
                        loading = false,
                        summary = report.summary,
                        labels = report.labels,
                        hasUsageAccess = report.hasUsageAccess,
                        permissions = readPermissions(),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                _state.update {
                    it.copy(
                        loaded = true,
                        loading = false,
                        summary = it.summary.copy(
                            folded = emptyList(),
                            unfolded = emptyList(),
                            foldedTotalMillis = 0,
                            unfoldedTotalMillis = 0,
                            slices = emptyList(),
                        ),
                        hasUsageAccess = false,
                        permissions = readPermissions(),
                    )
                }
            } catch (_: Exception) {
                _state.update { it.copy(loaded = true, loading = false, permissions = readPermissions()) }
                _messages.tryEmit(contextString(R.string.load_failed))
            }
        }
    }

    fun refreshStatus() {
        _state.update { it.copy(permissions = readPermissions()) }
    }

    fun setMonitoring(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled) {
            preferences.setEnabled(true)
            try {
                FoldMonitorService.start(context)
            } catch (_: RuntimeException) {
                preferences.setEnabled(false)
                _messages.tryEmit(contextString(R.string.start_failed))
            }
        } else {
            preferences.setEnabled(false)
            FoldMonitorService.stop(context)
        }
        refreshStatus()
    }

    fun currentCsv(): String? {
        val current = _state.value
        if (current.summary.slices.isEmpty()) return null
        return CsvFormatter.format(current.summary.slices, current.labels, ZoneId.systemDefault())
    }

    fun shareCsv() {
        val csv = currentCsv()
        if (csv == null) {
            _messages.tryEmit(contextString(R.string.nothing_to_export))
            return
        }
        CsvShare.share(getApplication(), csv)
    }

    private fun readPermissions(): PermissionSnapshot {
        val context = getApplication<Application>()
        val monitoring = preferences.isEnabled()
        return PermissionSnapshot(
            usageAccess = PermissionChecks.hasUsageAccess(context),
            notifications = PermissionChecks.hasNotifications(context),
            batteryExempt = PermissionChecks.isBatteryExempt(context),
            monitoring = monitoring,
            readingText = describeReading(
                context,
                preferences.latest(),
                System.currentTimeMillis(),
                monitoring,
            ),
            versionName = versionName(context),
        )
    }

    private fun contextString(resId: Int): String = getApplication<Application>().getString(resId)

    private fun versionName(context: Application): String {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName.orEmpty()
    }
}

internal fun describeReading(
    context: android.content.Context,
    reading: FoldReading?,
    nowMillis: Long,
    monitoring: Boolean,
): String {
    if (reading == null) return context.getString(R.string.current_reading_missing)
    if (monitoring && nowMillis - reading.atMillis > STALE_MILLIS) {
        return context.getString(R.string.reading_stale)
    }
    val posture = postureName(context, reading.posture)
    if (!reading.screenInteractive) {
        return context.getString(R.string.reading_off, posture)
    }
    if (reading.widthPx > 0 && reading.heightPx > 0) {
        return context.getString(R.string.reading_size, posture, reading.widthPx, reading.heightPx)
    }
    return posture
}

internal fun postureName(context: android.content.Context, posture: FoldPosture): String {
    return when (posture) {
        FoldPosture.FOLDED -> context.getString(R.string.posture_folded)
        FoldPosture.UNFOLDED -> context.getString(R.string.posture_unfolded)
        FoldPosture.UNKNOWN -> context.getString(R.string.posture_unknown)
    }
}

private const val STALE_MILLIS = 2 * 60 * 1000L
