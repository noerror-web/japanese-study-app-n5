package com.momin.japanesestudyappn5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.JMdictEntry
import com.momin.japanesestudyappn5.data.model.KanjiDicEntry
import com.momin.japanesestudyappn5.data.model.TatoebaSentence
import com.momin.japanesestudyappn5.data.repository.DictionaryRepository
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailBottomSheet(
    entry: JMdictEntry,
    dictionaryRepository: DictionaryRepository,
    onDismiss: () -> Unit,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kanjiBreakdown by remember { mutableStateOf<List<KanjiDicEntry>>(emptyList()) }
    var tatoebaSentences by remember { mutableStateOf<List<TatoebaSentence>>(emptyList()) }
    var selectedKanjiDetail by remember { mutableStateOf<KanjiDicEntry?>(null) }

    LaunchedEffect(entry) {
        val targetWord = if (entry.kanji.isNotBlank()) entry.kanji else entry.reading
        val kanjiChars = targetWord.filter { char ->
            char in '\u4e00'..'\u9faf' || char in '\u3400'..'\u4dbf'
        }.map { it.toString() }.distinct()

        val foundKanji = mutableListOf<KanjiDicEntry>()
        for (char in kanjiChars) {
            val k = dictionaryRepository.getKanjiByChar(char)
            if (k != null) foundKanji.add(k)
        }
        kanjiBreakdown = foundKanji

        tatoebaSentences = dictionaryRepository.searchSentences(targetWord).take(5)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val displayFurigana = if (entry.furigana.isNotBlank()) entry.furigana else if (entry.kanji.isNotBlank()) "${entry.kanji}[${entry.reading}]" else entry.reading
                        FuriganaText(
                            rawText = displayFurigana,
                            mainFontSize = 32.sp,
                            furiganaFontSize = 14.sp,
                            mainColor = MaterialTheme.colorScheme.primary
                        )
                        if (entry.romaji.isNotBlank()) {
                            Text(
                                text = entry.romaji,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { AudioPlayer.playTts(context, if (entry.kanji.isNotBlank()) entry.kanji else entry.reading) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Text("🔊", fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }

            // Priority & JLPT Badges
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.isCommon) {
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Common",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (!entry.jlptLevel.isNullOrBlank()) {
                        Surface(
                            color = Color(0xFF1565C0),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                entry.jlptLevel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    entry.priority.forEach { prio ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                prio,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Meanings / Senses Section (JMdict)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📖 Meanings (JMdict)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))

                        entry.senses.forEachIndexed { index, sense ->
                            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                if (sense.partsOfSpeech.isNotEmpty()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        sense.partsOfSpeech.forEach { pos ->
                                            Surface(
                                                color = Color(0xFF6A1B9A).copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    pos,
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF6A1B9A),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                val displayMeanings = if (appLanguage == "bn" && !entry.bangla.isNullOrBlank()) {
                                    "${index + 1}. ${entry.bangla} (${sense.meanings.joinToString(", ")})"
                                } else {
                                    "${index + 1}. ${sense.meanings.joinToString(", ")}"
                                }

                                Text(
                                    text = displayMeanings,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Kanji Breakdown (KANJIDIC2)
            if (kanjiBreakdown.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🈁 Kanji Breakdown (KANJIDIC2)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBD1F2D)
                            )
                            Spacer(Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                kanjiBreakdown.forEach { kanjiItem ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedKanjiDetail = kanjiItem },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(kanjiItem.kanji, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBD1F2D))
                                            Spacer(Modifier.height(4.dp))
                                            Text(kanjiItem.meanings.take(2).joinToString(", "), fontSize = 11.sp, maxLines = 1, color = Color.Gray)
                                            Spacer(Modifier.height(2.dp))
                                            Text("Strokes: ${kanjiItem.strokeCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tatoeba Example Sentences
            if (tatoebaSentences.isNotEmpty()) {
                item {
                    Text(
                        text = "💬 Example Sentences (Tatoeba)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(tatoebaSentences) { sentence ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            val furiganaStr = if (sentence.furigana.isNotBlank()) sentence.furigana else sentence.japanese
                            FuriganaText(rawText = furiganaStr, mainFontSize = 18.sp, furiganaFontSize = 11.sp)
                            Spacer(Modifier.height(6.dp))
                            val trans = if (appLanguage == "bn" && !sentence.bangla.isNullOrBlank()) sentence.bangla else sentence.english
                            Text(trans, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Source: ${sentence.attribution}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // Kanji Detail Modal Dialog
    if (selectedKanjiDetail != null) {
        val k = selectedKanjiDetail!!
        AlertDialog(
            onDismissRequest = { selectedKanjiDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k.kanji, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBD1F2D))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("KANJIDIC2 Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${k.strokeCount} strokes • Grade ${k.grade ?: "-"} • ${k.jlptLevel ?: "N5"}", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Meanings: ${k.meanings.joinToString(", ")}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    if (k.onyomi.isNotEmpty()) {
                        Text("On'yomi (音): ${k.onyomi.joinToString(", ")}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (k.kunyomi.isNotEmpty()) {
                        Text("Kun'yomi (訓): ${k.kunyomi.joinToString(", ")}", fontSize = 13.sp, color = Color(0xFF2E7D32))
                    }
                    if (k.examples.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Common Words:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        k.examples.forEach { ex ->
                            Text("• ${ex.word} (${ex.reading}) - ${ex.meaning}", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedKanjiDetail = null }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
