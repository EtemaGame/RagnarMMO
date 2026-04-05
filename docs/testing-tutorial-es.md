# RagnarMMO - Tutorial de Testeo

Tutorial de testeo rehecho para el estado actual del mod, usando debug mode por comando y pensado para validar con datos reales en logs.

Log principal en entorno local:

- `run/logs/latest.log`

Logs utiles si pruebas fuera del entorno `runClient`:

- Servidor dedicado: `D:\MinecraftTestServer\RagnarMMO\logs\latest.log`
- Cliente Curse: `D:\MinecraftCurse\Instances\Ragnar MMO\logs\latest.log`

## 0. Preparacion

1. Ejecuta `runClient`.
2. Crea un mundo con `Cheats ON`.
3. Empieza en `Creative` para setup rapido.
4. Abre `run/logs/latest.log` en paralelo.

## 1. Activar Debug

Usa esto al entrar al mundo:

```mcfunction
/ragnar admin debug all on
/ragnar admin debug status
```

Si quieres solo por categoria:

```mcfunction
/ragnar admin debug combat on
/ragnar admin debug player on
/ragnar admin debug mobs on
/ragnar admin debug bosses on
```

Prefijos esperados en el log:

- `[RO-DEBUG][COMBAT]`
- `[RO-DEBUG][PLAYER]`
- `[RO-DEBUG][MOB]`
- `[RO-DEBUG][BOSS]`

## 2. Sanity Check Basico

Valida que el core carga y responde.

```mcfunction
/ragnar stats
```

Luego prueba:

- `V`: stats
- `K`: skills
- `Left Alt`: combat mode

Que revisar:

- ves `STR/AGI/VIT/INT/DEX/LUK`
- ves `HP/SP`, `HIT/FLEE/CRIT/ASPD/P.DODGE`
- Combat Mode se activa y desactiva

## 3. Player Data y Sync

Esto comprueba progresion dual y sincronizacion.

```mcfunction
/ragnar exp lv 5000
/ragnar exp joblv 5000
/ragnar admin debug player on
```

Que hacer:

1. Abre `V`.
2. Sube algunos stats con `+`.
3. Cierra y vuelve a abrir la pantalla.
4. Cambia de dimension o reloguea.

Que buscar en log:

- `JOIN`
- `SYNC source=service`
- `SYNC source=tick`
- `KILL_XP`
- `DEATH_PENALTY`

Que validar:

- Base Level y Job Level suben por separado
- Stat Points y Skill Points cambian bien
- no hay desync raro entre UI y servidor

## 4. Jobs

Prueba el flujo `Novice -> 1st job -> 2nd job`.

```mcfunction
/ragnar exp joblv 10000
```

Haz esto:

1. Siendo `Novice`, sube `Basic Skill` hasta Lv 9.
2. Gasta skill points pendientes.
3. Cambia a una `1st class` desde la UI.
4. Luego sube Job Level y prueba `2nd class`.

Rutas utiles:

- `Swordsman -> Knight`
- `Mage -> Wizard`
- `Archer -> Hunter`
- `Acolyte -> Priest`
- `Thief -> Assassin`
- `Merchant -> Blacksmith`

Que validar:

- no resetea Base Level
- si reinicia progreso de Job cuando corresponde
- las restricciones de promocion si se respetan

## 5. Skills y Combate

Activa logs de combate:

```mcfunction
/ragnar admin debug combat on
```

Aprende una skill y asignala al hotbar:

1. Pulsa `K`.
2. Pasa el mouse sobre la skill.
3. Pulsa `1`.
4. Cierra `K`.
5. Usa `Left Alt` para Combat Mode.

Invoca objetivos:

```mcfunction
/summon minecraft:zombie
/summon minecraft:skeleton
/summon minecraft:spider
```

Pruebas recomendadas:

- `Swordsman`: Bash
- `Mage`: Fire Bolt
- `Acolyte`: Heal

Que buscar en log:

- `ATTACK result=MISS`
- `ATTACK result=HIT`
- `ATTACK result=CRIT`
- `DEFEND result=MISS`
- `DEFEND result=HIT`
- `PERFECT_DODGE`

Que validar:

- hay miss reales
- hay crit reales
- el dano final cambia por defensa
- el consumo de SP se nota en runtime

## 6. Refine

Esto valida backend de refine, costo y feedback.

Consigue un item refinable en mano principal y materiales desde creative tab del mod.

Comandos utiles:

```mcfunction
/roitems dump_held_item
/roitems refine_info
/roitems try_refine
/roitems refine 5
```

Que validar:

- el item muestra refine en tooltip
- `try_refine` consume material y costo correctamente
- el item cambia stats reales
- el refine influye en combate despues

## 7. Mob Scaling

Activa logs de mobs:

```mcfunction
/ragnar admin debug mobs on
```

Invoca mobs:

```mcfunction
/summon minecraft:zombie
/summon minecraft:blaze
/summon minecraft:enderman
```

Que buscar en log:

- `SPAWN mob=... tier=... level=... class=... stats=...`

Que validar:

- cada mob recibe tier
- recibe level
- recibe class
- las stats y multiplicadores no salen absurdos

## 8. Bosses del Mundo

Activa logs de bosses:

```mcfunction
/ragnar admin debug bosses on
```

Primero prueba spawn directo:

```mcfunction
/ragnar mobstats boss spawn minecraft:enderman boss altar void_walker 300
/ragnar mobstats boss
/ragnar mobstats boss cooldown void_walker
```

`/ragnar mobstats boss state` es equivalente al listado de estado.

Luego mata al boss y revisa otra vez:

```mcfunction
/ragnar mobstats boss
/ragnar mobstats boss cooldown void_walker
```

Que buscar en log:

- `SPAWN_CONTROLLED result=OK`
- `REGISTER boss=...`
- `DEFEAT boss=...`
- `cooldownStart`
- `cooldownEnd`

Que validar:

- se registra en world state
- al morir entra a cooldown
- no deja respawnear si sigue en cooldown

## 9. Altar con Sigilo

Genera un sigilo:

```mcfunction
/ragnar mobstats boss sigil give @s minecraft:enderman boss void_walker 300
```

Pasos:

1. Coloca un `Lodestone`.
2. Usa el `Boss Sigil` en la cara superior.

Que validar:

- aparece el boss
- usa el mismo `spawnKey`
- respeta cooldown igual que el spawn directo

## 10. Persistencia

Despues de probar stats, refine y bosses:

1. Sal del mundo.
2. Vuelve a entrar.

Que revisar:

- stats y job siguen bien
- el item refinado conserva refine
- el boss cooldown sigue guardado

## 11. Lectura Rapida del Log

Si quieres revisar por bloques, busca estas cadenas:

```text
"[RO-DEBUG][PLAYER]"
"[RO-DEBUG][COMBAT]"
"[RO-DEBUG][MOB]"
"[RO-DEBUG][BOSS]"
```

## 12. Apagado Rapido

Cuando termines:

```mcfunction
/ragnar admin debug all off
/ragnar admin debug status
```

## Notas

- Este flujo esta pensado para `runClient`, pero las mismas cadenas de debug sirven para validar en cliente externo o servidor dedicado.
- Si una prueba falla, guarda el comando usado, el resultado visible y la linea relevante del `latest.log`.
