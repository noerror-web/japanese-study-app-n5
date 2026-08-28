package com.momin.japanesestudyappn5

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object KanaLearn : NavKey
@Serializable data object Vocabulary : NavKey
@Serializable data class AnkiDeck(val quickMode: Boolean = false) : NavKey
@Serializable data class KanjiParticles(val initialTab: Int = 0) : NavKey
@Serializable data class BookReader(val pdfPath: String, val title: String) : NavKey
@Serializable data object ExamPractice : NavKey
@Serializable data object Grammar : NavKey
@Serializable data object Stats : NavKey
@Serializable data object Onboarding : NavKey
@Serializable data object UniversalSearch : NavKey
@Serializable data object VocabQuiz : NavKey
@Serializable data object DailyChallenge : NavKey
@Serializable data class KanaTrace(val char: String? = null) : NavKey
@Serializable data class GrammarExercises(val lessonId: Int = 1) : NavKey
@Serializable data object KanaSpeedQuiz : NavKey
@Serializable data object Achievements : NavKey
@Serializable data object SentenceBuilder : NavKey
@Serializable data object ReadingPractice : NavKey
@Serializable data object FallingWords : NavKey
@Serializable data object MatchingPairs : NavKey
@Serializable data object FillBlank : NavKey
@Serializable data object AIChat : NavKey
@Serializable data object WeakWords : NavKey
@Serializable data object Login : NavKey
@Serializable data object StreakSaver : NavKey
@Serializable data object QuestShop : NavKey
@Serializable data object ParticleGame : NavKey
@Serializable data object CdSection : NavKey
@Serializable data object OwnerDashboard : NavKey
@Serializable data object Translation : NavKey
@Serializable data object Dictionary : NavKey



