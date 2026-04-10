# Mob Stat Derivation Spec

Status: Living reference for stat semantics and derivation

## 1. Purpose of This Document

This document defines the semantic meaning and derivation surface of mob stats for the new manual mob system.

It specifies:

- what each stat means
- which final combat stats may be derived from `ro_stats`
- which validation rules belong to the spec layer
- which concerns remain implementation or runtime concerns

This document does not define:

- Java formulas
- loaders
- codecs
- runtime application
- migration
- final game balance tuning

This is a semantic spec, not an implementation plan.

## 2. Stat Surface of the System

The system distinguishes two stat layers.

### 2.1 RO base stats

These are the authored RO-style base inputs:

- `str`
- `agi`
- `vit`
- `int`
- `dex`
- `luk`

These are not the final runtime combat profile.

### 2.2 Runtime-facing final combat stats

These are the final combat-facing values that the system must be able to resolve before producing `ComputedMobProfile`:

- `max_hp`
- `atk_min`
- `atk_max`
- `def`
- `mdef`
- `hit`
- `flee`
- `crit`
- `aspd`
- `move_speed`

These are semantic combat stats.

They are not automatically the same thing as:

- vanilla attributes
- capability storage fields
- Minecraft AI parameters
- current legacy combat-engine intermediates

`ComputedMobProfile` remains the only final runtime model.

## 3. Semantics of Each Final Field

### `max_hp`

The mob's final maximum health pool for the encounter.

It is a semantic combat stat, not a direct statement about how Minecraft health attributes must be written.

### `atk_min`

The lower bound of the mob's final physical attack range.

It represents final authored or derived combat power at the semantic model layer, not a direct vanilla damage attribute.

### `atk_max`

The upper bound of the mob's final physical attack range.

It must always be coherent with `atk_min`.

### `def`

The mob's final physical defense stat.

It represents resistance against physical damage in the new semantic model. It is not automatically identical to vanilla armor points or any specific runtime reduction formula.

### `mdef`

The mob's final magical defense stat.

It represents resistance against magical damage and magic-related status interactions in the new semantic model. It is distinct from `def`, and it remains a first-class stat even if parts of the current legacy runtime underuse it.

### `hit`

The mob's final hit or accuracy stat.

It represents how well the mob lands attacks against targets, semantically separate from projectile spread, AI aim, or vanilla hit registration.

### `flee`

The mob's final evade stat.

It represents how hard the mob is to hit in the combat model, separate from pathfinding, hurtbox size, or vanilla movement behavior.

### `crit`

The mob's final critical-hit chance stat.

It is a semantic combat chance value, not a guarantee that the runtime will expose it through any existing vanilla attribute.

### `aspd`

The mob's final RO-style attack speed stat.

It represents attack cadence in the Ragnarok-style combat layer. It is not the same thing as movement speed, and it is not automatically the same thing as a vanilla attack-speed attribute or cooldown field.

### `move_speed`

The mob's final movement speed stat.

It represents movement velocity for the encounter and is completely separate from `aspd`.

`move_speed` affects how the mob moves.

`aspd` affects how quickly the mob attacks.

## 4. V1 Derivation Policy

In V1, `direct_stats` remains authoritative per field.

A final combat field may be resolved in this order:

1. explicit `direct_stats.<field>` after merge
2. derived value from complete resolved `ro_stats`
3. validation failure if still unresolved

Important V1 rule:

- derivation from `ro_stats` is allowed only when the resolved `ro_stats` block is complete
- this gate applies even if a particular formula later uses only a subset of RO stats

`level` may participate in later derivation formulas where appropriate.

`rank` does not act as an implicit stat multiplier in this spec.

### Derivation table

| Final field | May be authored in `direct_stats` | May derive from `ro_stats` in V1 | Requires complete resolved `ro_stats` | Primary semantic RO drivers |
| --- | --- | --- | --- | --- |
| `max_hp` | yes | yes | yes | primarily `vit`, plus resolved `level` |
| `atk_min` | yes | yes | yes | primarily `str`, `dex`, `luk`, plus resolved `level` |
| `atk_max` | yes | yes | yes | primarily `str`, `dex`, `luk`, plus resolved `level` |
| `def` | yes | yes | yes | primarily `vit`, `agi`, plus resolved `level` |
| `mdef` | yes | yes | yes | primarily `int`, `vit` |
| `hit` | yes | yes | yes | primarily `dex`, `luk`, plus resolved `level` |
| `flee` | yes | yes | yes | primarily `agi`, `luk`, plus resolved `level` |
| `crit` | yes | yes | yes | primarily `luk` |
| `aspd` | yes | yes | yes | primarily `agi`, `dex` |
| `move_speed` | yes | yes | yes | primarily `agi` |

For V1, none of the final combat fields are marked as "never derivable".

`level` and `rank` are outside this derivation table because they are authored manual fields, not derived combat stats.

## 5. Concrete V1 Decisions

The following decisions are closed for V1.

### 5.1 Full derivation surface

All runtime-facing final combat stats are derivable in V1 when the resolved `ro_stats` block is complete.

That means V1 does not require any final combat field to be authored explicitly in `direct_stats`.

### 5.2 `max_hp`

`max_hp` may derive from RO stats in V1.

Its primary semantic driver is `vit`, and derivation may also use resolved `level`.

### 5.3 `atk_min` and `atk_max`

`atk_min` and `atk_max` may both derive from RO stats in V1.

Their derivation may use `str`, `dex`, `luk`, and resolved `level`.

The exact numeric range formula is intentionally deferred.

### 5.4 `def` and `mdef`

`def` may derive in V1.

`mdef` may also derive in V1.

This is a deliberate semantic decision. The new spec does not weaken `mdef` just because the current legacy runtime has incomplete support in some paths.

### 5.5 `hit`, `flee`, `crit`, `aspd`, and `move_speed`

All of these may derive in V1 from complete resolved `ro_stats`.

Their primary semantic drivers are:

- `hit`: `dex`, `luk`, and resolved `level`
- `flee`: `agi`, `luk`, and resolved `level`
- `crit`: primarily `luk`
- `aspd`: `agi` and `dex`
- `move_speed`: primarily `agi`

### 5.6 No implicit rank scaling

`rank` is not a hidden derivation multiplier in this spec.

If a `BOSS` should have much higher combat stats than a `NORMAL` mob, that difference must come from authored `level`, authored `direct_stats`, or derivation from authored `ro_stats`, not from an unstated rank multiplier.

## 6. Coherence Rules Between Fields

The following rules are semantic V1 rules.

### 6.1 Scalar coherence

- `max_hp > 0`
- `atk_min >= 0`
- `atk_max >= atk_min`
- `def >= 0`
- `mdef >= 0`
- `hit >= 0`
- `flee >= 0`
- `crit >= 0`
- `aspd > 0`
- `move_speed > 0`

### 6.2 Mixed authored and derived values

V1 allows mixing authored and derived final fields.

Examples:

- `atk_min` authored and `atk_max` derived is valid
- `def` derived and `mdef` authored is valid
- `move_speed` authored while `agi` also exists is valid

The rule is simple:

- each explicit `direct_stats` field is authoritative for itself
- missing final fields may be derived only from complete resolved `ro_stats`
- the final resolved field set must still satisfy all coherence rules

### 6.3 No back-solving or forced coupling

V1 does not require the system to back-solve authored fields against RO expectations.

For example:

- if `move_speed` is authored explicitly and `agi` is also present, the authored `move_speed` wins
- if `mdef` is authored explicitly and `int` or `vit` is also present, the authored `mdef` wins

The spec does not require a secondary consistency check such as "authored value must equal the derived value".

### 6.4 Attack-range coherence

If one attack-range endpoint is authored and the other is derived, the final resolved pair must still satisfy:

- `atk_max >= atk_min`

If the final pair violates that rule, the definition is invalid.

The system must not silently reorder them.

### 6.5 `mdef` remains valid even if legacy runtime has gaps

`mdef` authored directly is always semantically valid in V1.

Current legacy limitations, including paths where the existing runtime underuses mob `mdef`, do not reduce the meaning or legitimacy of the field in this spec.

## 7. Relation to the Current Runtime

This document belongs to the spec layer.

It defines:

- the meaning of the stats
- the derivation surface
- the semantic validation rules

The future runtime or applier layer will define:

- how these stats are stored
- how they map to capabilities or attributes
- how they feed actual combat formulas
- how they sync to client systems

This spec must not be degraded to fit accidental limitations of the current legacy runtime.

Explicit V1 decision:

- the spec remains semantically complete even if the current combat implementation has temporary gaps, such as incomplete mob-side use of `mdef`

Implementation debt is not a reason to shrink the model.

## 8. Limits of This Document

This document does not yet close:

- exact numeric formulas for derived values
- final balance curves
- definitive runtime clamps beyond the semantic validation rules already stated
- exact translation to vanilla attributes
- exact interaction with skills, damage formulas, or AI systems
- exact client presentation rules

Those belong to later implementation or runtime-design phases.
