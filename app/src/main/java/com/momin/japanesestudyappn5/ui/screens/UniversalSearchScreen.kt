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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.data.model.GrammarLesson
import com.momin.japanesestudyappn5.data.model.KanjiItem

private sealed class SearchResult {
    data class Vocab(val item: VocabItem) : SearchResult()
    data class Grammar(val lesson: GrammarLesson) : SearchResult()
    data class Kanji(val item: KanjiItem) : SearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalSearchScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en",
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var allGrammar by remember { mutableStateOf<List<GrammarLesson>>(emptyList()) }
    var allKanji by remember { mutableStateOf<List<KanjiItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(appLanguage) {
        allVocab = repository.getVocabulary()
        allGrammar = repository.getGrammarLessons(appLanguage)
        allKanji = repository.getKanjis()
        isLoading = false
        focusRequester.requestFocus()
    }

    val results: List<SearchResult> = remember(query, allVocab, allGrammar, allKanji) {
        if (query.length < 2) emptyList()
        else {
            val q = query.trim()
            val vocabResults = allVocab
                .filter {
                    it.japanese.contains(q, ignoreCase = true) ||
                    it.furigana.contains(q, ignoreCase = true) ||
                    it.romaji.contains(q, ignoreCase = true) ||
                    it.english.contains(q, ignoreCase = true) ||
                    it.bangla.contains(q, ignoreCase = true)
                }
                .take(20)
                .map { SearchResult.Vocab(it) }

            val grammarResults = allGrammar
                .filter { it.title.contains(q, ignoreCase = true) }
                .take(5)
                .map { SearchResult.Grammar(it) }

            val kanjiResults = allKanji
                .filter {
                    it.kanji.contains(q) ||
                    it.meanings.contains(q, ignoreCase = true) ||
                    it.on.contains(q, ignoreCase = true) ||
                    it.kun.contains(q, ignoreCase = true) ||
                    it.onExampleJapanese.contains(q, ignoreCase = true) ||
                    it.onExampleEnglish.contains(q, ignoreCase = true) ||
                    it.onExampleBangla.contains(q, ignoreCase = true) ||
                    it.kunExampleJapanese.contains(q, ignoreCase = true) ||
                    it.kunExampleEnglish.contains(q, ignoreCase = true) ||
                    it.kunExampleBangla.contains(q, ignoreCase = true)
                }
                .take(10)
                .map { SearchResult.Kanji(it) }

            vocabResults + grammarResults + kanjiResults
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search vocab, grammar, kanji…") },
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
            query.length < 2 -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Type at least 2 characters to search", color = MaterialTheme.colorScheme.outline)
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
                        is SearchResult.Vocab -> VocabSearchCard(result.item, appLanguage)
                        is SearchResult.Grammar -> GrammarSearchCard(result.lesson)
                        is SearchResult.Kanji -> KanjiSearchCard(result.item)
                    }
                }
            }
        }
    }
}

@Composable
private fun VocabSearchCard(item: VocabItem, appLanguage: String = "en") {
    val context = androidx.compose.ui.platform.LocalContext.current
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
private fun KanjiSearchCard(item: KanjiItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
        modifier = Modifier.fillMaxWidth()
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
