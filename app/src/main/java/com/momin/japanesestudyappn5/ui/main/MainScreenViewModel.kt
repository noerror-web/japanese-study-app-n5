package com.momin.japanesestudyappn5.ui.main

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardStats(
    val totalOpens: Int,
    val bookmarkedVocabCount: Int,
    val totalVocabCount: Int,
    val totalKanjiCount: Int,
    val totalParticlesCount: Int,
    val totalGrammarCount: Int,
    val totalExamsCount: Int,
    val wordOfTheDay: VocabItem? = null
)

class MainScreenViewModel(
    private val dataRepository: DataRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<MainScreenUiState>(MainScreenUiState.Loading)
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    init {
        loadDashboardStats()
    }

    fun loadDashboardStats() {
        viewModelScope.launch {
            _uiState.value = MainScreenUiState.Loading
            try {
                val vocabList = dataRepository.getVocabulary()
                val kanjiList = dataRepository.getKanjis()
                val particleList = dataRepository.getParticles()
                val grammarList = dataRepository.getGrammarLessons()
                val examSets = dataRepository.getExamSets()
                
                val bookmarks = sharedPreferences.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
                val totalOpens = sharedPreferences.getInt("total_opens", 1)

                // Word of the day — seeded by day of year for consistent daily pick
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val wordOfDay = if (vocabList.isNotEmpty()) vocabList[dayOfYear % vocabList.size] else null

                val stats = DashboardStats(
                    totalOpens = totalOpens,
                    bookmarkedVocabCount = bookmarks.size,
                    totalVocabCount = vocabList.size,
                    totalKanjiCount = kanjiList.size,
                    totalParticlesCount = particleList.size,
                    totalGrammarCount = grammarList.size,
                    totalExamsCount = examSets.size,
                    wordOfTheDay = wordOfDay
                )
                _uiState.value = MainScreenUiState.Success(stats)
            } catch (e: Exception) {
                _uiState.value = MainScreenUiState.Error(e)
            }
        }
    }
}

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val data: DashboardStats) : MainScreenUiState
}
