package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.runtime.SkillSequencer;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Heaven's Drive — Active (Earth AoE)
 * RO: Launches multiple rocks from ground in a 5x5 area.
 *     Deals Earth property MATK damage. Does not work on Undead or Boss monsters.
 *     Damage per hit = MATK × (100 + 40 × level)%.
 *     Hits = 5.
 *
 * Minecraft:
 *  - Erupts stone particles in a ring around the target area.
 *  - Deals Earth-property magic damage to all entities in a 4-block radius.
 *  - Knockback effect pushing entities upward (simulating rocks).
 */
public class HeavensDriveSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "heavens_drive");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // RO: MATK × (100 + 40 × level)%
        float damage = SkillDamageHelper.scaleByMATK(player, 100f + 40f * level);
        double radius = 4.0; // RO 5×5 AoE

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        // 5 hits × damage (RO: 5 separate rock impacts)
        int hitCount = 5;
        for (int hit = 0; hit < hitCount; hit++) {
            int delay = hit * 3;
            SkillSequencer.schedule(delay, () -> {
                if (!(player.level() instanceof ServerLevel sl)) return;
                for (LivingEntity target : targets) {
                    if (!target.isAlive()) continue;
                    SkillDamageHelper.dealSkillDamage(target, player.damageSources().magic(), damage / hitCount);
                    // Simulate rock impact — push upward slightly
                    Vec3 vel = target.getDeltaMovement();
                    target.setDeltaMovement(vel.add(
                            (RANDOM.nextDouble() - 0.5) * 0.3,
                            0.25 + RANDOM.nextDouble() * 0.15,
                            (RANDOM.nextDouble() - 0.5) * 0.3));
                }
                // Rock/earth particle burst
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
                    double rad = radius * (0.5 + RANDOM.nextDouble() * 0.5);
                    double px = player.getX() + Math.cos(angle) * rad;
                    double pz = player.getZ() + Math.sin(angle) * rad;
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                            px, player.getY(), pz, 5, 0.2, 0.3, 0.2, 0.1);
                }
            });
        }

        // Impact sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.2f, 0.6f);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}
