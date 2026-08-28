package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.data.model.GrammarLesson
import com.momin.japanesestudyappn5.data.model.KanjiItem
import com.momin.japanesestudyappn5.data.model.JMdictEntry
import com.momin.japanesestudyappn5.data.model.KanjiDicEntry
import com.momin.japanesestudyappn5.data.repository.DefaultDictionaryRepository
import com.momin.japanesestudyappn5.ui.components.WordDetailBottomSheet
import com.momin.japanesestudyappn5.ui.components.KanjiDetailBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed class SearchResult {
    data class Vocab(val item: VocabItem) : SearchResult()
    data class DictWord(val word: JMdictEntry) : SearchResult()
    data class Grammar(val lesson: GrammarLesson) : SearchResult()
    data class Kanji(val item: KanjiItem) : SearchResult()
    data class DictKanji(val kanji: KanjiDicEntry) : SearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var allGrammar by remember { mutableStateOf<List<GrammarLesson>>(emptyList()) }
    var allKanji by remember { mutableStateOf<List<KanjiItem>>(emptyList()) }
    var dictWordResults by remember { mutableStateOf<List<JMdictEntry>>(emptyList()) }
    var dictKanjiResults by remember { mutableStateOf<List<KanjiDicEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var selectedDictWord by remember { mutableStateOf<JMdictEntry?>(null) }
    var selectedDictKanji by remember { mutableStateOf<KanjiDicEntry?>(null) }

    val dictRepository = remember { DefaultDictionaryRepository(context, context.assets, repository) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(appLanguage) {
        withContext(Dispatchers.IO) {
            val vocab = repository.getVocabulary()
            val grammar = repository.getGrammarLessons(appLanguage)
            val kanji = repository.getKanjis()

            withContext(Dispatchers.Main) {
                allVocab = vocab
                allGrammar = grammar
                allKanji = kanji
                isLoading = false
                focusRequester.requestFocus()
            }
        }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length >= 2) {
            withContext(Dispatchers.IO) {
                val words = dictRepository.searchWords(q).take(25)
                val kanji = dictRepository.searchKanji(q).take(10)
                withContext(Dispatchers.Main) {
                    dictWordResults = words
                    dictKanjiResults = kanji
                }
            }
        } else {
            dictWordResults = emptyList()
            dictKanjiResults = emptyList()
        }
    }

    val results: List<SearchResult> = remember(query, allVocab, allGrammar, allKanji, dictWordResults, dictKanjiResults) {
        fun getJlptRank(level: String?): Int {
            val l = level?.uppercase()?.trim() ?: ""
            return when {
                l.contains("N5") -> 1
                l.contains("N4") -> 2
                l.contains("N3") -> 3
                l.contains("N2") -> 4
                l.contains("N1") -> 5
                else -> 6
            }
        }

        if (query.trim().length < 2) emptyList()
        else {
            val q = query.trim()

            val dWords = dictWordResults
                .sortedWith(compareBy<JMdictEntry> { getJlptRank(it.jlptLevel) }.thenByDescending { it.isCommon })
                .map { SearchResult.DictWord(it) }

            val vocabResults = allVocab
                .filter {
                    it.japanese.contains(q, ignoreCase = true) ||
                    it.furigana.contains(q, ignoreCase = true) ||
                    it.romaji.contains(q, ignoreCase = true) ||
                    it.english.contains(q, ignoreCase = true) ||
                    it.bangla.contains(q, ignoreCase = true)
                }
                .take(15)
                .map { SearchResult.Vocab(it) }

            val grammarResults = allGrammar
                .filter { it.title.contains(q, ignoreCase = true) }
                .take(5)
                .map { SearchResult.Grammar(it) }

            val dKanji = dictKanjiResults
                .sortedWith(compareBy<KanjiDicEntry> { getJlptRank(it.jlptLevel) })
                .map { SearchResult.DictKanji(it) }

            val kanjiResults = allKanji
                .filter {
                    it.kanji.contains(q) ||
                    it.meanings.contains(q, ignoreCase = true) ||
                    it.on.contains(q, ignoreCase = true) ||
                    it.kun.contains(q, ignoreCase = true)
                }
                .take(5)
                .map { SearchResult.Kanji(it) }

            val allList = mutableListOf<SearchResult>()
            allList.addAll(dWords)
            allList.addAll(vocabResults)
            allList.addAll(dKanji)
            allList.addAll(kanjiResults)
            allList.addAll(grammarResults)

            allList.sortedWith(compareBy { item ->
                when (item) {
                    is SearchResult.DictWord -> getJlptRank(item.word.jlptLevel)
                    is SearchResult.Vocab -> 1 // App core N5 vocabulary
                    is SearchResult.DictKanji -> getJlptRank(item.kanji.jlptLevel)
                    is SearchResult.Kanji -> 1 // App core N5 kanji
                    is SearchResult.Grammar -> 1 // N5 Grammar
                }
            })
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search 23,000+ words, kanji, grammar…") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            query.trim().length < 2 -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Type at least 2 characters to search full dictionary", color = MaterialTheme.colorScheme.outline)
                }
            }
            results.isEmpty() -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("😅", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No results for \"$query\"", color = MaterialTheme.colorScheme.outline)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text("${results.size} results for \"$query\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 4.dp))
                }
                items(results) { result ->
                    when (result) {
                        is SearchResult.DictWord -> DictWordCard(result.word) { selectedDictWord = result.word }
                        is SearchResult.Vocab -> VocabSearchCard(result.item, appLanguage)
                        is SearchResult.Grammar -> GrammarSearchCard(result.lesson)
                        is SearchResult.DictKanji -> DictKanjiCard(result.kanji) { selectedDictKanji = result.kanji }
                        is SearchResult.Kanji -> KanjiSearchCard(result.item) {
                            val kEntry = KanjiDicEntry(
                                kanji = result.item.kanji,
                                onyomi = result.item.on.split("、", ",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() },
                                kunyomi = result.item.kun.split("、", ",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() },
                                nanori = emptyList(),
                                meanings = listOf(result.item.meanings),
                                meaningsBn = emptyList(),
                                jlptLevel = "N5",
                                grade = result.item.grade,
                                strokeCount = result.item.strokes,
                                radical = result.item.kanji,
                                examples = emptyList()
                            )
                            selectedDictKanji = kEntry
                        }
                    }
                }
            }
        }
    }

    selectedDictWord?.let { word ->
        WordDetailBottomSheet(
            entry = word,
            dictionaryRepository = dictRepository,
            onDismiss = { selectedDictWord = null },
            appLanguage = appLanguage
        )
    }

    selectedDictKanji?.let { kanji ->
        KanjiDetailBottomSheet(
            kanjiEntry = kanji,
            dictionaryRepository = dictRepository,
            onDismiss = { selectedDictKanji = null },
            appLanguage = appLanguage
        )
    }
}

@Composable
private fun DictWordCard(word: JMdictEntry, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📖", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.kanji.ifBlank { word.reading }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (word.reading != word.kanji && word.reading.isNotEmpty()) {
                        Text(" (${word.reading})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
                val primaryMeaning = word.senses.firstOrNull()?.meanings?.joinToString(", ") ?: word.bangla ?: ""
                Text(primaryMeaning, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { com.momin.japanesestudyappn5.util.AudioPlayer.playTts(context, word.reading.ifBlank { word.kanji }) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("🔊", fontSize = 18.sp)
            }
            Spacer(Modifier.width(6.dp))
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(6.dp)) {
                Text("Dict ${word.jlptLevel ?: ""}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun DictKanjiCard(kanji: KanjiDicEntry, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(kanji.kanji, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp).width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(kanji.meanings.joinToString(", "), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("On: ${kanji.onyomi.joinToString("・")}  Kun: ${kanji.kunyomi.joinToString("・")}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
            Surface(color = Color(0xFFBD1F2D), shape = RoundedCornerShape(6.dp)) {
                Text("Kanji", fontSize = 10.sp, color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun VocabSearchCard(item: VocabItem, appLanguage: String = "en") {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📚", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.japanese, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (item.furigana != item.japanese) {
                        Text(" (${item.furigana})", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline)
                    }
                }
                val displayMeaning = if (appLanguage == "bn" && item.bangla.isNotEmpty()) item.bangla else item.english
                Text(displayMeaning, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(
                onClick = { com.momin.japanesestudyappn5.util.AudioPlayer.playTts(context, item.japanese) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("🔊", fontSize = 18.sp)
            }
            Spacer(Modifier.width(6.dp))
            val lessonLabel = item.lesson?.let { "Lesson $it" } ?: item.sectionLabel.ifEmpty { "Vocab" }
            Surface(color = Color(0xFF1A73E8), shape = RoundedCornerShape(6.dp)) {
                Text(lessonLabel, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun GrammarSearchCard(lesson: GrammarLesson) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F0FF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📖", fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Lesson ${lesson.lesson}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(lesson.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(color = Color(0xFF6A1B9A), shape = RoundedCornerShape(6.dp)) {
                Text("Grammar", fontSize = 10.sp, color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun KanjiSearchCard(item: KanjiItem, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.kanji, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp).width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.meanings, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("On: ${item.on}  Kun: ${item.kun}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline)
            }
            Surface(color = Color(0xFFBD1F2D), shape = RoundedCornerShape(6.dp)) {
                Text("Kanji", fontSize = 10.sp, color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
