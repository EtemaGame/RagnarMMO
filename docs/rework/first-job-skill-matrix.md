# First-Job Skill Matrix

Skill tree JSON is the layout source. Skill JSON is metadata and tuning. This matrix was seeded from the current first-job tree and skill JSON files.

| Class | Skill id | Display name | Type | Prerequisites | Max level | Resource | MC adaptation | Current status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Archer | `ragnarmmo:owls_eye` | Owl's Eye | Passive | None | 10 | None | No | JSON+Java; no `level_data` |
| Archer | `ragnarmmo:vultures_eye` | Vulture's Eye | Passive | `ragnarmmo:owls_eye` 3 | 10 | None | Yes | JSON+Java; behavior rework needed |
| Archer | `ragnarmmo:improve_concentration` | Attention Concentrate | Active | `ragnarmmo:vultures_eye` 1 | 10 | SP | Yes | JSON+Java; no `level_data` |
| Archer | `ragnarmmo:double_strafe` | Double Strafing | Active | None | 10 | SP | Bow/projectile | JSON+Java; no `level_data` |
| Archer | `ragnarmmo:arrow_shower` | Arrow Shower | Active | `ragnarmmo:double_strafe` 5 | 10 | SP | AoE/projectile | JSON+Java; no `level_data` |
| Swordsman | `ragnarmmo:sword_mastery` | Sword Mastery | Passive | None | 10 | None | No | JSON+Java; no `level_data` |
| Swordsman | `ragnarmmo:increase_hp_recovery` | Increase Recuperative Power | Passive | None | 10 | None | Regen tick model | JSON+Java; no `level_data` |
| Swordsman | `ragnarmmo:bash` | Bash | Active | None | 10 | SP | Hit/stun timing | JSON+Java; has `level_data` |
| Swordsman | `ragnarmmo:provoke` | Provoke | Active | None | 10 | SP | Aggro/threat | JSON+Java; no `level_data` |
| Swordsman | `ragnarmmo:two_hand_mastery` | Two-Handed Sword Mastery | Passive | `ragnarmmo:sword_mastery` 1 | 10 | None | Weapon mapping | JSON+Java; no `level_data` |
| Swordsman | `ragnarmmo:magnum_break` | Magnum Break | Active | `ragnarmmo:bash` 5 | 10 | SP/HP | AoE/fire buff | JSON+Java; has `level_data` |
| Swordsman | `ragnarmmo:endure` | Endure | Active | `ragnarmmo:provoke` 5 | 10 | SP | Anti-flinch | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:increase_sp_recovery` | Increase Spiritual Power | Passive | None | 10 | None | Regen tick model | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:sight` | Sight | Active | None | 1 | Mana | Reveal | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:napalm_beat` | Napalm Beat | Active | None | 10 | Mana | AoE targeting | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:soul_strike` | Soul Strike | Active | `ragnarmmo:napalm_beat` 4 | 10 | Mana | Projectile/multi-hit | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:safety_wall` | Safety Wall | Active | `ragnarmmo:napalm_beat` 7; `ragnarmmo:soul_strike` 5 | 10 | Mana | Ground defense | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:cold_bolt` | Cold Bolt | Active | None | 10 | Mana | Projectile | JSON+Java; has `level_data` |
| Mage | `ragnarmmo:frost_diver` | Frost Diver | Active | `ragnarmmo:cold_bolt` 5 | 10 | Mana | Freeze/control | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:stone_curse` | Stone Curse | Active | None | 10 | Mana | Status/control | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:fire_bolt` | Fire Bolt | Active | None | 10 | Mana | Projectile | JSON+Java; has `level_data` |
| Mage | `ragnarmmo:fire_ball` | Fire Ball | Active | `ragnarmmo:fire_bolt` 4 | 10 | Mana | Splash projectile | JSON+Java; has `level_data` |
| Mage | `ragnarmmo:fire_wall` | Fire Wall | Active | `ragnarmmo:sight` 1; `ragnarmmo:fire_ball` 5 | 10 | Mana | Ground control | JSON+Java; no `level_data` |
| Mage | `ragnarmmo:lightning_bolt` | Lightning Bolt | Active | None | 10 | Mana | Projectile | JSON+Java; has `level_data` |
| Mage | `ragnarmmo:thunder_storm` | Thunder Storm | Active | `ragnarmmo:lightning_bolt` 4 | 10 | Mana | AoE | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:divine_protection` | Divine Protection | Passive | None | 10 | None | Race mitigation | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:demon_bane` | Demon Bane | Passive | `ragnarmmo:divine_protection` 3 | 10 | None | Race damage | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:angelus` | Angelus | Active | `ragnarmmo:divine_protection` 3 | 10 | Mana | Buff model | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:blessing` | Blessing | Active | `ragnarmmo:divine_protection` 5 | 10 | Mana | Buff model | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:heal` | Heal | Active | None | 10 | Mana | Undead targeting | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:increase_agi` | Increase Agility | Active | `ragnarmmo:heal` 3 | 10 | Mana/HP | Buff model | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:decrease_agi` | Decrease Agility | Active | `ragnarmmo:increase_agi` 1 | 10 | Mana | Debuff model | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:cure` | Cure | Active | `ragnarmmo:heal` 2 | 1 | Mana | Status cleanse | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:ruwach` | Ruwach | Active | None | 1 | Mana | Reveal pulse | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:teleportation` | Teleportation | Active | `ragnarmmo:ruwach` 1 | 2 | Mana | World travel | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:warp_portal` | Warp Portal | Active | `ragnarmmo:teleportation` 2 | 4 | Mana | World travel | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:pneuma` | Pneuma | Active | `ragnarmmo:warp_portal` 4 | 1 | Mana | Anti-projectile zone | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:aqua_benedicta` | Aqua Benedicta | Active | None | 1 | Mana | Ritual/item utility | JSON+Java; no `level_data` |
| Acolyte | `ragnarmmo:holy_light` | Holy Light | Active | None | 1 | Mana | Holy projectile | JSON+Java; has `level_data` |
| Acolyte | `ragnarmmo:signum_crucis` | Signum Crucis | Active | `ragnarmmo:demon_bane` 3 | 10 | Mana | Race debuff | JSON+Java; has `level_data` |
| Thief | `ragnarmmo:double_attack` | Double Attack | Passive | None | 10 | None | Proc hook | JSON+Java; no `level_data` |
| Thief | `ragnarmmo:improve_dodge` | Increase Dodge | Passive | None | 10 | None | FLEE mapping | JSON+Java; no `level_data` |
| Thief | `ragnarmmo:steal` | Steal | Active | None | 10 | SP | Loot/economy | JSON+Java; no `level_data` |
| Thief | `ragnarmmo:hiding` | Hiding | Active | `ragnarmmo:steal` 5 | 10 | SP | Threat/stealth | JSON+Java; no `level_data` |
| Thief | `ragnarmmo:envenom` | Envenom | Active | None | 10 | SP | Poison status | JSON+Java; no `level_data` |
| Thief | `ragnarmmo:detoxify` | Detoxify | Active | `ragnarmmo:envenom` 3 | 1 | SP | Poison cleanse | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:enlarge_weight_limit` | Enlarge Weight Limit | Passive | None | 10 | None | Inventory capacity | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:discount` | Discount | Passive | `ragnarmmo:enlarge_weight_limit` 3 | 10 | None | Vendor economy | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:overcharge` | Overcharge | Passive | `ragnarmmo:discount` 3 | 10 | None | Vendor economy | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:pushcart` | Pushcart | Active | `ragnarmmo:enlarge_weight_limit` 5 | 10 | SP | Cart/inventory | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:vending` | Vending | Active | `ragnarmmo:pushcart` 3 | 10 | SP | Shop UI/economy | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:buying_store` | Buying Store | Active | `ragnarmmo:vending` 1 | 1 | SP | Shop UI/economy | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:identify` | Identify | Active | None | 1 | SP | Item reveal | JSON+Java; no `level_data` |
| Merchant | `ragnarmmo:mammonite` | Mammonite | Active | None | 10 | SP/zeny | Currency cost | JSON+Java; no `level_data` |

## Known Layout Mismatches

The current JSON already shows several `skill_trees/*.json` positions that differ from each skill's `ui.grid_x` and `ui.grid_y`. The rework policy is to keep skill tree JSON as the single layout source and later add a validator for mismatches.

