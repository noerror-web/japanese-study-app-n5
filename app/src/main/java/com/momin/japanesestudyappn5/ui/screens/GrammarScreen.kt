package com.momin.japanesestudyappn5.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.GrammarLesson
import com.momin.japanesestudyappn5.data.model.GrammarContentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String,
    modifier: Modifier = Modifier
) {
    var lessonsList by remember { mutableStateOf<List<GrammarLesson>>(emptyList()) }
    var selectedLesson by remember { mutableStateOf<GrammarLesson?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(appLanguage) {
        lessonsList = repository.getGrammarLessons(appLanguage)
        isLoading = false
    }

    // Intercept back button when in detail view to return to list
    BackHandler(enabled = selectedLesson != null) {
        selectedLesson = null
    }

    AnimatedContent(
        targetState = selectedLesson,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "GrammarScreenTransition"
    ) { activeLesson ->
        if (activeLesson == null) {
            // Lesson List View
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (appLanguage == "bn") "N5 Bengali Grammar Guide" else "N5 Grammar Guide", fontWeight = FontWeight.Bold) },
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
                    // Search Field
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search lessons, rules, or explanations...") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val filteredLessons = remember(lessonsList, searchQuery) {
                            lessonsList.filter { lesson ->
                                searchQuery.isBlank() ||
                                        lesson.lesson.toString() == searchQuery ||
                                        lesson.title.contains(searchQuery, ignoreCase = true) ||
                                        lesson.rules.any { it.contains(searchQuery, ignoreCase = true) } ||
                                        lesson.content.any { it.text.contains(searchQuery, ignoreCase = true) }
                            }
                        }

                        if (filteredLessons.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No lessons found matching your query.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredLessons, key = { it.lesson }) { lesson ->
                                    GrammarLessonCard(
                                        lesson = lesson,
                                        onClick = { selectedLesson = lesson }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Lesson Detail View
            GrammarLessonDetailScreen(
                lesson = activeLesson,
                appLanguage = appLanguage,
                onBackToList = { selectedLesson = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLessonCard(
    lesson: GrammarLesson,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Lesson badge with gradient
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Lesson ${lesson.lesson}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "${lesson.rules.size} Rules",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = lesson.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (lesson.rules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                lesson.rules.take(3).forEach { rule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(end = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = rule,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (lesson.rules.size > 3) {
                    Text(
                        text = "+ ${lesson.rules.size - 3} more grammar rules...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarLessonDetailScreen(
    lesson: GrammarLesson,
    appLanguage: String,
    onBackToList: () -> Unit
) {
    var isRulesExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lesson ${lesson.lesson}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToList) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (appLanguage == "bn") "ব্যাকরণ ও ব্যাখ্যা" else "Grammar & Explanation",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = lesson.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Expandable rules summary index
            if (lesson.rules.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        onClick = { isRulesExpanded = !isRulesExpanded }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = "Rules",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Rules Index (${lesson.rules.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isRulesExpanded) "Collapse" else "Expand",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (isRulesExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))
                                lesson.rules.forEachIndexed { index, rule ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(24.dp)
                                        )
                                        Text(
                                            text = rule,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lesson Details Content items
            items(lesson.content) { item ->
                GrammarContentItemView(item = item)
            }
        }
    }
}

@Composable
fun GrammarContentItemView(item: GrammarContentItem) {
    val context = LocalContext.current
    val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
    val displayText = if (isKanjiOff && com.momin.japanesestudyappn5.util.KanjiConverter.hasKanji(item.text)) {
        com.momin.japanesestudyappn5.util.KanjiConverter.toKana(item.text)
    } else item.text

    when (item.type.lowercase()) {
        "title" -> {
            // Inner section titles
            Text(
                text = displayText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                textAlign = TextAlign.Start
            )
        }
        "rule" -> {
            // Main grammar rules highlights
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Rule Icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = displayText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 22.sp
                    )
                }
            }
        }
        "example" -> {
            // Example labels/groups
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            ) {
                Text(
                    text = displayText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        "dialogue" -> {
            // Conversational Q&A dialogue dialogues
            val isQuestion = item.text.trim().startsWith("Q :", ignoreCase = true) || item.text.trim().startsWith("Q:", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = if (isQuestion) Arrangement.Start else Arrangement.End
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(vertical = 2.dp),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isQuestion) 4.dp else 16.dp,
                        bottomEnd = if (isQuestion) 16.dp else 4.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isQuestion) {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = displayText,
                            fontSize = 15.sp,
                            color = if (isQuestion) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 21.sp,
                            fontWeight = if (isQuestion) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
        "item" -> {
            // Numbered bullet items preceding example text
            Text(
                text = displayText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
        else -> {
            // Standard body paragraph explaining rules in Bengali
            Text(
                text = displayText,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
