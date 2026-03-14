import os
import shutil

source_dir = r"E:\OneDrive\Desktop\ScreenShot Mods\Ragnar"
skills_dest = r"d:\Mods\RagnarMMO\src\main\resources\assets\ragnarmmo\textures\gui\skills"
items_dest = r"d:\Mods\RagnarMMO\src\main\resources\assets\ragnarmmo\textures\item"

# Mod's expected valid skills and items mapping can be inferred since the user already named them exactly.
# We will copy all PNGs from specific non-craftpix folders.

valid_skill_folders = [
    "Acolyte", "Archer", "Life", "Mage", "Merchant", "Novice", "Swordman", "Thief", "Knight", "Wizard", "Hunter", "Priest"
]

valid_item_folders = [
    "Baculos", "Dagger", "Mace 1H", "Mace 2H", "Shield", "Staff", "Sword 1H", "Sword 2H", "WarAxe 1H", "WarAxe 2H"
]

copied_skills = 0
copied_items = 0

for folder in os.listdir(source_dir):
    folder_path = os.path.join(source_dir, folder)
    if not os.path.isdir(folder_path) or "craftpix" in folder.lower() or "vanilla" in folder.lower():
        continue
        
    for file in os.listdir(folder_path):
        if not file.lower().endswith(".png"):
            continue
            
        file_path = os.path.join(folder_path, file)
        
        # If it's a Sprite layer or similar, ignore unless explicitly named
        if "Layer" in file or "sprite" in file.lower():
            # There were files like "Layer 1_sprite_01.png", these aren't the final item names
            continue
            
        # The user named the real ones correctly, e.g., "swordman_bash.png", "test_sword_1h.png"
        if folder in valid_skill_folders:
            dest_path = os.path.join(skills_dest, file)
            shutil.copy2(file_path, dest_path)
            copied_skills += 1
            print(f"Copied skill: {file}")
            
        elif folder in valid_item_folders or "test_" in file.lower() or file.lower() == "card_template.png":
            dest_path = os.path.join(items_dest, file)
            shutil.copy2(file_path, dest_path)
            copied_items += 1
            print(f"Copied item: {file}")

print(f"Finished copying! {copied_skills} skills and {copied_items} items copied.")
