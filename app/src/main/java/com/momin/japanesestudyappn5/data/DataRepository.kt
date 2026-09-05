package com.momin.japanesestudyappn5.data

import android.content.res.AssetManager
import com.momin.japanesestudyappn5.data.model.ExamSet
import com.momin.japanesestudyappn5.data.model.ExampleSentence
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.data.model.GrammarLesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

interface DataRepository {
    suspend fun getVocabulary(): List<VocabItem>
    suspend fun getExamSets(): List<ExamSet>
    suspend fun getHiraganaSvg(char: String): String?
    suspend fun getKatakanaSvg(char: String): String?
    suspend fun getKanjis(): List<com.momin.japanesestudyappn5.data.model.KanjiItem>
    suspend fun getParticles(): List<com.momin.japanesestudyappn5.data.model.ParticleItem>
    suspend fun getGrammarLessons(language: String = "bn"): List<GrammarLesson>
    suspend fun getSentences(): Map<String, List<ExampleSentence>>
    suspend fun searchDictionary(query: String, maxResults: Int = 50): List<com.momin.japanesestudyappn5.data.model.JMdictEntry>
}

class DefaultDataRepository(private val assetManager: AssetManager) : DataRepository {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

    private var vocabList: List<VocabItem>? = null
    private var examSetsList: List<ExamSet>? = null
    private var hiraganaSvgMap: Map<String, String>? = null
    private var katakanaSvgMap: Map<String, String>? = null
    private var kanjiList: List<com.momin.japanesestudyappn5.data.model.KanjiItem>? = null
    private var particleList: List<com.momin.japanesestudyappn5.data.model.ParticleItem>? = null
    private var grammarListBn: List<GrammarLesson>? = null
    private var grammarListEn: List<GrammarLesson>? = null
    private var sentencesMap: Map<String, List<ExampleSentence>>? = null

    private val vocabLock = Any()
    private val examLock = Any()
    private val hiraganaLock = Any()
    private val katakanaLock = Any()
    private val kanjiLock = Any()
    private val particleLock = Any()
    private val grammarEnLock = Any()
    private val grammarBnLock = Any()
    private val sentencesLock = Any()

    private fun cleanJsonString(raw: String): String {
        val trimmed = raw.trim().removePrefix("\uFEFF")
        if (trimmed.startsWith("[")) {
            val lastIndex = trimmed.lastIndexOf(']')
            if (lastIndex != -1) {
                return trimmed.substring(0, lastIndex + 1)
            }
        } else if (trimmed.startsWith("{")) {
            val lastIndex = trimmed.lastIndexOf('}')
            if (lastIndex != -1) {
                return trimmed.substring(0, lastIndex + 1)
            }
        }
        return trimmed
    }

    override suspend fun getVocabulary(): List<VocabItem> = withContext(Dispatchers.IO) {
        vocabList ?: synchronized(vocabLock) {
            vocabList ?: try {
                val jsonString = assetManager.open("anki_vocab_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<VocabItem>>(cleanJsonString(jsonString)).also { vocabList = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getExamSets(): List<ExamSet> = withContext(Dispatchers.IO) {
        examSetsList ?: synchronized(examLock) {
            examSetsList ?: try {
                val jsonString = assetManager.open("exam_question_bank.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<ExamSet>>(cleanJsonString(jsonString)).also { examSetsList = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getHiraganaSvg(char: String): String? = withContext(Dispatchers.IO) {
        val map = hiraganaSvgMap ?: synchronized(hiraganaLock) {
            hiraganaSvgMap ?: try {
                val jsonString = assetManager.open("hiragana_svg_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<Map<String, String>>(cleanJsonString(jsonString)).also { hiraganaSvgMap = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
        map[char]
    }

    override suspend fun getKatakanaSvg(char: String): String? = withContext(Dispatchers.IO) {
        val map = katakanaSvgMap ?: synchronized(katakanaLock) {
            katakanaSvgMap ?: try {
                val jsonString = assetManager.open("katakana_svg_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<Map<String, String>>(cleanJsonString(jsonString)).also { katakanaSvgMap = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
        map[char]
    }

    override suspend fun getKanjis(): List<com.momin.japanesestudyappn5.data.model.KanjiItem> = withContext(Dispatchers.IO) {
        kanjiList ?: synchronized(kanjiLock) {
            kanjiList ?: try {
                val jsonString = assetManager.open("n5_kanji_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<com.momin.japanesestudyappn5.data.model.KanjiItem>>(cleanJsonString(jsonString)).also { kanjiList = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getParticles(): List<com.momin.japanesestudyappn5.data.model.ParticleItem> = withContext(Dispatchers.IO) {
        particleList ?: synchronized(particleLock) {
            particleList ?: try {
                val jsonString = assetManager.open("particles_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<com.momin.japanesestudyappn5.data.model.ParticleItem>>(cleanJsonString(jsonString)).also { particleList = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getGrammarLessons(language: String): List<GrammarLesson> = withContext(Dispatchers.IO) {
        if (language == "en") {
            grammarListEn ?: synchronized(grammarEnLock) {
                grammarListEn ?: try {
                    val jsonString = assetManager.open("n5_grammar_data_en.json").bufferedReader().use { it.readText() }
                    json.decodeFromString<List<GrammarLesson>>(cleanJsonString(jsonString)).also { grammarListEn = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        } else {
            grammarListBn ?: synchronized(grammarBnLock) {
                grammarListBn ?: try {
                    val jsonString = assetManager.open("n5_grammar_data.json").bufferedReader().use { it.readText() }
                    json.decodeFromString<List<GrammarLesson>>(cleanJsonString(jsonString)).also { grammarListBn = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
        }
    }

    private var jmdictList: List<com.momin.japanesestudyappn5.data.model.JMdictEntry>? = null
    private val jmdictLock = Any()

    override suspend fun getSentences(): Map<String, List<ExampleSentence>> = withContext(Dispatchers.IO) {
        sentencesMap ?: synchronized(sentencesLock) {
            sentencesMap ?: try {
                val jsonString = assetManager.open("sentences.json").bufferedReader().use { it.readText() }
                json.decodeFromString<Map<String, List<ExampleSentence>>>(cleanJsonString(jsonString)).also { sentencesMap = it }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }

    override suspend fun searchDictionary(query: String, maxResults: Int): List<com.momin.japanesestudyappn5.data.model.JMdictEntry> = withContext(Dispatchers.IO) {
        val entries = jmdictList ?: synchronized(jmdictLock) {
            jmdictList ?: try {
                val jsonString = assetManager.open("full_dictionary_data.json").bufferedReader().use { it.readText() }
                json.decodeFromString<List<com.momin.japanesestudyappn5.data.model.JMdictEntry>>(cleanJsonString(jsonString)).also { jmdictList = it }
            } catch (e: Exception) {
                emptyList()
            }
        }
        com.momin.japanesestudyappn5.util.SearchIndexEngine.search(entries, query, maxResults)
    }
}

