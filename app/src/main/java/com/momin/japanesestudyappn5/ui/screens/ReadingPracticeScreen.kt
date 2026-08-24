package com.momin.japanesestudyappn5.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.momin.japanesestudyappn5.util.AudioPlayer
import kotlinx.coroutines.launch
import com.momin.japanesestudyappn5.data.DataRepository
import com.momin.japanesestudyappn5.data.model.VocabItem
import androidx.compose.foundation.layout.FlowRow

data class ReadingPassage(
    val title: String,
    val content: String,
    val furigana: String,
    val translation: String,
    val questions: List<ReadingQuestion>
)

data class ReadingQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

enum class KanaDisplayMode {
    MIXED,
    HIRAGANA,
    KATAKANA
}

fun String.katakanaToHiragana(): String {
    return this.map { char ->
        if (char in '\u30a1'..'\u30f6') {
            (char.code - 0x60).toChar()
        } else {
            char
        }
    }.joinToString("")
}

fun String.hiraganaToKatakana(): String {
    return this.map { char ->
        if (char in '\u3041'..'\u3096') {
            (char.code + 0x60).toChar()
        } else {
            char
        }
    }.joinToString("")
}

@Composable
fun ToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

private val PASSAGES = listOf(
    ReadingPassage(
        title = "私の家族",
        content = "私の 家族は 四人です。 父、 母、 お姉さんと 私です。 父は 会社員で、 毎朝 電車で 会社へ 行きます。 母は 小学校の 先生です。 お姉さんは 京都の 大学生で、 天ぷらが 大好きです。 私は 高校生で、 よく デパートで パンを 買います。",
        furigana = "わたしのかぞくは よにんです。 ちち、 はは、 おねえさんと わたしです。 ちちは かいしゃいんで、 まいあさ でんしゃで かいしゃへ いきます。 ははは しょうがっこうの せんせいです。 おねえさんは きょうとの だいがくせいであって、 てんぷらが だいすきです。 わたしは こうこうせいで、 よく デパートで パンを かいます。",
        translation = "My family has four people. It's my father, mother, older sister, and me. My father is a company employee and goes to work by train every morning. My mother is an elementary school teacher. My older sister is a university student in Kyoto and loves tempura. I am a high school student and often buy bread at the department store.",
        questions = listOf(
            ReadingQuestion("How many people are in the family?", listOf("3 people", "4 people", "5 people", "2 people"), 1),
            ReadingQuestion("What does the mother do?", listOf("Company employee", "Student", "Teacher", "Doctor"), 2),
            ReadingQuestion("What is the writer?", listOf("University student", "Teacher", "High school student", "Company employee"), 2)
        )
    ),
    ReadingPassage(
        title = "毎日の生活",
        content = "私は 毎朝 六時に 起きます。 シャワーを 浴びて、 温かい お茶と パンを 食べます。 七時半に 家を出て、 バスで 学校へ 行きます。 授業は 八時半に 始まります。 放課後は 図書館で 宿題を します。",
        furigana = "わたしは まいあさ ろくじに おきます。 シャワーを あびて、 あたたかい おちゃと パンを たべます。 しちじはんに おうちを でて、 バスで がっこうへ いきます。 じゅぎょうは はちじはんに はじまります。 ほうかごは としょかんで しゅくだいを します。",
        translation = "I wake up at 6 o'clock every morning. I take a shower and eat warm green tea and bread. I leave home at 7:30 and go to school by bus. Classes start at 8:30. After school, I do my homework in the library.",
        questions = listOf(
            ReadingQuestion("What time does the writer wake up?", listOf("5 AM", "6 AM", "7 AM", "8 AM"), 1),
            ReadingQuestion("How does the writer go to school?", listOf("By car", "By bus", "By train", "On foot"), 1),
            ReadingQuestion("What time do classes start?", listOf("7:00", "7:30", "8:00", "8:30"), 3)
        )
    ),
    ReadingPassage(
        title = "好きな食べ物",
        content = "私は 食べることが 大好きです。 特に 寿司と 天ぷらが 好きです。 毎週 土曜日に、 家族と一緒に 和食の レストランへ 行きます。 辛い 料理は 苦手ですが、 甘い ケーキや 果物は たくさん 食べます。",
        furigana = "わたしは たべることが だいすきです。 とくに すしと てんぷらが すきです。 まいしゅう どようびに、 かぞくと いっしょに わしょくの レストランへ いきます。 からい りょうりは にがてですが、 あまい ケーキや くだものは たくさん たべます。",
        translation = "I love eating. In particular, I love sushi and tempura. Every Saturday, I go to a Japanese restaurant with my family. I am not good with spicy food, but I eat a lot of sweet cakes and fruits.",
        questions = listOf(
            ReadingQuestion("What does the writer love most?", listOf("Ramen and noodles", "Sushi and tempura", "Meat and vegetables", "Sweet food"), 1),
            ReadingQuestion("What food does the writer NOT like?", listOf("Sushi", "Ramen", "Spicy food", "Vegetables"), 2),
            ReadingQuestion("What does the writer eat rarely?", listOf("Meat", "Vegetables", "Ramen", "Sweet food"), 3)
        )
    ),
    ReadingPassage(
        title = "日本の旅行",
        content = "私は 去年の 秋に 日本へ 旅行しました。 京都と 東京へ 行って、 古い お寺を 見ました。 京都は とても 静かで 綺麗でした。 東京は 賑やかで 面白かったです。 デパートで 絵葉書と お土産の コップを たくさん 買いました。",
        furigana = "わたしは きょねんの あきに にほんへ りょこうしました。 きょうとと とうきょうへ いって、 ふるい おてらを みました。 きょうとは とても しずかで きれいでした。 とうきょうは にぎやかで おもしろかったです。 デパートで えはがきと おみやげの コップを たくさん かいました。",
        translation = "I traveled to Japan last autumn. I went to Kyoto and Tokyo and saw old temples. Kyoto was very quiet and beautiful. Tokyo was lively and interesting. I bought many postcards and souvenir cups at the department store.",
        questions = listOf(
            ReadingQuestion("When did the writer go to Japan?", listOf("Yesterday", "Last week", "Last month", "Last year"), 3),
            ReadingQuestion("How was Kyoto?", listOf("Lively", "Cold", "Quiet and beautiful", "Hot"), 2),
            ReadingQuestion("What did the writer buy a lot of?", listOf("Books", "Cups and Postcards", "Clothes", "Food"), 1)
        )
    ),
    ReadingPassage(
        title = "友達の誕生日",
        content = "今日は 親友の 誕生日です。 プレゼントに 綺麗な 青い シャツと 可愛い 切符を 買いました。 夜は 賑やかな 中華料理の レストランで パーティーを します。 皆で 一緒に 美味しい ジュースを 飲みます。",
        furigana = "きょうは しんゆうの たんじょうびです。 プレゼントに きれいな あおい シャツと かわいい きっぷを かいました。 よるは にぎやかな ちゅうかりょうりの レストランで パーティーを します。 みんなで いっしょに おいしい ジュースを のみます。",
        translation = "Today is my close friend's birthday. I bought a beautiful blue shirt and a cute ticket as a present. In the evening, we will have a party at a lively Chinese restaurant. We will all drink delicious juice together.",
        questions = listOf(
            ReadingQuestion("Whose birthday is today?", listOf("The writer's", "My teacher's", "My close friend's", "My sister's"), 2),
            ReadingQuestion("What present will the writer give?", listOf("A book and pen", "Blue shirt and ticket", "Clothes and shoes", "A watch"), 1),
            ReadingQuestion("Where will they eat dinner?", listOf("At school", "At home", "At a Chinese restaurant", "At a park"), 2)
        )
    ),
    ReadingPassage(
        title = "図書館の勉強",
        content = "私は 毎週 水曜日に 図書館へ 行って 勉強します。 静かな 部屋で 日本語の 文法や 漢字を 練習します。 辞書を 使って 短い 小説を 読みます。 疲れた 時は、 近くの 喫茶店で 美味しい コーヒーと プリンを 食べます。",
        furigana = "わたしは まいしゅう すいようびに としょかんへ いって べんきょうします。 しずかな へやで にほんごの ぶんぽうや かんじを れんしゅうします。 じしょを つかって みじかい しょうせつを よみます。 つかれた ときは、 ちかくの きっさてんで おいしい コーヒーと プリンを たべます。",
        translation = "I go to the library to study every Wednesday. I practice Japanese grammar and kanji in a quiet room. I read short stories using a dictionary. When I'm tired, I eat delicious coffee and pudding at a nearby cafe.",
        questions = listOf(
            ReadingQuestion("When does the writer go to the library?", listOf("On Mondays", "On Wednesdays", "On Fridays", "On Saturdays"), 1),
            ReadingQuestion("What time does the library close?", listOf("6 PM", "7 PM", "8 PM", "9 PM"), 2),
            ReadingQuestion("What does the writer study there?", listOf("English", "Maths", "Japanese grammar and Kanji", "History"), 2)
        )
    ),
    ReadingPassage(
        title = "私の趣味",
        content = "私の 趣味は カメラで 写真を 撮ることです。 日曜日に 公園に 行って、 珍しい 鳥や 花の 写真を たくさん 撮ります。 時々、 高い 山に 登って 美味しい 風景を 撮影します。 将来は、 日本へ 行って 写真の 発表会を 開きたいです。",
        furigana = "わたしの しゅみは カメラで しゃしんを とることです。 にちようびに こうえんに いって、 めずらしい とりや はなの しゃしんを たくさん とります。 ときどき、 たかい やまに のぼって うつくしい ふうけいを さつえいします。 しょうらいは、 にほんへ いって しゃしんの はっぴょうかいを ひらきたいです。",
        translation = "My hobby is taking photos with a camera. On Sundays, I go to the park and take many photos of rare birds and flowers. Sometimes I climb high mountains and photograph beautiful scenery. In the future, I want to go to Japan and hold a photo exhibition.",
        questions = listOf(
            ReadingQuestion("What is the writer's hobby?", listOf("Reading books", "Taking photos", "Climbing mountains", "Swimming"), 1),
            ReadingQuestion("Where does the writer go with a camera?", listOf("To a hospital", "To a school", "To a park", "To a station"), 2),
            ReadingQuestion("What does the writer take photos of?", listOf("Cars and trains", "Rare birds and flowers", "People and buildings", "Food"), 1)
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingPracticeScreen(
    onBack: () -> Unit,
    repository: DataRepository
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE) }
    val apiKey = prefs.getString("gemini_api_key", "") ?: ""
    val coroutineScope = rememberCoroutineScope()

    var allVocab by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var lookupWord by remember { mutableStateOf<String?>(null) }
    var lookupResults by remember { mutableStateOf<List<VocabItem>>(emptyList()) }
    var lookupAiResult by remember { mutableStateOf<String?>(null) }
    var isSearchingDefinition by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        allVocab = repository.getVocabulary()
    }

    var selectedPassageIndex by remember { mutableStateOf(-1) }
    var selectedAnswers by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showResults by remember { mutableStateOf(false) }

    var aiGeneratedPassage by remember { mutableStateOf<ReadingPassage?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTopic by remember { mutableStateOf("") }

    // Toggle states for display
    var includeKanji by remember { mutableStateOf(true) }
    var kanaDisplayMode by remember { mutableStateOf(KanaDisplayMode.MIXED) }

    val topics = listOf("🍜 Food", "🏫 School", "✈️ Travel", "👨‍👩‍👧 Family", "🛍️ Shopping", "☁️ Weather")

    fun awardXP(points: Int) {
        val current = prefs.getInt("xp_total", 0)
        prefs.edit().putInt("xp_total", current + points).apply()
        recordDailyXp(prefs, points)
    }

    val passage = when (selectedPassageIndex) {
        -2 -> aiGeneratedPassage
        in 0..PASSAGES.lastIndex -> PASSAGES[selectedPassageIndex]
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (passage == null) "📖 Reading Practice" else "📖 ${passage.title}",
                            fontWeight = FontWeight.Bold
                        )
                        if (passage != null && !showResults) {
                            Text("N5 Level", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (passage != null) {
                            selectedPassageIndex = -1
                            selectedAnswers = emptyMap()
                            showResults = false
                        } else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        if (passage == null) {
            // Passage selection list
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Choose a reading passage:",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) {},
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Text("✨", fontSize = 26.sp)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("💡 Generate AI Story", fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Fresh N5 passage with comprehension questions",
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.8f))
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            // Topic picker
                            Text("Choose a topic (optional):", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f),
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            topics.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                                    row.forEach { topic ->
                                        val isSelected = selectedTopic == topic
                                        Surface(
                                            onClick = { selectedTopic = if (isSelected) "" else topic },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(topic, fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                                textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    coroutineScope.launch {
                                        val topicName = selectedTopic.replace(Regex("[^\\w ]"), "").trim()
                                        val isKanjiOff = com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context)
                                        val generated = com.momin.japanesestudyappn5.util.AIGenerator.generateReadingPassage(apiKey, topicName, kanjiDisabled = isKanjiOff)
                                        isLoading = false
                                        if (generated != null) {
                                            aiGeneratedPassage = generated
                                            selectedPassageIndex = -2
                                            selectedAnswers = emptyMap()
                                            showResults = false
                                        } else {
                                            android.widget.Toast.makeText(context, "AI Generation failed. Check key/internet.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("✨  Generate Story${if (selectedTopic.isNotEmpty()) " — $selectedTopic" else ""}",
                                    fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                itemsIndexed(PASSAGES) { index, p ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { selectedPassageIndex = index },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${index + 1}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("${p.questions.size} comprehension questions",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(4.dp))
                                Text(p.content.take(60) + "…",
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        } else {
            val isKanjiOff = remember { com.momin.japanesestudyappn5.util.KanjiConverter.isKanjiDisabled(context) }
            val rawBaseText = if (isKanjiOff) passage.furigana.ifBlank { com.momin.japanesestudyappn5.util.KanjiConverter.toKana(passage.content) } else if (includeKanji) passage.content else passage.furigana
            val baseText = if (isKanjiOff) com.momin.japanesestudyappn5.util.KanjiConverter.toKana(rawBaseText) else rawBaseText
            val renderedText = remember(baseText, kanaDisplayMode) {
                when (kanaDisplayMode) {
                    KanaDisplayMode.HIRAGANA -> baseText.katakanaToHiragana()
                    KanaDisplayMode.KATAKANA -> baseText.hiraganaToKatakana()
                    KanaDisplayMode.MIXED -> baseText
                }
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Passage
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1565C0)
                                    ) {
                                        Text(" N5 ", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("Reading Passage", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                val context = LocalContext.current
                                IconButton(
                                    onClick = {
                                        AudioPlayer.ensureTts(context)
                                        AudioPlayer.speakJapanese(renderedText)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Text("🔊", fontSize = 18.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))

                            // Display preference toggles
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToggleChip(
                                    selected = includeKanji,
                                    onClick = { includeKanji = !includeKanji },
                                    label = "漢字 Kanji"
                                )
                                ToggleChip(
                                    selected = kanaDisplayMode == KanaDisplayMode.MIXED,
                                    onClick = { kanaDisplayMode = KanaDisplayMode.MIXED },
                                    label = "Mixed"
                                )
                                ToggleChip(
                                    selected = kanaDisplayMode == KanaDisplayMode.HIRAGANA,
                                    onClick = { kanaDisplayMode = KanaDisplayMode.HIRAGANA },
                                    label = "ひらがな Hira"
                                )
                                ToggleChip(
                                    selected = kanaDisplayMode == KanaDisplayMode.KATAKANA,
                                    onClick = { kanaDisplayMode = KanaDisplayMode.KATAKANA },
                                    label = "カタカナ Kata"
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            TextFlow(
                                text = renderedText,
                                onWordClick = { clickedWord ->
                                    lookupWord = clickedWord
                                    val cleaned = clickedWord.trim().replace(Regex("[。、？！，.!?]"), "")
                                    val cleanedHiragana = cleaned.katakanaToHiragana()
                                    val cleanedKatakana = cleaned.hiraganaToKatakana()
                                    val matches = allVocab.filter { item ->
                                        val jp = item.japanese.trim()
                                        val fg = item.furigana.trim()
                                        val jpHiragana = jp.katakanaToHiragana()
                                        val fgHiragana = fg.katakanaToHiragana()
                                        (jp.isNotEmpty() && (cleaned.contains(jp) || cleanedHiragana.contains(jpHiragana) || cleanedKatakana.contains(jp))) ||
                                        (fg.isNotEmpty() && (cleaned.contains(fg) || cleanedHiragana.contains(fgHiragana) || cleanedKatakana.contains(fg)))
                                    }
                                    lookupResults = matches
                                    
                                    if (matches.isEmpty() && cleaned.isNotEmpty()) {
                                        isSearchingDefinition = true
                                        lookupAiResult = null
                                        coroutineScope.launch {
                                            val definition = com.momin.japanesestudyappn5.util.AIGenerator.defineWord(
                                                apiKey,
                                                cleaned,
                                                prefs.getString("app_language", "en") ?: "en"
                                            )
                                            lookupAiResult = definition
                                            isSearchingDefinition = false
                                        }
                                    } else {
                                        lookupAiResult = null
                                        isSearchingDefinition = false
                                    }
                                }
                            )
                        }
                    }
                }

                // Translation (expandable)
                item {
                    var showTranslation by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { showTranslation = !showTranslation },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (showTranslation) "Hide Translation ▲" else "Show Translation ▼")
                    }
                    if (showTranslation) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                        ) {
                            Text(passage.translation, Modifier.padding(16.dp),
                                fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF5D4037))
                        }
                    }
                }

                // Questions
                item {
                    Text("Comprehension Questions:", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        modifier = Modifier.padding(top = 8.dp))
                }

                itemsIndexed(passage.questions) { qIndex, question ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("${qIndex + 1}. ${question.question}",
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Spacer(Modifier.height(10.dp))
                            question.options.forEachIndexed { optIndex, opt ->
                                val isSelected = selectedAnswers[qIndex] == optIndex
                                val isCorrect = showResults && optIndex == question.correctIndex
                                val isWrong = showResults && isSelected && optIndex != question.correctIndex
                                val bgColor = when {
                                    isCorrect -> Color(0xFF43A047)
                                    isWrong -> Color(0xFFEF5350)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val textColor = when {
                                    isCorrect || isWrong || isSelected -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !showResults) {
                                            selectedAnswers = selectedAnswers + (qIndex to optIndex)
                                            if (optIndex == question.correctIndex)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = bgColor
                                ) {
                                    Text("${('A' + optIndex)}. $opt",
                                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        color = textColor, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Submit button
                item {
                    val correct = passage.questions.indices.count {
                        selectedAnswers[it] == passage.questions[it].correctIndex
                    }
                    if (!showResults) {
                        Button(
                            onClick = {
                                showResults = true
                                awardXP(correct * 3 + 10)
                                saveQuizScore(prefs, "Reading", correct, passage.questions.size)
                            },
                            enabled = selectedAnswers.size == passage.questions.size,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Submit Answers", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (correct == passage.questions.size) Color(0xFFE8F5E9) else Color(0xFFFFF8E1)
                            )
                        ) {
                            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (correct == passage.questions.size) "🎉 Perfect Score!" else "📚 Score: $correct / ${passage.questions.size}",
                                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                                    color = if (correct == passage.questions.size) Color(0xFF2E7D32) else Color(0xFF5D4037)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { selectedPassageIndex = -1; selectedAnswers = emptyMap(); showResults = false },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Choose Another Passage") }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (lookupWord != null) {
        AlertDialog(
            onDismissRequest = {
                lookupWord = null
                lookupResults = emptyList()
                lookupAiResult = null
            },
            title = {
                Text("🔍 Word Translation", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Clicked: \"$lookupWord\"",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider()
                    
                    if (lookupResults.isNotEmpty()) {
                        Text("Offline Database matches:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        lookupResults.take(3).forEach { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.japanese, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        if (item.furigana != item.japanese) {
                                            Text(" (${item.furigana})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                    val appLang = prefs.getString("app_language", "en") ?: "en"
                                    Text(
                                        text = if (appLang == "bn" && item.bangla.isNotEmpty()) item.bangla else item.english,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (isSearchingDefinition) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Fetching live AI definition...", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    } else if (lookupAiResult != null) {
                        Text("Live AI Translation:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Text(
                                text = lookupAiResult ?: "",
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    } else {
                        Text("No translation found. Check key/internet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    lookupWord = null
                    lookupResults = emptyList()
                    lookupAiResult = null
                }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextFlow(
    text: String,
    onWordClick: (String) -> Unit
) {
    val words = remember(text) { text.split(" ") }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.Center
    ) {
        words.forEach { word ->
            val cleaned = word.trim().replace(Regex("[。、？！，.!?]"), "")
            Text(
                text = word,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onWordClick(cleaned) }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}
