/**
 * <b>Mob Stats Module</b>
 * <p>
 * Contains the legacy capability-driven mob-stats pipeline plus compatibility paths that still
 * coexist with the newer mob architecture.
 *
 * <p>This package historically assigned mob levels, {@code MobTier}, classes, and stat
 * multipliers on spawn. That legacy behavior still exists for compatibility, but new semantic
 * migration work should prefer the newer read surfaces and runtime profile boundaries instead of
 * treating this package as the primary authority for migrated manual content.</p>
 *
 * <h3>Sub-packages</h3>
 * <ul>
 * <li>{@code core/} - capability and legacy data classes</li>
 * <li>{@code events/} - spawn handlers and other old-pipeline hooks</li>
 * <li>{@code network/} - legacy mob stats sync used by older client consumers</li>
 * <li>{@code config/} - legacy config paths such as {@code MobConfig} and {@code SpeciesConfig}</li>
 * </ul>
 *
 * <h3>Dependencies</h3>
 * <ul>
 * <li>{@code common.api.mobs} - legacy compatibility types including {@code MobTier}</li>
 * <li>{@code common.net} - network channel</li>
 * </ul>
 */
package com.etema.ragnarmmo.system.mobstats;
