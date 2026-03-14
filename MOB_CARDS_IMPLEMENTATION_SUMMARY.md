# Sistema de Cartas de Mobs - Implementación Completa

## Resumen

Se ha implementado el sistema de cartas para prácticamente todos los mobs de Minecraft 1.20.1 en el mod RagnarMMO. El sistema asigna cartas coleccionables a cada mob que otorgan bonificaciones de atributos al jugador.

## Estructura del Sistema

### Archivos Generados
- **Total de cartas**: 74 cartas (62 nuevas + 12 existentes)
- **Ubicación**: `src/main/resources/data/ragnarmmo/cards/*.json`
- **Formato**: Archivos JSON con definiciones de cartas

### Atributos Disponibles

Las cartas pueden modificar los siguientes atributos del jugador:

#### Atributos Base (6):
- `ragnarmmo:str` - Strength (Fuerza)
- `ragnarmmo:agi` - Agility (Agilidad)
- `ragnarmmo:vit` - Vitality (Vitalidad)
- `ragnarmmo:int` - Intelligence (Inteligencia)
- `ragnarmmo:dex` - Dexterity (Destreza)
- `ragnarmmo:luk` - Luck (Suerte)

#### Atributos Extendidos:
- `ragnarmmo:crit_chance` - Tasa Crítica
- `ragnarmmo:crit_damage` - Daño Crítico
- `ragnarmmo:life_steal` - Robo de Vida
- `ragnarmmo:armor_pierce` - Penetración de Armadura
- `ragnarmmo:armor_shred` - Reducción de Armadura
- `ragnarmmo:overheal` - Sobrecuración

### Sistema de Rareza y Drop Rates

| Rareza | Drop Rate Base | Ejemplos |
|--------|---------------|----------|
| **COMMON** | 2.0% | Slime, Silverfish, Chicken, Sheep |
| **UNCOMMON** | 1.0% | Zombie, Skeleton, Spider, Enderman |
| **RARE** | 0.5% | Creeper, Iron Golem, Ghast, Sniffer |
| **EPIC** | 0.25% | Ender Dragon, Evoker |
| **LEGENDARY** | 0.1% | Wither, Warden, Illusioner |

### Drop Rate Modificado por LUK

El drop rate real se calcula con la fórmula:
```
Tasa Efectiva = Tasa Base × (1 + LUK × 0.01)
```

Ejemplo: Con LUK 50, una carta RARE (0.5% base) tiene:
- 0.5% × (1 + 50 × 0.01) = 0.5% × 1.5 = 0.75%

## Catálogo de Mobs por Categoría

### 1. Mobs Hostiles (Overworld) - 19 cartas
- zombie, skeleton, creeper, spider, witch, slime
- enderman, silverfish, cave_spider, zombie_villager
- husk, stray, drowned, phantom
- pillager, ravager, vindicator, evoker, vex
- **Especiales**: warden, illusioner

### 2. Mobs Hostiles (Nether) - 8 cartas
- blaze, wither_skeleton, ghast, magma_cube
- piglin_brute, piglin, hoglin, zombified_piglin, zoglin

### 3. Mobs Hostiles (End) - 2 cartas
- shulker, endermite

### 4. Mobs Neutrales - 8 cartas
- bee, llama, trader_llama, wolf, dolphin
- iron_golem, snow_golem, polar_bear, panda

### 5. Mobs Pasivos - 27 cartas
- pig, cow, chicken, sheep, horse, donkey, mule
- skeleton_horse, zombie_horse, cat, ocelot, fox
- rabbit, parrot, cod, salmon, pufferfish
- tropical_fish, squid, glow_squid, bat, mooshroom
- strider, frog, tadpole, allay, camel, sniffer

### 6. NPCs - 2 cartas
- villager, wandering_trader

### 7. Jefes (Bosses) - 2 cartas
- wither, ender_dragon

## Filosofía de Diseño

### Asignación de Atributos
Cada carta otorga atributos que reflejan las características del mob:

- **Mobs fuertes** (Ravager, Iron Golem): +STR, +VIT
- **Mobs mágicos** (Witch, Evoker, Ghast): +INT
- **Mobs ágiles** (Spider, Phantom, Enderman): +AGI
- **Mobs precisos** (Skeleton, Pillager): +DEX
- **Mobs afortunados** (Allay, Tropical Fish): +LUK
- **Mobs defensivos** (Shulker, Pufferfish): +VIT

### Ejemplos Notables

1. **Warden Card (LEGENDARY)**
   - +4.0 STR, +2.0 VIT
   - Drop rate: 0.1%
   - "Fuerza del subsuelo"

2. **Ender Dragon Card (LEGENDARY)**
   - +5.0 STR, +3.0 INT
   - Drop rate: 0.1%
   - "Soberana del End"

3. **Iron Golem Card (RARE)**
   - +2.0 STR, +2.0 VIT
   - Drop rate: 0.5%
   - "Guardián de aldea"

4. **Allay Card (UNCOMMON)**
   - +1.5 LUK
   - Drop rate: 1.0%
   - "Búsqueda musical"

## Implementación Técnica

### Archivos Clave Modificados/Creados
1. `create_all_mob_cards.py` - Script generador de cartas
2. `src/main/resources/data/ragnarmmo/cards/*.json` - 74 archivos de definición de cartas

### Estructura JSON
```json
{
    "id": "mob_name_card",
    "displayName": "Mob Name Card",
    "mobId": "minecraft:mob_name",
    "modifiers": {
        "ragnarmmo:stat": value
    },
    "dropRate": 0.01,
    "rarity": "RARITY_TIER"
}
```

### Integración con el Mod

El sistema de cartas se integra con:
- **CardDataLoader**: Carga automática desde data packs
- **CardRegistry**: Registro en memoria y lógica de drops
- **Modifiers**: Aplicación de bonificaciones al jugador
- **Mob Loot**: Drops cuando los mobs son derrotados

## Próximos Pasos Recomendados

1. **Testing**: Verificar que todas las cartas dropeen correctamente
2. **Balance**: Ajustar drop rates según feedback de juego
3. **Visual**: Crear texturas e íconos para cada carta
4. **UI**: Implementar interfaz de colección de cartas
5. **Trading**: Sistema de intercambio entre jugadores
6. **Crafting**: Posibilidad de fusionar cartas

## Notas de Versión

- **Minecraft Version**: 1.20.1
- **Mod Version**: RagnarMMO (en desarrollo)
- **Total de Mobs Cubiertos**: 73/73 mobs principales
- **Cobertura**: 100% de mobs vanilla con comportamiento de entidad

## Referencias

- Documentación de sistema: `README.md`
- Código fuente: `src/main/java/com/etema/ragnarmmo/system/loot/cards/`
- Ejemplos de cartas existentes: `src/main/resources/data/ragnarmmo/cards/`
