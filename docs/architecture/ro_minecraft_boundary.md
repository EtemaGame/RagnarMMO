# RO/Minecraft Boundary

RagnarMMO treats Ragnarok Online pre-renewal rules as the gameplay domain and
Minecraft as the runtime shell.

Minecraft is responsible for input, rendering, AI/pathfinding scaffolding,
networking, entity persistence, and compatibility surfaces such as health bars
or vanilla attributes needed by AI.

RagnarMMO is responsible for combat stats, combat formulas, damage resolution,
base/job EXP, party sharing, companions, drops, and balance. Vanilla attack
damage, armor, attack cooldown, max health, and `LivingHurtEvent#getAmount()`
must not be used as RO balance inputs.

Passive combat modifiers and attack procs, such as mastery bonuses, target-race
bonuses, Double Attack, and future counter/secondary-hit effects, are part of
the RO combat domain. They must be resolved from explicit combat profiles,
skill state, and contract-owned RNG rather than by mutating `LivingHurtEvent`
damage after the fact.

Secondary combat actions, such as Auto Counter and Auto Blitz, may be triggered
by Minecraft events only after a valid RO combat outcome exists. Their damage is
still applied through RagnarMMO combat services and marked as contract damage so
vanilla hurt-event formulas cannot recalculate or stack it.

Players, mobs, companions, and summons must resolve to an explicit RO combatant
kind before combat resolution. Companions and summons may use Minecraft AI and
ownership APIs for behavior, but their damage, hit chance, kill credit, and
reward attribution must pass through RagnarMMO domain services.

Any intentional deviation from Ragnarok Online pre-renewal rules must be named
as a Minecraft adaptation near the code that implements it.
