package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.momin.japanesestudyappn5.util.AIGenerator
import com.momin.japanesestudyappn5.util.FillBlankQuestion
import kotlinx.coroutines.launch

private val STATIC_FILL_BLANK = listOf(
    FillBlankQuestion("わたし は まいにち ___ を たべます。", "わたし は まいにち ___ を たべます。", "I eat [bread] every day.", listOf("パン", "みず", "ほん", "いえ"), 0, "আমি প্রতিদিন [পাউরুটি] খাই।"),
    FillBlankQuestion("これ は ___ です か？", "これ は ___ です か？", "Is this a [book]?", listOf("ねこ", "ほん", "くるま", "いぬ"), 1, "এটি কি একটি [বই]?"),
    FillBlankQuestion("わたし は がっこう に ___ います。", "わたし は がっこう に ___ います。", "I am [going] to school.", listOf("のみ", "たべ", "いき", "かき"), 2, "আমি স্কুলে [যাচ্ছি]।"),
    FillBlankQuestion("きょう は ___ が います。", "きょう は ___ が います。", "Today [my friend] is here.", listOf("ともだち", "きもの", "みせ", "やま"), 0, "আজ [আমার বন্ধু] এখানে এসেছে।"),
    FillBlankQuestion("わたし の ___ は おおきい です。", "わたし の ___ は おおきい です。", "My [house] is big.", listOf("てがみ", "おかね", "いえ", "みず"), 2, "আমার [বাড়ি] বড়।"),
    FillBlankQuestion("かれ は ___ が じょうず です。", "かれ は ___ が じょうず です。", "He is good at [Japanese].", listOf("えいご", "にほんご", "すうがく", "おんがく"), 1, "সে [জাপানি ভাষা]-য় দক্ষ।"),
    FillBlankQuestion("きのう ___ に いきました。", "きのう ___ に いきました。", "I went to a [restaurant] yesterday.", listOf("としょかん", "レストラン", "びょういん", "こうえん"), 1, "আমি গতকাল একটি [রেস্তোরাঁ]-য় গিয়েছিলাম।"),
    FillBlankQuestion("___ は あかい です。", "___ は あかい です。", "[That apple] is red.", listOf("そのりんご", "このほん", "あのいえ", "このかさ"), 0, "ঐ [আপেলটি] লাল।"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillBlankScreen(
    onBack: () -> Unit,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val apiKey = remember { prefs.getString("gemini_api_key", "") ?: "" }
    val scope = rememberCoroutineScope()

    var questions by remember { mutableStateOf(STATIC_FILL_BLANK) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }
    var isLoadingAI by remember { mutableStateOf(false) }
    var showFurigana by remember { mutableStateOf(false) }

    val current = questions.getOrNull(currentIndex)

    fun loadAIQuestions() {
        isLoadingAI = true
        scope.launch {
            val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
            val aiQuestions = AIGenerator.generateFillBlankSentences(apiKey, 8, kanjiDisabled = isKanjiOff)
            if (!aiQuestions.isNullOrEmpty()) {
                questions = aiQuestions
                currentIndex = 0
                selectedAnswer = null
                score = 0
                isComplete = false
            }
            isLoadingAI = false
        }
    }

    fun onAnswer(optionIndex: Int) {
        if (selectedAnswer != null) return
        selectedAnswer = optionIndex
        if (optionIndex == current?.correctIndex) score++
    }

    fun nextQuestion() {
        if (currentIndex + 1 >= questions.size) {
            isComplete = true
            saveQuizScore(prefs, "Fill in the Blank", score, questions.size)
        } else {
            currentIndex++
            selectedAnswer = null
            showFurigana = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🟩 Fill in the Blank", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    TextButton(onClick = { loadAIQuestions() }, enabled = !isLoadingAI) {
                        if (isLoadingAI) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("✨ AI Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(Color(0xFFF1F8E9), Color(0xFFE8F5E9))))
        ) {
            if (isComplete) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val emoji = when {
                        score >= questions.size * 0.8 -> "🏆"
                        score >= questions.size * 0.5 -> "😊"
                        else -> "📚"
                    }
                    Text(emoji, fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Quiz Complete!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                    Spacer(Modifier.height(8.dp))
                    Text("Score: $score / ${questions.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { questions = STATIC_FILL_BLANK; currentIndex = 0; selectedAnswer = null; score = 0; isComplete = false },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("🔄  Play Again", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("← Back")
                    }
                }
            } else if (current != null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Progress
                    item {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Question ${currentIndex + 1} of ${questions.size}", fontSize = 12.sp, color = Color(0xFF555555))
                                Text("Score: $score", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / questions.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF43A047)
                            )
                        }
                    }

                    // Sentence card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                val displaySentence = if (isKanjiOff) current.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(current.sentence) } else current.sentence
                                val finalSentence = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(displaySentence) else displaySentence
                                Text("Fill in the blank:", fontSize = 12.sp, color = Color(0xFF888888))
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = finalSentence,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = Color(0xFF1A1A1A),
                                    lineHeight = 30.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                if (!isKanjiOff && showFurigana) {
                                    Text(current.furigana, fontSize = 13.sp, color = Color(0xFF666666), textAlign = TextAlign.Center)
                                }
                                if (!isKanjiOff) {
                                    TextButton(onClick = { showFurigana = !showFurigana }) {
                                        Text(if (showFurigana) "Hide furigana" else "Show furigana", fontSize = 11.sp, color = Color(0xFF43A047))
                                    }
                                }
                                if (selectedAnswer != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFE8F5E9)) {
                                        val displayHint = if (appLanguage == "bn" && !current.bangla.isNullOrBlank()) {
                                            current.bangla
                                        } else {
                                            current.english
                                        }
                                        Text(
                                            "💡 $displayHint",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontSize = 13.sp, color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Options
                    item {
                        Text("Choose the correct word:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                    }
                    val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                    items(current.options.size) { i ->
                        val rawOpt = current.options[i]
                        val opt = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(rawOpt) else rawOpt
                        val isCorrect = i == current.correctIndex
                        val isSelected = selectedAnswer == i
                        val bgColor = when {
                            selectedAnswer == null -> Color.White
                            isCorrect -> Color(0xFFE8F5E9)
                            isSelected -> Color(0xFFFFEBEE)
                            else -> Color.White
                        }
                        val borderColor = when {
                            selectedAnswer == null -> Color(0xFFDDDDDD)
                            isCorrect -> Color(0xFF43A047)
                            isSelected -> Color(0xFFEF5350)
                            else -> Color(0xFFDDDDDD)
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                                .clickable(enabled = selectedAnswer == null) { onAnswer(i) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            elevation = CardDefaults.cardElevation(if (selectedAnswer == null) 2.dp else 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        selectedAnswer == null -> ('A' + i).toString()
                                        isCorrect -> "✅"
                                        isSelected -> "❌"
                                        else -> ('A' + i).toString()
                                    },
                                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedAnswer == null) Color(0xFF555555) else if (isCorrect) Color(0xFF2E7D32) else if (isSelected) Color(0xFFC62828) else Color(0xFF999999)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(opt, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                            }
                        }
                    }

                    // Next button
                    if (selectedAnswer != null) {
                        item {
                            Button(
                                onClick = { nextQuestion() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text(if (currentIndex + 1 >= questions.size) "🏁 See Results" else "Next →", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}
