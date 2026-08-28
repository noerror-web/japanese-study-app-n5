package com.momin.japanesestudyappn5.util

import android.util.Log
import com.momin.japanesestudyappn5.data.model.ExampleSentence
import com.momin.japanesestudyappn5.ui.screens.ReadingPassage
import com.momin.japanesestudyappn5.ui.screens.ReadingQuestion
import com.momin.japanesestudyappn5.ui.screens.QuizQuestion
import com.momin.japanesestudyappn5.data.model.VocabItem
import com.momin.japanesestudyappn5.ui.screens.ChatScenario
import com.momin.japanesestudyappn5.ui.screens.ParticleQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class FillBlankQuestion(
    val sentence: String,
    val furigana: String,
    val english: String,
    val options: List<String>,
    val correctIndex: Int,
    val bangla: String? = null
)

object AIGenerator {

    private suspend fun callGemini(apiKey: String, prompt: String): String? = withContext(Dispatchers.IO) {
        val resolvedApiKey = apiKey.trim()
        if (resolvedApiKey.isEmpty()) {
            throw Exception("Gemini API key is empty.")
        }
        val maxRetries = 3
        var attempt = 0
        var lastException: Exception? = null
        while (attempt < maxRetries) {
            var conn: HttpURLConnection? = null
            try {
                val modelName = when (attempt) {
                    0 -> "gemini-3.6-flash"
                    1 -> "gemini-2.5-flash"
                    else -> "gemini-2.5-pro"
                }
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$resolvedApiKey")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("x-goog-api-key", resolvedApiKey)
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true
                conn.doInput = true

                val requestBody = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.7)
                    })
                }

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(requestBody.toString())
                writer.flush()
                writer.close()
                os.close()

                val responseCode = conn.responseCode
                Log.d("AIGenerator", "Response Code ($modelName): $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val stream = conn.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotEmpty()) return@withContext text
                        }
                    }
                    throw Exception("Invalid response format from Gemini API")
                } else {
                    val stream = conn.errorStream ?: conn.inputStream
                    val errorText = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("AIGenerator", "Error code ($modelName): $responseCode, body: $errorText")
                    
                    if (responseCode == 429) {
                        Log.w("AIGenerator", "Rate limited (429). Waiting 5 seconds before retry...")
                        kotlinx.coroutines.delay(5000L)
                    }
                    
                    val parsedMessage = if (errorText.isNotEmpty()) {
                        try {
                            JSONObject(errorText).getJSONArray("errors").getJSONObject(0).getString("message")
                        } catch (e1: Exception) {
                            try {
                                JSONObject(errorText).getJSONObject("error").getString("message")
                            } catch (e2: Exception) {
                                errorText
                            }
                        }
                    } else {
                        "Unknown API error"
                    }
                    if (responseCode == 429) {
                        throw Exception("Gemini API quota limit reached. Please try again in a few moments or enter your personal API key in Settings.")
                    }
                    throw Exception(parsedMessage)
                }
            } catch (e: Exception) {
                Log.e("AIGenerator", "Attempt ${attempt + 1} failed: ${e.message}")
                lastException = e
                if (attempt >= maxRetries - 1) {
                    throw e
                }
            } finally {
                conn?.disconnect()
            }
            attempt++
            if (attempt < maxRetries) {
                Log.d("AIGenerator", "Retrying Gemini request in 1 second... (Attempt ${attempt + 1}/$maxRetries)")
                kotlinx.coroutines.delay(1000L)
            }
        }
        throw lastException ?: Exception("Unknown error calling Gemini API.")
    }

    private fun cleanModelResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```")) {
            val firstLineBreak = clean.indexOf('\n')
            if (firstLineBreak != -1) {
                clean = clean.substring(firstLineBreak).trim()
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length - 3).trim()
            }
        }
        return clean
    }

    private suspend fun cleanCallGemini(apiKey: String, prompt: String): String? {
        val raw = callGemini(apiKey, prompt) ?: return null
        return cleanModelResponse(raw)
    }

    suspend fun generateReadingPassage(apiKey: String, topic: String = "", kanjiDisabled: Boolean = false): ReadingPassage? {
        val topicHint = if (topic.isNotEmpty()) "The story topic should be about: $topic." else "Choose any everyday N5 topic."
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in title, content, or options. Write ALL Japanese text ENTIRELY in Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Generate a short Japanese reading practice story suitable for JLPT N5 learners.
            $topicHint$noKanjiInstruction
            CRITICAL REQUIREMENT: To provide comprehensive reading practice, the story MUST actively include several words using:
            1. Dakuten (e.g. が, じ, だ, ば)
            2. Handakuten (e.g. ぱ, ぴ, ぷ, ぺ, ぽ)
            3. Yoon / combination sounds (e.g. しゃ, しゅ, しょ, ちゃ, じゅ, りょ, ぴょ)
            Ensure these characters are used naturally in standard N5 vocabulary (e.g. りょこう, てんぷら, しゅくだい, じゅぎょう, しゃしん, さんぽ, きっぷ, びょういん, ひゃく, etc.).

            The response MUST be a single JSON object with the following structure:
            {
              "title": "A short simple title in Japanese",
              "content": "A story written in ${if (kanjiDisabled) "Hiragana and Katakana only (NO Kanji)" else "a mix of Hiragana, Katakana, and simple N5 Kanji"}.",
              "furigana": "The story with all Kanji replaced by Hiragana (preserving Katakana), fully matching the content.",
              "translation": "English translation of the story.",
              "questions": [
                {
                  "question": "Comprehension question in English (based on the story)",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 0
                },
                {
                  "question": "Second comprehension question in English",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 1
                },
                {
                  "question": "Third comprehension question in English",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctIndex": 2
                }
              ]
            }
            Do not wrap the JSON in markdown code blocks. Make sure the content is highly engaging and clean.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val json = JSONObject(responseText)
            val rawTitle = json.getString("title")
            val rawContent = json.getString("content")
            val rawFurigana = json.getString("furigana")
            val translation = json.getString("translation")
            val questionsJson = json.getJSONArray("questions")
            val questionsList = mutableListOf<ReadingQuestion>()
            for (i in 0 until questionsJson.length()) {
                val qJson = questionsJson.getJSONObject(i)
                val question = qJson.getString("question")
                val optionsJson = qJson.getJSONArray("options")
                val rawOptions = List(optionsJson.length()) { idx ->
                    val opt = optionsJson.getString(idx)
                    if (kanjiDisabled) KanjiConverter.toKana(opt) else opt
                }
                val rawCorrectIndex = qJson.getInt("correctIndex")
                val correctOption = rawOptions.getOrNull(rawCorrectIndex) ?: rawOptions.firstOrNull() ?: ""
                val shuffledOptions = rawOptions.shuffled()
                val newCorrectIndex = shuffledOptions.indexOf(correctOption).coerceAtLeast(0)
                questionsList.add(ReadingQuestion(question, shuffledOptions, newCorrectIndex))
            }
            val title = if (kanjiDisabled) KanjiConverter.toKana(rawTitle, rawFurigana) else rawTitle
            val content = if (kanjiDisabled) rawFurigana.ifBlank { KanjiConverter.toKana(rawContent) } else rawContent
            val furigana = if (kanjiDisabled) KanjiConverter.toKana(rawFurigana) else rawFurigana
            ReadingPassage(title, content, furigana, translation, questionsList)
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse ReadingPassage", e)
            null
        }
    }

    suspend fun generateSentences(apiKey: String, count: Int = 10, kanjiDisabled: Boolean = false): List<ExampleSentence>? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji! Write ALL Japanese sentences ENTIRELY in Hiragana and Katakana (Kana only). Zero Kanji!" else ""
        val prompt = """
            Generate a list of $count random Japanese sentences suitable for JLPT N5 learners.
            $noKanjiInstruction
            CRITICAL: Each Japanese sentence MUST contain space characters separating Japanese words and grammatical particles, so it can be split into individual tiles for a sentence building game.
            For example: "わたし は にほんご を べんきょう します".
            The response MUST be a JSON array of objects with the following structure:
            [
              {
                "japanese": "Japanese sentence with spaces separating words and particles",
                "furigana": "Furigana pronunciation string (without spaces)",
                "english": "English translation of the sentence"
              }
            ]
            Do not wrap the JSON in markdown code blocks. Make sure they are simple, diverse, and clear.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val array = JSONArray(responseText)
            val list = mutableListOf<ExampleSentence>()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val rawJapanese = json.getString("japanese")
                val furigana = json.getString("furigana")
                val english = json.getString("english")
                val japanese = if (kanjiDisabled) KanjiConverter.toKana(rawJapanese, furigana) else rawJapanese
                list.add(ExampleSentence(japanese, furigana, english))
            }
            list
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse ExampleSentences list", e)
            null
        }
    }

    suspend fun generateVocabQuiz(apiKey: String, count: Int, mode: String, kanjiDisabled: Boolean = false): List<QuizQuestion>? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji! Write ALL Japanese words ENTIRELY in Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Generate a list of $count vocabulary quiz questions suitable for JLPT N5 level.
            $noKanjiInstruction
            The response MUST be a JSON array of objects with the following structure:
            [
              {
                "japanese": "Target Japanese word",
                "furigana": "Furigana of target word",
                "english": "English meaning of target word",
                "options": [
                  {
                    "japanese": "Word Option 1",
                    "furigana": "Furigana Option 1",
                    "english": "English Option 1"
                  },
                  {
                    "japanese": "Word Option 2",
                    "furigana": "Furigana Option 2",
                    "english": "English Option 2"
                  },
                  {
                    "japanese": "Word Option 3",
                    "furigana": "Furigana Option 3",
                    "english": "English Option 3"
                  },
                  {
                    "japanese": "Word Option 4",
                    "furigana": "Furigana Option 4",
                    "english": "English Option 4"
                  }
                ],
                "correctIndex": 0
              }
            ]
            Make sure one of the options matches the target word exactly, and correctIndex points to it.
            The direction of the quiz is ${if (mode == "jp_to_en") "Japanese word to English translation" else "English meaning to Japanese word"}. Make sure all options are plausible N5 vocabulary words so it is challenging but fair.
            Do not wrap the JSON in markdown code blocks.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val array = JSONArray(responseText)
            val result = mutableListOf<QuizQuestion>()
            for (i in 0 until array.length()) {
                val qJson = array.getJSONObject(i)
                val rawJapanese = qJson.getString("japanese")
                val furigana = qJson.getString("furigana")
                val english = qJson.getString("english")
                val correctIndex = qJson.getInt("correctIndex")

                val targetJp = if (kanjiDisabled) furigana.ifBlank { KanjiConverter.toKana(rawJapanese) } else rawJapanese
                val targetItem = createDummyVocabItem(targetJp, furigana, english)

                val optionsJson = qJson.getJSONArray("options")
                val optionsList = mutableListOf<VocabItem>()
                for (j in 0 until optionsJson.length()) {
                    val opt = optionsJson.getJSONObject(j)
                    val optJpRaw = opt.getString("japanese")
                    val optFuri = opt.getString("furigana")
                    val optJp = if (kanjiDisabled) optFuri.ifBlank { KanjiConverter.toKana(optJpRaw) } else optJpRaw
                    optionsList.add(createDummyVocabItem(
                        optJp,
                        optFuri,
                        opt.getString("english")
                    ))
                }

                val correctItem = optionsList.getOrNull(correctIndex) ?: targetItem
                val shuffledOptions = optionsList.shuffled()
                val newCorrectIndex = shuffledOptions.indexOf(correctItem).coerceAtLeast(0)

                result.add(QuizQuestion(targetItem, shuffledOptions, newCorrectIndex, mode))
            }
            result
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse QuizQuestions list", e)
            null
        }
    }

    suspend fun generateFillBlankSentences(apiKey: String, count: Int = 8, kanjiDisabled: Boolean = false): List<FillBlankQuestion>? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji! Write ALL sentences and options ENTIRELY in Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Generate $count fill-in-the-blank Japanese sentences for JLPT N5 learners.
            $noKanjiInstruction
            Each sentence has one key word blanked out. Provide 4 options where exactly one is correct.
            The response MUST be a JSON array:
            [
              {
                "sentence": "Full sentence in Japanese with blank word replaced by ___",
                "furigana": "Full sentence furigana with blank replaced by ___",
                "english": "English translation showing the correct answer in [brackets]",
                "options": ["option1_japanese", "option2_japanese", "option3_japanese", "option4_japanese"],
                "correctIndex": 0
              }
            ]
            Keep sentences simple: N5 grammar and vocabulary only. Do not wrap in markdown code blocks.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val array = JSONArray(responseText)
            val list = mutableListOf<FillBlankQuestion>()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val rawSentence = json.getString("sentence")
                val furigana = json.getString("furigana")
                val english = json.getString("english")
                val optionsJson = json.getJSONArray("options")
                val rawOptions = List(optionsJson.length()) { idx ->
                    val opt = optionsJson.getString(idx)
                    if (kanjiDisabled) KanjiConverter.toKana(opt) else opt
                }
                val rawCorrectIndex = json.getInt("correctIndex")
                val correctOption = rawOptions.getOrNull(rawCorrectIndex) ?: rawOptions.firstOrNull() ?: ""
                val shuffledOptions = rawOptions.shuffled()
                val newCorrectIndex = shuffledOptions.indexOf(correctOption).coerceAtLeast(0)
                val sentence = if (kanjiDisabled) furigana.ifBlank { KanjiConverter.toKana(rawSentence) } else rawSentence
                list.add(FillBlankQuestion(sentence, furigana, english, shuffledOptions, newCorrectIndex))
            }
            list
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse FillBlankQuestions", e)
            null
        }
    }

    suspend fun generateChatResponse(
        apiKey: String,
        conversationHistory: List<Pair<String, String>>,
        userMessage: String,
        scenarioPrompt: String = "",
        kanjiDisabled: Boolean = false
    ): String? {
        val historyText = conversationHistory.takeLast(6).joinToString("\n") { (role, msg) ->
            "${if (role == "user") "Student" else "Tutor"}: $msg"
        }
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in your Japanese parts! Write ALL Japanese words ENTIRELY in Hiragana and Katakana (Kana only). Do NOT put Kanji in parentheses; just write pure Kana." else ""
        val prompt = """
            You are a friendly Japanese language tutor for JLPT N5 beginners.
            ${if (scenarioPrompt.isNotEmpty()) "SCENARIO INSTRUCTION:\n$scenarioPrompt" else ""}$noKanjiInstruction
            RULES:
            - Only use N5-level vocabulary and grammar in your Japanese parts.
            - ${if (kanjiDisabled) "Write Japanese parts ENTIRELY in Hiragana/Katakana." else "Always show Japanese with furigana in parentheses, then English translation on the next line."}
            - Keep responses SHORT (1-3 sentences max).
            - Be encouraging and fun. Use emoji occasionally.
            - If the student writes in English, respond in both Japanese and English.
            - If the student writes in Japanese, gently correct mistakes and continue.
            
            Conversation so far:
            $historyText
            
            Student: $userMessage
            
            Respond as Tutor. Return JSON: {"reply": "your response here"}
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val json = JSONObject(responseText)
            val reply = json.getString("reply")
            if (kanjiDisabled) KanjiConverter.toKana(reply) else reply
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse chat response", e)
            null
        }
    }

    private fun createDummyVocabItem(japanese: String, furigana: String, english: String): VocabItem {
        return VocabItem(
            audioId = "generated_" + System.nanoTime(),
            audioText = japanese,
            japanese = japanese,
            furigana = furigana,
            romaji = "",
            english = english,
            bangla = "",
            lesson = null,
            lessonOrder = null,
            sectionKey = "ai_generated",
            sectionLabel = "AI Generated",
            source = "Gemini AI",
            extraUseful = false
        )
    }

    suspend fun generateCustomScenario(apiKey: String, topic: String, kanjiDisabled: Boolean = false): ChatScenario? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in initialMessage, systemPrompt, or goalMatches. Write ALL Japanese text ENTIRELY in Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Create a custom JLPT N5 Japanese language practice chat roleplay scenario based on the topic: "$topic".
            $noKanjiInstruction
            The student will chat in character with an AI partner who acts as a clerk, friend, doctor, or helpful stranger.
            
            Provide exactly 3 conversation goals in English for the student to achieve (e.g., "Order ramen using 'ください'", "Ask price using 'いくらですか'").
            For each goal, provide a regex pattern to match user input (Hiragana, Kanji, or Romaji).
            
            The response MUST be a single JSON object with the following structure:
            {
              "title": "Short scenario title (e.g. Order Ramen)",
              "icon": "One emoji representing the theme (e.g. 🍜)",
              "description": "Short description of the roleplay in English (1 sentence)",
              "initialMessage": "The AI partner's first sentence in Japanese (${if (kanjiDisabled) "in Hiragana/Katakana only" else "with Furigana in parentheses, e.g. こんにちは！"}) and English translation on the next line.",
              "systemPrompt": "System character instructions for the AI partner. Guide the student to achieve the goals politely.",
              "goals": ["Goal 1 text", "Goal 2 text", "Goal 3 text"],
              "goalMatches": ["regex_pattern_1", "regex_pattern_2", "regex_pattern_3"]
            }
            Do not wrap the JSON in markdown code blocks. Keep patterns simple and match keywords (e.g. "ください|kudasai", "いくら|ikura").
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val json = JSONObject(responseText)
            val title = json.getString("title")
            val icon = json.getString("icon")
            val description = json.getString("description")
            val rawInitial = json.getString("initialMessage")
            val systemPrompt = json.getString("systemPrompt")
            
            val goalsJson = json.getJSONArray("goals")
            val goals = List(goalsJson.length()) { goalsJson.getString(it) }
            
            val matchesJson = json.getJSONArray("goalMatches")
            val goalMatches = List(matchesJson.length()) { Regex(matchesJson.getString(it), RegexOption.IGNORE_CASE) }
            
            val initialMessage = if (kanjiDisabled) KanjiConverter.toKana(rawInitial) else rawInitial

            ChatScenario(
                id = "custom_" + System.currentTimeMillis(),
                title = title,
                icon = icon,
                description = description,
                initialMessage = initialMessage,
                systemPrompt = systemPrompt,
                goals = goals,
                goalMatches = goalMatches
            )
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse custom scenario", e)
            null
        }
    }

    suspend fun explainSentenceStructure(apiKey: String, sentence: String, appLanguage: String, kanjiDisabled: Boolean = false): String? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in Japanese words or readings. Use Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Perform a structured N5-level grammatical breakdown of the following Japanese sentence: "$sentence".
            $noKanjiInstruction
            You must return a JSON object with the following keys:
            - "translation": Natural translation of the sentence in ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.
            - "breakdown": A list of words and particles in the sentence in sequential order. Each item in the list must have:
              - "word": The Japanese word or particle.
              - "reading": Hiragana/Katakana reading.
              - "definition": Short meaning in ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.
              - "role": The grammatical function or explanation in ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.
            - "grammarNote": A brief explanation of the overall sentence structure or N5 grammar patterns used in ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.
            
            Example JSON output:
            {
              "translation": "I am a student.",
              "breakdown": [
                {"word": "わたし", "reading": "わたし", "definition": "I", "role": "Pronoun (Subject)"},
                {"word": "は", "reading": "は", "definition": "Topic marker", "role": "Particle marking the topic"},
                {"word": "がくせい", "reading": "がくせい", "definition": "student", "role": "Noun (Predicate)"},
                {"word": "です", "reading": "です", "definition": "to be", "role": "Polite copula verb"}
              ],
              "grammarNote": "This uses the 'A は B です' (A is B) pattern where 'わたし' is the topic and 'がくせい' is the identity."
            }
            
            Return ONLY the raw JSON object. Do not wrap in markdown blocks or HTML tags.
        """.trimIndent()

        return try {
            val res = cleanCallGemini(apiKey, prompt) ?: return null
            if (kanjiDisabled) {
                try {
                    val jsonObj = JSONObject(res)
                    if (jsonObj.has("breakdown")) {
                        val arr = jsonObj.getJSONArray("breakdown")
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            if (item.has("word")) {
                                val w = item.getString("word")
                                val r = item.optString("reading", "")
                                item.put("word", KanjiConverter.toKana(w, r))
                            }
                        }
                    }
                    jsonObj.toString()
                } catch (pe: Exception) {
                    KanjiConverter.toKana(res)
                }
            } else res
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to get sentence explanation", e)
            val errorMsg = e.message ?: "Unknown error"
            try {
                JSONObject().apply {
                    put("error", errorMsg)
                }.toString()
            } catch (je: Exception) {
                "{\"error\": \"$errorMsg\"}"
            }
        }
    }

    suspend fun defineWord(apiKey: String, word: String, appLanguage: String, kanjiDisabled: Boolean = false): String? {
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in Japanese text or readings. Use Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Provide a definition, reading (romaji/furigana), and translation for the Japanese word: "$word".
            Make the translation in ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.$noKanjiInstruction
            Keep the response short, N5 level, and format it nicely in 1-2 lines.
            Return a JSON object: {"definition": "your brief definition/translation here"}
            Do not wrap in markdown code blocks.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val json = org.json.JSONObject(responseText)
            val def = json.getString("definition")
            if (kanjiDisabled) KanjiConverter.toKana(def) else def
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse word definition", e)
            null
        }
    }

    suspend fun generateCustomParticleQuestions(apiKey: String, topic: String, count: Int = 5, kanjiDisabled: Boolean = false): List<ParticleQuestion>? {
        val topicHint = if (topic.isNotEmpty()) "The exercises must be on the topic: $topic." else "Choose general everyday N5 topics."
        val noKanjiInstruction = if (kanjiDisabled) "\nCRITICAL KANJI RULE: Do NOT use ANY Kanji in sentenceBefore, sentenceAfter, or options. Write ALL Japanese text ENTIRELY in Hiragana and Katakana (Kana only)." else ""
        val prompt = """
            Generate $count Japanese particle matching exercise questions suitable for JLPT N5 learners.
            $topicHint$noKanjiInstruction
            Each question must have a sentence split around a single target particle (the blank).
            The response MUST be a JSON array of objects with the following structure:
            [
              {
                "sentenceBefore": "The part of the Japanese sentence BEFORE the particle blank",
                "sentenceAfter": "The part of the Japanese sentence AFTER the particle blank",
                "correctParticle": "The correct N5 particle that fits in the blank (e.g. は, が, を, に, で, と, の, も)",
                "translation": "English translation of the full sentence",
                "bangla": "Bengali translation of the full sentence",
                "explanation": "Brief grammatical explanation of why this particle is correct (1-2 sentences)",
                "options": ["correct_particle", "option2", "option3", "option4"]
              }
            ]
            RULES:
            - Make sure correctParticle is exactly one of the options.
            - The options list must have exactly 4 items.
            - Keep grammar and vocabulary strictly at JLPT N5 level.
            - Do not wrap the JSON in markdown code blocks.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val array = JSONArray(responseText)
            val list = mutableListOf<ParticleQuestion>()
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                val rawBefore = json.getString("sentenceBefore")
                val rawAfter = json.getString("sentenceAfter")
                val correctParticle = json.getString("correctParticle").trim()
                val translation = json.getString("translation")
                val bangla = json.getString("bangla")
                val explanation = json.getString("explanation")
                val optionsJson = json.getJSONArray("options")
                val options = List(optionsJson.length()) { idx ->
                    val opt = optionsJson.getString(idx).trim()
                    if (kanjiDisabled) KanjiConverter.toKana(opt) else opt
                }.shuffled()
                val sentenceBefore = if (kanjiDisabled) KanjiConverter.toKana(rawBefore) else rawBefore
                val sentenceAfter = if (kanjiDisabled) KanjiConverter.toKana(rawAfter) else rawAfter
                list.add(ParticleQuestion(sentenceBefore, sentenceAfter, correctParticle, translation, bangla, explanation, options))
            }
            list
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse Custom ParticleQuestions", e)
            null
        }
    }

    suspend fun translateText(
        apiKey: String,
        text: String,
        appLanguage: String = "en",
        kanjiDisabled: Boolean = false
    ): TranslationResult? {
        val prompt = """
            You are an expert English, Bengali, and Japanese translator specialized in JLPT N5 level translation.
            Translate the following input text: "$text".
            
            TRANSLATION RULES:
            1. If the input text is in English or Bengali:
               - Translate it into Japanese.
               - CRITICAL KANJI RULE: Write the Japanese translation ENTIRELY in Hiragana and Katakana (NO KANJI!). Absolutely ZERO Kanji characters!
               - Provide furigana (Hiragana reading), romaji (pronunciation), English translation, Bengali translation, and brief grammar/vocabulary notes.
               - Set "detectedSource": "English / Bangla".
            2. If the input text is in Japanese:
               - Set "detectedSource": "Japanese".
               - Translate it into ${if (appLanguage == "bn") "Bengali (বাংলা)" else "English"}.
               - Provide Hiragana reading for the input Japanese, Romaji, English translation, Bengali translation, and brief grammar/vocabulary notes.
               - Set "translatedText" to the ${if (appLanguage == "bn") "Bengali" else "English"} translation.

            Return a single raw JSON object matching this structure:
            {
              "detectedSource": "English / Bangla" or "Japanese",
              "translatedText": "Main translated text output",
              "furigana": "Japanese text reading in Hiragana/Katakana",
              "romaji": "Romanized pronunciation",
              "english": "English translation",
              "bangla": "Bengali translation (বাংলায় অনুবাদ)",
              "notes": "Brief educational note on vocabulary or pattern used"
            }
            
            Do not wrap in markdown code blocks.
        """.trimIndent()

        return try {
            val responseText = cleanCallGemini(apiKey, prompt) ?: return null
            val json = JSONObject(responseText)
            val detectedSource = json.optString("detectedSource", "Auto-detected")
            val rawTranslatedText = json.optString("translatedText", "")
            val furigana = json.optString("furigana", "")
            val romaji = json.optString("romaji", "")
            val english = json.optString("english", "")
            val bangla = json.optString("bangla", "")
            val notes = json.optString("notes", "")

            val translatedText = if (detectedSource.contains("English") || detectedSource.contains("Bangla")) {
                KanjiConverter.toKana(rawTranslatedText, furigana)
            } else rawTranslatedText

            TranslationResult(
                originalText = text,
                detectedSource = detectedSource,
                translatedText = translatedText,
                furigana = KanjiConverter.toKana(furigana),
                romaji = romaji,
                english = english,
                bangla = bangla,
                notes = notes
            )
        } catch (e: Exception) {
            Log.e("AIGenerator", "Failed to parse TranslationResult", e)
            null
        }
    }
}

data class TranslationResult(
    val originalText: String,
    val detectedSource: String,
    val translatedText: String,
    val furigana: String = "",
    val romaji: String = "",
    val english: String = "",
    val bangla: String = "",
    val notes: String = ""
)

