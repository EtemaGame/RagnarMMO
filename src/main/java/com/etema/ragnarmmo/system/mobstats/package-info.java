/**
 * <b>Mob Stats Module</b>
 * <p>
 * Assigns levels, tiers, classes, and stat multipliers to mobs on spawn.
 * Semi-independent — only depends on {@code common.api.mobs} enums.
 *
 * <h3>Sub-packages</h3>
 * <ul>
 * <li>{@code core/} — Capability and data classes</li>
 * <li>{@code events/} — Spawn handlers and combat modifiers</li>
 * <li>{@code network/} — Client sync for health bars</li>
 * <li>{@code config/} — {@code MobConfig} (ragnarmmo-mobstats.toml)</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code common.api.mobs} — {@code MobTier} enum</li>
 * <li>{@code common.net} — Network channel</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.mobstats;
