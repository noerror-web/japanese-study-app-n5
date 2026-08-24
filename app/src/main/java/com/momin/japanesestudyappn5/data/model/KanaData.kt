package com.momin.japanesestudyappn5.data.model

data class KanaItem(
    val char: String,
    val romaji: String,
    val type: String
)

object KanaData {
    val hiraganaBasic = listOf(
        KanaItem("あ", "a", "basic"), KanaItem("い", "i", "basic"), KanaItem("う", "u", "basic"), KanaItem("え", "e", "basic"), KanaItem("お", "o", "basic"),
        KanaItem("か", "ka", "basic"), KanaItem("き", "ki", "basic"), KanaItem("く", "ku", "basic"), KanaItem("け", "ke", "basic"), KanaItem("こ", "ko", "basic"),
        KanaItem("さ", "sa", "basic"), KanaItem("し", "shi", "basic"), KanaItem("す", "su", "basic"), KanaItem("せ", "se", "basic"), KanaItem("そ", "so", "basic"),
        KanaItem("た", "ta", "basic"), KanaItem("ち", "chi", "basic"), KanaItem("つ", "tsu", "basic"), KanaItem("て", "te", "basic"), KanaItem("と", "to", "basic"),
        KanaItem("な", "na", "basic"), KanaItem("に", "ni", "basic"), KanaItem("ぬ", "nu", "basic"), KanaItem("ね", "ne", "basic"), KanaItem("の", "no", "basic"),
        KanaItem("は", "ha", "basic"), KanaItem("ひ", "hi", "basic"), KanaItem("ふ", "fu", "basic"), KanaItem("へ", "he", "basic"), KanaItem("ほ", "ho", "basic"),
        KanaItem("ま", "ma", "basic"), KanaItem("み", "mi", "basic"), KanaItem("む", "mu", "basic"), KanaItem("め", "me", "basic"), KanaItem("も", "mo", "basic"),
        KanaItem("や", "ya", "basic"), null, KanaItem("ゆ", "yu", "basic"), null, KanaItem("よ", "yo", "basic"),
        KanaItem("ら", "ra", "basic"), KanaItem("り", "ri", "basic"), KanaItem("る", "ru", "basic"), KanaItem("れ", "re", "basic"), KanaItem("ろ", "ro", "basic"),
        KanaItem("わ", "wa", "basic"), null, null, null, KanaItem("を", "wo", "basic"),
        KanaItem("ん", "n", "basic")
    )

    val hiraganaDakuten = listOf(
        KanaItem("が", "ga", "dakuten"), KanaItem("ぎ", "gi", "dakuten"), KanaItem("ぐ", "gu", "dakuten"), KanaItem("げ", "ge", "dakuten"), KanaItem("ご", "go", "dakuten"),
        KanaItem("ざ", "za", "dakuten"), KanaItem("じ", "ji", "dakuten"), KanaItem("ず", "zu", "dakuten"), KanaItem("ぜ", "ze", "dakuten"), KanaItem("ぞ", "zo", "dakuten"),
        KanaItem("だ", "da", "dakuten"), KanaItem("ぢ", "ji", "dakuten"), KanaItem("づ", "zu", "dakuten"), KanaItem("で", "de", "dakuten"), KanaItem("ど", "do", "dakuten"),
        KanaItem("ば", "ba", "dakuten"), KanaItem("び", "bi", "dakuten"), KanaItem("ぶ", "bu", "dakuten"), KanaItem("べ", "be", "dakuten"), KanaItem("ぼ", "bo", "dakuten")
    )

    val hiraganaHandakuten = listOf(
        KanaItem("ぱ", "pa", "handakuten"), KanaItem("ぴ", "pi", "handakuten"), KanaItem("ぷ", "pu", "handakuten"), KanaItem("ぺ", "pe", "handakuten"), KanaItem("ぽ", "po", "handakuten")
    )

    val hiraganaCombination = listOf(
        KanaItem("きゃ", "kya", "combination"), KanaItem("きゅ", "kyu", "combination"), KanaItem("きょ", "kyo", "combination"),
        KanaItem("ぎゃ", "gya", "combination"), KanaItem("ぎゅ", "gyu", "combination"), KanaItem("ぎょ", "gyo", "combination"),
        KanaItem("しゃ", "sha", "combination"), KanaItem("しゅ", "shu", "combination"), KanaItem("しょ", "sho", "combination"),
        KanaItem("じゃ", "ja", "combination"), KanaItem("じゅ", "ju", "combination"), KanaItem("じょ", "jo", "combination"),
        KanaItem("ちゃ", "cha", "combination"), KanaItem("ちゅ", "chu", "combination"), KanaItem("ちょ", "cho", "combination"),
        KanaItem("にゃ", "nya", "combination"), KanaItem("にゅ", "nyu", "combination"), KanaItem("にょ", "nyo", "combination"),
        KanaItem("ひゃ", "hya", "combination"), KanaItem("ひゅ", "hyu", "combination"), KanaItem("ひょ", "hyo", "combination"),
        KanaItem("びゃ", "bya", "combination"), KanaItem("びゅ", "byu", "combination"), KanaItem("びょ", "byo", "combination"),
        KanaItem("ぴゃ", "pya", "combination"), KanaItem("ぴゅ", "pyu", "combination"), KanaItem("ぴょ", "pyo", "combination"),
        KanaItem("みゃ", "mya", "combination"), KanaItem("みゅ", "myu", "combination"), KanaItem("みょ", "myo", "combination"),
        KanaItem("りゃ", "rya", "combination"), KanaItem("りゅ", "ryu", "combination"), KanaItem("りょ", "ryo", "combination")
    )

    val katakanaBasic = listOf(
        KanaItem("ア", "a", "basic"), KanaItem("イ", "i", "basic"), KanaItem("ウ", "u", "basic"), KanaItem("エ", "e", "basic"), KanaItem("オ", "o", "basic"),
        KanaItem("カ", "ka", "basic"), KanaItem("キ", "ki", "basic"), KanaItem("ク", "ku", "basic"), KanaItem("ケ", "ke", "basic"), KanaItem("コ", "ko", "basic"),
        KanaItem("サ", "sa", "basic"), KanaItem("シ", "shi", "basic"), KanaItem("ス", "su", "basic"), KanaItem("セ", "se", "basic"), KanaItem("ソ", "so", "basic"),
        KanaItem("タ", "ta", "basic"), KanaItem("チ", "chi", "basic"), KanaItem("ツ", "tsu", "basic"), KanaItem("テ", "te", "basic"), KanaItem("ト", "to", "basic"),
        KanaItem("ナ", "na", "basic"), KanaItem("ニ", "ni", "basic"), KanaItem("ヌ", "nu", "basic"), KanaItem("ネ", "ne", "basic"), KanaItem("ノ", "no", "basic"),
        KanaItem("ハ", "ha", "basic"), KanaItem("ヒ", "hi", "basic"), KanaItem("フ", "fu", "basic"), KanaItem("ヘ", "he", "basic"), KanaItem("ホ", "ho", "basic"),
        KanaItem("マ", "ma", "basic"), KanaItem("ミ", "mi", "basic"), KanaItem("ム", "mu", "basic"), KanaItem("メ", "me", "basic"), KanaItem("モ", "mo", "basic"),
        KanaItem("ヤ", "ya", "basic"), null, KanaItem("ユ", "yu", "basic"), null, KanaItem("ヨ", "yo", "basic"),
        KanaItem("ラ", "ra", "basic"), KanaItem("リ", "ri", "basic"), KanaItem("ル", "ru", "basic"), KanaItem("レ", "re", "basic"), KanaItem("ロ", "ro", "basic"),
        KanaItem("ワ", "wa", "basic"), null, null, null, KanaItem("ヲ", "wo", "basic"),
        KanaItem("ン", "n", "basic")
    )

    val katakanaDakuten = listOf(
        KanaItem("ガ", "ga", "dakuten"), KanaItem("ギ", "gi", "dakuten"), KanaItem("グ", "gu", "dakuten"), KanaItem("ゲ", "ge", "dakuten"), KanaItem("ゴ", "go", "dakuten"),
        KanaItem("ザ", "za", "dakuten"), KanaItem("ジ", "ji", "dakuten"), KanaItem("ズ", "zu", "dakuten"), KanaItem("ゼ", "ze", "dakuten"), KanaItem("ゾ", "zo", "dakuten"),
        KanaItem("ダ", "da", "dakuten"), KanaItem("ヂ", "ji", "dakuten"), KanaItem("ヅ", "zu", "dakuten"), KanaItem("デ", "de", "dakuten"), KanaItem("ド", "do", "dakuten"),
        KanaItem("バ", "ba", "dakuten"), KanaItem("ビ", "bi", "dakuten"), KanaItem("ブ", "bu", "dakuten"), KanaItem("ベ", "be", "dakuten"), KanaItem("ボ", "bo", "dakuten")
    )

    val katakanaHandakuten = listOf(
        KanaItem("パ", "pa", "handakuten"), KanaItem("ピ", "pi", "handakuten"), KanaItem("プ", "pu", "handakuten"), KanaItem("ペ", "pe", "handakuten"), KanaItem("ポ", "po", "handakuten")
    )

    val katakanaCombination = listOf(
        KanaItem("キャ", "kya", "combination"), KanaItem("キュ", "kyu", "combination"), KanaItem("キョ", "kyo", "combination"),
        KanaItem("ギャ", "gya", "combination"), KanaItem("ギュ", "gyu", "combination"), KanaItem("ギョ", "gyo", "combination"),
        KanaItem("シャ", "sha", "combination"), KanaItem("シュ", "shu", "combination"), KanaItem("ショ", "sho", "combination"),
        KanaItem("ジャ", "ja", "combination"), KanaItem("ジュ", "ju", "combination"), KanaItem("ジョ", "jo", "combination"),
        KanaItem("チャ", "cha", "combination"), KanaItem("チュ", "chu", "combination"), KanaItem("チョ", "cho", "combination"),
        KanaItem("ニャ", "nya", "combination"), KanaItem("ニュ", "nyu", "combination"), KanaItem("ニョ", "nyo", "combination"),
        KanaItem("ヒャ", "hya", "combination"), KanaItem("ヒュ", "hyu", "combination"), KanaItem("ヒョ", "hyo", "combination"),
        KanaItem("ビャ", "bya", "combination"), KanaItem("ビュ", "byu", "combination"), KanaItem("ビョ", "byo", "combination"),
        KanaItem("ピャ", "pya", "combination"), KanaItem("ピュ", "pyu", "combination"), KanaItem("ピョ", "pyo", "combination"),
        KanaItem("ミャ", "mya", "combination"), KanaItem("ミュ", "myu", "combination"), KanaItem("ミョ", "myo", "combination"),
        KanaItem("リャ", "rya", "combination"), KanaItem("リュ", "ryu", "combination"), KanaItem("リョ", "ryo", "combination")
    )
}
