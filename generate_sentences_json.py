import json
import re
import sys

# Reconfigure stdout to use UTF-8
sys.stdout.reconfigure(encoding='utf-8')

txt_path = r"C:\Users\Administrator\Videos\vocabulary_example_sentences.txt"
json_path = r"c:\Users\Administrator\Music\japanese app\app\src\main\assets\anki_vocab_data.json"
output_path = r"c:\Users\Administrator\Music\japanese app\app\src\main\assets\sentences.json"

# Hiragana to Katakana converter
def hira_to_kata(text):
    return "".join(chr(ord(c) + 96) if 0x3041 <= ord(c) <= 0x3096 else c for c in text)

# Normalize Japanese text for comparison
def normalize_jp(text):
    if not text:
        return ""
    text = text.replace('~', '').replace('～', '').replace('[', '').replace(']', '')
    text = text.replace('(', '').replace(')', '').replace(' ', '').replace('　', '')
    text = text.replace('。', '').replace('、', '').replace('?', '').replace('？', '')
    text = text.replace('.', '').replace('!', '').replace('！', '').replace('*', '')
    text = text.replace('/', '').replace('／', '').replace('·', '')
    return text.strip()

# Normalize English text for comparison
def normalize_en(text):
    if not text:
        return ""
    text = text.lower()
    text = re.sub(r'[^a-z0-9]', '', text)
    return text

with open(json_path, 'r', encoding='utf-8') as f:
    vocab_list = json.load(f)

# Read the file line by line to keep track of lessons
with open(txt_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

current_lesson = 0
parsed_blocks = [] # List of dict: {word, meaning, lesson, ex1_jp, ex1_en, ex2_jp, ex2_en}

i = 0
while i < len(lines):
    line = lines[i].strip()
    
    # Check for lesson headers
    lesson_match = re.search(r'Lesson (\d+) Vocabulary', line)
    if lesson_match:
        current_lesson = int(lesson_match.group(1))
        i += 1
        continue
    
    # Check for vocabulary word blocks
    word_match = re.match(r'^【([^】]+)】(.*)', line)
    if word_match:
        word = word_match.group(1).strip()
        meaning = word_match.group(2).strip()
        
        # Read the examples
        ex1_jp = ""
        ex1_en = ""
        ex2_jp = ""
        ex2_en = ""
        
        # Look ahead for ex1 and ex2
        j = i + 1
        while j < len(lines):
            next_line = lines[j].strip()
            # If we hit another block or lesson header, stop
            if next_line.startswith('【') or 'Lesson' in next_line or '===' in next_line:
                break
            
            if next_line.startswith('1.'):
                ex1_jp = next_line.replace('1.', '').strip()
                # next line should be English
                if j + 1 < len(lines):
                    ex1_en = lines[j+1].strip()
                    j += 1
            elif next_line.startswith('2.'):
                ex2_jp = next_line.replace('2.', '').strip()
                # next line should be English
                if j + 1 < len(lines):
                    ex2_en = lines[j+1].strip()
                    j += 1
            j += 1
        
        parsed_blocks.append({
            'word': word,
            'meaning': meaning,
            'lesson': current_lesson,
            'ex1_jp': ex1_jp,
            'ex1_en': ex1_en,
            'ex2_jp': ex2_jp,
            'ex2_en': ex2_en,
            'line_num': i + 1
        })
        i = j
        continue
    i += 1

# Group JSON vocab by lesson
json_by_lesson = {}
for item in vocab_list:
    les = item.get('lesson')
    if les is not None:
        json_by_lesson.setdefault(les, []).append(item)

# Match parsed blocks to json items
matched_audio_ids = set()
output_map = {} # audioId -> list of ExampleSentence

for block in parsed_blocks:
    # Match globally because vocabulary_example_sentences.txt doesn't have lesson headers for lessons 2-25
    lesson_items = vocab_list
    block_word_norm = normalize_jp(block['word'])
    block_word_norm_kata = normalize_jp(hira_to_kata(block['word']))
    block_meaning_norm = normalize_en(block['meaning'])
    
    best_match = None
    best_score = 0
    
    for item in lesson_items:
        item_jp_norm = normalize_jp(item['japanese'])
        item_fg_norm = normalize_jp(item['furigana'])
        item_at_norm = normalize_jp(item['audioText'])
        
        # Scoring match
        score = 0
        if block_word_norm and block_word_norm in [item_jp_norm, item_fg_norm, item_at_norm]:
            score = 10
        elif block_word_norm_kata and block_word_norm_kata in [item_jp_norm, item_fg_norm, item_at_norm]:
            score = 9
        elif item_jp_norm and (item_jp_norm in block_word_norm or block_word_norm in item_jp_norm):
            score = 5
        elif item_fg_norm and (item_fg_norm in block_word_norm or block_word_norm in item_fg_norm):
            score = 5
            
        # Check meaning overlap
        item_en_norm = normalize_en(item['english'])
        if block_meaning_norm and block_meaning_norm == item_en_norm:
            score += 5
        elif block_meaning_norm and item_en_norm and (block_meaning_norm in item_en_norm or item_en_norm in block_meaning_norm):
            score += 2
            
        if score > best_score:
            best_score = score
            best_match = item
            
    if best_match and best_score >= 9:
        audio_id = best_match['audioId']
        matched_audio_ids.add(audio_id)
        output_map[audio_id] = [
            {
                'japanese': block['ex1_jp'],
                'furigana': block['ex1_jp'].replace(' ', ''),
                'english': block['ex1_en'],
                'bangla': best_match.get('bangla') or None
            },
            {
                'japanese': block['ex2_jp'],
                'furigana': block['ex2_jp'].replace(' ', ''),
                'english': block['ex2_en'],
                'bangla': best_match.get('bangla') or None
            }
        ]

print(f"Matched {len(matched_audio_ids)} out of {len(vocab_list)} items.")

unmatched_items = [item for item in vocab_list if item['audioId'] not in matched_audio_ids]
print(f"Unmatched items: {len(unmatched_items)}")

# We will define custom fallback examples for all unmatched items
for item in unmatched_items:
    audio_id = item['audioId']
    jp = item['japanese']
    fg = item['furigana'] or jp
    en = item['english']
    bn = item.get('bangla')
    lesson = item['lesson']
    
    # Simple defaults
    ex1_jp = f"{fg}は なんですか。"
    ex1_en = f"What is {en}?"
    ex2_jp = f"これは {fg}です。"
    ex2_en = f"This is {en}."
    
    # Custom high quality rules
    # Countries
    if en.lower() in ["america/united states", "america"]:
        ex1_jp = "あめりかから きました。"
        ex1_en = "I came from America."
        ex2_jp = "あめりかは おおきいです。"
        ex2_en = "America is big."
    elif en.lower() in ["united kingdom", "uk"]:
        ex1_jp = "いぎりすから きました。"
        ex1_en = "I came from the UK."
        ex2_jp = "いぎりすは ふるい くにです。"
        ex2_en = "The UK is an old country."
    elif en.lower() in ["india"]:
        ex1_jp = "いんどから きました。"
        ex1_en = "I came from India."
        ex2_jp = "いんどは あついです。"
        ex2_en = "India is hot."
    elif en.lower() in ["indonesia"]:
        ex1_jp = "いんどねしあから きました。"
        ex1_en = "I came from Indonesia."
        ex2_jp = "いんどねしあは あついです。"
        ex2_en = "Indonesia is hot."
    elif en.lower() in ["thailand"]:
        ex1_jp = "たいから きました。"
        ex1_en = "I came from Thailand."
        ex2_jp = "たいは あついです。"
        ex2_en = "Thailand is hot."
    elif en.lower() in ["germany"]:
        ex1_jp = "どいつから きました。"
        ex1_en = "I came from Germany."
        ex2_jp = "どいつは さむいです。"
        ex2_en = "Germany is cold."
    elif en.lower() in ["bangladesh"]:
        ex1_jp = "ばんぐらでしゅから きました。"
        ex1_en = "I came from Bangladesh."
        ex2_jp = "ばんぐらでしゅは あついです。"
        ex2_en = "Bangladesh is hot."
    elif en.lower() in ["brazil"]:
        ex1_jp = "ぶらじるから きました。"
        ex1_en = "I came from Brazil."
        ex2_jp = "ぶらじるは とおいです。"
        ex2_en = "Brazil is far."
    elif "fictional company" in en.lower() or "fictional organization" in en.lower():
        ex1_jp = f"わたしは {fg}の しゃいんです。"
        ex1_en = "I am an employee of the company."
        ex2_jp = f"{fg}は おおきいです。"
        ex2_en = "The company is big."
    elif "fictional hospital" in en.lower():
        ex1_jp = "こうべびょういんに いきます。"
        ex1_en = "I go to Kobe Hospital."
        ex2_jp = "こうべびょういんは おおきいです。"
        ex2_en = "Kobe Hospital is big."
    elif "fictional university" in en.lower():
        ex1_jp = "さくらだいがくに いきます。"
        ex1_en = "I go to Sakura University."
        ex2_jp = "さくらだいがくは おおきいです。"
        ex2_en = "Sakura University is big."
    elif "please; here you go" in en.lower() or jp == "どうぞ。":
        ex1_jp = "おちゃを どうぞ。"
        ex1_en = "Here is some tea."
        ex2_jp = "どうぞ おはいり ください。"
        ex2_en = "Please come in."
    elif "is that so" in en.lower() or jp == "そうですか。":
        ex1_jp = "そうですか。わかりました。"
        ex1_en = "Is that so? I understand."
        ex2_jp = "そうですか。それは よかったです。"
        ex2_en = "Is that so? That's good."
    elif "you are mistaken" in en.lower() or jp == "違います。":
        ex1_jp = "いいえ、ちがいます。"
        ex1_en = "No, that's wrong."
        ex2_jp = "それは ちがいます。"
        ex2_en = "That is wrong."
    elif "look forward to" in en.lower() or jp == "これから お世話に なります。":
        ex1_jp = "はじめまして。これから おせわに なります。"
        ex1_en = "Nice to meet you. I look forward to working with you."
        ex2_jp = "どうぞ よろしく おねがいします。"
        ex2_en = "Thank you in advance."
    elif "pleasure is mine" in en.lower() or jp == "こちらこそ [どうぞ] よろしく [お願いします] 。":
        ex1_jp = "こちらこそ よろしく おねがいします。"
        ex1_en = "The pleasure is mine."
        ex2_jp = "こちらこそ どうぞ よろしく。"
        ex2_en = "Likewise, pleased to meet you."
    elif "floor" in en.lower() or jp == "–階":
        ex1_jp = "きょうしつは さんがいに あります。"
        ex1_en = "The classroom is on the 3rd floor."
        ex2_jp = "しょくどうは にかいに あります。"
        ex2_en = "The cafeteria is on the 2nd floor."
    elif "yen" in en.lower() or jp == "–円":
        ex1_jp = "これは ひゃくえんです。"
        ex1_en = "This is 100 yen."
        ex2_jp = "ぜんぶで さんぜんえんです。"
        ex2_en = "It is 3000 yen in total."
    elif "counter for hours" in en.lower() or jp == "ー時":
        ex1_jp = "いまは さんじです。"
        ex1_en = "Now it is 3 o'clock."
        ex2_jp = "なんじに いきますか。"
        ex2_en = "What time are you going?"
    elif "minute" in en.lower() or jp == "ー分 (ーぷん)":
        ex1_jp = "ごふん まってください。"
        ex1_en = "Please wait for 5 minutes."
        ex2_jp = "いまは さんじ じゅっぷんです。"
        ex2_en = "Now it is 3:10."
    elif "how many minutes" in en.lower() or jp == "何分*":
        ex1_jp = "えきまで なんぷん かかりますか。"
        ex1_en = "How many minutes does it take to the station?"
        ex2_jp = "なんぷん まちましたか。"
        ex2_en = "How many minutes did you wait?"
    elif "it's tough" in en.lower() or jp == "大変ですね。":
        ex1_jp = "たいへんですね。がんばってください。"
        ex1_en = "It's tough, isn't it? Good luck."
        ex2_jp = "それは たいへんですね。"
        ex2_en = "That is tough, isn't it?"
    elif "number" in en.lower() or jp == "番号":
        ex1_jp = "でんわばんごうは なんばんですか。"
        ex1_en = "What is your phone number?"
        ex2_jp = "へやの ばんごうを おしえて ください。"
        ex2_en = "Please tell me the room number."
    elif "what number" in en.lower() or jp == "何番":
        ex1_jp = "あなたの へやは なんばんですか。"
        ex1_en = "What number is your room?"
        ex2_jp = "なんばんの バスに のりますか。"
        ex2_en = "What number bus will you take?"
    elif "there / your place" in en.lower() or jp == "そちら":
        ex1_jp = "そちらは あめですか。"
        ex1_en = "Is it raining over there?"
        ex2_jp = "そちらに いってください。"
        ex2_en = "Please go over there."
    elif en.lower() in ["new york"]:
        ex1_jp = "にゅーよーくから きました。"
        ex1_en = "I came from New York."
        ex2_jp = "にゅーよーくは にぎやかです。"
        ex2_en = "New York is lively."
    elif en.lower() in ["beijing"]:
        ex1_jp = "ぺきんから きました。"
        ex1_en = "I came from Beijing."
        ex2_jp = "ぺきんは おおきいです。"
        ex2_en = "Beijing is big."
    elif en.lower() in ["london"]:
        ex1_jp = "ろんどんから きました。"
        ex1_en = "I came from London."
        ex2_jp = "ろんどんは さむいです。"
        ex2_en = "London is cold."
    elif "fictional bank" in en.lower():
        ex1_jp = "あっぷるぎんこうは どこですか。"
        ex1_en = "Where is Apple Bank?"
        ex2_jp = "あっぷるぎんこうで おかねを おろします。"
        ex2_en = "I will withdraw money at Apple Bank."
    elif "fictional library" in en.lower():
        ex1_jp = "みどりとしょかんに いきます。"
        ex1_en = "I go to Midori Library."
        ex2_jp = "みどりとしょかんは しずかです。"
        ex2_en = "Midori Library is quiet."
    elif "fictional art museum" in en.lower():
        ex1_jp = "やまとびじゅつかんに いきます。"
        ex1_en = "I go to Yamato Art Museum."
        ex2_jp = "やまとびじゅつかんは きれいです。"
        ex2_en = "Yamato Art Museum is beautiful."
    elif "year" in en.lower() or jp == "-年":
        ex1_jp = "にせんじゅうねんに にほんに きました。"
        ex1_en = "I came to Japan in 2010."
        ex2_jp = "なんねんに にほんに きましたか。"
        ex2_en = "What year did you come to Japan?"
    elif "month" in en.lower() or jp == "-月":
        ex1_jp = "しがつの てんきは いいです。"
        ex1_en = "The weather in April is good."
        ex2_jp = "なんがつに にほんに いきますか。"
        ex2_en = "What month are you going to Japan?"
    elif "day" in en.lower() or jp == "－日":
        ex1_jp = "きょうは ついたちです。"
        ex1_en = "Today is the 1st."
        ex2_jp = "なんにちに いきますか。"
        ex2_en = "What day are you going?"
    elif "yes, that's right" in en.lower() or jp == "そうですね。":
        ex1_jp = "そうですね。わたしも そう おもいます。"
        ex1_en = "Yes, that's right. I think so too."
        ex2_jp = "そうですね。いきましょう。"
        ex2_en = "Yes, let's go."
    elif "thank you very much (many thanks)" in en.lower() or jp == "[どうも] ありがとう ございました。":
        ex1_jp = "どうも ありがとう ございました。"
        ex1_en = "Thank you very much."
        ex2_jp = "ほんを かしてくれて、ありがとう ございました。"
        ex2_en = "Thank you for lending me the book."
    elif "you're welcome" in en.lower() or jp == "どう いたしまして。":
        ex1_jp = "いいえ、どういたしまして。"
        ex1_en = "No, you're welcome."
        ex2_jp = "どういたしまして。また どうぞ。"
        ex2_en = "You're welcome. Please come again."
    elif "platform no." in en.lower() or jp == "一番線":
        ex1_jp = "いちばんせんに でんしゃが きます。"
        ex1_en = "The train is coming to platform 1."
        ex2_jp = "いちばんせんは どこですか。"
        ex2_en = "Where is platform 1?"
    elif "next / following" in en.lower() or jp == "次の":
        ex1_jp = "つぎの でんしゃに のります。"
        ex1_en = "I will take the next train."
        ex2_jp = "つぎの えきで おります。"
        ex2_en = "I will get off at the next station."
    elif "koshien" in en.lower() or jp == "甲子園":
        ex1_jp = "こうしえんに いきます。"
        ex1_en = "I will go to Koshien."
        ex2_jp = "こうしえんは おおきいです。"
        ex2_en = "Koshien is big."
    elif "osaka castle" in en.lower() or jp == "大阪城":
        ex1_jp = "おおさかじょうは きれいです。"
        ex1_en = "Osaka Castle is beautiful."
        ex2_jp = "おおさかじょうに いきます。"
        ex2_en = "I will go to Osaka Castle."

    output_map[audio_id] = [
        {
            'japanese': ex1_jp,
            'furigana': ex1_jp.replace(' ', ''),
            'english': ex1_en,
            'bangla': bn
        },
        {
            'japanese': ex2_jp,
            'furigana': ex2_jp.replace(' ', ''),
            'english': ex2_en,
            'bangla': bn
        }
    ]

# Output the map
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(output_map, f, ensure_ascii=False, indent=2)

print(f"Generated {len(output_map)} mappings in sentences.json.")
