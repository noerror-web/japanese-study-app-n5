package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.ExampleSentence
import androidx.compose.ui.text.style.TextAlign
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CompactChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    globalShowRomaji: Boolean = true,
    globalShowFurigana: Boolean = true,
    appLanguage: String = "en",
    onTraceClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    
    val viewModel: VocabularyViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        VocabularyViewModel(repository, prefs)
    }

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedLesson by viewModel.selectedLesson.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()

    val bookmarkedIds by viewModel.bookmarkedIds.collectAsStateWithLifecycle()
    val masteredIds by viewModel.masteredIds.collectAsStateWithLifecycle()
    val notesMap by viewModel.notesMap.collectAsStateWithLifecycle()

    val confettiMessage by viewModel.confettiMessage.collectAsStateWithLifecycle()
    val vocabToSentences by viewModel.vocabToSentences.collectAsStateWithLifecycle()

    val listeningMode by viewModel.listeningMode.collectAsStateWithLifecycle()
    val listeningIndex by viewModel.listeningIndex.collectAsStateWithLifecycle()

    val explanationSentence by viewModel.explanationSentence.collectAsStateWithLifecycle()
    val explanationText by viewModel.explanationText.collectAsStateWithLifecycle()
    val isExplaining by viewModel.isExplaining.collectAsStateWithLifecycle()

    var showFurigana by remember { mutableStateOf(globalShowFurigana) }
    var showRomaji by remember { mutableStateOf(globalShowRomaji) }
    var langMode by remember(appLanguage) { mutableStateOf(if (appLanguage == "bn") "bangla" else "english") }

    var noteEditItem by remember { mutableStateOf<VocabItem?>(null) }
    var noteEditText by remember { mutableStateOf("") }
    var shadowingVocabItem by remember { mutableStateOf<VocabItem?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val categoryList = viewModel.categoryList

    val filteredList by viewModel.filteredList.collectAsStateWithLifecycle()
    var expandedCardId by remember { mutableStateOf<String?>(null) }

    // Scroll state tracking for auto-hiding filter chips
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var showChips by remember { mutableStateOf(true) }
    var lastScrollOffset by remember { mutableStateOf(0) }
    var lastVisibleIndex by remember { mutableStateOf(0) }

    // Listening mode auto-play with zero overlap (speakTextAndWait) and 2-second thinking pause
    LaunchedEffect(listeningMode, listeningIndex, filteredList, appLanguage, langMode) {
        if (listeningMode && filteredList.isNotEmpty()) {
            if (listeningIndex < filteredList.size) {
                val item = filteredList[listeningIndex]

                // Step 1: Speak Japanese word and WAIT until speech completes 100%
                AudioPlayer.speakTextAndWait(context, item.japanese, "ja")

                // Step 2: Exact 2 seconds pause for user to think of the meaning
                delay(2000L)

                // Step 3: Determine meaning text & language based on app language settings
                val isBangla = appLanguage == "bn" || langMode == "bangla"
                val meaningText = if (isBangla && item.bangla.isNotBlank()) item.bangla else item.english
                val langCode = if (isBangla && item.bangla.isNotBlank()) "bn" else "en"

                // Step 4: Speak meaning in Bangla or English and WAIT until speech completes 100%
                AudioPlayer.speakTextAndWait(context, meaningText, langCode)

                // Step 5: Short rest pause before advancing to next word
                delay(800L)

                viewModel.listeningIndex.value++
            } else {
                viewModel.listeningMode.value = false
                viewModel.listeningIndex.value = 0
            }
        }
    }

    // Foreground service management for continuous background playback when screen turns off
    DisposableEffect(listeningMode) {
        if (listeningMode) {
            com.momin.japanesestudyappn5.util.VocabularyPlaybackService.start(context)
        } else {
            com.momin.japanesestudyappn5.util.VocabularyPlaybackService.stop(context)
        }
        onDispose {
            com.momin.japanesestudyappn5.util.VocabularyPlaybackService.stop(context)
        }
    }

    // Scroll-direction based visibility for filter chips
    LaunchedEffect(lazyListState) {
        snapshotFlow { Pair(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset) }
            .collect { (currentIndex, currentOffset) ->
                if (currentIndex == 0 && currentOffset < 100) {
                    showChips = true
                } else {
                    if (currentIndex != lastVisibleIndex) {
                        showChips = currentIndex < lastVisibleIndex
                    } else if (kotlin.math.abs(currentOffset - lastScrollOffset) > 10) {
                        showChips = currentOffset < lastScrollOffset
                    }
                }
                lastVisibleIndex = currentIndex
                lastScrollOffset = currentOffset
            }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vocabulary List", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.listeningMode.value = !listeningMode
                        viewModel.listeningIndex.value = 0
                    }) {
                        Text(
                            if (listeningMode) "⏹" else "🎧",
                            fontSize = 18.sp
                        )
                    }
                    // Furigana toggle
                    IconButton(onClick = { showFurigana = !showFurigana }) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (showFurigana) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                "ふり",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (showFurigana) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Language Selector
                    TextButton(
                        onClick = {
                            langMode = when (langMode) {
                                "both" -> "english"
                                "english" -> "bangla"
                                else -> "both"
                            }
                        }
                    ) {
                        Text(
                            text = langMode.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search Japanese, Romaji, English...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            // Filter and Category chips with auto-hide animation on scroll
            androidx.compose.animation.AnimatedVisibility(
                visible = showChips,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Column {
                    // Filter chips
                    val chips = listOf(
                        "all" to "All",
                        "bookmarked" to "⭐ Bookmarked",
                        "mastered" to "✅ Mastered",
                        "unknown" to "📝 Not Yet Known"
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(chips.size) { i ->
                            val (id, label) = chips[i]
                            CompactChip(
                                selected = filterMode == id,
                                onClick = { viewModel.filterMode.value = id },
                                label = label
                            )
                        }
                    }

                    // Category filter chips
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categoryList.size) { i ->
                            val cat = categoryList[i]
                            CompactChip(
                                selected = categoryFilter == cat,
                                onClick = { viewModel.categoryFilter.value = cat },
                                label = cat
                            )
                        }
                    }
                }
            }

            // Lesson filter row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedLesson,
                edgePadding = 12.dp,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedLesson == 0,
                    onClick = { viewModel.selectedLesson.value = 0 },
                    text = { Text("All Lessons", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.height(38.dp)
                )
                (1..25).forEach { lesson ->
                    Tab(
                        selected = selectedLesson == lesson,
                        onClick = { viewModel.selectedLesson.value = lesson },
                        text = { Text("L $lesson", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.height(38.dp)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No vocabulary found matching filters", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(filteredList, key = { it.audioId }) { item ->
                        val isBookmarked = bookmarkedIds.contains(item.audioId)
                        val isMastered = masteredIds.contains(item.audioId)
                        val hasNote = notesMap.containsKey(item.audioId)
                        val isCurrentlyPlaying = listeningMode && listeningIndex < filteredList.size &&
                            filteredList[listeningIndex].audioId == item.audioId
                        val itemSentences = vocabToSentences[item.audioId] ?: emptyList()
                        val hasSentences = itemSentences.isNotEmpty()
                        val isExpanded = expandedCardId == item.audioId
                        VocabRow(
                            item = item,
                            showFurigana = showFurigana,
                            showRomaji = showRomaji,
                            langMode = langMode,
                            isBookmarked = isBookmarked,
                            isMastered = isMastered,
                            hasNote = hasNote,
                            isPlaying = isCurrentlyPlaying,
                            hasSentences = hasSentences,
                            isExpanded = isExpanded,
                            sentences = itemSentences,
                            appLanguage = appLanguage,
                            onTraceClick = {
                                val char = item.furigana.firstOrNull()?.toString() ?: item.japanese.firstOrNull()?.toString() ?: ""
                                onTraceClick(char)
                            },
                            onShadowClick = {
                                shadowingVocabItem = item
                            },
                            onExplainClick = { sentence ->
                                viewModel.loadExplanation(sentence, appLanguage)
                            },
                            onExpandToggle = {
                                expandedCardId = if (isExpanded) null else item.audioId
                            },
                            onPlayAudio = {
                                AudioPlayer.playTts(context, item.japanese)
                            },
                            onBookmarkToggle = {
                                viewModel.toggleBookmark(item.audioId)
                            },
                            onMasterToggle = {
                                viewModel.toggleMastered(item.audioId)
                            },
                            onNoteClick = {
                                noteEditItem = item
                                noteEditText = notesMap[item.audioId] ?: ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Custom Note dialog
    noteEditItem?.let { item ->
        AlertDialog(
            onDismissRequest = { noteEditItem = null },
            title = { Text("📝 Note for ${item.japanese}", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteEditText,
                    onValueChange = { noteEditText = it },
                    placeholder = { Text("Add your personal note here...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveNote(item.audioId, noteEditText)
                    noteEditItem = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { noteEditItem = null }) { Text("Cancel") }
            }
        )
    }

    // Milestone confetti
    confettiMessage?.let { (msg, sub) ->
        ConfettiOverlay(
            message = msg,
            subMessage = sub,
            onDismiss = { viewModel.dismissConfetti() }
        )
    }

    // AI Explanation Dialog
    explanationSentence?.let { sentence ->
        AlertDialog(
            onDismissRequest = { 
                if (!isExplaining) {
                    viewModel.clearExplanation()
                }
            },
            title = { Text("💡 Sentence Breakdown", fontWeight = FontWeight.Bold) },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // Keep dialog bounded
                        .verticalScroll(scrollState)
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = sentence,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider()
                    if (isExplaining) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (appLanguage == "bn") "এআই বাক্য বিশ্লেষণ করছে..." else "AI is analyzing sentence structure...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        val exp = explanationText ?: ""
                        var parsedBreakdown: JSONObject? = null
                        try {
                            parsedBreakdown = JSONObject(exp)
                        } catch (e: Exception) {
                            // Fallback to normal text if parsing fails
                        }

                        if (parsedBreakdown != null) {
                            val errorMsg = parsedBreakdown.optString("error", "")
                            if (errorMsg.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (appLanguage == "bn") "ত্রুটি ঘটেছে (Error):" else "An error occurred:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = errorMsg,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    )
                                }
                            } else {
                                val translation = parsedBreakdown.optString("translation", 
                                    parsedBreakdown.optString("Translation", 
                                    parsedBreakdown.optString("translate", 
                                    parsedBreakdown.optString("english", 
                                    parsedBreakdown.optString("bengali", "")))))

                                val grammarNote = parsedBreakdown.optString("grammarNote", 
                                    parsedBreakdown.optString("grammar_note", 
                                    parsedBreakdown.optString("GrammarNote", 
                                    parsedBreakdown.optString("grammar", 
                                    parsedBreakdown.optString("note", "")))))

                                val breakdownArr = parsedBreakdown.optJSONArray("breakdown")
                                    ?: parsedBreakdown.optJSONArray("Breakdown")
                                    ?: parsedBreakdown.optJSONArray("words")
                                    ?: parsedBreakdown.optJSONArray("elements")
                                    ?: parsedBreakdown.optJSONArray("Word Breakdown")
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Translation Section
                                    if (translation.isNotEmpty()) {
                                        Text(
                                            text = if (appLanguage == "bn") "অনুবাদ:" else "Translation:",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = translation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                    
                                    // Word Breakdown Section
                                    Text(
                                        text = if (appLanguage == "bn") "শব্দ বিশ্লেষণ:" else "Word Breakdown:",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    if (breakdownArr != null && breakdownArr.length() > 0) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (i in 0 until breakdownArr.length()) {
                                                val item = breakdownArr.optJSONObject(i)
                                                if (item != null) {
                                                    val word = item.optString("word", 
                                                        item.optString("Word", 
                                                        item.optString("japanese", 
                                                        item.optString("kanji", ""))))

                                                    val reading = item.optString("reading", 
                                                        item.optString("Reading", 
                                                        item.optString("pronunciation", 
                                                        item.optString("kana", ""))))

                                                    val definition = item.optString("definition", 
                                                        item.optString("Definition", 
                                                        item.optString("meaning", 
                                                        item.optString("Meaning", 
                                                        item.optString("translation", "")))))

                                                    val role = item.optString("role", 
                                                        item.optString("Role", 
                                                        item.optString("grammatical_role", 
                                                        item.optString("explanation", 
                                                        item.optString("type", "")))))
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(
                                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = word,
                                                                    fontWeight = FontWeight.Bold,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                                if (reading.isNotEmpty() && reading != word) {
                                                                    Text(
                                                                        text = " ($reading)",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.outline
                                                                    )
                                                                }
                                                            }
                                                            if (role.isNotEmpty()) {
                                                                Text(
                                                                    text = role,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.secondary,
                                                                    modifier = Modifier.padding(top = 2.dp)
                                                                )
                                                            }
                                                        }
                                                        
                                                        if (definition.isNotEmpty()) {
                                                            Text(
                                                                text = definition,
                                                                fontWeight = FontWeight.SemiBold,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(start = 8.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    // Grammar Note Section
                                    if (grammarNote.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Text(
                                            text = if (appLanguage == "bn") "ব্যাকরণ নোট:" else "Grammar Note:",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = grammarNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = exp,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearExplanation()
                    },
                    enabled = !isExplaining
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
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

@Composable
fun VocabRow(
    item: VocabItem,
    showFurigana: Boolean,
    showRomaji: Boolean,
    langMode: String,
    isBookmarked: Boolean,
    isMastered: Boolean,
    hasNote: Boolean = false,
    isPlaying: Boolean = false,
    hasSentences: Boolean = false,
    isExpanded: Boolean = false,
    sentences: List<ExampleSentence> = emptyList(),
    appLanguage: String = "en",
    onTraceClick: () -> Unit = {},
    onShadowClick: () -> Unit = {},
    onExpandToggle: () -> Unit = {},
    onPlayAudio: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onMasterToggle: () -> Unit,
    onNoteClick: () -> Unit = {},
    onExplainClick: (String) -> Unit = {}
) {
    val borderColor = when {
        isPlaying -> androidx.compose.ui.graphics.Color(0xFF1A73E8)
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlaying) 6.dp else 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Row 1: Word Header (Audio play, Japanese Text) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Audio play button
                IconButton(
                    onClick = onPlayAudio,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play sound",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text info
                val context = LocalContext.current
                val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
                val displayJp = remember(item, isKanjiOff) {
                    if (isKanjiOff) item.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(item.japanese) }
                    else item.japanese
                }
                Column(modifier = Modifier.weight(1f)) {
                    // Japanese text with Furigana on top
                    if (!isKanjiOff && showFurigana && item.furigana.isNotBlank() && item.furigana != item.japanese) {
                        Text(
                            text = item.furigana,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    val vocabFontSize = when {
                        displayJp.length > 8 -> 16.sp
                        displayJp.length > 5 -> 18.sp
                        else -> 22.sp
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = displayJp,
                            fontSize = vocabFontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val lessonTag = item.lesson?.let { "Lesson $it" } ?: item.sectionLabel.ifEmpty { null }
                        if (lessonTag != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = lessonTag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (showRomaji && item.romaji.isNotBlank()) {
                        Text(
                            text = item.romaji,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Row 2: Translations (BN and EN) spanning the full width ---
            Column(modifier = Modifier.fillMaxWidth()) {
                if (langMode == "bangla" || langMode == "both") {
                    Text(
                        text = "BN: ${item.bangla}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E88E5)
                    )
                }
                if (langMode == "english" || langMode == "both") {
                    Text(
                        text = "EN: ${item.english}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Row 3: Practice Buttons ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onTraceClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("✏️ Practice Writing", fontSize = 11.sp)
                }
                FilledTonalButton(
                    onClick = onShadowClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("🎙️ Practice Speaking", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            // --- Row 4: bottom utility actions (Master, Bookmark, Notes, Examples toggle) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Notes Button
                TextButton(
                    onClick = onNoteClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = if (hasNote) "📝 Edit Note" else "🗒️ Add Note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Bookmark/Favorite icon
                    IconButton(onClick = onBookmarkToggle) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Star else Icons.Outlined.Star,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline
                        )
                    }

                    // Mark as Known / Mastered
                    IconButton(onClick = onMasterToggle) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Mark as Known",
                            tint = if (isMastered) Color(0xFF43A047) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }

                    // Sentences expand toggle
                    if (hasSentences) {
                        TextButton(
                            onClick = onExpandToggle,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Hide Examples ▲" else "Show Examples ▼",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Expandable sentence examples
        AnimatedVisibility(visible = isExpanded && sentences.isNotEmpty()) {
            Column {
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Column(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Example Sentences:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    sentences.take(2).forEach { sentence ->
                        val context = LocalContext.current
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sentence.japanese, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                val displayTranslation = if (appLanguage == "bn" && !sentence.bangla.isNullOrBlank()) {
                                    sentence.bangla
                                } else {
                                    sentence.english
                                }
                                Text(
                                    text = displayTranslation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        AudioPlayer.ensureTts(context)
                                        AudioPlayer.speakJapanese(sentence.japanese)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("🔊", fontSize = 16.sp)
                                }
                                IconButton(
                                    onClick = { onExplainClick(sentence.japanese) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("💡", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class CustomSentenceData(
    val japanese: String,
    val furigana: String,
    val english: String,
    val bangla: String
)

private fun generateExampleSentence(item: VocabItem, category: String?): ExampleSentence {
    val jp = item.japanese
    val en = item.english.trim()
    val fg = if (item.furigana.isNotBlank()) item.furigana else item.japanese
    val bnMeaning = item.bangla.trim()
    val customSentences = mapOf(
        "これ" to CustomSentenceData("これ は わたし の 辞書 です。", "こちらはわたしのじしょです。", "This is my dictionary.", "এটি আমার অভিধান।"),
        "それ" to CustomSentenceData("それ は あなた の 傘 です か。", "それはあなたのかさですか。", "Is that your umbrella?", "ওটি কি আপনার ছাতা?"),
        "あれ" to CustomSentenceData("あれ は 図書館 です。", "あれはとしょかんです。", "That over there is the library.", "ওটি একটি লাইব্রেরি।"),
        "この" to CustomSentenceData("この 本 は おもしろい です。", "このほんはおもしろいです。", "This book is interesting.", "এই বইটি আকর্ষণীয়।"),
        "この～" to CustomSentenceData("この 本 は おもしろい です。", "このほんはおもしろいです。", "This book is interesting.", "এই বইটি আকর্ষণীয়।"),
        "その" to CustomSentenceData("その 辞書 を 貸してください。", "そのじしょをかしてください。", "Please lend me that dictionary.", "অনুগ্রহ করে আমাকে ওই অভিধানটি ধার দিন।"),
        "その～" to CustomSentenceData("その 辞書 を 貸してください。", "そのじしょをかしてください。", "Please lend me that dictionary.", "অনুগ্রহ করে আমাকে ওই অভিধানটি ধার দিন।"),
        "あの" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
        "あの～" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
        "ここ" to CustomSentenceData("ここ は 教室 です。", "ここはきょうしつです。", "This place is the classroom.", "এটি ক্লাসরুম।"),
        "そこ" to CustomSentenceData("そこ は 食堂 です。", "そこはしょくどうです。", "That place is the cafeteria.", "ওটি ক্যাফেটেরিয়া।"),
        "あそこ" to CustomSentenceData("あそこ は 駅 です。", "あそこはえきです。", "That place over there is the station.", "ওটি স্টেশন।"),
        "わたし" to CustomSentenceData("わたし は 学生 です。", "わたしはがくせいです。", "I am a student.", "আমি একজন ছাত্র।"),
        "わたしたち" to CustomSentenceData("わたしたち は 友達 です。", "わたしたちはともだちです。", "We are friends.", "আমরা বন্ধু।"),
        "あなた" to CustomSentenceData("あなた は 先生 です か。", "あなたはせんせいですか。", "Are you a teacher?", "আপনি কি একজন শিক্ষক?"),
        "あのひと" to CustomSentenceData("あの 人 は だれ です か。", "あのひとはだれですか。", "Who is that person?", "ঐ ব্যক্তি কে?"),
        "あのかた" to CustomSentenceData("あの 方 は どなた です か。", "あのかたはどなたですか。", "Who is that person (polite)?", "ঐ ব্যক্তি কে (সম্মানসূচক)?"),
        "みなさん" to CustomSentenceData("みなさん、こんにちは。", "みなさん、こんにちは。", "Hello, everyone.", "সবাইকে হ্যালো।"),
        "せんせい" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
        "先生" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
        "がくせい" to CustomSentenceData("わたし は 東京 大学 の 学生 です。", "わたしはとうきょうだいがくのがくせいです。", "I am a student of Tokyo University.", "আমি টোকিও বিশ্ববিদ্যালয়ের একজন ছাত্র।"),
        "学生" to CustomSentenceData("わたし は 東京 大学 の 学生 です。", "わたしはとうきょうだいがくのがくせいです。", "I am a student of Tokyo University.", "আমি টোকিও বিশ্ববিদ্যালয়ের একজন ছাত্র।"),
        "ともだち" to CustomSentenceData("友達 と 一緒に 日本 に 行きます。", "ともだちといっしょににほんにいきます。", "I will go to Japan with my friend.", "আমি আমার বন্ধুর সাথে জাপানে যাব।"),
        "友達" to CustomSentenceData("友達 と 一緒に 日本 に 行きます。", "ともだちといっしょににほんにいきます。", "I will go to Japan with my friend.", "আমি আমার বন্ধুর সাথে জাপানে যাব।"),
        "にほん" to CustomSentenceData("日本 は きれい な 国 です。", "にほんはきれいなくにです。", "Japan is a beautiful country.", "জাপান একটি সুন্দর দেশ।"),
        "日本" to CustomSentenceData("日本 は きれい な 国 です。", "にほんはきれいなくにです。", "Japan is a beautiful country.", "জাপান একটি সুন্দর দেশ।"),
        "にほんご" to CustomSentenceData("日本語 は おもしろい です。", "にほんごはおもしろいです。", "Japanese is interesting.", "জাপানি ভাষা আকর্ষণীয়।"),
        "日本語" to CustomSentenceData("日本語 は おもしろい です。", "にほんごはおもしろいです。", "Japanese is interesting.", "জাপানি ভাষা আকর্ষণীয়।"),
        "えいご" to CustomSentenceData("わたし は 英語 を 話します。", "わたしはえいごをはなします。", "I speak English.", "আমি ইংরেজি বলি।"),
        "英語" to CustomSentenceData("わたし は 英語 を 話します。", "わたしはえいごをはなします。", "I speak English.", "আমি ইংরেজি বলি।"),
        "ほん" to CustomSentenceData("これ は わたし の 本 です。", "こちらはわたしのほんです。", "This is my book.", "এটি আমার বই।"),
        "本" to CustomSentenceData("これ は わたし の 本 です。", "こちらはわたしのほんです。", "This is my book.", "এটি আমার বই।"),
        "くるま" to CustomSentenceData("これ は だれ の 車 です か。", "こちらはだれのくるまですか。", "Whose car is this?", "এটি কার গাড়ি?"),
        "車" to CustomSentenceData("これ は だれ の 車 です か。", "こちらはだれのくるまですか。", "Whose car is this?", "এটি কার গাড়ি?"),
        "でんしゃ" to CustomSentenceData("電車 で 学校 に 行きます。", "でんしゃでがっこうにいきます。", "I go to school by train.", "আমি ট্রেনে করে স্কুলে যাই।"),
        "電車" to CustomSentenceData("電車 で 学校 に 行きます。", "でんしゃでがっこうにいきます。", "I go to school by train.", "আমি ট্রেনে করে স্কুলে যাই।"),
        "がっこう" to CustomSentenceData("学校 は どこ です か。", "がっこうはどこですか。", "Where is the school?", "স্কুলটি কোথায়?"),
        "学校" to CustomSentenceData("学校 は どこ です か。", "がっこうはどこですか。", "Where is the school?", "স্কুলটি কোথায়?"),
        "みず" to CustomSentenceData("水 を 飲みます。", "みずをのみます。", "I drink water.", "আমি পানি পান করি।"),
        "水" to CustomSentenceData("水 を 飲みます。", "みずをのみます。", "I drink water.", "আমি পানি পান করি।"),
        "おちゃ" to CustomSentenceData("お茶 を 飲みます か。", "おちゃをのみますか。", "Would you like to drink tea?", "আপনি কি চা পান করবেন?"),
        "お茶" to CustomSentenceData("お茶 を 飲みます か。", "おちゃをのみますか。", "Would you like to drink tea?", "আপনি কি চা পান করবেন?"),
        "コーヒー" to CustomSentenceData("コーヒー が 好き です。", "コーヒーがすきです。", "I like coffee.", "আমি কফি পছন্দ করি।"),
        "ごはん" to CustomSentenceData("朝ごはん を 食べました。", "あさごはんをたべました。", "I ate breakfast.", "আমি সকালের নাস্তা খেয়েছি।"),
        "さかな" to CustomSentenceData("魚 を 食べます。", "さかなをたべます。", "I eat fish.", "আমি মাছ খাই।"),
        "魚" to CustomSentenceData("魚 を 食べます。", "さかなをたべます。", "I eat fish.", "আমি মাছ খাই।"),
        "にく" to CustomSentenceData("肉 を 食べます。", "にくをたべます。", "I eat meat.", "আমি মাংস খাই।"),
        "肉" to CustomSentenceData("肉 を 食べます。", "にくをたべます。", "I eat meat.", "আমি মাংস খাই।"),
        "うち" to CustomSentenceData("うち に 帰ります。", "うちにかえります。", "I return home.", "আমি বাড়ি ফিরব।"),
        "へや" to CustomSentenceData("部屋 は きれい です。", "へやはきれいです。", "The room is clean.", "ঘরটি পরিষ্কার।"),
        "部屋" to CustomSentenceData("部屋 は きれい です。", "へやはきれいです。", "The room is clean.", "ঘরটি পরিষ্কার।"),
        "つくえ" to CustomSentenceData("机 の 上 に 本 が あります。", "つくえのうえにほんがあります。", "There is a book on the desk.", "টেবিলের উপর একটি বই আছে।"),
        "机" to CustomSentenceData("机 の 上 に 本 が あります。", "つくえのうえにほんがあります。", "There is a book on the desk.", "টেবিলের উপর একটি বই আছে।"),
        "いす" to CustomSentenceData("いす に 座ってください。", "いすにすわってください。", "Please sit on the chair.", "অনুগ্রহ করে চেয়ারে বসুন।"),
        "かばん" to CustomSentenceData("かばん の 中 に 何 が あります か。", "かばんの中になにがありますか。", "What is in the bag?", "ব্যাগের ভেতর কী আছে?"),
        "とけい" to CustomSentenceData("これ は 新しい 時計 です。", "これは新しい時計です。", "This is a new watch.", "এটি একটি নতুন ঘড়ি।"),
        "時計" to CustomSentenceData("これ は 新しい 時計 です。", "これは新しい時計です。", "This is a new watch.", "এটি একটি নতুন ঘড়ি।"),
        "かさ" to CustomSentenceData("かさ を 買いました。", "かさを買いました。", "I bought an umbrella.", "আমি একটি ছাতা কিনেছি।"),
        "えんぴつ" to CustomSentenceData("えんぴつ で 書きます。", "えんぴつで書きます。", "I write with a pencil.", "আমি পেন্সিল দিয়ে লিখি।"),
        "てがみ" to CustomSentenceData("手紙 を 書きます。", "手紙を書きます。", "I write a letter.", "আমি একটি চিঠি লিখি।"),
        "手紙" to CustomSentenceData("手紙 を 書きます。", "手紙を書きます。", "I write a letter.", "আমি একটি চিঠি লিখি।"),
        "しゃしん" to CustomSentenceData("写真 を 撮ります。", "写真を撮ります。", "I take a photo.", "আমি ছবি তুলি।"),
        "写真" to CustomSentenceData("写真 を 撮ります。", "写真を撮ります。", "I take a photo.", "আমি ছবি তুলি।"),
        "えいが" to CustomSentenceData("週末 に 映画 を 見ました。", "週末に映画を見ました。", "I watched a movie on the weekend.", "আমি উইকএন্ডে সিনেমা দেখেছি।"),
        "映画" to CustomSentenceData("週末 に 映画 を 見ました。", "週末に映画を見ました。", "I watched a movie on the weekend.", "আমি উইকএন্ডে সিনেমা দেখেছি।"),
        "おんがく" to CustomSentenceData("音楽 を 聞く の が 好き です。", "音楽を聞くのが好きです。", "I like listening to music.", "আমি গান শুনতে পছন্দ করি।"),
        "音楽" to CustomSentenceData("音楽 を 聞く の が 好き です。", "音楽を聞くのが好きです。", "I like listening to music.", "আমি গান শুনতে পছন্দ করি।"),
        "てんき" to CustomSentenceData("今日 は いい 天気 です ね。", "今日はいい天気ですね。", "Today the weather is nice, isn't it?", "আজ আবহাওয়া খুব ভালো, তাই না?"),
        "天気" to CustomSentenceData("今日 は いい 天気 です ね。", "今日はいい天気ですね。", "Today the weather is nice, isn't it?", "আজ আবহাওয়া খুব ভালো, তাই না?"),
        "あつい" to CustomSentenceData("今日 は とても 暑い です。", "今日はとても暑いです。", "Today is very hot.", "আজ খুব গরম।"),
        "暑い" to CustomSentenceData("今日 は とても 暑い です。", "今日はとても暑いです。", "Today is very hot.", "আজ খুব গরম।"),
        "さむい" to CustomSentenceData("昨日 は 寒かった です。", "昨日は寒かったです。", "Yesterday was cold.", "গতকাল শীত ছিল।"),
        "寒い" to CustomSentenceData("昨日 は 寒かった です。", "昨日は寒かったです。", "Yesterday was cold.", "গতকাল শীত ছিল।"),
        "おいしい" to CustomSentenceData("この ラーメン は おいしい です。", "このラーメンはおいしいです。", "This ramen is delicious.", "এই রামেনটি সুস্বাদু।")
    )

    val custom = customSentences[jp] ?: customSentences[fg]
    if (custom != null) {
        return ExampleSentence(
            japanese = custom.japanese,
            furigana = custom.furigana,
            english = custom.english,
            bangla = custom.bangla
        )
    }

    // 2. Verbs Detection
    val isVerb = en.startsWith("to ", ignoreCase = true)
    if (isVerb) {
        val verbBase = en.removePrefix("to ").removePrefix("To ").trim()
        val isMotionVerb = jp.endsWith("いきます") || jp.endsWith("きます") || jp.endsWith("かえります") ||
                en.contains("go ", ignoreCase = true) || en.contains("come ", ignoreCase = true) || en.contains("return ", ignoreCase = true)
        
        val isTransitiveVerb = jp.endsWith("たべます") || jp.endsWith("のみます") || jp.endsWith("かいます") || 
                jp.endsWith("よみます") || jp.endsWith("かきます") || jp.endsWith("とります") || jp.endsWith("ききます") ||
                en.contains("eat", ignoreCase = true) || en.contains("drink", ignoreCase = true) || en.contains("buy", ignoreCase = true) ||
                en.contains("read", ignoreCase = true) || en.contains("write", ignoreCase = true) || en.contains("take", ignoreCase = true)

        return when {
            isMotionVerb -> ExampleSentence(
                japanese = "わたし は 学校 へ $jp。",
                furigana = "わたしはがっこうへ${fg}。",
                english = "I $verbBase to school.",
                bangla = "আমি স্কুলে $bnMeaning।"
            )
            isTransitiveVerb -> ExampleSentence(
                japanese = "これ を $jp。",
                furigana = "これを${fg}。",
                english = "I $verbBase this.",
                bangla = "আমি এটি $bnMeaning।"
            )
            else -> ExampleSentence(
                japanese = "毎日 $jp。",
                furigana = "まいにち${fg}。",
                english = "I $verbBase every day.",
                bangla = "আমি প্রতিদিন $bnMeaning।"
            )
        }
    }

    // 3. Question words fallback
    if (en.endsWith("?") || jp in listOf("なに", "なん", "どこ", "いつ", "どう", "いくら", "いくつ", "だれ", "どなた")) {
        return ExampleSentence(
            japanese = "それ は $jp です か。",
            furigana = "それは${fg}ですか。",
            english = "Where/What/Who is that?",
            bangla = "ওটি $bnMeaning?"
        )
    }

    // 4. Suffixes
    val isSuffix = en.contains("suffix", ignoreCase = true) || en.contains("prefix", ignoreCase = true) || en.startsWith("-") || jp in listOf("さん", "ちゃん", "くん", "じん", "さい")
    if (isSuffix) {
        val suffixLabel = en.replace("suffix for", "").replace("suffix", "").replace("-", "").trim()
        val suffixEn = when (jp) {
            "さん" -> "Mr./Mrs. Tanaka"
            "じん" -> "Japanese person"
            "さい" -> "10 years old"
            else -> "Tanaka-$suffixLabel"
        }
        val suffixBn = when (jp) {
            "さん" -> "জনাব তানাকা"
            "じん" -> "জাপানি নাগরিক"
            "さい" -> "১০ বছর বয়স"
            else -> "তানাকা-$bnMeaning"
        }
        return ExampleSentence(
            japanese = "田中 $jp。",
            furigana = "たなか${fg}。",
            english = suffixEn,
            bangla = suffixBn
        )
    }

    // 5. Adjectives
    if (category == "Adjectives" || jp.endsWith("い") || en.contains("easy", ignoreCase=true) || en.contains("difficult", ignoreCase=true) || en.contains("heavy", ignoreCase=true) || en.contains("light", ignoreCase=true)) {
        return ExampleSentence(
            japanese = "これ は $jp です。",
            furigana = "こちらは${fg}です。",
            english = "This is $en.",
            bangla = "এটি $bnMeaning।"
        )
    }

    // 6. Time and Days
    val isTime = category == "Time" || en.contains("today", ignoreCase=true) || en.contains("tomorrow", ignoreCase=true) || en.contains("yesterday", ignoreCase=true) || en.contains("now", ignoreCase=true) || en.contains("o'clock", ignoreCase=true) || en.contains("minute", ignoreCase=true) || en.contains("hour", ignoreCase=true) || en.contains("day", ignoreCase=true) || en.contains("month", ignoreCase=true) || en.contains("year", ignoreCase=true)
    if (isTime) {
        return ExampleSentence(
            japanese = "いま は $jp です。",
            furigana = "いまは${fg}です。",
            english = "Now it is $en.",
            bangla = "এখন $bnMeaning।"
        )
    }

    // 7. Places
    val isPlace = category == "Places" || en.contains("station", ignoreCase=true) || en.contains("park", ignoreCase=true) || en.contains("room", ignoreCase=true) || en.contains("school", ignoreCase=true) || en.contains("university", ignoreCase=true) || en.contains("hospital", ignoreCase=true) || en.contains("country", ignoreCase=true) || en.contains("shop", ignoreCase=true) || en.contains("store", ignoreCase=true) || en.contains("house", ignoreCase=true) || en.contains("office", ignoreCase=true)
    if (isPlace) {
        val preposition = if (en.lowercase() in listOf("japan", "china", "vietnam", "london", "tokyo")) "" else "the "
        return ExampleSentence(
            japanese = "わたし は $jp へ 行きます。",
            furigana = "わたしは${fg}へいきます。",
            english = "I go to $preposition$en.",
            bangla = "আমি $bnMeaning-এ যাব।"
        )
    }

    // 8. Food and Drinks
    val isFood = category == "Food" || en.contains("rice", ignoreCase=true) || en.contains("water", ignoreCase=true) || en.contains("tea", ignoreCase=true) || en.contains("coffee", ignoreCase=true) || en.contains("meal", ignoreCase=true) || en.contains("drink", ignoreCase=true) || en.contains("food", ignoreCase=true) || en.contains("apple", ignoreCase=true) || en.contains("fish", ignoreCase=true) || en.contains("meat", ignoreCase=true)
    if (isFood) {
        val isDrink = en.contains("water", ignoreCase=true) || en.contains("tea", ignoreCase=true) || en.contains("coffee", ignoreCase=true) || en.contains("beer", ignoreCase=true) || en.contains("juice", ignoreCase=true) || en.contains("drink", ignoreCase=true) || en.contains("sake", ignoreCase=true) || en.contains("wine", ignoreCase=true) || en.contains("milk", ignoreCase=true)
        return if (isDrink) {
            ExampleSentence(
                japanese = "$jp を 飲みます。",
                furigana = "${fg}をのみます。",
                english = "I drink $en.",
                bangla = "আমি $bnMeaning পান করি।"
            )
        } else {
            ExampleSentence(
                japanese = "$jp を 食べます。",
                furigana = "${fg}をたべます。",
                english = "I eat $en.",
                bangla = "আমি $bnMeaning খাই।"
            )
        }
    }

    // 9. People/Jobs
    val isPeople = category == "People" || category == "School" || en.contains("person", ignoreCase=true) || en.contains("people", ignoreCase=true) || en.contains("student", ignoreCase=true) || en.contains("teacher", ignoreCase=true) || en.contains("doctor", ignoreCase=true) || en.contains("employee", ignoreCase=true) || en.contains("worker", ignoreCase=true) || en.contains("friend", ignoreCase=true) || en.contains("child", ignoreCase=true)
    if (isPeople) {
        return ExampleSentence(
            japanese = "あの 人 は $jp です。",
            furigana = "あのひとは${fg}です。",
            english = "That person is a $en.",
            bangla = "ঐ ব্যক্তি একজন $bnMeaning।"
        )
    }

    // 10. General Nouns Fallback
    val cleanEn = en.split(",").first().split("(").first().trim()
    val firstChar = cleanEn.firstOrNull()?.lowercaseChar()
    val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"

    val isUncountable = cleanEn.endsWith("s") && !cleanEn.endsWith("ss") && !cleanEn.endsWith("sh") && !cleanEn.endsWith("ch") ||
        cleanEn in listOf("water", "coffee", "tea", "milk", "beer", "rice", "money", "paper", "light", "electricity", "luggage", "baggage", "furniture", "information", "news")

    val finalEn = if (isUncountable) cleanEn else "$article $cleanEn"

    return ExampleSentence(
        japanese = "これ は $jp です。",
        furigana = "こちらは${fg}です。",
        english = "This is $finalEn.",
        bangla = "এটি $bnMeaning।"
    )
}

