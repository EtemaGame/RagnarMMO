package com.etema.ragnarmmo.client.hud;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class RagnarStatsIntegration implements EntityStatResolver {

    @Override
    public String getDisplayName(LivingEntity e) {
        return e.getName().getString();
    }

    @Override
    public String getLevel(LivingEntity e) {
        if (!(e instanceof Player p)) return "";
        return p.getCapability(PlayerStatsProvider.CAP)
                .map(IPlayerStats::getLevel)
                .map(String::valueOf)
                .orElse("");
    }

    @Override
    public String getRank(LivingEntity e) {
        return "";
    }

    @Override
    public String getClazz(LivingEntity e) {
        if (!(e instanceof Player p)) return "";
        return p.getCapability(PlayerStatsProvider.CAP)
                .map(IPlayerStats::getJobId)
                .map(jobId -> {
                    if (jobId == null || jobId.isBlank()) {
                        return "";
                    }
                    int namespaceSeparator = jobId.indexOf(':');
                    if (namespaceSeparator >= 0 && namespaceSeparator < jobId.length() - 1) {
                        jobId = jobId.substring(namespaceSeparator + 1);
                    }
                    if (jobId.isBlank()) {
                        return "";
                    }
                    return jobId.substring(0, 1).toUpperCase() + jobId.substring(1).toLowerCase();
                })
                .orElse("");
    }
}






