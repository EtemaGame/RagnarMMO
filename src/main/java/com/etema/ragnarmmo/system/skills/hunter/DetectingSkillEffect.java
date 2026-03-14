package com.etema.ragnarmmo.system.skills.hunter;

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

import java.util.List;

/**
 * Detecting — Active
 * RO: Reveals all hidden enemies in the area.
 * Range scales with level.
 */
public class DetectingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "detecting");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double radius = 4.0 + level * 0.8;
        int glowDuration = (5 + level * 2) * 20;

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive());

        int revealed = 0;
        for (LivingEntity e : targets) {
            if (e.getPersistentData().contains("ragnar_cloaked_until")) {
                e.getPersistentData().remove("ragnar_cloaked_until");
                e.setInvisible(false);
                revealed++;
            }
            if (e.isInvisible()) {
                e.setInvisible(false);
                revealed++;
            }
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowDuration, 0, false, false, false));
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Scanning ring particles (uses ServerLevel.sendParticles)
        if (player.level() instanceof ServerLevel sl) {
            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                double px = player.getX() + Math.cos(angle) * radius;
                double pz = player.getZ() + Math.sin(angle) * radius;
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, px, player.getY() + 1, pz, 2, 0.1, 0.1, 0.1, 0.02);
            }
        }

        if (revealed > 0) {
            player.sendSystemMessage(Component.literal("§aDetecting: §f" + revealed + " hidden entit" + (revealed == 1 ? "y" : "ies") + " revealed."));
        }
    }
}
