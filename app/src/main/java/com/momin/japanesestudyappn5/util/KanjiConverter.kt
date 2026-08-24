package com.momin.japanesestudyappn5.util

import android.content.Context

object KanjiConverter {

    fun isKanjiDisabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("japanese_study_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("kanji_disabled", false)
    }

    fun hasKanji(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        return text.any { c ->
            c in '\u4E00'..'\u9FAF' || c in '\u3400'..'\u4DBF' || c in '\uF900'..'\uFAFF'
        }
    }

    // Extended mapping of N5/N4 compound words and Kanji characters to Hiragana/Katakana
    private val kanjiToKanaMap: Map<String, String> by lazy {
        mapOf(
            // Compound words (Longest first)
            "私" to "わたし", "僕" to "ぼく", "俺" to "おれ",
            "日本人" to "にほんじん", "日本語" to "にほんご", "日本" to "にほん",
            "学校" to "がっこう", "学生" to "がくせい", "先生" to "せんせい", "大学" to "だいがく", "小学生" to "しょうがくせい", "中学生" to "ちゅうがくせい", "高校生" to "こうこうせい",
            "勉強" to "べんきょう", "友達" to "ともだち", "名前" to "なまえ", "時間" to "じかん",
            "今日" to "きょう", "明日" to "あした", "昨日" to "きのう", "毎日" to "まいにち", "今週" to "こんしゅう", "来週" to "らいしゅう", "先週" to "せんしゅう", "今年" to "ことし", "来年" to "らいねん", "去年" to "きょねん",
            "月曜日" to "げつようび", "火曜日" to "かようび", "水曜日" to "すいようび",
            "木曜日" to "もくようび", "金曜日" to "きんようび", "土曜日" to "どようび", "日曜日" to "にちようび",
            "何" to "なに", "何時" to "なんじ", "何分" to "なんぷん", "何人" to "なにじん", "何曜日" to "なんようび",
            "食べる" to "たべる", "食べます" to "たべます", "食べた" to "たべた",
            "飲む" to "のむ", "飲みます" to "のみます", "飲んだ" to "のんだ",
            "行く" to "いく", "行きます" to "いきます", "行った" to "いった",
            "来る" to "くる", "来ます" to "きます", "来た" to "きた",
            "見る" to "みる", "見ます" to "みます", "見た" to "みた",
            "聞く" to "きく", "聞きます" to "ききます", "聞いた" to "きいた",
            "書く" to "かく", "書きます" to "かきます", "書いた" to "かいた",
            "読む" to "よむ", "読みます" to "よみます", "読んだ" to "よんだ",
            "話す" to "はなす", "話します" to "はなします", "話した" to "はなした",
            "買う" to "かう", "買います" to "かいます", "買った" to "かった",
            "会う" to "あう", "会います" to "あいます", "会った" to "あった",
            "休む" to "やすむ", "休みます" to "やすみます", "休んだ" to "やすんだ",
            "起きる" to "おきる", "起きま" to "おきま", "起きた" to "おきた",
            "寝る" to "ねる", "寝ます" to "ねます", "寝た" to "ねた",
            "教える" to "おしえる", "教えます" to "おしえます", "習う" to "ならう", "習います" to "ならいます",
            "帰る" to "かえる", "帰ります" to "かえります", "入る" to "はいる", "入ります" to "はいります",
            "出かける" to "でかける", "出かけます" to "でかけます", "出る" to "でる", "出ます" to "でます",
            "持つ" to "もつ", "持ちます" to "もちます", "待つ" to "まつ", "待ちます" to "まちます",
            "作る" to "つくる", "作ります" to "つくります", "使う" to "つかう", "使います" to "つかいます",
            "一" to "いち", "二" to "に", "三" to "さん", "四" to "よん", "五" to "ご",
            "六" to "ろく", "七" to "なな", "八" to "はち", "九" to "きゅう", "十" to "じゅう",
            "百" to "ひゃく", "千" to "せん", "万" to "まん", "円" to "えん",
            "一日" to "ついたち", "二日" to "ふつか", "三日" to "みっか", "四日" to "よっか", "五日" to "いつか",
            "六日" to "むいか", "七日" to "なのか", "八日" to "ようか", "九日" to "ここのか", "十日" to "とおか",
            "日" to "ひ", "月" to "つき", "火" to "ひ", "水" to "みず", "木" to "き", "金" to "かね", "土" to "つち",
            "年" to "とし", "時" to "じ", "分" to "ふん", "半" to "はん",
            "人" to "ひと", "男" to "おとこ", "女" to "おんな", "子" to "こ", "男の子" to "おとこのこ", "女の子" to "おんなのこ",
            "父" to "ちち", "母" to "はは", "お父さん" to "おとうさん", "お母さん" to "おかあさん",
            "兄" to "あに", "弟" to "おとうと", "姉" to "あね", "妹" to "いもうと", "お兄さん" to "おにいさん", "お姉さん" to "おねえさん",
            "山" to "やま", "川" to "かわ", "花" to "はな", "雨" to "あめ", "空" to "そら", "天" to "てん", "天気" to "てんき",
            "上" to "うえ", "下" to "した", "中" to "なか", "右" to "みぎ", "左" to "ひだり",
            "前" to "まえ", "後" to "うしろ", "外" to "そと", "間" to "あいだ", "東" to "ひがし", "西" to "にし", "南" to "みなみ", "北" to "きた",
            "大" to "おお", "小" to "ちい", "高" to "たか", "安" to "やす", "新" to "あたら", "古" to "ふる",
            "長" to "なが", "多" to "おお", "少" to "すく", "広" to "ひろ", "白" to "しろ", "赤" to "あか", "青" to "あお", "黒" to "くろ",
            "店" to "みせ", "駅" to "えき", "車" to "くるま", "電" to "でん", "電車" to "でんしゃ", "電気" to "でんき",
            "国" to "くに", "道" to "みち", "社" to "しゃ", "会社" to "かいしゃ", "校" to "こう",
            "語" to "ご", "本" to "ほん", "文" to "ぶん", "字" to "じ", "漢字" to "かんじ", "図" to "と", "館" to "かん", "図書館" to "としょかん",
            "目" to "め", "耳" to "みみ", "手" to "て", "足" to "あし", "口" to "くち", "心" to "こころ", "力" to "ちから",
            "犬" to "いぬ", "猫" to "ねこ", "魚" to "さかな", "鳥" to "とり", "肉" to "にく", "物" to "もの", "食べ物" to "たべもの", "飲み物" to "のみもの", "買い物" to "かいもの",
            "部屋" to "へや", "本屋" to "ほんや", "パン屋" to "ぱんや", "花屋" to "はなや", "病院" to "びょういん", "病気" to "びょうき",
            "旅行" to "りょこう", "写真" to "しゃしん", "映画" to "えいが", "映画館" to "えいがかん", "音楽" to "おんがく", "歌" to "うた",
            "仕事" to "しごと", "授業" to "じゅぎょう", "宿題" to "しゅくだい", "試験" to "しけん", "問題" to "もんだい", "質問" to "しつもん",
            "朝" to "あさ", "昼" to "ひる", "夜" to "よる", "晩" to "ばん", "今晩" to "こんばん", "毎朝" to "まいあさ", "毎晩" to "まいばん"
        )
    }

    private val sortedKeys: List<String> by lazy {
        kanjiToKanaMap.keys.sortedByDescending { it.length }
    }

    /**
     * Converts a Japanese string containing Kanji into Hiragana/Katakana.
     * If furigana is provided and is valid Kana, uses furigana directly.
     */
    fun toKana(text: String?, furigana: String? = null): String {
        if (text.isNullOrBlank()) return ""
        if (!furigana.isNullOrBlank() && !hasKanji(furigana)) {
            return furigana
        }
        if (!hasKanji(text)) return text

        var result: String = text
        for (key in sortedKeys) {
            if (result.contains(key)) {
                result = result.replace(key, kanjiToKanaMap[key] ?: key)
            }
        }
        return result
    }
}
