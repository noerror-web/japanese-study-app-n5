package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.momin.japanesestudyappn5.data.model.KanjiItem
import com.momin.japanesestudyappn5.data.model.ParticleItem
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanjiParticlesScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    appLanguage: String = "en"
) {
    var activeTab by remember { mutableIntStateOf(initialTab) } // 0 = Kanji, 1 = Particles
    var kanjiList by remember { mutableStateOf<List<KanjiItem>>(emptyList()) }
    var particleList by remember { mutableStateOf<List<ParticleItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedKanji by remember { mutableStateOf<KanjiItem?>(null) }
    var selectedParticle by remember { mutableStateOf<ParticleItem?>(null) }
    var showParticleQuiz by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        kanjiList = repository.getKanjis()
        particleList = repository.getParticles()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kanji & Particles Guide", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (activeTab == 0) {
                            if (appLanguage == "bn") "কানজি, অর্থ বা উচ্চারণ খুঁজুন..." else "Search Kanji, meaning, reading..."
                        } else {
                            if (appLanguage == "bn") "পার্টিকেল বা অর্থ খুঁজুন..." else "Search particle name, meaning..."
                        }
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            // Tabs
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = {
                        activeTab = 0
                        searchQuery = ""
                    },
                    text = { Text("Kanji (${kanjiList.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = {
                        activeTab = 1
                        searchQuery = ""
                    },
                    text = { Text("Particles (${particleList.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (activeTab == 0) {
                    // Kanji Grid
                    val filteredKanji = remember(kanjiList, searchQuery) {
                        kanjiList.filter { item ->
                            searchQuery.isBlank() ||
                                    item.kanji.contains(searchQuery, ignoreCase = true) ||
                                    item.meanings.contains(searchQuery, ignoreCase = true) ||
                                    item.on.contains(searchQuery, ignoreCase = true) ||
                                    item.kun.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (filteredKanji.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No Kanji found matching query.", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredKanji) { item ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { selectedKanji = item }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = item.kanji,
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.meanings.split(",").firstOrNull() ?: "",
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Particles List
                    val filteredParticles = remember(particleList, searchQuery) {
                        particleList.filter { item ->
                            searchQuery.isBlank() ||
                                    item.particle.contains(searchQuery, ignoreCase = true) ||
                                    item.meaning.contains(searchQuery, ignoreCase = true) ||
                                    item.description.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = { showParticleQuiz = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Take Particles Quiz", fontWeight = FontWeight.Bold)
                        }

                        if (filteredParticles.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No particles found matching query.", color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredParticles) { item ->
                                    Card(
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedParticle = item }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Play audio button
                                            IconButton(
                                                onClick = {
                                                    val textToPlay = when (item.particle) {
                                                        "は" -> "わ"
                                                        "を" -> "お"
                                                        "へ" -> "え"
                                                        else -> item.particle
                                                    }
                                                    AudioPlayer.playTts(context, textToPlay)
                                                },
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Play Pronunciation",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.Bottom) {
                                                    Text(
                                                        text = item.particle,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "(${item.reading})",
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.secondary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.meaning.uppercase(),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = item.description,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }

    // Kanji Detail Modal Dialog
    selectedKanji?.let { item ->
        Dialog(onDismissRequest = { selectedKanji = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (appLanguage == "bn") "কানজি বিস্তারিত" else "Kanji Detail",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                AudioPlayer.playTts(context, item.kanji)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Pronunciation")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var isPlayingStrokes by remember { mutableStateOf(true) }
                    var manualStrokeIndex by remember { mutableIntStateOf(0) }

                    // Stroke order Canvas with interactive controls
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            KanjiStrokeAnimation(
                                svgPaths = item.svgPaths,
                                activeColor = MaterialTheme.colorScheme.primary,
                                baseColor = MaterialTheme.colorScheme.outlineVariant,
                                strokeWidth = 6f
                            )
                        }

                        if (item.svgPaths.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "✏️ ${item.strokes} Strokes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Character & Meanings
                    Text(
                        text = item.kanji,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.meanings,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- ON READING SECTION & EXAMPLE ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "ON (音読み)",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = item.on.ifBlank { "—" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val onJa = item.onExampleJapanese.ifBlank { item.exampleJapanese }
                            val onFuri = item.onExampleFurigana.ifBlank { item.exampleFurigana }
                            val onRoma = item.onExampleRomaji.ifBlank { item.exampleRomaji }
                            val onMeaning = if (appLanguage == "bn") {
                                item.onExampleBangla.ifBlank { item.exampleBangla }
                            } else {
                                item.onExampleEnglish.ifBlank { item.exampleEnglish }
                            }

                            if (onJa.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (onFuri.isNotBlank() && onFuri != onJa) "$onJa ($onFuri)" else onJa,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (onRoma.isNotBlank()) {
                                            Text(
                                                text = onRoma,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (onMeaning.isNotBlank()) {
                                            Text(
                                                text = onMeaning,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { AudioPlayer.playTts(context, onJa) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play ON example",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- KUN READING SECTION & EXAMPLE ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                        tonalElevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "KUN (訓読み)",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = item.kun.ifBlank { "—" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val kunJa = item.kunExampleJapanese.ifBlank { item.exampleJapanese }
                            val kunFuri = item.kunExampleFurigana.ifBlank { item.exampleFurigana }
                            val kunRoma = item.kunExampleRomaji.ifBlank { item.exampleRomaji }
                            val kunMeaning = if (appLanguage == "bn") {
                                item.kunExampleBangla.ifBlank { item.exampleBangla }
                            } else {
                                item.kunExampleEnglish.ifBlank { item.exampleEnglish }
                            }

                            if (kunJa.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (kunFuri.isNotBlank() && kunFuri != kunJa) "$kunJa ($kunFuri)" else kunJa,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (kunRoma.isNotBlank()) {
                                            Text(
                                                text = kunRoma,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (kunMeaning.isNotBlank()) {
                                            Text(
                                                text = kunMeaning,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { AudioPlayer.playTts(context, kunJa) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play KUN example",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { selectedKanji = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (appLanguage == "bn") "বন্ধ করুন" else "Close")
                    }
                }
            }
        }
    }

    // Particle Detail Modal Dialog
    selectedParticle?.let { item ->
        Dialog(onDismissRequest = { selectedParticle = null }) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Particle Detail: ${item.particle}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                val textToPlay = when (item.particle) {
                                    "は" -> "わ"
                                    "を" -> "お"
                                    "へ" -> "え"
                                    else -> item.particle
                                }
                                AudioPlayer.playTts(context, textToPlay)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play Pronunciation")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = item.particle,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.meaning.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (item.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: ${item.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Example Sentence
                    Text("Example Sentence:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.exampleJa,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = item.exampleRomaji,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (appLanguage == "bn") {
                        Text(
                            text = item.translationBn,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1E88E5),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = item.translation,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { selectedParticle = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    // Particle Quiz Dialog
    if (showParticleQuiz) {
        ParticlesQuizDialog(
            pool = particleList,
            onDismiss = { showParticleQuiz = false }
        )
    }
}

@Composable
fun KanjiStrokeAnimation(
    svgPaths: List<String>,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Float = 8f
) {
    val parsedPaths = remember(svgPaths) {
        svgPaths.mapNotNull { pathStr ->
            try {
                PathParser().parsePathString(pathStr).toPath()
            } catch (e: Exception) {
                null
            }
        }
    }

    if (parsedPaths.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stroke data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "kanjiTransition")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = parsedPaths.size.toFloat() + 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = parsedPaths.size * 900 + 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "kanjiProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / 109f
        val scaleY = size.height / 109f
        val scale = minOf(scaleX, scaleY) * 0.85f
        val offsetX = (size.width - 109f * scale) / 2f
        val offsetY = (size.height - 109f * scale) / 2f

        withTransform({
            translate(offsetX, offsetY)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // Outline
            parsedPaths.forEach { path ->
                drawPath(
                    path = path,
                    color = baseColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            // Animated strokes
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

@JvmName("KanjiMultiStrokeAnimation")
@Composable
fun KanjiStrokeAnimation(
    characterPathsList: List<List<String>>,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    baseColor: Color = MaterialTheme.colorScheme.outlineVariant,
    strokeWidth: Float = 8f
) {
    if (characterPathsList.size <= 1) {
        KanjiStrokeAnimation(
            svgPaths = characterPathsList.flatten(),
            modifier = modifier,
            activeColor = activeColor,
            baseColor = baseColor,
            strokeWidth = strokeWidth
        )
        return
    }

    val parsedCharacterPaths = remember(characterPathsList) {
        characterPathsList.map { charPaths ->
            charPaths.mapNotNull { pathStr ->
                try {
                    PathParser().parsePathString(pathStr).toPath()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    val totalStrokes = parsedCharacterPaths.sumOf { it.size }
    if (totalStrokes == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No stroke data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "kanjiComboTransition")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = totalStrokes.toFloat() + 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = totalStrokes * 900 + 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "kanjiComboProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val scaleX = size.width / 109f
        val scaleY = size.height / 109f
        val baseScale = minOf(scaleX, scaleY)

        val currentGlobalStroke = animProgress.toInt()
        val currentStrokeProgress = animProgress - currentGlobalStroke

        var globalIndex = 0

        parsedCharacterPaths.forEachIndexed { charIdx, paths ->
            val scale = if (charIdx == 0) baseScale * 0.60f else baseScale * 0.42f
            val offsetX = if (charIdx == 0) (size.width - 109f * baseScale) / 2f + 2f * baseScale
                          else (size.width - 109f * baseScale) / 2f + 54f * baseScale
            val offsetY = if (charIdx == 0) (size.height - 109f * baseScale) / 2f + 15f * baseScale
                          else (size.height - 109f * baseScale) / 2f + 32f * baseScale

            withTransform({
                translate(offsetX, offsetY)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = baseColor,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                paths.forEach { path ->
                    if (globalIndex < currentGlobalStroke) {
                        drawPath(
                            path = path,
                            color = activeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    } else if (globalIndex == currentGlobalStroke) {
                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
                        val partialPath = Path()
                        pathMeasure.getSegment(0f, pathMeasure.length * minOf(currentStrokeProgress, 1f), partialPath, true)
                        drawPath(
                            path = partialPath,
                            color = activeColor,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    globalIndex++
                }
            }
        }
    }
}

@Composable
fun ParticlesQuizDialog(
    pool: List<ParticleItem>,
    onDismiss: () -> Unit
) {
    if (pool.isEmpty()) return

    val totalQuestions = 10
    val quizItems = remember(pool) {
        pool.shuffled().take(minOf(totalQuestions, pool.size))
    }

    var questionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<ParticleItem?>(null) }
    var showResults by remember { mutableStateOf(false) }

    val currentItem = quizItems.getOrNull(questionIndex)
    // 3 modes: cloze, meaning, example
    val quizMode = remember(questionIndex) {
        val modes = listOf("cloze", "meaning", "example")
        modes[Random.nextInt(modes.size)]
    }

    val options = remember(currentItem, questionIndex) {
        if (currentItem == null) emptyList()
        else {
            val list = mutableListOf(currentItem)
            val others = pool.filter { it.audioId != currentItem.audioId }
                .shuffled()
                .distinctBy { it.particle }
                .take(3)
            list.addAll(others)
            list.shuffled()
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
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
                    Text("Your Score: $score / ${quizItems.size}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Finish")
                    }
                } else if (currentItem != null) {
                    Text(
                        text = "Question ${questionIndex + 1} of ${quizItems.size}",
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = when (quizMode) {
                                        "cloze" -> "Which particle completes this sentence?"
                                        "meaning" -> "Which particle usually does this job?"
                                        else -> "Which particle is practiced here?"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                val context = LocalContext.current
                                val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
                                val rawText = when (quizMode) {
                                    "cloze" -> currentItem.cloze
                                    "meaning" -> currentItem.meaning
                                    else -> currentItem.exampleJa
                                }
                                val displayText = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(rawText) else rawText
                                Text(
                                    text = displayText,
                                    fontSize = if (quizMode == "meaning") 24.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
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
                            val isCorrect = option.audioId == currentItem.audioId
                            val isSelected = selectedOption?.audioId == option.audioId

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
                                    text = option.particle + " - " + option.reading,
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
                                    if (questionIndex < quizItems.size - 1) {
                                        questionIndex++
                                        answered = false
                                        selectedOption = null
                                    } else {
                                        showResults = true
                                    }
                                }
                            ) {
                                Text(if (questionIndex < quizItems.size - 1) "Next" else "Results")
                            }
                        }
                    }
                }
            }
        }
    }
}
