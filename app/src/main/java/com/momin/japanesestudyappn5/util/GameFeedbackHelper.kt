package com.momin.japanesestudyappn5.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object GameFeedbackHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playFeedbackTone(isSuccess: Boolean) {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            if (isSuccess) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
            } else {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playVictoryTone() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 300)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerHaptic(context: Context, isSuccess: Boolean) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (isSuccess) {
                        VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
                    } else {
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 80, 100, 80),
                            intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                            -1
                        )
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    if (isSuccess) {
                        vibrator.vibrate(80)
                    } else {
                        vibrator.vibrate(longArrayOf(0, 80, 100, 80), -1)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun triggerVictoryHaptic(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 100, 80, 100, 80, 200),
                        intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                        -1
                    )
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 80, 100, 80, 200), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
