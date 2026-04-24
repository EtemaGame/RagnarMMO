package com.etema.ragnarmmo.player.stats.service;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.command.CommandUtil;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService;
import com.etema.ragnarmmo.player.stats.progression.StatCost;
import com.etema.ragnarmmo.skill.api.ISkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public final class CharacterResetService {

    private CharacterResetService() {
    }

    public static void resetAllocatedStats(ServerPlayer player) {
        RagnarCoreAPI.get(player).ifPresent(stats -> {
            int totalRefunded = 0;
            for (StatKeys key : StatKeys.values()) {
                if (key == StatKeys.LEVEL) {
                    continue;
                }

                int currentVal = stats.get(key);
                for (int v = 1; v < currentVal; v++) {
                    totalRefunded += StatCost.costToIncrease(v);
                }
                stats.set(key, 1);
            }

            stats.setStatPoints(stats.getStatPoints() + totalRefunded);
            PlayerStatsSyncService.sync(player, stats, RoPlayerSyncDomain.STATS.bit() | RoPlayerSyncDomain.PROGRESSION.bit());
        });
    }

    public static void resetLearnedSkills(ServerPlayer player) {
        CommandUtil.getSkills(player).ifPresent(skills -> {
            RagnarCoreAPI.get(player).ifPresent(stats -> {
                int totalRefunded = 0;

                for (ResourceLocation skillId : SkillRegistry.getAllIds()) {
                    int level = skills.getSkillLevel(skillId);
                    if (level > 0) {
                        Optional<ISkillDefinition> defOpt = SkillRegistry.get(skillId).map(d -> (ISkillDefinition) d);
                        if (defOpt.isPresent() && defOpt.get().canUpgradeWithPoints()) {
                            totalRefunded += level * defOpt.get().getUpgradeCost();
                        }
                    }
                }

                skills.resetAll(ChangeReason.ADMIN_COMMAND);
                stats.setSkillPoints(stats.getSkillPoints() + totalRefunded);

                PlayerStatsSyncService.sync(player, stats, RoPlayerSyncDomain.PROGRESSION.bit());
            });
        });
    }

    public static void wipeCharacter(ServerPlayer player) {
        RagnarCoreAPI.get(player).ifPresent(stats -> {
            stats.resetAll(ChangeReason.ADMIN_COMMAND);
            PlayerStatsSyncService.sync(player, stats, RoPlayerSyncDomain.allMask());
        });

        CommandUtil.getSkills(player).ifPresent(skills -> skills.resetAll(ChangeReason.ADMIN_COMMAND));
    }
}
