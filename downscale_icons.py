from PIL import Image
import os
import glob
import shutil

brain_dir = r'C:\Users\Etema\.gemini\antigravity\brain\857cad15-2a9e-4e18-8d7e-c12fb8f8f00e'
dest_dir = r'd:\Mods\RagnarMMO\src\main\resources\assets\ragnarmmo\textures\gui\skills'

# Diccionario de archivos generados hacia su destino
mapping = {
    'hunter_beast_bane': 'hunter_beast_bane.png',
    'hunter_blitz_beat': 'hunter_blitz_beat.png',
    'hunter_double_strafe': 'hunter_double_strafe.png',
    'hunter_freezing_trap': 'hunter_freezing_trap.png',
    'knight_bowling_bash': 'knight_bowling_bash.png',
    'knight_brandish_spear': 'knight_brandish_spear.png',
    'knight_pierce': 'knight_pierce.png',
    'knight_spear_mastery': 'knight_spear_mastery.png',
    'mage_cold_bolt': 'mage_cold_bolt.png',
    'mage_fire_ball': 'mage_fire_ball.png',
    'mage_fire_bolt': 'mage_fire_bolt.png',
    'mage_lightning_bolt': 'mage_lightning_bolt.png',
    'priest_blessing': 'priest_blessing.png',
    'priest_heal': 'priest_heal.png',
    'priest_magnificat': 'priest_magnificat.png',
}

processed_count = 0

for gen_name, target_file in mapping.items():
    # Buscar el archivo generado (tiene un sufijo numérico)
    files = glob.glob(os.path.join(brain_dir, f"{gen_name}_*.png"))
    if not files:
        continue
    
    src_file = files[0]
    
    img = Image.open(src_file)
    
    # Redimensionar la imagen a pixel-art 64x64
    # Si la imagen no es cuadrada, forzamos recorte centrado
    width, height = img.size
    min_dim = min(width, height)
    left = (width - min_dim)/2
    top = (height - min_dim)/2
    right = (width + min_dim)/2
    bottom = (height + min_dim)/2
    
    img = img.crop((left, top, right, bottom))
    img = img.resize((64, 64), Image.Resampling.LANCZOS)
    
    out_path = os.path.join(dest_dir, target_file)
    img.save(out_path, format="PNG")
    processed_count += 1
    
    print(f"[{processed_count}] Resized and replaced: {target_file} from {src_file}")

print(f"\nFinal: {processed_count} images replaced in {dest_dir}")
