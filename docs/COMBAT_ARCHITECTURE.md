# RagnarMMO Combat Architecture Specification

Status: authoritative design document
Target stack: Minecraft 1.20.1 + Forge 47.4.0 + Better Combat 1.20.1
Audience: human contributors and AI coding agents

---

## 1. Purpose

This document defines the mandatory architecture for combat in RagnarMMO.

The goal is to implement a long-term maintainable, server-authoritative, Ragnarok Online-inspired combat system while still reusing Better Combat for visuals and melee feel.

This document is normative.

- "MUST" means required.
- "MUST NOT" means forbidden.
- "SHOULD" means strongly recommended.
- "MAY" means optional.

If code contradicts this document, the code is wrong unless this document is explicitly updated first.

---

## 2. Core decision

### 2.1 Final architecture

RagnarMMO MUST use the following boundary:

- Better Combat = presentation layer and target candidate acquisition
- RagnarMMO = combat authority and rules engine

In practical terms:

### Better Combat is allowed to handle

- weapon poses
- attack animations
- upswing visuals
- combo visuals
- dual-wield presentation
- target candidate selection based on hitbox/reach
- weapon feel and swing timing for presentation

### Better Combat is NOT allowed to decide

- whether an attack truly lands
- final hit/miss result
- crit result
- FLEE / Perfect Dodge result
- final damage
- skill legality
- skill resource consumption
- cast legality
- global delay / after-cast delay
- multihit semantics
- server truth

### RagnarMMO MUST own

- attack legality
- skill legality
- ASPD logic
- cast time
- cast delay / global delay / after-cast delay
- HIT vs FLEE
- crit chance / crit damage
- damage formulas
- elemental/racial/size modifiers when implemented
- physical and magical defense handling
- multihit policy
- resource checks and consumption
- final damage application
- authoritative combat logs / feedback

---

## 3. Hard rules

### Rule 1 - Vanilla `Player.attack()` is not the combat engine

RagnarMMO MUST NOT use `Player.attack(Entity)` as the semantic foundation of the RO-style combat system.

Reason:
- vanilla attack logic includes cooldown semantics not designed for RO
- vanilla target invulnerability windows create inconsistent multihit behavior
- vanilla attack logic couples combat outcome to systems RagnarMMO does not control

Allowed usage:
- temporary debug comparison
- isolated compatibility tests

Forbidden usage:
- basic attacks as final design
- active skills
- multihit skills
- projectile spells
- continuous bolt-style damage

### Rule 2 - Server authority is mandatory

All final combat outcomes MUST be decided on the server.

The client MAY:
- start animation
- display cast bar
- display predicted targets
- send intention packets

The client MUST NOT:
- decide hit success
- decide final damage
- decide crits
- decide misses
- decide cooldown legality
- decide skill legality

### Rule 3 - Better Combat integration must be isolated

All Better Combat integration MUST live behind a small integration boundary.

The project MUST NOT spread Better Combat assumptions across the whole combat codebase.

### Rule 4 - Presentation and resolution must stay separate

Animation code MUST remain separate from formula and combat-resolution code.

A class that plays attack animations MUST NOT also compute hit/flee/crit/damage.

### Rule 5 - Skills use RagnarMMO pipeline only

All active skills MUST use RagnarMMO-owned request, validation, resolution, and feedback paths.

Better Combat MAY provide visual support for some melee skills, but skills MUST NOT rely on Better Combat or vanilla attack semantics for final behavior.

---

## 4. Required package and class structure

The following structure MUST exist or be approximated closely.

```text
com.etema.ragnarmmo.combat
├── api
│   ├── CombatActionType.java
│   ├── CombatHitResultType.java
│   ├── CombatRequestContext.java
│   ├── CombatResolution.java
│   └── CombatTargetCandidate.java
├── engine
│   ├── RagnarCombatEngine.java
│   ├── RagnarBasicAttackResolver.java
│   ├── RagnarSkillResolver.java
│   ├── RagnarHitCalculator.java
│   ├── RagnarDamageCalculator.java
│   ├── RagnarCombatCooldownService.java
│   ├── RagnarCombatValidationService.java
│   └── RagnarCombatFeedbackService.java
├── bridge
│   ├── BetterCombatBridge.java
│   ├── BetterCombatAttackSnapshot.java
│   └── BetterCombatHooks.java
├── net
│   ├── ServerboundRagnarBasicAttackPacket.java
│   ├── ServerboundRagnarSkillUsePacket.java
│   ├── ClientboundRagnarCombatResultPacket.java
│   └── ClientboundRagnarCastStatePacket.java
├── state
│   ├── CombatActorState.java
│   ├── CombatCooldownState.java
│   └── CombatCastState.java
└── util
    ├── CombatDebugLog.java
    └── CombatMathFacade.java
```

Names MAY differ slightly, but responsibilities MUST remain equivalent.

---

## 5. Required responsibilities

### 5.1 `RagnarCombatEngine`

Single authoritative entry point for combat resolution.

MUST:
- receive validated requests
- route to basic attack or skill resolver
- produce combat result objects
- call final feedback sync

MUST NOT:
- know Better Combat internals directly
- know animation playback internals
- contain rendering logic

### 5.2 `RagnarBasicAttackResolver`

Resolves normal weapon attacks.

MUST:
- validate attack cadence using RagnarMMO logic
- resolve target list from validated candidates
- run hit/flee/crit rolls
- compute damage
- apply results

MUST NOT:
- rely on vanilla `Player.attack()` as final behavior

### 5.3 `RagnarSkillResolver`

Resolves active skills.

MUST:
- validate skill unlock/job legality
- validate costs and cooldowns
- support cast start/cast complete/instant skills
- support single hit, multihit, projectile, area, and support patterns

MUST NOT:
- reuse melee attack code blindly for all skills

### 5.4 `RagnarHitCalculator`

MUST compute:
- hit chance
- flee/evasion result
- perfect dodge if present
- crit result

MUST return explicit result types, not booleans only.

### 5.5 `RagnarDamageCalculator`

MUST compute:
- physical damage
- magical damage
- damage variance
- crit damage modifiers
- defense reduction
- optional later modifiers: size/race/element/property

### 5.6 `RagnarCombatCooldownService`

MUST own all logical timing.

MUST track separately:
- logical basic attack interval
- skill cooldowns
- global delay
- cast delay / after-cast delay
- cast lock if used

MUST NOT depend only on vanilla attack cooldown.

### 5.7 `RagnarCombatValidationService`

MUST validate:
- attacker exists and is alive
- target exists and is alive if required
- world/server side is correct
- weapon/skill state is legal
- target is in valid range
- request is not stale or duplicated
- actor is not hard-locked by cast/delay/death/etc.

### 5.8 `RagnarCombatFeedbackService`

MUST send combat feedback to clients.

Examples:
- MISS
- DODGE
- HIT
- CRIT
- heal numbers if implemented
- cast start / cast cancel / cast complete

---

## 6. Better Combat integration contract

### 6.1 Integration scope

A dedicated bridge layer MUST be the only place where RagnarMMO directly references Better Combat combat-flow types or helpers.

Examples of allowed bridge behavior:
- read Better Combat current attack hand
- read combo index
- read Better Combat attack animation id
- read Better Combat upswing ratio
- read Better Combat candidate targets
- read Better Combat weapon attribute data

### 6.2 Forbidden bridge behavior

The bridge MUST NOT:
- compute final hit/miss
- compute damage
- spend skill resources
- apply final damage to entities
- decide skill legality
- own cooldowns

### 6.3 Goal of the bridge

The bridge exists so Better Combat can be replaced, forked, reduced, or removed later without rewriting the RO combat engine.

---

## 7. Packet model

RagnarMMO MUST own its combat packets.

### 7.1 `ServerboundRagnarBasicAttackPacket`

Required fields:
- sequenceId
- comboIndex
- hand
- selectedSlot
- candidateTargetIds
- optional snapshot fields needed for validation

Client meaning:
- "I attempted a basic attack with these visual conditions"

Server meaning:
- never trust blindly
- revalidate everything

### 7.2 `ServerboundRagnarSkillUsePacket`

Required fields:
- sequenceId
- skillId
- skillLevel if applicable
- target entity id or target position depending on skill type
- optional cast metadata

### 7.3 `ClientboundRagnarCombatResultPacket`

Required fields:
- attacker id
- target id
- result type
- amount
- flags (crit, miss, dodge, blocked, resisted, etc.)

### 7.4 Duplicate/stale protection

The server SHOULD reject duplicated or stale sequence ids.

---

## 8. Combat flow

### 8.1 Basic attack flow

This flow is mandatory.

1. Client attack input begins.
2. Better Combat MAY start upswing animation.
3. Better Combat MAY produce candidate targets.
4. Client sends `ServerboundRagnarBasicAttackPacket`.
5. Server validates request.
6. Server computes legal target set.
7. Server resolves HIT/FLEE/crit/damage.
8. Server applies combat results using RagnarMMO pipeline.
9. Server sends feedback packets.

### Important note

Candidate targets from the client are hints only.
The server MUST treat them as suggestions, not truth.

### 8.2 Skill flow

1. Client requests skill use.
2. Client MAY display pre-cast or animation.
3. Server validates skill legality and costs.
4. If casted, server opens cast state.
5. On cast completion or instant use, server resolves targeting and effect.
6. Server applies result.
7. Server sends combat feedback.

---

## 9. ASPD specification

### 9.1 Rule

ASPD MUST be a RagnarMMO-owned logical timing system.

### 9.2 Why

Better Combat uses vanilla-oriented attack timing and upswing timing for presentation. That is useful visually, but insufficient as the semantic source of RO attack cadence.

### 9.3 Required implementation

RagnarMMO MUST track, at minimum:
- `logicalAttackIntervalTicks`
- `globalDelayTicks`
- `afterCastDelayTicks`
- `visualAttackWindowTicks` if needed for synchronization

### 9.4 Forbidden implementation

The following are forbidden as the only ASPD implementation:
- only reading `GENERIC_ATTACK_SPEED`
- only reading `getAttackCooldownProgress()`
- only reusing Better Combat timing values as truth

---

## 10. HIT / FLEE specification

### 10.1 Rule

Being inside the Better Combat hitbox means only:
- the target is a valid candidate for resolution

It does NOT mean:
- the attack landed

### 10.2 Mandatory steps

For each validated target candidate, RagnarMMO MUST compute:
- whether the attack misses due to HIT vs FLEE
- whether perfect dodge applies if implemented
- whether the hit is critical
- final damage after defense/modifiers

---

## 11. Multihit policy

### 11.1 Default policy

The default multihit policy MUST be `aggregated-multihit`.

Meaning:
- a skill may conceptually contain N subhits
- each subhit may roll hit/crit independently if desired
- the server aggregates them into a controlled result form
- the final application uses one or a small bounded number of damage applications

### 11.2 Why this is default

This avoids relying on vanilla target invulnerability behavior and is much more stable for multiplayer and balance.

### 11.3 Separate-real-hit policy

A skill MAY use real separated applications only if:
- the skill requires it semantically
- the implementation explicitly controls timing/invulnerability interactions
- tests exist proving consistency

If these conditions are not met, DO NOT use separated real hits.

---

## 12. Damage application policy

### 12.1 Required principle

RagnarMMO MUST have a controlled internal method for applying final combat results.

Example shape:
- `applyResolvedPhysicalHit(...)`
- `applyResolvedMagicHit(...)`
- `applyResolvedSupportEffect(...)`

### 12.2 Forbidden shortcut

The code MUST NOT skip the combat engine and directly call random entity damage APIs from scattered skill implementations.

All damage application must pass through shared combat result handling.

---

## 13. AI-agent implementation rules

These rules are specifically for AI coding agents.

### 13.1 The agent MUST

- create a bridge layer rather than scattering Better Combat calls
- keep all formulas in RagnarMMO-owned services
- keep server authority intact
- centralize combat timing
- centralize result application
- centralize feedback packet emission

### 13.2 The agent MUST NOT

- implement RO formulas inside Better Combat mixins
- use `Player.attack()` as the final design
- solve multihit by random vanilla cooldown hacks
- place Better Combat types deep inside combat engine packages
- duplicate targeting/damage logic across each skill class
- let individual skills invent their own combat pipeline without going through shared engine services

### 13.3 If the agent needs to intercept Better Combat

It MUST:
- do so in a minimal number of classes
- keep integration localized to `combat.bridge`
- document why the interception exists
- avoid mixing animation interception with formula logic

---

## 14. Required anti-pattern list

The following are forbidden anti-patterns.

1. Using Better Combat as the final combat authority.
2. Using client target selection as trusted truth.
3. Putting HIT/FLEE formulas into Better Combat mixins.
4. Letting skills directly damage entities without shared resolution.
5. Modeling ASPD only with vanilla attack speed.
6. Fixing multihit by resetting vanilla fields without architecture.
7. Coupling packets to Better Combat internal packet contracts.
8. Mixing animation playback classes with damage formulas.
9. Copy-pasting attack-resolution code into each skill effect.
10. Forking Better Combat before a small integration bridge has been attempted.

---

## 15. Next Implementation Phases

### Phase 5 - Cleanup (IN PROGRESS)
- [ ] Remove accidental dependence on vanilla melee semantics.
- [ ] Reduce direct Better Combat coupling further.
- [ ] Evaluate whether a Better Combat fork is still necessary.
- [ ] Add deterministic server tests where possible.

### Phase 6 - Expansion
- [ ] Implement more Pre-Renewal skills using the new `RagnarSkillResolver`.
- [ ] Implement support for Projectile skills (Bow/Magic) in the Engine.
- [ ] Implement Area of Effect (AoE) logic in the Engine.

---

## 16. When to consider a Better Combat fork

A fork SHOULD only be considered if all of the following are true:
- the bridge approach is already implemented
- the remaining issues are clearly integration issues, not architecture mistakes
- Better Combat internals must be changed to stop conflicting behavior cleanly
- keeping compatibility through mixins becomes more expensive than maintaining a fork

A fork MUST NOT be the first strategy.

---

## 17. Acceptance criteria

A combat implementation is only acceptable if all of the following are true.

### Basic attacks
- Better Combat animations still work
- RagnarMMO decides final hit/miss/crit/damage
- no reliance on `Player.attack()` as final truth

### Skills
- all skills use RagnarMMO packet + validation + resolution pipeline
- multihit skills behave consistently
- no random lost hits due to vanilla behavior

### Server authority
- client cannot force damage
- client cannot force hit success
- client cannot bypass cooldown legality

### Maintainability
- Better Combat references are localized
- formulas are centralized
- cooldown logic is centralized
- damage application is centralized
- feedback emission is centralized

---

## 18. Short directive for future contributors and agents

Use this design rule at all times:

> Better Combat is a visual/melee presentation dependency.
> RagnarMMO is the combat engine and the only source of combat truth.

If a proposed change violates that sentence, reject the change or redesign it.
