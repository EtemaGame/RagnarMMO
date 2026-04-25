package com.etema.ragnarmmo.common.api.player;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.skills.api.IPlayerSkills;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public final class RoPlayerDataAccess {

    private RoPlayerDataAccess() {
    }

    public static Optional<IRoPlayerData> get(Player player) {
        if (player == null) {
            return Optional.empty();
        }

        Optional<IPlayerStats> stats = RagnarCoreAPI.get(player);
        Optional<IPlayerSkills> skills = PlayerSkillsProvider.get(player).resolve().map(value -> (IPlayerSkills) value);
        if (stats.isEmpty() || skills.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedRoPlayerData(player, stats.get(), skills.get()));
    }

    public static boolean with(Player player, Consumer<IRoPlayerData> action) {
        return get(player).map(data -> {
            action.accept(data);
            return true;
        }).orElse(false);
    }

    public static int with(Player player, ToIntFunction<IRoPlayerData> action) {
        return get(player).map(action::applyAsInt).orElse(0);
    }

    private record ResolvedRoPlayerData(Player player, IPlayerStats stats,
                                        IPlayerSkills skills) implements IRoPlayerData {
        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public IPlayerStats getStats() {
            return stats;
        }

        @Override
        public IPlayerSkills getSkills() {
            return skills;
        }
    }
}
