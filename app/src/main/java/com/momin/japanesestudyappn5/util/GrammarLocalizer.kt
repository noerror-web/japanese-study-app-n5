package com.momin.japanesestudyappn5.util

import com.momin.japanesestudyappn5.data.model.GrammarLesson
import com.momin.japanesestudyappn5.data.model.GrammarContentItem

object GrammarLocalizer {
    
    fun cleanBengaliText(input: String): String {
        var text = input
        
        // Match [Bengali (English)] or [Bengali / English]
        val mixedBrackets = Regex("\\[[\\u0980-\\u09FF\\s/,]*\\(([^)]+)\\)[\\u0980-\\u09FF\\s/,]*\\]")
        text = text.replace(mixedBrackets) { matchResult ->
            "[${matchResult.groupValues[1]}]"
        }
        
        // Match (Bengali [English])
        val mixedParentheses = Regex("\\([\\u0980-\\u09FF\\s/,]*\\[([^\\]]+)\\][\\u0980-\\u09FF\\s/,]*\\)")
        text = text.replace(mixedParentheses) { matchResult ->
            "(${matchResult.groupValues[1]})"
        }

        // Remove text inside brackets/parentheses that is purely Bengali
        val bnInsideBrackets = Regex("\\[[\\s\\u0980-\\u09FF/\\-–—,.:;!]*\\]")
        val bnInsideParentheses = Regex("\\([\\s\\u0980-\\u09FF/\\-–—,.:;!]*\\)")
        text = text.replace(bnInsideBrackets, "")
        text = text.replace(bnInsideParentheses, "")
        
        // Remove remaining Bengali characters and the Bengali full stop (।).
        text = text.replace(Regex("[\\u0980-\\u09FF।]+"), "")
        
        // Clean up empty parentheses/brackets, spaces, etc.
        text = text.replace(Regex("\\(\\s*\\)"), "")
        text = text.replace(Regex("\\[\\s*\\]"), "")
        text = text.replace(Regex("\\s+"), " ")
        text = text.replace(Regex("\\s+([,.:;?!)\\]])"), "$1")
        
        return text.trim()
    }

    fun localizeLesson(lesson: GrammarLesson, language: String): GrammarLesson {
        if (language == "bn") return lesson
        
        val newTitle = when (lesson.lesson) {
            0 -> "Japanese Pronunciation Rules"
            1 -> "Grammar Lesson 1: Basic Sentence Structure"
            2 -> "Grammar Lesson 2: Demonstrative Pronouns"
            3 -> "Grammar Lesson 3: Locations & Directions"
            4 -> "Grammar Lesson 4: Time, Verbs & Ranges"
            5 -> "Grammar Lesson 5: Direction Particles & Companions"
            6 -> "Grammar Lesson 6: Direct Objects & Places of Action"
            7 -> "Grammar Lesson 7: Tools, Means & Giving/Receiving"
            8 -> "Grammar Lesson 8: Adjectives (Introduction)"
            9 -> "Grammar Lesson 9: Preferences, Abilities & Reasons"
            10 -> "Grammar Lesson 10: Existence (あります and います)"
            11 -> "Grammar Lesson 11: Counters & Frequency"
            12 -> "Grammar Lesson 12: Past Tense & Comparisons"
            13 -> "Grammar Lesson 13: Desires (欲しい and たい) & Purpose"
            14 -> "Grammar Lesson 14: Te-Form (Part 1)"
            15 -> "Grammar Lesson 15: Te-Form (Part 2)"
            16 -> "Grammar Lesson 16: Te-Form (Part 3) & Linking"
            17 -> "Grammar Lesson 17: Nai-Form & Obligation"
            18 -> "Grammar Lesson 18: Dictionary Form & Ability"
            19 -> "Grammar Lesson 19: Ta-Form, Experience & State Change"
            20 -> "Grammar Lesson 20: Plain Style (Informal Speech)"
            21 -> "Grammar Lesson 21: Opinions, Quotes & Predictions"
            22 -> "Grammar Lesson 22: Noun Modifying Clauses"
            23 -> "Grammar Lesson 23: Conditionals (とき and と)"
            24 -> "Grammar Lesson 24: Giving/Receiving Actions (て-form)"
            25 -> "Grammar Lesson 25: Conditionals (たら and ても)"
            else -> cleanBengaliText(lesson.title)
        }

        val newRules = lesson.rules.map { rule ->
            when {
                rule.contains("সমস্বর দীর্ঘ উচ্চারণ", ignoreCase = true) -> "Rule - 1: Elongation of Vowels"
                rule.contains("এ + ই দীর্ঘ উচ্চারণ", ignoreCase = true) -> "Rule - 2: e + i Rule"
                rule.contains("ও + উ দীর্ঘ উচ্চারণ", ignoreCase = true) -> "Rule - 3: o + u Rule"
                rule.contains("K-গ্রুপের অক্ষরের উচ্চারণ", ignoreCase = true) -> "Rule - 4: K-line Pronunciation"
                rule.contains("tsu এবং ছোট", ignoreCase = true) -> "Rule - 5: Small っ (tsu)"
                rule.contains("ん উচ্চারণ", ignoreCase = true) -> "Rule - 6: ん (n) Sound"
                rule.contains("স্বরবর্ণের অনুচ্চারণ", ignoreCase = true) -> "Rule - 7: Silent Vowels"
                rule.contains("N1 + は + N2 + です", ignoreCase = true) -> "Rule - 1: N1 + は + N2 + です (N1 is N2)"
                rule.contains("N1 + は + N2 +じゃありません", ignoreCase = true) -> "Rule - 2: N1 は N2 じゃありません (N1 is not N2)"
                rule.contains("Subject か", ignoreCase = true) -> "Rule - 3: Question Marker か"
                rule.contains("Noun も", ignoreCase = true) -> "Rule - 4: Noun も (Also/Too)"
                rule.contains("N1 の N2", ignoreCase = true) -> "Rule - 5: N1 の N2 (Possession / Origin)"
                rule.contains("これ / それ / あれ", ignoreCase = true) -> "Rule - 1: これ / それ / あれ (This / That / That over there)"
                rule.contains("この / その / あの", ignoreCase = true) -> "Rule - 2: この / その / あの + Noun"
                rule.contains("そうです / そうじゃありません", ignoreCase = true) -> "Rule - 3: そうです / そうじゃありません"
                rule.contains("Subject か, Subject か", ignoreCase = true) -> "Rule - 4: S1 か, S2 か (Alternative questions)"
                rule.contains("ここ / そこ / あそこ", ignoreCase = true) -> "Rule - 1: ここ / そこ / あそこ / どこ (Locations)"
                rule.contains("こちら / そちら / あちら", ignoreCase = true) -> "Rule - 2: こちら / そちら / あちら / どちら (Directions)"
                rule.contains("N1 は N2 (Place) です", ignoreCase = true) -> "Rule - 3: N1 は N2 (Place) です"
                rule.contains("いま (ইমা)", ignoreCase = true) -> "Rule - 1: Telling Time (~じ / ~ふん)"
                rule.contains("Verb ます", ignoreCase = true) -> "Rule - 2: Verb ます (Polite Form)"
                rule.contains("Noun (time) + に + Verb", ignoreCase = true) -> "Rule - 3: Time Particle に"
                rule.contains("থেকে/হতে", ignoreCase = true) -> "Rule - 4: N1 から N2 まで (From N1 to N2)"
                rule.contains("Noun と(তো) Noun", ignoreCase = true) -> "Rule - 5: N1 と N2 (N1 and N2)"
                rule.contains("へ いきます", ignoreCase = true) -> "Rule - 1: Place へ 行きます/来ます/帰ります (Direction)"
                rule.contains("どこも いきません", ignoreCase = true) -> "Rule - 2: どこ [へ] も 行きません (Not going anywhere)"
                rule.contains("Noun (যানবাহন) で", ignoreCase = true) -> "Rule - 3: Transport Particle で (By vehicle)"
                rule.contains("Noun (ব্যক্তি/প্রাণী) と", ignoreCase = true) -> "Rule - 4: Companion Particle と (With someone)"
                rule.contains("いつ (ইতসু)", ignoreCase = true) -> "Rule - 5: Question word いつ (When)"
                else -> cleanBengaliText(rule)
            }
        }

        val newContent = lesson.content.map { contentItem ->
            val cleanText = cleanBengaliText(contentItem.text)
            
            // Map specific content strings to high-quality English descriptions
            val mappedText = when {
                contentItem.type == "title" && cleanText.contains("Grammar Lesson", ignoreCase = true) -> cleanText
                contentItem.text.contains("জাপানিজ উচ্চারণ", ignoreCase = true) -> "Here are 7 important rules for Japanese pronunciation:"
                contentItem.text.contains("পর পর দুটি একই স্বরবর্ণ", ignoreCase = true) -> "When two identical vowels appear in succession, they are blended into a single prolonged vowel sound."
                contentItem.text.contains("যদি কোনো শব্দের বানানে え", ignoreCase = true) -> "When え (e) is followed by い (i), they are pronounced as a long 'ee' sound."
                contentItem.text.contains("যদি বানানে お", ignoreCase = true) -> "When お (o) is followed by う (u), they are pronounced as a long 'oo' sound."
                contentItem.text.contains("যখন K-গ্রুপের অক্ষরসমূহ", ignoreCase = true) -> "When K-line characters (か, き, く, け, こ) are at the start of a word, they are pronounced with a slight aspiration (like 'kh')."
                contentItem.text.contains("つ এবং ছোট", ignoreCase = true) -> "つ (tsu) is pronounced softly. A small っ (sokuon) doubles the consonant sound that follows it, representing a brief pause."
                contentItem.text.contains("N1 = Subject", ignoreCase = true) -> "N1 is the Subject (the topic of the sentence)."
                contentItem.text.contains("ওয়া = এটা একটা", ignoreCase = true) -> "は (pronounced 'wa') is the topic marker particle. When written as a particle, it is pronounced as 'wa'."
                contentItem.text.contains("N2 = Object", ignoreCase = true) -> "N2 is the Object or copula predicate. です (desu) is the polite ending (is/am/are) and is used to make a sentence polite."
                contentItem.text.contains("যে বাক্যে ক্রিয়া বা কর্ম", ignoreCase = true) -> "In sentences that contain a verb, です (desu) is not used. Verbs end in ます (masu) instead."
                contentItem.text.contains("Present Positive", ignoreCase = true) -> "1. Present Positive: です (is/am/are)"
                contentItem.text.contains("Present Negative", ignoreCase = true) -> "2. Present Negative: じゃありません / ではありません (is not)"
                contentItem.text.contains("Past Positive", ignoreCase = true) -> "3. Past Positive: でした (was/were)"
                contentItem.text.contains("Past Negative", ignoreCase = true) -> "4. Past Negative: じゃありませんでした (was not)"
                contentItem.text.contains("একটি পার্টিকেল। প্রশ্ন করার", ignoreCase = true) -> "か is the question particle. Placed at the end of a sentence, it turns it into a question. It acts like a question mark."
                contentItem.text.contains("সহজ কথায়, কোনো বাক্যকে প্রশ্নবোধক", ignoreCase = true) -> "Simply put, adding か at the end of a sentence makes it a question."
                contentItem.text.contains("N1 এর পরে も", ignoreCase = true) -> "も (mo) means 'also' or 'too'. It replaces the topic marker は when describing a similar state/action."
                contentItem.text.contains("এর নির্দিষ্ট কোন অর্থ নেই", ignoreCase = true) -> "の is the possessive/attributive particle. It connects N1 and N2, meaning N1's N2 or N2 of N1."
                contentItem.text.contains("দূরে অবস্থিত কোনো বস্তুকে", ignoreCase = true) -> "これ (this), それ (that), and あれ (that over there) are demonstrative pronouns."
                contentItem.text.contains("Noun (স্থান) へ", ignoreCase = true) -> "Rule - 1: Noun (Place) へ 行きます/来ます/帰ります"
                contentItem.text.contains("এদের আগে যদি কোন", ignoreCase = true) -> "Particle へ (pronounced 'e') indicates the direction or destination of movement (to/toward)."
                contentItem.text.contains("কোথাও যাইনি বা যাইনা", ignoreCase = true) -> "To say you are not going anywhere (complete negation), use どこ [へ] も + negative verb."
                contentItem.text.contains("যে যানবাহনে করে যাওয়া", ignoreCase = true) -> "Particle で (de) is used after a vehicle to indicate the method of transport (by car, by train, etc.)."
                contentItem.text.contains("যার সাথে যাওয়া আসা", ignoreCase = true) -> "Particle と (to) is used after a person/companion to mean 'with' (with friends, with family)."
                
                // If it is a body explanation, and still has Bengali characters, let's translate or clean it up
                contentItem.type == "body" && contentItem.text.contains("।") -> {
                    // This is usually a translation sentence like "わたし は がくせい です।আমি হই ছাত্র।"
                    val parts = contentItem.text.split("।")
                    if (parts.size >= 2) {
                        val jpPart = parts[0].trim()
                        val bnPart = parts[1].trim()
                        // Look up translation for jpPart
                        val enPart = getEnglishTranslationForJp(jpPart)
                        if (enPart != null) {
                            "$jpPart [$enPart]"
                        } else {
                            // If we don't have it, let's just use cleaned text
                            cleanText
                        }
                    } else {
                        cleanText
                    }
                }
                
                // fallback to cleaned text
                else -> cleanText
            }
            contentItem.copy(text = mappedText)
        }
        
        return lesson.copy(
            title = newTitle,
            rules = newRules,
            content = newContent
        )
    }

    private fun getEnglishTranslationForJp(jp: String): String? {
        val cleanJp = jp.replace(" ", "").replace("　", "").replace("।", "").replace("。", "").trim()
        val translations = mapOf(
            "わたしはマイクミラーです" to "I am Mike Miller",
            "わたしはがくせいです" to "I am a student",
            "わたしはいしゃです" to "I am a doctor",
            "わたしはかいしゃいんです" to "I am a company employee",
            "わたしはごはんをたべます" to "I eat rice",
            "わたしはがくせいじゃありません" to "I am not a student",
            "わたしはがくせいでした" to "I was a student",
            "わたしはがくせいじゃありませんでした" to "I was not a student",
            "ミラーさんはかいしゃいんですか" to "Is Mr. Miller a company employee?",
            "サントスさんもかいしゃいんです" to "Mr. Santos is also a company employee",
            "ミラーさんはIMCのしゃいんです" to "Mr. Miller is an employee of IMC",
            "これはじしょです" to "This is a dictionary",
            "それはわたしのほんです" to "That is my book",
            "あれはだれのカメラですか" to "Whose camera is that over there?",
            "このかばんはわたしのです" to "This bag is mine",
            "おてあらいはあそこです" to "The restroom is over there",
            "でんわはどこですか" to "Where is the telephone?",
            "ぎんこうは９じから３じまでです" to "The bank is from 9 o'clock to 3 o'clock",
            "らいげつきょうとへいきます" to "I am going to Kyoto next month",
            "にほんへきました" to "I came to Japan",
            "うちへかえります" to "I am returning home",
            "どこへもいきません" to "I am not going anywhere",
            "でんしゃでいきます" to "I go by train",
            "ともだちといきます" to "I go with friends",
            "ありがとうございます" to "Thank you very much",
            "どうぞよろしくおねがいします" to "Nice to meet you / Please be kind to me",
            "おはよございます" to "Good morning"
        )
        // Find if any key matches cleanJp
        for ((key, value) in translations) {
            if (cleanJp.contains(key) || key.contains(cleanJp)) {
                return value
            }
        }
        return null
    }
}
