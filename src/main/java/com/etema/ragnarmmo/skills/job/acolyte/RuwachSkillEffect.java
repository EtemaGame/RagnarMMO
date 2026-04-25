package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.job.mage.SightMobEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class RuwachSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "ruwach");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        var definition = SkillRegistry.require(ID);
        double radius = definition.getLevelDouble("reveal_radius", level, 5.0D);
        AABB area = player.getBoundingBox().inflate(radius);

        List<Entity> nearby = player.level().getEntities(player, area,
                entity -> entity instanceof LivingEntity living
                        && living != player
                        && AcolyteTargetingHelper.isHostileTarget(player, living));

        float damagePercent = (float) definition.getLevelDouble("damage_percent", level, 145.0D);
        int glowDurationTicks = definition.getLevelInt("glow_duration_ticks", level, 200);
        float damage = Math.max(SkillDamageHelper.MIN_ATK, SkillDamageHelper.scaleByATK(player, damagePercent));
        int revealed = 0;

        for (Entity e : nearby) {
            LivingEntity target = (LivingEntity) e;
            boolean hidden = target.isInvisible() || target.hasEffect(MobEffects.INVISIBILITY)
                    || target.getPersistentData().contains(SightMobEffect.CLOAKED_TAG);
            if (!hidden) {
                continue;
            }

            target.getPersistentData().remove(SightMobEffect.CLOAKED_TAG);
            target.setInvisible(false);
            target.removeEffect(MobEffects.INVISIBILITY);
            SkillDamageHelper.dealSkillDamage(target, player.damageSources().magic(), damage);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, glowDurationTicks, 0, false, false, true));
            revealed++;
        }

        // VFX and SFX
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f, 1.0f);

        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // Circle of light particles
            for (int i = 0; i < 360; i += 20) {
                double x = player.getX() + Math.cos(Math.toRadians(i)) * radius;
                double z = player.getZ() + Math.sin(Math.toRadians(i)) * radius;
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, player.getY() + 0.5, z, 1, 0, 0, 0, 0);
            }
        }

        if (revealed == 0) {
            player.sendSystemMessage(Component.literal("Ruwach no detectó enemigos ocultos.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
