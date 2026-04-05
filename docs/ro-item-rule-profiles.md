# RO Item Rule Profiles

El sistema de reglas RO para items ahora resuelve en este orden:

1. `fallbacks` por tipo de equipo
2. `modEquipmentTypes` por `modid + tipo`
3. reglas por tag
4. regla por item exacto

Esto permite que un arma de un mod desconocido siga mostrando el bloque RO base
como si fuera equipo vanilla, pero deja espacio para sobreescribir luego con
stats, jobs, slots o requisitos mas precisos.

## Archivo base

Ejemplo en [fallback_profiles.json](/d:/Mods/RagnarMMO/src/main/resources/data/ragnarmmo/ro_item_rules/fallback_profiles.json)

```json
{
  "fallbacks": {
    "weapon": { "showTooltip": true }
  },
  "modEquipmentTypes": {
    "minecraft": {
      "weapon": {
        "showTooltip": true,
        "requiredBaseLevel": 1
      }
    }
  }
}
```

## Campos soportados

- `displayName`
- `attributeBonuses`
- `requiredBaseLevel`
- `allowedJobs`
- `cardSlots`
- `showTooltip`

## Tipos validos

- `weapon`
- `armor`
- `headgear`
- `shoes`
- `garment`
- `accessory`
- `shield`

## Recomendacion de uso

- Usa `fallbacks` para comportamiento vanilla minimo.
- Usa `modEquipmentTypes` cuando un mod instalado tenga defaults claros.
- Usa tags para familias amplias de items compartidos entre mods.
- Usa item exacto para excepciones importantes.
