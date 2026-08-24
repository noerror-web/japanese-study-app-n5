package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestShopScreen(
    onBack: () -> Unit,
    themeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now().toString() }

    // Coins wallet
    var coins by remember { mutableIntStateOf(prefs.getInt("sakura_coins", 0)) }

    // Quests states
    var speakDone by remember { mutableStateOf(prefs.getBoolean("quest_speak_done_$today", false)) }
    var traceDone by remember { mutableStateOf(prefs.getBoolean("quest_trace_done_$today", false)) }
    var quizDone by remember { mutableStateOf(prefs.getBoolean("quest_quiz_done_$today", false)) }
    var grammarDone by remember { mutableStateOf(prefs.getBoolean("quest_grammar_done_$today", false)) }

    // Themes unlock states
    var sakuraUnlocked by remember { mutableStateOf(prefs.getBoolean("theme_sakura_unlocked", false)) }
    var tokyoNightUnlocked by remember { mutableStateOf(prefs.getBoolean("theme_tokyonight_unlocked", false)) }
    var retroArcadeUnlocked by remember { mutableStateOf(prefs.getBoolean("theme_retroarcade_unlocked", false)) }

    // Refresh wallet function
    fun refreshWallet() {
        coins = prefs.getInt("sakura_coins", 0)
    }

    LaunchedEffect(today) {
        // Double check values
        speakDone = prefs.getBoolean("quest_speak_done_$today", false)
        traceDone = prefs.getBoolean("quest_trace_done_$today", false)
        quizDone = prefs.getBoolean("quest_quiz_done_$today", false)
        grammarDone = prefs.getBoolean("quest_grammar_done_$today", false)
        sakuraUnlocked = prefs.getBoolean("theme_sakura_unlocked", false)
        tokyoNightUnlocked = prefs.getBoolean("theme_tokyonight_unlocked", false)
        retroArcadeUnlocked = prefs.getBoolean("theme_retroarcade_unlocked", false)
        refreshWallet()
    }

    // List of themes
    val themeItems = remember(sakuraUnlocked, tokyoNightUnlocked, retroArcadeUnlocked, themeMode) {
        listOf(
            ThemeShopItem(
                id = "light",
                name = "Classic Light",
                description = "Clean & elegant indigo aesthetic",
                cost = 0,
                isUnlocked = true,
                gradient = Brush.linearGradient(listOf(Color(0xFF3D5193), Color(0xFFFAF9F7)))
            ),
            ThemeShopItem(
                id = "dark",
                name = "Classic Dark",
                description = "Easy on the eyes for night study",
                cost = 0,
                isUnlocked = true,
                gradient = Brush.linearGradient(listOf(Color(0xFF1A1C24), Color(0xFF2C3E72)))
            ),
            ThemeShopItem(
                id = "amoled",
                name = "AMOLED Black",
                description = "Pure black pixels for battery saving",
                cost = 0,
                isUnlocked = true,
                gradient = Brush.linearGradient(listOf(Color.Black, Color(0xFF1C1C1C)))
            ),
            ThemeShopItem(
                id = "sakura",
                name = "🌸 Sakura Breeze",
                description = "Soft cherry-blossom pink theme",
                cost = 30,
                isUnlocked = sakuraUnlocked,
                gradient = Brush.linearGradient(listOf(Color(0xFFC2185B), Color(0xFFFFF0F5)))
            ),
            ThemeShopItem(
                id = "tokyonight",
                name = "🌌 Tokyo Night",
                description = "Deep neon cybernetic dark theme",
                cost = 80,
                isUnlocked = tokyoNightUnlocked,
                gradient = Brush.linearGradient(listOf(Color(0xFF1F2335), Color(0xFF7AA2F7)))
            ),
            ThemeShopItem(
                id = "retroarcade",
                name = "🕹️ Retro Arcade",
                description = "High contrast CRT terminal green",
                cost = 100,
                isUnlocked = retroArcadeUnlocked,
                gradient = Brush.linearGradient(listOf(Color.Black, Color(0xFF00FF00)))
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quest Board & Shop", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Wallet Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sakura Wallet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Earn coins by completing daily quests!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }

                    // Coins Badge with rotation effect
                    val infiniteTransition = rememberInfiniteTransition(label = "coinSpin")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 0.95f,
                        targetValue = 1.05f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "coinPulse"
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🌸",
                            fontSize = 28.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "$coins",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DAILY QUESTS SECTION
                item {
                    Text(
                        text = "📅 Daily Quests (Resets Daily)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Quest Item: Speak
                item {
                    QuestCard(
                        title = "🎙️ Accent Shadowing",
                        description = "Practice speaking Japanese. Speak one word via the micro button.",
                        reward = "10 Coins + 20 XP",
                        isDone = speakDone,
                        onCompleteCheat = {
                            if (!speakDone) {
                                prefs.edit().putBoolean("quest_speak_done_$today", true)
                                    .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                    .putInt("xp_total", prefs.getInt("xp_total", 0) + 20).apply()
                                speakDone = true
                                refreshWallet()
                            }
                        }
                    )
                }

                // Quest Item: Trace
                item {
                    QuestCard(
                        title = "📝 Writing Tracing",
                        description = "Trace any Kana letter in Writing Practice and click Next.",
                        reward = "10 Coins + 20 XP",
                        isDone = traceDone,
                        onCompleteCheat = {
                            if (!traceDone) {
                                prefs.edit().putBoolean("quest_trace_done_$today", true)
                                    .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                    .putInt("xp_total", prefs.getInt("xp_total", 0) + 20).apply()
                                traceDone = true
                                refreshWallet()
                            }
                        }
                    )
                }

                // Quest Item: Quiz
                item {
                    QuestCard(
                        title = "🧠 Vocabulary Quiz",
                        description = "Complete one full Vocabulary Quiz.",
                        reward = "10 Coins + 20 XP",
                        isDone = quizDone,
                        onCompleteCheat = {
                            if (!quizDone) {
                                prefs.edit().putBoolean("quest_quiz_done_$today", true)
                                    .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                    .putInt("xp_total", prefs.getInt("xp_total", 0) + 20).apply()
                                quizDone = true
                                refreshWallet()
                            }
                        }
                    )
                }

                // Quest Item: Grammar (Particle Matching)
                item {
                    QuestCard(
                        title = "🌸 Particle Matcher",
                        description = "Complete one exercise in the Particle Matcher game.",
                        reward = "10 Coins + 20 XP",
                        isDone = grammarDone,
                        onCompleteCheat = {
                            if (!grammarDone) {
                                prefs.edit().putBoolean("quest_grammar_done_$today", true)
                                    .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                    .putInt("xp_total", prefs.getInt("xp_total", 0) + 20).apply()
                                grammarDone = true
                                refreshWallet()
                            }
                        }
                    )
                }

                // THEME SHOP SECTION
                item {
                    Text(
                        text = "🛍️ Unlock Custom Themes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }

                items(themeItems, key = { it.id }) { item ->
                    ThemeCard(
                        item = item,
                        currentSelected = themeMode == item.id,
                        onUnlock = {
                            if (coins >= item.cost) {
                                val newCoins = coins - item.cost
                                val key = "theme_${item.id}_unlocked"
                                prefs.edit()
                                    .putInt("sakura_coins", newCoins)
                                    .putBoolean(key, true)
                                    .apply()
                                refreshWallet()
                                // Update local state
                                if (item.id == "sakura") sakuraUnlocked = true
                                if (item.id == "tokyonight") tokyoNightUnlocked = true
                                if (item.id == "retroarcade") retroArcadeUnlocked = true
                            }
                        },
                        onSelect = {
                            onThemeModeChange(item.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuestCard(
    title: String,
    description: String,
    reward: String,
    isDone: Boolean,
    onCompleteCheat: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isDone) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Icon
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎁 Reward: $reward",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (isDone) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF43A047),
                    modifier = Modifier.size(36.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onCompleteCheat() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏳", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    item: ThemeShopItem,
    currentSelected: Boolean,
    onUnlock: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            if (currentSelected) 2.5.dp else 1.dp,
            if (currentSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preview Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.gradient)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            when {
                currentSelected -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("Active", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                item.isUnlocked -> {
                    Button(onClick = onSelect) {
                        Text("Use Theme", fontSize = 12.sp)
                    }
                }
                else -> {
                    Button(
                        onClick = onUnlock,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD1F2D))
                    ) {
                        Text("🌸 Unlock (${item.cost})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

data class ThemeShopItem(
    val id: String,
    val name: String,
    val description: String,
    val cost: Int,
    val isUnlocked: Boolean,
    val gradient: Brush
)
