/**
 * <b>Loot Module</b>
 * <p>
 * Handles item generation from mob kills and block breaks. Includes:
 * <ul>
 * <li>{@code affix/} — Prefix/suffix system for equipment (data-driven
 * JSON)</li>
 * <li>{@code cards/} — Ragnarok Online-style card drops (data-driven JSON)</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code player.stats} — Player LUK for drop rate scaling</li>
 * <li>{@code roitems} — {@code RoItemNbtHelper} for rarity application</li>
 * <li>{@code common.api} — Stat access via {@code RagnarCoreAPI}</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.loot;
