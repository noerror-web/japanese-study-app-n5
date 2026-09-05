package com.momin.japanesestudyappn5.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.PitchAccentHelper

@Composable
fun PitchAccentView(
    japanese: String,
    furigana: String = "",
    modifier: Modifier = Modifier,
    showBadge: Boolean = true,
    compact: Boolean = false
) {
    val pitchInfo = remember(japanese, furigana) {
        PitchAccentHelper.getPitchInfo(rawKana = furigana, word = japanese)
    }

    val morae = pitchInfo.morae
    if (morae.isEmpty()) return

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(japanese, furigana) {
        animationTriggered = true
    }

    val animProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "pitchAnim"
    )

    Card(
        shape = RoundedCornerShape(if (compact) 12.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showBadge) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📈 Pitch Accent",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 11.sp else 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = pitchInfo.typeName,
                            fontSize = if (compact) 9.sp else 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(if (compact) 6.dp else 10.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 44.dp else 56.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val sizeMorae = morae.size
                    val segmentWidth = width / (sizeMorae + 1)

                    val highY = 12f
                    val lowY = height - 18f

                    // Dotted baseline guidelines
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, highY),
                        end = Offset(width, highY),
                        strokeWidth = 1.5f
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(0f, lowY),
                        end = Offset(width, lowY),
                        strokeWidth = 1.5f
                    )

                    val points = List(sizeMorae) { idx ->
                        val x = segmentWidth * (idx + 1)
                        val targetY = if (pitchInfo.pitchLevels[idx] == 1f) highY else lowY
                        val animatedY = lowY + (targetY - lowY) * animProgress
                        Offset(x, animatedY)
                    }

                    // Contour curve line
                    if (points.size > 1) {
                        val path = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p1 = points[i]
                                val p2 = points[i + 1]
                                cubicTo(
                                    (p1.x + p2.x) / 2f, p1.y,
                                    (p1.x + p2.x) / 2f, p2.y,
                                    p2.x, p2.y
                                )
                            }
                        }

                        // Outer glow & main stroke
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                            style = Stroke(width = 8f, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = path,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                        )
                    }

                    // Draw mora dots & drop marker
                    points.forEachIndexed { idx, pt ->
                        val isHigh = pitchInfo.pitchLevels[idx] == 1f
                        val dotColor = if (isHigh) Color(0xFFFF2A85) else Color(0xFF7C4DFF)

                        drawCircle(
                            color = dotColor.copy(alpha = 0.35f),
                            radius = 7f,
                            center = pt
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4f,
                            center = pt
                        )

                        // Draw step-down drop marker if pitch drops right after this mora
                        if (idx == pitchInfo.dropMoraIndex) {
                            val dropX = pt.x + (segmentWidth * 0.4f)
                            drawLine(
                                color = Color(0xFFFF2A85),
                                start = Offset(dropX, highY - 4f),
                                end = Offset(dropX, lowY + 4f),
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Render mora hiragana underneath dots
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    morae.forEachIndexed { idx, mora ->
                        val isDropMora = idx == pitchInfo.dropMoraIndex
                        Text(
                            text = if (isDropMora) "${mora}ꜜ" else mora,
                            fontSize = if (compact) 10.sp else 12.sp,
                            fontWeight = if (pitchInfo.pitchLevels[idx] == 1f) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (pitchInfo.pitchLevels[idx] == 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
