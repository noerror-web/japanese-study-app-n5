package com.momin.japanesestudyappn5.ui.screens

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.KanaData
import com.momin.japanesestudyappn5.data.model.KanaItem
import com.momin.japanesestudyappn5.data.DataRepository
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaTraceScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    initialChar: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", android.content.Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current
    val allKana: List<KanaItem> = remember {
        (KanaData.hiraganaBasic + KanaData.hiraganaDakuten +
        KanaData.hiraganaHandakuten + KanaData.hiraganaCombination).filterNotNull()
    }
    val katakanaAll: List<KanaItem> = remember {
        (KanaData.katakanaBasic + KanaData.katakanaDakuten +
        KanaData.katakanaHandakuten + KanaData.katakanaCombination).filterNotNull()
    }

    var kanjiItems by remember { mutableStateOf<List<com.momin.japanesestudyappn5.data.model.KanjiItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        kanjiItems = repository.getKanjis()
    }
    val kanjiAll: List<KanaItem> = remember(kanjiItems) {
        kanjiItems.map { KanaItem(char = it.kanji, romaji = it.meanings, type = "kanji") }
    }

    var selectedMode by remember { mutableIntStateOf(if (initialChar != null && katakanaAll.any { it.char == initialChar }) 1 else 0) }

    var currentIndex by remember { mutableIntStateOf(0) }

    val displayList: List<KanaItem> = when (selectedMode) {
        0 -> allKana
        1 -> katakanaAll
        else -> if (kanjiAll.isNotEmpty()) kanjiAll else allKana
    }
    if (displayList.isEmpty()) return
    val currentKana: KanaItem = displayList[currentIndex % displayList.size]

    var combinationSvgContents by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(currentKana, selectedMode) {
        if (selectedMode == 0 || selectedMode == 1) {
            val charList = currentKana.char.map { it.toString() }
            val svgs = mutableListOf<String>()
            for (c in charList) {
                val svg = if (selectedMode == 0) repository.getHiraganaSvg(c) else repository.getKatakanaSvg(c)
                if (svg != null) svgs.add(svg)
            }
            combinationSvgContents = svgs
        } else {
            combinationSvgContents = emptyList()
        }
    }
    val characterPathsList = remember(combinationSvgContents, currentKana, selectedMode, kanjiItems) {
        if (selectedMode == 2) {
            val kanjiMatch = kanjiItems.find { it.kanji == currentKana.char }
            if (kanjiMatch != null) listOf(kanjiMatch.svgPaths) else emptyList()
        } else if (combinationSvgContents.isNotEmpty()) {
            val pathPattern = """d="([^"]+)"""".toRegex()
            combinationSvgContents.map { svg ->
                pathPattern.findAll(svg).map { it.groupValues[1] }.toList()
            }
        } else emptyList()
    }
    val svgPaths = remember(characterPathsList) { characterPathsList.flatten() }

    // Drawing paths: list of strokes, each stroke is a list of Offsets
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Writing Practice", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        val surfaceColor = MaterialTheme.colorScheme.surface
        val ghostColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        val strokeColor = MaterialTheme.colorScheme.primary
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

        val controlsTop = @Composable {
            // Hiragana / Katakana / N5 Kanji toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedMode == 0,
                    onClick = {
                        selectedMode = 0
                        currentIndex = 0
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    label = { Text("Hiragana") }
                )
                FilterChip(
                    selected = selectedMode == 1,
                    onClick = {
                        selectedMode = 1
                        currentIndex = 0
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    label = { Text("Katakana") }
                )
                FilterChip(
                    selected = selectedMode == 2,
                    onClick = {
                        selectedMode = 2
                        currentIndex = 0
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    label = { Text("N5 Kanji") }
                )
            }
            Spacer(Modifier.height(8.dp))

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${currentIndex % displayList.size + 1} / ${displayList.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    currentKana.romaji.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        val canvasBox = @Composable { modifier: Modifier ->
            Box(modifier = modifier.background(surfaceColor, RoundedCornerShape(24.dp))) {
                // Ghost character/Stroke order animation in background
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (characterPathsList.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .graphicsLayer { alpha = 0.25f }
                        ) {
                            KanjiStrokeAnimation(
                                characterPathsList = characterPathsList,
                                activeColor = MaterialTheme.colorScheme.primary,
                                baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                strokeWidth = 10f
                            )
                        }
                    } else {
                        Text(
                            text = currentKana.char,
                            fontSize = 200.sp,
                            color = ghostColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Canvas for drawing
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentKana) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke = listOf(offset)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, _ ->
                                    currentStroke = currentStroke + change.position
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes = strokes + listOf(currentStroke)
                                        currentStroke = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    // Grid guide lines
                    drawLine(outlineColor, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1.5f)
                    drawLine(outlineColor, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.5f)

                    // Completed strokes
                    strokes.forEach { stroke ->
                        if (stroke.size >= 2) {
                            for (i in 0 until stroke.size - 1) {
                                drawLine(
                                    color = strokeColor,
                                    start = stroke[i],
                                    end = stroke[i + 1],
                                    strokeWidth = 14f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                    // Current stroke
                    if (currentStroke.size >= 2) {
                        for (i in 0 until currentStroke.size - 1) {
                            drawLine(
                                color = strokeColor,
                                start = currentStroke[i],
                                end = currentStroke[i + 1],
                                strokeWidth = 14f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Stroke count badge
                if (strokes.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "✏️ ${strokes.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val controlsBottom = @Composable {
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Clear") }

                Button(
                    onClick = {
                        val today = java.time.LocalDate.now().toString()
                        if (!prefs.getBoolean("quest_trace_done_$today", false)) {
                            prefs.edit()
                                .putBoolean("quest_trace_done_$today", true)
                                .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                .putInt("xp_total", prefs.getInt("xp_total", 0) + 20)
                                .apply()
                        }
                        currentIndex++
                        strokes = emptyList()
                        currentStroke = emptyList()
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Next →") }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Trace the character shown above. Hold to start drawing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                canvasBox(Modifier.weight(1f).fillMaxHeight())
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    controlsTop()
                    Spacer(Modifier.height(24.dp))
                    controlsBottom()
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                controlsTop()
                Spacer(Modifier.height(8.dp))
                canvasBox(Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                controlsBottom()
            }
        }
    }
}
