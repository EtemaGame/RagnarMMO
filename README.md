# RagnarMMO

RagnarMMO is a Minecraft mod inspired by classic Ragnarok Online systems: base/job progression, RO-style stats, data-driven skills, item rules, mob taxonomy, and a full manual mob authoring path.

This repository is no longer documented as a "wishlist". This README reflects the current state of the codebase.

## Current status

### Implemented and usable

- Base Level and Job Level progression
- RO-style stat allocation (`STR`, `AGI`, `VIT`, `INT`, `DEX`, `LUK`)
- Derived combat formulas and custom combat engine
- Novice and first-job progression
- Second-job promotion framework, second-job job tables, second-job skill trees, and a large amount of second-job skill content
- Data-driven skills loaded from datapacks/resources
- Data-driven skill trees loaded from datapacks/resources
- RO item rules, weapon attributes, and card data
- Life Skills with progression and perk choices
- Party system and party HUD
- Four effective mob difficulty methods:
  - `DISTANCE`
  - `BIOME_DISTANCE`
  - `PLAYER_LEVEL`
  - `MANUAL`
- New manual mob datapack path with runtime application of real combat stats

### Implemented but still not "finished content"

- Second jobs are present in code and playable, but they should still be treated as an expanding content/balance area, not as "fully content-complete MMO parity"
- Merchant-side vending/shop gameplay is still partial
- The manual mob path is production-ready when authored with `direct_stats`, but full broad derivation from `ro_stats` to every final combat stat is still not complete
- Legacy compatibility layers still exist around some older mob/storage systems, even though the old `MANUAL_SPECIES` difficulty path was removed from the active system

### Not implemented yet

- Transcendent / Rebirth jobs
- Third jobs
- Formal PvP
- Guild systems
- War of Emperium
- Castle / Emperium gameplay loops

## How the game works

### 1. Character progression

Players progress in two tracks:

- Base Level: grants status points for `STR`, `AGI`, `VIT`, `INT`, `DEX`, `LUK`
- Job Level: grants skill points for the active class tree

You start as `Novice`.

- At Job Lv. 10, you can promote from `Novice` to a first job
- By default, at first-job Job Lv. 40, you can promote to a second job
- Job change also requires no unspent skill points
- Novice -> first-job promotion also requires `Basic Skill` level 9

Key code areas:

- `PacketChangeJob.java`
- `JobType.java`
- `JobBonusData.java`

### 2. Combat model

Combat is not vanilla-first. RagnarMMO computes RO-style derived stats and resolves:

- attack and defense
- hit and flee
- crit
- cast time and cast delay
- job/resource-specific mana or SP logic
- weapon/class interactions

Key code areas:

- `CombatMath.java`
- `StatComputer.java`
- `RagnarCombatEngine.java`

### 3. Jobs and skills

Jobs are modeled in code, but skills are primarily data-driven:

- skill definitions live in `data/<namespace>/skills/*.json`
- skill tree layouts live in `data/<namespace>/skill_trees/*.json`
- effects are instantiated through `effect_class`

The repository already contains:

- first-job trees such as `novice_1.json`, `mage_1.json`, `merchant_1.json`
- second-job trees such as `wizard_2.json`, `knight_2.json`, `hunter_2.json`

Key code areas:

- `SkillDataLoader.java`
- `SkillTreeDataLoader.java`
- `SkillRegistry.java`
- `SkillEffectHandler.java`

### 4. Mobs and difficulty

The active system now has four effective methods:

- `DISTANCE`: level is resolved from configured distance bands
- `BIOME_DISTANCE`: level is resolved from biome-specific bands first, then generic distance bands
- `PLAYER_LEVEL`: level is anchored to nearby players, then to nearest valid player, then dimension floor
- `MANUAL`: datapack-authored manual mob profile, bypassing automatic difficulty selection

Important:

- `MANUAL` is not selected through `LEVEL_SCALING_MODE`
- `LEVEL_SCALING_MODE` now only chooses the three automatic methods
- old per-species TOML manual behavior was removed from the active system

Key code areas:

- `MobConfig.java`
- `MobLevelManager.java`
- `MobSpawnHandler.java`

### 5. Manual mobs by datapack

The manual path is the new authoritative authored mob path.

Runtime flow:

1. Datapack resources are loaded from `mob_templates` and `mob_definitions`
2. Definitions are resolved into a `ComputedMobProfile`
3. Runtime integration attaches that profile to the entity
4. Runtime attributes and combat reads consume that resolved profile
5. Manual mobs bypass automatic difficulty selection

Current authoring rule:

- if you want a manual mob to be production-ready today, author `direct_stats`
- `ro_stats` are useful and partially consumed, but broad full derivation is still not complete for every final field

Key code areas:

- `MobDefinitionDataLoader.java`
- `ManualMobProfileResolver.java`
- `ManualMobProfileRuntimeIntegrator.java`
- `ComputedMobProfileAttributeApplier.java`
- `MeleeAttackGoalMixin.java`

### 6. Life Skills

Life Skills are implemented as a parallel progression system with their own point sources and perk choices.

Current tracked skills include:

- Mining
- Woodcutting
- Excavation
- Farming
- Fishing
- Exploration

Key code areas:

- `LifeSkillsModule.java`
- `LifeSkillManager.java`
- `LifeSkillPerkRegistry.java`

### 7. Party, items and content layers

The repo also contains:

- party service, commands, and HUD
- RO item rules
- weapon attribute data
- mob cards
- loot and XP sources

Relevant content roots:

- `src/main/resources/data/ragnarmmo/ro_item_rules`
- `src/main/resources/data/ragnarmmo/weapon_attributes`
- `src/main/resources/data/ragnarmmo/cards`
- `src/main/resources/data/ragnarmmo/xp_sources`

## Quick start tutorial

### For players

1. Start as `Novice`
2. Open the stats/skills UI and spend status points
3. Raise Job Level to 10
4. Learn `Basic Skill` to level 9
5. Change into a first job
6. Keep leveling Job Level until the configured second-job threshold
7. Promote into a second job once you have no unspent skill points
8. Use `/mobstats` if you want to inspect how a mob is being resolved

### For server owners

1. Configure automatic mob difficulty in `ragnarmmo-mobstats.toml`
2. Pick one automatic mode:
   - `DISTANCE`
   - `BIOME_DISTANCE`
   - `PLAYER_LEVEL`
3. Use datapacks for manual authored mobs
4. Do not rely on old species TOML manual behavior anymore

## How to extend the project later

### Add a new manual mob

1. Create datapack resources under:
   - `data/<namespace>/mob_templates/*.json`
   - `data/<namespace>/mob_definitions/*.json`
2. Author `entity`, `rank`, `level`, taxonomy, and `direct_stats`
3. Reload datapacks
4. Verify the entity gets a `ComputedMobProfile`
5. Verify the mob bypasses automatic difficulty and uses manual runtime stats

If you need examples or field semantics, use these reference files from the repository:

- `mob-datapack-spec.md`
- `mob-taxonomy-spec.md`
- `mob-stat-derivation-spec.md`

### Add a new skill

1. Add a JSON definition in `data/<namespace>/skills`
2. Optionally add or reuse a skill family in `data/<namespace>/skill_families`
3. If the skill needs behavior, implement an effect class under `src/main/java/com/etema/ragnarmmo/skill/job/...`
4. Reference that class through the skill's `effect_class`
5. Add the skill to a tree in `data/<namespace>/skill_trees`
6. Reload and verify it appears in `SkillRegistry`

### Add or expand a second job

For content extension of an existing second job:

1. Add or update skill JSONs
2. Update the corresponding `*_2.json` skill tree
3. Add Java effects for any new active/passive behavior
4. Update job bonus data if needed

For a brand-new job family:

1. Add the job in `JobType.java`
2. Add bonus tables in `JobBonusData.java`
3. Create a skill tree JSON
4. Add skill definitions and effects
5. Review job-change flow, UI strings, and allowed-job checks

This is a bigger change than adding a skill or manual mob. It is not just data.

### Add new item-rule systems

Use these data roots:

- `ro_item_rules` for job/stat/weapon rules
- `weapon_attributes` for authored weapon behavior
- `cards` for card data
- `life_points_sources` and `xp_sources` for progression data

## What to treat as deprecated or removed

- `MANUAL_SPECIES` is no longer an active difficulty method
- `mob_species.toml` is no longer part of the supported mob-difficulty flow
- new mob authoring should use manual datapack definitions, not old species config

## Documentation map

- `docs/README.md`
- `docs/system-extension-guide.md`
- `docs/design/mob-datapack-spec.md`
- `docs/design/mob-taxonomy-spec.md`
- `docs/design/mob-stat-derivation-spec.md`
