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

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "mammonite");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        SkillDefinition definition = SkillRegistry.get(ID).orElse(null);
        int cost = EconomicSkillHelper.zenyCost(definition, level, level * ZenyWalletHelper.GOLD_VALUE);
        int found = ZenyWalletHelper.getTotalZeny(player);
        if (found < cost) {
            player.sendSystemMessage(Component.literal(
                    "Mammonite needs " + ZenyWalletHelper.formatZeny(cost) + " (you have "
                            + ZenyWalletHelper.formatZeny(found) + ")"));
            return;
        }

        double range = definition != null ? definition.getLevelDouble("range", level, 3.5D) : 3.5D;
        LivingEntity target = getMeleeTarget(player, range);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No target in range."));
            return;
        }

        if (!ZenyWalletHelper.tryConsume(player, cost)) {
            player.sendSystemMessage(Component.literal("Mammonite could not consume the required Zeny."));
            return;
        }

        float damagePercent = definition != null
                ? (float) definition.getLevelDouble("damage_percent", level, 100.0D + 50.0D * level)
                : 100.0f + 50.0f * level;
        float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, damagePercent));

        com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(3, () -> {
            if (!target.isAlive()) {
                return;
            }

            target.hurt(player.damageSources().mobAttack(player), damage);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0f, 1.4f);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 0.6f);

            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        new net.minecraft.core.particles.ItemParticleOption(
                                net.minecraft.core.particles.ParticleTypes.ITEM,
                                new ItemStack(ZenyItems.GOLD_ZENY.get())),
                        target.getX(), target.getY() + 1.2, target.getZ(),
                        30, 0.3, 0.3, 0.3, 0.15);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        10, 0.2, 0.4, 0.2, 0.1);
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_OFF,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        10, 0.2, 0.4, 0.2, 0.05);
            }

            player.displayClientMessage(Component.literal("-" + ZenyWalletHelper.formatZeny(cost) + " Mammonite!"),
                    true);
        });
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
