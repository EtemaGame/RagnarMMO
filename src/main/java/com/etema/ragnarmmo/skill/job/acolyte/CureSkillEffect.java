package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Cure — Active (Acolyte)
 * RO: Removes stone/frozen/sleep/chaos/blind status from a target.
 * MC: Removes Slowness, Poison, Weakness, Blindness, Nausea, Wither and Levitation
 *     from the targeted entity (5-block ray) or self if no target.
 */
public class CureSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "cure");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = getTarget(player, 5.0);

        // Remove negative status effects
        target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WEAKNESS);
        target.removeEffect(MobEffects.BLINDNESS);
        target.removeEffect(MobEffects.CONFUSION);
        target.removeEffect(MobEffects.WITHER);
        target.removeEffect(MobEffects.LEVITATION);
        target.removeEffect(MobEffects.DIG_SLOWDOWN);

        // VFX
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.4, 0.5, 0.4, 0.1);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.5f);
        }

        String name = target == player ? "ti mismo" : target.getDisplayName().getString();
        player.sendSystemMessage(Component.literal("§b✦ Cure §faplicado a " + name + "."));
    }

    private LivingEntity getTarget(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity e : candidates) {
            var hit = e.getBoundingBox().inflate(0.5).clip(start, end);
            if (hit.isPresent()) {
                double d = start.distanceToSqr(e.position());
                if (d < bestDist) { bestDist = d; best = e; }
            }
        }
        return best != null ? best : player;
    }
}
