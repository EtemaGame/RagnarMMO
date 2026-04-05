# RagnarMMO Mod - Complete Guide

Welcome to **RagnarMMO**, a Minecraft mod that brings the classic RPG depth, mathematics, and spirit of *Ragnarok Online (Pre-Renewal)* to your blocky world. This mod completely transforms Minecraft's combat, replacing it with an ecosystem of attributes, statistics, classes, parallel progression, and skills.

---

## 🌟 1. Dual Progression System

RagnarMMO removes the standard Minecraft experience usage and divides it into two parallel levels that increase independently by killing mobs or performing activities:

1.  **Base Level**: Increases your general physical/magical power and grants **Status Points** to improve your basic stats.
2.  **Job Level**: Defines your mastery in your current profession. Leveling up grants **Skill Points** to unlock or improve spells and passives. Reaching maximum job levels allows for class advancement.

---

## 📊 2. Attributes (Stats) and Formulas

Every time you level up your Base Level, you get points to invest freely by pressing the `V` key (default). Each stat profoundly impacts your character:

*   **STR (Strength)**
    *   **Melee ATK:** Exponentially increases physical melee damage.
    *   **Weight Limit:** Determines your inventory weight limit (+50 capacity per point).
*   **AGI (Agility)**
    *   **Flee (Evasion):** Increases your chance to completely dodge enemy physical attacks (+1 Flee per point).
    *   **ASPD (Attack Speed):** Increases the speed at which you strike.
*   **VIT (Vitality)**
    *   **Max HP:** Exponentially increases your maximum health based on your class.
    *   **HP Regen & Soft DEF:** Increases natural health regeneration and grants flat physical defense.
    *   **Status Resistance:** Reduces the probability and duration of harmful status effects (e.g., Poison, Bleeding).
*   **INT (Intelligence)**
    *   **Max SP & Regen:** Increases maximum Mana and its regeneration per second.
    *   **MATK (Magic Attack) & Soft MDEF:** Increases spell potency and provides flat magic resistance.
    *   **Cast Time:** Categorically reduces dynamic spell casting time.
*   **DEX (Dexterity)**
    *   **Hit (Accuracy):** Crucial for not missing hits against high-level monsters (+1 Hit per point).
    *   **Ranged ATK:** The primary attribute for Bow and Crossbow damage.
    *   **Cast Time & ASPD:** Sharply reduces cast time (more effective than INT) and slightly contributes to Attack Speed.
    *   **Damage Variance:** Stabilizes your weapon's minimum damage.
*   **LUK (Luck)**
    *   **Critical Hit:** Increases the chance to land critical hits (+0.3% per point).
    *   **Perfect Dodge:** Grants an absolute chance to dodge attacks, ignoring enemy Hit (+0.1% per point).
    *   Provides minor increments to almost all other attributes (ATK, MATK, HIT, FLEE).

---

## 🐣 3. The Job System (Classes)

You begin your adventure as a **Novice**.

*   **Novice (Max Job Lv. 10):** Invest points in basic survival skills. At Job Level 10, you can evolve to your "First Job".
*   **First Classes (Max Job Lv. 50):**
    *   🛡️ **Swordsman**: Formidable tanks specializing in damage absorption and melee mastery.
    *   🗡️ **Thief**: Masters of Flee and quick strikes. Ideal for AGI-based builds. Supporting **Off-hand Daggers** for dual-wielding.
    *   🏹 **Archer**: Ranged damage specialists relying primarily on DEX.
    *   🧙 **Mage**: Elemental casters with high area-of-effect destruction.
    *   ⛪ **Acolyte**: Support class. Heals through faith and blesses allies.
    *   💰 **Merchant**: Masters of inventory. Use the **Cart** to bypass weight limits.
*   **Second Classes (WIP):**
    *   Knight, Assassin, Wizard, Priest, Hunter, Blacksmith (Currently in active development).

---

## ⚔️ 4. Classic Combat Mechanics

### Hit vs Flee (Accuracy vs Evasion)
Not all hits land. Your `HIT` is compared against the enemy's `FLEE`. If you miss, you'll see "Miss" particles.

### Critical Hits & Crit Shield
Critical hits (based on LUK) ignore enemy Flee and Soft DEF. High-level enemies have a "Crit Shield" to reduce your crit chance.

### Dual Wielding (Off-hand Attacks)
Thieves and Assassins can utilize an off-hand weapon (Daggers). This allows for a secondary attack with its own cooldown and swing, providing high-rate damage at the cost of shield protection.

### ASPD and Shield Penalty
Equipping a **Shield** in the off-hand significantly reduces your base ASPD.

### Cast Time and Global Cooldown (Cast Delay)
*   **Cast Time:** Some skills have a cast bar. Reduced by DEX and INT.
*   **Cast Delay:** A global cooldown that prevents spamming skills.

---

## 💎 5. Equipment & Rarities
Items dropped by monsters come unidentified. Once revealed, they acquire a **Rarity Tier** (Common to Unique) and random stat lines.

---

## ⚒️ 6. Life Skills (WIP)
Non-combat activities grant parallel XP in **Mining, Woodcutting, Excavation, Farming, Fishing, and Exploration.** Unlock perks every 10 levels.

---

## 👥 7. Party System & World Scaling
*   **Zone Difficulty:** Mobs scale by distance from spawn, depth, and structure proximity.
*   **Dynamic Scaling:** Monster HP and ATK scale based on the number of nearby players.
*   **Party XP:** Shared XP in a 50-block radius with group bonuses.

---

## ⌨️ 8. Commands & UI
*   **`V` (Default)**: Opens the character menu (Stats, Skills, Life Skills).
*   `/mobstats`: Detailed mathematical breakdown of a monster.
*   `/ragnar party`: Manage your group.
*   `/ragnar cart`: Remote access to the Merchant's cart.

---

## 📢 9. Community & Support
Do you have ideas, want to report bugs, or suggest improvements? Join our community!

**Discord:** [https://discord.gg/sZ64CgX3JN](https://discord.gg/sZ64CgX3JN)

---
*RagnarMMO is currently in active development (WIP). Features are subject to rebalancing.*
