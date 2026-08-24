package com.momin.japanesestudyappn5.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VocabItem(
    val audioId: String,
    val audioText: String,
    val japanese: String,
    val furigana: String,
    val romaji: String,
    val english: String,
    val bangla: String,
    val lesson: Int? = null,
    val lessonOrder: Int? = null,
    val sectionKey: String,
    val sectionLabel: String,
    val source: String,
    val extraUseful: Boolean
)

@Serializable
data class Question(
    val number: Int,
    val stem: String,
    val choices: List<String>,
    val answer: Int,
    val explanation: String? = null
)

@Serializable
data class ExamPart(
    val id: String,
    val title: String,
    val instruction: String,
    val context: String? = null,
    val questions: List<Question>
)

@Serializable
data class ExamSet(
    val id: String,
    val title: String,
    val subtitle: String,
    val sourceNote: String? = null,
    val parts: List<ExamPart>
)

@Serializable
data class KanjiItem(
    val audioId: String,
    val audioText: String,
    val kanji: String,
    val meanings: String,
    val on: String,
    val kun: String,
    val strokes: Int,
    val grade: Int,
    val freq: Int,
    val coverage: Int,
    val group: String,
    val gradeGroup: String,
    val exampleJapanese: String = "",
    val exampleFurigana: String = "",
    val exampleRomaji: String = "",
    val exampleEnglish: String = "",
    val exampleBangla: String = "",
    val onExampleJapanese: String = "",
    val onExampleFurigana: String = "",
    val onExampleRomaji: String = "",
    val onExampleEnglish: String = "",
    val onExampleBangla: String = "",
    val kunExampleJapanese: String = "",
    val kunExampleFurigana: String = "",
    val kunExampleRomaji: String = "",
    val kunExampleEnglish: String = "",
    val kunExampleBangla: String = "",
    val svgPaths: List<String>
)

@Serializable
data class ParticleItem(
    val audioId: String,
    val audioText: String,
    val particle: String,
    val reading: String,
    val meaning: String,
    val description: String,
    val group: String,
    val exampleJa: String,
    val exampleRomaji: String,
    val translation: String,
    val translationBn: String,
    val note: String,
    val cloze: String
)

@Serializable
data class GrammarContentItem(
    val text: String,
    val type: String
)

@Serializable
data class GrammarLesson(
    val lesson: Int,
    val title: String,
    val content: List<GrammarContentItem>,
    val rules: List<String>
)

@Serializable
data class ExampleSentence(
    val japanese: String,
    val furigana: String,
    val english: String,
    val bangla: String? = null
)
