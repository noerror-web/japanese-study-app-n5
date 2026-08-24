package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.AIGenerator
import com.momin.japanesestudyappn5.util.AudioPlayer
import com.momin.japanesestudyappn5.util.TranslationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    onBack: () -> Unit,
    appLanguage: String = "en",
    onTraceClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val geminiApiKey = remember { prefs.getString("gemini_api_key", "") ?: "" }

    var inputText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TranslationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val quickExamples = remember {
        listOf(
            "Hello, good morning!",
            "আমি জাপানি ভাষা শিখছি",
            "Thank you very much",
            "おはようございます",
            "ありがとうございます"
        )
    }

    fun performTranslation() {
        val query = inputText.trim()
        if (query.isEmpty()) return

        if (geminiApiKey.isBlank()) {
            errorMessage = "Gemini API key is required for online AI translation. Please set your key in App Settings."
            return
        }

        isTranslating = true
        errorMessage = null
        result = null

        coroutineScope.launch {
            try {
                val res = AIGenerator.translateText(
                    apiKey = geminiApiKey,
                    text = query,
                    appLanguage = appLanguage
                )
                if (res != null) {
                    result = res
                } else {
                    errorMessage = "Translation failed. Please check your internet connection or API key."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown translation error"
            } finally {
                isTranslating = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🌐 AI Translator", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text(
                            text = if (appLanguage == "bn") "বাংলা, ইংরেজি ও জাপানি অনুবাদক" else "Instant English, Bangla & Japanese",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Enter Text to Translate:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (inputText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        inputText = ""
                                        result = null
                                        errorMessage = null
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear input", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    "Type English, Bangla, or Japanese text...\ne.g. 'Good morning', 'আমি বই পড়ি', 'こんにちは'",
                                    fontSize = 13.sp
                                )
                            },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Quick Example Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Try Quick Examples:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )
                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickExamples.size) { idx ->
                                    val example = quickExamples[idx]
                                    Surface(
                                        onClick = {
                                            inputText = example
                                            performTranslation()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = example,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Translate Action Button
                        Button(
                            onClick = { performTranslation() },
                            enabled = inputText.trim().isNotEmpty() && !isTranslating,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isTranslating) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Text("Translating with AI...", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Translate", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("🌐", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Error Display Card
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚠️ Translation Issue", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                            Text(errorMessage ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Result Display Card
            result?.let { res ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Source language badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Detected: ${res.detectedSource}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Audio TTS button
                                    val ttsText = if (res.detectedSource.contains("Japanese")) res.originalText else res.translatedText
                                    IconButton(
                                        onClick = { AudioPlayer.playTts(context, ttsText) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Text("🔊", fontSize = 18.sp)
                                    }

                                    // Copy button
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(res.translatedText))
                                            Toast.makeText(context, "Copied translation to clipboard! ✓", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Text("📋", fontSize = 18.sp)
                                    }
                                }
                            }

                            // Primary Translation Box
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                shadowElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Translation Output:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Furigana if applicable
                                    if (res.furigana.isNotEmpty() && res.furigana != res.translatedText) {
                                        Text(
                                            text = res.furigana,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = res.translatedText,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 32.sp
                                    )

                                    if (res.romaji.isNotEmpty()) {
                                        Text(
                                            text = "Romaji: ${res.romaji}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }

                            // Secondary Translations (English & Bangla)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (res.bangla.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🇧🇩 Bangla: ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                                        Text(res.bangla, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                if (res.english.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🇬🇧 English: ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(res.english, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // Grammar Notes
                            if (res.notes.isNotEmpty()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("💡 Vocabulary & Usage Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(res.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                                }
                            }

                            // Practice Trace Button if Japanese text available
                            val firstKana = res.translatedText.firstOrNull { it.toString().matches(Regex("[\\u3040-\\u309F\\u30A0-\\u30FF]")) }?.toString()
                            if (firstKana != null) {
                                OutlinedButton(
                                    onClick = { onTraceClick(firstKana) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("✏️ Practice Writing '$firstKana' in Kana Trace", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
