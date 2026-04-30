# RagnarMMO

RagnarMMO is a Forge 1.20.1 overhaul mod that brings a Ragnarok Online inspired progression and combat model into Minecraft.

The goal is not to add a generic RPG layer on top of vanilla combat. RagnarMMO replaces Minecraft combat balance with an RO-first system: base/job progression, classic stats, HIT/FLEE, CRIT, ASPD, cast timing, skill routing, mob profiles, EXP, party rewards, equipment rules, cards and refinement.

Minecraft remains the world, input, rendering, AI shell and persistence layer. RagnarMMO owns the RPG rules.

## Current Highlights

- Dual progression: Base Level and Job Level.
- Classic stats: STR, AGI, VIT, INT, DEX and LUK.
- RO-style combat math: HIT vs FLEE, CRIT, Perfect Dodge, DEF/MDEF, ASPD and skill damage contracts.
- Packet-first server-authoritative combat pipeline.
- Basic attacks, combat skills and mob attacks are routed through RagnarMMO combat contracts instead of vanilla damage formulas.
- Skill system with trees, job requirements, cast timing, delays, cooldowns and resource costs.
- Jobs from Novice into first and second class paths.
- Dynamic mob profiles with level, tier, race, element, size, ATK/MATK, DEF/MDEF, HIT/FLEE and base/job EXP.
- Bestiary and mob inspection tools.
- Party system with shared base/job EXP.
- Zeny currency, item restrictions, cards, equipment rules and refinement.
- Life skills: Mining, Woodcutting, Excavation, Farming, Fishing and Exploration.

## Jobs

Start as a Novice and advance into classic class roles.

First jobs:

- Swordsman
- Mage
- Archer
- Thief
- Merchant
- Acolyte

Second jobs currently represented:

- Knight
- Wizard
- Hunter
- Assassin
- Blacksmith
- Priest

## Stats

RagnarMMO uses RO-style stat identity:

- STR increases physical attack and weight-related power.
- AGI improves attack rhythm and FLEE.
- VIT improves HP and physical durability.
- INT improves MATK, SP and magical durability.
- DEX improves HIT and reduces variable cast time.
- LUK improves CRIT and Perfect Dodge.

## Combat Direction

Recent versions focus heavily on making combat feel closer to classic Ragnarok Online:

- Vanilla attack cooldown is not the source of attack speed.
- Vanilla attack damage and armor are not used as combat balance inputs.
- Damage skills produce combat intent; the combat engine resolves hit, damage and mitigation.
- Mob definitions describe monster identity and baselines; runtime profiles scale full RO stats, not only HP.
- Environmental damage such as fall, lava and void remains Minecraft damage unless explicitly wrapped later.

This is an active system and balance is still evolving. Feedback on feel, TTK, skill rhythm and mob progression is especially valuable.

## Controls And Commands

| Key / Command | Action |
| --- | --- |
| V | Open character status and stat allocation |
| K | Open the skill tree |
| Y | Open achievements / quest log |
| Left Alt | Toggle combat mode and skill hotbar |
| 1-6 | Use assigned skills while in combat mode |
| `/stats` | Show your character stats |
| `/party create <name>` | Create a party |
| `/pc <message>` | Send party chat |
| `/ragnar mobmanualui` | Open the mob catalog |
| `/mobstats` | Inspect the targeted mob profile |

## Server And Modpack Configuration

Most systems are configurable through `ragnarmmo-server.toml`, including:

- Level caps and EXP rates.
- Mob scaling modes and difficulty behavior.
- Party sharing and anti-farm behavior.
- Item restrictions, card slots and refinement settings.
- Economy-related values.

The mod is intended to be modpack-friendly, but the combat system deliberately avoids using vanilla attack damage, armor and cooldown as balance sources.

## Development Status

RagnarMMO is in active development. Many systems are playable, but some content, balance values, skill behaviors and translations may still change between builds.

Useful feedback includes:

- Combat feel and attack rhythm.
- Mob time-to-kill at different levels.
- Skill cast/delay/cooldown behavior.
- Progression speed for base/job levels.
- Missing or incorrect translations.
- Compatibility issues with other mods.

Discord: <https://discord.gg/sZ64CgX3JN>

## Project Goal

RagnarMMO aims to make Minecraft combat and progression feel like an RO-inspired MMORPG while still preserving Minecraft as the explorable world.

