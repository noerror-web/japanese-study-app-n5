package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.content.Intent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.AIGenerator
import kotlinx.coroutines.launch
import java.util.Locale
import java.time.LocalDate

data class ChatMessage(val role: String, val text: String, val id: String = java.util.UUID.randomUUID().toString()) // role = "user" or "ai"

data class ChatScenario(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val initialMessage: String,
    val systemPrompt: String,
    val goals: List<String>,
    val goalMatches: List<Regex>
)

val freeChatScenario = ChatScenario(
    id = "free",
    title = "Free Chat",
    icon = "💬",
    description = "Practice Japanese in an open conversation.",
    initialMessage = "こんにちは！(Konnichiwa) 😊\nI'm your N5 Japanese tutor! Let's practice together.\nTry saying: \"私の名前は＿＿です。\" (My name is ___)",
    systemPrompt = "You are a friendly Japanese language tutor for JLPT N5 beginners. Guide the student, correct mistakes, and chat in Japanese.",
    goals = emptyList(),
    goalMatches = emptyList()
)

val scenarios = listOf(
    ChatScenario(
        id = "combini",
        title = "Convenience Store",
        icon = "🏪",
        description = "Order food or drinks at a Japanese convenience store.",
        initialMessage = "いらっしゃいませ！(Irasshaimase!) 😊\nWelcome to FamilyMart! What would you like to buy today?",
        systemPrompt = "You are a polite Japanese convenience store clerk. The student is a customer. Act in character, speak politely using desu/masu. Guide them to order food using 'ください' and ask for the price using 'いくら'.",
        goals = listOf("Order food (ください)", "Ask price (いくらですか)", "Refer to something (これ)"),
        goalMatches = listOf(
            Regex("ください|kudasai", RegexOption.IGNORE_CASE),
            Regex("いくら|ikura", RegexOption.IGNORE_CASE),
            Regex("これ|kore", RegexOption.IGNORE_CASE)
        )
    ),
    ChatScenario(
        id = "directions",
        title = "Asking Directions",
        icon = "🗺️",
        description = "Ask a passerby for directions to the train station.",
        initialMessage = "こんにちは！(Konnichiwa) 🌸\nCan I help you with something?",
        systemPrompt = "You are a helpful Japanese passerby on the street. The student is a lost traveler. Act in character, guide them to ask for the station using 'どこ' and 'すみません', and thank you using 'ありがとう'.",
        goals = listOf("Excuse yourself (すみません)", "Ask where (どこですか)", "Say thank you (ありがとう)"),
        goalMatches = listOf(
            Regex("すみません|sumimasen", RegexOption.IGNORE_CASE),
            Regex("どこ|doko", RegexOption.IGNORE_CASE),
            Regex("ありがとう|arigatou|arigato", RegexOption.IGNORE_CASE)
        )
    ),
    ChatScenario(
        id = "intro",
        title = "Self Introduction",
        icon = "🤝",
        description = "Introduce yourself to a new coworker or classmate.",
        initialMessage = "はじめまして！(Hajimemashite) 💼\nI'm Ken, a new team member here. Nice to meet you!",
        systemPrompt = "You are a friendly Japanese coworker or classmate. Guide the student to introduce their name, where they are from (using 'から'), and finish with 'よろしくお願いします'.",
        goals = listOf("Say name (です)", "Say where you are from (から)", "Nice to meet you (よろしく)"),
        goalMatches = listOf(
            Regex("です|desu", RegexOption.IGNORE_CASE),
            Regex("から|kara", RegexOption.IGNORE_CASE),
            Regex("よろしく|yoroshiku", RegexOption.IGNORE_CASE)
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val apiKey = remember { prefs.getString("gemini_api_key", "") ?: "" }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedScenario by remember { mutableStateOf<ChatScenario?>(freeChatScenario) }
    var completedGoals by remember { mutableStateOf(setOf<String>()) }
    var messages by remember { mutableStateOf(listOf(ChatMessage("ai", freeChatScenario.initialMessage))) }

    var inputText by remember { mutableStateOf("") }
    var showScenarioMenu by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }
    var autoSpeakEnabled by remember { mutableStateOf(prefs.getBoolean("ai_chat_auto_speak", false)) }
    var showCustomScenarioDialog by remember { mutableStateOf(false) }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                inputText = results[0]
            }
        }
    }

    fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Japanese / 日本語を話してください...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Speech recognition is not supported or failed to start.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // TTS setup
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var t: TextToSpeech? = null
        t = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                t?.language = Locale.JAPANESE
            }
        }
        tts = t
        onDispose { t.stop(); t.shutdown() }
    }

    LaunchedEffect(messages.size, isGenerating) {
        val target = messages.size + if (isGenerating) 1 else 0
        if (target > 0) {
            listState.animateScrollToItem(target - 1)
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || selectedScenario == null || isGenerating) return
        isGenerating = true
        inputText = ""
        messages = messages + ChatMessage("user", text)

        // Track and complete scenario goals (if not free chat)
        selectedScenario?.let { scenario ->
            if (scenario.goals.isNotEmpty()) {
                val newCompleted = completedGoals.toMutableSet()
                scenario.goals.forEachIndexed { idx, goal ->
                    val regex = scenario.goalMatches[idx]
                    if (regex.containsMatchIn(text)) {
                        newCompleted.add(goal)
                    }
                }
                if (newCompleted.size > completedGoals.size) {
                    completedGoals = newCompleted
                    val todayKey = "studied_today_${LocalDate.now()}"
                    prefs.edit().putBoolean(todayKey, true).apply()
                }
            }
        }

        if (apiKey.isBlank()) {
            val errorResponse = if (prefs.getString("app_language", "en") == "bn") {
                "জেমিনি এপিআই কি (Gemini API Key) সেট করা নেই। অনুগ্রহ করে সেটিংস-এ গিয়ে এপিআই কি সেট করুন, অথবা আপনার লাইসেন্স কি ভ্যালিডেট করে অটো-সিঙ্ক করুন।"
            } else {
                "Gemini API Key is not configured. Please enter your Gemini API Key in Settings, or validate your license key to sync it automatically."
            }
            messages = messages + ChatMessage("ai", errorResponse)
            isGenerating = false
            return
        }

        scope.launch {
            val kanjiDisabled = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
            val history = messages.dropLast(1).map { it.role to it.text }
            val reply = AIGenerator.generateChatResponse(
                apiKey, 
                history, 
                text, 
                selectedScenario?.systemPrompt ?: "",
                kanjiDisabled = kanjiDisabled
            )

            val response = reply ?: "Failed to connect to AI Tutor. Please check your internet connection or try again."
            messages = messages + ChatMessage("ai", response)
            isGenerating = false

            // Auto-speak response if enabled and generation succeeded
            if (autoSpeakEnabled && reply != null) {
                val cleanText = cleanTextForSpeech(response)
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(selectedScenario?.title ?: "AI Tutor Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(if (selectedScenario != null && selectedScenario != freeChatScenario) "Roleplay Practice" else "N5 Japanese Chat", fontSize = 11.sp, color = Color.White.copy(0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        autoSpeakEnabled = !autoSpeakEnabled
                        prefs.edit().putBoolean("ai_chat_auto_speak", autoSpeakEnabled).apply()
                    }) {
                        Text(if (autoSpeakEnabled) "🔊" else "🔈", fontSize = 20.sp)
                    }
                    // Scenario/Roleplay Selector Dropdown Icon
                    Box {
                        IconButton(onClick = { showScenarioMenu = true }) {
                            Text("🎭", fontSize = 20.sp)
                        }
                        DropdownMenu(
                            expanded = showScenarioMenu,
                            onDismissRequest = { showScenarioMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("💬 Free Chat") },
                                onClick = {
                                    selectedScenario = freeChatScenario
                                    messages = listOf(ChatMessage("ai", freeChatScenario.initialMessage))
                                    completedGoals = emptySet()
                                    showScenarioMenu = false
                                }
                            )
                            scenarios.forEach { scenario ->
                                DropdownMenuItem(
                                    text = { Text("${scenario.icon} ${scenario.title}") },
                                    onClick = {
                                        selectedScenario = scenario
                                        messages = listOf(ChatMessage("ai", scenario.initialMessage))
                                        completedGoals = emptySet()
                                        showScenarioMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("✨ Custom Scenario...") },
                                onClick = {
                                    showScenarioMenu = false
                                    showCustomScenarioDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating,
                        placeholder = { Text("Type in English or Japanese…") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        trailingIcon = {
                            if (inputText.isEmpty() && !isGenerating) {
                                IconButton(onClick = { startSpeechToText() }) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Speak now",
                                        tint = Color(0xFF43A047)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { sendMessage() },
                        enabled = inputText.isNotBlank() && !isGenerating,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isGenerating) Color(0xFF43A047) else Color.Gray)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val scenario = selectedScenario!!
            val allGoalsMet = completedGoals.size == scenario.goals.size

            // Only show the target checklist card if goals are defined (roleplay mode)
            if (scenario.goals.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (allGoalsMet) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (allGoalsMet) "🎉 All Scenario Goals Completed!" else "🎯 Active Chat Goals:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (allGoalsMet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            scenario.goals.forEach { goal ->
                                val met = completedGoals.contains(goal)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (met) Color(0xFF43A047) else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(goal.substringBefore(" "), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    count = messages.size,
                    key = { messages[it].id }
                ) { index ->
                    val msg = messages[index]
                    ChatBubble(msg = msg, onSpeak = { text ->
                        val cleanText = cleanTextForSpeech(text)
                        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
                    })
                }
                if (isGenerating) {
                    item(key = "typing_indicator") {
                        TypingIndicatorBubble()
                    }
                }
            }
        }
    }

    if (showCustomScenarioDialog) {
        var customTopic by remember { mutableStateOf("") }
        var isCreating by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isCreating) showCustomScenarioDialog = false },
            title = { Text("✨ Create Custom Roleplay") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter any Japanese conversation topic or scenario you want to practice. AI will generate a custom roleplay for you!", fontSize = 13.sp)
                    if (isCreating) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF43A047))
                        }
                    } else {
                        OutlinedTextField(
                            value = customTopic,
                            onValueChange = { customTopic = it },
                            placeholder = { Text("e.g. Ordering Ramen, Hotel check-in...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customTopic.isNotBlank()) {
                            isCreating = true
                            scope.launch {
                                val kanjiDisabled = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                val customScenario = AIGenerator.generateCustomScenario(apiKey, customTopic, kanjiDisabled = kanjiDisabled)
                                if (customScenario != null) {
                                    selectedScenario = customScenario
                                    messages = listOf(ChatMessage("ai", customScenario.initialMessage))
                                    completedGoals = emptySet()
                                    showCustomScenarioDialog = false
                                } else {
                                    android.widget.Toast.makeText(context, "Failed to generate scenario. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                isCreating = false
                            }
                        }
                    },
                    enabled = customTopic.isNotBlank() && !isCreating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomScenarioDialog = false },
                    enabled = !isCreating
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage, onSpeak: (String) -> Unit) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF43A047)).padding(2.dp),
                contentAlignment = Alignment.Center
            ) { Text("🤖", fontSize = 16.sp) }
            Spacer(Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            val context = LocalContext.current
            val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
            val displayText = remember(msg.text, isKanjiOff) {
                if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(msg.text) else msg.text
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) Color(0xFF1B5E20) else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = displayText,
                    modifier = Modifier.padding(12.dp),
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
            if (!isUser) {
                var isDeconstructing by remember { mutableStateOf(false) }
                var breakdownJson by remember { mutableStateOf<String?>(null) }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onSpeak(msg.text) }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                        Text("🔊 Listen", fontSize = 11.sp, color = Color(0xFF43A047))
                    }
                    TextButton(
                        onClick = {
                            isDeconstructing = !isDeconstructing
                        }, 
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(if (isDeconstructing) "🧩 Hide Structure" else "🧩 Deconstruct", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (isDeconstructing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🧩 Sentence Structure Analysis", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            val parts = remember(msg.text) {
                                msg.text.split(Regex("[。、？！\n\\s]")).filter { it.isNotBlank() }
                            }
                            parts.forEach { part ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp)).padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(part, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        when {
                                            part.endsWith("です") || part.endsWith("ます") -> "Polite Verb / Copula"
                                            part.endsWith("は") || part.endsWith("が") -> "Subject / Topic Marker"
                                            part.endsWith("を") -> "Object Marker"
                                            part.endsWith("に") || part.endsWith("へ") -> "Direction / Time Particle"
                                            part.endsWith("で") -> "Location / Method Particle"
                                            else -> "N5 Vocabulary Word"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF1B5E20)),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun TypingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF43A047)).padding(2.dp),
            contentAlignment = Alignment.Center
        ) { Text("🤖", fontSize = 16.sp) }
        Spacer(Modifier.width(6.dp))
        
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "dots")
                    
                    val dot1Scale by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.4f at 0
                                1f at 150
                                0.4f at 300
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot1"
                    )
                    val dot2Scale by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.4f at 100
                                1f at 250
                                0.4f at 400
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot2"
                    )
                    val dot3Scale by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 600
                                0.4f at 200
                                1f at 350
                                0.4f at 500
                            },
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "dot3"
                    )

                    Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot1Scale; scaleY = dot1Scale }.clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot2Scale; scaleY = dot2Scale }.clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Box(modifier = Modifier.size(6.dp).graphicsLayer { scaleX = dot3Scale; scaleY = dot3Scale }.clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

private val JP_SPEECH_REGEX = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\u3000-\\u303F\\uFF00-\\uFFEF]+")

private fun cleanTextForSpeech(text: String): String {
    val matches = JP_SPEECH_REGEX.findAll(text).map { it.value }.toList()
    return if (matches.isEmpty()) {
        text
    } else {
        matches.joinToString(" ")
            .replace("_", "")
            .replace("＿", "")
            .trim()
    }
}
