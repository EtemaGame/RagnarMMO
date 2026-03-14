import os, re, json

en_file = 'd:/Mods/RagnarMMO/src/main/resources/assets/ragnarmmo/lang/en_us.json'
with open(en_file, 'r', encoding='utf-8') as f:
    en_keys = set(json.load(f).keys())

found_keys = set()
pattern = re.compile(r'translatable\(\s*\"([^\"]+)\"\s*\)')

for root, dirs, files in os.walk('d:/Mods/RagnarMMO/src/main/java'):
    for file in files:
        if file.endswith('.java'):
            with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                content = f.read()
                found_keys.update(pattern.findall(content))
                
missing_in_en = found_keys - en_keys
print('Potential keys in code (translatable("...")) missing in en_us:')
for k in sorted(missing_in_en):
    print("  -", k)

# Also let's check for keys that might be dynamically generated.
# Things like "skill.ragnarmmo." or "stat.ragnarmmo."
# We can check Registries.
