package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Hardcoded grammar exercises covering major N5 patterns
data class GrammarExercise(
    val sentence: String,   // Sentence with ___ for the blank
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val lesson: String,
    val requiredVocab: List<String> = emptyList()
)

private val grammarExercises = listOf(
    GrammarExercise("私 ___ 学生です。", listOf("は", "が", "を", "に"), 0, "は marks the topic of the sentence.", "Particles", listOf("私", "学生")),
    GrammarExercise("コーヒー ___ 飲みます。", listOf("は", "が", "を", "に"), 2, "を marks the direct object of an action.", "Particles", listOf("コーヒー", "飲む", "飲みます")),
    GrammarExercise("学校 ___ 行きます。", listOf("は", "も", "を", "に"), 3, "に marks direction or destination.", "Particles", listOf("学校", "行く", "行きます")),
    GrammarExercise("これ ___ 本です。", listOf("は", "を", "に", "で"), 0, "は marks the topic (this = topic, book = comment).", "Particles", listOf("これ", "本")),
    GrammarExercise("東京 ___ 住んでいます。", listOf("が", "を", "に", "から"), 2, "に marks location of existence/residence.", "Particles", listOf("東京", "住む", "住んでいます")),
    GrammarExercise("友達 ___ 話します。", listOf("を", "と", "が", "は"), 1, "と means 'with' when talking about companions.", "Particles", listOf("友達", "話す", "話します")),
    GrammarExercise("電車 ___ 来ました。", listOf("が", "で", "を", "に"), 1, "で marks the means or method of doing something.", "Particles", listOf("電車", "来る", "来ます", "来ました")),
    GrammarExercise("毎日 ___ 勉強します。", listOf("日本語", "日本語を", "日本語が", "日本語で"), 1, "を marks the direct object — Japanese (language) is studied.", "Particles", listOf("毎日", "勉強", "勉強します", "日本語")),
    GrammarExercise("明日 ___ 来てください。", listOf("早く", "早い", "早", "早かった"), 0, "Use the adverb form (く-form of adjective) to modify verbs.", "Adjectives", listOf("明日", "来る", "来てください")),
    GrammarExercise("この映画は ___ です。", listOf("面白い", "面白く", "面白", "面白くて"), 0, "Plain form adjective before です.", "Adjectives", listOf("映画", "面白い")),
    GrammarExercise("昨日、図書館 ___ 本を読みました。", listOf("が", "を", "に", "で"), 3, "で marks the place where an action occurs.", "Particles", listOf("昨日", "図書館", "本", "読む", "読みました")),
    GrammarExercise("りんごが三つ ___。", listOf("あります", "います", "なります", "します"), 0, "あります is for inanimate objects.", "Existence", listOf("りんご", "三つ")),
    GrammarExercise("ねこが二匹 ___。", listOf("あります", "います", "なります", "します"), 1, "います is for animate beings (animals, people).", "Existence", listOf("ねこ", "二匹")),
    GrammarExercise("今日は暑い ___ 、窓を開けました。", listOf("だから", "でも", "そして", "けれど"), 0, "だから means 'therefore / so' — cause and effect.", "Conjunctions", listOf("今日", "暑い", "窓", "開ける", "開けました")),
    GrammarExercise("日本語は難しい ___、面白いです。", listOf("だから", "でも", "が", "ので"), 2, "が here means 'but' — a soft contrast.", "Conjunctions", listOf("日本語", "難しい", "面白い")),
    GrammarExercise("食べ ___ いません。", listOf("て", "で", "に", "を"), 0, "〜ていません = negative ongoing state (te-form + いません).", "Te-form", listOf("食べる", "食べます")),
    GrammarExercise("今、勉強し ___ います。", listOf("に", "で", "て", "を"), 2, "〜ています = ongoing action or state (te-form + います).", "Te-form", listOf("今", "勉強", "勉強します")),
    GrammarExercise("すみません、駅はどこ ___？", listOf("ですか", "だ", "です", "か"), 0, "どこですか asks 'where is it?' politely.", "Questions", listOf("駅")),
    GrammarExercise("これはいくら ___？", listOf("ですか", "だ", "です", "か"), 0, "いくらですか asks for a price.", "Questions", listOf("これ")),
    GrammarExercise("彼女は先生 ___。", listOf("です", "ます", "だ", "は"), 0, "です is the polite copula (is/are/am).", "Copula", listOf("彼女", "先生")),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarExercisesScreen(
    onBack: () -> Unit,
    repository: DataRepository
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val masteredIds = remember { prefs.getStringSet("mastered_vocab", emptySet()) ?: emptySet() }

    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary()
    }

    val masteredWords = remember(allVocab, masteredIds) {
        allVocab.filter { it.audioId in masteredIds }
            .flatMap { listOf(it.japanese, it.furigana) }
            .toSet()
    }

    var onlyLearnedWordsMode by remember { mutableStateOf(true) }

    val exercises = remember(masteredWords, onlyLearnedWordsMode) {
        val filtered = if (onlyLearnedWordsMode) {
            grammarExercises.filter { exercise ->
                exercise.requiredVocab.all { req ->
                    masteredWords.contains(req) || 
                    masteredWords.any { mastered ->
                        req.contains(mastered) || mastered.contains(req)
                    }
                }
            }
        } else {
            grammarExercises
        }
        
        if (filtered.size >= 3) {
            filtered.shuffled()
        } else {
            grammarExercises.shuffled()
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(onlyLearnedWordsMode) {
        currentIndex = 0
        score = 0
        selectedAnswer = null
        showExplanation = false
        isFinished = false
    }

    LaunchedEffect(selectedAnswer) {
        if (selectedAnswer != null) {
            showExplanation = true
            delay(1800)
            showExplanation = false
            if (currentIndex + 1 < exercises.size) {
                currentIndex++
                selectedAnswer = null
            } else {
                isFinished = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grammar Exercises", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            "Learned Only",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = onlyLearnedWordsMode,
                            onCheckedChange = { onlyLearnedWordsMode = it },
                            modifier = Modifier.scale(0.8f),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00695C),
                                checkedTrackColor = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00695C),
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
            if (isFinished) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val pct = score * 100 / exercises.size
                    Text(if (pct >= 80) "🎉" else "📚", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Exercises Done!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$score / ${exercises.size} correct ($pct%)",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (pct >= 70) Color(0xFF00695C) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Done") }
                }
            } else {
                val ex = exercises[currentIndex]
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF00695C).copy(alpha = 0.1f)
                        ) {
                            Text(
                                ex.lesson,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF00695C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "${currentIndex + 1} / ${exercises.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (currentIndex + 1).toFloat() / exercises.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00695C)
                    )
                    
                    val showFallbackInfo = onlyLearnedWordsMode && grammarExercises.filter { exercise ->
                        exercise.requiredVocab.all { req ->
                            masteredWords.contains(req) || 
                            masteredWords.any { mastered ->
                                req.contains(mastered) || mastered.contains(req)
                            }
                        }
                    }.size < 3

                    if (showFallbackInfo) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "Showing all questions since you haven't mastered enough words yet. Keep studying vocabulary!",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // Sentence card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00695C))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val parts = ex.sentence.split("___")
                            Text(
                                buildAnnotatedString {
                                    append(parts.getOrElse(0) { "" })
                                    withStyle(SpanStyle(
                                        background = Color.White.copy(alpha = 0.3f),
                                        fontWeight = FontWeight.Bold
                                    )) {
                                        append(if (selectedAnswer != null) ex.options[ex.correctIndex] else "　？　")
                                    }
                                    append(parts.getOrElse(1) { "" })
                                },
                                fontSize = 24.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 36.sp
                            )
                        }
                    }

                    // Explanation
                    if (showExplanation) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedAnswer == ex.correctIndex)
                                    Color(0xFF43A047).copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                "💡 ${ex.explanation}",
                                modifier = Modifier.padding(14.dp),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Fill in the blank:", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))

                    ex.options.forEachIndexed { idx, opt ->
                        val isSelected = selectedAnswer == idx
                        val isCorrect = idx == ex.correctIndex
                        val bgColor = when {
                            selectedAnswer == null -> MaterialTheme.colorScheme.surface
                            isCorrect -> Color(0xFF43A047)
                            isSelected -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
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
                                    opt,
                                    color = if (selectedAnswer != null && (isCorrect || isSelected)) Color.White
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
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
