package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.momin.japanesestudyappn5.data.model.KanaData
import com.momin.japanesestudyappn5.data.model.KanaItem
import kotlinx.coroutines.delay

private const val TIME_PER_QUESTION = 5 // seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaSpeedQuizScreen(onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", android.content.Context.MODE_PRIVATE) }

    val allKana: List<KanaItem> = remember {
        (KanaData.hiraganaBasic + KanaData.hiraganaDakuten +
                KanaData.hiraganaHandakuten + KanaData.hiraganaCombination +
                KanaData.katakanaBasic + KanaData.katakanaDakuten +
                KanaData.katakanaHandakuten + KanaData.katakanaCombination)
            .filterNotNull().distinctBy { it.char }
    }

    var isStarted by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var totalAnswered by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableFloatStateOf(TIME_PER_QUESTION.toFloat()) }
    var options by remember { mutableStateOf<List<KanaItem>>(emptyList()) }
    var selectedAnswer by remember { mutableStateOf<KanaItem?>(null) }
    var showFeedback by remember { mutableStateOf(false) }
    var shuffledQuestions by remember { mutableStateOf<List<KanaItem>>(emptyList()) }

    fun buildOptions(correct: KanaItem): List<KanaItem> {
        val wrongs = allKana.filter { it.romaji != correct.romaji }.shuffled().take(3)
        return (wrongs + correct).shuffled()
    }

    fun startQuiz() {
        shuffledQuestions = allKana.shuffled().take(20)
        currentIndex = 0
        score = 0
        totalAnswered = 0
        timeLeft = TIME_PER_QUESTION.toFloat()
        selectedAnswer = null
        showFeedback = false
        if (shuffledQuestions.isNotEmpty()) {
            options = buildOptions(shuffledQuestions[0])
        }
        isStarted = true
        isFinished = false
    }

    fun nextQuestion() {
        if (currentIndex + 1 >= shuffledQuestions.size) {
            isFinished = true
            saveQuizScore(prefs, "Kana Speed Quiz", score, shuffledQuestions.size)
        } else {
            currentIndex++
            timeLeft = TIME_PER_QUESTION.toFloat()
            selectedAnswer = null
            showFeedback = false
            options = buildOptions(shuffledQuestions[currentIndex])
        }
    }

    // Timer
    LaunchedEffect(isStarted, currentIndex, showFeedback) {
        if (!isStarted || isFinished || showFeedback) return@LaunchedEffect
        while (timeLeft > 0f) {
            delay(100L)
            timeLeft -= 0.1f
        }
        // Time's up
        if (!showFeedback) {
            totalAnswered++
            showFeedback = true
            delay(1200L)
            nextQuestion()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("⚡ Kana Speed Quiz", fontWeight = FontWeight.Bold)
                        if (isStarted && !isFinished) {
                            Text("${currentIndex + 1} / ${shuffledQuestions.size}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                        }
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !isStarted -> {
                    // Start screen
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⚡", fontSize = 80.sp)
                        Spacer(Modifier.height(24.dp))
                        Text("Kana Speed Quiz", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text("20 kana characters — 5 seconds each.\nTap the correct romaji before time runs out!",
                            fontSize = 15.sp, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                        Spacer(Modifier.height(40.dp))
                        Button(onClick = { startQuiz() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)) {
                            Text("Start Quiz →", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                isFinished -> {
                    // Results
                    val accuracy = if (shuffledQuestions.isNotEmpty())
                        score * 100 / shuffledQuestions.size else 0
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(when {
                            accuracy >= 90 -> "🏆 Perfect!"
                            accuracy >= 70 -> "🎉 Great job!"
                            accuracy >= 50 -> "👍 Good try!"
                            else -> "📚 Keep practicing!"
                        }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(16.dp))
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$accuracy%", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        accuracy >= 70 -> Color(0xFF2E7D32)
                                        accuracy >= 50 -> Color(0xFFF57F17)
                                        else -> Color(0xFFC62828)
                                    })
                                Text("Accuracy", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { accuracy / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = when {
                                        accuracy >= 70 -> Color(0xFF43A047)
                                        accuracy >= 50 -> Color(0xFFFFB300)
                                        else -> Color(0xFFEF5350)
                                    }
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("✅", fontSize = 24.sp)
                                        Text("$score", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                        Text("Correct", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("❌", fontSize = 24.sp)
                                        Text("${shuffledQuestions.size - score}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                        Text("Wrong", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("📝", fontSize = 24.sp)
                                        Text("${shuffledQuestions.size}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                        Text("Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(28.dp))
                        Button(onClick = { startQuiz() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Try Again", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Back to Home")
                        }
                    }
                }

                shuffledQuestions.isNotEmpty() -> {
                    val question = shuffledQuestions[currentIndex]
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Timer bar
                        val timerColor by animateColorAsState(
                            targetValue = when {
                                timeLeft > 3f -> Color(0xFF43A047)
                                timeLeft > 1.5f -> Color(0xFFFFB300)
                                else -> Color(0xFFEF5350)
                            }, label = "timer_color"
                        )
                        LinearProgressIndicator(
                            progress = { (timeLeft / TIME_PER_QUESTION).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = timerColor
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${timeLeft.toInt() + 1}s", fontSize = 12.sp,
                            color = timerColor, fontWeight = FontWeight.Bold)

                        Spacer(Modifier.height(24.dp))

                        // Kana display
                        Card(modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(6.dp)) {
                            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                                Text(question.char, fontSize = 120.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.height(28.dp))
                        Text("What is the romaji for this character?",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))

                        // Option buttons (2x2 grid)
                        val chunked = options.chunked(2)
                        chunked.forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { option ->
                                    val isSelected = selectedAnswer?.romaji == option.romaji
                                    val isCorrect = option.romaji == question.romaji
                                    val bgColor = when {
                                        !showFeedback -> MaterialTheme.colorScheme.surfaceVariant
                                        isCorrect -> Color(0xFF43A047)
                                        isSelected -> Color(0xFFEF5350)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                    val textColor = when {
                                        showFeedback && (isCorrect || isSelected) -> Color.White
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f).height(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = !showFeedback) {
                                                selectedAnswer = option
                                                showFeedback = true
                                                totalAnswered++
                                                if (option.romaji == question.romaji) {
                                                    score++
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                        color = bgColor,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(option.romaji, fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold, color = textColor)
                                        }
                                    }
                                }
                                // Fill empty cell if row has only 1 item
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }

                        // Auto-advance after feedback
                        if (showFeedback) {
                            LaunchedEffect(currentIndex) {
                                delay(1200L)
                                nextQuestion()
                            }
                        }
                    }
                }
            }
        }
    }
}
