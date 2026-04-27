package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class StatusRecoverySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.parse("ragnarmmo:status_recovery");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Status Recovery: Removes negative status effects.
        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            return;

        target.removeAllEffects();
        // VFX and SFX
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
