import os, json

en_file = 'd:/Mods/RagnarMMO/src/main/resources/assets/ragnarmmo/lang/en_us.json'
with open(en_file, 'r', encoding='utf-8') as f:
    en_keys = set(json.load(f).keys())

es_file = 'd:/Mods/RagnarMMO/src/main/resources/assets/ragnarmmo/lang/es_es.json'
with open(es_file, 'r', encoding='utf-8') as f:
    es_keys = set(json.load(f).keys())

expected_keys = set()
# Extraer llaves de los datapacks JSON
skills_dir = 'd:/Mods/RagnarMMO/src/main/resources/data/ragnarmmo/skills'
for f in os.listdir(skills_dir):
    if f.endswith('.json'):
        basename = f.replace('.json', '')
        # Las translation keys para names y descriptions de skills suelen ser así
        expected_keys.add(f"skill.ragnarmmo.{basename}")
        expected_keys.add(f"skill.ragnarmmo.{basename}.desc")

missing_in_en = expected_keys - en_keys
missing_in_es = expected_keys - es_keys

print("Missing skill keys in EN:", len(missing_in_en))
for k in sorted(missing_in_en):
    print("  -", k)
print("\nMissing skill keys in ES:", len(missing_in_es))
for k in sorted(missing_in_es):
    print("  -", k)

