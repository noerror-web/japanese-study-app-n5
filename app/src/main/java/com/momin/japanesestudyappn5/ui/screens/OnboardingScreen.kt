package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val gradient: List<Color>
)

private val PAGES = listOf(
    OnboardingPage(
        emoji = "🎌",
        title = "ようこそ！\nWelcome!",
        description = "日本語 Study Hub is your all-in-one offline Japanese learning app.\n\nStudy Kana, Vocabulary, Kanji, Grammar, and practice with Flashcards and Mock Exams — all in Bengali and English.",
        gradient = listOf(Color(0xFF1E3264), Color(0xFF3D5193))
    ),
    OnboardingPage(
        emoji = "🃏",
        title = "Smart Flashcards\n& Kana Practice",
        description = "Use the Anki-style SRS flashcard system to review words efficiently.\n\nTap any Kana character to hear it and mark it as practiced. Long-press to see stroke order animation.",
        gradient = listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
    ),
    OnboardingPage(
        emoji = "🎯",
        title = "Set Your\nStudy Goal",
        description = "How much time can you dedicate each day?\n\nEven 5 minutes a day adds up fast — consistency beats intensity!",
        gradient = listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
    )
)

@Composable
fun OnboardingScreen(
    onComplete: (studyGoalMinutes: Int) -> Unit
) {
    var pageIndex by remember { mutableIntStateOf(0) }
    var selectedGoal by remember { mutableIntStateOf(10) }
    val page = PAGES[pageIndex]
    val isLast = pageIndex == PAGES.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(page.gradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(16.dp))

            // Page indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAGES.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == pageIndex) 10.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pageIndex) Color.White
                                else Color.White.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Page content with slide animation
            AnimatedContent(
                targetState = pageIndex,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "onboarding_page"
            ) { idx ->
                val p = PAGES[idx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(p.emoji, fontSize = 80.sp)
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = p.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = p.description,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    // Goal selector on last page
                    if (idx == 2) {
                        Spacer(Modifier.height(32.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(5, 10, 20).forEach { mins ->
                                val selected = selectedGoal == mins
                                Surface(
                                    onClick = { selectedGoal = mins },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(width = 72.dp, height = 72.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$mins",
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (selected) Color(0xFF1B5E20) else Color.White
                                        )
                                        Text(
                                            text = "min",
                                            fontSize = 11.sp,
                                            color = if (selected) Color(0xFF1B5E20) else Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (isLast) onComplete(selectedGoal)
                        else pageIndex++
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = page.gradient.first()
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isLast) "🎌  Let's Start!" else "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLast) {
                    TextButton(onClick = { onComplete(selectedGoal) }) {
                        Text("Skip", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}
