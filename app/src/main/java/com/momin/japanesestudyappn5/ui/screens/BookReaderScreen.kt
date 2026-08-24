package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.momin.japanesestudyappn5.util.AudioPlayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

private fun getPdfRenderer(context: Context, relativePath: String): PdfRenderer? {
    return try {
        val file = com.momin.japanesestudyappn5.util.OnlineAssetsManager.getLocalFile(context, relativePath)
        if (!file.exists() || file.length() == 0L) {
            return null
        }
        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(fileDescriptor)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookReaderScreen(
    pdfPath: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDownloaded by remember(pdfPath) { mutableStateOf(com.momin.japanesestudyappn5.util.OnlineAssetsManager.isDownloaded(context, pdfPath)) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showLogsDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val renderer = remember(pdfPath, isDownloaded) {
        if (isDownloaded) getPdfRenderer(context, pdfPath) else null
    }
    var nightMode by remember { mutableStateOf(false) }

    DisposableEffect(renderer) {
        onDispose {
            try {
                renderer?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Night mode toggle
                    IconButton(onClick = { nightMode = !nightMode }) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = if (nightMode)
                                MaterialTheme.colorScheme.secondary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = if (nightMode) "☀️" else "🌙",
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightMode) Color(0xFF1A1A2E)
                    else MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = if (nightMode) Color.White
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        if (!isDownloaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (nightMode) Color(0xFF0D0D1A) else MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (nightMode) Color(0xFF1E1E30) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "📄",
                            fontSize = 64.sp
                        )

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (nightMode) Color.White else MaterialTheme.colorScheme.onSurface
                        )

                        val sizeEst = when (pdfPath) {
                            "minna_no_nihongo_n5_2013.pdf" -> "56.1 MB"
                            "minna_no_nihongo_bangla_vocab.pdf" -> "1.4 MB"
                            "textbook_lesson_all.pdf" -> "9.9 MB"
                            "leall_bn_t.pdf" -> "15.5 MB"
                            "minna_no_nihongo_n5_bangla.pdf" -> "80.0 MB"
                            else -> "Unknown size"
                        }

                        Text(
                            text = "To keep the app size small, this textbook is hosted online. Download it once to read it offline anytime.\nEstimated size: $sizeEst",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (nightMode) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "Server Source:\n${com.momin.japanesestudyappn5.util.OnlineAssetsManager.getBaseUrl(context)}",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (isDownloading) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LinearProgressIndicator(
                                    progress = downloadProgress,
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "Downloading: ${(downloadProgress * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (nightMode) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            if (downloadError != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "⚠️ Download Failed",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = downloadError ?: "Unknown download error",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp
                                        )
                                        TextButton(
                                            onClick = { showLogsDialog = true }
                                        ) {
                                            Text("📋 View Diagnostic Logs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    isDownloading = true
                                    downloadError = null
                                    coroutineScope.launch {
                                        val result = com.momin.japanesestudyappn5.util.OnlineAssetsManager.downloadAsset(context, pdfPath) { progress ->
                                            downloadProgress = progress
                                        }
                                        isDownloading = false
                                        if (result.isSuccess) {
                                            isDownloaded = true
                                        } else {
                                            downloadError = result.exceptionOrNull()?.message ?: "Unknown download error"
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (downloadError != null) "Retry Download" else "Download Textbook", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (renderer == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Opening and rendering PDF...", color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = {
                        val file = com.momin.japanesestudyappn5.util.OnlineAssetsManager.getLocalFile(context, pdfPath)
                        if (file.exists()) {
                            file.delete()
                        }
                        isDownloaded = false
                    }) {
                        Text("Redownload (Delete Corrupted File)", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            val pageCount = renderer.pageCount
            val pagerState = rememberPagerState(pageCount = { pageCount })
            val coroutineScope = rememberCoroutineScope()

            var isInitialLoad by remember { mutableStateOf(true) }
            var isCurrentPageZoomed by remember { mutableStateOf(false) }

            LaunchedEffect(pagerState.currentPage) {
                isCurrentPageZoomed = false
                if (isInitialLoad) {
                    isInitialLoad = false
                } else {
                    AudioPlayer.playAssetAudio(context, "book-page-flip.wav")
                }
            }

            // Auto-hide page indicator
            var showPageIndicator by remember { mutableStateOf(true) }
            LaunchedEffect(pagerState.currentPage) {
                showPageIndicator = true
                delay(2000)
                showPageIndicator = false
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (nightMode) Color(0xFF0D0D1A) else Color.DarkGray)
            ) {
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentPageZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val isCurrentPage = pagerState.currentPage == pageIndex
                    val pageOffset = ((pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction)
                    val zIndex = if (pageOffset > 0f) 1f else 0f
                    PdfPage(
                        renderer = renderer,
                        pageIndex = pageIndex,
                        isCurrentPage = isCurrentPage,
                        onZoomChanged = { zoomed ->
                            if (isCurrentPage) {
                                isCurrentPageZoomed = zoomed
                            }
                        },
                        modifier = Modifier
                            .zIndex(zIndex)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0f, 0.5f)
                                cameraDistance = 16f * density
                                when {
                                    pageOffset > 0f -> {
                                        val rotation = -180f * pageOffset.coerceIn(0f, 1f)
                                        rotationY = rotation
                                        alpha = if (rotation < -90f) 0f else 1f - pageOffset.coerceIn(0f, 1f)
                                        translationX = pageOffset * size.width
                                    }
                                    pageOffset < 0f -> {
                                        translationX = pageOffset * size.width
                                        rotationY = 0f
                                        alpha = 1f
                                    }
                                    else -> {
                                        translationX = 0f
                                        rotationY = 0f
                                        alpha = 1f
                                    }
                                }
                            }
                    )
                }

                // Floating page indicator pill (aligned within Box)
                if (showPageIndicator) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / $pageCount",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                } // end Box

                // Controls footer
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page ${pagerState.currentPage + 1} of $pageCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )

                        Row {
                            Button(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launchPageScroll(pagerState, pagerState.currentPage - 1)
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Text("Previous")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (pagerState.currentPage < pageCount - 1) {
                                        coroutineScope.launchPageScroll(pagerState, pagerState.currentPage + 1)
                                    }
                                },
                                enabled = pagerState.currentPage < pageCount - 1
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogsDialog) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        val logs = com.momin.japanesestudyappn5.util.OnlineAssetsManager.lastDownloadLog
        AlertDialog(
            onDismissRequest = { showLogsDialog = false },
            title = {
                Text("📋 Download Logs & Diagnostics", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Target File: $pdfPath\nBase URL: ${com.momin.japanesestudyappn5.util.OnlineAssetsManager.getBaseUrl(context)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    text = logs,
                                    color = Color(0xFF00FF66),
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(logs))
                        android.widget.Toast.makeText(context, "Logs copied to clipboard! ✓", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy Logs")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Extension to avoid launching directly in UI
private fun kotlinx.coroutines.CoroutineScope.launchPageScroll(
    state: androidx.compose.foundation.pager.PagerState,
    page: Int
) {
    this.launch {
        try {
            state.animateScrollToPage(page)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

private fun Modifier.fillMedValue(): Modifier = this.fillMaxSize()

@Composable
fun PdfPage(
    renderer: PdfRenderer,
    pageIndex: Int,
    isCurrentPage: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember(pageIndex) { mutableStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
            onZoomChanged(false)
        }
    }

    val bitmapState = produceState<Bitmap?>(initialValue = null, keys = arrayOf(renderer, pageIndex)) {
        value = withContext(Dispatchers.IO) {
            synchronized(renderer) {
                try {
                    renderer.openPage(pageIndex).use { page ->
                        // Render at 2x resolution for crisp text
                        val width = page.width * 2
                        val height = page.height * 2
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        // Clear to white background before rendering
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(8.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(pageIndex) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                                onZoomChanged(false)
                            } else {
                                scale = 2.5f
                                onZoomChanged(true)
                                
                                val centerX = containerSize.width / 2f
                                val centerY = containerSize.height / 2f
                                val targetX = (centerX - tapOffset.x) * (2.5f - 1f)
                                val targetY = (centerY - tapOffset.y) * (2.5f - 1f)
                                
                                val maxX = (containerSize.width * (2.5f - 1f)) / 2f
                                val maxY = (containerSize.height * (2.5f - 1f)) / 2f
                                
                                offset = Offset(
                                    x = targetX.coerceIn(-maxX, maxX),
                                    y = targetY.coerceIn(-maxY, maxY)
                                )
                            }
                        }
                    )
                }
                .pointerInput(pageIndex) {
                    detectZoomPanGestures(
                        scaleState = { scale },
                        onGesture = { _, pan, zoom ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            if (newScale > 1f) {
                                onZoomChanged(true)
                                val newOffset = (offset * zoom) + pan
                                
                                val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                
                                offset = Offset(
                                    x = newOffset.x.coerceIn(-maxX, maxX),
                                    y = newOffset.y.coerceIn(-maxY, maxY)
                                )
                            } else {
                                offset = Offset.Zero
                                onZoomChanged(false)
                            }
                            scale = newScale
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page $pageIndex",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectZoomPanGestures(
    scaleState: () -> Float,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
) {
    awaitEachGesture {
        // Wait for the first finger down; don't require unconsumed so pager doesn't block us
        awaitFirstDown(requireUnconsumed = false)

        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var accumulatedZoom = 1f
        var accumulatedPan = Offset.Zero

        do {
            val event = awaitPointerEvent()
            val pointerCount = event.changes.count { it.pressed }
            val currentScale = scaleState()
            val isMultiTouch = pointerCount >= 2
            val isZoomed = currentScale > 1f

            // Only process when zoomed in or multi-touch (pinch)
            if (isMultiTouch || isZoomed) {
                val zoomChange = event.calculateZoom()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    accumulatedZoom *= zoomChange
                    accumulatedPan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - accumulatedZoom) * centroidSize
                    val panMotion = accumulatedPan.getDistance()

                    // Multi-touch immediately passes slop check
                    if (isMultiTouch || zoomMotion > touchSlop || panMotion > touchSlop) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange)
                    }
                    // Consume all active pointer changes to block pager from paging while zoomed
                    event.changes.forEach {
                        if (it.pressed) it.consume()
                    }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}
