/**
 * <b>Health Bar Module</b>
 * <p>
 * Client-side mob health bar overlay during coexistence.
 * Pure rendering module - no gameplay logic.
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code client} / coexistence reader - prefers the synced read-only new-source projection</li>
 * <li>{@code system.mobstats.integration} - provides legacy fallback reads when the new source is absent</li>
 * <li>{@code system.stats} - reads player data for integration</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.bar;
