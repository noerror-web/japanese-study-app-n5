package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.util.GameFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FallingWord(
    val id: Int,
    val vocab: VocabItem,
    val xFraction: Float,   // 0f..1f across screen width
    val options: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallingWordsScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // Game state
    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var level by remember { mutableIntStateOf(1) }
    var gameRunning by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var fallingItems by remember { mutableStateOf<List<FallingWord>>(emptyList()) }
    var idCounter by remember { mutableIntStateOf(0) }
    var selectedWordId by remember { mutableIntStateOf(-1) }
    var feedbackMap by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    // Fall animation — fraction 0f (top) to 1f (bottom) per item, keyed by id
    // We use a map of animated floats
    val fallProgress = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary().shuffled()
    }

    fun spawnWord() {
        if (allVocab.isEmpty()) return
        val vocab = allVocab.random()
        // Build 3 wrong options + 1 correct
        val getTranslation: (VocabItem) -> String = {
            if (appLanguage == "bn") it.bangla else it.english
        }
        val wrongs = allVocab.filter { it.audioId != vocab.audioId }.shuffled().take(3).map(getTranslation)
        val correct = getTranslation(vocab)
        val allOptions = (wrongs + correct).shuffled()
        val newWord = FallingWord(
            id = idCounter++,
            vocab = vocab,
            xFraction = Random.nextFloat().coerceIn(0.05f, 0.75f),
            options = allOptions
        )
        fallingItems = fallingItems + newWord
        val anim = Animatable(0f)
        fallProgress[newWord.id] = anim
        scope.launch {
            val durationMs = (8000L - (level - 1) * 600L).coerceAtLeast(2500L)
            anim.animateTo(1f, animationSpec = tween(durationMs.toInt(), easing = LinearEasing))
            // Reached bottom without being tapped
            if (fallingItems.any { it.id == newWord.id }) {
                lives = (lives - 1).coerceAtLeast(0)
                fallingItems = fallingItems.filter { it.id != newWord.id }
                fallProgress.remove(newWord.id)
                if (lives <= 0) {
                    gameOver = true
                    gameRunning = false
                    if (score >= 5) showConfetti = true
                    GameFeedbackHelper.playFeedbackTone(isSuccess = false)
                    GameFeedbackHelper.triggerHaptic(context, isSuccess = false)
                    saveQuizScore(prefs, "Falling Words", score, score + 1)
                } else {
                    GameFeedbackHelper.playFeedbackTone(isSuccess = false)
                    GameFeedbackHelper.triggerHaptic(context, isSuccess = false)
                }
            }
        }
    }

    // Spawner loop
    LaunchedEffect(gameRunning, level) {
        if (!gameRunning) return@LaunchedEffect
        while (gameRunning) {
            val spawnDelay = (3000L - (level - 1) * 200L).coerceAtLeast(1200L)
            delay(spawnDelay)
            if (fallingItems.size < 3 + level) spawnWord()
        }
    }

    // Level up
    LaunchedEffect(score) {
        if (score > 0 && score % 5 == 0) level = (score / 5) + 1
    }

    fun handleAnswer(word: FallingWord, answer: String) {
        val correct = answer == (if (appLanguage == "bn") word.vocab.bangla else word.vocab.english)
        feedbackMap = feedbackMap + (word.id to correct)
        
        GameFeedbackHelper.playFeedbackTone(isSuccess = correct)
        GameFeedbackHelper.triggerHaptic(context, isSuccess = correct)
        
        scope.launch {
            delay(600)
            if (correct) score++
            else lives = (lives - 1).coerceAtLeast(0)
            fallingItems = fallingItems.filter { it.id != word.id }
            fallProgress.remove(word.id)
            feedbackMap = feedbackMap - word.id
            selectedWordId = -1
            if (lives <= 0) {
                gameOver = true
                gameRunning = false
                if (score >= 5) {
                    showConfetti = true
                    GameFeedbackHelper.playVictoryTone()
                    GameFeedbackHelper.triggerVictoryHaptic(context)
                } else {
                    GameFeedbackHelper.playFeedbackTone(isSuccess = false)
                    GameFeedbackHelper.triggerHaptic(context, isSuccess = false)
                }
                saveQuizScore(prefs, "Falling Words", score, score + 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌧️ Falling Words", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1565C0),
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
                .background(Brush.verticalGradient(listOf(Color(0xFF0D1B4B), Color(0xFF1A237E), Color(0xFF283593))))
        ) {
            when {
                !gameRunning && !gameOver -> {
                    // Start screen
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🌧️", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Falling Words", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        val targetLangText = if (appLanguage == "bn") "Bangla meaning" else "English meaning"
                        Text(
                            "Japanese words fall from the sky!\nTap to select a word, then pick the correct $targetLangText before it hits the ground.",
                            fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("❤️ 3 lives  •  Speed increases each level", fontSize = 13.sp, color = Color(0xFFFFCC80))
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { gameRunning = true; lives = 3; score = 0; level = 1; spawnWord() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                        ) {
                            Text("▶  START GAME", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }
                gameOver -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("💀", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Game Over!", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text("Score: $score", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFCC80))
                        Text("Level reached: $level", fontSize = 16.sp, color = Color.White.copy(0.8f))
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                gameOver = false; gameRunning = true
                                lives = 3; score = 0; level = 1
                                fallingItems = emptyList(); fallProgress.clear()
                                spawnWord()
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                        ) {
                            Text("🔄  PLAY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("← Back", color = Color.White)
                        }
                    }
                }
                else -> {
                    // HUD
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Status bar
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = (1..3).joinToString("") { if (it <= lives) "❤️" else "🖤" },
                                fontSize = 20.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Score: $score  •  Lv.$level",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }

                        // Game field
                        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val fieldHeight = constraints.maxHeight.toFloat()
                            val fieldWidth = constraints.maxWidth.toFloat()
                            val density = LocalDensity.current

                            fallingItems.forEach { word ->
                                val progress = fallProgress[word.id]?.value ?: 0f
                                val yPx = progress * fieldHeight
                                val yDp = with(density) { yPx.toDp() }
                                val xDp = with(density) { (word.xFraction * fieldWidth).toDp() }
                                val isSelected = selectedWordId == word.id
                                val fb = feedbackMap[word.id]

                                val cardColor = when {
                                    fb == true -> Color(0xFF43A047)
                                    fb == false -> Color(0xFFEF5350)
                                    isSelected -> Color(0xFF42A5F5)
                                    else -> Color(0xFF283593)
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = xDp, y = yDp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cardColor)
                                        .border(2.dp, Color.White.copy(0.4f), RoundedCornerShape(12.dp))
                                        .clickable { selectedWordId = if (isSelected) -1 else word.id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(word.vocab.japanese, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        Text(word.vocab.furigana, fontSize = 11.sp, color = Color.White.copy(0.75f))
                                    }
                                }
                            }
                        }

                        // Answer panel - shown when a word is selected
                        val activeWord = fallingItems.find { it.id == selectedWordId }
                        AnimatedVisibility(visible = activeWord != null) {
                            activeWord?.let { word ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0D1B4B))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "What does「${word.vocab.japanese}」mean?",
                                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                    )
                                    word.options.chunked(2).forEach { row ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            row.forEach { opt ->
                                                Button(
                                                    onClick = { handleAnswer(word, opt) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                                ) {
                                                    Text(opt, fontSize = 12.sp, textAlign = TextAlign.Center, color = Color.White)
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
            if (showConfetti) {
                ConfettiOverlay(
                    message = "Great Effort! 🌟",
                    subMessage = "You scored $score points on Level $level!",
                    onDismiss = { showConfetti = false }
                )
            }
        }
    }
}
