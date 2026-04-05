package com.etema.ragnarmmo.skill.job.novice;

import com.etema.ragnarmmo.combat.aggro.AggroManager;
import com.etema.ragnarmmo.system.mobstats.util.MobUtils;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Play Dead - Active (Novice-only unlock through Basic Skill Lv.7)
 * Minecraft adaptation:
 * - Applies the same invisibility/slowness marker pattern used by Hiding.
 * - Nearby non-boss mobs immediately drop aggro and target.
 */
public class PlayDeadSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "play_dead");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(LivingEntity entity, int level) {
        if (!(entity instanceof Player player) || level <= 0) return;

        int durationTicks = 100;
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, durationTicks, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 6, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1, false, false, true));

        AABB area = player.getBoundingBox().inflate(12.0);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area, mob -> mob.isAlive() && !MobUtils.isMVPBoss(mob))) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            mob.setLastHurtByMob(null);
            mob.setLastHurtMob(null);
            AggroManager.clearAggro(mob);
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 0.8f, 0.7f);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    player.getX(), player.getY() + 0.2, player.getZ(),
                    16, 0.4, 0.15, 0.4, 0.01);
        }
    }
}
