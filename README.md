# RagnarMMO Mod - Complete Guide

Welcome to **RagnarMMO**, a Minecraft mod that brings the classic RPG depth, mathematics, and spirit of *Ragnarok Online (Pre-Renewal)* to your blocky world. This mod completely transforms Minecraft's combat, replacing it with an ecosystem of attributes, statistics, classes, parallel progression, and skills.

---

## 🎯 Scope

RagnarMMO is currently scoped as:
- A **Pre-Renewal-inspired** adaptation, not a literal 1:1 port.
- **PvE-first** for singleplayer, co-op, and dedicated servers.
- Focused on **Novice + 1st jobs + 2nd jobs**.
- Built around RO systems layered over Minecraft mobs, zones, and structures.

**Out of scope for this project:**
- 3rd jobs
- War of Emperium
- Castles, Emperium systems, and guild-war gameplay

**Later-scope systems (not part of the core release track):**
- Transcendent / Rebirth classes
- Formal PvP
- Basic Guild Systems
- Vending

---

## 🌟 1. Dual Progression System

RagnarMMO removes the standard Minecraft experience usage and divides it into two parallel levels that increase independently by killing mobs or performing activities:

1. **Base Level**: Increases your general physical/magical power and grants **Status Points** to improve your basic stats.
2. **Job Level**: Defines your mastery in your current profession. Leveling up grants **Skill Points** to unlock or improve spells and passives. Reaching maximum job levels allows for class advancement.

---

## 📊 2. Attributes (Stats) and Formulas

Every time you level up your Base Level, you get points to invest freely by pressing the `V` key (default). Each stat profoundly impacts your character:

* **STR (Strength)**: Exponentially increases physical melee damage (ATK) and increases your Weight Limit (+50 capacity per point).
* **AGI (Agility)**: Increases your chance to completely dodge enemy physical attacks (Flee) and improves Attack Speed (ASPD).
* **VIT (Vitality)**: Exponentially increases your Max HP based on your class. Boosts HP Regen, flat physical defense (Soft DEF), and status resistance.
* **INT (Intelligence)**: Increases Max SP and regeneration. Boosts magical potency (MATK), magic defense (Soft MDEF), and reduces dynamic cast time.
* **DEX (Dexterity)**: Crucial for Accuracy (Hit) to avoid missing, primary attribute for Ranged ATK, heavily reduces cast time, and stabilizes weapon damage variance.
* **LUK (Luck)**: Increases Critical Hit chance (ignoring Flee/DEF) and Perfect Dodge chance (absolute dodge chance). Grants minor increments to almost all other attributes.

---

## ⚔️ 3. Classic Combat Mechanics

* **Hit vs Flee**: Not all hits land. Your `HIT` is compared against the enemy's `FLEE`. If you miss, you'll see "Miss" particles.
* **Critical Hits**: Based on LUK, critical hits ignore enemy Flee and Soft DEF. High-level enemies have a "Crit Shield" to reduce your crit chance.
* **Dual Wielding**: Thieves and Assassins can utilize an off-hand weapon (Daggers) for a secondary attack with its own cooldown, sacrificing shield protection.
* **ASPD & Penalties**: Equipping a shield in the off-hand significantly reduces your base ASPD.
* **Cast Time & Delay**: Spells have a cast bar reduced by DEX/INT. Cast Delay acts as a global cooldown preventing skill spam.

---

## 🐣 4. The Job System & Skills Catalog

You begin your adventure as a **Novice** and at Job Level 10, you can evolve to your "First Job". Reaching the max job level (50) allows advancement into a Second Job.

### 🛡️ Novice (Max Job Lv. 10)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **First Aid** | Passive | Regenerates HP outside of combat. |
| **Survival Instinct** | Passive | Grants defense when HP is critically low. |

### ⚔️ Swordsman
Tanks specializing in damage absorption and melee mastery.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Sword Mastery** | Passive | Increases damage dealt with one-handed swords. |
| **Two-Handed Sword Mastery** | Passive | Increases damage dealt with two-handed swords. |
| **Increase HP Recovery** | Passive | Enhances HP recovery and healing item effectiveness. |
| **Bash** | Active | Strikes an enemy with high precision and massive damage. |
| **Provoke** | Active | Taunts an enemy, lowering its defense but increasing its attack. |
| **Magnum Break** | Active | AoE fire explosion that consumes HP and pushes enemies. |
| **Endure** | Active | Prevents interruption when receiving damage and grants MDEF+. |

### 🛡️ Knight (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Spear Mastery** | Passive | Increases damage with spears (extra bonus while mounted). |
| **Pierce** | Active | Multiple spear strikes based on the target's size. |
| **Spear Stab** | Active | Attacks with a spear, pushing the target backward. |
| **Spear Boomerang** | Active | Throws the spear to attack from a distance. |
| **Brandish Spear** | Active | Frontal AoE attack while mounted. |
| **Two-Hand Quicken**| Active | Increases attack speed with two-handed swords. |
| **Auto Counter** | Active | Defensive stance that blocks an attack and counters. |
| **Bowling Bash** | Active | Pushes the target, causing massive damage if it collides. |
| **Peco Peco Ride** | Passive | Allows riding a Peco Peco mount. |

### 🧙 Mage
Elemental casters with high area-of-effect destruction.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **SP Recovery** | Passive | Passively improves SP regeneration. |
| **Sight** | Active | Reveals hidden enemies in a 7x7 area. |
| **Napalm Beat** | Active | Ghost-property attack in a 3x3 area. |
| **Soul Strike** | Active | Launches quick spiritual bolts (extra damage to Undead). |
| **Safety Wall** | Active | Magical shield that blocks short-range physical attacks. |
| **Cold Bolt / Fire Bolt / Lightning Bolt** | Active | Drops elemental bolts of ice, fire, or lightning from the sky. |
| **Frost Diver** | Active | Ice attack with a high chance to freeze the target. |
| **Stone Curse** | Active | Curse that gradually petrifies the enemy. |
| **Fire Ball / Thunder Storm**| Active | Damaging AoE explosion/storm. |
| **Fire Wall** | Active | Places fire walls to block and damage enemies. |

### 🔮 Wizard (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Monster Property** | Active | Analyzes a target to reveal HP, SP, and elements. |
| **Meteor Storm** | Active | Massive fire meteor shower with a chance to stun. |
| **Jupitel Thunder** | Active | Shoots an electric sphere that violently knocks back targets. |
| **Storm Gust** | Active | Snow tempests that freeze enemies after 3 hits. |
| **Lord of Vermillion** | Active | Destructive lightning storm with a chance to blind. |
| **Earth Spike / Heaven's Drive** | Active | Summons earth spikes from the ground. |
| **Water Ball** | Active | Fires powerful water blasts (requires user to stand on water). |
| **Quagmire** | Active | Creates a mud zone that reduces movement, AGI, and DEX. |

### 🏹 Archer
Ranged damage specialists relying primarily on DEX.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Owl's Eye** | Passive | Permanently increases DEX. |
| **Vulture's Eye** | Passive | Improves attack range and accuracy. |
| **Improve Concentration**| Buff | Temporarily increases DEX/AGI and detects hidden enemies. |
| **Double Strafe** | Active | Fires two powerful arrows consecutively. |
| **Arrow Shower** | Active | AoE arrow bombardment that knocks back targets. |

### 🦅 Hunter (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Traps (Skid, Land Mine, Ankle Snare, etc.)** | Active | Various tactical traps containing elements, CC, or utility. |
| **Beast Bane** | Passive | Increases damage against Animal and Insect monsters. |
| **Falconry Mastery** | Passive | Allows acquiring and commanding a Falcon. |
| **Blitz Beat** | Active | Commands the Falcon to perform rapid AoE strikes. |
| **Steel Crow** | Passive | Permanently increases the Falcon's damage. |

### ⛪ Acolyte
Support class that heals through faith and blesses allies.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Divine Protection / Demon Bane** | Passive | Enhances defense or attack against Undead and Demons. |
| **Ruwach** | Active | Reveals nearby invisible enemies and inflicts holy damage. |
| **Pneuma** | Active | Shield that blocks all long-range physical attacks. |
| **Heal** | Active | Restores target's HP or damages Undead creatures. |
| **Increase Agility**| Buff | Temporarily increases AGI and movement speed. |
| **Blessing** | Buff | Temporarily increases STR, DEX, and INT. Cures Curse. |
| **Warp Portal / Teleport**| Active | Instant long-distance travel and point-saving. |

### ✝️ Priest (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Mace Mastery** | Passive | Increases damage dealt with maces. |
| **Resurrection** | Active | Revives a fallen player. |
| **Magnificat** | Active | Doubles the SP recovery speed for the entire party. |
| **Kyrie Eleison** | Active | Creates a physical shield that blocks a set amount of damage. |
| **Gloria** | Active | Substantially increases LUK for the entire party. |
| **Magnus Exorcismus**| Active | Massive holy cross AoE severely damaging Demons/Undead. |
| **Sanctuary** | Active | Holy ground that heals allies and damages Undead. |

### 🗡️ Thief
Masters of Flee and quick strikes. Ideal for AGI builds and Off-hand Daggers.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Double Attack** | Passive | Chance to deal a double attack when using daggers. |
| **Improve Dodge** | Passive | Permanently increases Flee Rate. |
| **Steal** | Active | Chance to snatch items directly from monsters. |
| **Hiding** | Active | Hide to avoid enemy detection. |
| **Envenom** | Active | Attack that damages and bears a high chance to poison. |
| **Detoxify** | Active | Cures poison from a target. |

### ☠️ Assassin (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Dual Wield Mastery**| Passive | Recovers damage penalties when wielding two weapons. |
| **Katar Mastery** | Passive | Permanently increases damage with Katar weapons. |
| **Sonic Blow** | Active | 8 rapid consecutive hits with a Katar that can stun. |
| **Cloaking** | Active | Allows movement while hidden. |
| **Grimtooth** | Active | Ranged attack usable only while hidden with a Katar. |
| **Enchant Poison** | Buff | Temporarily imbues the weapon with Poison element. |
| **Venom Dust / Splasher**| Active | Creates toxic clouds or plants poison explosives. |

### 💰 Merchant
Masters of inventory. Bypasses weight limitations using the Cart.
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Enlarge Weight Limit**| Passive | Permanently increases max carry capacity. |
| **Discount / Overcharge**| Passive | Favorable transaction rates when dealing with NPCs. |
| **Pushcart** | Passive | Allows acquiring a cart for massive extra storage. |
| **Identify** | Active | Identifies unknown dropped equipment. |
| **Mammonite** | Active | Deals massive physical damage at the cost of Zeny. |

### 🔨 Blacksmith (2nd Class)
| Skill | Type | Description |
| :--- | :--- | :--- |
| **Tempering & Crafting**| Passive | Allows the refinement of ores, stones, and weapons. |
| **Weaponry Research**| Passive | Increases ATK, HIT, and forging success rates. |
| **Adrenaline Rush** | Active | Drastically increases ASPD with Axes and Maces. |
| **Weapon Perfection** | Active | Bypasses all weapon size penalties. |
| **Maximize Power** | Active | Guarantees hitting the maximum possible damage variance. |
| **Hammer Fall** | Active | Ground slam that has a high chance to stun an AoE. |

---

## 💎 5. Equipment & Rarities
Items dropped by monsters come unidentified. Once revealed, they acquire a **Rarity Tier** (Common to Unique) and random stat lines tailored for build diversity.

---

## ⚒️ 6. Life Skills (WIP)
Non-combat activities grant parallel XP. You can unlock perks every 10 levels for:
* **Mining**: Better ore efficiency.
* **Woodcutting**: Hardwood and rare tree drops.
* **Excavation**: Digging up buried treasures.
* **Farming**: Better crop yields.
* **Fishing**: Catching rare marine treasures.
* **Exploration**: Bonuses for charting the world.

---

## 👥 7. Party System & World Scaling
* **Zone Difficulty**: Mobs scale by distance from spawn, depth, and structure proximity.
* **Dynamic Scaling**: Monster HP and ATK seamlessly scale based on the number of nearby players.
* **Party XP**: Shared XP in a 50-block radius with lucrative group bonuses.

---

## ⌨️ 8. Commands & UI
* **`V` (Default)**: Opens the character menu (Stats, Skills, Life Skills).
* `/mobstats`: Detailed mathematical breakdown of a monster.
* `/ragnar party`: Manage your group.
* `/ragnar cart`: Remote access to the Merchant's cart.

---

## 📢 9. Community & Support
Do you have ideas, want to report bugs, or suggest improvements? Join our community!

**Discord:** [https://discord.gg/sZ64CgX3JN](https://discord.gg/sZ64CgX3JN)

---
*RagnarMMO is currently in active development (WIP). Features are subject to rebalancing.*
