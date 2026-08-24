package com.momin.japanesestudyappn5

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.momin.japanesestudyappn5.data.DefaultDataRepository
import com.momin.japanesestudyappn5.ui.main.MainScreen
import com.momin.japanesestudyappn5.ui.screens.*
import com.momin.japanesestudyappn5.util.FirebaseSyncManager


@Composable
fun MainNavigation(
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DefaultDataRepository(context.assets) }
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }

    val onboardingDone = remember { prefs.getBoolean("onboarding_done", false) }
    val storedLicenseKey = remember { prefs.getString("validated_license_key", "") ?: "" }
    val isUserLoggedIn = storedLicenseKey.isNotEmpty()

    val isOwnerAdmin = remember { prefs.getBoolean("is_admin_mode", false) }
    val initialKey: NavKey = when {
        isOwnerAdmin -> OwnerDashboard
        !isUserLoggedIn -> Login
        onboardingDone -> Main
        else -> Onboarding
    }
    val backStack = rememberNavBackStack(initialKey)

    androidx.activity.compose.BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    var appLanguage by remember { mutableStateOf(prefs.getString("app_language", null)) }
    var romajiEnabled by remember { mutableStateOf(prefs.getBoolean("global_romaji", true)) }
    var furiganaEnabled by remember { mutableStateOf(prefs.getBoolean("global_furigana", true)) }
    var kanjiDisabled by remember { mutableStateOf(prefs.getBoolean("kanji_disabled", false)) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var studyGoal by remember { mutableIntStateOf(prefs.getInt("study_goal_minutes", 10)) }
    var fontScale by remember { mutableFloatStateOf(prefs.getFloat("font_scale", 1.0f)) }

    Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Login> {
                    LoginScreen(
                        onComplete = {
                            val onboarded = prefs.getBoolean("onboarding_done", false)
                            backStack.removeLastOrNull()
                            if (onboarded) {
                                backStack.add(Main)
                            } else {
                                backStack.add(Onboarding)
                            }
                        },
                        onAdminComplete = {
                            backStack.removeLastOrNull()
                            backStack.add(OwnerDashboard)
                        }
                    )
                }
                entry<OwnerDashboard> {
                    OwnerDashboardScreen(
                        onBack = {
                            FirebaseSyncManager.signOut(context)
                            backStack.removeLastOrNull()
                            backStack.add(Login)
                        }
                    )
                }
                entry<Onboarding> {
                    OnboardingScreen(
                        onComplete = { goalMins ->
                            prefs.edit()
                                .putBoolean("onboarding_done", true)
                                .putInt("study_goal_minutes", goalMins)
                                .apply()
                            studyGoal = goalMins
                            backStack.removeLastOrNull()
                            backStack.add(Main)
                        }
                    )
                }
                entry<Main> {
                    MainScreen(
                        onItemClick = { navKey -> backStack.add(navKey) },
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange,
                        romajiEnabled = romajiEnabled,
                        onRomajiToggle = { romajiEnabled = it },
                        furiganaEnabled = furiganaEnabled,
                        onFuriganaToggle = { furiganaEnabled = it },
                        kanjiDisabled = kanjiDisabled,
                        onKanjiDisabledToggle = { kanjiDisabled = it },
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsToggle = { notificationsEnabled = it },
                        studyGoal = studyGoal,
                        onStudyGoalChange = { studyGoal = it },
                        fontScale = fontScale,
                        onFontScaleChange = {
                            fontScale = it
                            prefs.edit().putFloat("font_scale", it).apply()
                        },
                        appLanguage = appLanguage ?: "en",
                        onAppLanguageChange = {
                            appLanguage = it
                            prefs.edit().putString("app_language", it).apply()
                        },
                        repository = repository
                    )
                }
                entry<KanaLearn> {
                    KanaLearnScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        globalShowRomaji = romajiEnabled
                    )
                }
                entry<Vocabulary> {
                    VocabularyScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        globalShowRomaji = romajiEnabled,
                        globalShowFurigana = furiganaEnabled,
                        appLanguage = appLanguage ?: "en",
                        onTraceClick = { char -> backStack.add(KanaTrace(char)) }
                    )
                }
                entry<AnkiDeck> { args ->
                    AnkiDeckScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        quickMode = args.quickMode,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<KanjiParticles> { args ->
                    KanjiParticlesScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        initialTab = args.initialTab,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<BookReader> { args ->
                    BookReaderScreen(
                        pdfPath = args.pdfPath,
                        title = args.title,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<ExamPractice> {
                    ExamPracticeScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository
                    )
                }
                entry<Grammar> {
                    GrammarScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<Stats> {
                    StatsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        onAchievementsClick = { backStack.add(Achievements) },
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<UniversalSearch> {
                    UniversalSearchScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<VocabQuiz> {
                    VocabQuizScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<DailyChallenge> {
                    DailyChallengeScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<KanaTrace> { args ->
                    KanaTraceScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        initialChar = args.char
                    )
                }
                entry<GrammarExercises> {
                    GrammarExercisesScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository
                    )
                }
                entry<KanaSpeedQuiz> {
                    KanaSpeedQuizScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<Achievements> {
                    AchievementsScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<SentenceBuilder> {
                    SentenceBuilderScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<ReadingPractice> {
                    ReadingPracticeScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository
                    )
                }
                entry<FallingWords> {
                    FallingWordsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<MatchingPairs> {
                    MatchingPairsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<FillBlank> {
                    FillBlankScreen(
                        onBack = { backStack.removeLastOrNull() },
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<AIChat> {
                    AIChatScreen(onBack = { backStack.removeLastOrNull() })
                }
                entry<WeakWords> {
                    WeakWordsScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository,
                        appLanguage = appLanguage ?: "en"
                    )
                }
                entry<StreakSaver> {
                    StreakSaverScreen(
                        onBack = { backStack.removeLastOrNull() },
                        repository = repository
                    )
                }
                entry<QuestShop> {
                    QuestShopScreen(
                        onBack = { backStack.removeLastOrNull() },
                        themeMode = themeMode,
                        onThemeModeChange = onThemeModeChange
                    )
                }
                entry<ParticleGame> {
                    ParticleDragDropScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<CdSection> {
                    CdSectionScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Translation> {
                    TranslationScreen(
                        onBack = { backStack.removeLastOrNull() },
                        appLanguage = appLanguage ?: "en",
                        onTraceClick = { char -> backStack.add(KanaTrace(char)) }
                    )
                }
            }
        )

        if (appLanguage == null) {
            LanguageSelectionStartupOverlay(
                onSelected = { selectedLang ->
                    prefs.edit().putString("app_language", selectedLang).apply()
                    appLanguage = selectedLang
                }
            )
        }
    }
}

@Composable
private fun LanguageSelectionStartupOverlay(onSelected: (String) -> Unit) {
    Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(Color(0xE012131A))
            .clickable(enabled = false) {},
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Card(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(16.dp),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🌐",
                    fontSize = 56.sp
                )
                
                Text(
                    text = "Select Language\nভাষা নির্বাচন করুন",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 28.sp
                )

                Text(
                    text = "Select your primary translation language for vocabulary. Menus and navigation will remain in English.\n\nশব্দার্থ ও বাক্যের অনুবাদ আপনার পছন্দের ভাষায় দেখতে ভাষা নির্বাচন করুন। মেনু ও সেটিংস ইংরেজিতেই থাকবে।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

                Card(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelected("en") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🇬🇧  ", fontSize = 20.sp)
                        Text(
                            "English (Meanings in English)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Card(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelected("bn") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🇧🇩  ", fontSize = 20.sp)
                        Text(
                            "বাংলা (বাংলায় অর্থ)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
