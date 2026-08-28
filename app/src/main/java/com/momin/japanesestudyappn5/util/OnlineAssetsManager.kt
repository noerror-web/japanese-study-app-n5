package com.momin.japanesestudyappn5.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object OnlineAssetsManager {
    private const val PREFS_NAME = "japanese_study_prefs"
    private const val KEY_BASE_URL = "download_base_url"
    const val DEFAULT_BASE_URL = "https://raw.githubusercontent.com/noerror-web/japanese-n5-assets/master/"

    private val CLOUD_FALLBACK_URLS = listOf(
        "https://raw.githubusercontent.com/noerror-web/japanese-n5-assets/4e78913/",
        "https://raw.githubusercontent.com/noerror-web/japanese-n5-assets/master/",
        "https://raw.githubusercontent.com/noerror-web/japanese-n5-assets/main/"
    )

    var isBulkDownloading by mutableStateOf(false)
        private set
    var bulkDownloadProgress by mutableStateOf(0f)
        private set
    var bulkDownloadCurrentFile by mutableStateOf("")
        private set
    var bulkDownloadTotalFiles by mutableStateOf(92)
        private set
    var bulkDownloadCurrentIndex by mutableStateOf(0)
        private set
    private var shouldCancelBulkDownload = false

    var lastDownloadLog by mutableStateOf<String>("No download attempts logged yet.")
        private set

    fun logDiagnostic(msg: String) {
        val timestamp = try {
            java.time.LocalTime.now().toString().take(8)
        } catch (e: Exception) {
            ""
        }
        val entry = "[$timestamp] $msg\n"
        android.util.Log.d("OnlineAssetsManager", msg)
        if (lastDownloadLog == "No download attempts logged yet.") {
            lastDownloadLog = entry
        } else {
            lastDownloadLog = (lastDownloadLog + entry).takeLast(4000)
        }
    }

    fun clearLogs() {
        lastDownloadLog = "Logs cleared.\n"
    }

    fun cancelBulkDownload() {
        shouldCancelBulkDownload = true
        logDiagnostic("Bulk download cancelled by user.")
    }

    fun getCandidateBaseUrls(context: Context): List<String> {
        val configured = getBaseUrl(context)
        val candidates = mutableListOf<String>()

        if (configured.isNotBlank()) {
            candidates.add(configured)
        }

        for (fallback in CLOUD_FALLBACK_URLS) {
            if (!candidates.contains(fallback)) {
                candidates.add(fallback)
            }
        }

        return candidates
    }

    suspend fun testServerConnection(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val baseUrl = getBaseUrl(context)
        val testFile = "minna_no_nihongo_bangla_vocab.pdf"
        val testUrl = baseUrl + testFile
        val startTime = System.currentTimeMillis()

        try {
            logDiagnostic("⚡ Testing connection to: $baseUrl")
            val url = URL(testUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "HEAD"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) JapaneseStudyApp/1.0")

            val code = conn.responseCode
            val duration = System.currentTimeMillis() - startTime
            conn.disconnect()

            if (code in 200..399) {
                val msg = "✓ Connected to server in ${duration}ms! (HTTP $code)"
                logDiagnostic("✅ $msg")
                Pair(true, msg)
            } else {
                val msg = "⚠️ Server responded with HTTP status $code (${duration}ms)"
                logDiagnostic("⚠️ $msg")
                Pair(false, msg)
            }
        } catch (e: java.net.ConnectException) {
            val isEmul = baseUrl.contains("10.0.2.2")
            val msg = if (isEmul && !android.os.Build.FINGERPRINT.contains("generic")) {
                "❌ Connection failed: 10.0.2.2 cannot be reached from physical phones. Replace 10.0.2.2 with your computer's Wi-Fi IP (e.g. http://192.168.1.X:8000/)."
            } else {
                "❌ Could not reach server at '$baseUrl'. Server may be offline."
            }
            logDiagnostic(msg)
            Pair(false, msg)
        } catch (e: Exception) {
            val msg = "❌ Health check error: ${e.message ?: e.javaClass.simpleName}"
            logDiagnostic(msg)
            Pair(false, msg)
        }
    }

    const val DICTIONARY_ASSET_PATH = "full_dictionary_data.json"

    fun isDictionaryDownloaded(context: Context): Boolean {
        return isDownloaded(context, DICTIONARY_ASSET_PATH)
    }

    suspend fun downloadDictionary(
        context: Context,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        downloadAsset(context, DICTIONARY_ASSET_PATH, onProgress)
    }

    suspend fun downloadAllAssets(context: Context) = withContext(Dispatchers.IO) {
        if (isBulkDownloading) return@withContext
        isBulkDownloading = true
        shouldCancelBulkDownload = false
        bulkDownloadProgress = 0f
        bulkDownloadCurrentIndex = 0

        val pdfList = listOf(
            "minna_no_nihongo_n5_bangla.pdf",
            "minna_no_nihongo_n5_2013.pdf",
            "minna_no_nihongo_bangla_vocab.pdf",
            "textbook_lesson_all.pdf",
            "leall_bn_t.pdf"
        )
        val dictList = listOf(DICTIONARY_ASSET_PATH)
        val cdList = (1..87).map { "cd_audio/cd_$it.mp3" }
        val allFiles = pdfList + dictList + cdList
        bulkDownloadTotalFiles = allFiles.size

        for ((index, relPath) in allFiles.withIndex()) {
            if (shouldCancelBulkDownload) break
            
            bulkDownloadCurrentIndex = index + 1
            bulkDownloadCurrentFile = relPath.substringAfterLast("/")

            if (isDownloaded(context, relPath)) {
                bulkDownloadProgress = (index + 1).toFloat() / bulkDownloadTotalFiles.toFloat()
                continue
            }

            // Download individual asset
            downloadAsset(context, relPath) { _ -> }

            if (shouldCancelBulkDownload) break

            bulkDownloadProgress = (index + 1).toFloat() / bulkDownloadTotalFiles.toFloat()
        }

        isBulkDownloading = false
        shouldCancelBulkDownload = false
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var url = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        url = url.trim()
        if (url.isNotEmpty()) {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            if (!url.endsWith("/")) {
                url += "/"
            }
        }
        return url
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, url.trim()).apply()
    }

    /**
     * Get local file for a relative path inside internal storage downloads folder.
     */
    fun getLocalFile(context: Context, relativePath: String): File {
        val downloadsDir = File(context.filesDir, "downloads")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, relativePath)
        file.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        return file
    }

    fun isDownloaded(context: Context, relativePath: String): Boolean {
        val file = getLocalFile(context, relativePath)
        return file.exists() && file.length() > 0
    }

    /**
     * Downloads an asset using multi-server candidate fallback.
     */
    suspend fun downloadAsset(
        context: Context,
        relativePath: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val candidateUrls = getCandidateBaseUrls(context)
        var lastError: Exception? = null

        for ((index, baseUrl) in candidateUrls.withIndex()) {
            logDiagnostic("──────────────────────────────────────────")
            logDiagnostic("Attempt #${index + 1}/${candidateUrls.size} using Base URL: '$baseUrl'")

            val result = downloadFromUrl(context, relativePath, baseUrl, onProgress)
            if (result.isSuccess) {
                return@withContext result
            } else {
                lastError = result.exceptionOrNull() as? Exception ?: Exception("Download failed")
                if (index < candidateUrls.size - 1) {
                    logDiagnostic("⚠️ Base URL '$baseUrl' failed. Trying fallback server...")
                }
            }
        }

        logDiagnostic("❌ All ${candidateUrls.size} server candidates failed.")
        Result.failure(lastError ?: Exception("All download servers failed"))
    }

    /**
     * Downloads an asset from a single base URL.
     */
    private suspend fun downloadFromUrl(
        context: Context,
        relativePath: String,
        baseUrl: String,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val localFile = getLocalFile(context, relativePath)
        val downloadUrl = baseUrl + relativePath

        logDiagnostic("Downloading file: '$relativePath'")
        logDiagnostic("Target Full URL: '$downloadUrl'")

        if (baseUrl.contains("10.0.2.2") && !android.os.Build.FINGERPRINT.contains("generic")) {
            logDiagnostic("⚠️ NOTICE: Base URL is '10.0.2.2' (Android Emulator IP). If testing on a physical phone, 10.0.2.2 cannot connect. Set your computer's local Wi-Fi IP (e.g., http://192.168.1.X:8000/) in Settings!")
        }

        try {
            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 5

            while (redirectCount < maxRedirects) {
                logDiagnostic("Connecting to: $currentUrl (attempt #${redirectCount + 1})")
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) JapaneseStudyApp/1.0")

                conn.connect()

                val responseCode = conn.responseCode
                logDiagnostic("Server returned HTTP response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    logDiagnostic("HTTP $responseCode Redirect -> Location: '$location'")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                            location
                        } else {
                            URL(URL(currentUrl), location).toString()
                        }
                        redirectCount++
                        continue
                    } else {
                        val err = "HTTP $responseCode redirect without Location header"
                        logDiagnostic("❌ Error: $err")
                        return@withContext Result.failure(Exception(err))
                    }
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    conn.disconnect()
                    val msg = when (responseCode) {
                        404 -> "HTTP 404: File '$relativePath' not found on server ($currentUrl)"
                        403 -> "HTTP 403: Access forbidden on server ($currentUrl)"
                        500, 502, 503 -> "HTTP $responseCode: Server error. Check host server status."
                        else -> "Server returned HTTP status $responseCode"
                    }
                    logDiagnostic("❌ Error: $msg")
                    return@withContext Result.failure(Exception(msg))
                }

                connection = conn
                break
            }

            if (connection == null) {
                val err = "Too many HTTP redirects (max $maxRedirects)"
                logDiagnostic("❌ Error: $err")
                return@withContext Result.failure(Exception(err))
            }

            val fileLength = connection.contentLength
            logDiagnostic("Connected successfully. Total file size: ${if (fileLength > 0) "$fileLength bytes" else "Unknown size"}")
            val inputStream = connection.inputStream

            // Write to a temporary file first, to avoid corrupted partial files
            val tempFile = File(localFile.parentFile, localFile.name + ".tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }

            val outputStream = FileOutputStream(tempFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int
            while (inputStream.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    onProgress(total.toFloat() / fileLength.toFloat())
                }
                outputStream.write(data, 0, count)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            connection.disconnect()

            // Rename temp file to target local file
            if (localFile.exists()) {
                localFile.delete()
            }
            if (tempFile.renameTo(localFile)) {
                logDiagnostic("✅ SUCCESS: Saved file to internal storage ($total bytes)")
                Result.success(localFile)
            } else {
                val err = "Failed to rename temp file to local target"
                logDiagnostic("❌ Error: $err")
                Result.failure(Exception(err))
            }
        } catch (e: java.net.ConnectException) {
            val err = "Connection failed: Could not reach server at '$downloadUrl'."
            logDiagnostic("❌ ConnectException: $err")
            Result.failure(Exception(err))
        } catch (e: java.net.UnknownHostException) {
            val err = "Unknown host: Could not resolve server hostname '$baseUrl'."
            logDiagnostic("❌ UnknownHostException: $err")
            Result.failure(Exception(err))
        } catch (e: java.net.SocketTimeoutException) {
            val err = "Download timed out (15s)."
            logDiagnostic("❌ SocketTimeoutException: $err")
            Result.failure(Exception(err))
        } catch (e: Exception) {
            val err = "Exception: ${e.message ?: "Unknown error"}"
            logDiagnostic("❌ $err")
            e.printStackTrace()
            // Clean up temporary file
            val tempFile = File(localFile.parentFile, localFile.name + ".tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            Result.failure(e)
        }
    }

    fun isAllDownloaded(context: Context): Boolean {
        val pdfList = listOf(
            "minna_no_nihongo_n5_bangla.pdf",
            "minna_no_nihongo_n5_2013.pdf",
            "minna_no_nihongo_bangla_vocab.pdf",
            "textbook_lesson_all.pdf",
            "leall_bn_t.pdf"
        )
        val cdList = (1..87).map { "cd_audio/cd_$it.mp3" }
        return (pdfList + cdList).all { isDownloaded(context, it) }
    }
}
