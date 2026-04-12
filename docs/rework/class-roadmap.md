# Pre-Renewal Core Roadmap

Working branch: `rework/pre-renewal-core`

## Scope

The first milestone is first jobs only:

- Archer
- Swordsman
- Mage
- Acolyte
- Thief
- Merchant

Second jobs, third jobs, expanded jobs, and new feature work are frozen for this rework until the first-job core is stable.

## Delivery Order

| Sprint | Focus | Exit criteria |
| --- | --- | --- |
| 1 | Scope, policy, canonical matrix, legacy fallback guard | First-job canonical skills are documented and cannot be silently reconstructed from `SkillType`. |
| 2 | Data grammar, derived stats, reusable helpers | Projectile/control/economic adaptations have shared primitives instead of per-skill hacks. |
| 3 | Archer | Owl's Eye, Vulture's Eye, Improve Concentration, Double Strafing, and Arrow Shower are fully data-tuned and tested. |
| 4 | Swordsman + Mage | Melee/control and cast/projectile flows are validated against shared helper behavior. |
| 5 | Acolyte + Thief | Buff, support, reveal, stealth, FLEE, and poison behavior are normalized. |
| 6 | Merchant + cleanup | Currency/economy decisions are explicit and remaining legacy fallback is removed or isolated. |

## Class Notes

| Class | Why this order matters |
| --- | --- |
| Archer | Forces projectile velocity, gravity, spread, bow requirements, hit count, and ranged consistency decisions. |
| Swordsman | Exercises melee burst, aggro, knockback/flinch, and frontline defensive identity. |
| Mage | Exercises cast time, after-cast delay, interruption, elemental projectiles, and ground control. |
| Acolyte | Exercises healing, undead targeting, party/support buffs, reveal, and anti-projectile zones. |
| Thief | Exercises FLEE, proc passives, poison, stealth, and threat reset. |
| Merchant | Exercises zeny/currency, vendor hooks, inventory/capacity, and explicit economic costs. |

## Merge Rule

Do not merge the entire rework as one large change. Each class should land only after its data, Java behavior, UI tree, and validation tests agree.

