# Mob Taxonomy Spec

Status: Living reference for taxonomy semantics and external contract

## 1. Purpose of This Document

This document defines the semantic meaning and external V1 contract of the mob taxonomy fields:

- `race`
- `element`
- `size`

It specifies:

- what each field means
- what each field does not mean
- how taxonomy is resolved in V1
- what belongs to the external contract versus internal canonical resolution

This document does not define:

- Java implementation
- loaders
- combat formulas
- runtime damage logic
- migration
- full skill or bonus systems

This is a taxonomy semantics document, not an implementation document.

## 2. Role of Taxonomy in the New System

The taxonomy layer exists to provide semantic classification for mobs.

In V1, it serves four purposes:

- semantic classification of the encounter target
- reusable authored metadata in templates and definitions
- a stable external hook surface for future combat systems
- a canonical fallback source when taxonomy was not authored explicitly

Taxonomy does not replace:

- `level`
- `rank`
- final combat stats

Taxonomy is descriptive classification, not progression scaling.

It may later be used by:

- damage formulas
- skills
- weapon bonuses
- cards
- resistances
- UI or encyclopedia-style inspection

But those downstream uses are outside the scope of this document.

## 3. `race`

`race` represents the mob's semantic race classification in the Ragnarok-style combat model.

Examples of what `race` may capture:

- undead
- demon
- brute
- fish

`race` does not represent:

- exact species
- biome
- faction
- hostility
- encounter rank
- boss lifecycle policy

In V1, `race` is a single categorical classification.

That means:

- each mob resolves to exactly one `race`
- multivalue race classification is not supported
- compound race classification is not supported

`race` may be authored explicitly in a definition or template.

If `race` is authored explicitly, that value overrides canonical fallback.

If `race` is not authored, canonical fallback may resolve it.

The current repository already exposes race taxonomy through shared canonical infrastructure. Current known canonical race ids in the repo include:

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

These current ids are reference guidance for V1 authoring, not a separate authority from the canonical taxonomy layer itself.

## 4. `element`

`element` represents the mob's semantic base defensive element classification.

In V1, this field is intentionally simple.

It represents:

- the mob's base elemental family for defensive or taxonomy purposes

It does not represent:

- offensive weapon element
- temporary elemental state applied at runtime
- a full resistance table
- element level or grade
- a compound elemental profile

In V1, `element` is a single categorical classification.

That means:

- each mob resolves to exactly one `element`
- multivalue element classification is not supported
- element levels are not supported
- compound element structures are not supported

This simplicity is deliberate.

It keeps V1 stable while preserving room for future expansion. If the project later needs richer elemental semantics, that future model should extend the concept without changing what `element` already means in V1: the mob's base elemental family.

For example, a future design could add a separate field such as:

- `element_level`

or a structured elemental object.

That future work would extend V1 semantics rather than redefine them.

`element` may be authored explicitly in a definition or template.

If `element` is authored explicitly, that value overrides canonical fallback.

If `element` is not authored, canonical fallback may resolve it.

The current repository already exposes element taxonomy through shared canonical infrastructure. Current known canonical element ids in the repo include:

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

These current ids are reference guidance for V1 authoring, not a separate authority from the canonical taxonomy layer itself.

## 5. `size`

`size` represents the mob's semantic combat size classification.

It represents:

- the combat-relevant size bucket used by the taxonomy layer

It does not represent:

- literal model scale
- exact hitbox dimensions
- collision box dimensions
- pathfinding width
- animation scale

In V1, `size` is a single exclusive classification.

The allowed V1 size categories are:

- `small`
- `medium`
- `large`

That means:

- each mob resolves to exactly one `size`
- multivalue size classification is not supported
- compound size classification is not supported

`size` may be authored explicitly in a definition or template.

If `size` is authored explicitly, that value overrides canonical fallback.

If `size` is not authored, canonical fallback may resolve it.

Unlike literal dimensions, `size` remains a semantic combat category. The canonical taxonomy layer may internally determine it using tags, resolver logic, or other canonical classification mechanisms, but the external contract is the resolved taxonomy id, not the internal method used to obtain it.

## 6. Resolution and Precedence Rules

Taxonomy resolution in V1 is field-by-field.

For each of:

- `race`
- `element`
- `size`

the precedence order is:

1. explicit authored value in `mob_definition`
2. inherited value from referenced `mob_template`
3. canonical taxonomy fallback from the mod's shared taxonomy infrastructure
4. validation failure if the field remains unresolved

This fallback is taxonomy-only.

It may assist only:

- `race`
- `element`
- `size`

It must not resolve or backfill:

- `level`
- `rank`
- final combat stats

The external contract does not freeze the internal fallback implementation.

That means this document does not require the canonical taxonomy layer to use one specific internal mechanism such as:

- one exact helper class
- one exact tag layout
- one exact hardcoded fallback path

What V1 does require is that canonical fallback, whatever its internal implementation, resolves to the same external taxonomy contract:

- one canonical `race` id
- one canonical `element` id
- one canonical `size` id

or fails clearly.

## 7. Semantic V1 Rules

The following decisions are closed for V1.

### 7.1 Single value per field

Each mob has exactly one resolved value for:

- `race`
- `element`
- `size`

### 7.2 No multivalue taxonomy

V1 does not allow:

- multiple races
- multiple elements
- multiple sizes

### 7.3 No compound taxonomy

V1 does not allow composed taxonomy values such as:

- hybrid race objects
- dual elements
- multi-bucket size classifications

### 7.4 No element levels in V1

`element` in V1 represents only the base elemental family.

It does not encode:

- level
- grade
- intensity
- stack count

### 7.5 Ambiguity is invalid

If canonical fallback would imply more than one competing value for a required taxonomy field, the definition is invalid.

V1 requires a single resolved taxonomy value per field, not best-effort ambiguity resolution.

### 7.6 Authored override remains allowed

Explicit authored taxonomy remains valid and authoritative in V1.

Canonical fallback exists to supply missing taxonomy, not to overrule explicit author intent.

## 8. Relation to Future Combat

This taxonomy layer is intentionally designed to connect later to:

- damage relationships
- skills
- equipment bonuses
- status interactions
- resistances
- informational UI

V1 does not close those formulas or interactions yet.

The taxonomy layer must not be modeled only around temporary limitations of the current legacy combat runtime.

If the current combat system underuses or inconsistently consumes some taxonomy today, that does not weaken the semantic contract defined here.

## 9. Limits of This Document

This document does not yet define:

- exact race-based damage formulas
- exact element-vs-element resist tables
- exact size-based weapon penalties
- card bonuses
- skill interactions
- status formulas by taxonomy
- runtime-specific attribute mappings
- concrete resolver or loader implementation

Those belong to later combat or runtime specification work.
