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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Signum Crucis — Active (Acolyte)
 * RO: Reduces DEF of Demon and Undead enemies in range.
 * MC: AoE 6 blocks — applies WEAKNESS II + GLOWING to all Undead/Nether mobs
 *     for (15 + level * 5) seconds. The "cursed" visual makes them targetable.
 */
public class SignumCrucisSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "signum_crucis");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        double radius = 6.0;
        int durationTicks = (15 + level * 5) * 20;

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive()
                        && (e.getMobType() == MobType.UNDEAD || e.getMobType() == MobType.ILLAGER));

        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8Signum Crucis: No hay objetivos no-muertos cercanos."));
            return;
        }

        for (LivingEntity e : targets) {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1));      // WEAKNESS II
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, durationTicks, 0));       // GLOWING
        }

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    5, 0.5, 0.5, 0.5, 0.0);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }

        player.sendSystemMessage(Component.literal(
                "§c✝ Signum Crucis §faplica Weakness a " + targets.size() + " enemigo(s)."));
    }
}
