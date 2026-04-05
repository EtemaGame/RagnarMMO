package com.etema.ragnarmmo.skill.job.knight;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Spear Boomerang — Active (Ranged)
 * RO: Throws the spear at the target and it returns. Long range.
 *     Damage = (Spear ATK) × (150 + 50×level)%
 *
 * Minecraft:
 *  - Raycasts forward up to 9 blocks to find a target.
 *  - Deals ranged physical damage.
 *  - Visual: particle trail simulating the thrown spear going and returning.
 *  - Does NOT actually spawn a trident entity — a real ThrownTrident
 *    would remove the item; instead we simulate it as a hitscan with effects.
 */
public class SpearBoomerangSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "spear_boomerang");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double range = 3.0 + (level * 0.6); // Level 1 = 3.6, Level 10 = 9.0 blocks
        LivingEntity target = raycastTarget(player, range);
        if (target == null) return;

        // RO: (150 + 50×level)% ATK — ranged spear throw
        float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, 150f + 50f * level));
        com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(
                target, player.damageSources().playerAttack(player), damage);

        // --- Particle trail: from player to target and back ---
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 playerPos = player.position().add(0, 1.2, 0);
            Vec3 targetPos = target.position().add(0, 1.0, 0);
            Vec3 dir = targetPos.subtract(playerPos).normalize();
            double dist = playerPos.distanceTo(targetPos);

            // Forward trail
            for (double d = 0; d < dist; d += 0.4) {
                Vec3 pos = playerPos.add(dir.scale(d));
                serverLevel.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
            // Return trail (reversed, slightly offset)
            for (double d = dist; d > 0; d -= 0.5) {
                Vec3 pos = playerPos.add(dir.scale(d)).add(0.1, 0, 0.1);
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
            // Impact burst
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    12, 0.3, 0.3, 0.3, 0.1);
        }

        // Sounds: throw + impact + return swoosh
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.1f);
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private LivingEntity raycastTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = new AABB(start, end).inflate(0.5);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(0.3).clip(start, end).isPresent())
                .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}
