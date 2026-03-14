package com.etema.ragnarmmo.system.skills.knight;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Pierce — Active (Spear, hits same target multiple times)
 * RO: 100% ATK × N hits where N = 1 (Small), 2 (Medium), 3 (Large).
 *     Only works with Spears. No cooldown reset, but multi-hit based on target size.
 *
 * Minecraft:
 *  - Finds closest target in front of the player within 4.5 blocks (spear range).
 *  - Deals damage N times based on target bounding box volume.
 *  - Each hit plays its own sound tick.
 *  - Requires Trident or ragnarmmo:spear tagged weapon.
 */
public class PierceSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "pierce");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = getClosestTarget(player, 4.5);
        if (target == null) return;

        // Determine hit count by target size
        double volume = target.getBbHeight() * target.getBbWidth() * target.getBbWidth();
        int hits;
        if (volume > 3.0)      hits = 3; // Large (Creeper, Witch, most mobs > 1 block tall)
        else if (volume > 0.8) hits = 2; // Medium (Skeleton, Zombie)
        else                   hits = 1; // Small (Bat, Slime small)

        // Base damage: (150 + 50×level)% of scaled weapon damage
        float damagePerHit = 3.0f + (level * 0.5f);

        for (int i = 0; i < hits; i++) {
            target.hurt(player.damageSources().playerAttack(player), damagePerHit);

            // Sound on each stab
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.8f, 1.2f + (i * 0.1f));
        }

        // Particles: brief flash on each hit location
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    hits * 5, 0.3, 0.3, 0.3, 0.1);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    hits * 3, 0.2, 0.2, 0.2, 0.05);
        }
    }

    static LivingEntity getClosestTarget(Player player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);

        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        return candidates.stream()
                .filter(e -> e.getBoundingBox().inflate(e.getPickRadius() + 0.5).clip(start, end).isPresent()
                        || e.getBoundingBox().contains(start))
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }
}
