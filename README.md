# RagnarMMO Mod - Guía Completa

Bienvenido a **RagnarMMO**, un mod para Minecraft que adapta la profundidad, las matemáticas y el espírituRPG clásico de *Ragnarok Online (Pre-Renewal)* a tu mundo de bloques. Este mod transforma completamente el combate de Minecraft, reemplazándolo por un ecosistema de atributos, estadísticas, clases, progresión paralela y habilidades.

A continuación, encontrarás la guía definitiva y detallada de cada función del mod de comienzo a fin.

---

## 🌟 1. Sistema de Progresión Dual

RagnarMMO elimina el uso estándar de la experiencia de Minecraft y lo divide en dos niveles paralelos que suben de forma independiente al matar mobs o realizar actividades:

1. **Base Level (Nivel Base)**: Aumenta tu poder físico/mágico en general y te otorga **Status Points (Puntos de Atributo)** para mejorar tus estadísticas básicas.
2. **Job Level (Nivel de Clase)**: Define tu maestría en tu profesión actual. Al subir, te otorga **Skill Points (Puntos de Habilidad)** para desbloquear o mejorar tus hechizos y pasivas. Al alcanzar ciertos niveles máximos, te permite cambiar de clase.

---

## 📊 2. Atributos (Stats) y sus Fórmulas

Cada vez que subes de Nivel Base, obtienes puntos para invertir libremente pulsando la tecla `V` (por defecto). Cada estadística impacta profundamente en tu personaje:

*   **STR (Fuerza)**
    *   **Melee ATK:** Aumenta exponencialmente el daño físico cuerpo a cuerpo.
    *   **Weight Limit:** Determina tu Límite de Peso en el inventario (+50 de capacidad por cada punto).
*   **AGI (Agilidad)**
    *   **Flee (Evasión):** Aumenta tu probabilidad de esquivar ataques físicos enemigos por completo (+1 Flee por punto).
    *   **ASPD (Attack Speed):** Aumenta la velocidad a la que golpeas.
*   **VIT (Vitalidad)**
    *   **Max HP:** Incrementa exponencialmente tu vida máxima según tu clase.
    *   **HP Regen & Soft DEF:** Aumenta la regeneración natural de vida y te otorga defensa física plana (reducción directa de daño numérico).
    *   **Status Resistance:** Reduce la probabilidad de sufrir estados alterados dañinos y su duración (ej. Veneno, Sangrado).
*   **INT (Inteligencia)**
    *   **Max SP & Regen:** Aumenta el Maná máximo y su regeneración por segundo.
    *   **MATK (Magic Attack) & Soft MDEF:** Aumenta la potencia de tus hechizos mágicos y te da resistencia plana contra la magia enemiga.
    *   **Cast Time:** Disminuye el tiempo de casteo dinámico de los hechizos.
*   **DEX (Destreza)**
    *   **Hit (Precisión):** Vital para no fallar golpes contra monstruos de alto nivel (+1 Hit por punto).
    *   **Ranged ATK:** Es el atributo principal para aumentar el daño con Arcos y Ballestas.
    *   **Cast Time & ASPD:** Reduce bruscamente el tiempo de casteo (mucho más rápido que INT) y aporta ligeramente a la Velocidad de Ataque.
    *   **Damage Variance:** Estabiliza el daño mínimo de tus armas, haciendo que pegues tu daño máximo más frecuentemente.
*   **LUK (Suerte)**
    *   **Critical Hit:** Aumenta la probabilidad de asestar golpes críticos (+0.3% por punto).
    *   **Perfect Dodge:** Otorga una chance absoluta de esquivar ataques, ignorando el "Hit" del enemigo (+0.1% por punto).
    *   Aporta incrementos menores a casi todos los demás atributos (ATK, MATK, HIT, FLEE, Crit Shield).

---

## 🐣 3. El Sistema de Clases (Jobs)

Empiezas tu aventura como **Novice**.

*   **Novice (Max Job Lv. 10):** Debes invertir tus puntos en habilidades básicas de supervivencia (Basic Conditioning, First Aid). Una vez seas Job Level 10, podrás evolucionar a tu "Primer Job".
*   **First Classes (Max Job Lv. 50):**
    *   🛡️ **Swordman**: Tanques formidables especializados en aguantar daño (Endurance) y dominar armas cuerpo a cuerpo.
    *   🗡️ **Thief**: Maestros del esquive (Flee) y golpes rápidos. Ideales para armados basados en AGI.
    *   🏹 **Archer**: Especialistas en daño a distancia masivo valiéndose primariamente de DEX.
    *   🧙 **Mage (WIP)**: Lanzadores de elementales puros con altos tiempos de casteo pero destrucción en red (AoE).
    *   ⛪ **Acolyte**: Soporte. Curan mediante fe, bendicen a los aliados aumentando sus sus estadísticas base y son el némesis de mobs tipo Undead.
    *   💰 **Merchant**: Maestros del inventario. Utilizan el **Carro (Cart)** para sobrepasar ampliamente los límites de peso de Minecraft y atacan usando sus recursos.

*(El mod está diseñado para alojar "Second Classes" como Knight, Assassin, Wizard, Priest, etc., que se irán implementando paulatinamente).*

---

## ⚔️ 4. Mecánicas de Combate Clásico (El "Core")

RagnarMMO inyecta un loop de combate totalmente matemático en el fondo de Minecraft:

### Hit vs Flee (Precisión vs Evasión)
No todos tus golpes impactarán. Al atacar un mob, tu `HIT` se compara con el `FLEE` del enemigo. Si la fórmula indica que vas a fallar, verás partículas de "Miss" y no infligirás daño. Lo mismo aplica a la inversa: si vas a AGI pura, los zombis raramente lograrán golpearte.

### Critical Hits & Crit Shield
Si un golpe resulta ser **Crítico** (basado en LUK):
1. Sumará un multiplicador extra de daño.
2. **Ignorará completamente** el Flee del enemigo (Hit 100% garantizado) así como su defensa blanda (Soft DEF).
*A la inversa, los enemigos de alto nivel y LUK poseen un "Crit Shield", el cual resta probabilidad absoluta a tus chances de atinarles un crítico.*

### Attack Speed (ASPD) y la Penalización de Escudo
Puedes pegar a gran velocidad invirtiendo en AGI. Sin embargo, equiparte un **Escudo en la mano secundaria (Off-Hand)** reduce automáticamente tu ASPD base de forma notable. Es el clásico balance de Ragnarok: *¿Daño rápido con arma de dos manos, o supervivencia lenta con escudo?*

### Cast Time y Global Cooldown (Cast Delay)
Al usar habilidades activas (Magia de Mago, curas de Acolyte):
*   **Cast Time:** Varias habilidades tienen una barra de casteo que te inmoviliza antes de salir. Reducido drásticamente por DEX y ligeramente por INT.
*   **Cast Delay:** Una vez terminada y ejecutada la habilidad (o instantáneas), todas tus habilidades entrarán en un Enfriamiento Global bloqueándote de "spamear" magias. Este tiempo viene escrito en la descripción de cada hechizo en Ticks.

### Modificadores Data-Driven (Elemento / Raza / Tamaño)
A través de los *Datapacks* de NeoForge (`#ragnarmmo:races/demihuman`, `#ragnarmmo:sizes/large`, `#ragnarmmo:elements/fire`), el combate aplica multiplicadores ocultos. Una espada con elemento Agua infligirá 1.5x (150%) más daño a un enemigo marcado con el elemento Fuego. Las dagas tienen penalización si atacas a enemigos gigantes.

### Restricciones de Peso (Weight Limit System)
Cargar demasiados ítems pesados en tu inventario acarrea penalizaciones extremas:
*   **> 50% de Peso:** Tu regeneración natural de HP y SP se deshabilita.
*   **> 90% de Peso:** Entras en estado de extrema lentitud (Slowness severo) y no puedes regenerar pasivamente.
*Sube Fuerza (STR) para cargar más, o hazte Merchant.*

---

## 💎 5. Equipamiento y Rarezas

Los items caídos de monstruos no son estáticos. Al caer, vienen sin identificar.
*   Una vez generados, adquirirán un **Tier de Rareza** (Common, Uncommon, Rare, Epic, Ancient, Legendary, Unique) dictaminado por un color en su nombre.
*   Dependiendo la rareza, obtendrán líneas de stats al azar (+X Fuerza, +X Ataque Mágico, +X% Velocidad).

---

## ⚒️ 6. Habilidades de Vida (Life Skills) y Perks

Lejos del combate, tu labor en Minecraft también te otorga XP paralela categorizada en 6 áreas de Life Skills: **Mining, Woodcutting, Excavation, Farming, Fishing y Exploration.**
*   Cada 10 niveles en una Life Skill, abrirás el "Perk Tree", pudiendo elegir 1 entre 2 ventajas únicas de ese Tier. (Ejemplo en Minería de Nivel 10: *Elegir entre +5% Velocidad de Pico o +10% de sacar doble mineral*).

---

## 👥 7. Sistema de Party y Escalado de Dificultad del Mundo

**Este mod no está hecho para ser fácil.**
*   **Dificultad por zonas:** Los mobs son Nivel 1 cerca del respawn, pero mientras más viajas a coordenadas lejanas, bajas a las profundidades de las cavernas, o te acercas a Estructuras RPG (Dungeons), su nivel, daño y vida escala de forma brutal. Las criaturas tendrán auras (Élite, Boss) y barras de vida sobre su cabeza mostrando su HP real.
*   **Grupos y Party:** Puedes crear partys. La **Experiencia se comparte** si están en un radio de 50 bloques, y recibe un bono porcentual (mientras más miembros, más exp extra global generada para todos).
*   **Escalado Dinámico:** Para evitar que vayas con 10 amigos y destrocen un jefe fácilmente, cuando haya múltiples jugadores en la misma área de combate, el HP máximo y el Ataque Base de los monstruos se multiplicará en tiempo real para acomodar el desafío al número de atacantes.

---

## ⌨️ 8. Comandos e Interfaces

**Menús y Teclas**
*   **`V` (Por Defecto)**: Abre el menú de jugador principal. Posee pestañas para ver el estado general (Fórmula directa de daño de ATK, Hitrates, ASPD exacto), la distribución de Atributos, el Árbol de Habilidades (Skills) y el Árbol de Life Skills.
*   La UI de Vida predeterminada de Minecraft se oculta; es suplantada por orbes de HP y SP configurables y movibles.

**Comandos Útiles (`/`)**
*   `/mobstats`: Al mirar a un monstruo, te desglosa matemáticamente sus stats de RO (Nivel, Defensa Blanda, Flee, Salud), ignorando las barreras de Minecraft.
*   `/mobstats difficulty`: Te avisa la zona de riesgo actual en la que te encuentras parado (Bioma y Lejanía).
*   `/ragnar party`: Administrar membresías, invitaciones y salidas de tu grupo de caza.
*   `/ragnar cart`: Para los Merchants, abre a distancia su segundo inventario blindado a peso.

*(El mod es altamente configurable vía `config/ragnarmmo-common.toml` y Datapacks).*
