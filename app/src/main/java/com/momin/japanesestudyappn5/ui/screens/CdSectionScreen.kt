package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdSectionScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Preference storage for speed
    val sharedPrefs = remember { context.getSharedPreferences("cd_player_prefs", Context.MODE_PRIVATE) }
    var playbackSpeed by remember { mutableFloatStateOf(sharedPrefs.getFloat("cd_playback_speed", 1.0f)) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Player state
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingCd by remember { mutableStateOf<Int?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    var downloadingTrack by remember { mutableStateOf<Int?>(null) }
    var trackDownloadProgress by remember { mutableStateOf(0f) }
    var trackDownloadError by remember { mutableStateOf<String?>(null) }

    // Helper to safely apply playback speed to MediaPlayer
    fun applySpeedToPlayer(mp: MediaPlayer?, speed: Float) {
        if (mp != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                val wasPlaying = mp.isPlaying
                val params = mp.playbackParams
                params.speed = speed
                mp.playbackParams = params
                if (!wasPlaying && mp.isPlaying) {
                    mp.pause()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSpeed(newSpeed: Float) {
        val roundedSpeed = (Math.round(newSpeed * 100) / 100f)
        playbackSpeed = roundedSpeed
        sharedPrefs.edit().putFloat("cd_playback_speed", roundedSpeed).apply()
        applySpeedToPlayer(mediaPlayer, roundedSpeed)
    }

    // Format speed string label
    fun getSpeedLabel(speed: Float): String {
        val formatted = String.format(Locale.US, "%.2f", speed).removeSuffix("0").removeSuffix(".0")
        return when {
            speed < 0.95f -> "🐢 ${formatted}x"
            speed > 1.05f -> "🐇 ${formatted}x"
            else -> "⚡ 1.0x"
        }
    }

    // Progress updates
    LaunchedEffect(playingCd, isPlaying) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying) {
                mediaPlayer?.let {
                    currentPosition = it.currentPosition
                    duration = it.duration
                }
                delay(200L)
            }
        }
    }

    // Release player on exit
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun playTrack(trackNum: Int) {
        val relPath = "cd_audio/cd_$trackNum.mp3"
        val file = com.momin.japanesestudyappn5.util.OnlineAssetsManager.getLocalFile(context, relPath)

        if (!file.exists() || file.length() == 0L) {
            downloadingTrack = trackNum
            trackDownloadProgress = 0f
            trackDownloadError = null
            
            coroutineScope.launch {
                val result = com.momin.japanesestudyappn5.util.OnlineAssetsManager.downloadAsset(context, relPath) { progress ->
                    trackDownloadProgress = progress
                }
                downloadingTrack = null
                if (result.isSuccess) {
                    playTrack(trackNum)
                } else {
                    trackDownloadError = result.exceptionOrNull()?.message ?: "Download failed"
                }
            }
            return
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                applySpeedToPlayer(this, playbackSpeed)
                start()
            }
            mediaPlayer = mp
            playingCd = trackNum
            isPlaying = true
            duration = mp.duration
            currentPosition = 0
            
            mp.setOnCompletionListener {
                isPlaying = false
                playingCd = null
                currentPosition = 0
                mp.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (file.exists()) {
                file.delete()
            }
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
        }
    }

    fun stopTrack() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        playingCd = null
        isPlaying = false
        currentPosition = 0
    }

    // Format milliseconds to MM:SS
    fun formatMillis(ms: Int): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.US, "%02d:%02d", mins, secs)
    }

    // Filter CD list
    val trackCount = 87
    val filteredTracks = remember(searchQuery) {
        (1..trackCount).filter { i ->
            searchQuery.isEmpty() || "CD $i".contains(searchQuery, ignoreCase = true) || i.toString().contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("💿 CD Audio Reference", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Minna no Nihongo listening tracks", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Quick Speed Button in Top Bar
                    Surface(
                        onClick = { showSpeedDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(getSpeedLabel(playbackSpeed), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
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
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search track (e.g. CD 12 or 12)...", fontSize = 13.sp, color = Color.LightGray.copy(alpha = 0.6f)) },
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

                if (filteredTracks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No audio tracks found matching search", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = if (playingCd != null) 140.dp else 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTracks) { trackNum ->
                            val isCurrent = playingCd == trackNum
                            val cardBg = if (isCurrent) Color(0xFF334155) else Color(0xFF1E293B).copy(alpha = 0.8f)
                            val borderCol = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isCurrent) {
                                            togglePlayPause()
                                        } else {
                                            playTrack(trackNum)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, borderCol) else null,
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play icon / Indicator
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xFF475569)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrent && isPlaying) {
                                            Text("⏸️", fontSize = 18.sp)
                                        } else {
                                            Text("▶️", fontSize = 18.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Track Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "CD $trackNum",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Listening track reference $trackNum",
                                            fontSize = 11.sp,
                                            color = Color.LightGray.copy(alpha = 0.8f)
                                        )
                                    }

                                    // Right icon
                                    Text("🎧", fontSize = 20.sp, modifier = Modifier.padding(end = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Player Overlay
            AnimatedVisibility(
                visible = playingCd != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                playingCd?.let { trackNum ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💿 CD $trackNum", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
                                
                                // Speed adjustment trigger in player controls bar
                                Surface(
                                    onClick = { showSpeedDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = getSpeedLabel(playbackSpeed),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                IconButton(onClick = { stopTrack() }) {
                                    Text("⏹️", fontSize = 18.sp)
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = formatMillis(currentPosition),
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.width(36.dp)
                                )
                                
                                Slider(
                                    value = currentPosition.toFloat(),
                                    onValueChange = { pos ->
                                        currentPosition = pos.toInt()
                                        mediaPlayer?.seekTo(pos.toInt())
                                    },
                                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Text(
                                    text = formatMillis(duration),
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Speed Selection Dialog
            if (showSpeedDialog) {
                AlertDialog(
                    onDismissRequest = { showSpeedDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ Sound Playback Speed", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Slow down sound speed for easier listening practice or adjust speed to your preference.",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            // Current Speed Indicator
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Selected Speed:", fontSize = 13.sp, color = Color.LightGray)
                                    Text(
                                        text = getSpeedLabel(playbackSpeed),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Text("Quick Presets:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                            // Speed Presets Grid (0.5x, 0.75x, 0.85x, 1.0x, 1.25x, 1.5x)
                            val presets = listOf(
                                0.50f to "0.50x (Very Slow 🐢)",
                                0.75f to "0.75x (Slow 🐢)",
                                0.85f to "0.85x (Slightly Slow)",
                                1.00f to "1.00x (Normal ⚡)",
                                1.25f to "1.25x (Fast 🐇)",
                                1.50f to "1.50x (Very Fast 🚀)"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                presets.chunked(2).forEach { rowPresets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowPresets.forEach { (speedVal, label) ->
                                            val isSelected = Math.abs(playbackSpeed - speedVal) < 0.02f
                                            Button(
                                                onClick = { updateSpeed(speedVal) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF334155),
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                            ) {
                                                Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }
                            }

                            // Dynamic Slider for custom speed tuning
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fine-tune Speed (0.5x - 1.5x)", fontSize = 12.sp, color = Color.Gray)
                                    Text("${String.format(Locale.US, "%.2f", playbackSpeed)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Slider(
                                    value = playbackSpeed,
                                    onValueChange = { updateSpeed(it) },
                                    valueRange = 0.5f..1.5f,
                                    steps = 19, // steps of 0.05
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showSpeedDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done")
                        }
                    }
                )
            }

            // Dialogs
            if (downloadingTrack != null) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Downloading Audio", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Downloading track CD $downloadingTrack from server to keep app size small...")
                            LinearProgressIndicator(
                                progress = { trackDownloadProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                            )
                            Text("Progress: ${(trackDownloadProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                        }
                    },
                    confirmButton = {}
                )
            }

            if (trackDownloadError != null) {
                AlertDialog(
                    onDismissRequest = { trackDownloadError = null },
                    title = { Text("Download Failed", fontWeight = FontWeight.Bold) },
                    text = {
                        Text("Failed to download audio track. Error details: $trackDownloadError\n\nPlease check your internet connection or server source configuration in settings.")
                    },
                    confirmButton = {
                        Button(onClick = { trackDownloadError = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

