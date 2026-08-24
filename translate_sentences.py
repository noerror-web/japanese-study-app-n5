import json
import urllib.request
import urllib.parse
import time
import sys
import os

# Reconfigure stdout to use UTF-8
sys.stdout.reconfigure(encoding='utf-8')

json_path = r"app/src/main/assets/sentences.json"

def translate_en_to_bn(text):
    url = "https://translate.googleapis.com/translate_a/single"
    params = {
        "client": "gtx",
        "sl": "en",
        "tl": "bn",
        "dt": "t",
        "q": text
    }
    url_parts = list(urllib.parse.urlparse(url))
    query = urllib.parse.urlencode(params)
    url_parts[4] = query
    full_url = urllib.parse.urlunparse(url_parts)
    
    req = urllib.request.Request(full_url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        content = response.read().decode('utf-8')
        data = json.loads(content)
        translated_text = ""
        for part in data[0]:
            if part[0]:
                translated_text += part[0]
        return translated_text

def main():
    if not os.path.exists(json_path):
        print(f"Error: {json_path} not found.")
        sys.exit(1)

    print("Loading sentences.json...")
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    # Flatten sentences to a list of dicts to keep order and references
    flat_sentences = []
    for key, val_list in data.items():
        for index, item in enumerate(val_list):
            flat_sentences.append({
                "key": key,
                "index": index,
                "english": item["english"],
                "original_bangla": item.get("bangla", "")
            })

    total = len(flat_sentences)
    print(f"Total sentences to translate: {total}")

    batch_size = 50
    completed = 0

    # We will do batch translations
    for i in range(0, total, batch_size):
        batch = flat_sentences[i:i+batch_size]
        batch_texts = [item["english"] for item in batch]
        
        # Combine texts with newlines
        combined_text = "\n".join(batch_texts)
        
        translated_lines = []
        success = False
        
        print(f"Translating batch {i // batch_size + 1}/{(total + batch_size - 1) // batch_size} (sentences {i} to {i + len(batch)})...")
        
        try:
            translated_combined = translate_en_to_bn(combined_text)
            # Split lines
            translated_lines = [line.strip() for line in translated_combined.split("\n") if line.strip()]
            
            # Verify line count
            if len(translated_lines) == len(batch):
                success = True
            else:
                print(f"Warning: Batch returned {len(translated_lines)} lines, expected {len(batch)}. Falling back to individual translation for this batch.")
        except Exception as e:
            print(f"Error translating batch: {e}. Falling back to individual translation.")

        if success:
            for idx, item in enumerate(batch):
                item["translated_bangla"] = translated_lines[idx]
            completed += len(batch)
            time.sleep(1.0) # Rate limiting delay
        else:
            # Fallback: translate one by one
            for item in batch:
                try:
                    translated = translate_en_to_bn(item["english"])
                    item["translated_bangla"] = translated.strip()
                    completed += 1
                    print(f"  Translated individual: '{item['english']}' -> '{item['translated_bangla']}'")
                    time.sleep(1.0)
                except Exception as ex:
                    print(f"  Failed to translate '{item['english']}': {ex}")
                    item["translated_bangla"] = item["original_bangla"] # Keep original on failure
                    time.sleep(2.0)

        print(f"Progress: {completed}/{total} translated.")

    # Reconstruct data dictionary
    for item in flat_sentences:
        key = item["key"]
        idx = item["index"]
        data[key][idx]["bangla"] = item.get("translated_bangla", item["original_bangla"])

    print("Saving updated sentences.json...")
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print("Translation process complete!")

if __name__ == "__main__":
    main()
