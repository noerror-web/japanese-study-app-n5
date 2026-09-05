package com.momin.japanesestudyappn5.util

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

object AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var wakeLock: PowerManager.WakeLock? = null

    var isJapaneseSupported by mutableStateOf(true)
        internal set
    var showTtsAlert by mutableStateOf(false)

    /**
     * Sanitizes Japanese text for TTS engines by extracting furigana from bracket notation
     * (e.g., "私[わたし]" -> "わたし") and cleaning non-verbal symbols.
     * Leaves clean Japanese text for Google TTS engine to pronounce natively with proper context and pitch accent.
     */
    fun cleanTextForJapaneseTts(raw: String): String {
        if (raw.isBlank()) return ""
        // 1. Extract furigana reading from bracket notation ONLY for Kanji/words immediately preceding brackets (e.g., "84円[えん]" -> "84えん", "私[わたし]" -> "わたし")
        val cleanBracket = raw.replace(Regex("([\\u4E00-\\u9FAF\\u3400-\\u4DBF]+)\\[([^\\]]+)\\]")) { matchResult ->
            matchResult.groupValues[2]
        }
        // 2. Remove isolated brackets, tildes (~/～), asterisks (*), or formatting symbols
        val cleanSymbols = cleanBracket
            .replace("[", "").replace("]", "")
            .replace("~", "").replace("～", "")
            .replace("*", "")
            .trim()

        // 3. Return cleaned string directly. Google TTS engine (com.google.android.tts) natively handles Kanji & Kana
        // with context and proper pitch accents without relying on simplistic static dictionary converters.
        return cleanSymbols
    }

    fun ensureTts(context: Context) {
        if (tts == null) {
            val listener = TextToSpeech.OnInitListener { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                Log.d("AudioPlayer", "TTS Init status: $status")
                if (ttsReady) {
                    val result = tts?.setLanguage(Locale.JAPANESE)
                    Log.d("AudioPlayer", "TTS Set Language Japanese result: $result")
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        isJapaneseSupported = false
                        ttsReady = false
                        showTtsAlert = true
                    } else {
                        isJapaneseSupported = true
                    }
                } else {
                    isJapaneseSupported = false
                    showTtsAlert = true
                }
            }
            tts = try {
                Log.d("AudioPlayer", "Attempting to initialize Google TTS engine...")
                TextToSpeech(context.applicationContext, listener, "com.google.android.tts")
            } catch (e: Exception) {
                Log.d("AudioPlayer", "Failed to init Google TTS, falling back to default engine: ${e.message}")
                TextToSpeech(context.applicationContext, listener)
            }
        }
    }

    fun playTts(context: Context, text: String) {
        if (tts == null) {
            val listener = TextToSpeech.OnInitListener { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                Log.d("AudioPlayer", "TTS Init status (playTts): $status")
                if (ttsReady) {
                    val result = tts?.setLanguage(Locale.JAPANESE)
                    Log.d("AudioPlayer", "TTS Set Language Japanese result (playTts): $result")
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        isJapaneseSupported = false
                        ttsReady = false
                        showTtsAlert = true
                    } else {
                        isJapaneseSupported = true
                        speakJapanese(text)
                    }
                } else {
                    isJapaneseSupported = false
                    showTtsAlert = true
                }
            }
            tts = try {
                Log.d("AudioPlayer", "Attempting to initialize Google TTS engine (playTts)...")
                TextToSpeech(context.applicationContext, listener, "com.google.android.tts")
            } catch (e: Exception) {
                Log.d("AudioPlayer", "Failed to init Google TTS (playTts), falling back to default engine: ${e.message}")
                TextToSpeech(context.applicationContext, listener)
            }
        } else {
            if (isJapaneseSupported) {
                speakJapanese(text)
            } else {
                showTtsAlert = true
            }
        }
    }

    fun openTtsPlayStore(context: Context) {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=com.google.android.tts")).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val intentWeb = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts")).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intentWeb)
        }
    }

    fun playAssetAudio(context: Context, assetPath: String) {
        ensureTts(context)
        try {
            val player = mediaPlayer ?: MediaPlayer().also { mediaPlayer = it }
            player.reset()
            val fd = context.assets.openFd(assetPath)
            player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            player.prepare()
            player.start()
        } catch (e: IOException) {
            // Audio file missing — fall back to TTS using the word from the file path
            speakJapanese(assetPath.substringAfterLast("/").removeSuffix(".mp3"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speakTextInLanguage(text: String, langCode: String = "ja", speed: Float = 1.0f) {
        if (ttsReady && tts != null) {
            val targetLocale = when (langCode) {
                "bn" -> Locale.forLanguageTag("bn-BD")
                "en" -> Locale.ENGLISH
                else -> Locale.JAPANESE
            }
            val res = tts?.setLanguage(targetLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (langCode == "bn") {
                    tts?.setLanguage(Locale.ENGLISH)
                } else {
                    tts?.setLanguage(Locale.JAPANESE)
                }
            }
            tts?.setSpeechRate(speed)
            val textToSpeak = if (langCode == "ja") cleanTextForJapaneseTts(text) else text
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance_$langCode")
        } else if (!isJapaneseSupported) {
            showTtsAlert = true
        }
    }

    fun speakJapanese(text: String, speed: Float = 1.0f) {
        speakTextInLanguage(text, "ja", speed)
    }

    suspend fun speakTextAndWait(
        context: Context,
        text: String,
        langCode: String = "ja",
        speed: Float = 1.0f
    ) {
        ensureTts(context)
        if (tts == null || !ttsReady) {
            return
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            val targetLocale = when (langCode) {
                "bn" -> Locale.forLanguageTag("bn-BD")
                "en" -> Locale.ENGLISH
                else -> Locale.JAPANESE
            }
            val setLangRes = tts?.setLanguage(targetLocale)
            if (setLangRes == TextToSpeech.LANG_MISSING_DATA || setLangRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (langCode == "bn") {
                    tts?.setLanguage(Locale.ENGLISH)
                } else {
                    tts?.setLanguage(Locale.JAPANESE)
                }
            }
            tts?.setSpeechRate(speed)

            val textToSpeak = if (langCode == "ja") cleanTextForJapaneseTts(text) else text
            val utteranceId = "utt_${System.currentTimeMillis()}_${(1000..9999).random()}"

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uttId: String?) {}
                override fun onDone(uttId: String?) {
                    if (uttId == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(uttId: String?) {
                    if (uttId == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                override fun onError(uttId: String?, errorCode: Int) {
                    if (uttId == utteranceId && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            })

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }

            val speakRes = tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            if (speakRes != TextToSpeech.SUCCESS && continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun acquireWakeLock(context: Context) {
        if (wakeLock == null) {
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JapaneseStudyApp:AudioPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }
        }
        if (wakeLock?.isHeld == false) {
            try {
                wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes timeout
                Log.d("AudioPlayer", "Partial WakeLock acquired for background audio playback")
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to acquire WakeLock", e)
            }
        }
    }

    fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            try {
                wakeLock?.release()
                Log.d("AudioPlayer", "Partial WakeLock released")
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Failed to release WakeLock", e)
            }
        }
    }

    fun shutdown() {
        releaseWakeLock()
        stop()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
    }
}
