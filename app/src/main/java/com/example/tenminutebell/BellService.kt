
package com.example.tenminutebell

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

class BellService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var stopBellRunnable: Runnable? = null

    private val tick = object : Runnable {
        override fun run() {
            playBell()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TenMinuteBell:WakeLock"
            ).apply { acquire() }

        handler.postDelayed(tick, INTERVAL_MS)
    }

    private fun playBell() {
        try {
            stopBellRunnable?.let { handler.removeCallbacks(it) }
            ringtone?.stop()

            val uri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_RINGTONE
            ) ?: return

            val newRingtone = RingtoneManager.getRingtone(
                applicationContext, uri
            ) ?: return

            ringtone = newRingtone

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                newRingtone.audioAttributes =
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
            }

            newRingtone.play()

            val stopTask = Runnable {
                try {
                    newRingtone.stop()
                } catch (_: Exception) {
                }

                if (ringtone === newRingtone) {
                    ringtone = null
                }
                stopBellRunnable = null
            }

            stopBellRunnable = stopTask
            handler.postDelayed(stopTask, BELL_DURATION_MS)

        } catch (_: Exception) {
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bell timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the ten-minute bell timer running"
                setSound(null, null)
            }

            (getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= 23)
                    PendingIntent.FLAG_IMMUTABLE else 0
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Ten Minute Bell is active")
                .setContentText("A bell will sound every 10 minutes")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Ten Minute Bell is active")
                .setContentText("A bell will sound every 10 minutes")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .build()
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        stopBellRunnable?.let { handler.removeCallbacks(it) }
        stopBellRunnable = null

        ringtone?.stop()
        ringtone = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val INTERVAL_MS = 10 * 60 * 1000L
        private const val BELL_DURATION_MS = 1000L
        private const val CHANNEL_ID = "ten_minute_bell_status"
        private const val NOTIFICATION_ID = 410
    }
}
