package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.KanaData
import com.momin.japanesestudyappn5.data.model.VocabItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class QuizHistoryEntry(val date: String, val quizType: String, val score: Int, val total: Int)

fun recordDailyXp(prefs: android.content.SharedPreferences, points: Int) {
    if (points <= 0) return
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val dateStr = sdf.format(java.util.Date())
    val key = "xp_daily_$dateStr"
    val current = prefs.getInt(key, 0)
    prefs.edit().putInt(key, current + points).apply()
}

fun saveQuizScore(prefs: android.content.SharedPreferences, quizType: String, score: Int, total: Int) {
    val key = "quiz_history"
    val existing = prefs.getString(key, "[]")
    val list = try { Json.decodeFromString<List<QuizHistoryEntry>>(existing ?: "[]") } catch (e: Exception) { emptyList() }
    val now = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date())
    val updated = (listOf(QuizHistoryEntry(now, quizType, score, total)) + list).take(15)

    // Unify XP accumulation: 5 XP per correct answer + 10 completion bonus.
    // Reading and Vocab Quiz already award XP manually in their screens.
    val isAlreadyAwarded = quizType.contains("Vocab Quiz", ignoreCase = true) || quizType.equals("Reading", ignoreCase = true)
    val points = if (isAlreadyAwarded) 0 else (score * 5 + 10)

    val edit = prefs.edit().putString(key, Json.encodeToString(updated))
    if (points > 0) {
        val currentXp = prefs.getInt("xp_total", 0)
        edit.putInt("xp_total", currentXp + points)
        recordDailyXp(prefs, points)
    }
    edit.apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    onAchievementsClick: () -> Unit = {},
    appLanguage: String = "en",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }

    var totalVocab by remember { mutableIntStateOf(0) }
    val masteredCount = remember { prefs.getStringSet("mastered_vocab", emptySet())?.size ?: 0 }
    val bookmarkedCount = remember { prefs.getStringSet("bookmarked_vocab", emptySet())?.size ?: 0 }
    val practicedKana = remember { prefs.getStringSet("practiced_kana", emptySet()) ?: emptySet() }
    val totalOpens = remember { prefs.getInt("total_opens", 0) }

    // Kana counts
    val totalHiragana = KanaData.hiraganaBasic.size +
            KanaData.hiraganaDakuten.size +
            KanaData.hiraganaHandakuten.size +
            KanaData.hiraganaCombination.size

    val totalKatakana = KanaData.katakanaBasic.size +
            KanaData.katakanaDakuten.size +
            KanaData.katakanaHandakuten.size +
            KanaData.katakanaCombination.size

    val practicedH = practicedKana.count { it.startsWith("h_") }
    val practicedK = practicedKana.count { it.startsWith("k_") }

    // Flashcard stats
    var flashcardCorrect by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var total = 0
            val allKeys = prefs.all
            for ((key, value) in allKeys) {
                if (key.startsWith("score_correct_") && value is Int) total += value
            }
            flashcardCorrect = total
        }
    }

    var allVocabList by remember { mutableStateOf<List<VocabItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val vocabAll = repository.getVocabulary()
        totalVocab = vocabAll.size
        allVocabList = vocabAll
    }

    fun buildExportText(): String {
        val masteredIds = prefs.getStringSet("mastered_vocab", emptySet()) ?: emptySet()
        val bookmarkedIds = prefs.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
        val streak = prefs.getInt("streak_count", 1)
        val sb = StringBuilder()
        sb.appendLine("=== 日本語 Study Hub — Progress Report ===")
        sb.appendLine()
        sb.appendLine("🔥 Day Streak: $streak")
        sb.appendLine("📂 App Opens: $totalOpens")
        sb.appendLine("⭐ Bookmarked: ${bookmarkedIds.size} words")
        sb.appendLine("✅ Mastered: ${masteredIds.size} / $totalVocab words")
        sb.appendLine()
        sb.appendLine("=== Mastered Words ===")
        val masteredWords = allVocabList.filter { it.audioId in masteredIds }
        if (masteredWords.isEmpty()) {
            sb.appendLine("(none yet — keep studying!)")
        } else {
            masteredWords.forEach { w ->
                val meaning = if (appLanguage == "bn" && w.bangla.isNotEmpty()) w.bangla else w.english
                sb.appendLine("• ${w.japanese} (${w.romaji}) — $meaning")
            }
        }
        sb.appendLine()
        sb.appendLine("=== Kana Progress ===")
        sb.appendLine("Hiragana: $practicedH / $totalHiragana")
        sb.appendLine("Katakana: $practicedK / $totalKatakana")
        return sb.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Study Stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(
                                listOf(Color(0xFF1E3264), Color(0xFF3D5193), Color(0xFF6B4F9E))
                            ))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("📊 Your Progress", color = Color.White, fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text("Keep studying every day to reach N5 fluency!",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                        }
                    }
                }
            }

            item { WeeklyProgressChart(prefs) }
            item { StudyHeatmap(prefs) }
            item { RivalLeaderboard(prefs) }

            // Vocab section
            item { StatsSectionHeader("📚 Vocabulary") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatsNumberCard("Total Words", "$totalVocab", Color(0xFFE8F0FE), Color(0xFF1A73E8), Modifier.weight(1f))
                    StatsNumberCard("Mastered ✅", "$masteredCount", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                    StatsNumberCard("Bookmarked ⭐", "$bookmarkedCount", Color(0xFFFEF7E0), Color(0xFFF9AB00), Modifier.weight(1f))
                }
            }
            item {
                val masPercent = if (totalVocab > 0) masteredCount * 100 / totalVocab else 0
                StatsProgressRow("Mastery Progress", masPercent, totalVocab, masteredCount, Color(0xFF43A047))
            }

            // Kana section
            item { StatsSectionHeader("🔤 Kana Practice") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatsNumberCard("Hiragana", "$practicedH / $totalHiragana", Color(0xFFEEFCF4), Color(0xFF2E7D32), Modifier.weight(1f))
                    StatsNumberCard("Katakana", "$practicedK / $totalKatakana", Color(0xFFF4F0FF), Color(0xFF6A1B9A), Modifier.weight(1f))
                }
            }
            item {
                val hPct = if (totalHiragana > 0) practicedH * 100 / totalHiragana else 0
                StatsProgressRow("Hiragana Practiced", hPct, totalHiragana, practicedH, Color(0xFF43A047))
            }
            item {
                val kPct = if (totalKatakana > 0) practicedK * 100 / totalKatakana else 0
                StatsProgressRow("Katakana Practiced", kPct, totalKatakana, practicedK, Color(0xFF7B1FA2))
            }

            // SRS Memory Retention section
            item { StatsSectionHeader("🧠 SRS Memory Retention") }
            item { SrsAnalyticsCard(prefs, totalVocab) }

            // Flashcards section
            item { StatsSectionHeader("🃏 Flashcards") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatsNumberCard("Correct Answers", "$flashcardCorrect", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                    StatsNumberCard("App Opens", "$totalOpens", Color(0xFFE8F0FE), Color(0xFF1A73E8), Modifier.weight(1f))
                }
            }

            // Milestone checklist
            item { StatsSectionHeader("🏆 Milestones") }
            item {
                MilestoneList(
                    milestones = listOf(
                        "First word mastered" to (masteredCount >= 1),
                        "10 words mastered" to (masteredCount >= 10),
                        "25 words mastered" to (masteredCount >= 25),
                        "50 words mastered" to (masteredCount >= 50),
                        "100 words mastered" to (masteredCount >= 100),
                        "All Hiragana practiced" to (practicedH >= totalHiragana && totalHiragana > 0),
                        "All Katakana practiced" to (practicedK >= totalKatakana && totalKatakana > 0),
                        "Opened app $totalOpens times" to (totalOpens >= 5)
                    )
                )
            }

            // Quiz History section
            item { StatsSectionHeader("📋 Quiz History") }
            item {
                val historyJson = prefs.getString("quiz_history", "[]")
                val history = try {
                    Json.decodeFromString<List<QuizHistoryEntry>>(historyJson ?: "[]")
                } catch (e: Exception) { emptyList() }

                if (history.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            "No quiz history yet — complete a quiz to see scores here!",
                            Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        history.take(8).forEach { entry ->
                            val accuracy = if (entry.total > 0) entry.score * 100 / entry.total else 0
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.quizType, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(entry.date, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text(
                                        "${entry.score}/${entry.total}",
                                        fontWeight = FontWeight.Bold, fontSize = 14.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            accuracy >= 80 -> Color(0xFF43A047)
                                            accuracy >= 50 -> Color(0xFFFFB300)
                                            else -> Color(0xFFEF5350)
                                        }
                                    ) {
                                        Text(
                                            "$accuracy%",
                                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            color = Color.White, fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Achievements button
            item {
                OutlinedButton(
                    onClick = onAchievementsClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🏆  View Achievements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // Export button
            item {
                Button(
                    onClick = {
                        val text = buildExportText()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "My Japanese Study Progress")
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Progress"))
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("📤 Export Progress", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StatsNumberCard(title: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun StatsProgressRow(label: String, percent: Int, total: Int, done: Int, barColor: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("$done / $total ($percent%)", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent.toFloat() / 100f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = barColor
            )
        }
    }
}

@Composable
private fun MilestoneList(milestones: List<Pair<String, Boolean>>) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            milestones.forEach { (label, achieved) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (achieved) "✅" else "⬜",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = if (achieved) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline,
                        fontWeight = if (achieved) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyProgressChart(prefs: android.content.SharedPreferences) {
    val totalXp = prefs.getInt("xp_total", 0)
    var chartData by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(totalXp) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val labelSdf = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
            val rawList = mutableListOf<Pair<String, Int>>()

            var sumDaily = 0
            for (i in 6 downTo 0) {
                val date = java.util.Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L)
                val dateStr = sdf.format(date)
                val label = labelSdf.format(date)
                val xp = prefs.getInt("xp_daily_$dateStr", 0)
                sumDaily += xp
                rawList.add(label to xp)
            }

            if (sumDaily == 0 && totalXp > 0) {
                val seedList = mutableListOf<Pair<String, Int>>()
                val parts = listOf((totalXp * 0.25).toInt(), (totalXp * 0.35).toInt(), (totalXp * 0.4).toInt())
                for (i in 6 downTo 0) {
                    val date = java.util.Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L)
                    val dateStr = sdf.format(date)
                    val label = labelSdf.format(date)
                    val xp = when (i) {
                        2 -> parts[0]
                        1 -> parts[1]
                        0 -> parts[2]
                        else -> 0
                    }
                    prefs.edit().putInt("xp_daily_$dateStr", xp).apply()
                    seedList.add(label to xp)
                }
                chartData = seedList
            } else {
                chartData = rawList
            }
        }
    }

    if (chartData.isEmpty()) return

    val data = chartData

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📈 Weekly Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("XP earned over the last 7 days", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))

            val maxVal = (data.maxOfOrNull { it.second } ?: 100).coerceAtLeast(100).toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    val paddingLeft = 60f
                    val paddingRight = 20f
                    val paddingTop = 20f
                    val paddingBottom = 40f
                    
                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Draw horizontal grid lines (3 lines)
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = paddingTop + chartHeight * (i.toFloat() / gridSteps)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.2f),
                            start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                            end = androidx.compose.ui.geometry.Offset(width - paddingRight, y),
                            strokeWidth = 2f
                        )
                    }

                    // Build path coordinates
                    val points = data.mapIndexed { idx, pair ->
                        val x = paddingLeft + (idx.toFloat() / 6f) * chartWidth
                        val y = paddingTop + chartHeight - (pair.second.toFloat() / maxVal) * chartHeight
                        androidx.compose.ui.geometry.Offset(x, y)
                    }

                    // Draw smooth curve (cubic Bezier)
                    if (points.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                val controlX = (p1.x + p2.x) / 2f
                                cubicTo(controlX, p1.y, controlX, p2.y, p2.x, p2.y)
                            }
                        }
                        
                        // Fill path
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, paddingTop + chartHeight)
                            lineTo(points.first().x, paddingTop + chartHeight)
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E3264).copy(alpha = 0.35f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        drawPath(
                            path = path,
                            color = Color(0xFF3D5193),
                            style = Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Draw points and labels
                        points.forEachIndexed { idx, pt ->
                            drawCircle(
                                color = Color(0xFF6B4F9E).copy(alpha = 0.4f),
                                radius = 12f,
                                center = pt
                            )
                            drawCircle(
                                color = Color(0xFF6B4F9E),
                                radius = 6f,
                                center = pt
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5f,
                                center = pt
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    data.forEach { pair ->
                        Text(
                            text = pair.first,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxHeight().padding(start = 2.dp, top = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${maxVal.toInt()} XP", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    Text("0 XP", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 14.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyHeatmap(prefs: android.content.SharedPreferences) {
    val totalXp = prefs.getInt("xp_total", 0)
    val cells = remember(totalXp) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val list = mutableListOf<Int>()
        for (i in 27 downTo 0) {
            val date = java.util.Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L)
            val dateStr = sdf.format(date)
            list.add(prefs.getInt("xp_daily_$dateStr", 0))
        }
        list
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🗓️ Daily Study Heatmap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Your study activity over the last 4 weeks", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Mon", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(14.dp))
                    Text("Wed", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(14.dp))
                    Text("Fri", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    for (w in 0..3) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            for (d in 0..6) {
                                val index = w * 7 + d
                                val xp = if (index < cells.size) cells[index] else 0
                                val color = when {
                                    xp == 0 -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                    xp < 25 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    xp < 75 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RivalLeaderboard(prefs: android.content.SharedPreferences) {
    val totalXp = prefs.getInt("xp_total", 0)
    val rivals = remember(totalXp) {
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        listOf(
            RivalEntry("🥇 Hiroto (Tokyo)", 1350 + (dayOfYear % 15) * 12, "🔥 Streak 12"),
            RivalEntry("🥈 You", totalXp, "🎯 Mastered"),
            RivalEntry(" Yuki (Osaka)", 910 + (dayOfYear % 9) * 15, "⭐ Reviewing"),
            RivalEntry(" Aoi (Kyoto)", 560 + (dayOfYear % 7) * 20, "📝 Tracing"),
            RivalEntry(" Daiki (Fukuoka)", 310 + (dayOfYear % 13) * 8, "🎓 Beginner")
        ).sortedByDescending { it.xp }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🏆 Tokyo League Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Compare your overall XP with simulated rivals!", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rivals.forEachIndexed { rank, entry ->
                    val isUser = entry.name.contains("You")
                    val rowBg = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rowBg)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${rank + 1}  ",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 13.sp
                            )
                            Column {
                                Text(
                                    text = entry.name,
                                    fontWeight = if (isUser) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = textColor,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = entry.status,
                                    fontSize = 10.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            text = "${entry.xp} XP",
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private data class RivalEntry(val name: String, val xp: Int, val status: String)

@Composable
private fun SrsAnalyticsCard(prefs: android.content.SharedPreferences, totalVocab: Int) {
    var learningCount by remember { mutableIntStateOf(0) }
    var reviewCount by remember { mutableIntStateOf(0) }
    var masteredCount by remember { mutableIntStateOf(0) }
    var dueTodayCount by remember { mutableIntStateOf(0) }
    var avgEaseFactor by remember { mutableFloatStateOf(2.5f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val now = System.currentTimeMillis()
            var learn = 0
            var rev = 0
            var mast = 0
            var due = 0
            var sumEf = 0f
            var countSrs = 0

            val allKeys = prefs.all
            for ((key, value) in allKeys) {
                if (key.startsWith("srs_card_") && value is String) {
                    try {
                        val json = org.json.JSONObject(value)
                        val reps = json.optInt("reps", 0)
                        val ef = json.optDouble("ef", 2.5).toFloat()
                        val dueMillis = json.optLong("due", 0L)

                        countSrs++
                        sumEf += ef

                        when {
                            reps == 0 -> learn++
                            reps in 1..2 -> rev++
                            else -> mast++
                        }

                        if (dueMillis <= now) due++
                    } catch (e: Exception) {}
                }
            }

            learningCount = learn
            reviewCount = rev
            masteredCount = mast
            dueTodayCount = due
            avgEaseFactor = if (countSrs > 0) sumEf / countSrs else 2.5f
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🧠 SuperMemo SM-2 Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Memory retention & review scheduling", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Avg EF: ${String.format(java.util.Locale.US, "%.2f", avgEaseFactor)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatsNumberCard("Learning", "$learningCount", Color(0xFFFFF3E0), Color(0xFFE65100), Modifier.weight(1f))
                StatsNumberCard("Reviewing", "$reviewCount", Color(0xFFE8F0FE), Color(0xFF1976D2), Modifier.weight(1f))
                StatsNumberCard("Mastered", "$masteredCount", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                StatsNumberCard("Due Today", "$dueTodayCount", Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
            }
        }
    }
}

