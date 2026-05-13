package com.etema.ragnarmmo.combat.resolver;

import com.etema.ragnarmmo.common.api.mobs.query.MobConsumerReadViewResolver;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import com.etema.ragnarmmo.player.stats.compute.EquipmentStatSnapshot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.OptionalInt;

public final class TargetCombatProfileResolver {
    private TargetCombatProfileResolver() {
    }

    public static OptionalInt tryGetTargetLevel(LivingEntity entity) {
        if (entity instanceof Player player) {
            var stats = player.getCapability(PlayerStatsProvider.CAP).resolve();
            if (stats.isPresent() && stats.get().getLevel() > 0) {
                return OptionalInt.of(stats.get().getLevel());
            }
            return OptionalInt.empty();
        }

        var readView = MobConsumerReadViewResolver.resolve(entity).orElse(null);
        if (readView != null && readView.level() > 0) {
            return OptionalInt.of(readView.level());
        }
        return OptionalInt.empty();
    }

    public static TargetCombatStats getTargetStats(LivingEntity entity) {
        if (entity instanceof Player player) {
            var stats = player.getCapability(PlayerStatsProvider.CAP).resolve();
            if (stats.isPresent()) {
                var resolved = stats.get();
                return new TargetCombatStats(
                        resolved.get(StatKeys.STR),
                        resolved.get(StatKeys.DEX),
                        resolved.get(StatKeys.VIT),
                        resolved.get(StatKeys.INT),
                        resolved.get(StatKeys.LUK),
                        resolved.get(StatKeys.AGI),
                        (int) Math.round(EquipmentStatSnapshot.computeArmorHardMdef(player))
                );
            }
        } else {
            var profile = MobProfileProvider.get(entity).resolve()
                    .filter(MobProfileState::isInitialized)
                    .map(MobProfileState::profile)
                    .orElse(null);
            var readView = MobConsumerReadViewResolver.resolve(entity).orElse(null);
            var inspectionStats = readView != null ? readView.inspectionStats() : null;
            if (profile != null) {
                return TargetCombatStats.neutralWithMdef(profile.mdef());
            }
            if (inspectionStats != null) {
                return TargetCombatStats.neutralWithMdef(inspectionStats.mdef());
            }
        }
        return TargetCombatStats.neutralWithMdef(0);
    }

    public record TargetCombatStats(int str, int dex, int vit, int intel, int luk, int agi, int mdef) {
        private static TargetCombatStats neutralWithMdef(int mdef) {
            return new TargetCombatStats(1, 1, 1, 1, 1, 1, mdef);
        }
    }
}
