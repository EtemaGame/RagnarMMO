package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Decrease AGI — Active (Acolyte)
 * RO: Reduces target's FLEE and ASPD by lowering their AGI stat.
 * MC: Applies SLOWNESS II + MINING FATIGUE I to the targeted enemy for
 *     (30 + level * 10) seconds (max 130s at lv10).
 */
public class DecreaseAgiSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "decrease_agi");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = getHostileTarget(player, 7.0);
        if (target == null) {
            player.sendSystemMessage(Component.literal("§cDecrease AGI: §fNo hay objetivo válido."));
            return;
        }

        int durationTicks = (30 + level * 10) * 20;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 1)); // SLOWNESS II
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 0));      // MINING FATIGUE I

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    10, 0.3, 0.5, 0.3, 0.05);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 0.8f);
        }

        player.sendSystemMessage(Component.literal("§9↓ Decrease AGI §faplica Slowness a "
                + target.getDisplayName().getString() + " por " + (30 + level * 10) + "s."));
    }

    private LivingEntity getHostileTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive() && e instanceof Mob);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            var hit = e.getBoundingBox().inflate(0.5).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(e.position());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best;
    }
}
