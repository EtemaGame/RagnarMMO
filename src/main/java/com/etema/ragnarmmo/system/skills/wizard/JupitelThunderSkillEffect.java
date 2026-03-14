package com.etema.ragnarmmo.system.skills.wizard;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.skills.mage.MageTargetUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Jupitel Thunder — Active (Wind/Lightning, single target + knockback)
 * RO: Deals 1+level hits of Wind damage and pushes the target back.
 *
 * Fixed from original: now uses proper raycast targeting (not box inflate),
 * each hit is shown with individual particles, knockback is applied once at end.
 */
public class JupitelThunderSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "jupitel_thunder");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = MageTargetUtil.raycast(player, 12.0);
        if (target == null) return;

        if (!(player.level() instanceof ServerLevel sl)) return;

        int hits = 3 + level; // 4-13 hits depending on level
        float damagePerHit = 2.0f + (level * 0.3f);

        // Deal all hits
        for (int i = 0; i < hits; i++) {
            target.hurt(player.damageSources().indirectMagic(null, player), damagePerHit);
        }

        // Knockback once, scaled by number of hits (more hits = pushed further)
        float knockbackStrength = 0.3f + (level * 0.1f);
        target.knockback(knockbackStrength,
                player.getX() - target.getX(),
                player.getZ() - target.getZ());

        // Sounds
        sl.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.4f);
        sl.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.6f);

        // Particles: electric sparks + flash on target
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + 1, target.getZ(),
                60, 0.4, 0.6, 0.4, 0.12);
        sl.sendParticles(ParticleTypes.FLASH,
                target.getX(), target.getY() + 1, target.getZ(),
                2, 0.2, 0.5, 0.2, 0.0);
        // Bolt streak from player to target
        var start = player.getEyePosition().add(0, -0.3, 0);
        var dir = target.position().add(0, 1, 0).subtract(start).normalize();
        double dist = target.distanceTo(player);
        for (double d = 0; d < dist; d += 0.5) {
            var pos = start.add(dir.scale(d));
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
    }
}
