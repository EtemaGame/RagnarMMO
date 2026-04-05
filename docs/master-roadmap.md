# RagnarMMO Master Roadmap

This document turns the project vision into a repo-grounded roadmap.
It is based on the current codebase state, not on a clean-sheet rewrite.

Last repo-grounded update:

- player facade baseline exists
- sync domains exist for player stats/progression/resources
- refine backend exists
- mob ranks now include `MINI_BOSS` and `MVP`
- active boss world state baseline exists through SavedData

Companion document:

- `docs/prerenewal-scope-checklist.md` - project scope freeze, gap checklist, and work-order rules for the pre-renewal target

## 1. Product thesis

RagnarMMO should not try to port Ragnarok Online 1:1.

The correct product target is:

- RO systems layered over Minecraft
- server-authoritative gameplay
- vanilla and modded entities reused as the PvE foundation
- RO identity preserved through stats, jobs, skills, gear, cards, economy, and boss variants

Core statement:

> Pre-Renewal inspired over Pre-Renewal literal.

Scope freeze:

- version-1 target is PvE-first and co-op friendly
- novice + first jobs + second jobs are the class scope for the core release
- transcendent/rebirth is later scope, not core-release scope
- 3rd jobs are out of scope
- WoE, castles, and guild-war systems are out of scope for this project

## 2. Technical baseline

Confirmed baseline for this repo:

- Forge 1.20.1
- Official mappings
- Java 17
- target support: singleplayer, LAN/multiplayer, dedicated server

Non-negotiable engineering rules:

- Do not mix Forge APIs with NeoForge or Fabric code.
- Keep gameplay authority on the server.
- Do not make custom mobs a dependency of the core loop.
- Do not design only for singleplayer.
- Do not prioritize guild/WoE/vending-style endgame before combat and progression are stable.

## 3. Current repo reality

The repo is already beyond "phase 0 prototype". It already contains:

- modular bootstrap in `RagnarMMO` with `CoreModule`, `StatsModule`, `MobStatsModule`, `SkillsModule`, `LifeSkillsModule`, `RoItemsModule`, and economy registration
- player stats capability in `system/stats/capability/PlayerStatsProvider`
- player skills capability in `skill/runtime/PlayerSkillsProvider`
- shared `SimpleChannel` in `common/net/Network`
- stats, skills, cast, party, mob, RO item, achievement, and visual effect packets
- saved world state for parties in `system/stats/party/PartySavedData`
- saved world state for placed-block / life-skill tracking
- data-driven skill loading in `skill/data/SkillDataLoader`
- data-driven skill tree loading in `skill/data/tree/SkillTreeDataLoader`
- data-driven RO item rules in `roitems/data/RoItemRuleLoader`
- data-driven cards in `system/loot/cards/CardDataLoader`
- mob stat capability and spawn scaling in `system/mobstats`
- party system, UI screens, achievements, life skills, custom weapons, card compounding, and zeny drops

This matters because the next roadmap must extend the current architecture, not restart it.

## 4. Status by phase

Status legend:

- Complete: foundation exists and is usable
- In progress: system exists but still has structural gaps
- Pending: little or no production implementation yet

### Phase 0 - Base architecture

Status: Complete

What is already in place:

- modular boot sequence
- shared network channel
- server/client separation by module and event subscribers
- config system
- client UI package separation
- dedicated-server-safe core loading pattern

Main note:

- logging exists, but the project would still benefit from a more explicit log taxonomy like `[RO-MOD][NET]`, `[RO-MOD][PLAYER]`, `[RO-MOD][MOB]`

### Phase 1 - Player data

Status: In progress

What exists:

- player stats capability
- player skills capability
- clone handling on death
- respawn sync
- dimension-change sync
- login sync for skills and stats through active event flows

Main gaps:

- player state is still split internally between capabilities, even though there is now a facade baseline
- `IRoPlayerData` / `RoPlayerDataAccess` exists as a first unification layer, but it is not yet the sole access path
- equipment-facing and broader domain ownership are still not folded into one stable player-state contract

Recommended direction:

- do not rewrite the current capabilities
- keep expanding the facade/service layer until central systems stop reaching directly into fragmented state

### Phase 2 - Network and synchronization

Status: Good baseline / In progress

What exists:

- shared `SimpleChannel`
- packet registration split by subsystem
- C2S skill use packets
- S2C stats/derived stats sync
- party packets
- mob sync packets
- RO item sync packets
- sync domains for player `stats`, `progression`, and `resources`
- centralized player stats sync service

Main gaps:

- sync is no longer only a coarse `dirty` boolean for player stats, but the domain model is still incomplete
- skills, equipment, and UI-facing state are not yet part of the same sync-domain contract
- some orchestration still lives in subsystem code even though the main stats path now has a central sync service

Recommended direction:

- keep the current channel
- extend the current dirty-domain model to skills/equipment/UI ownership where it adds value
- keep collapsing ad-hoc sync paths into central services

### Phase 3 - Stats and derived formulas

Status: In progress

What exists:

- STR / AGI / VIT / INT / DEX / LUK
- base level and job level
- base EXP and job EXP
- skill points and stat points
- mana and SP style resources
- derived stat computation via `StatComputer` and `CombatMath`

Main gaps:

- formulas already have RO flavor, but they are not yet a locked canonical math contract
- some calculations still use Minecraft-friendly shortcuts
- there is not yet a clear separation between "core formula", "job bonus", "skill modifier", and "gear/card/refine modifier" stages

Recommended direction:

- freeze a math contract before adding more classes and bosses
- treat formulas as a first-class system with debug logging and validation

### Phase 4 - Core combat

Status: In progress

What exists:

- hit vs flee logic
- crit logic
- derived ASPD
- cast handling
- mana/SP consumption
- mob combat handling
- skill damage helpers
- misses and visual feedback

Main gaps:

- combat still behaves like a progressive replacement over vanilla, not a fully coherent RO combat runtime
- cast interruption, after-cast delay, melee cadence, and server authority need a tighter contract
- dedicated-server parity should be treated as a stability goal, not assumed

Recommended direction:

- continue with progressive replacement
- do not pivot to an aggressive full rewrite of vanilla combat hooks

### Phase 5 - Jobs and progression

Status: In progress

What exists:

- novice and first jobs
- several second-job entries
- job switching packet flow
- job-aware resource semantics
- job-based skill trees and progression data

Main gaps:

- class coverage is uneven
- promotion flow and validation need to be treated as product logic, not only packet logic
- some second-job content is present in data but not yet uniformly production-ready

Recommended direction:

- stabilize first jobs and a narrow second-job slice before expanding breadth

### Phase 6 - Skills backend

Status: Advanced / In progress

What exists:

- data-driven skill definitions
- data-driven skill trees
- runtime skill registry and effect factory
- multiple skill execution modes
- cast updates
- hotbar packets
- many implemented job skill effects

Main gaps:

- the backend is broad, but consistency is more important than adding more skills
- skill validation rules should be tightened around range, targeting, interrupt, weapon restrictions, and cooldown ownership
- new visuals and effects are growing fast, so validation and logging need to keep up

Recommended direction:

- treat the skill backend as a core platform and harden it before content expansion

### Phase 7 - Equipment / cards / refine

Status: In progress

What exists:

- RO item rules loaded from JSON
- required base level restrictions
- allowed job restrictions
- card slots
- card item registry and drop data
- card compounding packet and UI
- tooltip rendering for slots and inserted cards
- refine persistence and bonus math
- refine service with chance, safe zone, downgrade behavior, material and zeny cost hooks

Main gaps:

- refine already exists as backend, but still needs world/NPC/UI integration
- item progression still needs better live-game visibility and economy integration around refine
- item progression currently leans more on rules/cards than on a full refine gameplay loop

Recommended direction:

- keep refine as an extension of the existing RO item rule/NBT pipeline
- next step is refiner interaction flow, not a parallel item-progression rewrite

### Phase 8 - Economy / weight / drops

Status: In progress

What exists:

- zeny items
- zeny drops
- villager/emerald replacement hooks
- weight system
- overweight penalties
- life-skill and loot-related progression hooks

Main gaps:

- economy currently uses physical zeny items, not an abstract wallet capability
- weight is primarily tag/constants driven, not yet a configurable JSON-first resolver
- the farm loop exists, but it is not yet unified into one economy service

Recommended direction:

- do not hard pivot to pure abstract currency right now
- the realistic next step is a hybrid economy service that can support both wallet semantics and physical zeny items if needed

### Phase 9 - Existing-entity mob system

Status: Advanced / In progress

What exists:

- mob capability
- spawn-time stat generation
- tier assignment
- extended rank model with `MINI_BOSS` and `MVP`
- species TOML overrides
- mob levels
- class-based stat weighting
- mob skill AI hooks
- client sync and UI integration

Main gaps:

- race / element / size are not yet unified as the main classification backbone
- controlled synthetic boss spawning still needs stronger rules and orchestration
- boss lifecycle is now persisted, but not yet fully expanded into respawn/event systems

Recommended direction:

- evolve the current mob system instead of replacing it
- add taxonomy fields and extend the existing tier model

### Phase 10 - Global world state

Status: Good baseline / In progress

What exists:

- `PartySavedData`
- life-skill saved trackers
- active boss SavedData baseline for tracked `MINI_BOSS+` instances

Main gaps:

- there is now dedicated boss lifecycle persistence, but it only tracks active instances
- respawn timers, event rotation, boss ownership/metadata, and controlled spawn orchestration are still missing

Recommended direction:

- evolve the current boss SavedData into a broader boss/event world-state service before shipping synthetic bosses widely

### Phase 11 - Compatibility with mob/boss mods

Status: Partial

What exists:

- enough mob architecture to support compat
- species config and mob sync patterns that can be extended

Main gaps:

- no clearly separated compat modules by mod id
- no whitelist-driven external preset layer yet
- no mature fallback story for external entity classification

Recommended direction:

- compat must be modular and optional
- whitelist external mobs explicitly

### Phase 12 - Party and cooperative play

Status: Good baseline / In progress

What exists:

- party creation and persistence
- invites
- party sync
- party XP sharing service
- party-aware skill behavior in some skills

Main gaps:

- assist/heal/buff rules still need broader systemic validation
- party UX can grow, but backend stability matters more first

### Phase 13 - UI / HUD / controls

Status: Advanced / In progress

What exists:

- stats, skills, achievements, cast overlay, and other UI screens
- hotbar/skill input support
- mob bar and client packet handlers

Main gaps:

- UI breadth is ahead of some backend contracts
- HUD should increasingly mirror server truth, not infer too much locally

Recommended direction:

- keep client code focused on render/input/feedback

### Phase 14 - Endgame systems

Status: Pending

Do not prioritize yet:

- guilds
- formal PvP
- WoE/proto-WoE
- vending as a persistent economy pillar
- castle ownership style systems

## 5. Strategic gaps to close next

These are the highest-value gaps from the current repo state.

### Gap A - Player state facade

Problem:

- player state is functionally rich but architecturally fragmented

Goal:

- expand the existing facade into one stable RO player domain API without rewriting working capabilities

### Gap B - Sync domains

Problem:

- the backend now has sync domains for core player stats, but not yet for all gameplay domains

Goal:

- complete the per-domain dirty model and keep centralizing sync

### Gap C - Refine

Problem:

- refine exists as backend, but not yet as a fully surfaced progression loop

Goal:

- add refiner-facing interaction flow, world integration, and clearer progression UX

### Gap D - Mob taxonomy v2

Problem:

- the current mob system is strong but does not yet match the intended RO identity model

Goal:

- add race, element, size, and extended rank model over the existing mob capability

### Gap E - Boss world state

Problem:

- active boss state now exists, but full boss/event orchestration does not

Goal:

- evolve current SavedData into boss respawns, controlled spawns, and event metadata

## 6. Recommended sprint order from today

This sprint order assumes the current repo is the starting point.

### Sprint A - Stabilize player authority and sync

Deliverables:

- create an RO player facade/service over existing capabilities
- introduce sync dirty domains
- centralize player sync dispatch
- verify login, respawn, dimension change, reconnect, and dedicated-server parity

Exit criteria:

- no double-execution of skill requests
- no stale HUD data after reconnect
- fewer ad-hoc sync calls spread across subsystems

Current state:

- partially complete

### Sprint B - Lock combat contract

Deliverables:

- define the canonical combat pipeline
- document and normalize hit, miss, crit, cast, interrupt, after-cast delay, and resource spending
- add debug logging switches for combat math

Exit criteria:

- melee and skill combat feel deterministic across singleplayer and dedicated server
- formulas stop drifting subsystem by subsystem

### Sprint C - Finish gear progression foundation

Deliverables:

- implement refine persistence and bonus application
- define safe zone + downgrade behavior
- connect refine to current RO item pipeline
- verify card and refine bonuses do not duplicate on equip/unequip

Exit criteria:

- gear progression is no longer "rules + cards only"

Current state:

- backend complete enough to move to integration/UI/service work

### Sprint D - Mob taxonomy v2 and synthetic bosses

Deliverables:

- extend mob classification to race / element / size / rank
- expand rank model to `NORMAL / ELITE / MINI_BOSS / BOSS / MVP`
- add controlled synthetic boss spawns
- add boss/event SavedData

Exit criteria:

- a vanilla mob can become a stable miniboss or boss with persistent server-managed state

Current state:

- rank expansion and boss persistence baseline are in place
- the remaining work is taxonomy completion and controlled boss lifecycle

### Sprint E - Economy and drops v2

Deliverables:

- formalize economy service
- decide hybrid wallet + item zeny semantics
- improve drop loop around boss/special loot
- move weight resolution toward data-driven config where it adds value

Exit criteria:

- kill -> loot -> improve loop is stable and easier to tune

### Sprint F - Compat modules

Deliverables:

- add opt-in compat modules by mod id
- whitelist external boss candidates
- add preset-based classification for external entities

Exit criteria:

- compat expands content without becoming a hard dependency

## 7. What not to do now

Do not spend the next major cycle on:

- fully custom mobs as core content
- highly custom boss AI trees
- wide endgame social systems
- 20 jobs at once
- large-scale WoE systems
- a full economy rewrite before refine and boss state are stable

## 8. Final project direction

The correct direction for this repo is not "build Ragnarok from zero inside Minecraft".
It is:

- stabilize the existing modular Forge architecture
- harden server-authoritative player/combat systems
- complete gear progression with refine
- evolve the current mob framework into RO taxonomy + synthetic bosses
- use SavedData for boss/event world state
- expand outward to compat and larger multiplayer loops only after the core is stable

In short:

- first stabilize stats, combat, jobs, skills, gear, economy, and existing-entity bosses
- then expand party, quests, compat, and UX
- only much later touch guild/PvP/WoE-style endgame

## 9. Resume Here

If work stops here and resumes later, the next recommended starting point is:

- core lane: `PvE identity freeze`
- content lane: `boss lifecycle / controlled spawns`
- explicitly out of scope: new jobs, wide UI expansion, guild/PvP work
- definition of done:
  boss SavedData tracks active instances, respawns, and cleanup safely
  synthetic bosses have controlled spawn rules
  `MINI_BOSS / BOSS / MVP` have distinct lifecycle behavior
  no regressions on dedicated server

Practical next backlog:

- add respawn timers and spawn metadata to boss SavedData
- define controlled spawn sources for synthetic bosses
- add boss bar / high-rank presentation
- continue race / element / size taxonomy so PvE counters feel more RO
