package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.KanaData

private data class Badge(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: (prefs: android.content.SharedPreferences) -> Boolean
)

private val ALL_BADGES = listOf(
    Badge("first_open", "🗾", "First Step",
        "Open the app for the first time") { it.getInt("total_opens", 0) >= 1 },
    Badge("streak_3", "🔥", "On Fire",
        "Maintain a 3-day study streak") { it.getInt("streak_count", 0) >= 3 },
    Badge("streak_7", "🏆", "Week Warrior",
        "Maintain a 7-day study streak") { it.getInt("streak_count", 0) >= 7 },
    Badge("vocab_10", "📚", "Word Learner",
        "Master 10 vocabulary words") {
        (it.getStringSet("mastered_vocab", emptySet()) ?: emptySet()).size >= 10 },
    Badge("vocab_50", "📖", "Vocab Star",
        "Master 50 vocabulary words") {
        (it.getStringSet("mastered_vocab", emptySet()) ?: emptySet()).size >= 50 },
    Badge("vocab_100", "🎓", "Vocab Master",
        "Master 100 vocabulary words") {
        (it.getStringSet("mastered_vocab", emptySet()) ?: emptySet()).size >= 100 },
    Badge("bookmark_5", "⭐", "Collector",
        "Bookmark 5 vocabulary words") {
        (it.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()).size >= 5 },
    Badge("hiragana_all", "あ", "Hiragana Hero",
        "Practice all Hiragana characters") { prefs ->
        val practiced = (prefs.getStringSet("practiced_kana", emptySet()) ?: emptySet())
            .count { it.startsWith("h_") }
        val total = KanaData.hiraganaBasic.filterNotNull().size +
                KanaData.hiraganaDakuten.size +
                KanaData.hiraganaHandakuten.size +
                KanaData.hiraganaCombination.size
        practiced >= total
    },
    Badge("katakana_all", "ア", "Katakana Hero",
        "Practice all Katakana characters") { prefs ->
        val practiced = (prefs.getStringSet("practiced_kana", emptySet()) ?: emptySet())
            .count { it.startsWith("k_") }
        val total = KanaData.katakanaBasic.filterNotNull().size +
                KanaData.katakanaDakuten.size +
                KanaData.katakanaHandakuten.size +
                KanaData.katakanaCombination.size
        practiced >= total
    },
    Badge("flashcard_50", "🃏", "Card Shark",
        "Answer 50 flashcards correctly") { prefs ->
        var total = 0
        prefs.all.forEach { (k, v) ->
            if (k.startsWith("score_correct_") && v is Int) total += v
        }
        total >= 50
    },
    Badge("daily_3", "📅", "Daily Champion",
        "Complete 3 Daily Challenges") {
        it.getInt("daily_challenge_completed_count", 0) >= 3 },
    Badge("opens_10", "🔓", "Regular",
        "Open the app 10 times") { it.getInt("total_opens", 0) >= 10 },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }

    val badgeStates = remember {
        ALL_BADGES.map { badge -> badge to badge.isUnlocked(prefs) }
    }

    val unlockedCount = badgeStates.count { it.second }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🏆 Achievements", fontWeight = FontWeight.Bold)
                        Text("$unlockedCount / ${ALL_BADGES.size} unlocked",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Progress card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3264))
                ) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$unlockedCount", fontSize = 56.sp, fontWeight = FontWeight.ExtraBold,
                            color = Color.White)
                        Text("badges unlocked", fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { unlockedCount.toFloat() / ALL_BADGES.size },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .padding(horizontal = 8.dp),
                            color = Color(0xFFFFD700),
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("${ALL_BADGES.size - unlockedCount} badges remaining",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }

            // Unlocked badges
            item {
                Text("✅  Unlocked", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp))
            }
            items(badgeStates.filter { it.second }.map { it.first }) { badge ->
                BadgeCard(badge = badge, unlocked = true)
            }

            // Locked badges
            if (badgeStates.any { !it.second }) {
                item {
                    Text("🔒  Locked", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 12.dp, start = 4.dp))
                }
                items(badgeStates.filter { !it.second }.map { it.first }) { badge ->
                    BadgeCard(badge = badge, unlocked = false)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, unlocked: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (unlocked) Color(0xFFFFD700).copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        if (unlocked) badge.emoji else "🔒",
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    badge.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.outline
                )
                Text(
                    badge.description,
                    fontSize = 12.sp,
                    color = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer.copy(0.7f)
                            else MaterialTheme.colorScheme.outline.copy(0.6f)
                )
            }
            if (unlocked) {
                Text("✅", fontSize = 20.sp)
            }
        }
    }
}
