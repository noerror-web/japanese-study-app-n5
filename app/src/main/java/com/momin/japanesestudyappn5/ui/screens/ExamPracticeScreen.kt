package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.ExamPart
import com.momin.japanesestudyappn5.data.model.ExamSet
import com.momin.japanesestudyappn5.data.model.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPracticeScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
    var examSets by remember { mutableStateOf<List<ExamSet>>(emptyList()) }
    var selectedExam by remember { mutableStateOf<ExamSet?>(null) }
    var currentPartIndex by remember { mutableIntStateOf(0) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var showResults by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        examSets = repository.getExamSets()
        isLoading = false
    }

    val activePart = selectedExam?.parts?.getOrNull(currentPartIndex)
    val activeQuestion = activePart?.questions?.getOrNull(currentQuestionIndex)

    // Calculate total questions in selected exam
    val totalQuestions = remember(selectedExam) {
        selectedExam?.parts?.sumOf { it.questions.size } ?: 0
    }

    // Get current global question number
    val globalQuestionNumber = remember(selectedExam, currentPartIndex, currentQuestionIndex) {
        if (selectedExam == null) 0
        else {
            var count = 0
            for (p in 0 until currentPartIndex) {
                count += selectedExam!!.parts[p].questions.size
            }
            count + currentQuestionIndex + 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedExam?.title ?: "Exam Practice",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (showResults) {
                                selectedExam = null
                                showResults = false
                            } else if (selectedExam != null) {
                                selectedExam = null
                            } else {
                                onBack()
                            }
                        }
                    ) {
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedExam == null) {
                // Exam Selection List
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "Select JLPT N5 Practice Set:",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(examSets) { set ->
                            Card(
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedExam = set
                                        currentPartIndex = 0
                                        currentQuestionIndex = 0
                                        score = 0
                                        answered = false
                                        selectedOptionIndex = null
                                        showResults = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = set.title,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = set.subtitle,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!set.sourceNote.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = set.sourceNote,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "${set.parts.size} Parts • ${set.parts.sumOf { it.questions.size }} Questions",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (showResults) {
                // Exam Completed Scorecard
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Exam Completed!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3DC487)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Practice Set: ${selectedExam?.title}",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(160.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Score", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$score",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text("out of $totalQuestions", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            selectedExam = null
                            showResults = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Back to Exams List", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (activeQuestion != null && activePart != null) {
                // Active Question UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Progress Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Part ${currentPartIndex + 1} of ${selectedExam?.parts?.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Question $globalQuestionNumber of $totalQuestions",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { globalQuestionNumber.toFloat() / totalQuestions.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Part Instruction Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = activePart.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = activePart.instruction,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Reading Context Passage (if present)
                        if (!activePart.context.isNullOrBlank()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Passage / Context:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val displayContext = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(activePart.context) else activePart.context
                                        Text(
                                            text = displayContext,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 24.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Question Stem Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val displayStem = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(activeQuestion.stem) else activeQuestion.stem
                                    Text(
                                        text = "Q${activeQuestion.number}. $displayStem",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Options
                        items(activeQuestion.choices.size) { index ->
                            val rawChoiceText = activeQuestion.choices[index]
                            val choiceText = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(rawChoiceText) else rawChoiceText
                            val isCorrect = index == activeQuestion.answer
                            val isSelected = selectedOptionIndex == index

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
                                        selectedOptionIndex = index
                                        if (isCorrect) score++
                                    }
                                },
                                colors = buttonColor,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}.  $choiceText",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (answered && isCorrect) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Correct")
                                    }
                                }
                            }
                        }

                        // Explanation Box
                        if (answered && !activeQuestion.explanation.isNullOrBlank()) {
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Explanation:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = activeQuestion.explanation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Next Question Navigation Button
                    if (answered) {
                        Button(
                            onClick = {
                                answered = false
                                selectedOptionIndex = null

                                // Advance question index
                                if (currentQuestionIndex < activePart.questions.size - 1) {
                                    currentQuestionIndex++
                                } else {
                                    // Move to next part
                                    if (currentPartIndex < selectedExam!!.parts.size - 1) {
                                        currentPartIndex++
                                        currentQuestionIndex = 0
                                    } else {
                                        // Finished Exam Set!
                                        showResults = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text(
                                text = if (currentPartIndex == selectedExam!!.parts.size - 1 &&
                                    currentQuestionIndex == activePart.questions.size - 1
                                ) "View Results" else "Next Question",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
