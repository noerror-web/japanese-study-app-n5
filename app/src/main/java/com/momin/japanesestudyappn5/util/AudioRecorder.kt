package com.momin.japanesestudyappn5.util

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.os.Build
import java.io.File

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val outputFile: File by lazy {
        File(context.cacheDir, "shadow_record.3gp")
    }

    fun startRecording(): Boolean {
        return try {
            stopRecording()
            if (outputFile.exists()) {
                outputFile.delete()
            }
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            false
        }
    }

    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
    }

    fun startPlayback(onComplete: () -> Unit) {
        try {
            stopPlayback()
            if (!outputFile.exists()) return
            mediaPlayer = MediaPlayer().apply {
                setDataSource(outputFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    onComplete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
        }
    }
}
