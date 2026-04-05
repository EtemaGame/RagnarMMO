# RagnarMMO Combat Implementation TODO

Status: execution checklist
Depends on: `docs/COMBAT_ARCHITECTURE.md`
Audience: human contributors and AI coding agents

---

## 1. Purpose

This document is the execution plan for implementing the combat architecture defined in `COMBAT_ARCHITECTURE.md`.

This file is intentionally explicit.

It exists to prevent:
- architecture drift
- random partial rewrites
- over-reliance on vanilla melee behavior
- Better Combat integration leaking into the whole codebase
- per-skill one-off combat pipelines

If there is any conflict between this file and `COMBAT_ARCHITECTURE.md`, the architecture document wins.

---

## 2. Global execution rules for agents

Before changing any combat code, the agent MUST follow these rules.

### Rule A
Read `docs/COMBAT_ARCHITECTURE.md` first.

### Rule B
Do not directly implement new combat behavior inside unrelated classes if the corresponding combat service does not exist yet.

### Rule C
Do not use `Player.attack(Entity)` as the final design solution for RO-like attacks or skills.

### Rule D
Do not create new Better Combat dependent logic outside a dedicated bridge package.

### Rule E
Do not implement a skill-specific combat shortcut if the shared combat engine can own it.

### Rule F
When in doubt, prefer centralization over convenience.

---

## 3. Work order summary

The work MUST be done in this order unless there is a clearly documented reason to deviate.

1. Foundation scaffolding
2. Combat state and packet layer
3. Core combat engine
4. Better Combat bridge
5. Basic attack migration
6. Skill request/resolution migration
7. Multihit stabilization
8. Feedback and debug instrumentation
9. Cleanup and enforcement

The agent MUST NOT jump directly to late-stage skills without the shared combat engine in place.

---

## 4. Phase 1 - Foundation scaffolding

## Task 1.1 - Create combat root packages

Create the following package structure or the closest equivalent:

```text
com.etema.ragnarmmo.combat
├── api
├── bridge
├── engine
├── net
├── state
└── util
```

### Definition of done
- packages exist
- package naming is stable
- no unrelated business logic is placed outside these packages once migration starts

---

## Task 1.2 - Create core enums and DTOs

Create at minimum:

- `CombatActionType`
- `CombatHitResultType`
- `CombatTargetCandidate`
- `CombatResolution`
- `CombatRequestContext`

### Requirements

#### `CombatActionType`
Should represent at least:
- BASIC_ATTACK
- SKILL
- PROJECTILE_SKILL
- SUPPORT_SKILL
- AREA_SKILL

#### `CombatHitResultType`
Should represent at least:
- HIT
- CRIT
- MISS
- DODGE
- BLOCKED
- IMMUNE
- HEAL

#### `CombatTargetCandidate`
Should contain at least:
- entity id or entity ref
- candidate source
- distance snapshot if useful
- whether it came from Better Combat hitbox or server-side selection

#### `CombatResolution`
Should contain at least:
- attacker
- target
- result type
- base damage
- final damage
- crit flag
- miss flag
- optional metadata map or flags

#### `CombatRequestContext`
Should contain at least:
- server player or combat actor
- action type
- sequence id
- item/skill context
- world/tick snapshot if needed

### Definition of done
- types exist and compile
- names are readable
- they are shared DTOs, not tied to rendering or Better Combat

---

## Task 1.3 - Add combat debug logger

Create `CombatDebugLog`.

### Responsibilities
- structured debug logging for attacks and skills
- log validation failures
- log hit roll results
- log damage results
- log rejected requests
- log multihit aggregated outcomes

### Minimum methods
- `logAttackRequest(...)`
- `logSkillRequest(...)`
- `logValidationReject(...)`
- `logHitResolution(...)`
- `logDamageResolution(...)`
- `logCooldownReject(...)`

### Definition of done
- logger exists
- logger is combat-specific
- future combat code uses this logger instead of random ad-hoc logs

---

## 5. Phase 2 - Combat state and packets

## Task 2.1 - Create combat actor state classes

Create at minimum:
- `CombatActorState`
- `CombatCooldownState`
- `CombatCastState`

### Requirements

#### `CombatActorState`
Contains transient combat state for an actor:
- current cast info if any
- last accepted attack sequence id if needed
- timestamps for recent attack actions

#### `CombatCooldownState`
Contains:
- logical basic attack ready tick
- global delay ready tick
- per-skill cooldown map
- after-cast delay ready tick

#### `CombatCastState`
Contains:
- active skill id
- cast start tick
- cast end tick
- cast target info
- cancellation/interruption info if needed

### Definition of done
- state classes compile
- responsibilities are separated
- no animation-specific fields live here unless absolutely necessary

---

## Task 2.2 - Attach or store combat state safely

Implement how combat state is stored for players and possibly mobs.

### Requirements
- use an existing capability system or a dedicated combat capability if needed
- avoid stuffing unrelated fields into stats classes unless already justified
- state must be server-authoritative

### Definition of done
- player combat state survives as intended for the gameplay model
- transient cooldown state is clearly distinguished from persisted progression state

---

## Task 2.3 - Create RagnarMMO combat packets

Create at minimum:
- `ServerboundRagnarBasicAttackPacket`
- `ServerboundRagnarSkillUsePacket`
- `ClientboundRagnarCombatResultPacket`
- `ClientboundRagnarCastStatePacket`

### Packet requirements

#### `ServerboundRagnarBasicAttackPacket`
Must contain:
- sequenceId
- comboIndex
- hand
- selectedSlot
- candidateTargetIds

#### `ServerboundRagnarSkillUsePacket`
Must contain:
- sequenceId
- skillId
- skillLevel if needed
- target entity or target pos
- skill intent metadata

#### `ClientboundRagnarCombatResultPacket`
Must contain:
- attackerId
- targetId
- result type
- amount
- flags

#### `ClientboundRagnarCastStatePacket`
Must contain:
- casterId
- skillId
- state (STARTED, INTERRUPTED, COMPLETED, FAILED)
- timing info when relevant

### Definition of done
- packets compile
- packets are registered in your network layer
- packet names are RagnarMMO-owned, not Better Combat-owned

---

## 6. Phase 3 - Core combat engine

## Task 3.1 - Create `RagnarCombatCooldownService`

### Responsibilities
- basic attack cadence
- global delay
- after-cast delay
- per-skill cooldowns
- cooldown readiness checks

### Required methods
- `canUseBasicAttack(...)`
- `markBasicAttackUsed(...)`
- `canUseSkill(...)`
- `markSkillUsed(...)`
- `isGlobalDelayReady(...)`
- `applyGlobalDelay(...)`
- `applyAfterCastDelay(...)`

### Definition of done
- service exists
- no call site depends only on vanilla attack cooldown as truth

---

## Task 3.2 - Create `RagnarCombatValidationService`

### Responsibilities
Validate incoming combat requests before resolution.

### Required checks
- attacker alive
- target alive if applicable
- correct side/server context
- weapon/skill legality
- not stunned/cast-locked/dead if relevant
- logical cooldown ready
- range valid
- request not stale/duplicated

### Required methods
- `validateBasicAttack(...)`
- `validateSkillRequest(...)`
- `validateCastCompletion(...)`

### Definition of done
- all attack/skill requests pass through validation
- validation results are explicit and logged

---

## Task 3.3 - Create `RagnarHitCalculator`

### Responsibilities
- compute hit chance
- compute miss/dodge/perfect dodge
- compute crit result

### Required methods
- `rollBasicAttackHit(...)`
- `rollSkillHit(...)`
- `rollCritical(...)`

### Definition of done
- hit logic is centralized
- no skill class performs ad-hoc hit logic on its own

---

## Task 3.4 - Create `RagnarDamageCalculator`

### Responsibilities
- compute physical damage
- compute magical damage
- apply crit multipliers
- apply defense calculations
- apply variance

### Required methods
- `computePhysicalDamage(...)`
- `computeMagicDamage(...)`
- `applyCriticalModifier(...)`
- `applyDefenseReduction(...)`

### Definition of done
- damage math is centralized
- formulas are not scattered across skill implementations

---

## Task 3.5 - Create `RagnarBasicAttackResolver`

### Responsibilities
- resolve final targets
- call hit calculator
- call damage calculator
- apply results

### Required method
- `resolveBasicAttack(CombatRequestContext ctx)`

### Definition of done
- a single place exists that resolves basic attacks
- result application is structured, not ad-hoc

---

## Task 3.6 - Create `RagnarSkillResolver`

### Responsibilities
- skill validation handoff
- cast start / cast finish / instant resolution
- hit/damage/effect resolution
- multihit policy selection

### Required methods
- `startSkill(...)`
- `resolveInstantSkill(...)`
- `resolveCompletedCast(...)`

### Definition of done
- skills no longer need to invent their own combat pipeline

---

## Task 3.7 - Create `RagnarCombatEngine`

### Responsibilities
- central entry point
- route request to basic or skill resolver
- own server-authoritative resolution orchestration

### Required methods
- `handleBasicAttackRequest(...)`
- `handleSkillUseRequest(...)`
- `handleCastCompletion(...)`

### Definition of done
- the engine exists and is used by packet handlers
- packet handlers are thin

---

## 7. Phase 4 - Better Combat bridge

## Task 4.1 - Create `BetterCombatBridge`

### Responsibilities
Read Better Combat combat context without letting Better Combat become the combat authority.

### Allowed responsibilities
- read current `AttackHand`
- read combo index
- read attack animation id
- read hand/main-offhand information
- read Better Combat attack metadata
- obtain Better Combat target candidates

### Forbidden responsibilities
- final damage
- hit/flee/crit
- final combat legality
- skill resource checks
- final target truth

### Definition of done
- bridge exists
- all Better Combat-specific code is localized

---

## Task 4.2 - Create a Better Combat snapshot DTO

Create `BetterCombatAttackSnapshot`.

### Suggested fields
- comboIndex
- hand
- animationId
- upswingRatio
- attackRange
- candidateTargetIds
- attack profile id if useful

### Definition of done
- snapshot is transport-safe inside RagnarMMO code
- engine does not need raw Better Combat internals outside bridge layer

---

## Task 4.3 - Intercept Better Combat minimal integration point

The goal is to let Better Combat still provide visuals and target candidates while RagnarMMO owns final attack resolution.

### Requirements
- use as few mixins/hooks as reasonably possible
- keep them isolated in bridge/integration package
- document each interception

### Strict instruction
Do NOT put formulas or combat resolution inside the interception layer.

### Definition of done
- Better Combat integration is isolated
- the rest of the combat engine does not depend on Better Combat mixins directly

---

## 8. Phase 5 - Basic attack migration

## Task 5.1 - Route basic attack packet handling through `RagnarCombatEngine`

### Requirements
- packet handler should only deserialize and pass context to engine
- validation and resolution live in engine/services

### Definition of done
- no packet handler applies final damage directly

---

## Task 5.2 - Remove final dependence on vanilla melee for basic attacks

### Requirements
- basic attack resolution should not depend on `Player.attack()` as truth
- damage should be applied through RagnarMMO result application methods

### Definition of done
- a basic attack can hit/miss/crit by RagnarMMO rules regardless of vanilla melee quirks

---

## Task 5.3 - Add basic attack feedback

### Requirements
- send MISS/HIT/CRIT feedback packets
- optionally play particles or sound hooks
- feedback should reflect server result, not client guess

### Definition of done
- visible combat feedback matches server-authoritative result

---

## 9. Phase 6 - Skill migration

## Task 6.1 - Create a shared skill use entrypoint

All active skills MUST eventually route through a shared entrypoint.

### Requirements
- one request path for skills
- one validation path
- one cooldown/resource gate

### Definition of done
- no new skill bypasses the shared resolver

---

## Task 6.2 - Move instant skills first

Start migration with instant or simplest skills.

Recommended first candidates:
- single-target basic offensive skill
- simple heal skill
- simple buff skill

### Definition of done
- at least one instant skill is fully resolved through RagnarMMO skill pipeline

---

## Task 6.3 - Move casted skills

### Requirements
- support cast state
- support interrupt/cancel rules if applicable
- resolve completion on server

### Definition of done
- at least one casted skill uses cast state instead of immediate local-only behavior

---

## 10. Phase 7 - Multihit stabilization

## Task 7.1 - Implement aggregated multihit policy

This is the default policy.

### Requirements
- multiple conceptual subhits
- configurable per-subhit roll behavior
- aggregated final application or bounded applications
- explicit logging

### Suggested API
- `resolveAggregatedMultiHit(...)`

### Definition of done
- 3-hit style skills no longer randomly lose hits because of vanilla semantics

---

## Task 7.2 - Mark which skills use aggregated multihit

### Requirements
- explicit per-skill policy
- no hidden behavior
- default to aggregated unless justified otherwise

### Definition of done
- each multihit skill declares its policy

---

## Task 7.3 - Only then evaluate separated real-hit policy

### Requirements
- use only when semantically necessary
- document why
- add tests/debug traces proving consistency

### Definition of done
- no skill uses separated real-hit policy casually

---

## 11. Phase 8 - Feedback and observability

## Task 8.1 - Centralize combat result feedback

### Requirements
- all final results pass through one feedback service
- one place to emit packets and display-type events

### Definition of done
- no scattered per-skill packet spam for standard combat results

---

## Task 8.2 - Add debug traces for failed requests

### Requirements
Log at least:
- cooldown rejection
- invalid target rejection
- stale sequence rejection
- dead actor rejection
- miss/flee results
- multihit aggregation summary

### Definition of done
- debugging combat desync becomes practical

---

## 12. Phase 9 - Cleanup and enforcement

## Task 9.1 - Remove combat logic from random skill classes where shared engine exists

### Requirements
- keep skill classes focused on skill-specific configuration/effects
- shared engine owns shared combat semantics

### Definition of done
- no copy-pasted hit/damage logic remains in many skill classes

---

## Task 9.2 - Localize Better Combat references

### Requirements
- Better Combat references are mostly in bridge package
- engine code does not depend directly on Better Combat types where avoidable

### Definition of done
- Better Combat can be swapped/forked later with bounded cost

---

## Task 9.3 - Add architecture guard comments

In sensitive files, add short comments such as:
- `// Better Combat is presentation only here.`
- `// Final hit/damage resolution belongs to RagnarCombatEngine.`
- `// Do not use Player.attack() as final RO combat authority.`

### Definition of done
- future contributors are less likely to violate architecture by accident

---

## 13. Explicit anti-tasks

The agent MUST NOT do any of these as a substitute for the plan.

### Anti-task 1
Do not fix rapid-hit issues by random direct manipulation of vanilla invulnerability fields unless it is part of a documented final combat policy.

### Anti-task 2
Do not place Better Combat-only logic inside core formula services.

### Anti-task 3
Do not add one packet per skill if a shared packet model already exists.

### Anti-task 4
Do not keep packet handlers fat.

### Anti-task 5
Do not make every skill compute damage separately.

### Anti-task 6
Do not fork Better Combat before the bridge approach is implemented and evaluated.

---

## 14. Milestone checklist

Use this as the high-level progress tracker.

### Milestone A - Foundation ready
- [ ] combat packages created
- [ ] shared DTOs created
- [ ] debug logger created

### Milestone B - Core services ready
- [ ] cooldown service created
- [ ] validation service created
- [ ] hit calculator created
- [ ] damage calculator created
- [ ] combat engine created

### Milestone C - Bridge ready
- [ ] Better Combat bridge created
- [ ] Better Combat snapshot DTO created
- [ ] integration points localized

### Milestone D - Basic attacks migrated
- [ ] basic attack packet exists
- [ ] engine handles basic attacks
- [ ] final resolution is server-authoritative
- [ ] no final dependence on `Player.attack()`

### Milestone E - Skills migrated
- [ ] shared skill packet exists
- [ ] at least one instant skill migrated
- [ ] at least one casted skill migrated
- [ ] shared skill resolver in place

### Milestone F - Multihit stabilized
- [ ] aggregated multihit implemented
- [ ] multihit skills assigned explicit policy
- [ ] rapid-hit inconsistency addressed by architecture, not hacks

### Milestone G - Cleanup done
- [ ] Better Combat references localized
- [ ] shared feedback service used
- [ ] random duplicated combat logic reduced

---

## 15. Suggested first implementation slice

If only one vertical slice is implemented first, it SHOULD be this:

1. build package scaffolding
2. build combat engine + cooldown/validation/hit/damage services
3. build basic attack packet
4. build Better Combat bridge snapshot
5. migrate one basic melee attack end-to-end
6. ensure server decides MISS/HIT/CRIT
7. add debug logs

This slice gives the highest architectural leverage with the least wasted effort.

---

## 16. Definition of success

This migration is successful only if:

- Better Combat still provides useful visual combat feel
- RagnarMMO fully owns final combat semantics
- rapid/multihit behavior is no longer hostage to vanilla attack quirks
- new skills can be added without inventing a new combat pipeline each time
- Better Combat can be changed later without rewriting the whole combat engine

---

## 17. One-sentence reminder for agents

> Use Better Combat as a visual/melee frontend, and use RagnarMMO as the only real combat backend.
