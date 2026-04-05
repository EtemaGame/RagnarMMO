package com.etema.ragnarmmo.skill.job.archer;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arrow Shower — Active AoE
 * RO: 3x3 AoE damage.
 * 
 * Minecraft implementation:
 * - Circular AoE around the point the player is looking at.
 * - Damage: 75% + 5% * level ATK.
 * - Knockback: Pushes enemies away from the center of the impact.
 */
public class ArrowShowerSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "arrow_shower");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Find impact point (block or entity)
        HitResult hit = player.pick(12.0, 0.0f, false);
        Vec3 pos = hit.getLocation();
        
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        double radius = 3.0; // 3x3 RO area
        AABB area = new AABB(pos.x - radius, pos.y - 2, pos.z - radius,
                             pos.x + radius, pos.y + 2, pos.z + radius);
        
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        // RO: (75 + 5×level)% ATK
        float pct = 75f + (5f * level);
        float damage = Math.max(SkillDamageHelper.MIN_ATK,
                SkillDamageHelper.scaleByATK(player, pct));

        for (LivingEntity victim : targets) {
            // Damage
            SkillDamageHelper.dealSkillDamage(
                    victim, player.damageSources().playerAttack(player), damage);

            // RO-accurate knockback (away from center of shower)
            Vec3 knockDir = victim.position().subtract(pos).normalize();
            victim.knockback(0.8, -knockDir.x, -knockDir.z);

            // Particles at each hit
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    victim.getX(), victim.getY() + 1, victim.getZ(),
                    6, 0.3, 0.3, 0.3, 0.1);
        }

        // --- Visual & Audio Feedback ---
        serverLevel.playSound(null, pos.x, pos.y, pos.z,
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.6f);
        
        // Denser arrow rain effect at impact
        for (int i = 0; i < 20; i++) {
            double rx = pos.x + (player.getRandom().nextDouble() - 0.5) * radius * 1.8;
            double rz = pos.z + (player.getRandom().nextDouble() - 0.5) * radius * 1.8;
            serverLevel.sendParticles(ParticleTypes.CRIT, rx, pos.y + 4.0, rz, 1, 0, -1.0, 0, 0.6);
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, rx, pos.y + 0.5, rz, 2, 0.1, 0.1, 0.1, 0);
        }
    }
}
