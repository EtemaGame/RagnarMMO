import os, json

skills_dir = 'd:/Mods/RagnarMMO/src/main/resources/data/ragnarmmo/skills'

# Nueva simetría adaptada estrictamente a grid_x <= 6 y grid_y <= 3
layout = {
    # NOVICE
    "first_aid": (1, 0),
    "weapon_trainer": (3, 0),
    "basic_conditioning": (5, 0),
    "survival_instinct": (3, 1),
    
    # SWORDSMAN
    "sword_mastery": (2, 0),
    "one_hand_mastery": (1, 1),
    "two_hand_mastery": (3, 1),
    "bash": (4, 0),
    "provoke": (5, 0),
    "endurance": (6, 0),
    
    # KNIGHT
    "spear_mastery": (2, 0),
    "pierce": (1, 1),
    "brandish_spear": (3, 1),
    "bowling_bash": (2, 2),

    # MAGE
    "staff_mastery": (0, 0),
    "spell_knowledge": (2, 0),
    "mana_control": (4, 0),
    "arcane_regeneration": (5, 0),
    "fire_bolt": (1, 1),
    "cold_bolt": (2, 1),
    "lightning_bolt": (3, 1),
    "fire_ball": (1, 2),
    "magic_guard": (2, 2),
    "elemental_affinity": (3, 2),
    "overcast": (4, 2),
    "magic_amplification": (2, 3),
    
    # WIZARD
    "meteor_storm": (1, 0),
    "storm_gust": (3, 0),
    "lord_of_vermillion": (5, 0),
    "ice_wall": (3, 1),

    # ARCHER
    "bow_mastery": (3, 0),
    "accuracy_training": (1, 1),
    "evasion_boost": (5, 1),
    "critical_shot": (1, 2),
    "kiting_instinct": (3, 2),
    "wind_walker": (5, 2),

    # HUNTER
    "beast_bane": (2, 0),
    "freezing_trap": (4, 0),
    "blitz_beat": (2, 1),
    "double_strafe": (4, 1),

    # THIEF
    "dagger_mastery": (2, 0),
    "flee_training": (4, 0),
    "backstab_training": (1, 1),
    "stealth_instinct": (3, 1),
    "poison_expertise": (5, 1),
    "fatal_instinct": (3, 2),

    # MERCHANT
    "trading_knowledge": (1, 0),
    "business_mind": (5, 0),
    "overcharge": (1, 1),
    "cart_strength": (3, 1),
    "weapon_maintenance": (1, 2),
    "armor_maintenance": (5, 2),

    # ACOLYTE
    "mace_mastery": (1, 0),
    "faith": (4, 0),
    "divine_protection": (0, 1),
    "heal_power": (2, 1),
    "holy_resistance": (4, 1),
    "blessing_aura": (2, 2),

    # PRIEST
    "heal": (3, 0),
    "sanctuary": (1, 1),
    "magnificat": (5, 1),
    "blessing": (3, 2),
    
    # LIFE SKILLS 
    "mining": (1, 0),
    "woodcutting": (3, 0),
    "excavation": (5, 0),
    "survival": (1, 1), 
    "fishing": (3, 1),
    "exploration": (5, 1),
    "farming": (3, 2)
}

for f in os.listdir(skills_dir):
    if f.endswith('.json'):
        basename = f.replace('.json', '')
        if basename in layout:
            filepath = os.path.join(skills_dir, f)
            with open(filepath, 'r', encoding='utf-8') as file:
                data = json.load(file)
            
            data.setdefault('ui', {})['grid_x'] = layout[basename][0]
            data['ui']['grid_y'] = layout[basename][1]
                
            with open(filepath, 'w', encoding='utf-8') as file:
                json.dump(data, file, indent=2)

print("Matriz reorganizada con Grid máximo: Y=3, X=6")
