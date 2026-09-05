package com.momin.japanesestudyappn5.util

import android.content.Context
import android.content.SharedPreferences
import com.momin.japanesestudyappn5.data.model.VocabItem
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max

data class SrsData(
    val audioId: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextDueDateMillis: Long = 0L,
    val lastReviewedMillis: Long = 0L,
    val lapses: Int = 0
)

enum class SrsRating(val quality: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(3, "Hard"),
    GOOD(4, "Good"),
    EASY(5, "Easy")
}

object SrsEngine {

    private const val PREFS_KEY_PREFIX = "srs_card_"

    /**
     * Calculates the next SRS state based on SuperMemo SM-2 algorithm.
     */
    fun processReview(current: SrsData, rating: SrsRating, nowMillis: Long = System.currentTimeMillis()): SrsData {
        val q = rating.quality
        val oldEF = current.easeFactor

        // Calculate new Ease Factor (EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        val newEF = max(1.3f, oldEF + (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f)))

        var newRepetitions = current.repetitions
        var newInterval: Int
        var newLapses = current.lapses

        if (q < 3) {
            // AGAIN: Failure response
            newRepetitions = 0
            newInterval = 1
            newLapses += 1
        } else {
            // SUCCESS (Hard, Good, Easy)
            newRepetitions += 1
            newInterval = when (newRepetitions) {
                1 -> if (rating == SrsRating.EASY) 4 else 1
                2 -> if (rating == SrsRating.EASY) 8 else 3
                else -> {
                    val base = ceil(current.intervalDays * newEF).toInt()
                    if (rating == SrsRating.EASY) ceil(base * 1.3f).toInt() else base
                }
            }
        }

        val millisPerDay = 24L * 60L * 60L * 1000L
        val nextDue = nowMillis + (newInterval * millisPerDay)

        return SrsData(
            audioId = current.audioId,
            repetitions = newRepetitions,
            intervalDays = newInterval,
            easeFactor = newEF,
            nextDueDateMillis = nextDue,
            lastReviewedMillis = nowMillis,
            lapses = newLapses
        )
    }

    /**
     * Formats interval preview label for answer buttons (e.g. "<10m", "1d", "3d", "8d").
     */
    fun getIntervalPreview(current: SrsData, rating: SrsRating): String {
        val calculated = processReview(current, rating)
        return when {
            rating == SrsRating.AGAIN -> "<10m"
            calculated.intervalDays == 1 -> "1d"
            else -> "${calculated.intervalDays}d"
        }
    }

    /**
     * SharedPreferences persistence helpers for SRS data.
     */
    fun loadSrsData(prefs: SharedPreferences, audioId: String): SrsData {
        val rawJson = prefs.getString(PREFS_KEY_PREFIX + audioId, null) ?: return SrsData(audioId = audioId)
        return try {
            val json = JSONObject(rawJson)
            SrsData(
                audioId = audioId,
                repetitions = json.optInt("reps", 0),
                intervalDays = json.optInt("interval", 0),
                easeFactor = json.optDouble("ef", 2.5).toFloat(),
                nextDueDateMillis = json.optLong("due", 0L),
                lastReviewedMillis = json.optLong("last", 0L),
                lapses = json.optInt("lapses", 0)
            )
        } catch (e: Exception) {
            SrsData(audioId = audioId)
        }
    }

    fun saveSrsData(prefs: SharedPreferences, data: SrsData) {
        val json = JSONObject().apply {
            put("reps", data.repetitions)
            put("interval", data.intervalDays)
            put("ef", data.easeFactor.toDouble())
            put("due", data.nextDueDateMillis)
            put("last", data.lastReviewedMillis)
            put("lapses", data.lapses)
        }
        prefs.edit().putString(PREFS_KEY_PREFIX + data.audioId, json.toString()).apply()
    }

    /**
     * Parses custom user CSV/TSV flashcard text into VocabItems.
     * Expected format: Japanese,Furigana,English,Bangla (or tab separated)
     */
    fun parseCustomCsv(csvText: String): List<VocabItem> {
        val items = mutableListOf<VocabItem>()
        val lines = csvText.lines()

        for ((idx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.lowercase().startsWith("japanese")) continue

            val delimiter = if (trimmed.contains("\t")) "\t" else ","
            val parts = trimmed.split(delimiter).map { it.trim().removeSurrounding("\"") }

            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val jp = parts[0]
                val furi = parts.getOrNull(1)?.ifBlank { jp } ?: jp
                val en = parts.getOrNull(2)?.ifBlank { "Custom Word" } ?: "Custom Word"
                val bn = parts.getOrNull(3) ?: ""

                val id = "custom_${jp}_$idx"
                items.add(
                    VocabItem(
                        audioId = id,
                        audioText = furi,
                        japanese = jp,
                        furigana = furi,
                        romaji = "",
                        english = en,
                        bangla = bn,
                        lesson = 99,
                        sectionKey = "custom",
                        sectionLabel = "Custom Deck",
                        source = "Custom CSV",
                        extraUseful = false
                    )
                )
            }
        }
        return items
    }
}
