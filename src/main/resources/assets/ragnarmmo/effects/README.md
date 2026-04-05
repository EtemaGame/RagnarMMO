# Skill Effects

Este directorio contiene el sistema visual genérico nuevo.

## Convenciones

- `assets/ragnarmmo/effects/aoe/...`: efectos persistentes de AoE o aura.
- `assets/ragnarmmo/effects/projectiles/...`: visuals continuos de proyectiles.
- `assets/ragnarmmo/effects/impact/...`: bursts efímeros de impacto.
- `assets/ragnarmmo/effect_bindings/...`: relación entre `skill_id`, fase y `effect`.

## Tipos soportados en el MVP

- `sprite_sheet`
- `layered`
- `composite`
- `str_layered`
- `particle_emitter`

## Ejemplo mínimo

```json
{
  "id": "ragnarmmo:aoe/example_loop",
  "type": "sprite_sheet",
  "texture": "minecraft:textures/block/fire_0.png",
  "size": 2.0,
  "fps": 12,
  "loop": true,
  "orientation": "billboard",
  "offset": [0.0, 0.5, 0.0],
  "render_type": "translucent_emissive"
}
```

```json
{
  "id": "ragnarmmo:projectiles/example_particles",
  "type": "particle_emitter",
  "particle": "minecraft:end_rod",
  "shape": "ring",
  "duration_ticks": 20,
  "loop": true,
  "emit_interval_ticks": 1,
  "count": 6,
  "radius": 0.35,
  "rotation_per_tick_deg": 10.0,
  "radial_velocity": 0.01,
  "spread": [0.02, 0.01, 0.02],
  "base_velocity": [0.0, 0.01, 0.0],
  "random_velocity": [0.01, 0.01, 0.01],
  "inherit_entity_velocity": 0.1
}
```

```json
{
  "skill_id": "ragnarmmo:example_skill",
  "bindings": [
    { "phase": "aoe_loop", "effect": "ragnarmmo:aoe/example_loop" },
    { "phase": "projectile_tick", "effect": "ragnarmmo:projectiles/example_particles" }
  ]
}
```

## `particle_emitter`

- Pensado para trails, bursts y auras procedurales, sin escribir una clase Java por skill.
- Convive con `layered` y `sprite_sheet`; la idea es combinarlos en el mismo `phase`.
- Campos principales:
- `particle`: `ResourceLocation` del tipo de partícula.
- `shape`: `point`, `ring` o `sphere`.
- `emit_interval_ticks`: cada cuántos ticks emite.
- `count`: cuántas partículas lanza por emisión.
- `radius`: radio base para `ring` y `sphere`.
- `rotation_per_tick_deg`: rotación progresiva para anillos.
- `radial_velocity`: empuje hacia afuera desde la forma emitida.
- `spread`: jitter de posición por eje.
- `base_velocity`: velocidad constante aplicada a todas las partículas.
- `random_velocity`: variación aleatoria por eje.
- `inherit_entity_velocity`: cuánto hereda de la velocidad de la entidad ancla.
- Limitación actual: por ahora soporta solo `SimpleParticleType` de Minecraft/Forge. Si luego quieres, lo extendemos a `block`, `dust`, `item`, etc.

## Notas de `.str`

- `.str` no es el formato interno del runtime.
- Usa `type: "str_layered"` para importar y adaptar a `layered`.
- El archivo fuente debe vivir dentro de resources del mod o de un resource pack.
- Si el parser encuentra datos no soportados, se registran warnings y se degrada el comportamiento en vez de inventarlo silenciosamente.
