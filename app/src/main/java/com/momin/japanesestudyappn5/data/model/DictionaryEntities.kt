package com.momin.japanesestudyappn5.data.model

import kotlinx.serialization.Serializable

@Serializable
data class JMdictSense(
    val partsOfSpeech: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val glossesBn: List<String>? = null,
    val info: String? = null
)

@Serializable
data class JMdictEntry(
    val id: String,
    val kanji: String = "",
    val reading: String,
    val furigana: String = "",
    val romaji: String = "",
    val senses: List<JMdictSense> = emptyList(),
    val priority: List<String> = emptyList(),
    val isCommon: Boolean = false,
    val jlptLevel: String? = null,
    val bangla: String? = null
)

@Serializable
data class KanjiDicEntry(
    val kanji: String,
    val onyomi: List<String> = emptyList(),
    val kunyomi: List<String> = emptyList(),
    val nanori: List<String> = emptyList(),
    val meanings: List<String> = emptyList(),
    val meaningsBn: List<String>? = null,
    val jlptLevel: String? = null,
    val grade: Int? = null,
    val strokeCount: Int = 0,
    val radical: String? = null,
    val freq: Int? = null,
    val examples: List<KanjiExample> = emptyList()
)

@Serializable
data class KanjiExample(
    val word: String,
    val reading: String,
    val meaning: String
)

@Serializable
data class TatoebaSentence(
    val id: String,
    val japanese: String,
    val furigana: String = "",
    val english: String,
    val bangla: String? = null,
    val attribution: String = "Tatoeba.org (CC-BY 2.0 FR / CC0)"
)

@Serializable
data class FuriganaSegment(
    val text: String,
    val furigana: String? = null
)

object FuriganaParser {
    /**
     * Parses standard furigana bracket notation like "食[た]べる" or "私[わたし]は 日本人[にほんじん]です"
     * into a list of FuriganaSegments.
     */
    fun parse(raw: String): List<FuriganaSegment> {
        if (raw.isBlank()) return emptyList()
        val segments = mutableListOf<FuriganaSegment>()
        val regex = Regex("([^\\s\\[\\]]+?)\\[([^\\]]+)\\]|([^\\[\\]]+)")
        val matches = regex.findAll(raw)

        for (match in matches) {
            val kanjiPart = match.groups[1]?.value
            val furiganaPart = match.groups[2]?.value
            val plainPart = match.groups[3]?.value

            if (kanjiPart != null && furiganaPart != null) {
                segments.add(FuriganaSegment(kanjiPart, furiganaPart))
            } else if (plainPart != null) {
                segments.add(FuriganaSegment(plainPart, null))
            }
        }

        if (segments.isEmpty() && raw.isNotBlank()) {
            segments.add(FuriganaSegment(raw, null))
        }

        return segments
    }
}
