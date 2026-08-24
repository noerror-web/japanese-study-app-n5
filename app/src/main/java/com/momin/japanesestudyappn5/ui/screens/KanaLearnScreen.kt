package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.KanaData
import com.momin.japanesestudyappn5.data.model.KanaItem
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaLearnScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    globalShowRomaji: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isHiragana by remember { mutableStateOf(true) }
    var showRomaji by remember { mutableStateOf(globalShowRomaji) }
    var activeTab by remember { mutableStateOf("basic") }
    var selectedKanaForWriting by remember { mutableStateOf<KanaItem?>(null) }
    var showQuiz by remember { mutableStateOf(false) }
    var practicedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confettiMessage by remember { mutableStateOf<Pair<String,String>?>(null) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", android.content.Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        practicedIds = prefs.getStringSet("practiced_kana", emptySet()) ?: emptySet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kana Study", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showQuiz = true }) {
                        Text("Quiz", fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = { showRomaji = !showRomaji }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (showRomaji) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                "abc",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (showRomaji) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
            // Character Type Selector (Hiragana / Katakana)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = isHiragana,
                        onClick = { isHiragana = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Hiragana (ひらがな)", fontWeight = FontWeight.Medium)
                    }
                    SegmentedButton(
                        selected = !isHiragana,
                        onClick = { isHiragana = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Katakana (カタカナ)", fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Tab Selector
            val tabs = listOf("basic" to "Basic", "dakuten" to "Dakuten", "handakuten" to "Handakuten", "combo" to "Combo", "all" to "All")
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                tabs.forEach { (id, label) ->
                    Tab(
                        selected = activeTab == id,
                        onClick = { activeTab = id },
                        text = { Text(label, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Practiced progress bar
            val allItems = remember(isHiragana) { getCombinedAllList(isHiragana) }
            val practicedCount = remember(practicedIds, isHiragana) {
                allItems.count { practicedIds.contains("${if (isHiragana) "h" else "k"}_${it.romaji}") }
            }
            if (allItems.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHiragana) "Hiragana practiced" else "Katakana practiced",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$practicedCount / ${allItems.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { practicedCount.toFloat() / allItems.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    )
                }
            }

            // Kana Cards Grid
            val itemsList = remember(isHiragana, activeTab) {
                getKanaItems(isHiragana, activeTab)
            }

            val columns = if (activeTab == "combo") 3 else 5

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedCorner(8.dp),
                verticalArrangement = Arrangement.spacedCorner(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(itemsList) { item ->
                    if (item == null) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        val practicedKey = "${if (isHiragana) "h" else "k"}_${item.romaji}"
                        val isPracticed = practicedIds.contains(practicedKey)
                        KanaCard(
                            item = item,
                            showRomaji = showRomaji,
                            isPracticed = isPracticed,
                            onClick = {
                                AudioPlayer.playTts(context, item.char)
                                // Mark as practiced
                                val newPracticed = practicedIds.toMutableSet().also { it.add(practicedKey) }
                                practicedIds = newPracticed
                                prefs.edit().putStringSet("practiced_kana", newPracticed).apply()
                                // Milestone: all hiragana or katakana practiced
                                val prefix = if (isHiragana) "h_" else "k_"
                                val totalOfType = if (isHiragana) {
                                    KanaData.hiraganaBasic.size + KanaData.hiraganaDakuten.size +
                                    KanaData.hiraganaHandakuten.size + KanaData.hiraganaCombination.size
                                } else {
                                    KanaData.katakanaBasic.size + KanaData.katakanaDakuten.size +
                                    KanaData.katakanaHandakuten.size + KanaData.katakanaCombination.size
                                }
                                val practicedOfType = newPracticed.count { it.startsWith(prefix) }
                                if (practicedOfType >= totalOfType && totalOfType > 0) {
                                    val label = if (isHiragana) "All Hiragana Practiced! 🎌" else "All Katakana Practiced! 🎌"
                                    confettiMessage = label to "You've practiced every ${if (isHiragana) "hiragana" else "katakana"} character! Amazing!"
                                }
                            },
                            onLongClick = { selectedKanaForWriting = item }
                        )
                    }
                }
            }

            // Status hint
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tap to play audio • Hold card for writing guide animation",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }

    // Kana milestone confetti
    confettiMessage?.let { (msg, sub) ->
        ConfettiOverlay(
            message = msg,
            subMessage = sub,
            onDismiss = { confettiMessage = null }
        )
    }

    // Writing Guide Modal Dialog
    selectedKanaForWriting?.let { item ->
        var svgXml by remember { mutableStateOf<String?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(item) {
            coroutineScope.launch {
                svgXml = if (isHiragana) {
                    repository.getHiraganaSvg(item.char)
                } else {
                    repository.getKatakanaSvg(item.char)
                }
            }
        }

        Dialog(onDismissRequest = { selectedKanaForWriting = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Writing Guide: ${item.char}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                AudioPlayer.playTts(context, item.char)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Sound")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stroke order canvas
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val xml = svgXml
                        if (xml != null) {
                            StrokeOrderAnimation(
                                svgXml = xml,
                                activeColor = MaterialTheme.colorScheme.primary,
                                baseColor = MaterialTheme.colorScheme.outlineVariant,
                                strokeWidth = 8f
                            )
                        } else {
                            CircularProgressIndicator()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Romaji: ${item.romaji.uppercase()} • Type: ${item.type.replaceFirstChar { it.uppercase() }}",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { selectedKanaForWriting = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close")
                        }

                        Button(
                            onClick = {
                                val allItems = getCombinedAllList(isHiragana)
                                val currentIndex = allItems.indexOfFirst { it.char == item.char }
                                if (currentIndex != -1) {
                                    val nextIndex = (currentIndex + 1) % allItems.size
                                    selectedKanaForWriting = allItems[nextIndex]
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Next →")
                        }
                    }
                }
            }
        }
    }

    // Quiz overlay
    if (showQuiz) {
        val pool = remember(isHiragana, activeTab) {
            getKanaItems(isHiragana, activeTab).filterNotNull()
        }
        if (pool.isNotEmpty()) {
            KanaQuizDialog(
                pool = pool,
                isHiragana = isHiragana,
                onDismiss = { showQuiz = false }
            )
        }
    }
}

private fun Arrangement.spacedCorner(size: androidx.compose.ui.unit.Dp) = Arrangement.spacedBy(size)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KanaCard(
    item: KanaItem,
    showRomaji: Boolean,
    isPracticed: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val containerColor = when (item.type) {
        "dakuten" -> if (isDark) Color(0xFF1B3A24) else Color(0xFFEEFCF4)
        "handakuten" -> if (isDark) Color(0xFF3E2A1C) else Color(0xFFFFF7EC)
        "combination" -> if (isDark) Color(0xFF2C1C3E) else Color(0xFFF8F1FF)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (item.type) {
        "dakuten" -> if (isDark) Color(0xFFA3E2B6) else Color(0xFF1B5E20)
        "handakuten" -> if (isDark) Color(0xFFF2C894) else Color(0xFFE65100)
        "combination" -> if (isDark) Color(0xFFD6BDF2) else Color(0xFF4A148C)
        else -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF243454)
    }

    val romajiColor = when (item.type) {
        "dakuten" -> if (isDark) Color(0xFFA3E2B6).copy(alpha = 0.7f) else Color(0xFF388E3C)
        "handakuten" -> if (isDark) Color(0xFFF2C894).copy(alpha = 0.7f) else Color(0xFFF57C00)
        "combination" -> if (isDark) Color(0xFFD6BDF2).copy(alpha = 0.7f) else Color(0xFF7B1FA2)
        else -> if (isDark) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else Color(0xFF5D6986)
    }

    Box {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.char,
                    fontSize = if (item.char.length > 1) 22.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (showRomaji) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.romaji,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = romajiColor
                    )
                }
            }
        }
        // Green practiced dot badge
        if (isPracticed) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp)
                    .background(Color(0xFF43A047), shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
fun StrokeOrderAnimation(
    svgXml: String,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Float = 8f
) {
    val pathStrings = remember(svgXml) {
        val pathRegex = "<path[^>]*\\bd=\"([^\"]*)\"".toRegex()
        pathRegex.findAll(svgXml).map { it.groupValues[1] }.toList()
    }

    val parsedPaths = remember(pathStrings) {
        android.util.Log.e("StrokeOrderAnimation", "svgXml content: $svgXml")
        android.util.Log.e("StrokeOrderAnimation", "pathStrings size: ${pathStrings.size}, contents: $pathStrings")
        val parsed = pathStrings.mapNotNull { pathStr ->
            try {
                PathParser().parsePathString(pathStr).toPath()
            } catch (e: Exception) {
                android.util.Log.e("StrokeOrderAnimation", "Error parsing path: $pathStr", e)
                null
            }
        }
        android.util.Log.e("StrokeOrderAnimation", "parsedPaths size: ${parsed.size}")
        parsed
    }

    if (parsedPaths.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stroke data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "strokeTransition")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = parsedPaths.size.toFloat() + 0.8f, // Extra padding at the end for pause
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = parsedPaths.size * 900 + 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "animProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        android.util.Log.e("StrokeOrderAnimation", "canvas draw size: $size")
        // Draw centered and scaled
        val scaleX = size.width / 109f
        val scaleY = size.height / 109f
        val scale = minOf(scaleX, scaleY) * 0.85f
        val offsetX = (size.width - 109f * scale) / 2f
        val offsetY = (size.height - 109f * scale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // Draw baseline outline paths
            parsedPaths.forEach { path ->
                drawPath(
                    path = path,
                    color = baseColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Draw animated strokes
            val currentStrokeIndex = animProgress.toInt()
            val currentStrokeProgress = animProgress - currentStrokeIndex

            for (i in 0 until minOf(currentStrokeIndex, parsedPaths.size)) {
                drawPath(
                    path = parsedPaths[i],
                    color = activeColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            if (currentStrokeIndex in parsedPaths.indices) {
                val currentPath = parsedPaths[currentStrokeIndex]
                val pathMeasure = PathMeasure()
                pathMeasure.setPath(currentPath, false)
                val partialPath = Path()
                pathMeasure.getSegment(0f, pathMeasure.length * minOf(currentStrokeProgress, 1f), partialPath, true)

                drawPath(
                    path = partialPath,
                    color = activeColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
fun KanaQuizDialog(
    pool: List<KanaItem>,
    isHiragana: Boolean,
    onDismiss: () -> Unit
) {
    val totalQuestions = 10
    val quizItems = remember(pool) {
        pool.shuffled().take(totalQuestions)
    }

    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<KanaItem?>(null) }
    var showResults by remember { mutableStateOf(false) }

    val currentItem = quizItems.getOrNull(questionIndex)
    // 50% chance to test Kana-to-Romaji or Romaji-to-Kana
    val quizMode = remember(questionIndex) { if (Random.nextBoolean()) "kana" else "romaji" }

    val options = remember(currentItem, questionIndex) {
        if (currentItem == null) emptyList()
        else {
            val list = mutableListOf(currentItem)
            val others = pool.filter { it.char != currentItem.char }
                .shuffled()
                .distinctBy { if (quizMode == "kana") it.romaji else it.char }
                .take(3)
            list.addAll(others)
            list.shuffled()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showResults) {
                    Text("Quiz Completed!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your Score: $score / $totalQuestions", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish")
                    }
                } else if (currentItem != null) {
                    Text(
                        text = "Question ${questionIndex + 1} of $totalQuestions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Question prompt
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (quizMode == "kana") "Which Romaji matches this?" else "Which Kana matches this?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (quizMode == "kana") currentItem.char else currentItem.romaji,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Options list
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        options.forEach { option ->
                            val isCorrect = option.char == currentItem.char
                            val isSelected = selectedOption?.char == option.char

                            val buttonColor = if (answered) {
                                when {
                                    isCorrect -> ButtonDefaults.buttonColors(containerColor = Color(0xFFCDEED9), contentColor = Color(0xFF15663A))
                                    isSelected -> ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0B7), contentColor = Color(0xFF922D3B))
                                    else -> ButtonDefaults.filledTonalButtonColors()
                                }
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (!answered) {
                                        answered = true
                                        selectedOption = option
                                        if (isCorrect) score++
                                    }
                                },
                                colors = buttonColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (quizMode == "kana") option.romaji else option.char,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Next / Score footer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Score: $score / ${questionIndex + (if (answered) 1 else 0)}")

                        if (answered) {
                            Button(
                                onClick = {
                                    if (questionIndex < totalQuestions - 1) {
                                        questionIndex++
                                        answered = false
                                        selectedOption = null
                                    } else {
                                        showResults = true
                                    }
                                }
                            ) {
                                Text(if (questionIndex < totalQuestions - 1) "Next" else "Results")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getKanaItems(isHiragana: Boolean, tab: String): List<KanaItem?> {
    return if (isHiragana) {
        when (tab) {
            "basic" -> KanaData.hiraganaBasic
            "dakuten" -> KanaData.hiraganaDakuten
            "handakuten" -> KanaData.hiraganaHandakuten
            "combo" -> KanaData.hiraganaCombination
            else -> getCombinedAllList(true)
        }
    } else {
        when (tab) {
            "basic" -> KanaData.katakanaBasic
            "dakuten" -> KanaData.katakanaDakuten
            "handakuten" -> KanaData.katakanaHandakuten
            "combo" -> KanaData.katakanaCombination
            else -> getCombinedAllList(false)
        }
    }
}

private fun getCombinedAllList(isHiragana: Boolean): List<KanaItem> {
    return if (isHiragana) {
        KanaData.hiraganaBasic.filterNotNull() +
                KanaData.hiraganaDakuten +
                KanaData.hiraganaHandakuten +
                KanaData.hiraganaCombination
    } else {
        KanaData.katakanaBasic.filterNotNull() +
                KanaData.katakanaDakuten +
                KanaData.katakanaHandakuten +
                KanaData.katakanaCombination
    }
}
