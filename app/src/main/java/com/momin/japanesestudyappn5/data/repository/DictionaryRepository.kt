package com.momin.japanesestudyappn5.data.repository

import android.content.Context
import android.content.res.AssetManager
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.*
import com.momin.japanesestudyappn5.util.OnlineAssetsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class DictionaryJsonWrapper(
    val words: List<JMdictEntry> = emptyList(),
    val kanji: List<KanjiDicEntry> = emptyList(),
    val sentences: List<TatoebaSentence> = emptyList()
)

interface DictionaryRepository {
    suspend fun searchWords(query: String): List<JMdictEntry>
    suspend fun searchKanji(query: String): List<KanjiDicEntry>
    suspend fun searchSentences(query: String): List<TatoebaSentence>
    suspend fun getKanjiByChar(char: String): KanjiDicEntry?
    suspend fun getWordById(id: String): JMdictEntry?
    suspend fun reload()
}

class DefaultDictionaryRepository(
    private val context: Context,
    private val assetManager: AssetManager,
    private val appDataRepository: DataRepository
) : DictionaryRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private var jmdictList: List<JMdictEntry>? = null
    private var kanjidicList: List<KanjiDicEntry>? = null
    private var tatoebaList: List<TatoebaSentence>? = null

    private val lock = Any()

    override suspend fun reload() {
        synchronized(lock) {
            jmdictList = null
            kanjidicList = null
            tatoebaList = null
        }
        ensureLoaded(force = true)
    }

    private suspend fun ensureLoaded(force: Boolean = false) = withContext(Dispatchers.IO) {
        if (!force && jmdictList != null && kanjidicList != null && tatoebaList != null) return@withContext

        synchronized(lock) {
            if (force || jmdictList == null || kanjidicList == null || tatoebaList == null) {
                try {
                    val downloadedFile = OnlineAssetsManager.getLocalFile(context, OnlineAssetsManager.DICTIONARY_ASSET_PATH)
                    val rawJson = if (downloadedFile.exists() && downloadedFile.length() > 0) {
                        OnlineAssetsManager.logDiagnostic("Loading downloaded expanded dictionary from internal storage (${downloadedFile.length()} bytes)")
                        downloadedFile.readText()
                    } else {
                        OnlineAssetsManager.logDiagnostic("Loading core dictionary asset from app package")
                        assetManager.open("dictionary_data.json").bufferedReader().use { it.readText() }
                    }
                    val wrapper = json.decodeFromString<DictionaryJsonWrapper>(rawJson)
                    jmdictList = wrapper.words
                    kanjidicList = wrapper.kanji
                    tatoebaList = wrapper.sentences
                } catch (e: Exception) {
                    e.printStackTrace()
                    jmdictList = emptyList()
                    kanjidicList = emptyList()
                    tatoebaList = emptyList()
                }
            }
        }
    }

    override suspend fun searchWords(query: String): List<JMdictEntry> = withContext(Dispatchers.IO) {
        ensureLoaded()
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        val jmResults = (jmdictList ?: emptyList()).filter { entry ->
            entry.kanji.contains(q, ignoreCase = true) ||
            entry.reading.contains(q, ignoreCase = true) ||
            entry.furigana.contains(q, ignoreCase = true) ||
            entry.romaji.contains(q, ignoreCase = true) ||
            entry.senses.any { sense -> sense.meanings.any { m -> m.contains(q, ignoreCase = true) } } ||
            (entry.bangla != null && entry.bangla.contains(q, ignoreCase = true))
        }.toMutableList()

        // Also search existing VocabItems from App Repository and convert to JMdictEntry if not present
        val existingVocab = appDataRepository.getVocabulary()
        val existingMatches = existingVocab.filter { item ->
            item.japanese.contains(q, ignoreCase = true) ||
            item.furigana.contains(q, ignoreCase = true) ||
            item.romaji.contains(q, ignoreCase = true) ||
            item.english.contains(q, ignoreCase = true) ||
            item.bangla.contains(q, ignoreCase = true)
        }

        for (item in existingMatches) {
            if (jmResults.none { it.kanji == item.japanese || it.reading == item.furigana }) {
                jmResults.add(
                    JMdictEntry(
                        id = item.audioId,
                        kanji = item.japanese,
                        reading = item.furigana.ifBlank { item.japanese },
                        furigana = if (item.japanese != item.furigana && item.furigana.isNotBlank()) "${item.japanese}[${item.furigana}]" else item.japanese,
                        romaji = item.romaji,
                        isCommon = true,
                        jlptLevel = "N5",
                        bangla = item.bangla,
                        senses = listOf(
                            JMdictSense(
                                partsOfSpeech = listOf(item.sectionLabel.ifEmpty { "Vocabulary" }),
                                meanings = listOf(item.english),
                                glossesBn = if (item.bangla.isNotBlank()) listOf(item.bangla) else null
                            )
                        )
                    )
                )
            }
        }

        jmResults.sortedByDescending { it.isCommon }
    }

    override suspend fun searchKanji(query: String): List<KanjiDicEntry> = withContext(Dispatchers.IO) {
        ensureLoaded()
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        val kanjiResults = (kanjidicList ?: emptyList()).filter { k ->
            k.kanji.contains(q) ||
            k.onyomi.any { o -> o.contains(q, ignoreCase = true) } ||
            k.kunyomi.any { ku -> ku.contains(q, ignoreCase = true) } ||
            k.meanings.any { m -> m.contains(q, ignoreCase = true) } ||
            (k.meaningsBn != null && k.meaningsBn.any { mb -> mb.contains(q, ignoreCase = true) })
        }.toMutableList()

        // Also check existing KanjiItems from App Repository
        val existingKanjis = appDataRepository.getKanjis()
        val existingMatches = existingKanjis.filter { k ->
            k.kanji.contains(q) ||
            k.meanings.contains(q, ignoreCase = true) ||
            k.on.contains(q, ignoreCase = true) ||
            k.kun.contains(q, ignoreCase = true)
        }

        for (k in existingMatches) {
            if (kanjiResults.none { it.kanji == k.kanji }) {
                kanjiResults.add(
                    KanjiDicEntry(
                        kanji = k.kanji,
                        onyomi = k.on.split(",", " ").filter { it.isNotBlank() },
                        kunyomi = k.kun.split(",", " ").filter { it.isNotBlank() },
                        meanings = k.meanings.split(",").map { it.trim() },
                        jlptLevel = "N5",
                        grade = k.grade,
                        strokeCount = k.strokes,
                        freq = k.freq
                    )
                )
            }
        }

        kanjiResults
    }

    override suspend fun searchSentences(query: String): List<TatoebaSentence> = withContext(Dispatchers.IO) {
        ensureLoaded()
        val q = query.trim()
        if (q.isEmpty()) return@withContext emptyList()

        val sentenceResults = (tatoebaList ?: emptyList()).filter { s ->
            s.japanese.contains(q, ignoreCase = true) ||
            s.furigana.contains(q, ignoreCase = true) ||
            s.english.contains(q, ignoreCase = true) ||
            (s.bangla != null && s.bangla.contains(q, ignoreCase = true))
        }.toMutableList()

        // Also check existing ExampleSentences map from App Repository
        val appSentencesMap = appDataRepository.getSentences()
        for ((_, sList) in appSentencesMap) {
            for (s in sList) {
                if (s.japanese.contains(q, ignoreCase = true) || s.english.contains(q, ignoreCase = true) || (s.bangla != null && s.bangla.contains(q, ignoreCase = true))) {
                    if (sentenceResults.none { it.japanese == s.japanese }) {
                        sentenceResults.add(
                            TatoebaSentence(
                                id = "app_sent_" + s.japanese.hashCode(),
                                japanese = s.japanese,
                                furigana = s.furigana,
                                english = s.english,
                                bangla = s.bangla,
                                attribution = "JLPT N5 Sentence Bank"
                            )
                        )
                    }
                }
            }
        }

        sentenceResults
    }

    override suspend fun getKanjiByChar(char: String): KanjiDicEntry? = withContext(Dispatchers.IO) {
        val matches = searchKanji(char)
        matches.firstOrNull { it.kanji == char } ?: matches.firstOrNull()
    }

    override suspend fun getWordById(id: String): JMdictEntry? = withContext(Dispatchers.IO) {
        ensureLoaded()
        (jmdictList ?: emptyList()).firstOrNull { it.id == id }
    }
}
