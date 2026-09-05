package com.momin.japanesestudyappn5.ui.main

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.momin.japanesestudyappn5.*
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.ui.screens.SettingsSheet
import com.momin.japanesestudyappn5.ui.screens.ConfettiOverlay
import com.momin.japanesestudyappn5.util.AudioPlayer
import java.util.Calendar

private val DAILY_TIPS = listOf(
    "💡 Japanese has 3 writing systems: Hiragana (ひらがな), Katakana (カタカナ), and Kanji (漢字).",
    "💡 Hiragana is used for native Japanese words. Katakana is mainly used for foreign loanwords.",
    "💡 The particle は (wa) marks the topic of a sentence — not the subject! It sets context.",
    "💡 Japanese sentence order is Subject → Object → Verb. e.g. 私はりんごを食べます。",
    "💡 Counters matter in Japanese! Different objects use different counting words (本, 枚, 匹…).",
    "💡 Pitch accent can change meaning: 橋 (hashi = bridge) vs 箸 (hashi = chopsticks).",
    "💡 The N5 level covers ~800 vocabulary words and ~100 kanji — you're on the right path! 頑張って！",
)

private data class BookSelectionItem(
    val pdfPath: String,
    val title: String,
    val bnTitle: String,
    val details: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    romajiEnabled: Boolean,
    onRomajiToggle: (Boolean) -> Unit,
    furiganaEnabled: Boolean,
    onFuriganaToggle: (Boolean) -> Unit,
    kanjiDisabled: Boolean = false,
    onKanjiDisabledToggle: (Boolean) -> Unit = {},
    notificationsEnabled: Boolean,
    onNotificationsToggle: (Boolean) -> Unit,
    studyGoal: Int,
    onStudyGoalChange: (Int) -> Unit,
    fontScale: Float = 1.0f,
    onFontScaleChange: (Float) -> Unit = {},
    appLanguage: String = "en",
    onAppLanguageChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    repository: DataRepository
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val streakCount = remember { sharedPrefs.getInt("streak_count", 1) }
    val totalXP = sharedPrefs.getInt("xp_total", 0)
    val currentLevel = (totalXP / 100) + 1

    var showLevelUpLevel by remember { mutableIntStateOf(0) }
    val lastLevel = remember { sharedPrefs.getInt("last_level", 1) }

    LaunchedEffect(currentLevel) {
        if (currentLevel > lastLevel) {
            sharedPrefs.edit().putInt("last_level", currentLevel).apply()
            showLevelUpLevel = currentLevel
            com.momin.japanesestudyappn5.util.GameFeedbackHelper.triggerVictoryHaptic(context)
        }
    }

    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(repository, sharedPrefs)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBookSelectionDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AudioPlayer.ensureTts(context)
    }

    val today = remember { java.time.LocalDate.now().toString() }
    val studiedToday = remember(today) {
        sharedPrefs.getBoolean("studied_today_$today", false) || 
        sharedPrefs.getBoolean("daily_challenge_$today", false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("日本語 Study Hub", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("JLPT N5 — Bengali Edition", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                    }
                },
                actions = {
                    IconButton(onClick = { onItemClick(Dictionary) }) {
                        Icon(Icons.Default.Book, contentDescription = "Dictionary",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = { onItemClick(UniversalSearch) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onItemClick(AnkiDeck(quickMode = true)) },
                icon = { Text("⚡", fontSize = 18.sp) },
                text = { Text("Quick Review", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        }
    ) { innerPadding ->
        when (val s = state) {
            MainScreenUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MainScreenUiState.Success -> {
                MainDashboardContent(
                    stats = s.data,
                    studyGoal = studyGoal,
                    streakCount = streakCount,
                    totalXP = totalXP,
                    currentLevel = currentLevel,
                    onItemClick = onItemClick,
                    onShowBookSelect = { showBookSelectionDialog = true },
                    appLanguage = appLanguage,
                    themeMode = themeMode,
                    studiedToday = studiedToday,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            }
            is MainScreenUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Error: ${s.throwable.message}", color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center)
                }
            }
        }

    }

    // Settings bottom sheet
    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            currentThemeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            notificationsEnabled = notificationsEnabled,
            onNotificationsToggle = onNotificationsToggle,
            romajiEnabled = romajiEnabled,
            onRomajiToggle = onRomajiToggle,
            furiganaEnabled = furiganaEnabled,
            onFuriganaToggle = onFuriganaToggle,
            kanjiDisabled = kanjiDisabled,
            onKanjiDisabledToggle = onKanjiDisabledToggle,
            studyGoal = studyGoal,
            onStudyGoalChange = onStudyGoalChange,
            fontScale = fontScale,
            onFontScaleChange = onFontScaleChange,
            appLanguage = appLanguage,
            onAppLanguageChange = onAppLanguageChange
        )
    }

    // Book selection dialog
    if (showBookSelectionDialog) {
        val books = remember {
            listOf(
                BookSelectionItem("minna_no_nihongo_n5_bangla.pdf", "Minna no Nihongo N5 Bangla (Merged)", "মিন্না নো নিহোঙ্গো বাংলা সংস্করণ (Merged Edition)", "Pages: 624 (Combined Edition)"),
                BookSelectionItem("minna_no_nihongo_n5_2013.pdf", "Minna no Nihongo N5 (2013)", "みんなの日本語 N5 (Standard Textbook)", "Pages: 312 (Full Textbook)"),
                BookSelectionItem("minna_no_nihongo_bangla_vocab.pdf", "Minna no Nihongo Bangla Guide", "মিন্না নো নিহোঙ্গো বাংলা গাইড (Vocab & Grammar)", "Vocabulary, Grammar & Kanji Guide"),
                BookSelectionItem("textbook_lesson_all.pdf", "Easy Japanese 2019 (Bangla)", "সহজে জাপানি ভাষা (২০১৯ সংস্করণ)", "Pages: 163 (Full Lesson Set)"),
                BookSelectionItem("leall_bn_t.pdf", "Easy Japanese 2015 (Bangla)", "সহজে জাপানি ভাষা (২০১৫ সংস্করণ)", "Pages: 61 (Classic Edition)")
            )
        }

        AlertDialog(
            onDismissRequest = { showBookSelectionDialog = false },
            title = { Text("Select Textbook Edition", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a Japanese textbook edition to read offline:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    books.forEach { book ->
                        val isCached = remember(book.pdfPath) {
                            com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDownloaded(context, book.pdfPath)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                showBookSelectionDialog = false
                                onItemClick(BookReader(pdfPath = book.pdfPath, title = book.title))
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(book.title, fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f))
                                    if (isCached) {
                                        Text("✅ Downloaded", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("⬇️ Online", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(book.bnTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                Text(book.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBookSelectionDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLevelUpLevel > 0) {
        ConfettiOverlay(
            message = "🎉 Level Up! 🎉",
            subMessage = "Congratulations! You reached Level $showLevelUpLevel! Keep up the excellent work! 💪",
            onDismiss = { showLevelUpLevel = 0 }
        )
    }

    if (AudioPlayer.showTtsAlert) {
        AlertDialog(
            onDismissRequest = { AudioPlayer.showTtsAlert = false },
            title = { Text("Japanese Speech Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Japanese Text-To-Speech is not installed or supported on this device. Please install or update 'Speech Services by Google' from the Google Play Store to enable Japanese pronunciations.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AudioPlayer.showTtsAlert = false
                        AudioPlayer.openTtsPlayStore(context)
                    }
                ) {
                    Text("Install / Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { AudioPlayer.showTtsAlert = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun MainDashboardContent(
    stats: DashboardStats,
    studyGoal: Int,
    streakCount: Int = 1,
    totalXP: Int,
    currentLevel: Int,
    onItemClick: (NavKey) -> Unit,
    onShowBookSelect: () -> Unit,
    appLanguage: String = "en",
    themeMode: String = "system",
    studiedToday: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayTip = remember {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        DAILY_TIPS[dayOfWeek % DAILY_TIPS.size]
    }
    var wordOfDay by remember { mutableStateOf<VocabItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        wordOfDay = stats.wordOfTheDay
    }

    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Streak Saver Banner
        if (!studiedToday) {
            item {
                val bannerBg = if (isDark) MaterialTheme.colorScheme.errorContainer else Color(0xFFFFF3E0)
                val bannerBorderColor = if (isDark) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f) else Color(0xFFFFB74D)
                val bannerTextColor = if (isDark) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFE65100)
                val bannerButtonBg = if (isDark) MaterialTheme.colorScheme.error else Color(0xFFE65100)
                val bannerButtonText = if (isDark) MaterialTheme.colorScheme.onError else Color.White

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, bannerBorderColor),
                    onClick = { onItemClick(StreakSaver) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Streak in Danger!",
                                fontWeight = FontWeight.Bold,
                                color = bannerTextColor,
                                fontSize = 15.sp
                            )
                            Text(
                                "You haven't studied today. Play the 1-min Streak Saver game now!",
                                color = bannerTextColor.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bannerButtonBg
                        ) {
                            Text(
                                "SAVE",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = bannerButtonText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Hero card
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Box(modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF1E3264), Color(0xFF3D5193), Color(0xFF6B4F9E))))
                    .padding(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ようこそ！ (Welcome)", color = Color.White, fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Surface(color = Color(0xFFFFD700).copy(alpha = 0.9f), shape = RoundedCornerShape(10.dp)) {
                                Text("⭐ Lv.$currentLevel", color = Color(0xFF1A1A1A), fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Study vocabulary, kanji, grammar & exams — all offline in Bengali & English.",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("🎯 $studyGoal min/day", color = Color.White, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                            Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                                Text("✨ $totalXP XP", color = Color.White, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Word of the Day
        item {
            wordOfDay?.let { word ->
                val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                val displayWord = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(word.japanese, word.furigana) else word.japanese
                Card(shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("📖 Word of the Day", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            Spacer(Modifier.height(4.dp))
                            Text(displayWord, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (!isKanjiOff && word.furigana != word.japanese) {
                                Text(word.furigana, fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                            Text(
                                text = if (appLanguage == "bn" && word.bangla.isNotEmpty()) word.bangla else word.english,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(onClick = {
                            AudioPlayer.playTts(context, word.audioText.ifBlank { word.furigana.ifBlank { word.japanese } })
                        }) {
                            Text("🔊", fontSize = 24.sp)
                        }
                    }
                }
            }
        }

        // Daily Tip
        item {
            Card(shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("📚", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp, top = 1.dp))
                    Column {
                        Text("Today's Japanese Tip", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f),
                            letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(todayTip, fontSize = 13.sp, lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }

        // Stats row
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashboardStatCard("📂", "Total Opens", "${stats.totalOpens}", Color(0xFFE8F0FE), Color(0xFF1A73E8), Modifier.weight(1f))
                DashboardStatCard("⭐", "Bookmarks", "${stats.bookmarkedVocabCount}", Color(0xFFFEF7E0), Color(0xFFF9AB00), Modifier.weight(1f))
                DashboardStatCard("🔥", "Day Streak", "$streakCount", Color(0xFFFFEDE0), Color(0xFFE65100), Modifier.weight(1f))
            }
        }

        // Daily Challenge card
        item {
            val context2 = LocalContext.current
            val todayKey = remember { "daily_challenge_${java.time.LocalDate.now()}" }
            val challengeDone = remember {
                context2.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                    .getBoolean(todayKey, false)
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onItemClick(DailyChallenge) },
                colors = CardDefaults.cardColors(
                    containerColor = if (challengeDone) Color(0xFFEDE7F6) else Color(0xFF6A1B9A)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (challengeDone) "✅" else "📅", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (challengeDone) "Daily Challenge Complete!" else "Daily Challenge",
                            fontWeight = FontWeight.Bold,
                            color = if (challengeDone) Color(0xFF6A1B9A) else Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            if (challengeDone) "Come back tomorrow for a new one 🎉" else "10 questions • Fresh every day",
                            color = if (challengeDone) Color(0xFF9C27B0) else Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                    if (!challengeDone) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "START",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Study Modules", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                TextButton(onClick = { onItemClick(Stats) }) {
                    Text("📊 Stats", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Category Selector Tab Row
        item {
            val categoryTabs = listOf(
                "📚 Core Lessons",
                "🎮 Practice & Play",
                "🎧 Books & Audio",
                "🤖 AI & Extras"
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(categoryTabs.size) { index ->
                    val isSelected = selectedTab == index
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        onClick = { selectedTab = index },
                        shape = RoundedCornerShape(12.dp),
                        color = containerColor,
                        contentColor = contentColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = categoryTabs[index],
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Tool grid dynamically switching based on the selected tab
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (selectedTab) {
                    0 -> {
                        // Core Lessons
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🔤", "Kana Study", "ひらがな / カタカナ", "92 characters", Color(0xFFEEFCF4), { onItemClick(KanaLearn) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("📚", "Vocabulary", "Nihongo Lessons", "${stats.totalVocabCount} words", Color(0xFFE8F0FE), { onItemClick(Vocabulary) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("📖", "Grammar", "N5 Bengali Guide", "${stats.totalGrammarCount} lessons", Color(0xFFF4F0FF), { onItemClick(Grammar) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("漢", "Kanji Guide", "N5 Kanji", "${stats.totalKanjiCount} characters", Color(0xFFFFF0F5), { onItemClick(KanjiParticles(initialTab = 0)) }, isDark, Modifier.weight(1f))
                        }
                    }
                    1 -> {
                        // Practice & Play
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🃏", "Anki SRS", "Flashcard Review", "Spaced Repet.", Color(0xFFFFF7EC), { onItemClick(AnkiDeck()) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🧠", "Vocab Quiz", "4-option MCQ", "JP ↔ EN", Color(0xFFE8F5E9), { onItemClick(VocabQuiz) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("✏️", "Writing Practice", "Trace Kana", "Canvas guide", Color(0xFFE3F2FD), { onItemClick(KanaTrace()) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🌸", "Particle Matcher", "Play grammar game", "Daily Quest", Color(0xFFFFF4EC), { onItemClick(ParticleGame) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("⚡", "Speed Quiz", "Kana flash quiz", "5s per card", Color(0xFFFFF9C4), { onItemClick(KanaSpeedQuiz) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🧩", "Sentence Build", "Word tile puzzles", "N5 sentences", Color(0xFFE8EAF6), { onItemClick(SentenceBuilder) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🌧️", "Falling Words", "Catch falling vocab", "3 lives", Color(0xFFE3F2FD), { onItemClick(FallingWords) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🃏", "Match Pairs", "Flip & match cards", "JP ↔ EN", Color(0xFFF3E5F5), { onItemClick(MatchingPairs) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🟩", "Fill in Blank", "Complete sentences", "AI + offline", Color(0xFFE8F5E9), { onItemClick(FillBlank) }, isDark, Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    2 -> {
                        // Books & Audio
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("📄", "PDF Textbooks", "Natively Rendered", "2 Books", Color(0xFFFFF1F4), onShowBookSelect, isDark, Modifier.weight(1f))
                            ToolShortcutCard("💿", "CD Audio", "Listening References", "87 tracks", Color(0xFFECEFF1), { onItemClick(CdSection) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("📖", "Reading", "N5 passages + AI", "AI topics", Color(0xFFE0F2F1), { onItemClick(ReadingPractice) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🟩", "Grammar Exercises", "Fill-in-blank", "N5 patterns", Color(0xFFE0F7FA), { onItemClick(GrammarExercises()) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🔍", "Search All", "Universal Search", "Cross-content", Color(0xFFF3E5F5), { onItemClick(UniversalSearch) }, isDark, Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    3 -> {
                        // AI & Extras
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("💬", "AI Chat Tutor", "Practice conversation", "N5 only", Color(0xFFFFF8E1), { onItemClick(AIChat) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("💪", "Weak Words", "Review mistakes", "Focused drill", Color(0xFFFFEBEE), { onItemClick(WeakWords) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("📅", "Quest & Shop", "Daily check & Theme", "Sakura coins", Color(0xFFFFF0F5), { onItemClick(QuestShop) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("🏆", "Achievements", "Unlock badges", "12 badges", Color(0xFFFFF3E0), { onItemClick(Achievements) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("🌐", "AI Translator", "Instant EN/BN ↔ JP", "Kana only", Color(0xFFE8F0FE), { onItemClick(Translation) }, isDark, Modifier.weight(1f))
                            ToolShortcutCard("📝", "Exam Practice", "Mock Tests MCQs", "${stats.totalExamsCount} Exam Sets", Color(0xFFEAF5FF), { onItemClick(ExamPractice) }, isDark, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            ToolShortcutCard("📕", "Dictionary", "JMdict, Kanji & Tatoeba", "Full Dictionary", Color(0xFFFCE4EC), { onItemClick(Dictionary) }, isDark, Modifier.weight(1f))
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardStatCard(emoji: String, title: String, value: String, color: Color, textColor: Color, modifier: Modifier) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color), modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ToolShortcutCard(
    emoji: String,
    title: String,
    subtitle: String,
    tag: String,
    color: Color,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else color
    val contentTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF172033)
    val subtitleTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color(0xFF63708D)
    val tagBgColor = if (isDark) MaterialTheme.colorScheme.primaryContainer else Color.White.copy(alpha = 0.65f)
    val tagTextColor = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF2C3E61)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(emoji, fontSize = 26.sp)
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = contentTextColor)
                Text(subtitle, fontSize = 10.sp, color = subtitleTextColor)
            }
            Surface(color = tagBgColor, shape = RoundedCornerShape(6.dp)) {
                Text(tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tagTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
}
}

