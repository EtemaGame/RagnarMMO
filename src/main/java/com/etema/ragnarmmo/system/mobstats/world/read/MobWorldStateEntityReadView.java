package com.etema.ragnarmmo.system.mobstats.world.read;

import com.etema.ragnarmmo.system.mobstats.world.BossSpawnSource;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal read-only world-state/history view for one living boss encounter instance.
 *
 * <p>This view is separate from semantic mob encounter reads. It does not redefine rank, does not
 * transport lifecycle through {@code MobRank}, and exists only for explicit world-state
 * inspection.</p>
 */
public record MobWorldStateEntityReadView(
        boolean activeRegistrationPresent,
        @Nullable String entityTypeId,
        @Nullable String encounterKey,
        @Nullable BossSpawnSource spawnSource,
        @Nullable Integer respawnDelayTicks,
        @Nullable Long lastSeenGameTime,
        boolean cooldownPresent,
        boolean cooldownReady,
        @Nullable Long nextAllowedGameTime,
        @Nullable Long lastDefeatedGameTime) {
}
