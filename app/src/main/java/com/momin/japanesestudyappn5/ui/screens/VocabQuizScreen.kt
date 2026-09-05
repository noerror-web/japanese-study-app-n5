package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabQuizScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", android.content.Context.MODE_PRIVATE) }
    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
    val scope = rememberCoroutineScope()

    var aiMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }
    var questionCount by remember { mutableIntStateOf(10) }
    var mode by remember { mutableStateOf("jp_to_en") } // jp_to_en or en_to_jp
    var quizStarted by remember { mutableStateOf(false) }

    val masteredIds = remember { prefs.getStringSet("mastered_vocab", emptySet()) ?: emptySet() }
    var onlyLearned by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { allVocab = repository.getVocabulary() }

    fun generateOfflineQuiz() {
        if (allVocab.isEmpty()) return
        val masteredVocab = allVocab.filter { it.audioId in masteredIds }
        val sourcePool = if (onlyLearned && masteredVocab.size >= 4) masteredVocab else allVocab
        val pool = sourcePool.shuffled().take(questionCount)
        questions = pool.map { item ->
            val wrongAnswers = allVocab.filter { it.audioId != item.audioId }
                .shuffled().take(3)
            val allOptions = (wrongAnswers + item).shuffled()
            QuizQuestion(
                item = item,
                options = allOptions,
                correctIndex = allOptions.indexOf(item),
                mode = mode
            )
        }
        currentIndex = 0
        score = 0
        selectedAnswer = null
        showResult = false
        isFinished = false
        quizStarted = true
    }

    fun generateQuiz() {
        val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
        if (aiMode && !onlyLearned) {
            isLoading = true
            scope.launch {
                val generated = com.momin.japanesestudyappn5.util.AIGenerator.generateVocabQuiz(apiKey, questionCount, mode, kanjiDisabled = isKanjiOff)
                isLoading = false
                if (!generated.isNullOrEmpty()) {
                    questions = generated
                    currentIndex = 0
                    score = 0
                    selectedAnswer = null
                    showResult = false
                    isFinished = false
                    quizStarted = true
                } else {
                    android.widget.Toast.makeText(context, "AI quiz generation failed. Falling back to offline database.", android.widget.Toast.LENGTH_LONG).show()
                    generateOfflineQuiz()
                }
            }
        } else {
            generateOfflineQuiz()
        }
    }

    fun awardXP(points: Int) {
        val current = prefs.getInt("xp_total", 0)
        prefs.edit().putInt("xp_total", current + points).apply()
        recordDailyXp(prefs, points)
    }

    fun markWeakWord(audioId: String) {
        val set = (prefs.getStringSet("weak_words", emptySet()) ?: emptySet()).toMutableSet()
        set.add(audioId)
        prefs.edit().putStringSet("weak_words", set).apply()
    }

    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            if (mode == "en_to_jp" && currentIndex < questions.size) {
                val qItem = questions[currentIndex].item
                val textToPlay = qItem.audioText.ifBlank { qItem.furigana.ifBlank { qItem.japanese } }
                com.momin.japanesestudyappn5.util.AudioPlayer.playTts(context, textToPlay)
            }
            delay(1000)
            if (currentIndex + 1 < questions.size) {
                currentIndex++
                selectedAnswer = null
                showResult = false
            } else {
                isFinished = true
                awardXP(score * 5)
                saveQuizScore(prefs, "Vocab Quiz (${if (mode == "jp_to_en") "JP→EN" else "EN→JP"})", score, questions.size)
                
                val today = java.time.LocalDate.now().toString()
                if (!prefs.getBoolean("quest_quiz_done_$today", false)) {
                    prefs.edit()
                        .putBoolean("quest_quiz_done_$today", true)
                        .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                        .apply()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocab Quiz", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                !quizStarted -> {
                    // Setup screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🧠", fontSize = 64.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Vocabulary Quiz",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Test your knowledge with multiple-choice questions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(32.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(20.dp)) {
                                Text("Number of Questions", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5, 10, 20).forEach { n ->
                                        FilterChip(
                                            selected = questionCount == n,
                                            onClick = { questionCount = n },
                                            label = { Text("$n") }
                                        )
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("Direction", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = mode == "jp_to_en",
                                        onClick = { mode = "jp_to_en" },
                                        label = { Text(if (appLanguage == "bn") "JP → BN" else "JP → EN") }
                                    )
                                    FilterChip(
                                        selected = mode == "en_to_jp",
                                        onClick = { mode = "en_to_jp" },
                                        label = { Text(if (appLanguage == "bn") "BN → JP" else "EN → JP") }
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text("Filter", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            val masteredCount = allVocab.filter { it.audioId in masteredIds }.size
                                            if (masteredCount < 4) {
                                                android.widget.Toast.makeText(context, "You need to master at least 4 words first!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                onlyLearned = !onlyLearned 
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Learned Words Only", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Text("Quiz only on words you've marked as Mastered (Requires >= 4 mastered words)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Switch(
                                        checked = onlyLearned, 
                                        onCheckedChange = { 
                                            val masteredCount = allVocab.filter { it.audioId in masteredIds }.size
                                            if (masteredCount < 4) {
                                                android.widget.Toast.makeText(context, "You need to master at least 4 words first!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                onlyLearned = it 
                                            }
                                        }
                                    )
                                }

                                if (!onlyLearned) {
                                    Spacer(Modifier.height(16.dp))
                                    Text("AI Mode", fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { aiMode = !aiMode },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Generate dynamically via Gemini API", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Switch(checked = aiMode, onCheckedChange = { aiMode = it })
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Generating AI Quiz questions...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            Button(
                                onClick = { generateQuiz() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Start Quiz", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                isFinished -> {
                    // Results screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val pct = score * 100 / questions.size
                        val emoji = when {
                            pct >= 90 -> "🏆"
                            pct >= 70 -> "⭐"
                            pct >= 50 -> "👍"
                            else -> "📚"
                        }
                        Text(emoji, fontSize = 72.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Quiz Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "$score / ${questions.size} correct ($pct%)",
                            style = MaterialTheme.typography.titleLarge,
                            color = when {
                                pct >= 70 -> Color(0xFF43A047)
                                pct >= 50 -> Color(0xFFFFB300)
                                else -> MaterialTheme.colorScheme.error
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { score.toFloat() / questions.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = if (pct >= 70) Color(0xFF43A047) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { generateQuiz() },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Try Again", fontSize = 18.sp) }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("Done", fontSize = 18.sp) }
                    }
                }

                questions.isNotEmpty() -> {
                    // Active quiz
                    val q = questions[currentIndex]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${currentIndex + 1} / ${questions.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Score: $score",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(Modifier.height(28.dp))

                        // Question card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (mode == "jp_to_en") {
                                    val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                    val displayTargetJp = if (isKanjiOff) q.item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(q.item.japanese) } else q.item.japanese
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = displayTargetJp,
                                                fontSize = 40.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                val textToPlay = q.item.audioText.ifBlank { q.item.furigana.ifBlank { q.item.japanese } }
                                                com.momin.japanesestudyappn5.util.AudioPlayer.playTts(context, textToPlay)
                                            }) {
                                                 Text("🔊", fontSize = 24.sp)
                                             }
                                         }
                                         if (!isKanjiOff && q.item.furigana.isNotBlank() && q.item.furigana != q.item.japanese) {
                                             Text(
                                                 text = q.item.furigana,
                                                 fontSize = 14.sp,
                                                 color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                             )
                                         }
                                     }
                                 } else {
                                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                         Text(
                                             text = if (appLanguage == "bn") q.item.bangla else q.item.english,
                                             fontSize = 22.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.onPrimaryContainer,
                                             textAlign = TextAlign.Center
                                         )
                                         if (selectedAnswer != null) {
                                             Spacer(Modifier.height(8.dp))
                                             val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                             val displayTargetJp = if (isKanjiOff) q.item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(q.item.japanese) } else q.item.japanese
                                             Row(
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.Center
                                             ) {
                                                 Text(
                                                     text = displayTargetJp,
                                                     fontSize = 18.sp,
                                                     fontWeight = FontWeight.SemiBold,
                                                     color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                 )
                                                 Spacer(Modifier.width(6.dp))
                                                 IconButton(onClick = {
                                                 val textToPlay = q.item.audioText.ifBlank { q.item.furigana.ifBlank { q.item.japanese } }
                                                 com.momin.japanesestudyappn5.util.AudioPlayer.playTts(context, textToPlay)
                                             }) {
                                                     Text("🔊", fontSize = 18.sp)
                                                 }
                                             }
                                         }
                                     }
                                 }
                             }
                         }
                         Spacer(Modifier.height(24.dp))
                         val targetLang = if (appLanguage == "bn") "Bangla meaning" else "English meaning"
                         Text(
                             "Choose the ${if (mode == "jp_to_en") targetLang else "Japanese word"}:",
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )
                         Spacer(Modifier.height(12.dp))

                         // Answer options
                         val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                         q.options.forEachIndexed { idx, option ->
                             val isSelected = selectedAnswer == idx
                             val isCorrect = idx == q.correctIndex
                             val bgColor = when {
                                 selectedAnswer == null -> MaterialTheme.colorScheme.surface
                                 isCorrect -> Color(0xFF43A047)
                                 isSelected -> MaterialTheme.colorScheme.error
                                 else -> MaterialTheme.colorScheme.surface
                             }
                             val textColor = when {
                                 selectedAnswer != null && (isCorrect || isSelected) -> Color.White
                                 else -> MaterialTheme.colorScheme.onSurface
                             }
                             val optionJpText = if (isKanjiOff) option.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(option.japanese) } else option.japanese
                             Card(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(vertical = 4.dp),
                                 shape = RoundedCornerShape(14.dp),
                                 colors = CardDefaults.cardColors(containerColor = bgColor),
                                 onClick = {
                                     if (selectedAnswer == null) {
                                         selectedAnswer = idx
                                         if (isCorrect) {
                                             haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                             score++
                                         } else {
                                             // Track as weak word
                                             markWeakWord(q.item.audioId)
                                         }
                                     }
                                 }
                             ) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(16.dp),
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                      Text(
                                          text = if (mode == "jp_to_en") (if (appLanguage == "bn") option.bangla else option.english) else optionJpText,
                                          color = textColor,
                                          fontWeight = FontWeight.Medium,
                                          modifier = Modifier.weight(1f),
                                          fontSize = if (mode == "en_to_jp") 22.sp else 16.sp
                                      )
                                    if (selectedAnswer != null && isCorrect) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    } else if (isSelected && !isCorrect) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class QuizQuestion(
    val item: VocabItem,
    val options: List<VocabItem>,
    val correctIndex: Int,
    val mode: String
)
