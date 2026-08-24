package com.momin.japanesestudyappn5

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.momin.japanesestudyappn5.theme.JapaneseStudyAppN5Theme
import com.momin.japanesestudyappn5.util.StudyReminderWorker
import com.momin.japanesestudyappn5.util.FirebaseSyncManager
import com.momin.japanesestudyappn5.util.AudioPlayer
import com.momin.japanesestudyappn5.util.GameFeedbackHelper
import com.momin.japanesestudyappn5.ui.screens.SuspendedScreen
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val notificationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val prefs = getSharedPreferences("japanese_study_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("notifications_enabled", true)) {
                StudyReminderWorker.schedule(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("japanese_study_prefs", MODE_PRIVATE)
        if (prefs.getString("gemini_api_key", "").isNullOrEmpty()) {
            prefs.edit().putString("gemini_api_key", "AIzaSyBilpnfadrS9rJ9Xxgpo4QzCPJLybpgIHU").apply()
        }

        // Increment open counter
        prefs.edit().putInt("total_opens", prefs.getInt("total_opens", 0) + 1).apply()

        // Streak tracking
        val today = LocalDate.now().toString()
        val lastOpenDate = prefs.getString("last_open_date", null)
        val currentStreak = prefs.getInt("streak_count", 1)
        val newStreak = when {
            lastOpenDate == null -> 1
            lastOpenDate == today -> currentStreak
            LocalDate.parse(lastOpenDate).plusDays(1).toString() == today -> currentStreak + 1
            else -> 1
        }
        prefs.edit()
            .putString("last_open_date", today)
            .putInt("streak_count", newStreak)
            .apply()

        // Create notification channel (safe to call multiple times)
        StudyReminderWorker.createChannel(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule reminder if enabled
        if (prefs.getBoolean("notifications_enabled", true)) {
            StudyReminderWorker.schedule(this)
        }

        // Initialize cloud sync manager on startup
        FirebaseSyncManager.initialize(applicationContext)

        // Pre-warm and initialize TTS engine early to avoid initial latency
        AudioPlayer.ensureTts(applicationContext)

        enableEdgeToEdge()
        setContent {
            val rawMode = remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
            val currentHour = remember { java.time.LocalTime.now().hour }
            val effectiveMode = when (rawMode.value) {
                "system" -> if (currentHour >= 19 || currentHour < 6) "tokyonight" else "sakura"
                else -> rawMode.value
            }
            val isBanned by FirebaseSyncManager.isBanned.collectAsState()

            JapaneseStudyAppN5Theme(themeMode = effectiveMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBanned) {
                        SuspendedScreen()
                    } else {
                        MainNavigation(
                            themeMode = rawMode.value,
                            onThemeModeChange = { mode ->
                                rawMode.value = mode
                                prefs.edit().putString("theme_mode", mode).apply()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseSyncManager.cleanUp()
        AudioPlayer.shutdown()
        GameFeedbackHelper.release()
    }
}
