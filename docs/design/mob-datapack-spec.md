# Mob Datapack Spec

Status: Living reference for V1 manual mob datapack resources

## 1. Purpose of This Spec

This document defines the external datapack specification for V1 of the new mob system.

It covers only the declarative resources used by the new manual mob pipeline:

- `mob_templates`
- `mob_definitions`

This spec is aligned with the already approved architecture, audit, and domain decisions.

This V1 spec does define:

- resource layout
- resource ids
- supported fields
- inheritance and override semantics
- precedence rules
- required fields
- validation rules
- valid and invalid examples

This V1 spec does not define:

- Java classes
- loaders
- codecs
- runtime application
- migration from the current system
- wiring with current `mobstats`
- automatic mode data for `DISTANCE`, `BIOME_DISTANCE`, or `PLAYER_LEVEL`
- boss lifecycle policy

In V1, these datapack resources are used only by `MANUAL`.

## 2. Proposed Resource Structure

V1 uses two datapack resource trees:

- `data/<namespace>/mob_templates/<path>.json`
- `data/<namespace>/mob_definitions/<path>.json`

Resource ids are derived from datapack paths in the standard way:

- template id: `<namespace>:<path>`
- definition resource id: `<namespace>:<path>`

Examples:

- `data/example/mob_templates/undead/elite_base.json` -> `example:undead/elite_base`
- `data/example/mob_definitions/vanilla/zombie.json` -> `example:vanilla/zombie`

V1 matching rules are strict:

- a `mob_definition` targets exactly one `entity_type` id through its `entity` field
- matching is exact by `entity_type` id
- tags, wildcards, regex-like patterns, or hierarchy matching are out of scope for V1

Normal datapack overlay rules still apply by resource location. After overlay resolution, the effective `mob_definitions` set must still satisfy the semantic rule that only one definition may target a given exact `entity_type` id.

## 3. `mob_templates`

A `mob_template` is a reusable declarative fragment for manual mob authoring.

Its purpose is to reduce duplication across multiple definitions while preserving explicit semantics.

A template is identified by its resource id, for example:

- `example:undead/elite_base`

In V1, a template may contain only reusable declarative fields:

- `rank`
- `level`
- `ro_stats`
- `direct_stats`
- `race`
- `element`
- `size`

A template may be partial. It does not need to be a complete mob definition by itself.

A template must not contain:

- `entity`
- `template`
- any scaling-mode selector
- any targeting rules
- any lifecycle or world-state policy
- any runtime-only fields

V1 keeps templates intentionally simple:

- a template may not reference another template
- a template may not chain inheritance
- a template must contain at least one supported field

## 4. `mob_definitions`

A `mob_definition` is the external declarative definition for one exact manual mob target.

In V1:

- it applies to exactly one `entity_type` id
- the target is expressed through the `entity` field
- `entity` is matched exactly

A definition may optionally reference one template through:

- `template`

In V1, a definition may reference at most one template.

A definition may author fields directly and use them as overrides on top of the referenced template.

Override semantics are field-based:

- top-level scalar fields override inherited values completely
- `ro_stats` merges by member name
- `direct_stats` merges by member name

Examples:

- if a template provides `rank: "ELITE"` and the definition provides `rank: "BOSS"`, the final authored value is `BOSS`
- if a template provides `direct_stats.hit: 140` and the definition provides `direct_stats.hit: 180`, the final authored value is `180`
- if a template provides `ro_stats.dex: 20` and the definition provides `ro_stats.dex: 28`, the final authored value is `28`

The result of template merge is still declarative data. It is not a runtime profile.

## 5. Supported Fields in V1

All resources in V1 are JSON objects.

Unknown fields are validation errors in V1.

`null` values are not valid. If a field is not authored, it must be omitted.

### 5.1 Top-level fields

| Field | Type | Allowed in `mob_template` | Allowed in `mob_definition` | Semantics |
| --- | --- | --- | --- | --- |
| `entity` | resource location string | no | yes | exact `entity_type` id target |
| `template` | resource location string | no | yes | optional template reference |
| `rank` | string enum | yes | yes | explicit manual `MobRank` |
| `level` | integer | yes | yes | explicit manual level |
| `ro_stats` | object | yes | yes | RO base stat input block |
| `direct_stats` | object | yes | yes | final scalar combat stat block |
| `race` | lowercase taxonomy id | yes | yes | explicit race taxonomy |
| `element` | lowercase taxonomy id | yes | yes | explicit defensive element taxonomy |
| `size` | lowercase taxonomy id | yes | yes | explicit size taxonomy |

### 5.2 `rank`

Valid `rank` values in V1 are exactly:

- `NORMAL`
- `ELITE`
- `BOSS`

No other rank values are valid.

### 5.3 `level`

`level` is an authored integer.

It is never derived from:

- `direct_stats`
- `ro_stats`
- taxonomy fallback

### 5.4 `ro_stats`

`ro_stats` is the RO base-stat block.

Supported fields:

- `str`
- `agi`
- `vit`
- `int`
- `dex`
- `luk`

Each `ro_stats` field is an integer.

These values are base RO inputs, not final runtime combat scalars.

`ro_stats` may be partial in authored data inside either a template or a definition.

However, `ro_stats` may be used as a derivation source only if, after final template merge and definition overrides, the resolved `ro_stats` block is complete.

If derivation from `ro_stats` is needed and the final resolved `ro_stats` block is still incomplete, the definition is invalid.

### 5.5 `direct_stats`

`direct_stats` is the final scalar combat block.

Supported fields:

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

These fields are final authored combat values.

If a `direct_stats` field is present, it is authoritative over any value that could otherwise be derived from `ro_stats`.

V1 accepts JSON numbers for `direct_stats`.

`direct_stats` may also be partial in authored data inside either a template or a definition.

Partial `direct_stats` is valid as authored input. Validity is determined only after final merge, precedence, and allowed derivation.

### 5.6 Taxonomy fields

The taxonomy fields are:

- `race`
- `element`
- `size`

These fields reuse the existing taxonomy model already present in the repository.

Canonical authored values in V1 are lowercase ids aligned with current resolver/tag ids.

Authored taxonomy values must use canonical ids supported by the mod's taxonomy layer.

This document does not freeze taxonomy independently from that canonical layer.

The ids listed below are current known ids in the repository at the time of writing. They are reference examples for V1 authoring, not a separate taxonomy authority.

Current known `race` ids include:

- `demihuman`
- `brute`
- `insect`
- `fish`
- `demon`
- `undead`
- `formless`
- `plant`
- `angel`
- `dragon`

Current known `element` ids include:

- `neutral`
- `water`
- `earth`
- `fire`
- `wind`
- `poison`
- `holy`
- `dark`
- `ghost`
- `undead`

Current known `size` ids include:

- `small`
- `medium`
- `large`

In V1, taxonomy fallback may assist only these three fields.

## 6. Precedence Rules

### 6.1 Definition vs template

The definition is the more specific authored source.

Precedence order:

1. field authored directly in `mob_definition`
2. same field inherited from referenced `mob_template`
3. taxonomy fallback for `race`, `element`, or `size` only
4. validation failure if the field is still required and unresolved

### 6.2 Authored field vs inherited field

For scalar top-level fields:

- definition value replaces template value

For block fields:

- `ro_stats` merges by member name
- `direct_stats` merges by member name

This means a definition may override only one member of `ro_stats` or `direct_stats` without rewriting the full block.

### 6.3 `direct_stats` vs `ro_stats`

The semantic precedence rule is:

1. explicit or inherited `direct_stats.<field>`
2. derived value from final resolved `ro_stats`, but only if the resolved `ro_stats` block is complete
3. validation failure if the field is required and still unavailable

`direct_stats` is never overwritten by derivation.

`ro_stats` exists to provide RO-authored base inputs and to derive missing final scalar values when allowed.

### 6.4 Explicit taxonomy vs taxonomy fallback

If `race`, `element`, or `size` is authored explicitly in the definition or inherited from the template, that explicit value wins.

Taxonomy fallback may be used only when the corresponding field remains absent after template merge.

Taxonomy fallback may only resolve:

- `race`
- `element`
- `size`

Taxonomy fallback must never resolve or backfill:

- `level`
- `rank`
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

### 6.5 `level` and `rank`

`level` and `rank` are always authored manual fields.

They may come from:

- the definition directly
- the referenced template

They may not come from:

- `direct_stats`
- `ro_stats`
- taxonomy fallback

For `MANUAL`, an authored explicit `rank` is authoritative.

### 6.6 Resolved completeness

A V1 `mob_definition` is valid only if, after:

1. template merge
2. definition overrides
3. allowed taxonomy fallback
4. allowed derivation from final resolved `ro_stats`

the system can completely resolve the required declarative inputs needed to produce the final runtime profile later.

The required resolved field set is:

- `entity`
- `level`
- `rank`
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
- `race`
- `element`
- `size`

This is a completeness rule for declarative inputs only.

It does not mean that `MobDefinition` or `ResolvedMobDefinition` becomes the runtime result. `ComputedMobProfile` remains the unique final runtime model.

## 7. Required Fields in V1

This section closes the required-field policy for V1.

### 7.1 Required fields for `mob_definition`

A `mob_definition` must contain:

- `entity`

A resolved definition must contain, after template merge:

- `level`
- `rank`

A resolved definition must also satisfy the resolved completeness rule defined in section 6.6.

Its combat authoring surface may come from any combination of `direct_stats` and `ro_stats`, as long as the resolved definition satisfies section 6.6.

In practice, the common valid cases are:

- complete `direct_stats`
- complete `ro_stats`
- partial `direct_stats` plus complete resolved `ro_stats`
- complete `direct_stats` plus optional partial or complete `ro_stats`

A resolved definition is also required to end with resolved taxonomy for:

- `race`
- `element`
- `size`

Those taxonomy values may be:

- authored directly in the definition
- inherited from the template
- supplied by allowed taxonomy fallback

### 7.2 Required fields for `mob_template`

No single field is mandatory for every `mob_template`.

However, a template is valid in V1 only if:

- it is not empty
- it contains at least one supported reusable field

The supported reusable fields are:

- `rank`
- `level`
- `ro_stats`
- `direct_stats`
- `race`
- `element`
- `size`

### 7.3 When `ro_stats` is sufficient

`ro_stats` may be partial in authored data.

It is sufficient only when the resolved definition has:

- complete `ro_stats`
- resolved `level`
- resolved `rank`
- resolved `race`
- resolved `element`
- resolved `size`

In that case, all final combat scalars not explicitly authored in `direct_stats` are expected to be derivable from `ro_stats` by the later stat specification.

If the definition needs derivation from `ro_stats` and the final resolved `ro_stats` block is incomplete, the definition is invalid.

### 7.4 When `direct_stats` is sufficient

`direct_stats` may be partial in authored data.

`direct_stats` alone is sufficient when the resolved definition has:

- complete `direct_stats`
- resolved `level`
- resolved `rank`
- resolved `race`
- resolved `element`
- resolved `size`

In that case, `ro_stats` may be omitted entirely.

If `direct_stats` is partial, the missing required runtime-facing combat fields may be derived from a complete resolved `ro_stats` block. If they still cannot be resolved, the definition is invalid.

### 7.5 When `ro_stats` and `direct_stats` coexist

They may coexist in V1, but they are not always both required.

They coexist validly in two common situations:

- the author wants RO base stats for documentation or future derivation, while still forcing some final scalar values explicitly
- the final resolved `direct_stats` is incomplete, and the missing required combat fields are derived from a complete resolved `ro_stats` block

### 7.6 Invalid combinations

The following resolved-definition states are invalid in V1:

- missing `entity` in `mob_definition`
- missing `level` after template merge
- missing `rank` after template merge
- derivation is required but the final resolved `ro_stats` block is incomplete
- required runtime-facing combat fields remain unresolved after precedence and allowed derivation
- no combat authoring surface after template merge
- unresolved `race`, `element`, or `size` after allowed taxonomy fallback

## 8. Mandatory Validations

The following validations are mandatory for V1.

### 8.1 Resource structure and ids

- every resource must be a JSON object
- every `template` reference must be a valid resource location id
- every `entity` value must be a valid `entity_type` resource location id
- every resource id must be valid under normal datapack rules
- `mob_template` ids are referenced by resource id only
- `mob_definition` matching is driven by `entity`, not by definition resource id

### 8.2 Definition-target validation

- `entity` is required in every `mob_definition`
- the referenced `entity` must resolve to a registered `LivingEntity` type
- in V1, no two effective `mob_definitions` may target the same exact `entity_type` id

### 8.3 Template validation

- referenced `template` must exist
- `mob_template` must not contain `entity`
- `mob_template` must not contain `template`
- template chaining is invalid in V1
- empty templates are invalid

### 8.4 Scalar validation

Whenever a scalar field is present in authored data or resolved during validation, it must satisfy the following constraints:

- `level >= 1`
- `rank` must be one of `NORMAL`, `ELITE`, `BOSS`
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
- each `ro_stats` field must be `>= 0`

### 8.5 Completeness validation

- partial authored `ro_stats` is allowed
- partial authored `direct_stats` is allowed
- `level` and `rank` must exist after template merge
- if derivation from `ro_stats` is needed, the final resolved `ro_stats` block must contain all six fields
- after precedence and allowed derivation, the resolved definition must satisfy the full required field set described in section 6.6

### 8.6 Taxonomy validation

- explicit `race`, `element`, and `size` values must resolve as canonical taxonomy ids supported by the mod's taxonomy layer
- the current known ids listed in section 5.6 are reference guidance, not an independent taxonomy source of truth
- if any taxonomy field is absent after template merge, only taxonomy fallback may supply it
- if `race`, `element`, or `size` remains unresolved after allowed fallback, the definition is invalid

### 8.7 Strictness validation

- unknown top-level fields are invalid
- unknown fields inside `ro_stats` are invalid
- unknown fields inside `direct_stats` are invalid
- `null` values are invalid

## 9. Valid Examples

The example namespaces used below, such as `example` and `examplemod`, are illustrative. They are valid examples of the format and rules of this spec when the referenced resources and entity ids exist.

### 9.1 Vanilla zombie manual

Path:

- `data/example/mob_definitions/vanilla/zombie.json`

```json
{
  "entity": "minecraft:zombie",
  "rank": "NORMAL",
  "level": 18,
  "ro_stats": {
    "str": 22,
    "agi": 12,
    "vit": 20,
    "int": 3,
    "dex": 15,
    "luk": 4
  },
  "race": "undead",
  "element": "undead",
  "size": "medium"
}
```

Why valid:

- exact `entity_type` id
- explicit manual `rank` and `level`
- complete `ro_stats`
- taxonomy resolved explicitly

### 9.2 External mod mob manual

Path:

- `data/example/mob_definitions/modded/forest_troll.json`

```json
{
  "entity": "examplemod:forest_troll",
  "rank": "ELITE",
  "level": 41,
  "direct_stats": {
    "max_hp": 4200,
    "atk_min": 180,
    "atk_max": 245,
    "def": 66,
    "mdef": 18,
    "hit": 155,
    "flee": 68,
    "crit": 12,
    "aspd": 142,
    "move_speed": 0.28
  },
  "race": "brute",
  "element": "earth",
  "size": "large"
}
```

Why valid:

- exact modded `entity_type` id
- complete `direct_stats`
- no dependency on automatic modes

### 9.3 Reusable template

Path:

- `data/example/mob_templates/undead/elite_base.json`

```json
{
  "rank": "ELITE",
  "level": 35,
  "ro_stats": {
    "str": 28,
    "agi": 18,
    "vit": 24,
    "int": 10,
    "dex": 20,
    "luk": 8
  },
  "direct_stats": {
    "hit": 170,
    "move_speed": 0.27
  },
  "race": "undead",
  "element": "dark",
  "size": "medium"
}
```

Why valid:

- non-empty template
- contains only supported reusable fields
- partial `direct_stats` is allowed in a template

### 9.4 Definition using template plus overrides

Path:

- `data/example/mob_definitions/vanilla/skeleton_knight.json`

```json
{
  "entity": "minecraft:skeleton",
  "template": "example:undead/elite_base",
  "level": 39,
  "direct_stats": {
    "atk_min": 96,
    "atk_max": 134,
    "hit": 190
  },
  "element": "undead"
}
```

Why valid:

- exact `entity_type` id
- template exists
- `level` overrides template `level`
- `element` overrides template `element`
- partial `direct_stats` is valid because the template provides complete `ro_stats`

### 9.5 Manual boss fully defined

Path:

- `data/example/mob_definitions/bosses/wither.json`

```json
{
  "entity": "minecraft:wither",
  "rank": "BOSS",
  "level": 88,
  "ro_stats": {
    "str": 70,
    "agi": 32,
    "vit": 64,
    "int": 48,
    "dex": 42,
    "luk": 25
  },
  "direct_stats": {
    "max_hp": 48000,
    "atk_min": 420,
    "atk_max": 580,
    "def": 140,
    "mdef": 110,
    "hit": 320,
    "flee": 145,
    "crit": 22,
    "aspd": 165,
    "move_speed": 0.30
  },
  "race": "undead",
  "element": "dark",
  "size": "large"
}
```

Why valid:

- explicit manual `BOSS` rank
- full `direct_stats`
- full `ro_stats`
- no lifecycle policy is implied by `rank`

## 10. Invalid Examples

### 10.1 `atk_max < atk_min`

Path:

- `data/example/mob_definitions/invalid/bad_attack_range.json`

```json
{
  "entity": "minecraft:zombie",
  "rank": "NORMAL",
  "level": 10,
  "direct_stats": {
    "max_hp": 300,
    "atk_min": 90,
    "atk_max": 70,
    "def": 10,
    "mdef": 4,
    "hit": 55,
    "flee": 20,
    "crit": 2,
    "aspd": 130,
    "move_speed": 0.24
  },
  "race": "undead",
  "element": "undead",
  "size": "medium"
}
```

Why invalid:

- `atk_max` is lower than `atk_min`

### 10.2 Missing `entity`

Path:

- `data/example/mob_definitions/invalid/missing_entity.json`

```json
{
  "rank": "NORMAL",
  "level": 10,
  "ro_stats": {
    "str": 10,
    "agi": 10,
    "vit": 10,
    "int": 10,
    "dex": 10,
    "luk": 10
  },
  "race": "formless",
  "element": "neutral",
  "size": "medium"
}
```

Why invalid:

- `entity` is mandatory in every `mob_definition`

### 10.3 Invalid `rank`

Path:

- `data/example/mob_definitions/invalid/bad_rank.json`

```json
{
  "entity": "minecraft:spider",
  "rank": "MINI_BOSS",
  "level": 22,
  "ro_stats": {
    "str": 18,
    "agi": 24,
    "vit": 12,
    "int": 2,
    "dex": 20,
    "luk": 6
  },
  "race": "insect",
  "element": "poison",
  "size": "medium"
}
```

Why invalid:

- `MINI_BOSS` is not a valid V1 `MobRank`

### 10.4 Missing template

Path:

- `data/example/mob_definitions/invalid/missing_template.json`

```json
{
  "entity": "minecraft:husk",
  "template": "example:undead/missing_base",
  "level": 25,
  "rank": "ELITE",
  "ro_stats": {
    "str": 20,
    "agi": 14,
    "vit": 18,
    "int": 4,
    "dex": 16,
    "luk": 5
  },
  "race": "undead",
  "element": "undead",
  "size": "medium"
}
```

Why invalid:

- referenced `template` does not exist

### 10.5 Manual definition too incomplete for V1

Path:

- `data/example/mob_definitions/invalid/too_incomplete.json`

```json
{
  "entity": "minecraft:zombie",
  "rank": "NORMAL",
  "level": 12,
  "direct_stats": {
    "max_hp": 220,
    "atk_min": 40
  },
  "race": "undead",
  "element": "undead",
  "size": "medium"
}
```

Why invalid:

- `direct_stats` is incomplete
- there is no complete `ro_stats` block to derive the missing final combat fields

### 10.6 Duplicate definition for the same exact `entity_type` id

Paths:

- `data/example/mob_definitions/invalid/zombie_a.json`
- `data/example/mob_definitions/invalid/zombie_b.json`

```json
{
  "entity": "minecraft:zombie",
  "rank": "NORMAL",
  "level": 15,
  "ro_stats": {
    "str": 16,
    "agi": 12,
    "vit": 14,
    "int": 3,
    "dex": 12,
    "luk": 4
  },
  "race": "undead",
  "element": "undead",
  "size": "medium"
}
```

```json
{
  "entity": "minecraft:zombie",
  "rank": "ELITE",
  "level": 28,
  "ro_stats": {
    "str": 28,
    "agi": 18,
    "vit": 24,
    "int": 6,
    "dex": 20,
    "luk": 8
  },
  "race": "undead",
  "element": "dark",
  "size": "medium"
}
```

Why invalid:

- both effective definitions target the same exact `entity_type` id
- V1 allows only one effective `mob_definition` per exact entity target

## 11. Explicit V1 Limits

The following items are intentionally out of scope for V1:

- targeting definitions by tags
- wildcard entity matching
- hierarchy-based entity matching
- multiple entities in one definition
- multiple templates on one definition
- template chaining
- automatic-mode datapack configuration for `DISTANCE`
- automatic-mode datapack configuration for `BIOME_DISTANCE`
- automatic-mode datapack configuration for `PLAYER_LEVEL`
- lifecycle policy for bosses
- persistent world-state encounter policy
- spawn rules or spawn weighting
- loaders
- codecs
- runtime behavior
- migration rules from legacy `MobTier`

V1 is intentionally narrow so that `MANUAL` remains authoritative, exact-match by `entity_type` id remains stable, and later implementation can proceed without semantic improvisation.
