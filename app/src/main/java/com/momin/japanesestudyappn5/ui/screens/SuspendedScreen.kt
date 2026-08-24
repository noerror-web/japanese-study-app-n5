package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SuspendedScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2C0A0A),
                        Color(0xFF140303)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚫", fontSize = 42.sp)
            }

            // Title
            Text(
                text = "アカウント停止\nAccount Suspended",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            // Warning Card
            Surface(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Your sync account has been suspended by the administrator due to policy violations, license key sharing, or abuse.",
                        color = Color(0xFFFFCDD2),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "আপনার সিঙ্ক অ্যাকাউন্টটি নিয়ম লঙ্ঘনের জন্য স্থগিত করা হয়েছে।",
                        color = Color(0xFFFFCDD2).copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Contact Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "If you believe this is an error, please contact support.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Support: support@japanesestudyappn5.com",
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            OutlinedButton(
                onClick = {
                    com.momin.japanesestudyappn5.util.FirebaseSyncManager.signOut(context)
                },
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("🔓 Use a Different License Key", fontWeight = FontWeight.Bold)
            }
        }
    }
}
