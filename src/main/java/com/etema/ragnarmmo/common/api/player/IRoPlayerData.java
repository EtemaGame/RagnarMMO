package com.etema.ragnarmmo.common.api.player;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.skills.api.IPlayerSkills;

import net.minecraft.world.entity.player.Player;

/**
 * Unified view over the player's RO-facing state.
 *
 * <p>This does not replace existing capabilities. It gives the rest of the
 * codebase one stable facade for the canonical RO domains that already exist:
 * stats/progression/resources and skill state.</p>
 */
public interface IRoPlayerData {

    Player getPlayer();

    IPlayerStats getStats();

    IPlayerSkills getSkills();
}
