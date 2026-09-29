package io.github.haratak.foldusage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.haratak.foldusage.MainActivity
import io.github.haratak.foldusage.R
import io.github.haratak.foldusage.data.db.AppDatabase
import io.github.haratak.foldusage.data.MonitorPreferences
import io.github.haratak.foldusage.data.PostureRepository
import io.github.haratak.foldusage.domain.FoldPosture
import io.github.haratak.foldusage.domain.FoldReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class FoldMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences by lazy { MonitorPreferences(this) }
    private val repository by lazy { PostureRepository(AppDatabase.get(this).postureDao()) }
    private lateinit var monitor: FoldStateMonitor
    private var lastNotificationText: String? = null
    private var heartbeatJob: Job? = null

    @Volatile
    private var acceptingReadings = false

    override fun onCreate() {
        super.onCreate()
        monitor = FoldStateMonitor(this, ::onReading)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        val notification = buildNotification(getString(R.string.posture_unknown))
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        if (!preferences.isEnabled()) {
            acceptingReadings = false
            stopSelf()
            return START_NOT_STICKY
        }
        acceptingReadings = true
        if (!::monitor.isInitialized) {
            monitor = FoldStateMonitor(this, ::onReading)
        }
        monitor.start(preferences.latest()?.posture ?: FoldPosture.UNKNOWN)
        if (heartbeatJob?.isActive != true) {
            heartbeatJob = scope.launch {
                while (isActive) {
                    delay(HEARTBEAT_MILLIS)
                    monitor.emit()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        acceptingReadings = false
        if (::monitor.isInitialized) monitor.stop()
        runBlocking(Dispatchers.IO) {
            repository.closeOpen(System.currentTimeMillis())
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onReading(reading: FoldReading) {
        scope.launch {
            repository.record(reading) { acceptingReadings }
            preferences.saveLatest(reading)
            val text = notificationText(reading)
            if (text != lastNotificationText) {
                lastNotificationText = text
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(text))
            }
        }
    }

    private fun notificationText(reading: FoldReading): String {
        if (!reading.screenInteractive) return getString(R.string.screen_off)
        return postureLabel(reading.posture)
    }

    private fun postureLabel(posture: FoldPosture): String {
        return when (posture) {
            FoldPosture.FOLDED -> getString(R.string.posture_folded)
            FoldPosture.UNFOLDED -> getString(R.string.posture_unfolded)
            FoldPosture.UNKNOWN -> getString(R.string.posture_unknown)
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_fold)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "fold_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val HEARTBEAT_MILLIS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, FoldMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FoldMonitorService::class.java))
        }
    }
}
