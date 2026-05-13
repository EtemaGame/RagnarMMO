# RagnarMMO Stabilization Guardrails

## Freeze Rules

During stabilization, only the following changes should land:

- critical bugs and crashes
- build fixes
- broken data fixes
- low-risk refactors
- tests and validation tooling

Do not add large content, new systems, complex skills, new jobs, or new mobs until the stabilization criteria are met.

## Rollback Branches

Use one branch or tag per phase:

- `stabilization/init-cleanup`
- `stabilization/network-contracts`
- `stabilization/combat-formulas`
- `stabilization/skills-xp`
- `stabilization/mobs-declarative`

## Packet Policy

The current packet order is locked by a source snapshot test. The target ID ranges are documented in `Network`, but they must not be applied until the baseline snapshot exists and a protocol migration is intentional.

Only bump `PROTOCOL` when the packet contract changes in a way that should reject old clients or servers.

## Legacy Compatibility

Current compatibility aliases are audited in `docs/LEGACY_COMPATIBILITY.md`.
Do not remove command aliases, mixin aliases, or skill data aliases without
updating that audit and planning a deprecation or migration path.

## Beta Smoke Checklist

Before each beta release:

1. Create a new world.
2. Enter as Novice.
3. Open `R`, `V`, `K`, `Y`, and `B`.
4. Assign stats.
5. Learn a skill.
6. Use a skill from the hotbar.
7. Kill a mob.
8. Verify base, job, and skill EXP.
9. Create a party.
10. Test party chat.
11. Obtain an item or card.
12. Refine an item.
13. Die and relog.
14. Verify persisted stats, skills, zeny, and critical party state.
15. Join a dedicated server.
