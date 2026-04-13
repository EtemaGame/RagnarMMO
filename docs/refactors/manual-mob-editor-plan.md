# Manual Mob Editor Refactor Plan (Forge 1.20.1)

## Objective
Formalize `MANUAL` as an explicit scaling mode and decouple effective scaling-method selection from passive manual data coverage.

## Existing classes in scope
- `RagnarConfigs`
- `MobStatsConfigAccess`
- `MobLevelManager`
- `ManualMobProfileRuntimeIntegrator`

## New classes in scope
- `ManualCoverageResult`
- `InternalManualMobEntry`
- `ManualMobRegistrySavedData`
- `ManualMobRegistryService`
- `InternalManualMobProfileResolver`
- `ManualMobBackendResolver`
- `MobManualCommand`

## Classes touched (non-destructive)
- `RagnarConfigs`
- `MobStatsConfigAccess`
- `MobLevelManager`
- `ManualMobProfileRuntimeIntegrator`

## Legacy classes intentionally kept
- `MobSpawnHandler`
- `MobStatDistributor`
- `MobAttributeHelper`
- Legacy capability path under `system.mobstats.core`

## Checklist
- [x] Add formal `MANUAL` option to level scaling config enum.
- [x] Add manual backend enum and config key.
- [x] Add uncovered behavior enum and config key.
- [x] Add manual editor/discovery feature flags.
- [x] Gate runtime manual-profile integration behind explicit `MANUAL` mode.
- [x] Separate manual coverage checks from effective method resolution.
- [x] Introduce coverage result model (`ManualCoverageResult`).
- [x] Add internal manual backend persistence (`SavedData`).
- [x] Add unified manual backend resolver (`INTERNAL` / `DATAPACK` / `HYBRID`).
- [x] Add admin command flow for internal manual entry CRUD (`/ragnar mobmanual ...`).
- [ ] Add GUI catalog + inspector + editor.
