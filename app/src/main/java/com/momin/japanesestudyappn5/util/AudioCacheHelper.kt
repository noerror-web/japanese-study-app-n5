package com.momin.japanesestudyappn5.util

import android.content.Context
import java.io.File

object AudioCacheHelper {

    private const val AUDIO_CACHE_DIR = "audio_tts_cache"

    fun getAudioCacheDirectory(context: Context): File {
        val dir = File(context.cacheDir, AUDIO_CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getAudioFile(context: Context, audioId: String): File {
        val safeId = audioId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return File(getAudioCacheDirectory(context), "tts_$safeId.mp3")
    }

    fun isAudioCached(context: Context, audioId: String): Boolean {
        val file = getAudioFile(context, audioId)
        return file.exists() && file.length() > 0
    }

    fun getCacheSizeMb(context: Context): Float {
        val dir = getAudioCacheDirectory(context)
        val bytes = dir.listFiles()?.sumOf { it.length() } ?: 0L
        return bytes / (1024f * 1024f)
    }

    fun clearAudioCache(context: Context): Boolean {
        val dir = getAudioCacheDirectory(context)
        var success = true
        dir.listFiles()?.forEach { file ->
            if (!file.delete()) success = false
        }
        return success
    }
}
