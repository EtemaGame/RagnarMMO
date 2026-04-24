package com.etema.ragnarmmo.skill.job.acolyte;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.skill.runtime.SkillVisualFx;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;
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
 * Signum Crucis — Active (Acolyte)
 * RO-inspired wide-area DEF debuff for Undead / Shadow analog targets.
 * The debuff uses a near-permanent MobEffect and naturally ends on death.
 */
public class SignumCrucisSkillEffect implements ISkillEffect {
    private static final int PERSISTENT_DURATION = Integer.MAX_VALUE / 4;

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "signum_crucis");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        var definition = SkillRegistry.require(ID);
        double radius = definition.getLevelDouble("aoe_radius", level, 6.0D);

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class, box,
                e -> e != player && e.isAlive()
                        && AcolyteTargetingHelper.isSignumTarget(e));

        if (targets.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8Signum Crucis: No hay objetivos no-muertos cercanos."));
            return;
        }

        float baseChance = (float) definition.getLevelDouble("success_rate", level, 23.0D + (level * 4.0D));
        int affected = 0;
        for (LivingEntity e : targets) {
            int targetLevel = CombatMath.tryGetTargetLevel(e).orElse(1);
            float levelBonus = Math.max(-15.0f,
                    Math.min(20.0f, (SkillDamageHelper.getBaseLevel(player) - targetLevel) * 2.0f));
            float chance = Math.max(5.0f, Math.min(95.0f, baseChance + levelBonus));

            if ((player.getRandom().nextFloat() * 100.0f) > chance) {
                continue;
            }

            e.addEffect(new MobEffectInstance(RagnarMobEffects.SIGNUM_CRUCIS.get(), PERSISTENT_DURATION, level - 1,
                    false, true, true));
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
            affected++;
        }

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    5, 0.5, 0.5, 0.5, 0.0);
            SkillVisualFx.spawnVerticalCross(sl, player.position(), 0.15, 2.0, 0.45, ParticleTypes.END_ROD, ParticleTypes.FLASH);
            SkillVisualFx.spawnRing(sl, player.position(), radius, 0.2, ParticleTypes.GLOW, 20);
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }

        if (affected > 0) {
            player.sendSystemMessage(Component.literal(
                    "§c✝ Signum Crucis §fdebilita a " + affected + " enemigo(s)."));
        } else {
            player.sendSystemMessage(Component.literal("§7Signum Crucis no logró afectar a ningún objetivo."));
        }
    }
}
