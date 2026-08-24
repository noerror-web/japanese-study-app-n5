package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val x: Float,
    val speed: Float,
    val angle: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float,
    val phaseOffset: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF1E88E5),
    Color(0xFF43A047), Color(0xFFF9A825), Color(0xFF00ACC1),
    Color(0xFFFF7043), Color(0xFFEC407A)
)

@Composable
fun ConfettiOverlay(
    message: String,
    subMessage: String = "",
    onDismiss: () -> Unit
) {
    val particles = remember {
        (0..80).map {
            ConfettiParticle(
                x = Random.nextFloat(),
                speed = 0.15f + Random.nextFloat() * 0.35f,
                angle = Random.nextFloat() * 30f - 15f,
                rotationSpeed = Random.nextFloat() * 720f - 360f,
                color = CONFETTI_COLORS.random(),
                size = 6f + Random.nextFloat() * 10f,
                phaseOffset = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )

    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(400)
        showDialog = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Falling confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                val y = ((progress * p.speed + p.phaseOffset / 6.28f) % 1f) * size.height
                val x = p.x * size.width + sin(progress * 6.28f * 2 + p.phaseOffset) * 40f
                val rotation = progress * p.rotationSpeed
                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    drawRect(
                        color = p.color.copy(alpha = 0.85f),
                        topLeft = Offset(x - p.size / 2, y - p.size / 2),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.5f)
                    )
                }
            }
        }

        // Celebration dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                    Text(
                        text = message,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    if (subMessage.isNotBlank()) {
                        Text(
                            text = subMessage,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = onDismiss) {
                        Text("🎉 Awesome!")
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}
