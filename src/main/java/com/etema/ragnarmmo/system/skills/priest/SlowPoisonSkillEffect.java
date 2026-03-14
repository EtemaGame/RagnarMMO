package com.etema.ragnarmmo.system.skills.priest;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Slow Poison — Active (Priest)
 * RO: Slows the progression of poison, preventing the target from dying.
 * MC: On the targeted entity (or self): replaces active Poison with a
 *     very weak POISON 0 of extended duration that won't kill (via NBT tag
 *     "slow_poison=true"), effectively freezing the damage. The kill-block
 *     mechanic is handled in SkillEvents where we prevent Poison from dealing
 *     the last heart of damage if slow_poison tag is set.
 */
public class SlowPoisonSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "slow_poison");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = getTarget(player, 6.0);

        // Check if target is poisoned
        if (!target.hasEffect(MobEffects.POISON)) {
            player.sendSystemMessage(Component.literal("§cSlow Poison: §fEl objetivo no está envenenado."));
            return;
        }

        int durationTicks = (10 + level * 10) * 20;

        // Replace poison with weakened, slow version
        target.removeEffect(MobEffects.POISON);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, durationTicks, 0, false, true));
        // Mark as slow-poisoned (prevents kill in SkillEvents hook)
        target.getPersistentData().putBoolean("slow_poison", true);
        target.getPersistentData().putLong("slow_poison_expiry",
                player.level().getGameTime() + durationTicks);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EFFECT,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 0.7f);
        }

        String name = target == player ? "ti mismo" : target.getDisplayName().getString();
        player.sendSystemMessage(Component.literal("§2✦ Slow Poison §faplicado a " + name
                + " — §7(" + (10 + level * 10) + "s)"));
    }

    private LivingEntity getTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> e.isAlive());
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            if (e == player) continue;
            var hit = e.getBoundingBox().inflate(0.5).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(e.position());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best != null ? best : player;
    }
}
