package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.FirebaseSyncManager

@Composable
fun LoginScreen(
    onComplete: () -> Unit,
    onAdminComplete: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    
    var enteredKey by remember { mutableStateOf("") }
    var storedKey by remember { mutableStateOf(prefs.getString("validated_license_key", "") ?: "") }
    var isAdminMode by remember { mutableStateOf(prefs.getBoolean("is_admin_mode", false)) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Logo & Title
            Text(
                text = "🎌",
                fontSize = 50.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Enter License Key",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "This app is locked until a valid license key is checked online. One key can now stay active on up to 2 devices, and your study progress syncs through the cloud profile whenever internet is available.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Main Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "LICENSE KEY",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = enteredKey,
                        onValueChange = { enteredKey = it; errorMessage = "" },
                        placeholder = { Text("Enter your license key", color = Color.Gray, fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        // Validate Key Button
                        Button(
                            onClick = {
                                val key = enteredKey.trim()
                                if (key.isEmpty()) {
                                    errorMessage = "Please enter a license key."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = ""
                                FirebaseSyncManager.validateLicenseKey(
                                    context = context,
                                    key = key,
                                    onSuccess = { isAdmin ->
                                        isLoading = false
                                        storedKey = prefs.getString("validated_license_key", "") ?: ""
                                        isAdminMode = isAdmin
                                        if (isAdmin) {
                                            onAdminComplete()
                                        } else {
                                            onComplete()
                                        }
                                    },
                                    onFailure = { err ->
                                        isLoading = false
                                        errorMessage = err
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Validate Key", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Continue To App Button
                    Button(
                        onClick = {
                            if (isAdminMode) {
                                onAdminComplete()
                            } else {
                                onComplete()
                            }
                        },
                        enabled = storedKey.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray.copy(alpha = 0.6f),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.2f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Continue To App", fontWeight = FontWeight.Bold)
                    }

                    // Remove Stored Key Button
                    Button(
                        onClick = {
                            FirebaseSyncManager.signOut(context)
                            storedKey = ""
                            isAdminMode = false
                            enteredKey = ""
                            errorMessage = ""
                        },
                        enabled = storedKey.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37474F),
                            disabledContainerColor = Color(0xFF37474F).copy(alpha = 0.2f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Remove Stored Key", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "Enter your license key to unlock the app.",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Stored License Section
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "STORED LICENSE",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (storedKey.isEmpty()) {
                        Text(
                            text = "No valid license has been stored on this device yet.\n\nIf the app goes offline later, it can still use the last successful validation until the next online check.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        Text(
                            text = if (isAdminMode) "Validated: OWNER ADMIN PORTAL" else "Validated: $storedKey",
                            color = Color(0xFF81C784),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Your device is fully unlocked. You can tap \"Continue To App\" to enter study hub.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Developer Contact Card for License Key
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💬 Need a License Key? Contact Developer",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/+8801811637906"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Telegram Button
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/noerror"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0088CC).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✈️ Telegram", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
