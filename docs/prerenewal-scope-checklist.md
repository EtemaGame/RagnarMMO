# RagnarMMO Pre-Renewal Scope Checklist

This document takes the big RO checklist and converts it into a project scope
that is usable for RagnarMMO as it exists today.

The goal is not "add everything that exists in RO".
The goal is:

- keep the pre-renewal identity
- adapt it to Minecraft where needed
- cut scope that does not belong in the current project
- define an order of work that avoids chaos

## 1. Scope freeze

This project should be treated as:

- RO pre-renewal inspired with a Minecraft adaptation layer
- novice + first jobs + second jobs as the core class target
- no hard dependency on one exact official episode patch
- pre-renewal generic baseline is the design authority
- no 3rd jobs
- no WoE in this project
- no castle economy in this project
- no guild war systems in this project

### Scope freeze v1

Version 1 of the project is:

- PvE-first
- singleplayer-safe
- cooperative multiplayer-safe
- dedicated-server-safe
- focused on the core RO loop:
  character progression
  jobs
  skills
  combat
  gear
  cards
  refine
  economy
  mob progression
  bosses

Version 1 is not:

- an MMO simulator
- a guild-war game
- a castle-ownership game
- a 3rd-job project

Primary play target:

- singleplayer
- cooperative PvE
- dedicated server multiplayer

Secondary / later target:

- PvP
- guild basic
- vending
- transcendent/rebirth after the core is stable

Explicitly excluded from this project:

- WoE
- castles
- Emperium systems
- guild dungeon / castle ownership loops
- large-scale war scheduling

If any of that is wanted later, it should be a separate expansion project.

### Design authority split

To avoid scope confusion, every feature should be classified as one of these:

- Canon RO:
  dual progression
  jobs
  classic stats
  hit/flee/crit/aspd
  HP/SP
  cards
  refine
  weight
  zeny
  parties
- Minecraft adaptation:
  existing mobs used as the PvE roster
  synthetic bosses and MVPs
  biomes/structures/dimensions acting as RO-like maps
  Kafra represented as service NPCs or service systems
- Technical concession:
  no custom mobs required for the core loop
  no WoE in this project
  no requirement to emulate official patch behavior 1:1 when it harms playability or maintainability

Separate project-local systems:

- Life Skills are not part of the RO core scope
- Life Skills must stay progression-separate from Base Level / Job Level
- Life Skills can coexist as side systems, but they are not allowed to redefine RO progression pacing

### Reference baseline

The combat and class target should be treated as:

- pre-renewal generic
- novice + first jobs + second jobs first
- transcendent/rebirth is future scope, not version-1 scope
- 3rd jobs are out of scope

That means any external references from Renewal-era pages are helper material only.
They are not the authority when they conflict with pre-renewal identity.

## 2. Minecraft adaptation rules

To keep the project sane, use these adaptation rules:

- Pre-renewal feel matters more than literal 1:1 recreation.
- Reuse vanilla and modded entities instead of depending on custom mobs.
- Use Minecraft zones, biomes, structures, and dimensions as the base for RO-like maps.
- Kafra should become a set of services:
  storage
  warp
  heal/save
- MVPs should be synthetic boss variants over existing entities.
- Headgear upper/mid/lower can be logical RO slots later, but they are not allowed to block core combat and progression.
- Do not force vanilla combat to remain the final authority.
- Do not build client-only gameplay systems.

## 3. Work rules

These rules exist to stop "a little here, a little there" development.

### Rule A - One core lane at a time

Only one of these can be the main active lane:

- player state and sync
- combat formulas and runtime
- gear progression
- PvE mob/world systems
- economy and commerce

### Rule B - One content lane at a time

While a core lane is active, only one content lane can be active:

- new job
- new skill batch
- new weapon family
- new boss batch
- new UI screen

### Rule C - Backend before content

Do not add:

- more jobs before class routing is stable
- more skills before cast/targeting/sync rules are stable
- more gear before refine exists
- more bosses before boss world state exists
- more UI before the server-side state is stable

### Rule D - Dedicated server is part of definition of done

Any system that touches gameplay is not done until:

- it works on dedicated server
- reconnect is safe
- death/respawn is safe
- dimension change is safe
- there is no obvious client trust bug

### Rule E - Data-driven first

Prefer data-driven definitions for:

- skills
- skill trees
- drops
- cards
- item rules
- mob species presets

Do not hardcode content when the architecture already supports data.

## 4. Status legend

- `[OK]` Implemented enough to count as a real system
- `[~]` Exists, but is partial / uneven / not closed
- `[ ]` Missing or still just a stub

## 5. Scope by system

### 5.1 Character identity

Overall status: `[~]` strong baseline

Already in repo:

- `[OK]` Base Level
- `[OK]` Base EXP
- `[OK]` Job Level
- `[OK]` Job EXP
- `[OK]` classic stats STR / AGI / VIT / INT / DEX / LUK
- `[OK]` stat points
- `[OK]` skill points
- `[OK]` first-job and second-job structure exists

Still missing to call this closed:

- `[~]` hard project definition for episode / temporal reference
- `[~]` class-route lock as a clear product rule
- `[~]` job change flow fully normalized as NPC / altar / quest logic
- `[~]` one unified RO player state facade over the current capabilities
- `[~]` explicit split between canon RO, Minecraft adaptation, and technical concession

### 5.2 Combat core

Overall status: `[~]`

Already in repo:

- `[OK]` hit vs flee logic
- `[OK]` crit logic
- `[OK]` ASPD calculations
- `[OK]` physical and magical damage pipelines
- `[OK]` cast bar / cast update support
- `[OK]` mana / SP-like resource handling
- `[OK]` server-side combat ownership is already the intended pattern

Still missing to call this closed:

- `[~]` one canonical combat runtime contract
- `[~]` clearer separation of auto-attack / cast / spammable skill delays
- `[~]` perfect dodge as a full gameplay system, not just math fragments
- `[~]` after-cast delay normalized across skills
- `[~]` full validation that animations match real attack speed

### 5.3 Derived substats and formulas

Overall status: `[~]`

Already in repo:

- `[OK]` HP / SP or mana-style resources
- `[OK]` ATK / MATK
- `[OK]` DEF / MDEF
- `[OK]` HIT / FLEE / CRIT
- `[OK]` ASPD
- `[OK]` regen logic exists
- `[OK]` stat computation layer exists

Still missing to call this closed:

- `[~]` formula freeze for pre-renewal target
- `[~]` clean separation of base formula, job bonuses, skill bonuses, gear bonuses, card bonuses, refine bonuses
- `[~]` validation and debug tools for balance tuning

### 5.4 Status ailments

Overall status: `[~]` partial

Already in repo:

- `[OK]` there are status-like skill effects and several debuff implementations
- `[OK]` poison / stun / blind / freeze / sleep / stone-like behavior exist in parts of the codebase

Still missing to call this closed:

- `[~]` one unified status system
- `[~]` consistent resistance model by stat / gear / boss immunity
- `[~]` normalized status rules instead of many per-skill approximations
- `[ ]` full catalog coverage for the status set you want to support

### 5.5 Elements, race, size, target properties

Overall status: `[~]`

Already in repo:

- `[OK]` element-aware skills exist
- `[OK]` some size / target-based bonuses exist
- `[OK]` some undead / target-family logic exists in skills

Still missing to call this closed:

- `[~]` one formal race taxonomy
- `[~]` one formal target size taxonomy as a first-class mob property
- `[~]` unified offensive/defensive element table
- `[~]` gear/card bonuses by race/size/element as a complete system
- `[~]` mob classification backbone built around race + element + size

This is one of the biggest missing pieces if the target is "feels like RO".

### 5.6 Skills

Overall status: `[~]` strong

Already in repo:

- `[OK]` active skills
- `[OK]` passive skills
- `[OK]` skill levels
- `[OK]` prerequisites through skill trees / progression data
- `[OK]` SP cost / resource logic
- `[OK]` multiple targeting and execution styles
- `[OK]` data-driven skill definitions
- `[OK]` data-driven skill trees
- `[OK]` hotbar / cast UX already exists

Still missing to call this closed:

- `[~]` consistency of validation across all skills
- `[~]` stronger weapon requirement enforcement
- `[~]` broad coverage for trap / summon / mobility / reveal / utility categories
- `[~]` cleaner failure feedback and blocked-state UX

### 5.7 Gear, slots, cards, refine

Overall status: `[~]`

Already in repo:

- `[OK]` RO item rules
- `[OK]` weapon restrictions by job and level
- `[OK]` card slots
- `[OK]` card items and card drops
- `[OK]` card compounding
- `[OK]` card tooltip visibility

Still missing to call this closed:

- `[OK]` real refine backend
- `[OK]` refine persistence and thresholds
- `[OK]` safe refine / failure outcome model
- `[~]` refine economy loop
- `[~]` full RO equipment slot model
- `[~]` shield / offhand / two-handed logic needs to stay aligned with combat
- `[~]` identification / appraisal as a real item progression system

Main note:

- the big missing piece is no longer refine backend
- it is refine integration into world flow, NPC/service flow, and final UX

### 5.8 Weight, consumables, storage

Overall status: `[~]`

Already in repo:

- `[OK]` weight system
- `[OK]` STR affects capacity
- `[OK]` overweight penalties exist

Still missing to call this closed:

- `[~]` full RO consumable ecosystem
- `[~]` potion / SP item pacing and UX
- `[ ]` ammo / arrows as a real system if archer identity depends on it
- `[ ]` storage / Kafra-style backend and UX
- `[ ]` persistent character or account storage rules

### 5.9 Mobs, PvE, bosses

Overall status: `[~]` strong baseline

Already in repo:

- `[OK]` mob level system
- `[OK]` mob stat generation
- `[OK]` mob tiers
- `[OK]` mob skill AI hooks
- `[OK]` species config / presets
- `[OK]` boss-like scaling already exists in the mob framework

Still missing to call this closed:

- `[OK]` formal mini-boss tier
- `[OK]` formal MVP tier
- `[~]` boss world-state persistence
- `[~]` controlled synthetic boss spawn rules
- `[~]` map/zonal identity beyond vanilla spawn feel
- `[ ]` dungeon / map routing with strong RO identity
- `[ ]` quest-driven PvE progression as a full system

### 5.10 Drops, loot, materials, crafting

Overall status: `[~]`

Already in repo:

- `[OK]` drops exist
- `[OK]` card drops exist
- `[OK]` loot modifiers exist
- `[OK]` zeny drop logic exists

Still missing to call this closed:

- `[~]` stronger drop identity by boss / mini-boss / map tier
- `[ ]` bestiary / drop log
- `[ ]` material loop that supports crafting without replacing the RO drop loop
- `[ ]` quest item / dungeon material structure

### 5.11 Economy, trade, vending

Overall status: `[~]`

Already in repo:

- `[OK]` zeny exists
- `[OK]` NPC-economy hooks exist
- `[OK]` merchant discount / overcharge direction exists
- `[~]` vending exists only as a lightweight skill stub

Still missing to call this closed:

- `[ ]` real player trade system with protections
- `[ ]` real vending system with inventory, prices, browsing, and persistence rules
- `[~]` unified economy service
- `[~]` cleaner economic sinks linked to refine, storage, warp, and consumables

### 5.12 Party and cooperative play

Overall status: `[OK]` baseline done, needs polish

Already in repo:

- `[OK]` party create / invite / join / leave
- `[OK]` party persistence
- `[OK]` party snapshots and sync
- `[OK]` XP sharing
- `[OK]` some party-aware skill logic exists

Still missing to call this closed:

- `[~]` stronger assist / support / shared combat logic
- `[~]` role validation in real group play
- `[~]` party UX polish

### 5.13 Guilds, PvP, WoE

Overall status:

- guild basic: `[ ]`
- PvP formal: `[ ]`
- WoE: excluded

Project rule:

- do not let guild/PvP work disrupt the core PvE roadmap
- WoE stays out

### 5.14 NPC services and world hubs

Overall status: `[ ]` as a real gameplay layer

Still missing:

- `[ ]` healer NPC/service
- `[ ]` storage NPC/service
- `[ ]` warp NPC/service
- `[ ]` refiner NPC/service
- `[ ]` card insert/remove NPC/service if desired
- `[ ]` job change NPC/service
- `[ ]` quest NPC structure

This does not mean there is zero UI.
It means there is not yet a clear RO-like service layer in the world.

### 5.15 UI and UX

Overall status: `[~]` strong

Already in repo:

- `[OK]` stats UI
- `[OK]` skills UI
- `[OK]` cast overlay
- `[OK]` hotbar overlay
- `[OK]` achievements UI
- `[OK]` party client data / party HUD direction

Still missing to call this closed:

- `[~]` final HUD composition for HP/SP/cast/party/weight
- `[~]` more complete tooltips for formulas and restrictions
- `[ ]` storage / vendor / refine / NPC service UI layers
- `[ ]` bestiary / drop log / death recap style QoL if desired

### 5.16 Persistence and multiplayer robustness

Overall status: `[~]`

Already in repo:

- `[OK]` stats persistence
- `[OK]` skills persistence
- `[OK]` party saved data
- `[OK]` server-authoritative packet direction is already the intended architecture

Still missing to call this closed:

- `[OK]` sync by domains instead of coarse dirty sync
- `[~]` boss/event SavedData
- `[ ]` deeper anti-dupe and economy safety review
- `[ ]` stronger audit logging for economy and combat

### 5.17 Config and data-driven support

Overall status: `[~]` strong

Already in repo:

- `[OK]` skill data
- `[OK]` skill trees
- `[OK]` item rules
- `[OK]` cards
- `[OK]` species config
- `[OK]` a lot of content already lives in data files

Still missing to call this closed:

- `[~]` more formula/rate configurability
- `[OK]` refine config
- `[ ]` broader economy rates and sinks config
- `[ ]` boss/MVP respawn and event config as first-class data

## 6. What is required for "RO pre-renewal feel"

If we strip the project down to what most strongly creates the RO feeling,
these are the must-have systems.

### Absolute must-have

- dual progression: base + job
- closed class identity
- STR / AGI / VIT / INT / DEX / LUK
- HIT / FLEE / CRIT / ASPD
- HP / SP or mana equivalent
- active and passive skills
- card system
- weight system
- zeny economy
- parties
- mobs with RO-like classification and counters

### Must-have soon after

- refine
- element interactions
- status ailments
- mini-boss / MVP structure
- storage / Kafra-like services
- real NPC service layer

### Can wait until later

- transcendent / rebirth
- guild basic
- formal PvP
- vending

### Out of scope here

- WoE

## 7. Current project reading

If the question is "does the project already feel RO?", the answer is:

- yes, partially
- not yet fully

Today the project already has a real RO-like identity in:

- character stats and progression
- jobs
- skills
- cards
- weight
- party
- mob scaling

Today the project still misses the strongest RO pillars in:

- race/element/size as a formal combat language
- world NPC service layer
- storage
- real vending / trade economy
- controlled boss lifecycle and boss presentation

## 8. Reordered execution plan

This is the order that makes the most sense from the current repo state.

### Lane 1 - Foundation freeze

Goal:

- stop architectural drift

Deliver:

- player-state facade over existing capabilities
- sync domains instead of one coarse dirty flag
- formula freeze for base combat and derived stats
- stronger log categories

Current read:

- partly completed
- this lane should stay in maintenance mode, not as the main active lane

Do not do in parallel:

- new jobs
- new bosses
- more UI screens unless needed for debugging

### Lane 2 - Combat freeze

Goal:

- make combat consistently feel pre-renewal

Deliver:

- final hit/flee/crit/ASPD behavior
- cast / after-cast / interrupt contract
- clearer status-system architecture
- formal element pipeline

Do not do in parallel:

- broad new content batches

### Lane 3 - Gear progression freeze

Goal:

- finish the core RO item loop

Deliver:

- refine
- refine economy sink
- item progression thresholds
- identification/appraisal if kept in scope
- gear/card/refine modifier order

This lane is mandatory before calling the project "serious RO adaptation".

Current read:

- refine backend is already in
- the remaining work is service/UI integration and economy tightening

### Lane 4 - PvE identity freeze

Goal:

- make the world itself feel RO

Deliver:

- formal race / element / size mob taxonomy
- mini-boss and MVP tiers
- boss/event world state
- special drops and boss loot identities
- basic quest layer if you want structured PvE progression

Current read:

- `MINI_BOSS` and `MVP` tiers already exist
- active boss SavedData baseline already exists
- this is now the best main lane to continue

### Lane 5 - Service and economy layer

Goal:

- make the game loop feel like RO outside pure combat

Deliver:

- Kafra-like storage
- warp / heal / refiner / job service NPCs
- cleaner zeny service
- player trade
- real vending later if still wanted

### Lane 6 - Expansion lane

Only after the above:

- more second jobs
- transcendent/rebirth
- guild basic
- formal PvP
- more map/dungeon content

## 9. Practical anti-chaos rule

For every sprint, write exactly these four lines before coding:

1. Core lane:
2. Content lane:
3. What is explicitly out of scope this sprint:
4. Definition of done:

If you cannot answer those four lines clearly, do not start the sprint.

## 10. Resume Here

If you stop now and resume later, start from this exact sprint frame:

1. Core lane: PvE identity freeze
2. Content lane: boss lifecycle / controlled spawns
3. What is explicitly out of scope this sprint:
   new jobs
   broad UI expansion
   guild/PvP/WoE work
4. Definition of done:
   active bosses persist cleanly
   respawn timers exist
   controlled spawn metadata exists
   `MINI_BOSS / BOSS / MVP` behave differently in lifecycle and reward terms
   dedicated server remains stable

Concrete next tasks:

- extend boss SavedData with respawn timers
- define controlled spawn sources for synthetic bosses
- add boss presentation layer like boss bar / high-rank feedback
- continue race / element / size taxonomy so counters and cards feel more RO
