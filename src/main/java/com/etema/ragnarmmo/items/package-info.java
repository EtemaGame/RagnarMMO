/**
 * <b>RO Items Module</b>
 * <p>
 * Ragnarok Online-style item rule system. Applies stat-based equip
 * restrictions,
 * combat restrictions, rarity bonuses, and custom tooltips to equipment.
 * Data-driven via JSON rules in {@code data/ragnarmmo/ro_item_rules/}.
 *
 * <h3>Sub-packages</h3>
 * <ul>
 * <li>{@code data/} — Rule definitions and JSON loader</li>
 * <li>{@code hooks/} — Event hooks (equip, combat, tooltip)</li>
 * <li>{@code runtime/} — NBT helpers and attribute application</li>
 * <li>{@code network/} — Client rule sync</li>
 * <li>{@code config/} — {@code RoItemsConfig} (ragnarmmo-roitems.toml)</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code common.api.stats} — {@code IPlayerStats} for equip checks</li>
 * <li>{@code common.net} — Network channel</li>
 * </ul>
 */
package com.etema.ragnarmmo.items;
