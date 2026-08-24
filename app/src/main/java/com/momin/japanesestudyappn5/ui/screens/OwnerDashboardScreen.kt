package com.momin.japanesestudyappn5.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

data class LicenseKeyModel(
    val key: String = "",
    val status: String = "active",
    val deviceId: String = "",
    val deviceIds: List<String> = emptyList(),
    val maxDevices: Long = 1L,
    val createdAt: Long = 0,
    val activatedAt: Long = 0,
    val userName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerDashboardScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var licensesList by remember { mutableStateOf<List<LicenseKeyModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var showGenerateDialog by remember { mutableStateOf(false) }
    var inputUserName by remember { mutableStateOf("") }
    var inputMaxDevices by remember { mutableStateOf("1") }
    var inputIncludeAiKey by remember { mutableStateOf(false) }
    var inputCustomAiKey by remember { mutableStateOf("") }

    // Fetch keys in real time
    DisposableEffect(Unit) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        var registration: com.google.firebase.firestore.ListenerRegistration? = null

        fun startListening() {
            registration = db.collection("licenses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        Toast.makeText(context, "Error: ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                LicenseKeyModel(
                                    key = doc.id,
                                    status = doc.getString("status") ?: "active",
                                    deviceId = doc.getString("deviceId") ?: "",
                                    deviceIds = (doc.get("deviceIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                    maxDevices = doc.getLong("maxDevices") ?: 1L,
                                    createdAt = doc.getLong("createdAt") ?: 0L,
                                    activatedAt = doc.getLong("activatedAt") ?: 0L,
                                    userName = doc.getString("userName") ?: "Unassigned"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        licensesList = list
                    }
                }
        }

        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                startListening()
                if (!task.isSuccessful) {
                    android.util.Log.w("OwnerDashboard", "Anonymous auth not enabled: ${task.exception?.localizedMessage}")
                }
            }
        } else {
            startListening()
        }

        onDispose {
            registration?.remove()
        }
    }

    fun generateNewKey(userName: String, maxDevices: Long, includeAiKey: Boolean, customAiKey: String) {
        val db = FirebaseFirestore.getInstance()
        val randomUuid = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val newKey = "N5-KEY-$randomUuid"

        val payload = mutableMapOf<String, Any>(
            "status" to "active",
            "deviceId" to "",
            "deviceIds" to emptyList<String>(),
            "maxDevices" to maxDevices,
            "createdAt" to System.currentTimeMillis(),
            "activatedAt" to 0L,
            "userName" to userName.trim(),
            "hasDefaultAiAccess" to includeAiKey
        )

        if (includeAiKey && customAiKey.trim().isNotEmpty()) {
            payload["gemini_api_key"] = customAiKey.trim()
        }

        db.collection("licenses").document(newKey)
            .set(payload)
            .addOnSuccessListener {
                Toast.makeText(context, "Key Generated for $userName: $newKey", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    fun toggleLicenseStatus(license: LicenseKeyModel) {
        val db = FirebaseFirestore.getInstance()
        val newStatus = if (license.status == "active") "suspended" else "active"
        db.collection("licenses").document(license.key)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(context, "License updated to $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    fun deleteLicense(licenseKey: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("licenses").document(licenseKey)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "License Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    fun resetLicenseDevices(licenseKey: String) {
        val db = FirebaseFirestore.getInstance()
        val updates = mapOf(
            "deviceId" to "",
            "deviceIds" to emptyList<String>(),
            "activatedAt" to 0L
        )
        db.collection("licenses").document(licenseKey)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Devices reset successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error resetting devices: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("License Key", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    val filteredLicenses = remember(licensesList, searchQuery) {
        licensesList.filter {
            searchQuery.isEmpty() ||
            it.key.contains(searchQuery, ignoreCase = true) ||
            it.deviceId.contains(searchQuery, ignoreCase = true) ||
            it.userName.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeCount = licensesList.count { it.status == "active" }
    val suspendedCount = licensesList.count { it.status == "suspended" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔑 Owner License Manager", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
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
                .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header summary stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Keys", color = Color.Gray, fontSize = 11.sp)
                            Text("${licensesList.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Active", color = Color(0xFF4CAF50), fontSize = 11.sp)
                            Text("$activeCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Suspended", color = Color(0xFFF44336), fontSize = 11.sp)
                            Text("$suspendedCount", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Generate button
                Button(
                    onClick = { showGenerateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Text("➕ Generate New License Key", fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search license or device UUID...", color = Color.LightGray.copy(alpha = 0.6f), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                    )
                )

                if (isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (filteredLicenses.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No license keys found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredLicenses) { license ->
                            LicenseKeyRow(
                                license = license,
                                onCopy = { copyToClipboard(license.key) },
                                onToggleStatus = { toggleLicenseStatus(license) },
                                onResetDevices = { resetLicenseDevices(license.key) },
                                onDelete = { deleteLicense(license.key) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = {
                showGenerateDialog = false
                inputUserName = ""
                inputMaxDevices = "1"
                inputIncludeAiKey = false
                inputCustomAiKey = ""
            },
            title = { Text("Generate New License Key", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the name of the user or description for this key:", fontSize = 14.sp)
                    OutlinedTextField(
                        value = inputUserName,
                        onValueChange = { inputUserName = it },
                        placeholder = { Text("e.g. John Doe / Tablet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Enter device usage limit (How many times it can be used):", fontSize = 14.sp)
                    OutlinedTextField(
                        value = inputMaxDevices,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                inputMaxDevices = it
                            }
                        },
                        placeholder = { Text("e.g. 1, 2, 5") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inputIncludeAiKey = !inputIncludeAiKey }
                    ) {
                        Checkbox(
                            checked = inputIncludeAiKey,
                            onCheckedChange = { inputIncludeAiKey = it }
                        )
                        Text(
                            text = "Grant AI Key Access to this user",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (inputIncludeAiKey) {
                        Text("Enter Gemini API Key to attach (or leave blank to use stored Cloud default):", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = inputCustomAiKey,
                            onValueChange = { inputCustomAiKey = it },
                            placeholder = { Text("AIzaSy...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userName = inputUserName.trim()
                        val maxDevStr = inputMaxDevices.trim()
                        val maxDev = maxDevStr.toLongOrNull() ?: 1L
                        if (userName.isNotEmpty()) {
                            generateNewKey(userName, maxDev, inputIncludeAiKey, inputCustomAiKey)
                            showGenerateDialog = false
                            inputUserName = ""
                            inputMaxDevices = "1"
                            inputIncludeAiKey = false
                            inputCustomAiKey = ""
                        } else {
                            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Generate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showGenerateDialog = false
                        inputUserName = ""
                        inputMaxDevices = "1"
                        inputIncludeAiKey = false
                        inputCustomAiKey = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LicenseKeyRow(
    license: LicenseKeyModel,
    onCopy: () -> Unit,
    onToggleStatus: () -> Unit,
    onResetDevices: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val createdDateStr = remember(license.createdAt) {
        if (license.createdAt > 0) dateFormatter.format(Date(license.createdAt)) else "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Key + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = license.key,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCopy() }
                )

                // Copy emoji button
                Text(
                    text = "📋",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { onCopy() }
                        .padding(horizontal = 8.dp)
                )

                Surface(
                    color = if (license.status == "active") Color(0xFF1B5E20) else Color(0xFFB71C1C),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = license.status.uppercase(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Name
            if (license.userName.isNotEmpty()) {
                Text(
                    text = "👤 User: ${license.userName}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Device bind status
            val devicesCount = if (license.deviceIds.isNotEmpty()) license.deviceIds.size else if (license.deviceId.isNotEmpty()) 1 else 0
            Text(
                text = "📱 Devices Bound: $devicesCount / ${license.maxDevices}",
                color = if (devicesCount == 0) Color.LightGray else Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            if (license.deviceIds.isNotEmpty()) {
                Text(
                    text = "Bound Device IDs: " + license.deviceIds.joinToString(", ") { it.take(8) + "..." },
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            } else if (license.deviceId.isNotEmpty()) {
                Text(
                    text = "Bound Device ID: " + license.deviceId.take(8) + "...",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            // Created date
            Text(
                text = "📅 Created: $createdDateStr",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onToggleStatus,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (license.status == "active") Color(0xFFEF5350) else Color(0xFF66BB6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (license.status == "active") "Suspend" else "Activate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onResetDevices,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE65100),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Reset Devices",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
