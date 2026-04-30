package com.etema.ragnarmmo.skills.execution;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.player.stats.capability.PlayerStats;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Read-only RO stat helpers for non-damage skill effects.
 */
public final class RoSkillStatHelper {
    private RoSkillStatHelper() {
    }

    public static int dex(Player player) {
        return RagnarCoreAPI.get(player)
                .map(stats -> stats instanceof PlayerStats playerStats ? playerStats.getDEX() : 1)
                .orElse(1);
    }

    public static int intel(Player player) {
        return RagnarCoreAPI.get(player)
                .map(stats -> stats instanceof PlayerStats playerStats ? playerStats.getINT() : 1)
                .orElse(1);
    }

    public static int baseLevel(Player player) {
        return RagnarCoreAPI.get(player)
                .map(stats -> stats instanceof PlayerStats playerStats ? playerStats.getLevel() : 1)
                .orElse(1);
    }

    public static int baseLevel(LivingEntity entity) {
        if (entity instanceof Player player) {
            return baseLevel(player);
        }
        if (entity instanceof Mob mob) {
            return MobProfileProvider.get(mob).resolve()
                    .filter(MobProfileState::isInitialized)
                    .map(state -> Math.max(1, state.profile().level()))
                    .orElse(1);
        }
        return 1;
    }

    public static int agi(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return RagnarCoreAPI.get(player)
                    .map(stats -> stats instanceof PlayerStats playerStats ? playerStats.getAGI() : 1)
                    .orElse(1);
        }
        if (entity instanceof Mob mob) {
            return MobProfileProvider.get(mob).resolve()
                    .filter(MobProfileState::isInitialized)
                    .map(state -> Math.max(1, state.profile().baseStats().agi()))
                    .orElse(1);
        }
        return 1;
    }

    public static float healAmount(LivingEntity caster, int skillLevel) {
        if (caster instanceof Player player) {
            double multiplier = Math.max(1.0D, Math.floor((baseLevel(player) + intel(player)) / 8.0D));
            return (float) (multiplier * (4 + (8 * skillLevel)));
        }
        return 4.0F + (skillLevel * 4.0F);
    }
}
