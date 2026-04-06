# RagnarMMO Combat Implementation TODO (Remaining)

This document tracks the **pending** tasks for the combat engine implementation. Completed tasks have been removed to keep the focus on current and future work.

---

## Phase 5 - Cleanup and enforcement

### Task 5.1 - Remove accidental dependence on vanilla melee semantics

- [ ] Audit remaining corner cases where `Player.attack()` might still be triggering logic.
- [ ] Ensure all damage applications use `applyResolvedPhysicalHit` or `applyResolvedMagicHit`.

### Task 5.2 - Reduce direct Better Combat coupling

- [ ] Evaluate if `BetterCombatBridge` can be further simplified.
- [ ] Ensure no Better Combat types are leaked into the core `engine` package.

### Task 5.3 - Evaluate Better Combat Fork

- [ ] Determine if advanced RO features (like specific hit-stop or animation cancels) require a local fork of Better Combat.

---

## Phase 6 - Expansion & New Skills

### Task 6.1 - Implement Projectile Support

- [ ] Add logic to `RagnarCombatEngine` for handling ranged entity tracking.
- [ ] Update `RagnarSkillResolver` to handle Bow and Wand projectile skills.

### Task 6.2 - Implement AoE (Area of Effect) Support

- [ ] Add spatial query support to `CombatRequestContext`.
- [ ] Implement `resolveAreaSkill` in `RagnarSkillResolver`.

---

## Phase 7 - Testing & Observability

### Task 7.1 - Add deterministic server tests

- [ ] Create unit tests for `RagnarHitCalculator` to verify classic RO hit chances.
- [ ] Create integration tests for `RagnarCombatEngine` sequences.

### Task 7.2 - Enhanced Debug Traces

- [ ] Add more detailed logging for complex status effect interactions.
