package com.etema.ragnarmmo.skills.job.thief;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ThrowStoneSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "throw_stone");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Throw Stone: Consumes a stone to deal 50 damage (flat 3.0f in MC roughly).
        // 5% chance to Stun/Blind.

        int stoneSlot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.COBBLESTONE));
        if (stoneSlot == -1 && !player.getInventory().contains(new ItemStack(Items.COBBLESTONE))) {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Requires a stone (Cobblestone) to throw."));
            return;
        }

        LivingEntity target = getRangedTarget(player);
        // Allow throwing even if target is null

        // RO: 50 fixed damage. MC: 5.0 (2.5 hearts) is a solid early poke.
        final float baseDamage = 5.0f; 

        // Preparation delay (5 ticks)
        com.etema.ragnarmmo.skills.runtime.SkillSequencer.schedule(5, () -> {
            // Consume 1 cobblestone
            player.getInventory().clearOrCountMatchingItems(p -> p.getItem() == Items.COBBLESTONE, 1,
                    player.inventoryMenu.getCraftSlots());

            Vec3 startPos = player.getEyePosition().subtract(0, 0.2, 0);
            Vec3 shootDir;
            
            if (target != null && target.isAlive()) {
                Vec3 targetVec = target.position().add(0, target.getBbHeight() / 0.8, 0);
                shootDir = targetVec.subtract(startPos).normalize();
            } else {
                shootDir = player.getLookAngle();
            }

            com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile projectile = 
                new com.etema.ragnarmmo.entity.projectile.AbstractMagicProjectile(player.level(), player, baseDamage, 
                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBBLESTONE)));
            
            projectile.setSecondaryParticle(ParticleTypes.CRIT);
            projectile.setProjectileType("default"); // Use particles only
            projectile.setHoming(false);
            projectile.setGravity(0.04f); // Heavier than magic
            
            projectile.setPos(startPos.x, startPos.y, startPos.z);
            projectile.shoot(shootDir.x, shootDir.y, shootDir.z, 1.2f, 1.0f); // Fast but affected by gravity
            
            projectile.setOnHitEffect(result -> {
                if (player.getRandom().nextFloat() <= 0.05f) {
                    if (result instanceof net.minecraft.world.phys.EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity victim) {
                        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, true, true));
                        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, false, true, false));
                    }
                }

                player.level().playSound(null, result.getLocation().x, result.getLocation().y, result.getLocation().z,
                        SoundEvents.STONE_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.2f);

                if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                     sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBBLESTONE)),
                            result.getLocation().x, result.getLocation().y, result.getLocation().z, 15, 0.2, 0.2, 0.2, 0.1);
                     sl.sendParticles(ParticleTypes.CRIT, result.getLocation().x, result.getLocation().y, result.getLocation().z, 5, 0.2, 0.2, 0.2, 0.1);
                }
            });

            player.level().addFreshEntity(projectile);
        });
    }

    private LivingEntity getRangedTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(15.0)); // Range 15

        AABB searchBox = player.getBoundingBox().inflate(15.0);
        List<LivingEntity> possibleTargets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        LivingEntity closestTarget = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity e : possibleTargets) {
            AABB targetBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
            var hitOpt = targetBox.clip(start, end);
            if (hitOpt.isPresent() || targetBox.contains(start)) {
                double dist = start.distanceToSqr(e.position());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestTarget = e;
                }
            }
        }
        return closestTarget;
    }
}
