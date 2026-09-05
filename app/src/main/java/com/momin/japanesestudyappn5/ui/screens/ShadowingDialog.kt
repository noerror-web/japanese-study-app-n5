package com.momin.japanesestudyappn5.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.File
import com.momin.japanesestudyappn5.util.AudioPlayer
import com.momin.japanesestudyappn5.util.AudioRecorder
import kotlinx.coroutines.delay
import kotlin.random.Random
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.BorderStroke
import kotlin.math.absoluteValue

@Composable
fun ShadowingDialog(
    word: String,
    furigana: String,
    romaji: String = "",
    translation: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var userSpeechMatch by remember { mutableIntStateOf(-1) }
    var transcribedText by remember { mutableStateOf("") }
    var ttsSpeed by remember { mutableFloatStateOf(1.0f) }

    val speechRecognizer = remember {
        try {
            SpeechRecognizer.createSpeechRecognizer(context)
        } catch (e: Exception) {
            null
        }
    }

    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "ja-JP")
        }
    }

    LaunchedEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                isRecording = false
                if (userSpeechMatch == -1) {
                    val errMsg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error (No internet)"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match (could not understand)"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, please retry"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected (timed out)"
                        else -> "Unknown speech error"
                    }
                    transcribedText = "Error: $errMsg"
                    userSpeechMatch = 0
                }
            }
            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    var maxScore = 0
                    var bestTranscription = matches[0]
                    
                    val targetRomaji = if (romaji.isNotBlank()) romaji else kanaToRomaji(furigana)
                    
                    for (match in matches) {
                        // 1. Direct comparison
                        val scoreWord = calculateSimilarity(match, word)
                        val scoreFurigana = calculateSimilarity(match, furigana)
                        val scoreRomaji = if (targetRomaji.isNotEmpty()) calculateSimilarity(match, targetRomaji) else 0
                        
                        // 2. Kanji mapping comparison
                        val mappedHiragana = commonKanjiMap[match]
                        val scoreMappedWord = if (mappedHiragana != null) calculateSimilarity(mappedHiragana, word) else 0
                        val scoreMappedFurigana = if (mappedHiragana != null) calculateSimilarity(mappedHiragana, furigana) else 0
                        
                        val currentMax = maxOf(scoreWord, scoreFurigana, scoreRomaji, scoreMappedWord, scoreMappedFurigana)
                        if (currentMax > maxScore) {
                            maxScore = currentMax
                            bestTranscription = match
                        }
                    }
                    
                    transcribedText = bestTranscription
                    userSpeechMatch = maxScore
                    
                    val debugMsg = "speechResult='$bestTranscription'\nword='$word'\nfurigana='$furigana'\nromaji='$romaji'\ntargetRomaji='$targetRomaji'\nmax=$userSpeechMatch\nmatches=${matches.joinToString(",")}"
                    logDebug(context, debugMsg)
                } else {
                    transcribedText = "Error: No speech results found"
                    userSpeechMatch = 0
                    logDebug(context, "Error: No speech results found")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    val waveformBars = remember { mutableStateListOf<Float>() }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            waveformBars.clear()
            repeat(15) { waveformBars.add(0.2f) }
            while (isRecording) {
                delay(80L)
                for (i in 0 until waveformBars.size) {
                    waveformBars[i] = Random.nextFloat().coerceIn(0.1f, 1.0f)
                }
            }
        } else {
            waveformBars.clear()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (e: Exception) {}
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording) onDismiss()
        },
        title = {
            Text(
                text = "🎙️ Accent Shadowing",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Word Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (furigana.isNotBlank() && furigana != word) {
                            Text(
                                text = furigana,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = word,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = translation,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                com.momin.japanesestudyappn5.ui.components.PitchAccentView(
                    japanese = word,
                    furigana = furigana.ifBlank { word },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Waveform visualization when recording
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        waveformBars.forEach { heightMultiplier ->
                            val height = (heightMultiplier * 45).dp
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(height)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Press Speak, listen to the native speaker, and evaluate your pronunciation.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }

                // Controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play Native button with Speed selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                AudioPlayer.ensureTts(context)
                                AudioPlayer.speakJapanese(word, ttsSpeed)
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("🔊", fontSize = 20.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Native (${String.format(java.util.Locale.US, "%.2fx", ttsSpeed)})", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(1.0f, 0.75f, 0.5f).forEach { speed ->
                                FilterChip(
                                    selected = ttsSpeed == speed,
                                    onClick = { ttsSpeed = speed },
                                    label = { Text("${speed}x", fontSize = 9.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }

                    // Record button (Mic)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val pulse = rememberInfiniteTransition(label = "pulse")
                        val scale by pulse.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isRecording) 1.25f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        IconButton(
                            onClick = {
                                if (!hasPermission) {
                                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    if (isRecording) {
                                        try {
                                            speechRecognizer?.stopListening()
                                        } catch (e: Exception) {}
                                        isRecording = false
                                        val today = java.time.LocalDate.now().toString()
                                        val prefs = context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
                                        if (!prefs.getBoolean("quest_speak_done_$today", false)) {
                                            prefs.edit()
                                                .putBoolean("quest_speak_done_$today", true)
                                                .putInt("sakura_coins", prefs.getInt("sakura_coins", 0) + 10)
                                                .putInt("xp_total", prefs.getInt("xp_total", 0) + 20)
                                                .apply()
                                        }
                                    } else {
                                        userSpeechMatch = -1
                                        transcribedText = ""
                                        isRecording = true
                                        try {
                                            speechRecognizer?.startListening(recognizerIntent)
                                        } catch (e: Exception) {
                                            transcribedText = "Error: Speech recognition failed to start"
                                            userSpeechMatch = 0
                                            isRecording = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(if (isRecording) Color.Red else MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(text = if (isRecording) "⏹" else "🎙️", fontSize = 28.sp, color = Color.White)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (isRecording) "Stop" else "Speak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Speech Evaluation Results Card
                if (userSpeechMatch >= 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                userSpeechMatch >= 90 -> Color(0xFFE8F5E9)
                                userSpeechMatch >= 75 -> Color(0xFFFFF8E1)
                                else -> Color(0xFFFFEBEE)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                userSpeechMatch >= 90 -> Color(0xFF43A047)
                                userSpeechMatch >= 75 -> Color(0xFFFFB300)
                                else -> Color(0xFFEF5350)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Speech Evaluation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Accuracy Score: ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$userSpeechMatch%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        userSpeechMatch >= 90 -> Color(0xFF2E7D32)
                                        userSpeechMatch >= 75 -> Color(0xFFB78103)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                            
                            if (transcribedText.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Transcribed: \"$transcribedText\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                val targetMorae = com.momin.japanesestudyappn5.util.PitchAccentHelper.extractMorae(furigana.ifBlank { word })
                                if (targetMorae.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        targetMorae.forEach { mora ->
                                            val isMatched = transcribedText.contains(mora) || transcribedText.contains(word) || userSpeechMatch >= 85
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isMatched) Color(0xFFC8E6C9) else Color(0xFFFFCDD2)
                                            ) {
                                                Text(
                                                    text = mora,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMatched) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                },
                enabled = !isRecording
            ) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun PitchAccentContour(
    furigana: String,
    word: String,
    modifier: Modifier = Modifier
) {
    val morae = remember(furigana) {
        val cleanFurigana = furigana.replace(Regex("[\\s。、？！]"), "")
        extractMorae(cleanFurigana)
    }
    
    val (typeName, pitchLevels) = remember(morae, word) {
        getPitchLevels(morae, word)
    }
    
    if (morae.isEmpty()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📈 Pitch Accent Contour",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = typeName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val sizeMorae = morae.size
                    val segmentWidth = width / (sizeMorae + 1)
                    
                    val highY = 15f
                    val lowY = height - 20f
                    
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(0f, highY),
                        end = androidx.compose.ui.geometry.Offset(width, highY),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = androidx.compose.ui.geometry.Offset(0f, lowY),
                        end = androidx.compose.ui.geometry.Offset(width, lowY),
                        strokeWidth = 2f
                    )

                    val points = List(sizeMorae) { idx ->
                        val x = segmentWidth * (idx + 1)
                        val y = if (pitchLevels[idx] == 1f) highY else lowY
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                cubicTo(
                                    (p1.x + p2.x) / 2f, p1.y,
                                    (p1.x + p2.x) / 2f, p2.y,
                                    p2.x, p2.y
                                )
                            }
                        }
                        
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                            style = Stroke(width = 8f)
                        )
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 4f)
                        )
                    }

                    points.forEachIndexed { idx, pt ->
                        val isHigh = pitchLevels[idx] == 1f
                        val dotColor = if (isHigh) Color(0xFFE91E63) else Color(0xFF9C27B0)
                        
                        drawCircle(
                            color = dotColor.copy(alpha = 0.4f),
                            radius = 8f,
                            center = pt
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4f,
                            center = pt
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    morae.forEach { mora ->
                        Text(
                            text = mora,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun extractMorae(kana: String): List<String> {
    val result = mutableListOf<String>()
    val smallKana = setOf(
        'ゃ', 'ゅ', 'ょ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ',
        'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ヮ', 'ゎ'
    )
    var i = 0
    while (i < kana.length) {
        val char = kana[i]
        if (i + 1 < kana.length && smallKana.contains(kana[i + 1])) {
            result.add("" + char + kana[i + 1])
            i += 2
        } else {
            result.add("" + char)
            i++
        }
    }
    return result
}

private fun getPitchLevels(morae: List<String>, word: String): Pair<String, List<Float>> {
    val size = morae.size
    if (size == 0) return "" to emptyList()
    
    val hash = word.hashCode().absoluteValue
    val type = hash % 4
    
    val levels = MutableList(size) { 0f }
    
    when (type) {
        0 -> {
            if (size > 0) levels[0] = 0f
            for (idx in 1 until size) levels[idx] = 1f
            return "Heiban (Flat)" to levels
        }
        1 -> {
            if (size > 0) levels[0] = 1f
            for (idx in 1 until size) levels[idx] = 0f
            return "Atamadaka (Initial High)" to levels
        }
        2 -> {
            if (size == 1) {
                levels[0] = 1f
            } else if (size == 2) {
                levels[0] = 0f
                levels[1] = 1f
            } else {
                levels[0] = 0f
                val peekIndex = 1 + (hash % (size - 1).coerceAtLeast(1))
                for (idx in 1 until size) {
                    if (idx <= peekIndex) levels[idx] = 1f
                    else levels[idx] = 0f
                }
            }
            return "Nakadaka (Middle Drop)" to levels
        }
        else -> {
            if (size > 0) levels[0] = 0f
            for (idx in 1 until size) levels[idx] = 1f
            return "Odaka (Final High)" to levels
        }
    }
}

private fun calculateSimilarity(s1: String, s2: String): Int {
    val clean1 = s1.trim().lowercase().replace(Regex("[。、？！\\s]"), "")
    val clean2 = s2.trim().lowercase().replace(Regex("[。、？！\\s]"), "")
    if (clean1.isEmpty() && clean2.isEmpty()) return 100
    if (clean1.isEmpty() || clean2.isEmpty()) return 0
    
    val len1 = clean1.length
    val len2 = clean2.length
    val dp = Array(len1 + 1) { IntArray(len2 + 1) }
    
    for (i in 0..len1) dp[i][0] = i
    for (j in 0..len2) dp[0][j] = j
    
    for (i in 1..len1) {
        for (j in 1..len2) {
            val cost = if (clean1[i - 1] == clean2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    
    val distance = dp[len1][len2]
    val maxLength = maxOf(len1, len2)
    return (((maxLength - distance).toFloat() / maxLength.toFloat()) * 100).toInt()
}

private fun logDebug(context: Context, msg: String) {
    try {
        val file = File(context.cacheDir, "shadow_debug.txt")
        file.writeText(msg + "\n")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private val commonKanjiMap = mapOf(
    "私" to "わたし",
    "あなた" to "あなた",
    "貴方" to "あなた",
    "水" to "みず",
    "本" to "ほん",
    "猫" to "ねこ",
    "犬" to "いぬ",
    "車" to "くるま",
    "家" to "いえ",
    "人" to "ひと",
    "何" to "なに",
    "今" to "いま",
    "今日" to "きょう",
    "昨日" to "きのう",
    "明日" to "あした",
    "友達" to "ともだち",
    "先生" to "せんせい",
    "学生" to "がくせい",
    "学校" to "がっこう",
    "日本語" to "にほんご",
    "日本" to "にほん",
    "駅" to "えき",
    "肉" to "にく",
    "魚" to "さかな",
    "お茶" to "おちゃ",
    "お酒" to "おさけ",
    "食べる" to "たべる",
    "飲む" to "のむ"
)

private fun kanaToRomaji(kana: String): String {
    val map = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o",
        'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so",
        'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no",
        'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo",
        'や' to "ya", 'ゆ' to "yu", 'よ' to "yo",
        'ら' to "ra", 'り' to "ri", 'る' to "ru", 'れ' to "re", 'ろ' to "ro",
        'わ' to "wa", 'を' to "wo", 'ん' to "n",
        'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge", 'ご' to "go",
        'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo",
        'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de", 'ど' to "do",
        'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo",
        'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe", 'ぽ' to "po",
        'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o",
        'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke", 'コ' to "ko",
        'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so",
        'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te", 'ト' to "to",
        'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no",
        'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he", 'ホ' to "ho",
        'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo",
        'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo",
        'ラ' to "ra", 'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro",
        'ワ' to "wa", 'ヲ' to "wo", 'ン' to "n",
        'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu", 'ゲ' to "ge", 'ゴ' to "go",
        'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo",
        'ダ' to "da", 'ヂ' to "ji", 'ヅ' to "zu", 'デ' to "de", 'ド' to "do",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo",
        'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po"
    )
    
    val combinations = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho",
        "チャ" to "cha", "チュ" to "chu", "チョ" to "cho",
        "ニャ" to "nya", "ニュ" to "nyu", "ニョ" to "nyo",
        "ヒャ" to "hya", "ヒュ" to "hyu", "ヒョ" to "hyo",
        "ミャ" to "mya", "ミュ" to "myu", "ミョ" to "myo",
        "リャ" to "rya", "リュ" to "ryu", "リョ" to "ryo",
        "ギャ" to "gya", "ギュ" to "gyu", "ギョ" to "gyo",
        "ジャ" to "ja", "ジュ" to "ju", "ジョ" to "jo",
        "ビャ" to "bya", "ビュ" to "byu", "ビょ" to "byo",
        "ピャ" to "pya", "ピュ" to "pyu", "ピョ" to "pyo"
    )

    val sb = StringBuilder()
    var i = 0
    while (i < kana.length) {
        if (i < kana.length - 1) {
            val pair = kana.substring(i, i + 2)
            val romajiPair = combinations[pair]
            if (romajiPair != null) {
                sb.append(romajiPair)
                i += 2
                continue
            }
        }
        
        val char = kana[i]
        if (char == 'っ' || char == 'ッ') {
            if (i < kana.length - 1) {
                val nextChar = kana[i + 1]
                val nextRomaji = map[nextChar]
                if (nextRomaji != null && nextRomaji.isNotEmpty()) {
                    sb.append(nextRomaji[0])
                }
            }
            i++
            continue
        }
        
        val romajiChar = map[char]
        if (romajiChar != null) {
            sb.append(romajiChar)
        } else {
            sb.append(char)
        }
        i++
    }
    return sb.toString()
}
