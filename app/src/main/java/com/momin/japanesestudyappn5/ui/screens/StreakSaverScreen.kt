package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
fun StreakSaverScreen(
    onBack: () -> Unit,
    repository: DataRepository
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    
    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var isFinished by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary()
        if (allVocab.isNotEmpty()) {
            val shuffledPool = allVocab.shuffled().take(5)
            questions = shuffledPool.map { item ->
                val wrongs = allVocab.filter { it.audioId != item.audioId }.shuffled().take(3)
                val opts = (wrongs + item).shuffled()
                QuizQuestion(item = item, options = opts, correctIndex = opts.indexOf(item), mode = "jp_to_en")
            }
            started = true
        }
    }

    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            delay(900)
            if (currentIndex + 1 < questions.size) {
                currentIndex++
                selectedAnswer = null
            } else {
                // Save streak & mark as studied today!
                val todayKey = "studied_today_${LocalDate.now()}"
                
                // Retrieve streak details
                val currentStreak = prefs.getInt("streak_count", 1)
                val lastOpenDate = prefs.getString("last_open_date", null)
                val today = LocalDate.now().toString()
                
                // Ensure streak is maintained or extended
                val newStreak = when {
                    lastOpenDate == today -> currentStreak
                    lastOpenDate != null && LocalDate.parse(lastOpenDate).plusDays(1).toString() == today -> currentStreak + 1
                    else -> currentStreak
                }
                
                prefs.edit()
                    .putBoolean(todayKey, true)
                    .putString("last_open_date", today)
                    .putInt("streak_count", newStreak)
                    .apply()
                
                isFinished = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Streak Saver Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE65100),
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
                isFinished -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔥", fontSize = 100.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Streak Saved!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFFE65100)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Score: $score / ${questions.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You extended your daily study streak for today! Keep it going tomorrow! 🗓️",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Back to Dashboard", fontSize = 18.sp, color = Color.White) }
                    }
                }

                !started || questions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFE65100))
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
                                "⚡ 5-Question Speed Run",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE65100),
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
                            color = Color(0xFFE65100)
                        )
                        Spacer(Modifier.height(28.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100))
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        q.item.japanese,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    if (q.item.furigana.isNotBlank() && q.item.furigana != q.item.japanese) {
                                        Text(q.item.furigana, fontSize = 16.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Translate this word:", style = MaterialTheme.typography.bodyMedium,
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
                                        option.english,
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
