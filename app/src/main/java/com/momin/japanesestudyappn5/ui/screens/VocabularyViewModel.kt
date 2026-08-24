package com.momin.japanesestudyappn5.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.ExampleSentence
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.util.AIGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VocabularyViewModel(
    private val repository: DataRepository,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _fullVocabList = MutableStateFlow<List<VocabItem>>(emptyList())
    val fullVocabList = _fullVocabList.asStateFlow()

    private val _sentencesMap = MutableStateFlow<Map<String, List<ExampleSentence>>>(emptyMap())

    val selectedLesson = MutableStateFlow(1)
    val searchQuery = MutableStateFlow("")
    val filterMode = MutableStateFlow("all")
    val categoryFilter = MutableStateFlow("All")

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds = _bookmarkedIds.asStateFlow()

    private val _masteredIds = MutableStateFlow<Set<String>>(emptySet())
    val masteredIds = _masteredIds.asStateFlow()

    private val _notesMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val notesMap = _notesMap.asStateFlow()

    private val _shownMilestones = MutableStateFlow<Set<Int>>(emptySet())

    private val _confettiMessage = MutableStateFlow<Pair<String, String>?>(null)
    val confettiMessage = _confettiMessage.asStateFlow()

    private val _vocabToSentences = MutableStateFlow<Map<String, List<ExampleSentence>>>(emptyMap())
    val vocabToSentences = _vocabToSentences.asStateFlow()

    val listeningMode = MutableStateFlow(false)
    val listeningIndex = MutableStateFlow(0)

    val explanationSentence = MutableStateFlow<String?>(null)
    val explanationText = MutableStateFlow<String?>(null)
    val isExplaining = MutableStateFlow(false)

    val lessonCategories = mapOf(
        1 to "Greetings", 2 to "Greetings", 3 to "Numbers", 4 to "Numbers",
        5 to "Time", 6 to "Time", 7 to "People", 8 to "People",
        9 to "Places", 10 to "Places", 11 to "Food", 12 to "Food",
        13 to "Actions", 14 to "Actions", 15 to "Transport", 16 to "Transport",
        17 to "Nature", 18 to "Nature", 19 to "Home", 20 to "Home",
        21 to "Adjectives", 22 to "Adjectives", 23 to "School", 24 to "School",
        25 to "Mixed"
    )
    val categoryList = listOf("All") + lessonCategories.values.distinct()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Read shared preferences off the main thread
            val loadedBookmarks = withContext(Dispatchers.IO) {
                prefs.getStringSet("bookmarked_vocab", emptySet()) ?: emptySet()
            }
            val loadedMastered = withContext(Dispatchers.IO) {
                prefs.getStringSet("mastered_vocab", emptySet()) ?: emptySet()
            }
            val loadedShownMilestones = withContext(Dispatchers.IO) {
                (prefs.getStringSet("shown_milestones", emptySet()) ?: emptySet())
                    .mapNotNull { it.toIntOrNull() }.toSet()
            }
            val loadedNotes = withContext(Dispatchers.IO) {
                val noteKeys = prefs.all.filter { it.key.startsWith("note_") }
                noteKeys.mapKeys { it.key.removePrefix("note_") }
                    .mapValues { it.value as? String ?: "" }
            }

            _bookmarkedIds.value = loadedBookmarks
            _masteredIds.value = loadedMastered
            _shownMilestones.value = loadedShownMilestones
            _notesMap.value = loadedNotes

            // Load resources off-thread
            val vocabList = repository.getVocabulary()
            val sentences = repository.getSentences()
            
            _fullVocabList.value = vocabList
            _sentencesMap.value = sentences

            // Pre-compute O(1) vocab sentences map
            withContext(Dispatchers.IO) {
                val computedMap = mutableMapOf<String, List<ExampleSentence>>()
                vocabList.forEach { item ->
                    // 1. Direct audioId lookup (primary — sentences.json is keyed by audioId)
                    val directMatch = sentences[item.audioId]
                    computedMap[item.audioId] = when {
                        !directMatch.isNullOrEmpty() -> directMatch
                        else -> {
                            // 2. Fallback: generate a sentence programmatically
                            val category = lessonCategories[item.lesson ?: 25]
                            listOf(generateExampleSentence(item, category))
                        }
                    }
                }
                _vocabToSentences.value = computedMap
            }

            _isLoading.value = false
        }
    }

    // Filter vocab flow
    @Suppress("UNCHECKED_CAST")
    val filteredList: StateFlow<List<VocabItem>> = combine(
        _fullVocabList, selectedLesson, searchQuery, _bookmarkedIds, _masteredIds, filterMode, categoryFilter
    ) { params ->
        val vocab = params[0] as List<VocabItem>
        val lesson = params[1] as Int
        val query = params[2] as String
        val bookmarks = params[3] as Set<String>
        val mastered = params[4] as Set<String>
        val mode = params[5] as String
        val category = params[6] as String

        vocab.filter { item ->
            val matchesLesson = if (lesson == 0) true else item.lesson == lesson
            val matchesSearch = if (query.isBlank()) true else {
                item.japanese.contains(query, ignoreCase = true) ||
                        item.furigana.contains(query, ignoreCase = true) ||
                        item.romaji.contains(query, ignoreCase = true) ||
                        item.english.contains(query, ignoreCase = true) ||
                        item.bangla.contains(query, ignoreCase = true)
            }
            val matchesFilter = when (mode) {
                "bookmarked" -> bookmarks.contains(item.audioId)
                "mastered"   -> mastered.contains(item.audioId)
                "unknown"    -> !mastered.contains(item.audioId) && !bookmarks.contains(item.audioId)
                else         -> true
            }
            val matchesCategory = category == "All" || lessonCategories[item.lesson] == category
            matchesLesson && matchesSearch && matchesFilter && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleBookmark(audioId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newBookmarks = _bookmarkedIds.value.toMutableSet()
            if (newBookmarks.contains(audioId)) {
                newBookmarks.remove(audioId)
            } else {
                newBookmarks.add(audioId)
            }
            _bookmarkedIds.value = newBookmarks
            prefs.edit().putStringSet("bookmarked_vocab", newBookmarks).apply()
        }
    }

    fun toggleMastered(audioId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newMastered = _masteredIds.value.toMutableSet()
            if (newMastered.contains(audioId)) {
                newMastered.remove(audioId)
            } else {
                newMastered.add(audioId)
            }
            _masteredIds.value = newMastered
            prefs.edit().putStringSet("mastered_vocab", newMastered).apply()

            val count = newMastered.size
            val milestones = listOf(10, 25, 50, 100)
            val currentShown = _shownMilestones.value.toMutableSet()
            
            milestones.forEach { milestone ->
                if (count >= milestone && !currentShown.contains(milestone)) {
                    _confettiMessage.value = "🎉 $milestone Words Mastered!" to
                            "Incredible! You've mastered $milestone Japanese words. Keep going!"
                    currentShown.add(milestone)
                    _shownMilestones.value = currentShown
                    prefs.edit().putStringSet("shown_milestones", currentShown.map { it.toString() }.toSet()).apply()
                }
            }
        }
    }

    fun saveNote(audioId: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newNotes = _notesMap.value.toMutableMap()
            if (note.isBlank()) {
                newNotes.remove(audioId)
                prefs.edit().remove("note_$audioId").apply()
            } else {
                newNotes[audioId] = note
                prefs.edit().putString("note_$audioId", note).apply()
            }
            _notesMap.value = newNotes
        }
    }

    fun loadExplanation(sentence: String, appLanguage: String) {
        explanationSentence.value = sentence
        isExplaining.value = true
        explanationText.value = null
        viewModelScope.launch {
            val apiKey = prefs.getString("gemini_api_key", "") ?: ""
            if (apiKey.isBlank()) {
                explanationText.value = if (appLanguage == "bn") {
                    "জেমিনি এপিআই কি (Gemini API Key) সেট করা নেই। অনুগ্রহ করে সেটিংস-এ গিয়ে এপিআই কি সেট করুন, অথবা আপনার লাইসেন্স কি ভ্যালিডেট করে অটো-সিঙ্ক করুন।"
                } else {
                    "Gemini API Key is not configured. Please enter your Gemini API Key in Settings, or validate your license key to sync it automatically."
                }
                isExplaining.value = false
                return@launch
            }
            val result = AIGenerator.explainSentenceStructure(apiKey, sentence, appLanguage)
            explanationText.value = result ?: "Failed to load explanation. Please check your internet connection and try again."
            isExplaining.value = false
        }
    }

    fun clearExplanation() {
        explanationSentence.value = null
        explanationText.value = null
    }

    fun dismissConfetti() {
        _confettiMessage.value = null
    }

    private data class CustomSentenceData(
        val japanese: String,
        val furigana: String,
        val english: String,
        val bangla: String
    )

    private fun generateExampleSentence(item: VocabItem, category: String?): ExampleSentence {
        val jp = item.japanese
        val en = item.english.trim()
        val fg = if (item.furigana.isNotBlank()) item.furigana else item.japanese
        val bnMeaning = item.bangla.trim()

        // 1. Custom exact mapping for N5 vocabulary items
        val customSentences = mapOf(
            "これ" to CustomSentenceData("これ は わたし の 辞書 です。", "これはわたしのじしょです。", "This is my dictionary.", "এটি আমার অভিধান।"),
            "それ" to CustomSentenceData("それ は あなた の 傘 です か。", "それはあなたのかさですか。", "Is that your umbrella?", "ওটি কি আপনার ছাতা?"),
            "あれ" to CustomSentenceData("あれ は 図書館 です。", "あれはとしょかんです。", "That over there is the library.", "ওটি একটি লাইব্রেরি।"),
            "この" to CustomSentenceData("この 本 は おもしろい です。", "このほんはおもしろいです。", "This book is interesting.", "এই বইটি আকর্ষণীয়।"),
            "この～" to CustomSentenceData("この 本 は おもしろい です。", "このほんはおもしろいです。", "This book is interesting.", "এই বইটি আকর্ষণীয়।"),
            "その" to CustomSentenceData("その 辞書 を 貸してください。", "そのじしょをかしてください。", "Please lend me that dictionary.", "অনুগ্রহ করে আমাকে ওই অভিধানটি ধার দিন।"),
            "その～" to CustomSentenceData("その 辞書 を 貸してください。", "そのじしょをかしてください。", "Please lend me that dictionary.", "অনুগ্রহ করে আমাকে ওই অভিধানটি ধার দিন।"),
            "あの" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
            "あの～" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
            "ここ" to CustomSentenceData("ここ は 教室 です。", "ここはきょうしつです。", "This place is the classroom.", "এটি ক্লাসরুম।"),
            "そこ" to CustomSentenceData("そこ は 食堂 です。", "そこはしょくどうです。", "That place is the cafeteria.", "ওটি ক্যাফেটেরিয়া।"),
            "あそこ" to CustomSentenceData("あそこ は 駅 です。", "あそこはえきです。", "That place over there is the station.", "ওটি স্টেশন।"),
            "わたし" to CustomSentenceData("わたし は 学生 です。", "わたしはがくせいです。", "I am a student.", "আমি একজন ছাত্র।"),
            "わたしたち" to CustomSentenceData("わたしたち は 友達 です。", "わたしたちはともだちです。", "We are friends.", "আমরা বন্ধু।"),
            "あなた" to CustomSentenceData("あなた は 先生 です か。", "あなたはせんせいですか。", "Are you a teacher?", "আপনি কি একজন শিক্ষক?"),
            "あのひと" to CustomSentenceData("あの 人 は だれ です か。", "あのひとはだれですか。", "Who is that person?", "ঐ ব্যক্তি কে?"),
            "あのかた" to CustomSentenceData("あの 方 は どなた です か。", "あのかたはどなたですか。", "Who is that person (polite)?", "ঐ ব্যক্তি কে (সম্মানসূচক)?"),
            "みなさん" to CustomSentenceData("みなさん、こんにちは。", "みなさん、こんにちは。", "Hello, everyone.", "সবাইকে হ্যালো।"),
            "せんせい" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
            "先生" to CustomSentenceData("あの 人 は 日本語 の 先生 です。", "あのひとはにほんごのせんせいです。", "That person is a Japanese teacher.", "ঐ ব্যক্তি জাপানি ভাষার শিক্ষক।"),
            "がくせい" to CustomSentenceData("わたし は 東京 大学 の 学生 です。", "わたしはとうきょうだいがくのがくせいです。", "I am a student of Tokyo University.", "আমি টোকিও বিশ্ববিদ্যালয়ের একজন ছাত্র।"),
            "学生" to CustomSentenceData("わたし は 東京 大学 の 学生 です。", "わたしはとうきょうだいがくのがくせいです。", "I am a student of Tokyo University.", "আমি টোকিও বিশ্ববিদ্যালয়ের একজন ছাত্র।"),
            "ともだち" to CustomSentenceData("友達 と 一緒に 日本 に 行きます。", "ともだちといっしょににほんにいきます。", "I will go to Japan with my friend.", "আমি আমার বন্ধুর সাথে জাপানে যাব।"),
            "友達" to CustomSentenceData("友達 と 一緒に 日本 に 行きます。", "ともだちといっしょににほんにいきます。", "I will go to Japan with my friend.", "আমি আমার বন্ধুর সাথে জাপানে যাব।"),
            "にほん" to CustomSentenceData("日本 は きれい な 国 です。", "にほんはきれいなくにです。", "Japan is a beautiful country.", "জাপান একটি সুন্দর দেশ।"),
            "日本" to CustomSentenceData("日本 は きれい な 国 です。", "にほんはきれいなくにです。", "Japan is a beautiful country.", "জাপান একটি সুন্দর দেশ।"),
            "にほんご" to CustomSentenceData("日本語 は おもしろい です。", "にほんごはおもしろいです。", "Japanese is interesting.", "জাপানি ভাষা আকর্ষণীয়।"),
            "日本語" to CustomSentenceData("日本語 は おもしろい です。", "にほんごはおもしろいです。", "Japanese is interesting.", "জাপানি ভাষা আকর্ষণীয়।"),
            "えいご" to CustomSentenceData("わたし は 英語 を 話します。", "わたしはえいごをはなします。", "I speak English.", "আমি ইংরেজি বলি।"),
            "英語" to CustomSentenceData("わたし は 英語 を 話します。", "わたしはえいごをはなします。", "I speak English.", "আমি ইংরেজি বলি।"),
            "ほん" to CustomSentenceData("これ は わたし の 本 です。", "こちらはわたしのほんです。", "This is my book.", "এটি আমার বই।"),
            "本" to CustomSentenceData("これ は わたし の 本 です。", "こちらはわたしのほんです。", "This is my book.", "এটি আমার বই।"),
            "くるま" to CustomSentenceData("これ は だれ の 車 です か。", "こちらはだれのくるまですか。", "Whose car is this?", "এটি কার গাড়ি?"),
            "車" to CustomSentenceData("これ は だれ の 車 です か。", "こちらはだれのくるまですか。", "Whose car is this?", "এটি কার গাড়ি?"),
            "でんしゃ" to CustomSentenceData("電車 で 学校 に 行きます。", "でんしゃでがっこうにいきます。", "I go to school by train.", "আমি ট্রেনে করে স্কুলে যাই।"),
            "電車" to CustomSentenceData("電車 で 学校 に 行きます。", "でんしゃでがっこうにいきます。", "I go to school by train.", "আমি ট্রেনে করে স্কুলে যাই।"),
            "がっこう" to CustomSentenceData("学校 は どこ です か。", "がっこうはどこですか。", "Where is the school?", "স্কুলটি কোথায়?"),
            "学校" to CustomSentenceData("学校 は どこ です か。", "がっこうはどこですか。", "Where is the school?", "স্কুলটি কোথায়?"),
            "みず" to CustomSentenceData("水 を 飲みます。", "みずをのみます。", "I drink water.", "আমি পানি পান করি।"),
            "水" to CustomSentenceData("水 を 飲みます。", "みずをのみます。", "I drink water.", "আমি পানি পান করি।"),
            "おちゃ" to CustomSentenceData("お茶 を 飲みます か。", "おちゃをのみますか。", "Would you like to drink tea?", "আপনি কি চা পান করবেন?"),
            "お茶" to CustomSentenceData("お茶 を 飲みます か。", "おちゃをのみますか。", "Would you like to drink tea?", "আপনি কি চা পান করবেন?"),
            "コーヒー" to CustomSentenceData("コーヒー が 好き です。", "コーヒーがすきです。", "I like coffee.", "আমি কফি পছন্দ করি।"),
            "ごはん" to CustomSentenceData("朝ごはん を 食べました。", "あさごはんをたべました。", "I ate breakfast.", "আমি সকালের নাস্তা খেয়েছি।"),
            "さかな" to CustomSentenceData("魚 を 食べます。", "さかなをたべます。", "I eat fish.", "আমি মাছ খাই।"),
            "魚" to CustomSentenceData("魚 を 食べます。", "さかなをたべます。", "I eat fish.", "আমি মাছ খাই।"),
            "にく" to CustomSentenceData("肉 を 食べます。", "にくをたべます。", "I eat meat.", "আমি মাংস খাই।"),
            "肉" to CustomSentenceData("肉 を 食べます。", "にくをたべます。", "I eat meat.", "আমি মাংস খাই।"),
            "うち" to CustomSentenceData("うち に 帰ります。", "うちにかえります。", "I return home.", "আমি বাড়ি ফিরব।"),
            "へや" to CustomSentenceData("部屋 は きれい です。", "へやはきれいです。", "The room is clean.", "ঘরটি পরিষ্কার।"),
            "部屋" to CustomSentenceData("部屋 は きれい です。", "へやはきれいです。", "The room is clean.", "ঘরটি পরিষ্কার।"),
            "つくえ" to CustomSentenceData("机 の 上 に 本 が あります。", "つくえのうえにほんがあります。", "There is a book on the desk.", "টেবিলের উপর একটি বই আছে।"),
            "机" to CustomSentenceData("机 の 上 に 本 が あります。", "つくえのうえにほんがあります。", "There is a book on the desk.", "টেবিলের উপর একটি বই আছে।"),
            "いす" to CustomSentenceData("いす に 座ってください。", "いすにすわってください。", "Please sit on the chair.", "অনুগ্রহ করে চেয়ারে বসুন।"),
            "かばん" to CustomSentenceData("かばん の 中 に 何 が あります か。", "かばんのなかになにがありますか。", "What is in the bag?", "ব্যাগের ভেতর কী আছে?"),
            "とけい" to CustomSentenceData("これ は 新しい 時計 です。", "こちらはあたらしいとけいです。", "This is a new watch.", "এটি একটি নতুন ঘড়ি।"),
            "時計" to CustomSentenceData("これ は 新しい 時計 です。", "こちらはあたらしいとけいです。", "This is a new watch.", "এটি একটি নতুন ঘড়ি।"),
            "かさ" to CustomSentenceData("かさ を 買いました。", "かさをかいました。", "I bought an umbrella.", "আমি একটি ছাতা কিনেছি।"),
            "えんぴつ" to CustomSentenceData("えんぴつ で 書きます。", "えんぴつでかきます。", "I write with a pencil.", "আমি পেন্সিল দিয়ে লিখি।"),
            "てがみ" to CustomSentenceData("手紙 を 書きます。", "てがみをかきます。", "I write a letter.", "আমি একটি চিঠি লিখি।"),
            "手紙" to CustomSentenceData("手紙 を 書きます。", "てがみをかきます。", "I write a letter.", "আমি একটি চিঠি লিখি।"),
            "しゃしん" to CustomSentenceData("写真 を 撮ります。", "しゃしんをとります。", "I take a photo.", "আমি ছবি তুলি।"),
            "写真" to CustomSentenceData("写真 を 撮ります。", "しゃしんをとります。", "I take a photo.", "আমি ছবি তুলি।"),
            "えいが" to CustomSentenceData("週末 に 映画 を 見ました。", "しゅうまつにえいがをみました。", "I watched a movie on the weekend.", "আমি উইকএন্ডে সিনেমা দেখেছি।"),
            "映画" to CustomSentenceData("週末 に 映画 を 見ました。", "しゅうまつにえいがをみました。", "I watched a movie on the weekend.", "আমি উইকএন্ডে সিনেমা দেখেছি।"),
            "おんがく" to CustomSentenceData("音楽 を 聞く の が 好き です。", "おんがくをきくのがすきです。", "I like listening to music.", "আমি গান শুনতে পছন্দ করি।"),
            "音楽" to CustomSentenceData("音楽 を 聞く の が 好き です。", "おんがくをきくのがすきです。", "I like listening to music.", "আমি গান শুনতে পছন্দ করি।"),
            "てんき" to CustomSentenceData("今日 は いい 天気 です ね。", "きょうはいいてんきですね。", "Today the weather is nice, isn't it?", "আজ আবহাওয়া খুব ভালো, তাই না?"),
            "天気" to CustomSentenceData("今日 は いい 天気 です ね。", "きょうはいいてんきですね。", "Today the weather is nice, isn't it?", "আজ আবহাওয়া খুব ভালো, তাই না?"),
            "あつい" to CustomSentenceData("今日 は とても 暑い です。", "きょうはとてもあついです。", "Today is very hot.", "আজ খুব গরম।"),
            "暑い" to CustomSentenceData("今日 は とても 暑い です。", "きょうはとてもあついです。", "Today is very hot.", "আজ খুব গরম。"),
            "さむい" to CustomSentenceData("昨日 は 寒かった です。", "きのうはさむかったです。", "Yesterday was cold.", "গতকাল শীত ছিল।"),
            "寒い" to CustomSentenceData("昨日 は 寒かった です。", "きのうはさむかったです。", "Yesterday was cold.", "গতকাল শীত ছিল।"),
            "おいしい" to CustomSentenceData("この ラーメン は おいしい です。", "このらーめんはおいしいです。", "This ramen is delicious.", "এই রামেনটি সুস্বাদু।")
        )

        val custom = customSentences[jp] ?: customSentences[fg]
        if (custom != null) {
            return ExampleSentence(
                japanese = custom.japanese,
                furigana = custom.furigana,
                english = custom.english,
                bangla = custom.bangla
            )
        }

        // 2. Verbs Detection
        val isVerb = en.startsWith("to ", ignoreCase = true)
        if (isVerb) {
            val verbBase = en.removePrefix("to ").removePrefix("To ").trim()
            val isMotionVerb = jp.endsWith("いきます") || jp.endsWith("きます") || jp.endsWith("かえります") ||
                    en.contains("go ", ignoreCase = true) || en.contains("come ", ignoreCase = true) || en.contains("return ", ignoreCase = true)
            
            val isTransitiveVerb = jp.endsWith("たべます") || jp.endsWith("のみます") || jp.endsWith("かいます") || 
                    jp.endsWith("よみます") || jp.endsWith("かきます") || jp.endsWith("とります") || jp.endsWith("ききます") ||
                    en.contains("eat", ignoreCase = true) || en.contains("drink", ignoreCase = true) || en.contains("buy", ignoreCase = true) ||
                    en.contains("read", ignoreCase = true) || en.contains("write", ignoreCase = true) || en.contains("take", ignoreCase = true)

            return when {
                isMotionVerb -> ExampleSentence(
                    japanese = "わたし は 学校 へ $jp。",
                    furigana = "わたしはがっこうへ${fg}。",
                    english = "I $verbBase to school.",
                    bangla = "আমি স্কুলে $bnMeaning।"
                )
                isTransitiveVerb -> ExampleSentence(
                    japanese = "これ を $jp。",
                    furigana = "これを${fg}。",
                    english = "I $verbBase this.",
                    bangla = "আমি এটি $bnMeaning।"
                )
                else -> ExampleSentence(
                    japanese = "毎日 $jp。",
                    furigana = "まいにち${fg}。",
                    english = "I $verbBase every day.",
                    bangla = "আমি প্রতিদিন $bnMeaning।"
                )
            }
        }

        // 3. Question words fallback
        if (en.endsWith("?") || jp in listOf("なに", "なん", "どこ", "いつ", "どう", "いくら", "いくつ", "だれ", "どなた")) {
            return ExampleSentence(
                japanese = "それ は $jp です か。",
                furigana = "それは${fg}ですか。",
                english = "Where/What/Who is that?",
                bangla = "ওটি $bnMeaning?"
            )
        }

        // 4. Suffixes
        val isSuffix = en.contains("suffix", ignoreCase = true) || en.contains("prefix", ignoreCase = true) || en.startsWith("-") || jp in listOf("さん", "ちゃん", "くん", "じん", "さい")
        if (isSuffix) {
            val suffixLabel = en.replace("suffix for", "").replace("suffix", "").replace("-", "").trim()
            val suffixEn = when (jp) {
                "さん" -> "Mr./Mrs. Tanaka"
                "じん" -> "Japanese person"
                "さい" -> "10 years old"
                else -> "Tanaka-$suffixLabel"
            }
            val suffixBn = when (jp) {
                "さん" -> "জনাব তানাকা"
                "じん" -> "জাপানি নাগরিক"
                "さい" -> "১০ বছর বয়স"
                else -> "তানাকা-$bnMeaning"
            }
            return ExampleSentence(
                japanese = "田中 $jp。",
                furigana = "たなか${fg}。",
                english = suffixEn,
                bangla = suffixBn
            )
        }

        // 5. Adjectives
        if (category == "Adjectives" || jp.endsWith("い") || en.contains("easy", ignoreCase=true) || en.contains("difficult", ignoreCase=true) || en.contains("heavy", ignoreCase=true) || en.contains("light", ignoreCase=true)) {
            return ExampleSentence(
                japanese = "これ は $jp です。",
                furigana = "こちらは${fg}です。",
                english = "This is $en.",
                bangla = "এটি $bnMeaning।"
            )
        }

        // 6. Time and Days
        val isTime = category == "Time" || en.contains("today", ignoreCase=true) || en.contains("tomorrow", ignoreCase=true) || en.contains("yesterday", ignoreCase=true) || en.contains("now", ignoreCase=true) || en.contains("o'clock", ignoreCase=true) || en.contains("minute", ignoreCase=true) || en.contains("hour", ignoreCase=true) || en.contains("day", ignoreCase=true) || en.contains("month", ignoreCase=true) || en.contains("year", ignoreCase=true)
        if (isTime) {
            return ExampleSentence(
                japanese = "いま は $jp です。",
                furigana = "いまは${fg}です。",
                english = "Now it is $en.",
                bangla = "এখন $bnMeaning।"
            )
        }

        // 7. Places
        val isPlace = category == "Places" || en.contains("station", ignoreCase=true) || en.contains("park", ignoreCase=true) || en.contains("room", ignoreCase=true) || en.contains("school", ignoreCase=true) || en.contains("university", ignoreCase=true) || en.contains("hospital", ignoreCase=true) || en.contains("country", ignoreCase=true) || en.contains("shop", ignoreCase=true) || en.contains("store", ignoreCase=true) || en.contains("house", ignoreCase=true) || en.contains("office", ignoreCase=true)
        if (isPlace) {
            val preposition = if (en.lowercase() in listOf("japan", "china", "vietnam", "london", "tokyo")) "" else "the "
            return ExampleSentence(
                japanese = "わたし は $jp へ 行きます。",
                furigana = "わたしは${fg}へいきます。",
                english = "I go to $preposition$en.",
                bangla = "আমি $bnMeaning-এ যাব।"
            )
        }

        // 8. Food and Drinks
        val isFood = category == "Food" || en.contains("rice", ignoreCase=true) || en.contains("water", ignoreCase=true) || en.contains("tea", ignoreCase=true) || en.contains("coffee", ignoreCase=true) || en.contains("meal", ignoreCase=true) || en.contains("drink", ignoreCase=true) || en.contains("food", ignoreCase=true) || en.contains("apple", ignoreCase=true) || en.contains("fish", ignoreCase=true) || en.contains("meat", ignoreCase=true)
        if (isFood) {
            val isDrink = en.contains("water", ignoreCase=true) || en.contains("tea", ignoreCase=true) || en.contains("coffee", ignoreCase=true) || en.contains("beer", ignoreCase=true) || en.contains("juice", ignoreCase=true) || en.contains("drink", ignoreCase=true) || en.contains("sake", ignoreCase=true) || en.contains("wine", ignoreCase=true) || en.contains("milk", ignoreCase=true)
            return if (isDrink) {
                ExampleSentence(
                    japanese = "$jp を 飲みます。",
                    furigana = "${fg}をのみます。",
                    english = "I drink $en.",
                    bangla = "আমি $bnMeaning পান করি।"
                )
            } else {
                ExampleSentence(
                    japanese = "$jp を 食べます。",
                    furigana = "${fg}をたべます。",
                    english = "I eat $en.",
                    bangla = "আমি $bnMeaning খাই।"
                )
            }
        }

        // 9. People/Jobs
        val isPeople = category == "People" || category == "School" || en.contains("person", ignoreCase=true) || en.contains("people", ignoreCase=true) || en.contains("student", ignoreCase=true) || en.contains("teacher", ignoreCase=true) || en.contains("doctor", ignoreCase=true) || en.contains("employee", ignoreCase=true) || en.contains("worker", ignoreCase=true) || en.contains("friend", ignoreCase=true) || en.contains("child", ignoreCase=true)
        if (isPeople) {
            return ExampleSentence(
                japanese = "あの 人 は $jp です。",
                furigana = "あのひとは${fg}です。",
                english = "That person is a $en.",
                bangla = "ঐ ব্যক্তি একজন $bnMeaning।"
            )
        }

        // 10. General Nouns Fallback
        val cleanEn = en.split(",").first().split("(").first().trim()
        val firstChar = cleanEn.firstOrNull()?.lowercaseChar()
        val article = if (firstChar in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"

        val isUncountable = cleanEn.endsWith("s") && !cleanEn.endsWith("ss") && !cleanEn.endsWith("sh") && !cleanEn.endsWith("ch") ||
            cleanEn in listOf("water", "coffee", "tea", "milk", "beer", "rice", "money", "paper", "light", "electricity", "luggage", "baggage", "furniture", "information", "news")

        val finalEn = if (isUncountable) cleanEn else "$article $cleanEn"

        return ExampleSentence(
            japanese = "これ は $jp です。",
            furigana = "こちらは${fg}です。",
            english = "This is $finalEn.",
            bangla = "এটি $bnMeaning।"
        )
    }
}
