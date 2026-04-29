package com.etema.ragnarmmo.skills.job.merchant;

import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.items.runtime.ZenyWalletHelper;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.execution.EconomicSkillHelper;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mammonite - Active (Merchant).
 * RO adaptation: spends Gold Zeny worth of funds to deliver a stronger melee hit.
 */
public class MammoniteSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "mammonite");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }

    private LivingEntity getMeleeTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);
        LivingEntity closest = null;
        double dist = Double.MAX_VALUE;
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive())) {
            var hit = entity.getBoundingBox().inflate(0.5).clip(start, end);
            if (hit.isPresent()) {
                double currentDistance = start.distanceToSqr(entity.position());
                if (currentDistance < dist) {
                    dist = currentDistance;
                    closest = entity;
                }
            }
        }
        return closest;
    }
}
