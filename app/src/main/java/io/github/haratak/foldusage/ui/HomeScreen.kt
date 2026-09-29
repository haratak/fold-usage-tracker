@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.haratak.foldusage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.haratak.foldusage.R
import io.github.haratak.foldusage.domain.AppDuration
import io.github.haratak.foldusage.domain.TimeRange
import io.github.haratak.foldusage.domain.UsageSummary
import io.github.haratak.foldusage.domain.formatDurationJa

@Composable
fun HomeScreen(
    state: UsageUiState,
    onRange: (TimeRange) -> Unit,
    onOpenSetup: () -> Unit,
    onExport: () -> Unit,
    onGrantUsage: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onExport) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_export))
                    }
                    IconButton(onClick = onOpenSetup) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RangePicker(state.range, onRange)
            if (!state.hasUsageAccess && state.loaded) {
                UsageBanner(onGrantUsage)
            }
            if (!state.loaded) {
                Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                SummaryCard(state.summary)
                if (state.loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                BoxWithConstraints {
                    AppSections(state, maxWidth >= 720.dp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RangePicker(selected: TimeRange, onRange: (TimeRange) -> Unit) {
    val options = listOf(
        TimeRange.TODAY to stringResource(R.string.range_today),
        TimeRange.DAYS_7 to stringResource(R.string.range_7),
        TimeRange.DAYS_30 to stringResource(R.string.range_30),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (range, label) ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onRange(range) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun UsageBanner(onGrant: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.usage_banner),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onGrant) {
                Text(stringResource(R.string.grant))
            }
        }
    }
}

@Composable
private fun SummaryCard(summary: UsageSummary) {
    val foldedColor = MaterialTheme.colorScheme.tertiary
    val unfoldedColor = MaterialTheme.colorScheme.primary
    val total = summary.foldedTotalMillis + summary.unfoldedTotalMillis
    val foldedPct = if (total == 0L) 0 else ((summary.foldedTotalMillis * 100) / total).toInt()
    val unfoldedPct = if (total == 0L) 0 else 100 - foldedPct
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TotalColumn(
                    color = foldedColor,
                    label = stringResource(R.string.posture_folded),
                    duration = formatDurationJa(summary.foldedTotalMillis),
                    percent = foldedPct,
                )
                TotalColumn(
                    color = unfoldedColor,
                    label = stringResource(R.string.posture_unfolded),
                    duration = formatDurationJa(summary.unfoldedTotalMillis),
                    percent = unfoldedPct,
                    alignEnd = true,
                )
            }
            RatioBar(
                folded = summary.foldedTotalMillis,
                unfolded = summary.unfoldedTotalMillis,
                foldedColor = foldedColor,
                unfoldedColor = unfoldedColor,
            )
            Text(
                stringResource(R.string.transitions, summary.transitionCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TotalColumn(
    color: Color,
    label: String,
    duration: String,
    percent: Int,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
        Text(duration, style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.percent, percent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RatioBar(
    folded: Long,
    unfolded: Long,
    foldedColor: Color,
    unfoldedColor: Color,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (folded <= 0L && unfolded <= 0L) return@Row
        if (folded > 0L) {
            Box(
                Modifier
                    .weight(folded.toFloat())
                    .fillMaxSize()
                    .background(foldedColor),
            )
        }
        if (unfolded > 0L) {
            Box(
                Modifier
                    .weight(unfolded.toFloat())
                    .fillMaxSize()
                    .background(unfoldedColor),
            )
        }
    }
}

@Composable
private fun AppSections(state: UsageUiState, wide: Boolean) {
    val summary = state.summary
    if (summary.folded.isEmpty() && summary.unfolded.isEmpty()) {
        Text(
            stringResource(R.string.empty_apps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        return
    }
    if (wide) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AppSection(
                title = stringResource(R.string.posture_folded),
                total = summary.foldedTotalMillis,
                apps = summary.folded,
                labels = state.labels,
                modifier = Modifier.weight(1f),
            )
            AppSection(
                title = stringResource(R.string.posture_unfolded),
                total = summary.unfoldedTotalMillis,
                apps = summary.unfolded,
                labels = state.labels,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        AppSection(
            title = stringResource(R.string.posture_folded),
            total = summary.foldedTotalMillis,
            apps = summary.folded,
            labels = state.labels,
        )
        AppSection(
            title = stringResource(R.string.posture_unfolded),
            total = summary.unfoldedTotalMillis,
            apps = summary.unfolded,
            labels = state.labels,
        )
    }
}

@Composable
private fun AppSection(
    title: String,
    total: Long,
    apps: List<AppDuration>,
    labels: Map<String, String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            "$title · ${formatDurationJa(total)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        if (apps.isEmpty()) {
            Text(
                stringResource(R.string.empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            apps.forEach { app ->
                ListItem(
                    headlineContent = {
                        Text(
                            labels[app.packageName] ?: app.packageName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = { AppIcon(app.packageName) },
                    trailingContent = {
                        Text(
                            formatDurationJa(app.durationMillis),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    body: String,
    status: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text(
                    status,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!granted) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SetupScreen(
    state: UsageUiState,
    onBack: () -> Unit,
    onGrantUsage: () -> Unit,
    onGrantNotifications: () -> Unit,
    onBattery: () -> Unit,
    onToggleMonitoring: (Boolean) -> Unit,
) {
    val permissions = state.permissions
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.setup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PermissionCard(
                title = stringResource(R.string.perm_usage_title),
                body = stringResource(R.string.perm_usage_body),
                status = if (permissions.usageAccess) {
                    stringResource(R.string.status_granted)
                } else {
                    stringResource(R.string.status_denied)
                },
                granted = permissions.usageAccess,
                actionLabel = stringResource(R.string.open_settings),
                onAction = onGrantUsage,
            )
            PermissionCard(
                title = stringResource(R.string.perm_notif_title),
                body = stringResource(R.string.perm_notif_body),
                status = if (permissions.notifications) {
                    stringResource(R.string.status_granted)
                } else {
                    stringResource(R.string.status_denied)
                },
                granted = permissions.notifications,
                actionLabel = stringResource(R.string.grant),
                onAction = onGrantNotifications,
            )
            PermissionCard(
                title = stringResource(R.string.perm_battery_title),
                body = stringResource(R.string.perm_battery_body),
                status = if (permissions.batteryExempt) {
                    stringResource(R.string.status_exempt)
                } else {
                    stringResource(R.string.status_optimized)
                },
                granted = permissions.batteryExempt,
                actionLabel = stringResource(R.string.request_exemption),
                onAction = onBattery,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.monitoring_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (permissions.monitoring) {
                            stringResource(R.string.monitoring_running)
                        } else {
                            stringResource(R.string.monitoring_stopped)
                        },
                        color = if (permissions.monitoring) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(permissions.readingText, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.how_detection),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { onToggleMonitoring(!permissions.monitoring) }) {
                        Text(
                            if (permissions.monitoring) {
                                stringResource(R.string.stop_monitoring)
                            } else {
                                stringResource(R.string.start_monitoring)
                            },
                        )
                    }
                }
            }
            if (permissions.versionName.isNotEmpty()) {
                Text(
                    stringResource(R.string.version_label, permissions.versionName),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
