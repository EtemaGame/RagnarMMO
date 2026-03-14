import json
import os

LANG_DIR = r"d:\Mods\RagnarMMO\src\main\resources\assets\ragnarmmo\lang"

def main():
    en_path = os.path.join(LANG_DIR, "en_us.json")
    with open(en_path, "r", encoding="utf-8") as f:
        en_data = json.load(f)

    for file_name in os.listdir(LANG_DIR):
        if not file_name.endswith(".json") or file_name in ["en_us.json", "es_es.json"]:
            continue

        file_path = os.path.join(LANG_DIR, file_name)
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        missing_keys = [k for k in en_data.keys() if k not in data]
        if not missing_keys:
            print(f"{file_name} is up to date.")
            continue
        
        fallback_count = 0
        for k in missing_keys:
            data[k] = en_data[k]
            fallback_count += 1
                
        # Write back preserving format
        # Important: Minecraft requires UTF-8 formatting and no unicode escapes.
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            
        print(f"Finished {file_name}: Injected {fallback_count} missing keys from English.")

if __name__ == "__main__":
    main()
