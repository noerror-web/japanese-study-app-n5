package com.momin.japanesestudyappn5.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.momin.japanesestudyappn5.MainActivity
import java.util.concurrent.TimeUnit
import java.util.Calendar

class StudyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "japanese_study_reminder"
        const val WORK_NAME = "daily_study_reminder"

        fun schedule(context: Context) {
            // Calculate delay until 8 PM today (or tomorrow if already past)
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
            val initialDelay = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Study Reminder",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminds you to study Japanese every day"
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
        val mastered = prefs.getStringSet("mastered_vocab", emptySet())?.size ?: 0
        val bookmarks = prefs.getStringSet("bookmarked_vocab", emptySet())?.size ?: 0

        val messages = listOf(
            "🌸 時間です！ Time to study Japanese! You have $bookmarks words to review.",
            "📚 勉強しましょう！ Keep it up — $mastered words mastered so far!",
            "🎌 Daily Japanese practice — just 5 minutes makes a difference!",
            "🔤 Don't break your streak! Open the app and review your flashcards.",
            "✨ がんばって！ You can do it! Your N5 goal is within reach."
        )
        val message = messages[System.currentTimeMillis().toInt().and(0xFF) % messages.size]

        createChannel(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🎌 Japanese Study Time!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1001, notification)

        return Result.success()
    }
}
