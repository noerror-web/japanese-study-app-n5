package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.util.GameFeedbackHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MatchCard(
    val id: Int,
    val pairId: Int,
    val text: String,
    val isJapanese: Boolean,
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchingPairsScreen(
    onBack: () -> Unit,
    repository: DataRepository,
    appLanguage: String = "en"
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    var cards by remember { mutableStateOf<List<MatchCard>>(emptyList()) }
    var selectedCard by remember { mutableStateOf<MatchCard?>(null) }
    var matchedCount by remember { mutableIntStateOf(0) }
    var moves by remember { mutableIntStateOf(0) }
    var gameComplete by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var wrongFlashIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    fun buildCards() {
        val vocab = allVocab.shuffled().take(8)
        val newCards = mutableListOf<MatchCard>()
        var id = 0
        vocab.forEachIndexed { index, item ->
            newCards.add(MatchCard(id++, index, item.japanese, isJapanese = true))
            val definition = if (appLanguage == "bn") item.bangla else item.english
            newCards.add(MatchCard(id++, index, definition, isJapanese = false))
        }
        cards = newCards.shuffled()
        selectedCard = null
        matchedCount = 0
        moves = 0
        gameComplete = false
    }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary().shuffled()
        buildCards()
    }

    fun onCardClick(card: MatchCard) {
        if (isChecking || card.isFlipped || card.isMatched) return

        GameFeedbackHelper.triggerHaptic(context, isSuccess = true)
        cards = cards.map { if (it.id == card.id) it.copy(isFlipped = true) else it }

        val flippedCard = cards.first { it.id == card.id }

        if (selectedCard == null) {
            selectedCard = flippedCard
        } else {
            val prev = selectedCard!!
            moves++
            isChecking = true
            selectedCard = null

            scope.launch {
                delay(700)
                if (prev.pairId == flippedCard.pairId && prev.isJapanese != flippedCard.isJapanese) {
                    // Match!
                    cards = cards.map {
                        if (it.pairId == prev.pairId) it.copy(isMatched = true) else it
                    }
                    matchedCount++
                    if (matchedCount >= 8) {
                        gameComplete = true
                        showConfetti = true
                        GameFeedbackHelper.playVictoryTone()
                        GameFeedbackHelper.triggerVictoryHaptic(context)
                        saveQuizScore(prefs, "Matching Pairs", matchedCount, 8)
                    } else {
                        GameFeedbackHelper.playFeedbackTone(isSuccess = true)
                        GameFeedbackHelper.triggerHaptic(context, isSuccess = true)
                    }
                } else {
                    // No match
                    wrongFlashIds = setOf(prev.id, flippedCard.id)
                    GameFeedbackHelper.playFeedbackTone(isSuccess = false)
                    GameFeedbackHelper.triggerHaptic(context, isSuccess = false)
                    delay(400)
                    wrongFlashIds = emptySet()
                    cards = cards.map {
                        if (it.id == prev.id || it.id == flippedCard.id) it.copy(isFlipped = false) else it
                    }
                }
                isChecking = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🃏 Matching Pairs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6A1B9A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(Color(0xFF2D0055), Color(0xFF4A148C), Color(0xFF6A1B9A))))
        ) {
            if (gameComplete) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🎉", fontSize = 72.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("All Pairs Matched!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text("Completed in $moves moves", fontSize = 16.sp, color = Color(0xFFCE93D8))
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { buildCards() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC))
                    ) {
                        Text("🔄  Play Again", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("← Back", color = Color.White)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(0.15f)) {
                            Text(
                                "Matched: $matchedCount / 8",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(shape = RoundedCornerShape(10.dp), color = Color.White.copy(0.15f)) {
                            Text(
                                "Moves: $moves",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        if (appLanguage == "bn") "Match JP ↔ BN pairs" else "Match JP ↔ EN pairs",
                        color = Color.White.copy(0.7f), fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(cards, key = { it.id }) { card ->
                            FlipCard(
                                card = card,
                                isWrong = card.id in wrongFlashIds,
                                onClick = { onCardClick(card) }
                            )
                        }
                    }
                }
            }
            if (showConfetti) {
                ConfettiOverlay(
                    message = "All Pairs Matched! 🎉",
                    subMessage = "You finished in $moves moves!",
                    onDismiss = { showConfetti = false }
                )
            }
        }
    }
}

@Composable
private fun FlipCard(card: MatchCard, isWrong: Boolean, onClick: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
        animationSpec = tween(400),
        label = "flip"
    )
    val cardColor = when {
        isWrong -> Color(0xFFEF5350)
        card.isMatched -> Color(0xFF43A047)
        card.isFlipped -> Color(0xFFAB47BC)
        else -> Color(0xFF4A148C)
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(10.dp))
            .graphicsLayer { rotationY = rotation; cameraDistance = 8 * density }
            .background(if (rotation > 90f) cardColor else Color(0xFF7B1FA2))
            .border(1.5.dp, Color.White.copy(0.3f), RoundedCornerShape(10.dp))
            .clickable(enabled = !card.isMatched) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (rotation > 90f) {
            Text(
                text = card.text,
                fontSize = if (card.isJapanese) 16.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp).graphicsLayer { rotationY = 180f }
            )
        } else {
            Text("？", fontSize = 22.sp, color = Color.White.copy(0.6f))
        }
    }
}
