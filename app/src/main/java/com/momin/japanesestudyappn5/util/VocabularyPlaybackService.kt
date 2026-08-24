package com.momin.japanesestudyappn5.util

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
import android.util.Log
import androidx.core.app.NotificationCompat
import com.momin.japanesestudyappn5.MainActivity

class VocabularyPlaybackService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("VocabPlaybackService", "VocabularyPlaybackService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("VocabPlaybackService", "VocabularyPlaybackService started in foreground")
        AudioPlayer.acquireWakeLock(this)

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("VocabPlaybackService", "VocabularyPlaybackService destroyed")
        AudioPlayer.releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vocabulary Auto-Play",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification for Japanese vocabulary listening mode"
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎧 Vocabulary Auto-Play Active")
            .setContentText("Playing Japanese words & translations in background")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "vocab_playback_channel"
        private const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, VocabularyPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VocabularyPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}
