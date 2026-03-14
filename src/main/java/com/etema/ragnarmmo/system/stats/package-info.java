/**
 * <b>Player Stats &amp; Progression Module</b>
 * <p>
 * Core stat system: STR, AGI, VIT, INT, DEX, LUK.
 * Includes capability storage, stat computation, party system, and level
 * progression.
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code common.api} — {@code IPlayerStats}, {@code StatAttributes},
 * {@code RagnarCoreAPI}</li>
 * <li>{@code common.net} — Network channel for stat sync packets</li>
 * </ul>
 *
 * <h3>Depended by</h3>
 * <ul>
 * <li>{@code system.skills} — stat requirements for skill effects</li>
 * <li>{@code system.loot} — LUK for drop calculations</li>
 * <li>{@code roitems} — stat-based equip restrictions</li>
 * <li>{@code client} — HUD overlays</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.stats;
