/**
 * <b>Combat Skills Module</b>
 * <p>
 * Job-based skill system with 9 archetypes (Swordman, Mage, Archer, Thief,
 * Acolyte,
 * Merchant, Novice + Global, Life). Handles skill effects, cooldowns, mana
 * costs,
 * hotbar management, and leveling.
 *
 * <h3>Sub-packages</h3>
 * <ul>
 * <li>{@code data/} — Data-driven skill definitions, registry, and loaders
 * (JSON)</li>
 * <li>{@code network/} — Skill use and hotbar packets</li>
 * <li>Per-job packages — Concrete skill effect implementations</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code system.stats} — Player stat values for skill calculations</li>
 * <li>{@code common.api} — {@code IPlayerSkills}, {@code SkillType},
 * {@code ISkillDefinition}</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.skills;
