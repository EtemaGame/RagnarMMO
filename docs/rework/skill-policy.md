# Skill Port Policy

This rework keeps Java focused on behavior, targeting, and engine hooks while moving tuning into skill JSON `level_data`.

## Port Types

| Type | Meaning |
| --- | --- |
| `RO_EXACT` | Preserve the Pre-Renewal fantasy and table shape as closely as the current engine allows. |
| `RO_ADAPTED` | Preserve the RO role, but translate mechanics that do not map directly to Minecraft. |
| `MC_INSPIRED` | Keep the class identity, but design around Minecraft systems because the RO mechanic needs an economy, inventory, or server rule that does not exist yet. |

## Rules

- Canonical first-job skills must exist in JSON.
- `SkillType` is only a temporary compatibility bridge and must not be used for new balance decisions.
- Active skill cost, cast time, cast delay, cooldown, damage, hit count, duration, and status chances should be read from `level_data`.
- Skill tree JSON owns layout. Skill JSON owns skill metadata.
- Minecraft adaptation keys are allowed only when the RO mechanic has no direct primitive in Minecraft.

## First-Job Policy Table

| Class | Skill | RO role | Current status | Port type | Priority |
| --- | --- | --- | --- | --- | --- |
| Archer | Owl's Eye | Passive DEX scaling | JSON+Java, no `level_data` | `RO_EXACT` | P0 |
| Archer | Vulture's Eye | Accuracy/range passive | JSON+Java, needs behavior rework | `RO_ADAPTED` | P0 |
| Archer | Improve Concentration | DEX/AGI buff + reveal | JSON+Java, no `level_data` | `RO_ADAPTED` | P0 |
| Archer | Double Strafing | Bow single-target burst | JSON+Java, no `level_data` | `RO_EXACT` | P0 |
| Archer | Arrow Shower | Bow area attack/control | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Swordsman | Sword Mastery | Passive weapon mastery | JSON+Java, no `level_data` | `RO_EXACT` | P1 |
| Swordsman | Increase Recuperative Power | Passive HP recovery | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Swordsman | Bash | Melee burst | JSON+Java, has `level_data` | `RO_EXACT` | P1 |
| Swordsman | Provoke | Offense/defense debuff | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Swordsman | Two-Handed Sword Mastery | Passive weapon mastery | JSON+Java, no `level_data` | `RO_EXACT` | P2 |
| Swordsman | Magnum Break | Fire AoE + fire weapon window | JSON+Java, has `level_data` | `RO_ADAPTED` | P1 |
| Swordsman | Endure | Anti-flinch frontline tool | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Mage | Increase Spiritual Power | Passive SP recovery | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Mage | Sight | Hidden reveal | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Mage | Napalm Beat | Ghost magic AoE | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Mage | Soul Strike | Ghost multi-hit magic | JSON+Java, no `level_data` | `RO_EXACT` | P1 |
| Mage | Safety Wall | Anti-melee ground defense | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Mage | Cold Bolt | Elemental bolt | JSON+Java, has `level_data` | `RO_EXACT` | P1 |
| Mage | Frost Diver | Freeze control | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Mage | Stone Curse | Stone control | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Mage | Fire Bolt | Elemental bolt | JSON+Java, has `level_data` | `RO_EXACT` | P1 |
| Mage | Fire Ball | Splash magic | JSON+Java, has `level_data` | `RO_ADAPTED` | P2 |
| Mage | Fire Wall | Ground control | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Mage | Lightning Bolt | Elemental bolt | JSON+Java, has `level_data` | `RO_EXACT` | P1 |
| Mage | Thunder Storm | Lightning AoE | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Divine Protection | Undead/demon mitigation | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Demon Bane | Undead/demon offense | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Angelus | Defensive buff | JSON+Java, has `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Blessing | STR/DEX/INT buff | JSON+Java, has `level_data` | `RO_EXACT` | P1 |
| Acolyte | Heal | Healing + undead damage | JSON+Java, no `level_data` | `RO_EXACT` | P1 |
| Acolyte | Increase Agility | AGI/move buff | JSON+Java, has `level_data` | `RO_ADAPTED` | P1 |
| Acolyte | Decrease Agility | Slow/debuff | JSON+Java, has `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Cure | Status cleanse | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Acolyte | Ruwach | Reveal + anti-hidden pulse | JSON+Java, no `level_data` | `RO_ADAPTED` | P1 |
| Acolyte | Teleportation | Mobility utility | JSON+Java, has `level_data` | `MC_INSPIRED` | P3 |
| Acolyte | Warp Portal | Party utility | JSON+Java, has `level_data` | `MC_INSPIRED` | P3 |
| Acolyte | Pneuma | Anti-ranged ground defense | JSON+Java, has `level_data` | `RO_ADAPTED` | P1 |
| Acolyte | Aqua Benedicta | Consumable ritual | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Acolyte | Holy Light | Quest/special holy attack | JSON+Java, has `level_data` | `RO_ADAPTED` | P3 |
| Acolyte | Signum Crucis | Undead defense debuff | JSON+Java, has `level_data` | `RO_ADAPTED` | P2 |
| Thief | Double Attack | Proc passive | JSON+Java, no `level_data` | `RO_EXACT` | P2 |
| Thief | Improve Dodge | FLEE passive | JSON+Java, no `level_data` | `RO_EXACT` | P2 |
| Thief | Steal | Loot/economy utility | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Thief | Hiding | Threat break/stealth | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Thief | Envenom | Poison attack | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |
| Thief | Detoxify | Poison cleanse | JSON+Java, no `level_data` | `RO_ADAPTED` | P3 |
| Merchant | Enlarge Weight Limit | Capacity passive | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Discount | Buy discount | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Overcharge | Sell bonus | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Pushcart | Cart utility | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Vending | Player shop utility | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Buying Store | Buying shop utility | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Identify | Item utility | JSON+Java, no `level_data` | `MC_INSPIRED` | P3 |
| Merchant | Mammonite | Zeny-cost melee burst | JSON+Java, no `level_data` | `RO_ADAPTED` | P2 |

