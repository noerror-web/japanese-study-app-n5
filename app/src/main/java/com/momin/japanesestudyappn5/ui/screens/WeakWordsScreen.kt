package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeakWordsScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }

    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var weakWords by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var showAnswer by remember { mutableStateOf(false) }
    var mastered by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isFlashcardMode by remember { mutableStateOf(true) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showFeedback by remember { mutableStateOf(false) }

    val quizOptions = remember(currentIndex, weakWords, allVocab) {
        val correct = weakWords.getOrNull(currentIndex) ?: return@remember emptyList<VocabItem>()
        val distractors = allVocab.filter { it.audioId != correct.audioId }.shuffled().take(3)
        (distractors + correct).shuffled()
    }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var t: TextToSpeech? = null
        t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                t?.language = Locale.JAPANESE
            }
        }
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary()
        val weakIds = prefs.getStringSet("weak_words", emptySet()) ?: emptySet()
        weakWords = allVocab.filter { it.audioId in weakIds }.shuffled()
    }

    fun removeFromWeak(audioId: String) {
        val set = (prefs.getStringSet("weak_words", emptySet()) ?: emptySet()).toMutableSet()
        set.remove(audioId)
        prefs.edit().putStringSet("weak_words", set).apply()
        weakWords = weakWords.filter { it.audioId != audioId }
        mastered = mastered + audioId
        if (currentIndex >= weakWords.size && weakWords.isNotEmpty()) {
            currentIndex = weakWords.size - 1
        }
    }

    val current = weakWords.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💪 Weak Words", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (weakWords.isNotEmpty()) {
                        TextButton(onClick = {
                            isFlashcardMode = !isFlashcardMode
                            showAnswer = false
                            selectedOptionIndex = null
                            showFeedback = false
                        }) {
                            Text(
                                text = if (isFlashcardMode) "✍️ Quiz" else "🃏 Cards",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFB71C1C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFEBEE))))
        ) {
            when {
                weakWords.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🎉", fontSize = 72.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (mastered.isEmpty()) "No weak words yet!" else "All clear! 🏆",
                            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB71C1C)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (mastered.isEmpty())
                                "Get some quiz questions wrong and they'll appear here for focused practice."
                            else
                                "You've mastered all ${mastered.size} weak word(s) in this session!",
                            fontSize = 14.sp, color = Color(0xFF666666),
                            textAlign = TextAlign.Center, lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                        ) {
                            Text("← Go Back", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                isFlashcardMode && current != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${currentIndex + 1} of ${weakWords.size} weak words", fontSize = 13.sp, color = Color(0xFF888888))
                                Text("Mastered this session: ${mastered.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / weakWords.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFEF5350)
                            )
                        }

                        // Flashcard
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFFFEBEE)
                                    ) {
                                        Text(
                                            "⚠️ Needs Practice",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp, color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        current.japanese,
                                        fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A), textAlign = TextAlign.Center
                                    )
                                    if (current.furigana != current.japanese) {
                                        Text(
                                            current.furigana, fontSize = 16.sp,
                                            color = Color(0xFF666666), textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    IconButton(onClick = { tts?.speak(current.japanese, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                                        Text("🔊", fontSize = 24.sp)
                                    }
                                    if (showAnswer) {
                                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))
                                        if (appLanguage == "bn" && current.bangla.isNotEmpty()) {
                                            Text(current.bangla, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5), textAlign = TextAlign.Center)
                                            Spacer(Modifier.height(4.dp))
                                            Text(current.english, fontSize = 16.sp, color = Color(0xFF555555), textAlign = TextAlign.Center)
                                        } else {
                                            Text(current.english, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), textAlign = TextAlign.Center)
                                        }
                                    } else {
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(onClick = { showAnswer = true }, shape = RoundedCornerShape(12.dp)) {
                                            Text("Show Answer")
                                        }
                                    }
                                }
                            }
                        }

                        // Action buttons
                        if (showAnswer) {
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    // Still hard
                                    OutlinedButton(
                                        onClick = {
                                            showAnswer = false
                                            if (currentIndex + 1 < weakWords.size) currentIndex++
                                            else currentIndex = 0
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350))
                                    ) {
                                        Text("😓 Still Hard", fontWeight = FontWeight.Bold)
                                    }
                                    // Mastered!
                                    Button(
                                        onClick = {
                                            removeFromWeak(current.audioId)
                                            showAnswer = false
                                        },
                                        modifier = Modifier.weight(1f).height(52.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                                    ) {
                                        Text("✅ Mastered!", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                !isFlashcardMode && current != null -> {
                    val correct = current
                    val options = quizOptions
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${currentIndex + 1} of ${weakWords.size} weak words", fontSize = 13.sp, color = Color(0xFF888888))
                                Text("Mastered this session: ${mastered.size}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (currentIndex + 1).toFloat() / weakWords.size },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFEF5350)
                            )
                        }

                        // Question card (Japanese word)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Text(
                                            "❓ Match Translation",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        correct.japanese,
                                        fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1A1A), textAlign = TextAlign.Center
                                    )
                                    if (correct.furigana != correct.japanese) {
                                        Text(
                                            correct.furigana, fontSize = 16.sp,
                                            color = Color(0xFF666666), textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    IconButton(onClick = { tts?.speak(correct.japanese, TextToSpeech.QUEUE_FLUSH, null, null) }) {
                                        Text("🔊", fontSize = 24.sp)
                                    }
                                }
                            }
                        }

                        // 4 Option items
                        items(options.size) { idx ->
                            val opt = options.getOrNull(idx)
                            if (opt != null) {
                                val isCorrect = opt.audioId == correct.audioId
                                val isSelected = selectedOptionIndex == idx

                                val cardColor = when {
                                    showFeedback && isCorrect -> Color(0xFFE8F5E9)
                                    showFeedback && isSelected -> Color(0xFFFFEBEE)
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.White
                                }

                                val borderStroke = when {
                                    showFeedback && isCorrect -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2E7D32))
                                    showFeedback && isSelected -> androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFC62828))
                                    isSelected -> androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    else -> null
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !showFeedback) {
                                            selectedOptionIndex = idx
                                            showFeedback = true
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardColor),
                                    border = borderStroke,
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${('A' + idx)}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                        val textToShow = if (appLanguage == "bn" && opt.bangla.isNotEmpty()) {
                                            opt.bangla
                                        } else {
                                            opt.english
                                        }
                                        Text(
                                            text = textToShow,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (showFeedback) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = if (isCorrect) "✅" else if (isSelected) "❌" else "",
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Next button
                        if (showFeedback) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val isCorrect = selectedOptionIndex != null && selectedOptionIndex!! < options.size &&
                                            options[selectedOptionIndex!!].audioId == correct.audioId
                                        
                                        if (isCorrect) {
                                            removeFromWeak(correct.audioId)
                                        } else {
                                            if (currentIndex + 1 < weakWords.size) {
                                                currentIndex++
                                            } else {
                                                currentIndex = 0
                                            }
                                        }
                                        showFeedback = false
                                        selectedOptionIndex = null
                                    },
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                                ) {
                                    val isCorrect = selectedOptionIndex != null && selectedOptionIndex!! < options.size &&
                                        options[selectedOptionIndex!!].audioId == correct.audioId
                                    Text(
                                        text = if (isCorrect) "Mastered & Next →" else "Next Question →",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}
