# RagnarMMO Legacy Compatibility Audit

This file tracks compatibility aliases that must not be removed by intuition.
Before deleting one, run a full grep, document the impact, and prefer a
deprecation window.

## Command Aliases

Registered in `RagnarCommand`:

- `ragnar` -> `/r`
- `stats` -> `/r stats`
- `skills` -> `/r skills`
- `cart` -> `/r cart`
- `party` -> `/r party`
- `memo` -> `/r memo`
- `lv` -> `/r lv`
- `job` -> `/r job`
- `exp` -> `/r exp`
- `set` -> `/r set`
- `unlock` -> `/r unlock`
- `reset` -> `/r reset`
- `debug` -> `/r debug`
- `admin` -> `/r admin`
- `pc` -> `/r party chat`

These are player-facing command shortcuts. Keep them unless a release note and
migration path exists.

## Mixin Field Aliases

`MerchantOfferMixin` uses obfuscated and named field aliases:

- `f_45310_`, `baseCostA`
- `f_45311_`, `costB`
- `f_45312_`, `result`

These are runtime compatibility aliases for different mapping environments.
Do not remove either side without checking client and dedicated server startup.

## Skill Level Data Aliases

`SkillDefinition#getResourceCost` accepts these level-data keys:

- `resource_cost`
- `sp_cost`
- `mana_cost`

`sp_cost` is the canonical authored key for current JSON skill data. The other
keys remain compatibility aliases for older or external data packs.
