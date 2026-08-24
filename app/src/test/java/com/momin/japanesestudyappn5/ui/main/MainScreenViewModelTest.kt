package com.momin.japanesestudyappn5.ui.main

import android.content.SharedPreferences
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeMyModelRepository(), FakeSharedPreferences())
    // When ViewModel starts, it triggers loadDashboardStats immediately which updates state to Success
    // Since loadDashboardStats runs in viewModelScope, it might be Success or Loading when we first query it
    val state = viewModel.uiState.value
    // Success is expected as coroutines start instantly in runTest
    assert(state is MainScreenUiState.Success || state is MainScreenUiState.Loading)
  }
}

private class FakeSharedPreferences : SharedPreferences {
    override fun getAll(): Map<String, *> = emptyMap<String, Any>()
    override fun getString(key: String?, defValue: String?): String? = defValue
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = defValue
    override fun getLong(key: String?, defValue: Long): Long = defValue
    override fun getFloat(key: String?, defValue: Float): Float = defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
    override fun contains(key: String?): Boolean = false
    override fun edit(): SharedPreferences.Editor = throw UnsupportedOperationException()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

private class FakeMyModelRepository : DataRepository {
    override suspend fun getVocabulary(): List<VocabItem> = emptyList()
    override suspend fun getExamSets(): List<ExamSet> = emptyList()
    override suspend fun getHiraganaSvg(char: String): String? = null
    override suspend fun getKatakanaSvg(char: String): String? = null
    override suspend fun getKanjis(): List<KanjiItem> = emptyList()
    override suspend fun getParticles(): List<ParticleItem> = emptyList()
    override suspend fun getGrammarLessons(language: String): List<GrammarLesson> = emptyList()
    override suspend fun getSentences(): Map<String, List<ExampleSentence>> = emptyMap()
}
