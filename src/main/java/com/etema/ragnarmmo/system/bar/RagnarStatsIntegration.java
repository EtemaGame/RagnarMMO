package com.etema.ragnarmmo.system.bar;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
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
                    // limpia namespace tipo "ragnarmmo:novice"
                    if (jobId.contains(":")) jobId = jobId.split(":")[1];
                    // capitaliza
                    return jobId.substring(0, 1).toUpperCase() + jobId.substring(1).toLowerCase();
                })
                .orElse("");
    }
}






