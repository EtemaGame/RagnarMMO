package com.etema.ragnarmmo.system.bar;

import com.etema.ragnarmmo.system.mobstats.integration.MobInfoIntegration;

import net.minecraft.world.entity.LivingEntity;

public class MobStatsIntegration implements EntityStatResolver {

    @Override
    public String getDisplayName(LivingEntity e) {
        return e.getName().getString();
    }

    @Override
    public String getLevel(LivingEntity e) {
        var levelOpt = MobInfoIntegration.getMobLevel(e);
        if (levelOpt.isPresent()) {
            int lvl = levelOpt.getAsInt();
            if (lvl > 0) {
                return String.valueOf(lvl);
            }
        }
        return "";
    }

    @Override
    public String getRank(LivingEntity e) {
        return MobInfoIntegration.getMobTier(e)
                .map(tier -> tier.name())
                .orElse("");
    }

    @Override
    public String getClazz(LivingEntity e) {
        return "";
    }
}






