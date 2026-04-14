# RagnarMMO

RagnarMMO is a comprehensive Minecraft overhaul mod inspired by the legendary systems of classic **Ragnarok Online**. It replaces vanilla combat and progression with a deep, data-driven RPG experience featuring dual leveling, classic stat allocation, and a unique class hierarchy.

## ⚔️ Core Gameplay Features

### 1. Dual Progression System
Your character grows in two distinct ways:
- **Base Level (Max 99)**: Grants **Status Points** used to increase your core attributes (STR, AGI, etc.).
- **Job Level (Max 50)**: Grants **Skill Points** used to unlock and level up abilities in your current class's skill tree.

### 2. Classic Attribute System
Allocate your points to customize your build:
- **STR (Strength)**: Increases physical damage and weight limit.
- **AGI (Agility)**: Increases attack speed and Evasion (Flee).
- **VIT (Vitality)**: Increases Max HP and Physical Defense.
- **INT (Intelligence)**: Increases Magic Attack and Max Mana.
- **DEX (Dexterity)**: Increases Accuracy (Hit) and reduces Cast Time.
- **LUK (Luck)**: Increases Critical Rate and Perfect Dodge.

### 3. Class Hierarchy (Jobs)
Start your journey as a **Novice** and choose your path:
- **First Jobs**: Swordsman, Mage, Archer, Thief, Merchant, Acolyte.
- **Second Jobs**: Knight, Wizard, Hunter, Assassin, Blacksmith, Priest.
*Note: Promotion to 1st Job requires Job Lv. 10. Promotion to 2nd Job requires Job Lv. 40.*

---

## 🐲 Mobs & Difficulty

The world is dangerous and adapts to your presence. Mobs follow a tier system that significantly affects their power and loot:
- **Normal**: Standard enemies.
- **Elite**: Stronger version with increased health and damage.
- **Mini-Boss**: Rare, powerful encounters.
- **Boss**: Massive challenges found in dungeons or structures.
- **MVP**: The ultimate threat. Extremely rare and dangerous.

### In-Game Catalog (Bestiary)
Use the built-in **Bestiary** to discover mobs, check their stats, and see their tiers. 
- Command: `/ragnar mobmanualui`

---

## 🛠️ Life Skills
Progression isn't just about combat. RagnarMMO includes a dedicated Life Skills system with its own levels and perks:
- **Mining**: Harvest ores efficiently.
- **Woodcutting**: Master the forests.
- **Excavation**: Discover hidden treasures.
- **Farming**: Optimized crop management.
- **Fishing**: Master the waters.
- **Exploration**: Travel the world to gain experience.
*Gain a powerful perk every 10 levels in any Life Skill!*

---

## 🎒 Social & Items

### Party System
Team up with friends to share experience and tackle bosses.
- **Party HUD**: Real-time health and status of your teammates.
- **Share Rules**: Configurable XP sharing within range.

### Zeny Economy
Gold, Silver, and Copper Zeny coins drop from mobs. Use them to trade or save for powerful upgrades.

### Equipment & Refining
- **Refinement System**: Safely upgrade your gear up to +4. Exceeding the safe limit risks breaking the item but grants massive power.
- **Item Rules**: Weapons and armor have job and level restrictions inspired by RO rules.

---

## ⌨️ Controls & Commands

### Keybinds
| Key | Action |
|-----|--------|
| **V** | Open Character Status / Stat Allocation |
| **K** | Open Skill Tree |
| **Y** | Open Achievements / Quest Log |
| **Left Alt** | Toggle Combat Mode (Enables Skill Hotbar) |
| **1 - 6** | Use Skills (While in Combat Mode) |

### Commands
| Command | Result |
|---------|--------|
| `/stats` | Quick access to your character stats |
| `/party create <name>` | Start a new party |
| `/pc <message>` | Chat with your party members |
| `/ragnar mobmanualui` | Open the Mob Catalog (Bestiary) |
| `/mobstats` | Inspect the scaling of the mob you are looking at |

---

## ⚙️ Configuration
Server owners can customize almost everything in `ragnarmmo-server.toml`, including:
- Level caps and XP rates.
- Mob scaling modes (Distance-based, Biome-based, or Player Level-based).
- Safe refinement levels and break chances.
- Anti-farm systems to prevent exploit camping.
