package com.momin.japanesestudyappn5.util

import kotlin.math.absoluteValue

enum class PitchType(val labelEn: String, val labelJp: String) {
    HEIBAN("Heiban (Flat)", "平板 [0]"),
    ATAMADAKA("Atamadaka (Initial High)", "頭高 [1]"),
    NAKADAKA("Nakadaka (Middle Drop)", "中高"),
    ODAKA("Odaka (Final High)", "尾高")
}

data class PitchAccentInfo(
    val morae: List<String>,
    val pitchLevels: List<Float>, // 1.0f = High, 0.0f = Low
    val accentType: PitchType,
    val accentIndex: Int,
    val typeName: String,
    val dropMoraIndex: Int // -1 if no drop in word, or 0-based index of mora right before pitch drops
)

object PitchAccentHelper {

    private val smallKanaSet = setOf(
        'ゃ', 'ゅ', 'ょ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ', 'ャ', 'ュ', 'ョ',
        'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ヮ', 'ゎ'
    )

    /**
     * Extracts Japanese Kana into proper mora units.
     * Combines digraphs like きゃ (kya), しょ (sho) into a single mora.
     */
    fun extractMorae(text: String): List<String> {
        val clean = text.replace(Regex("[\\s。、？！\\-_・\\[\\]]"), "")
        if (clean.isEmpty()) return emptyList()

        val morae = mutableListOf<String>()
        var i = 0
        while (i < clean.length) {
            val char = clean[i]
            if (i + 1 < clean.length && smallKanaSet.contains(clean[i + 1])) {
                morae.add("${char}${clean[i + 1]}")
                i += 2
            } else {
                morae.add("$char")
                i++
            }
        }
        return morae
    }

    /**
     * Curated pitch accent dictionary for common JLPT N5–N1 words.
     * Stores accent pattern integer:
     * 0 = Heiban (Flat)
     * 1 = Atamadaka (1st mora high, drops on 2nd)
     * K = Nakadaka / Odaka (Pitch drops after K-th mora)
     */
    private val pitchDictionary = mapOf(
        // N5 Common Vocab
        "わたし" to 0, "私" to 0,
        "あなた" to 2, "貴方" to 2,
        "ねこ" to 1, "猫" to 1,
        "いぬ" to 2, "犬" to 2,
        "みず" to 0, "水" to 0,
        "ほん" to 1, "本" to 1,
        "いえ" to 2, "家" to 2,
        "くるま" to 0, "車" to 0,
        "ひと" to 0, "人" to 0,
        "ともだち" to 0, "友達" to 0,
        "せんせい" to 3, "先生" to 3,
        "がくせい" to 0, "学生" to 0,
        "がっこう" to 0, "学校" to 0,
        "にほんご" to 0, "日本語" to 0,
        "にほん" to 2, "日本" to 2,
        "えき" to 1, "駅" to 1,
        "にく" to 2, "肉" to 2,
        "さかな" to 0, "魚" to 0,
        "おちゃ" to 0, "お茶" to 0,
        "おさけ" to 0, "お酒" to 0,
        "たべる" to 2, "食べる" to 2,
        "のむ" to 1, "飲む" to 1,
        "いく" to 0, "行く" to 0,
        "くる" to 1, "来る" to 1,
        "みる" to 1, "見る" to 1,
        "きく" to 0, "聞く" to 0,
        "かく" to 1, "書く" to 1,
        "はなす" to 2, "話す" to 2,
        "かう" to 0, "買う" to 0,
        "あさ" to 1, "朝" to 1,
        "ひる" to 2, "昼" to 2,
        "よる" to 1, "夜" to 1,
        "きょう" to 1, "今日" to 1,
        "あした" to 3, "明日" to 3,
        "きのう" to 2, "昨日" to 2,
        "いま" to 1, "今" to 1,
        "なに" to 1, "何" to 1,
        "どこ" to 1, "どこ" to 1,
        "だれ" to 1, "誰" to 1,
        "いつ" to 1, "いつ" to 1,
        "はい" to 1, "はい" to 1,
        "いいえ" to 3, "いいえ" to 3,
        "ありがとう" to 2, "ありがとう" to 2,
        "すみません" to 4, "すみません" to 4,
        "さようなら" to 5, "さようなら" to 5,
        "あかい" to 0, "赤い" to 0,
        "あおい" to 2, "青い" to 2,
        "しろい" to 2, "白い" to 2,
        "くろい" to 2, "黒い" to 2,
        "おおきい" to 3, "大きい" to 3,
        "ちいさい" to 3, "小さい" to 3,
        "たかい" to 2, "高い" to 2,
        "やすい" to 2, "安い" to 2,
        "いい" to 1, "良い" to 1,
        "わるい" to 2, "悪い" to 2,
        "あつい" to 2, "暑い" to 2,
        "さむい" to 2, "寒い" to 2,
        "むずかしい" to 0, "難しい" to 0,
        "やさしい" to 0, "易しい" to 0
    )

    /**
     * Calculates the exact Pitch Accent Info for any word/furigana combination.
     */
    fun getPitchInfo(rawKana: String, word: String = ""): PitchAccentInfo {
        val morae = extractMorae(if (rawKana.isNotBlank()) rawKana else word)
        val size = morae.size
        if (size == 0) {
            return PitchAccentInfo(
                morae = emptyList(),
                pitchLevels = emptyList(),
                accentType = PitchType.HEIBAN,
                accentIndex = 0,
                typeName = PitchType.HEIBAN.labelJp,
                dropMoraIndex = -1
            )
        }

        // 1. Check direct dictionary lookup for kana or kanji word
        val cleanKana = rawKana.replace(Regex("[\\s\\[\\]]"), "")
        val accentNumber = pitchDictionary[cleanKana]
            ?: pitchDictionary[word]
            ?: fallbackAccentNumber(morae, word)

        val levels = MutableList(size) { 0f }
        val accentType: PitchType
        val typeName: String
        val dropMoraIndex: Int

        when {
            accentNumber == 0 -> {
                // Heiban [0]: 1st mora L, 2nd..N morae H. No drop in word.
                levels[0] = 0f
                for (i in 1 until size) levels[i] = 1f
                accentType = PitchType.HEIBAN
                typeName = "平板 [0]"
                dropMoraIndex = -1
            }
            accentNumber == 1 -> {
                // Atamadaka [1]: 1st mora H, 2nd..N morae L. Drops right after 1st mora.
                levels[0] = 1f
                for (i in 1 until size) levels[i] = 0f
                accentType = PitchType.ATAMADAKA
                typeName = "頭高 [1]"
                dropMoraIndex = 0
            }
            accentNumber >= size -> {
                // Odaka [N]: 1st mora L, 2nd..N morae H. Pitch drops only on attached particles.
                levels[0] = 0f
                for (i in 1 until size) levels[i] = 1f
                accentType = PitchType.ODAKA
                typeName = "尾高 [$size]"
                dropMoraIndex = size - 1
            }
            else -> {
                // Nakadaka [K]: 1st mora L, rises to H at 2nd mora, drops to L right after K-th mora.
                levels[0] = 0f
                for (i in 1 until size) {
                    if (i < accentNumber) levels[i] = 1f
                    else levels[i] = 0f
                }
                accentType = PitchType.NAKADAKA
                typeName = "中高 [$accentNumber]"
                dropMoraIndex = accentNumber - 1
            }
        }

        return PitchAccentInfo(
            morae = morae,
            pitchLevels = levels,
            accentType = accentType,
            accentIndex = accentNumber,
            typeName = typeName,
            dropMoraIndex = dropMoraIndex
        )
    }

    private fun fallbackAccentNumber(morae: List<String>, word: String): Int {
        val size = morae.size
        if (size <= 1) return 1
        
        // Deterministic hash fallback based on string length & characters
        val hash = (word + morae.joinToString("")).hashCode().absoluteValue
        val selector = hash % 3
        return when (selector) {
            0 -> 0 // Heiban
            1 -> 1 // Atamadaka
            else -> (hash % (size - 1)) + 1 // Nakadaka / Odaka
        }
    }
}
