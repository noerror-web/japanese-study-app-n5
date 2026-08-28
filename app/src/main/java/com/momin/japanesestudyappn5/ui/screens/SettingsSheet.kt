package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.StudyReminderWorker
import com.momin.japanesestudyappn5.util.FirebaseSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    currentThemeMode: String,
    onThemeModeChange: (String) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    romajiEnabled: Boolean,
    onRomajiToggle: (Boolean) -> Unit,
    furiganaEnabled: Boolean,
    onFuriganaToggle: (Boolean) -> Unit,
    studyGoal: Int,
    onStudyGoalChange: (Int) -> Unit,
    kanjiDisabled: Boolean = false,
    onKanjiDisabledToggle: (Boolean) -> Unit = {},
    fontScale: Float = 1.0f,
    onFontScaleChange: (Float) -> Unit = {},
    appLanguage: String = "en",
    onAppLanguageChange: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    var geminiApiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var isApiKeySaved by remember { mutableStateOf(true) }

    var downloadBaseUrl by remember { mutableStateOf(com.momin.japanesestudyappn5.util.OnlineAssetsManager.getBaseUrl(context)) }
    var isBaseUrlSaved by remember { mutableStateOf(true) }

    val isOwnerAdmin = remember { prefs.getBoolean("is_admin_mode", false) }
    val coroutineScope = rememberCoroutineScope()
    var isAllDownloaded by remember { mutableStateOf(com.momin.japanesestudyappn5.util.OnlineAssetsManager.isAllDownloaded(context)) }
    var isDictDownloaded by remember { mutableStateOf(com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDictionaryDownloaded(context)) }
    var isDictDownloading by remember { mutableStateOf(false) }
    var dictDownloadProgress by remember { mutableFloatStateOf(0f) }



    val syncCode = remember { FirebaseSyncManager.getOrCreateSyncCode(context) }
    val syncStatus by FirebaseSyncManager.syncStatus.collectAsState()

    val auth = remember {
        if (FirebaseSyncManager.isFirebaseInitialized()) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null
    }
    val currentUser = auth?.currentUser

    var restoreCodeInput by remember { mutableStateOf("") }
    var restoreError by remember { mutableStateOf("") }
    var restoreSuccess by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var isTestingServer by remember { mutableStateOf(false) }
    var testServerResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Scrollable Settings Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── CARD 1: Appearance & Study Goals ────────────────────────────
                item {
                    OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CardHeader(
                            icon = Icons.Default.Palette,
                            title = "Appearance & Goals",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "App Theme",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemePreviewItem(
                                name = "System",
                                primaryColor = Color(0xFF121318),
                                backgroundColor = Color(0xFFFAF9F7),
                                selected = currentThemeMode == "system" || currentThemeMode == "",
                                onClick = { onThemeModeChange("system") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePreviewItem(
                                name = "Light",
                                primaryColor = Color(0xFF3D5193),
                                backgroundColor = Color(0xFFFAF9F7),
                                selected = currentThemeMode == "light",
                                onClick = { onThemeModeChange("light") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePreviewItem(
                                name = "Dark",
                                primaryColor = Color(0xFFB8C4FF),
                                backgroundColor = Color(0xFF121318),
                                selected = currentThemeMode == "dark",
                                onClick = { onThemeModeChange("dark") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePreviewItem(
                                name = "AMOLED",
                                primaryColor = Color(0xFFB8C4FF),
                                backgroundColor = Color(0xFF000000),
                                selected = currentThemeMode == "amoled",
                                onClick = { onThemeModeChange("amoled") },
                                modifier = Modifier.weight(1f)
                            )
                            ThemePreviewItem(
                                name = "Sakura",
                                primaryColor = Color(0xFFC2185B),
                                backgroundColor = Color(0xFFFFF0F5),
                                selected = currentThemeMode == "sakura",
                                onClick = { onThemeModeChange("sakura") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Text(
                            "Daily Study Goal",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(5, 10, 20).forEach { mins ->
                                val selected = studyGoal == mins
                                Surface(
                                    onClick = {
                                        onStudyGoalChange(mins)
                                        context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                            .edit().putInt("study_goal_minutes", mins).apply()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Text(
                                            "$mins",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "min/day",
                                            fontSize = 11.sp,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                    else MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // ── CARD 2: Display & Reading ──────────────────────────────────
                item {
                    OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CardHeader(
                            icon = Icons.Default.List,
                            title = "Display & Reading",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Translation Language",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("en" to "English", "bn" to "বাংলা (Bangla)").forEach { (langCode, langName) ->
                                val selected = appLanguage == langCode
                                Surface(
                                    onClick = {
                                        onAppLanguageChange(langCode)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                    border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            langName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsToggleRow(
                            label = "Show Romaji",
                            description = "Show romanized pronunciation in Vocab & Kana",
                            checked = romajiEnabled,
                            onCheckedChange = {
                                onRomajiToggle(it)
                                context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("global_romaji", it).apply()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsToggleRow(
                            label = "Show Furigana",
                            description = "Show furigana reading above kanji",
                            checked = furiganaEnabled,
                            onCheckedChange = {
                                onFuriganaToggle(it)
                                context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("global_furigana", it).apply()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        SettingsToggleRow(
                            label = "Disable Kanji (Kana Only)",
                            description = "Replace all Kanji with Hiragana & Katakana across screens & AI features",
                            checked = kanjiDisabled,
                            onCheckedChange = {
                                onKanjiDisabledToggle(it)
                                context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("kanji_disabled", it).apply()
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Font Size Scale", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Adjust readability of kanji/kana", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "あ",
                                        fontSize = (20 * fontScale).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Slider(
                                value = fontScale,
                                onValueChange = { onFontScaleChange(it) },
                                valueRange = 0.85f..1.3f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "${(fontScale * 100).toInt()}% — affects reading texts and dictionaries",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                }

                // ── CARD 3: Notification Reminders ──────────────────────────────
                item {
                    OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CardHeader(
                            icon = Icons.Default.Notifications,
                            title = "Reminders",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        SettingsToggleRow(
                            label = "Daily Study Reminder",
                            description = "Remind me to study at 8:00 PM every day",
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                onNotificationsToggle(enabled)
                                context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("notifications_enabled", enabled).apply()
                                if (enabled) StudyReminderWorker.schedule(context)
                                else StudyReminderWorker.cancel(context)
                            }
                        )
                    }
                }
                }

                // ── CARD 4: Gemini AI Online Features ───────────────────────────
                item {
                    OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CardHeader(
                            icon = Icons.Default.Info,
                            title = "AI Online Features (Gemini)",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Paste your Gemini API key below to dynamically generate N5 reading stories, quizzes, and grammar sentences.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        OutlinedTextField(
                            value = geminiApiKey,
                            onValueChange = { newValue ->
                                geminiApiKey = newValue
                                isApiKeySaved = false
                            },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(
                                        imageVector = if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (apiKeyVisible) "Hide API Key" else "Show API Key"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isApiKeySaved) {
                                Text(
                                    "Saved ✓ ",
                                    color = Color(0xFF43A047),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    prefs.edit().putString("gemini_api_key", geminiApiKey.trim()).apply()
                                    isApiKeySaved = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isApiKeySaved) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isApiKeySaved) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (isApiKeySaved) "Saved" else "Save Key")
                            }
                        }
                    }
                }
                }

                // ── CARD 4.4: Offline Assets Downloader ──────────────────────────────
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CardHeader(
                                icon = Icons.Default.Download,
                                title = "Offline Assets Downloader",
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                "Download all 5 textbooks (PDFs) and 87 listening audio tracks at once for complete offline study (Estimated size: ~180MB).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (com.momin.japanesestudyappn5.util.OnlineAssetsManager.isBulkDownloading) {
                                LinearProgressIndicator(
                                    progress = com.momin.japanesestudyappn5.util.OnlineAssetsManager.bulkDownloadProgress,
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Downloading: ${com.momin.japanesestudyappn5.util.OnlineAssetsManager.bulkDownloadCurrentIndex}/${com.momin.japanesestudyappn5.util.OnlineAssetsManager.bulkDownloadTotalFiles}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${(com.momin.japanesestudyappn5.util.OnlineAssetsManager.bulkDownloadProgress * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "File: ${com.momin.japanesestudyappn5.util.OnlineAssetsManager.bulkDownloadCurrentFile}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        com.momin.japanesestudyappn5.util.OnlineAssetsManager.cancelBulkDownload()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Text("Cancel Download", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                if (isAllDownloaded) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("✓ All 92 assets downloaded! Ready for offline use.", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                 Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            com.momin.japanesestudyappn5.util.OnlineAssetsManager.downloadAllAssets(context)
                                            isAllDownloaded = com.momin.japanesestudyappn5.util.OnlineAssetsManager.isAllDownloaded(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        text = if (isAllDownloaded) "Redownload All Assets" else "Download All Assets",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showDiagnosticsDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📋 View Download Logs & Diagnostics", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // ── CARD 4.5: Complete JLPT N1-N5 Vocabulary & Dictionary Downloader ──────────────
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CardHeader(
                                icon = Icons.Default.Book,
                                title = "Full JLPT N1–N5 Vocabulary & Dictionary",
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("JLPT N5", "JLPT N4", "JLPT N3", "JLPT N2", "JLPT N1").forEach { level ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = level,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                "Download complete offline JLPT N1, N2, N3, N4, and N5 vocabulary database, JMdict definitions, KANJIDIC2 kanji entries, and Tatoeba example sentences directly without increasing initial app size.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (isDictDownloading) {
                                LinearProgressIndicator(
                                    progress = { dictDownloadProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    "Downloading N1–N5 Vocabulary Database... ${(dictDownloadProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                if (isDictDownloaded) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("✓ Full N1–N5 Vocabulary & Dictionary Active!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isDictDownloading = true
                                            dictDownloadProgress = 0f
                                            val result = com.momin.japanesestudyappn5.util.OnlineAssetsManager.downloadDictionary(context) { p ->
                                                dictDownloadProgress = p
                                            }
                                            if (result.isSuccess) {
                                                android.widget.Toast.makeText(context, "Full N1–N5 Vocabulary downloaded successfully! ✓", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                val err = result.exceptionOrNull()?.message ?: "Download failed"
                                                android.widget.Toast.makeText(context, "Download error: $err", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            isDictDownloading = false
                                            isDictDownloaded = com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDictionaryDownloaded(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text(
                                        text = if (isDictDownloaded) "Redownload Full N1–N5 Vocabulary" else "Download Full N1–N5 Vocabulary Database",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // ── CARD 5: Cloud Backup & Sync (Firebase) ─────────────────────
                item {
                    OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CardHeader(
                            icon = Icons.Default.Share,
                            title = "Cloud Backup & Sync",
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            "Synchronize your vocabulary milestones, statistics, and trace exercises across devices dynamically.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            if (currentUser != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Account:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(currentUser.email ?: "Google Account", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Sync Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(syncStatus, fontWeight = FontWeight.Bold, color = Color(0xFF43A047), fontSize = 13.sp)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Button(
                                            onClick = { FirebaseSyncManager.signOut(context) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Sign Out Google Account")
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                Text("Active License Key:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(syncCode, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                                            }
                                            val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                                            IconButton(
                                                onClick = {
                                                    clipboard.setText(androidx.compose.ui.text.buildAnnotatedString { append(syncCode) })
                                                },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Copy code",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("Sync Status:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(syncStatus, fontWeight = FontWeight.Bold, color = Color(0xFF43A047), fontSize = 13.sp)
                                        }

                                        Text(
                                            "Your progress is automatically saved to the cloud under this key.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                FirebaseSyncManager.signOut(context)
                                                val activity = context as? android.app.Activity
                                                activity?.finish()
                                                activity?.startActivity(activity.intent)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ExitToApp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text("Log Out / Switch License Key", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Text(
                                    "Sync progress using another license key",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = restoreCodeInput,
                                        onValueChange = { restoreCodeInput = it; restoreError = ""; restoreSuccess = false },
                                        placeholder = { Text("e.g. N5-XXXXXX") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = {
                                            if (restoreCodeInput.trim().isEmpty()) {
                                                restoreError = "Enter a valid code."
                                                return@Button
                                            }
                                            FirebaseSyncManager.restoreProgress(
                                                context = context,
                                                targetLicenseKey = restoreCodeInput.trim(),
                                                onSuccess = {
                                                    restoreSuccess = true
                                                    restoreError = ""
                                                    restoreCodeInput = ""
                                                },
                                                onFailure = { msg ->
                                                    restoreError = msg
                                                    restoreSuccess = false
                                                }
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Restore")
                                    }
                                }

                                if (restoreError.isNotEmpty()) {
                                    Text(restoreError, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (restoreSuccess) {
                                    Text("Progress successfully restored! ✓ Please restart the app to apply all changes.", color = Color(0xFF43A047), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ── CARD 6: App Version Info ─────────────────────────────
                item {
                    val pkgInfo = remember {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        } catch (e: Exception) { null }
                    }
                    val versionName = pkgInfo?.versionName ?: "2.0.x"
                    val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        pkgInfo?.longVersionCode?.toString() ?: "10"
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo?.versionCode?.toString() ?: "10"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Japanese Study App N5  v$versionName (Build $versionCode)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Auto-Versioned & Open Source on GitHub",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (showDiagnosticsDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val logs = com.momin.japanesestudyappn5.util.OnlineAssetsManager.lastDownloadLog
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = {
                Text("📋 Download Logs & Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Active Base URL: ${com.momin.japanesestudyappn5.util.OnlineAssetsManager.getBaseUrl(context)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = logs,
                                    color = Color(0xFF00FF66),
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs))
                        android.widget.Toast.makeText(context, "Logs copied to clipboard! ✓", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CardHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(tint.copy(alpha = 0.15f), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun ThemePreviewItem(
    name: String,
    primaryColor: Color,
    backgroundColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw left half (background)
                drawRect(
                    color = backgroundColor,
                    size = size.copy(width = size.width / 2f)
                )
                // Draw right half (primary)
                drawRect(
                    color = primaryColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x = size.width / 2f, y = 0f),
                    size = size.copy(width = size.width / 2f)
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
