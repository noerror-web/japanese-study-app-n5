package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.ExampleSentence
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceBuilderScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
    val scope = rememberCoroutineScope()

    var aiMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    // All sentences flattened
    var allSentences by remember { mutableStateOf<List<ExampleSentence>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedTiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableTiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showAnswer by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var isStarted by remember { mutableStateOf(false) }
    var shuffledSentences by remember { mutableStateOf<List<ExampleSentence>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            val map = repository.getSentences()
            allSentences = map.values.flatten()
        }
    }

    fun getTextToSplit(sentence: ExampleSentence): String {
        val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
        return if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(sentence.japanese, sentence.furigana) else sentence.japanese
    }

    fun buildTiles(sentence: ExampleSentence) {
        val textToSplit = getTextToSplit(sentence)
        val words = textToSplit.split("\\s+".toRegex()).filter { it.isNotBlank() }
        availableTiles = words.shuffled()
        selectedTiles = emptyList()
        isCorrect = null
        showAnswer = false
    }

    fun startOfflineGame() {
        shuffledSentences = allSentences.filter {
            it.japanese.contains(" ")
        }.shuffled().take(10)
        if (shuffledSentences.isEmpty()) {
            val fallback = listOf(
                ExampleSentence("わたし は がくせい です", "わたしはがくせいです", "I am a student", "আমি একজন ছাত্র。"),
                ExampleSentence("これ は ほん です", "これはほんです", "This is a book", "এটি একটি বই。"),
                ExampleSentence("あの ひと は せんせい です", "あのひとはせんせいです", "That person is a teacher", "ঐ ব্যক্তি একজন শিক্ষক。"),
                ExampleSentence("わたし の なまえ は マリア です", "わたしのなまえはマリアです", "My name is Maria", "আমার নাম মারিয়া。"),
                ExampleSentence("あした がっこう に いきます", "あしたがっこうにいきます", "I will go to school tomorrow", "আমি আগামীকাল স্কুলে যাব。"),
                ExampleSentence("きょう は いい てんき です", "きょうはいいてんきです", "Today is good weather", "আজ আবহাওয়া খুব ভালো。"),
                ExampleSentence("まいにち にほんご を べんきょう します", "まいにちにほんごをべんきょうします", "I study Japanese every day", "আমি প্রতিদিন জাপানি ভাষা পড়াশোনা করি。"),
                ExampleSentence("すみません、えき は どこ です か", "すみません、えきは国内ですか", "Excuse me, where is the station?", "মাফ করবেন, স্টেশনটি কোথায়?"),
            )
            shuffledSentences = fallback
        }
        currentIndex = 0
        score = 0
        buildTiles(shuffledSentences[0])
        isStarted = true
    }

    fun startGame() {
        val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
        if (aiMode) {
            isLoading = true
            scope.launch {
                val generated = com.momin.japanesestudyappn5.util.AIGenerator.generateSentences(apiKey, count = 5, kanjiDisabled = isKanjiOff)
                isLoading = false
                if (!generated.isNullOrEmpty()) {
                    shuffledSentences = generated
                    currentIndex = 0
                    score = 0
                    buildTiles(shuffledSentences[0])
                    isStarted = true
                } else {
                    android.widget.Toast.makeText(context, "AI Generation failed. Falling back to offline database.", android.widget.Toast.LENGTH_LONG).show()
                    startOfflineGame()
                }
            }
        } else {
            startOfflineGame()
        }
    }

    fun checkAnswer() {
        val sentence = shuffledSentences[currentIndex]
        val textToSplit = getTextToSplit(sentence)
        val expected = textToSplit.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val correct = selectedTiles == expected
        isCorrect = correct
        if (correct) {
            score++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun nextSentence() {
        if (currentIndex + 1 >= shuffledSentences.size) {
            isStarted = false // show results via a flag
        } else {
            currentIndex++
            buildTiles(shuffledSentences[currentIndex])
        }
    }

    val isFinished = isStarted && currentIndex >= shuffledSentences.size - 1 && isCorrect != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🧩 Sentence Builder", fontWeight = FontWeight.Bold)
                        if (isStarted && !isFinished) Text("${currentIndex + 1} / ${shuffledSentences.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                !isStarted && shuffledSentences.isEmpty() -> {
                    // Start screen
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🧩", fontSize = 72.sp)
                        Spacer(Modifier.height(20.dp))
                        Text("Sentence Builder", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Text("Arrange the word tiles in the correct\norder to form a Japanese sentence!",
                            fontSize = 15.sp, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                        Spacer(Modifier.height(40.dp))
                        if (allSentences.isEmpty()) {
                            CircularProgressIndicator()
                        } else if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Generating AI sentences...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .clickable { aiMode = !aiMode },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("⚡ AI Mode (Online)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Generate fresh N5 sentences dynamically via Gemini API", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Switch(checked = aiMode, onCheckedChange = { aiMode = it })
                            }
                            Button(
                                onClick = { startGame() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Start Building!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                isFinished -> {
                    val accuracy = if (shuffledSentences.isNotEmpty()) score * 100 / shuffledSentences.size else 0
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (accuracy >= 70) "🎉 Well done!" else "📚 Keep going!", fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(20.dp))
                        Text("$accuracy%", fontSize = 72.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (accuracy >= 70) Color(0xFF2E7D32) else Color(0xFFF57F17))
                        Text("Accuracy — $score / ${shuffledSentences.size} correct",
                            color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = { shuffledSentences = emptyList(); isStarted = false; startGame() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp)) {
                            Text("Back to Home")
                        }
                    }
                }

                isStarted && shuffledSentences.isNotEmpty() -> {
                    val sentence = shuffledSentences[currentIndex]
                    LazyColumn(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // English cue
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        if (appLanguage == "bn" && !sentence.bangla.isNullOrBlank()) "Translation (অনুবাদ):" else "English:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                                    )
                                    val displayCue = if (appLanguage == "bn" && !sentence.bangla.isNullOrBlank()) sentence.bangla else sentence.english
                                    Text(displayCue, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        // Answer area
                        item {
                            val borderColor by animateColorAsState(
                                targetValue = when (isCorrect) {
                                    true -> Color(0xFF43A047)
                                    false -> Color(0xFFEF5350)
                                    null -> MaterialTheme.colorScheme.outline
                                }, label = "border"
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .border(2.dp, borderColor, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().minHeight(80.dp).padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedTiles.isEmpty()) {
                                        Text("Tap tiles below to build the sentence",
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 13.sp, textAlign = TextAlign.Center)
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            selectedTiles.forEach { tile ->
                                                Surface(
                                                    modifier = Modifier.padding(3.dp)
                                                        .clickable(enabled = isCorrect == null) {
                                                            // Remove tile back to available
                                                            selectedTiles = selectedTiles - tile
                                                            availableTiles = availableTiles + tile
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = when (isCorrect) {
                                                        true -> Color(0xFF43A047)
                                                        false -> Color(0xFFEF5350)
                                                        null -> MaterialTheme.colorScheme.primary
                                                    }
                                                ) {
                                                    Text(tile, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Available tiles
                        item {
                            Text("Available words:", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 4.dp))
                        }
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                availableTiles.forEach { tile ->
                                    Surface(
                                        modifier = Modifier.padding(4.dp)
                                            .clickable(enabled = isCorrect == null) {
                                                selectedTiles = selectedTiles + tile
                                                availableTiles = availableTiles - tile
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(tile, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                    }
                                }
                            }
                        }

                        // Action buttons
                        item {
                            if (isCorrect == null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            buildTiles(sentence)
                                        },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Reset") }
                                    Button(
                                        onClick = { checkAnswer() },
                                        enabled = selectedTiles.size == sentence.japanese.split("\\s+".toRegex()).count { it.isNotBlank() },
                                        modifier = Modifier.weight(2f).height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Check ✓", fontWeight = FontWeight.Bold) }
                                }
                            } else {
                                Column(Modifier.fillMaxWidth()) {
                                    // Feedback
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCorrect == true)
                                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (isCorrect == true) "✅ Correct!" else "❌ Not quite...",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                val displaySentenceText = getTextToSplit(sentence)
                                                Text(
                                                    if (isCorrect == true) displaySentenceText else "Correct: $displaySentenceText",
                                                    fontSize = 14.sp,
                                                    color = if (isCorrect == true) Color(0xFF2E7D32) else Color(0xFFC62828),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    AudioPlayer.ensureTts(context)
                                                    AudioPlayer.speakJapanese(sentence.japanese.replace(" ", ""))
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Text("🔊", fontSize = 18.sp)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { nextSentence() },
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("Next →", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private fun Modifier.minHeight(height: androidx.compose.ui.unit.Dp): Modifier =
    this.heightIn(min = height)
