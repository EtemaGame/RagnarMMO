# RagnarMMO Mod

Bienvenido a **RagnarMMO**, un mod de Minecraft que traslada la profundidad clásica de los RPG de Ragnarok Online a tu mundo de bloques. Este mod introduce un robusto sistema de progresión con Trabajos (Jobs), Atributos (Stats), Habilidades (Skills) y Habilidades de Vida, todo perfectamente integrado con la experiencia de Minecraft.

## 🌟 Resumen

RagnarMMO cambia el enfoque de los niveles tradicionales de Minecraft a un sistema de doble nivelación:

1. **Nivel Base**: Aumenta tu poder general y otorga **Puntos de Atributo**.
2. **Nivel de Trabajo (Job Level)**: Desbloquea y potencia tus **Habilidades** a la vez que proporciona **Puntos de Habilidad**.

---

## 🎮 Sistema de Progresión

### 🐣 El Sistema de Trabajos (Jobs)

Tu aventura comienza como un **Novice** (Novicio).

- **Novice**: Nivel máximo de trabajo 10. Al alcanzarlo, puedes cambiar a una Primera Clase.
- **First Class**: Nivel máximo de trabajo 50.
  - 🛡️ **Swordman** (Espadachín): Tanques de primera línea con gran poder físico.
  - 🗡️ **Thief** (Ladrón): Maestros de la evasión, el sigilo y los golpes críticos.
  - 🏹 **Archer** (Arquero): Especialistas a distancia con precisión y movilidad.
  - 🧙 **Mage (WIP)** (Mago): Lanzadores de hechizos elementales con gran ráfaga mágica.
  - ⛪ **Acolyte** (Acólito): Apoyo que cura aliados y purifica a los no-muertos.
  - 💰 **Merchant** (Mercader): Expertos en comercio con capacidad de carga pesada (Carros).

### 📊 Atributos Base

Invierte tus Puntos de Atributo en seis estadísticas principales:

- **STR (Fuerza)**: Aumenta el Daño Cuerpo a Cuerpo y la **Capacidad de Peso**.
- **AGI (Agilidad)**: Mejora la **Velocidad de Ataque (ASPD)** y el **Flee Rate** (Evasión).
- **VIT (Vitalidad)**: Aumenta la Vida Máxima y la **Defensa Física (DEF)**.
- **INT (Inteligencia)**: Aumenta el Maná Máximo (SP), el **Ataque Mágico (MATK)** y la Defensa Mágica (MDEF).
- **DEX (Destreza)**: Mejora el **Hit Rate** (Precisión), la Velocidad de Casteo y la estabilidad del daño.
- **LUK (Suerte)**: Aumenta la **Tasa de Crítico** y el Esquive Perfecto.

---

## ⚔️ Combate y Escalado de Mobs

### 🛡️ Escalado de Zonas
¡El mundo es más peligroso! Los niveles de los mobs se calculan dinámicamente según:
- **Distancia**: El nivel aumenta a medida que te alejas del spawn (progresión por anillos).
- **Biomas**: Los biomas tienen niveles de dificultad (Fácil, Medio, Difícil, Muy Difícil).
- **Profundidad**: Ir bajo tierra o a cuevas profundas aumenta el poder de los mobs.
- **Estructuras**: La cercanía a mazmorras y estructuras otorga un bono de nivel.

### 👥 Escalado de Dificultad Multijugador
¡Los mobs ya no son un paseo para los grupos!
- **Stats Dinámicos**: Los jugadores cercanos activan un multiplicador de Vida y Ataque para los mobs.
- **Ajuste para Solitario**: Los multiplicadores base de bosses están ajustados para ser viables solos, pero escalan para grupos.

### 📊 Indicadores de Vitalidad
- **Color de Brillo**: Indica el nivel de dificultad (Normal, Élite, Boss).
- **Vida Numérica**: Indicadores de vida `[Actual / Máxima]` en tiempo real sobre los mobs.

---

## 💎 Sistema de Rarezas y Equipo Escalonado

¡El equipo ya no es estático! Todo el equipo puede soltarse con atributos únicos:

- **Rangos de Rareza**: Common, Uncommon, Rare, Epic, Ancient, Legendary y Unique.
- **Stats Aleatorios**: Las rarezas altas otorgan más "slots de stats extra" y mejores rangos de valores para los atributos.
- **Visuales**: Los nombres de los items y sus descripciones están codificados por colores según su rareza.
- **Identificación**: Los items soltados por mobs se identifican al aparecer, revelando su potencial.

---

## ⚒️ Habilidades de Vida

La progresión se extiende más allá del combate. Mejora tus capacidades de recolección y exploración:

- ⛏️ **Mining** (Minería): Mayor velocidad y hallazgo de minerales raros.
- 🪓 **Woodcutting** (Tala): Tala más rápida y mayor rendimiento de madera.
- 🏺 **Excavation** (Excavación): Encuentra tesoros en tierra y grava.
- 🚜 **Farming** (Agricultura): Acelera el crecimiento de cultivos y mejora la cosecha.
- 🎣 **Fishing** (Pesca): Picadas más rápidas y mejores capturas.
- 🗺️ **Exploration** (Exploración): Bonificaciones al descubrir nuevos chunks y estructuras.

---

## ⚙️ Sistemas Clave

### 🛒 Sistema de Peso

Tu inventario está limitado por el peso (influenciado por STR).

- **Sobrepeso (50%+)**: La regeneración natural de Vida y Maná se desactiva.
- **Sobrepeso (90%+)**: No puedes atacar ni usar habilidades.
- *Consejo: Los Mercaderes pueden usar Carros para evitar muchas restricciones de peso.*

### ⚡ Maná (SP)

Los hechizos y habilidades activas consumen Maná. **INT** es tu fuente principal tanto para el SP máximo como para la velocidad de regeneración.

### 🛠️ Mantenimiento y Durabilidad

El equipo se degrada, pero los Mercaderes pueden aprender habilidades para reducir la pérdida de durabilidad en armas y armaduras.

### 👥 Sistema de Grupo y XP Compartida

¡Forma grupos para progresar juntos!
- **XP Compartida**: Activada en un **rango de 50 bloques**.
- **Factores de Eficiencia**: La XP se reparte con un bono de grupo (hasta un 342% de XP total para 6 miembros).
- **HUD de Grupo**: Seguimiento en tiempo real de la Vida, Niveles y XP de los miembros.

---

## ⌨️ Controles y Comandos

### Atajos de Interfaz

- **Abrir Pantalla de Stats**: `V` (Por defecto) - Gestiona atributos y habilidades.
- **Ajustes de HUD**: Personaliza la posición del HUD desde la pantalla de Stats.

### Comandos de Jugador

- `/mobstats`: Desglose detallado de los stats del mob al que apuntas.
- `/mobstats difficulty`: Mira las reglas de escalado activas y el estado del bioma actual.
- `/ragnar stats`: Mira tu nivel actual y atributos.
- `/ragnar skills`: Lista tus habilidades aprendidas y sus niveles.
- `/ragnar party`: Gestiona tu grupo (crear, unirse, invitar).
- `/ragnar cart`: (Solo Mercader) Gestiona tu carro.

---

## 🛠️ Instalación y Configuración

- **Configuración**: Los archivos de configuración se encuentran en `config/ragnarmmo-common.toml`.
- **Requisitos**: Minecraft Forge 1.20.1.

*RagnarMMO está actualmente en desarrollo activo. Las características están sujetas a reequilibrios.*
