package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnkiDeckScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    quickMode: Boolean = false,
    appLanguage: String = "en",
    modifier: Modifier = Modifier
) {
    var fullList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var selectedDeckType by remember { mutableStateOf("bookmarks") }
    var selectedLesson by remember { mutableIntStateOf(1) }
    var selectedKanjiRange by remember { mutableStateOf("1-10") }
    var shadowingVocabItem by remember { mutableStateOf<VocabItem?>(null) }
    val haptic = LocalHapticFeedback.current

    // Study session state
    var studyQueue by remember { mutableStateOf<ArrayDeque<VocabItem>>(ArrayDeque()) }
    var correctCount by remember { mutableIntStateOf(0) }
    var againCount by remember { mutableIntStateOf(0) }
    var totalSeen by remember { mutableIntStateOf(0) }   // unique cards seen
    var totalStarted by remember { mutableIntStateOf(0) } // original deck size

    var isFlipped by remember { mutableStateOf(false) }
    var isStudying by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    var showMeaningFirst by remember {
        mutableStateOf(prefs.getBoolean("anki_meaning_first", false))
    }

    LaunchedEffect(Unit) {
        fullList = repository.getVocabulary()
        // Auto-start if quickMode
        if (quickMode && fullList.isNotEmpty()) {
            val bookmarks = prefs.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
            val pool = if (bookmarks.isNotEmpty()) {
                fullList.filter { bookmarks.contains(it.audioId) }
            } else {
                fullList.shuffled().take(5)
            }
            val cards = pool.shuffled().take(5)
            studyQueue = ArrayDeque(cards)
            correctCount = 0
            againCount = 0
            totalSeen = cards.size
            totalStarted = cards.size
            isFlipped = false
            isStudying = true
        }
    }

    fun startDeck() {
        val bookmarks = prefs.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
        val cards = when (selectedDeckType) {
            "srs_due" -> {
                val now = System.currentTimeMillis()
                val dueList = fullList.filter { item ->
                    val srs = com.momin.japanesestudyappn5.util.SrsEngine.loadSrsData(prefs, item.audioId)
                    srs.nextDueDateMillis <= now || srs.repetitions == 0
                }
                if (dueList.isEmpty()) fullList.shuffled().take(10) else dueList
            }
            "weak" -> {
                // Top 15 words marked Again most often
                fullList
                    .map { it to prefs.getInt("weak_${it.audioId}", 0) }
                    .filter { (_, count) -> count > 0 }
                    .sortedByDescending { (_, count) -> count }
                    .take(15)
                    .map { (item, _) -> item }
            }
            "lesson" -> fullList.filter { it.lesson == selectedLesson && !it.audioId.startsWith("kanji_") }
            "kanji" -> {
                val kanjiList = fullList.filter { it.audioId.startsWith("kanji_") }
                if (selectedKanjiRange == "all") {
                    kanjiList.sortedBy { it.lessonOrder ?: 0 }
                } else {
                    val parts = selectedKanjiRange.split("-").mapNotNull { it.toIntOrNull() }
                    if (parts.size == 2) {
                        val start = parts[0]
                        val end = parts[1]
                        kanjiList.filter { item ->
                            val order = item.lessonOrder ?: 0
                            order in start..end
                        }.sortedBy { it.lessonOrder ?: 0 }
                    } else kanjiList.sortedBy { it.lessonOrder ?: 0 }
                }
            }
            else -> fullList.filter { bookmarks.contains(it.audioId) }
        }
        val finalCards = if (selectedDeckType == "kanji") cards else cards.shuffled()
        studyQueue = ArrayDeque(finalCards)
        correctCount = 0
        againCount = 0
        totalSeen = finalCards.size
        totalStarted = finalCards.size
        isFlipped = false
        isStudying = true
    }

    val isDeckDone = isStudying && studyQueue.isEmpty()
    val currentCard = studyQueue.firstOrNull()

    LaunchedEffect(isFlipped) {
        if (isFlipped && currentCard != null) {
            AudioPlayer.playTts(context, currentCard.audioText.ifBlank { currentCard.furigana.ifBlank { currentCard.japanese } })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anki Flashcards", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isStudying) {
                            isStudying = false
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilterChip(
                        selected = showMeaningFirst,
                        onClick = {
                            val newValue = !showMeaningFirst
                            showMeaningFirst = newValue
                            prefs.edit().putBoolean("anki_meaning_first", newValue).apply()
                        },
                        label = {
                            Text(
                                if (showMeaningFirst) "🔤 Meaning First" else "🇯🇵 Japanese First",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                !isStudying -> {
                    // ── Deck Selection ───────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🃏  Choose Study Deck",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedDeckType == "bookmarks",
                                onClick = { selectedDeckType = "bookmarks" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4)
                            ) { Text("⭐ Bookmarks", fontWeight = FontWeight.Medium, fontSize = 11.sp) }
                            SegmentedButton(
                                selected = selectedDeckType == "weak",
                                onClick = { selectedDeckType = "weak" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4)
                            ) { Text("💪 Weak", fontWeight = FontWeight.Medium, fontSize = 11.sp) }
                            SegmentedButton(
                                selected = selectedDeckType == "lesson",
                                onClick = { selectedDeckType = "lesson" },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4)
                            ) { Text("📚 Lesson", fontWeight = FontWeight.Medium, fontSize = 11.sp) }
                            SegmentedButton(
                                selected = selectedDeckType == "kanji",
                                onClick = { selectedDeckType = "kanji" },
                                shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4)
                            ) { Text("⛩️ Kanji", fontWeight = FontWeight.Medium, fontSize = 11.sp) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Card Side Order Toggle ────────────────────────────────
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text(
                                        "Meaning on 1st Page",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        if (showMeaningFirst) "Page 1: Meaning → Page 2: Japanese" else "Page 1: Japanese → Page 2: Meaning",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Switch(
                                    checked = showMeaningFirst,
                                    onCheckedChange = { checked ->
                                        showMeaningFirst = checked
                                        prefs.edit().putBoolean("anki_meaning_first", checked).apply()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedDeckType == "kanji") {
                            Text("Select Kanji Group Range:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            var showKanjiDropdown by remember { mutableStateOf(false) }
                            val ranges = listOf("1-10", "11-20", "21-30", "31-40", "41-50", "51-60", "61-70", "71-80", "81-90", "91-100", "101-110", "all")
                            Box {
                                OutlinedButton(onClick = { showKanjiDropdown = true }) {
                                    Text(if (selectedKanjiRange == "all") "All Kanji (1-110)" else "Kanji $selectedKanjiRange", fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(expanded = showKanjiDropdown, onDismissRequest = { showKanjiDropdown = false }) {
                                    ranges.forEach { range ->
                                        DropdownMenuItem(
                                            text = { Text(if (range == "all") "All Kanji (1-110)" else "Kanji $range") },
                                            onClick = { selectedKanjiRange = range; showKanjiDropdown = false }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedDeckType == "lesson") {
                            Text("Select Lesson:", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            var showDropdown by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(onClick = { showDropdown = true }) {
                                    Text("Lesson $selectedLesson", fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                                    (1..25).forEach { lesson ->
                                        DropdownMenuItem(
                                            text = { Text("Lesson $lesson") },
                                            onClick = { selectedLesson = lesson; showDropdown = false }
                                        )
                                    }
                                }
                            }
                        } else if (selectedDeckType == "kanji") {
                            val kanjiCount = remember(fullList, selectedKanjiRange) {
                                val kanjiList = fullList.filter { it.audioId.startsWith("kanji_") }
                                if (selectedKanjiRange == "all") kanjiList.size
                                else {
                                    val parts = selectedKanjiRange.split("-").mapNotNull { it.toIntOrNull() }
                                    if (parts.size == 2) kanjiList.count { (it.lessonOrder ?: 0) in parts[0]..parts[1] }
                                    else kanjiList.size
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⛩️", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("$kanjiCount Kanji Cards (${if (selectedKanjiRange == "all") "1-110" else selectedKanjiRange})", fontWeight = FontWeight.Bold)
                                        Text("Sequential N5 Kanji Flashcards", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else if (selectedDeckType == "weak") {
                            val weakCount = remember(fullList) {
                                fullList.count { prefs.getInt("weak_${it.audioId}", 0) > 0 }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💪", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("$weakCount words marked 'Again'", fontWeight = FontWeight.Bold)
                                        Text("Practice your hardest words", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        } else {
                            val bookmarksCount = remember(fullList) {
                                val bookmarks = prefs.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
                                fullList.count { bookmarks.contains(it.audioId) }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⭐", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                                    Column {
                                        Text("$bookmarksCount bookmarked cards", fontWeight = FontWeight.Bold)
                                        Text("Ready to review", fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { startDeck() },
                            enabled = selectedDeckType == "lesson" ||
                                    selectedDeckType == "weak" ||
                                    selectedDeckType == "kanji" ||
                                    (prefs.getStringSet("bookmarked_vocab", emptySet())?.isNotEmpty() == true),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text("▶  Start Reviewing", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                isDeckDone -> {
                    // ── Session Results ──────────────────────────────────
                    val total = correctCount + againCount
                    val accuracy = if (total > 0) (correctCount.toFloat() / total * 100).roundToInt() else 0

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (accuracy >= 80) "🎉 Great Session!" else "✅ Session Complete!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3DC487)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You reviewed $totalStarted cards",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(28.dp))

                        // Accuracy ring
                        Surface(
                            color = if (accuracy >= 80) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "$accuracy%",
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (accuracy >= 80) Color(0xFF2E7D32) else Color(0xFFF57F17)
                                )
                                Text(
                                    text = "Accuracy",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { accuracy / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (accuracy >= 80) Color(0xFF43A047) else Color(0xFFFFB300)
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("✅", fontSize = 22.sp)
                                        Text("$correctCount", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                        Text("Correct", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔄", fontSize = 22.sp)
                                        Text("$againCount", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                        Text("Again", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = { isStudying = false },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Back to Selection", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                currentCard != null -> {
                    // ── Active Card Review ───────────────────────────────
                    val item = currentCard
                    val remaining = studyQueue.size
                    val done = totalStarted - remaining

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Card $done / $totalStarted",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$remaining left",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { done.toFloat() / totalStarted },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        // 3D Flip Card
                        key(item.audioId) {
                            val rotation by animateFloatAsState(
                                targetValue = if (isFlipped) 180f else 0f,
                                animationSpec = tween(durationMillis = 400),
                                label = "cardRotation"
                            )

                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .graphicsLayer {
                                        rotationY = rotation
                                        cameraDistance = 12f * density
                                    }
                                    .clickable { isFlipped = !isFlipped }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (rotation <= 90f) {
                                        // Front side (Page 1)
                                        if (!showMeaningFirst) {
                                            // Standard Mode: Japanese on Front Page
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(24.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                ) {
                                                    Text(
                                                        "Page 1: Japanese",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                                val displayJp = if (isKanjiOff) item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(item.japanese) } else item.japanese
                                                if (!isKanjiOff && item.furigana.isNotBlank() && item.furigana != item.japanese) {
                                                    Text(item.furigana, fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Medium)
                                                    Spacer(Modifier.height(4.dp))
                                                }
                                                Text(displayJp, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center)
                                                if (item.romaji.isNotBlank()) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(item.romaji, fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        fontWeight = FontWeight.SemiBold)
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                com.momin.japanesestudyappn5.ui.components.PitchAccentView(
                                                    japanese = item.japanese,
                                                    furigana = item.furigana,
                                                    compact = true,
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                                Spacer(Modifier.height(16.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            AudioPlayer.playTts(context, item.audioText.ifBlank { item.furigana.ifBlank { item.japanese } })
                                                        },
                                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Play Audio",
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(28.dp))
                                                    }
                                                    Spacer(Modifier.width(16.dp))
                                                    IconButton(
                                                        onClick = {
                                                            shadowingVocabItem = item
                                                        },
                                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    ) {
                                                        Text("🎙️", fontSize = 24.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(24.dp))
                                                Text("Tap card to reveal translation", fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.outline)
                                            }
                                        } else {
                                            // Reverse Mode: Meaning on Front Page
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(24.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                ) {
                                                    Text(
                                                        "Page 1: Meaning",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                                if (appLanguage == "bn" && item.bangla.isNotBlank()) {
                                                    Text(
                                                        "Meaning (অর্থ):",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        item.bangla,
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E88E5),
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (item.english.isNotBlank()) {
                                                        Spacer(Modifier.height(8.dp))
                                                        Text(
                                                            item.english,
                                                            fontSize = 16.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        "Meaning:",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        item.english,
                                                        fontSize = 30.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                                Spacer(Modifier.height(36.dp))
                                                Text(
                                                    "Tap card to reveal Japanese",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    } else {
                                        // Back side (Page 2)
                                        if (!showMeaningFirst) {
                                            // Standard Mode: Meaning on Back Page
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(24.dp).graphicsLayer { rotationY = 180f }
                                            ) {
                                                Text(item.japanese, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.outline)
                                                Spacer(Modifier.height(16.dp))

                                                if (appLanguage == "bn") {
                                                    Text("Meaning (অর্থ):", style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary)
                                                    Text(item.bangla, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1E88E5), textAlign = TextAlign.Center)

                                                    Spacer(Modifier.height(12.dp))

                                                    Text("English:", style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline)
                                                    Text(item.english, fontSize = 16.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                        textAlign = TextAlign.Center)
                                                } else {
                                                    Text("Meaning:", style = MaterialTheme.typography.labelLarge,
                                                        color = MaterialTheme.colorScheme.primary)
                                                    Text(item.english, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.Center)
                                                }
                                            }
                                        } else {
                                            // Reverse Mode: Japanese on Back Page
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(24.dp).graphicsLayer { rotationY = 180f }
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.padding(bottom = 12.dp)
                                                ) {
                                                    Text(
                                                        "Page 2: Japanese",
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                val isKanjiOffBack = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                                val displayJpBack = if (isKanjiOffBack) item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(item.japanese) } else item.japanese
                                                if (!isKanjiOffBack && item.furigana.isNotBlank() && item.furigana != item.japanese) {
                                                    Text(item.furigana, fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        fontWeight = FontWeight.Medium)
                                                    Spacer(Modifier.height(4.dp))
                                                }
                                                Text(displayJpBack, fontSize = 40.sp, fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center)
                                                if (item.romaji.isNotBlank()) {
                                                    Spacer(Modifier.height(6.dp))
                                                    Text(item.romaji, fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        fontWeight = FontWeight.SemiBold)
                                                }
                                                Spacer(Modifier.height(16.dp))

                                                val meaningSummary = if (appLanguage == "bn" && item.bangla.isNotBlank()) item.bangla else item.english
                                                Text("Prompt: $meaningSummary", fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    textAlign = TextAlign.Center)

                                                Spacer(Modifier.height(24.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            AudioPlayer.playTts(context, item.audioText.ifBlank { item.furigana.ifBlank { item.japanese } })
                                                        },
                                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Play Audio",
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(28.dp))
                                                    }
                                                    Spacer(Modifier.width(16.dp))
                                                    IconButton(
                                                        onClick = {
                                                            shadowingVocabItem = item
                                                        },
                                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    ) {
                                                        Text("🎙️", fontSize = 24.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Answer buttons (SM-2 SRS Algorithm: Again, Hard, Good, Easy)
                        if (isFlipped) {
                            val currentSrsData = remember(item.audioId) { com.momin.japanesestudyappn5.util.SrsEngine.loadSrsData(prefs, item.audioId) }
                            
                            val againPreview = remember(currentSrsData) { com.momin.japanesestudyappn5.util.SrsEngine.getIntervalPreview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.AGAIN) }
                            val hardPreview = remember(currentSrsData) { com.momin.japanesestudyappn5.util.SrsEngine.getIntervalPreview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.HARD) }
                            val goodPreview = remember(currentSrsData) { com.momin.japanesestudyappn5.util.SrsEngine.getIntervalPreview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.GOOD) }
                            val easyPreview = remember(currentSrsData) { com.momin.japanesestudyappn5.util.SrsEngine.getIntervalPreview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.EASY) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. AGAIN
                                Button(
                                    onClick = {
                                        if (studyQueue.isNotEmpty()) {
                                            val updated = com.momin.japanesestudyappn5.util.SrsEngine.processReview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.AGAIN)
                                            com.momin.japanesestudyappn5.util.SrsEngine.saveSrsData(prefs, updated)
                                            val temp = ArrayDeque(studyQueue)
                                            val card = temp.removeFirst()
                                            temp.addLast(card)
                                            studyQueue = temp
                                            againCount++
                                            isFlipped = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(againPreview, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                        Text("Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 2. HARD
                                Button(
                                    onClick = {
                                        if (studyQueue.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val updated = com.momin.japanesestudyappn5.util.SrsEngine.processReview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.HARD)
                                            com.momin.japanesestudyappn5.util.SrsEngine.saveSrsData(prefs, updated)
                                            val temp = ArrayDeque(studyQueue)
                                            temp.removeFirst()
                                            studyQueue = temp
                                            correctCount++
                                            isFlipped = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(hardPreview, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                        Text("Hard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 3. GOOD
                                Button(
                                    onClick = {
                                        if (studyQueue.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val updated = com.momin.japanesestudyappn5.util.SrsEngine.processReview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.GOOD)
                                            com.momin.japanesestudyappn5.util.SrsEngine.saveSrsData(prefs, updated)
                                            val temp = ArrayDeque(studyQueue)
                                            temp.removeFirst()
                                            studyQueue = temp
                                            correctCount++
                                            isFlipped = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(goodPreview, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                        Text("Good", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 4. EASY
                                Button(
                                    onClick = {
                                        if (studyQueue.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val updated = com.momin.japanesestudyappn5.util.SrsEngine.processReview(currentSrsData, com.momin.japanesestudyappn5.util.SrsRating.EASY)
                                            com.momin.japanesestudyappn5.util.SrsEngine.saveSrsData(prefs, updated)
                                            val temp = ArrayDeque(studyQueue)
                                            temp.removeFirst()
                                            studyQueue = temp
                                            correctCount++
                                            isFlipped = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                                    modifier = Modifier.weight(1f).height(54.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(easyPreview, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
                                        Text("Easy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { isFlipped = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Text(
                                    if (showMeaningFirst) "Reveal Japanese" else "Reveal Answer",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    shadowingVocabItem?.let { item ->
        ShadowingDialog(
            word = item.japanese,
            furigana = item.furigana,
            romaji = item.romaji,
            translation = if (appLanguage == "bn") item.bangla else item.english,
            onDismiss = { shadowingVocabItem = null }
        )
    }
}
