package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val todayKey = remember { "daily_challenge_${LocalDate.now()}" }
    val alreadyDone = remember { prefs.getBoolean(todayKey, false) }

    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isFinished by remember { mutableStateOf(alreadyDone) }
    var previousScore by remember { mutableIntStateOf(prefs.getInt("${todayKey}_score", 0)) }
    var started by remember { mutableStateOf(!alreadyDone) }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary()
        if (allVocab.isNotEmpty() && !alreadyDone) {
            val seed = LocalDate.now().dayOfYear
            val rng = java.util.Random(seed.toLong())
            val pool = allVocab.sortedBy { it.audioId }.let { sorted ->
                (0 until minOf(10, sorted.size)).map { i ->
                    sorted[(seed * (i + 1)) % sorted.size]
                }.distinctBy { it.audioId }.take(10)
            }
            questions = pool.map { item ->
                val wrongs = allVocab.filter { it.audioId != item.audioId }.shuffled(rng).take(3)
                val opts = (wrongs + item).shuffled(rng)
                QuizQuestion(item = item, options = opts, correctIndex = opts.indexOf(item), mode = "jp_to_en")
            }
        }
    }

    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            delay(900)
            if (currentIndex + 1 < questions.size) {
                currentIndex++
                selectedAnswer = null
            } else {
                val completedCount = prefs.getInt("daily_challenge_completed_count", 0) + 1
                prefs.edit()
                    .putBoolean(todayKey, true)
                    .putInt("${todayKey}_score", score)
                    .putInt("daily_challenge_completed_count", completedCount)
                    .apply()
                saveQuizScore(prefs, "Daily Challenge", score, questions.size)
                previousScore = score
                isFinished = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Challenge", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6A1B9A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isFinished || alreadyDone -> {
                    var showConfetti by remember { mutableStateOf(true) }
                    if (showConfetti) {
                        ConfettiOverlay(
                            message = "🎉 Daily Challenge Complete!",
                            subMessage = "Great job keeping your daily study streak going!",
                            onDismiss = { showConfetti = false }
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⭐", fontSize = 80.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (alreadyDone && !isFinished) "Already Completed!" else "Challenge Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Today's score: $previousScore / ${questions.size.takeIf { it > 0 } ?: 10}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF6A1B9A),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Come back tomorrow for a new challenge! 🗓️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Back to Home", fontSize = 18.sp) }
                    }
                }

                !started || questions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6A1B9A))
                    }
                }

                else -> {
                    val q = questions[currentIndex]
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📅 Daily Challenge",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6A1B9A),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${currentIndex + 1} / ${questions.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF6A1B9A)
                        )
                        Spacer(Modifier.height(28.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF6A1B9A))
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                    val displayJp = if (isKanjiOff) q.item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(q.item.japanese) } else q.item.japanese
                                    Text(
                                        displayJp,
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    if (!isKanjiOff && q.item.furigana.isNotBlank() && q.item.furigana != q.item.japanese) {
                                        Text(q.item.furigana, fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("What does this mean?", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))

                        q.options.forEachIndexed { idx, option ->
                            val isSelected = selectedAnswer == idx
                            val isCorrect = idx == q.correctIndex
                            val bgColor = when {
                                selectedAnswer == null -> MaterialTheme.colorScheme.surface
                                isCorrect -> Color(0xFF43A047)
                                isSelected -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.surface
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor),
                                onClick = {
                                    if (selectedAnswer == null) {
                                        selectedAnswer = idx
                                        if (isCorrect) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            score++
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (appLanguage == "bn") option.bangla else option.english,
                                        color = if (selectedAnswer != null && (isCorrect || isSelected)) Color.White
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selectedAnswer != null && isCorrect)
                                        Icon(Icons.Default.Check, null, tint = Color.White)
                                    else if (isSelected && !isCorrect)
                                        Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
