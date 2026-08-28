package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.model.JMdictEntry
import com.momin.japanesestudyappn5.data.model.KanjiDicEntry
import com.momin.japanesestudyappn5.data.model.TatoebaSentence
import com.momin.japanesestudyappn5.data.repository.DictionaryRepository
import com.momin.japanesestudyappn5.ui.components.FuriganaText
import com.momin.japanesestudyappn5.ui.components.WordDetailBottomSheet
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch

private sealed class DictSearchResult {
    data class Word(val entry: JMdictEntry) : DictSearchResult()
    data class Kanji(val entry: KanjiDicEntry) : DictSearchResult()
    data class Sentence(val sentence: TatoebaSentence) : DictSearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    onBack: () -> Unit,
    dictionaryRepository: DictionaryRepository,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("食") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Words, 2: Kanji, 3: Sentences

    var wordsResults by remember { mutableStateOf<List<JMdictEntry>>(emptyList()) }
    var kanjiResults by remember { mutableStateOf<List<KanjiDicEntry>>(emptyList()) }
    var sentenceResults by remember { mutableStateOf<List<TatoebaSentence>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    var activeWordDetail by remember { mutableStateOf<JMdictEntry?>(null) }
    var selectedKanjiDialog by remember { mutableStateOf<KanjiDicEntry?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (query.trim().isEmpty()) {
            wordsResults = emptyList()
            kanjiResults = emptyList()
            sentenceResults = emptyList()
            return@LaunchedEffect
        }
        isLoading = true
        scope.launch {
            val q = query.trim()
            wordsResults = dictionaryRepository.searchWords(q)
            kanjiResults = dictionaryRepository.searchKanji(q)
            sentenceResults = dictionaryRepository.searchSentences(q)
            isLoading = false
        }
    }

    val combinedResults: List<DictSearchResult> = remember(selectedFilterIndex, wordsResults, kanjiResults, sentenceResults) {
        when (selectedFilterIndex) {
            1 -> wordsResults.map { DictSearchResult.Word(it) }
            2 -> kanjiResults.map { DictSearchResult.Kanji(it) }
            3 -> sentenceResults.map { DictSearchResult.Sentence(it) }
            else -> {
                val list = mutableListOf<DictSearchResult>()
                list.addAll(kanjiResults.map { DictSearchResult.Kanji(it) })
                list.addAll(wordsResults.map { DictSearchResult.Word(it) })
                list.addAll(sentenceResults.map { DictSearchResult.Sentence(it) })
                list
            }
        }
    }

    var isDictDownloaded by remember { mutableStateOf(com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDictionaryDownloaded(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📕 Japanese Dictionary", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Online Download Banner if Expanded Dictionary is not downloaded yet
            if (!isDictDownloaded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚡ Expanded Japanese Dictionary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("Download full multi-thousand JMdict words, KANJIDIC2 entries & Tatoeba sentences for offline search.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f))
                            }
                            Spacer(Modifier.width(8.dp))
                            if (isDownloading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            } else {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isDownloading = true
                                            downloadProgress = 0f
                                            val result = com.momin.japanesestudyappn5.util.OnlineAssetsManager.downloadDictionary(context) { p ->
                                                downloadProgress = p
                                            }
                                            if (result.isSuccess) {
                                                dictionaryRepository.reload()
                                                android.widget.Toast.makeText(context, "Expanded Dictionary downloaded successfully! ✓", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                val err = result.exceptionOrNull()?.message ?: "Download failed"
                                                android.widget.Toast.makeText(context, "Download error: $err", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                            isDownloading = false
                                            isDictDownloaded = com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDictionaryDownloaded(context)
                                            val q = query.trim()
                                            if (q.isNotEmpty()) {
                                                wordsResults = dictionaryRepository.searchWords(q)
                                                kanjiResults = dictionaryRepository.searchKanji(q)
                                                sentenceResults = dictionaryRepository.searchSentences(q)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Download", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (isDownloading) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = downloadProgress, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }

            // Search Input Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search Kanji, Kana, Romaji, or English…") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    // Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filterLabels = listOf(
                            "All (${wordsResults.size + kanjiResults.size + sentenceResults.size})",
                            "Words (${wordsResults.size})",
                            "Kanji (${kanjiResults.size})",
                            "Sentences (${sentenceResults.size})"
                        )
                        filterLabels.forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedFilterIndex == index,
                                onClick = { selectedFilterIndex = index },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }
            }

            // Results List
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (query.trim().isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📕", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("Search Japanese vocabulary, Kanji, and example sentences", color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text("Powered by JMdict, KANJIDIC2, JmdictFurigana & Tatoeba", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
                        }
                    }
                } else if (combinedResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 56.sp)
                            Spacer(Modifier.height(12.dp))
                            Text("No dictionary results found for \"$query\"", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(combinedResults) { item ->
                            when (item) {
                                is DictSearchResult.Word -> DictWordCard(
                                    entry = item.entry,
                                    appLanguage = appLanguage,
                                    onClick = { activeWordDetail = item.entry }
                                )
                                is DictSearchResult.Kanji -> DictKanjiCard(
                                    entry = item.entry,
                                    onClick = { selectedKanjiDialog = item.entry }
                                )
                                is DictSearchResult.Sentence -> DictSentenceCard(
                                    sentence = item.sentence,
                                    appLanguage = appLanguage
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Word Detail Bottom Sheet Modal
    if (activeWordDetail != null) {
        WordDetailBottomSheet(
            entry = activeWordDetail!!,
            dictionaryRepository = dictionaryRepository,
            onDismiss = { activeWordDetail = null },
            appLanguage = appLanguage
        )
    }

    // Kanji Detail Modal Sheet
    selectedKanjiDialog?.let { kanji ->
        com.momin.japanesestudyappn5.ui.components.KanjiDetailBottomSheet(
            kanjiEntry = kanji,
            dictionaryRepository = dictionaryRepository,
            onDismiss = { selectedKanjiDialog = null },
            appLanguage = appLanguage
        )
    }
}

@Composable
private fun DictWordCard(
    entry: JMdictEntry,
    appLanguage: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val displayFurigana = if (entry.furigana.isNotBlank()) entry.furigana else if (entry.kanji.isNotBlank()) "${entry.kanji}[${entry.reading}]" else entry.reading
                FuriganaText(rawText = displayFurigana, mainFontSize = 22.sp, furiganaFontSize = 11.sp, mainColor = MaterialTheme.colorScheme.primary)

                Spacer(Modifier.height(6.dp))

                val meaningsList = entry.senses.flatMap { it.meanings }.take(3).joinToString(", ")
                val displayMeaning = if (appLanguage == "bn" && !entry.bangla.isNullOrBlank()) entry.bangla else meaningsList

                Text(displayMeaning, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            IconButton(
                onClick = { AudioPlayer.playTts(context, if (entry.kanji.isNotBlank()) entry.kanji else entry.reading) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("🔊", fontSize = 18.sp)
            }

            Spacer(Modifier.width(8.dp))

            Surface(
                color = Color(0xFF1565C0),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("JMdict", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun DictKanjiCard(
    entry: KanjiDicEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(entry.kanji, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBD1F2D), modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.meanings.joinToString(", "), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("On: ${entry.onyomi.joinToString(", ")}  Kun: ${entry.kunyomi.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Text("Strokes: ${entry.strokeCount} • Grade: ${entry.grade ?: "-"}", fontSize = 10.sp, color = Color.Gray)
            }

            Surface(
                color = Color(0xFFBD1F2D),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("KANJIDIC", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun DictSentenceCard(
    sentence: TatoebaSentence,
    appLanguage: String
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F0FF)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val furiganaStr = if (sentence.furigana.isNotBlank()) sentence.furigana else sentence.japanese
                FuriganaText(rawText = furiganaStr, mainFontSize = 18.sp, furiganaFontSize = 11.sp, modifier = Modifier.weight(1f))

                IconButton(onClick = { AudioPlayer.playTts(context, sentence.japanese) }, modifier = Modifier.size(32.dp)) {
                    Text("🔊", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(6.dp))

            val displayTrans = if (appLanguage == "bn" && !sentence.bangla.isNullOrBlank()) sentence.bangla else sentence.english
            Text(displayTrans, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(4.dp))
            Text("Source: ${sentence.attribution}", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
