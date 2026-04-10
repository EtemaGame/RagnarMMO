# System Extension Guide

This guide explains where to make changes when you want to extend RagnarMMO after the current cleanup.

## 1. Add a manual mob

Use the manual datapack path when you want a mob with authored level, rank, taxonomy, and combat stats.

### Data entry points

- `data/<namespace>/mob_templates/*.json`
- `data/<namespace>/mob_definitions/*.json`

### Runtime flow

1. `MobDefinitionDataLoader.java` loads templates and definitions.
2. `ManualMobProfileResolver.java` resolves them into a `ComputedMobProfile`.
3. `ManualMobProfileRuntimeIntegrator.java` attaches that profile to the entity.
4. `ComputedMobProfileAttributeApplier.java` writes the runtime-facing attributes.
5. `MobLevelManager.java` reports the effective method as `MANUAL`, so the automatic selector is bypassed.

### Authoring rule

For production-ready manual mobs, author `direct_stats`.

`ro_stats` are still useful:

- for documentation
- for partial derivation
- for base combat semantics

But broad full derivation from `ro_stats` to every final combat stat is still not the primary authoring contract.

## 2. Change automatic difficulty

Automatic mob difficulty is configured in `MobConfig.java` and resolved in `MobLevelManager.java`.

### Supported automatic modes

- `DISTANCE`
- `BIOME_DISTANCE`
- `PLAYER_LEVEL`

### Important

`MANUAL` is not selected here. It is resolved by datapack/manual runtime coverage.

## 3. Add a new skill

### Data entry points

- `data/<namespace>/skills/*.json`
- `data/<namespace>/skill_families/*.json`
- `data/<namespace>/skill_trees/*.json`

### Java entry points

- `SkillDataLoader.java`
- `SkillTreeDataLoader.java`
- `SkillRegistry.java`
- `SkillEffectHandler.java`

### Typical workflow

1. Add the skill JSON.
2. Reuse or create a skill family if needed.
3. Add an effect class under `src/main/java/com/etema/ragnarmmo/skill/job/...` if the skill needs runtime behavior.
4. Reference the effect through `effect_class`.
5. Add the skill to a skill tree.
6. Reload and verify it appears in `SkillRegistry`.

## 4. Expand an existing second job

Second-job support already exists in:

- `JobType.java`
- `JobBonusData.java`
- `data/ragnarmmo/skill_trees/*_2.json`

Typical work:

1. Add or update skill definitions.
2. Add or update the second-job tree layout.
3. Implement any missing runtime effects.
4. Adjust job bonus tables if needed.
5. Test promotion flow through `PacketChangeJob.java`.

## 5. Add a brand-new job family

This is larger than content authoring.

Minimum touchpoints:

- `JobType.java`
- `JobBonusData.java`
- job change flow and UI
- new skill tree JSON
- new skill definitions
- new runtime effects

Treat this as a small feature project, not as a simple data patch.

## 6. Add item and progression content

Useful data roots:

- `data/ragnarmmo/ro_item_rules`
- `data/ragnarmmo/weapon_attributes`
- `data/ragnarmmo/cards`
- `data/ragnarmmo/life_points_sources`
- `data/ragnarmmo/xp_sources`

## 7. What not to reintroduce

Do not reintroduce:

- per-species TOML mob difficulty/manual behavior
- `MANUAL_SPECIES`
- old `mob_species.toml` as an authoring path

If you need manual mob content, use datapack `mob_definitions` and `mob_templates`.
