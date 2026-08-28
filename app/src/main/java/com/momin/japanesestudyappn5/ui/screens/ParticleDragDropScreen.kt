package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.AudioPlayer
import java.time.LocalDate
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticleDragDropScreen(
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now().toString() }

    // Sentences Pool
    val allQuestions = remember {
        listOf(
            ParticleQuestion(
                sentenceBefore = "わたし",
                sentenceAfter = "学生 です。",
                correctParticle = "は",
                translation = "I am a student.",
                bangla = "আমি একজন ছাত্র।",
                explanation = "は is the topic marker particle. It indicates that 'わたし' (I) is the main topic of the sentence.",
                options = listOf("が", "は", "を", "に")
            ),
            ParticleQuestion(
                sentenceBefore = "これ は わたし",
                sentenceAfter = "本 です。",
                correctParticle = "の",
                translation = "This is my book.",
                bangla = "এটি আমার বই।",
                explanation = "の indicates possession or association. Here it links わたし (I) to 本 (book) to mean 'my book'.",
                options = listOf("と", "の", "は", "が")
            ),
            ParticleQuestion(
                sentenceBefore = "電車",
                sentenceAfter = "学校 に 行きます。",
                correctParticle = "で",
                translation = "I go to school by train.",
                bangla = "আমি ট্রেনে করে স্কুলে যাই।",
                explanation = "で indicates the method or means of action. Here it shows the train (電車) is the mode of transport.",
                options = listOf("に", "を", "で", "へ")
            ),
            ParticleQuestion(
                sentenceBefore = "水",
                sentenceAfter = "飲みます。",
                correctParticle = "を",
                translation = "I drink water.",
                bangla = "আমি পানি পান করি।",
                explanation = "を is the direct object marker. It links the action 飲みます (drink) to its object 水 (water).",
                options = listOf("が", "は", "に", "を")
            ),
            ParticleQuestion(
                sentenceBefore = "机 の 上",
                sentenceAfter = "本 が あります。",
                correctParticle = "に",
                translation = "There is a book on the desk.",
                bangla = "টেবিলের উপর একটি বই আছে।",
                explanation = "に is used to indicate the location of existence (where something is located) with verbs like あります。",
                options = listOf("で", "に", "が", "は")
            ),
            ParticleQuestion(
                sentenceBefore = "友達",
                sentenceAfter = "一緒に 日本 に 行きます。",
                correctParticle = "と",
                translation = "I go to Japan together with my friend.",
                bangla = "আমি আমার বন্ধুর সাথে জাপানে যাব।",
                explanation = "と is the companion particle, meaning 'with' or 'together with' when followed by 一緒に (together).",
                options = listOf("に", "で", "と", "を")
            ),
            ParticleQuestion(
                sentenceBefore = "教室 に 学生",
                sentenceAfter = "います。",
                correctParticle = "が",
                translation = "There are students in the classroom.",
                bangla = "শ্রেণীকক্ষে ছাত্ররা আছে।",
                explanation = "が marks the subject of existence with active living subjects (います).",
                options = listOf("は", "が", "を", "に")
            ),
            ParticleQuestion(
                sentenceBefore = "昨日 どこ",
                sentenceAfter = "行きませんでした。",
                correctParticle = "も",
                translation = "I didn't go anywhere yesterday.",
                bangla = "আমি গতকাল কোথাও যাইনি।",
                explanation = "も combined with question words (like どこ) and a negative verb indicates complete negation ('nowhere').",
                options = listOf("へ", "も", "に", "が")
            ),
            ParticleQuestion(
                sentenceBefore = "図書館",
                sentenceAfter = "本 を 読みました。",
                correctParticle = "で",
                translation = "I read a book at the library.",
                bangla = "আমি লাইব্রেরিতে বই পড়েছি।",
                explanation = "で marks the location where an action takes place, as opposed to に which marks state of existence.",
                options = listOf("に", "へ", "で", "の")
            ),
            ParticleQuestion(
                sentenceBefore = "来週 友達",
                sentenceAfter = "会います。",
                correctParticle = "に",
                translation = "I will meet a friend next week.",
                bangla = "আমি আগামী সপ্তাহে বন্ধুর সাথে দেখা করব।",
                explanation = "に marks the target or person you meet when using the verb 会います (meet).",
                options = listOf("を", "に", "と", "が")
            )
        )
    }

    // Play state
    var gameQuestions by remember { mutableStateOf<List<ParticleQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isAnswerCorrect by remember { mutableStateOf<Boolean?>(null) }
    var gameFinished by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var isAiGame by remember { mutableStateOf(false) }
    var isAiLoading by remember { mutableStateOf(false) }
    var aiTopicInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val appLanguage = prefs.getString("app_language", "en") ?: "en"

    fun startNewGame() {
        gameQuestions = allQuestions.shuffled().take(5).map { q ->
            q.copy(options = q.options.shuffled())
        }
        currentIndex = 0
        score = 0
        selectedAnswer = null
        isAnswerCorrect = null
        gameFinished = false
        gameStarted = true
        isAiGame = false
    }

    fun startAiGame(questions: List<ParticleQuestion>) {
        gameQuestions = questions.map { q ->
            q.copy(options = q.options.shuffled())
        }
        currentIndex = 0
        score = 0
        selectedAnswer = null
        isAnswerCorrect = null
        gameFinished = false
        gameStarted = true
        isAiGame = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌸 Particle Matcher", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (gameStarted || gameFinished || isAiLoading) {
                            gameStarted = false
                            gameFinished = false
                            isAiLoading = false
                            gameQuestions = emptyList()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isAiLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI is writing custom sentences...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Formatting N5 grammar with custom topic: \"$aiTopicInput\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                !gameStarted && !gameFinished -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🌸 Particle Matcher", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Master N5 grammar particles with contextual exercises.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }

                        // Mode 1: Standard
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎲", fontSize = 28.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Standard Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text("Play curated N5 questions", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            startNewGame()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Play Standard Game", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Mode 2: AI Custom Topic
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🤖", fontSize = 28.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("AI Topic Matcher", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Text("Generate exercises on any custom topic", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = aiTopicInput,
                                        onValueChange = { aiTopicInput = it },
                                        placeholder = { Text("e.g. Dining at Ramen shop, Train travel") },
                                        label = { Text("Enter Topic") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            if (aiTopicInput.isBlank()) {
                                                android.widget.Toast.makeText(context, "Please enter a topic first!", android.widget.Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isAiLoading = true
                                            coroutineScope.launch {
                                                val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                                                val questions = com.momin.japanesestudyappn5.util.AIGenerator.generateCustomParticleQuestions(apiKey, aiTopicInput)
                                                isAiLoading = false
                                                if (!questions.isNullOrEmpty()) {
                                                    startAiGame(questions)
                                                } else {
                                                    android.widget.Toast.makeText(context, "AI Generation failed. Check API key/internet.", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("✨ Generate AI Exercises", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                gameFinished -> {
                    // Result display
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🌸", fontSize = 80.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Game Completed!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Score: $score / 5 correct answers",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (score >= 4) Color(0xFF43A047) else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "+10 Sakura Coins unlocked in Shop!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = {
                                if (isAiGame) {
                                    isAiLoading = true
                                    coroutineScope.launch {
                                        val apiKey = prefs.getString("gemini_api_key", "") ?: ""
                                        val questions = com.momin.japanesestudyappn5.util.AIGenerator.generateCustomParticleQuestions(apiKey, aiTopicInput)
                                        isAiLoading = false
                                        if (!questions.isNullOrEmpty()) {
                                            startAiGame(questions)
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to regenerate. Check internet.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    startNewGame()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Play Again", fontSize = 16.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                gameStarted = false
                                gameFinished = false
                                gameQuestions = emptyList()
                                aiTopicInput = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Change Game Mode", fontSize = 16.sp)
                        }
                    }
                }

                gameQuestions.isNotEmpty() && currentIndex < gameQuestions.size -> {
                    val q = gameQuestions[currentIndex]

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Game Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Question ${currentIndex + 1} / 5",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Score: $score",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / 5 },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Sentence Box with Blank
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = q.sentenceBefore,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.width(8.dp))

                                    // Blank space slot
                                    val blankBg = when {
                                        selectedAnswer == null -> MaterialTheme.colorScheme.primaryContainer
                                        isAnswerCorrect == true -> Color(0xFFE8F5E9)
                                        else -> Color(0xFFFFEBEE)
                                    }
                                    val blankBorderColor = when {
                                        selectedAnswer == null -> MaterialTheme.colorScheme.primary
                                        isAnswerCorrect == true -> Color(0xFF43A047)
                                        else -> Color(0xFFEF5350)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(width = 60.dp, height = 48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(blankBg)
                                            .border(BorderStroke(2.dp, blankBorderColor), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedAnswer ?: "?",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when {
                                                selectedAnswer == null -> MaterialTheme.colorScheme.onPrimaryContainer
                                                isAnswerCorrect == true -> Color(0xFF2E7D32)
                                                else -> Color(0xFFC62828)
                                            }
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = q.sentenceAfter,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(20.dp))

                                // Translations
                                Text(
                                    text = if (appLanguage == "bn") q.bangla else q.translation,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Interactive choices bubbles
                        Text(
                            text = "Tap the correct particle:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            q.options.forEach { option ->
                                val optionSelected = selectedAnswer == option
                                val optionBg = when {
                                    !optionSelected -> MaterialTheme.colorScheme.surface
                                    isAnswerCorrect == true -> Color(0xFF43A047)
                                    else -> Color(0xFFEF5350)
                                }
                                val optionTextColor = when {
                                    !optionSelected -> MaterialTheme.colorScheme.primary
                                    else -> Color.White
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(optionBg)
                                        .border(
                                            BorderStroke(
                                                2.dp,
                                                if (optionSelected) Color.Transparent else MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.3f
                                                )
                                            ), CircleShape
                                        )
                                        .clickable(enabled = selectedAnswer == null) {
                                            selectedAnswer = option
                                            val correct = option == q.correctParticle
                                            isAnswerCorrect = correct
                                            if (correct) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                score++
                                            } else {
                                                haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                            }
                                            AudioPlayer.speakJapanese(
                                                q.sentenceBefore.replace(" ", "") + option + q.sentenceAfter.replace(
                                                    " ",
                                                    ""
                                                )
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = optionTextColor
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // Explanation and Next button
                        if (selectedAnswer != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isAnswerCorrect == true) "✅ Correct!" else "❌ Incorrect",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isAnswerCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = q.explanation,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            if (currentIndex + 1 < gameQuestions.size) {
                                                currentIndex++
                                                selectedAnswer = null
                                                isAnswerCorrect = null
                                            } else {
                                                // Complete game, record coins & quest progress
                                                val currentCoins = prefs.getInt("sakura_coins", 0)
                                                prefs.edit()
                                                    .putInt("sakura_coins", currentCoins + 10)
                                                    .apply()

                                                if (!prefs.getBoolean("quest_grammar_done_$today", false)) {
                                                    prefs.edit()
                                                        .putBoolean("quest_grammar_done_$today", true)
                                                        .putInt("xp_total", prefs.getInt("xp_total", 0) + 20)
                                                        .apply()
                                                }
                                                gameFinished = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text(
                                            text = if (currentIndex + 1 < gameQuestions.size) "Next Sentence →" else "Finish Game",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

data class ParticleQuestion(
    val sentenceBefore: String,
    val sentenceAfter: String,
    val correctParticle: String,
    val translation: String,
    val bangla: String,
    val explanation: String,
    val options: List<String>
)
