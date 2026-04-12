package com.etema.ragnarmmo.skill.job.mage;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.combat.damage.SkillDamageHelper;
import com.etema.ragnarmmo.skill.job.mage.MageTargetUtil;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Napalm Beat — Ghost property splash attack.
 * Causes damage to enemies within a 9 cell area (3x3 / 1.5 radius) around the target.
 * Non-linear SP cost and Cast Delay scaling.
 */
public class NapalmBeatSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "napalm_beat");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getResourceCost(int level, int defaultCost) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("sp_cost", level, defaultCost))
                .orElse(defaultCost);
    }

    @Override
    public int getCastDelay(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_delay_ticks", level, 20))
                .orElse(20);
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        var definition = SkillRegistry.require(ID);
        double range = definition.getLevelDouble("range", level, 10.0D);
        LivingEntity mainTarget = MageTargetUtil.raycast(player, range);
        if (mainTarget == null) return;

        float matkPercent = (float) definition.getLevelDouble("damage_percent", level, 80.0D + (level - 1) * 10.0D);
        float damage = SkillDamageHelper.scaleByMATK(player, matkPercent);

        // Visuals on primary target
        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WITCH, mainTarget.getX(), mainTarget.getY() + 1.0, mainTarget.getZ(), 20, 0.2, 0.4, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.SOUL, mainTarget.getX(), mainTarget.getY() + 1.0, mainTarget.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, mainTarget.getX(), mainTarget.getY(), mainTarget.getZ(), 
                    SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.5f);
        }

        // Area Damage (~1.5 block radius = approx 3x3 cells)
        double radius = definition.getLevelDouble("aoe_radius", level, 1.5D);
        AABB area = mainTarget.getBoundingBox().inflate(radius);
        List<Entity> targets = player.level().getEntities(player, area, e -> e instanceof LivingEntity && e.isAlive());

        for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
                SkillDamageHelper.dealSkillDamage(living, player.level().damageSources().magic(), damage);
                
                // Small splash visual for extra targets
                if (player.level() instanceof ServerLevel sl && living != mainTarget) {
                    sl.sendParticles(ParticleTypes.WITCH, living.getX(), living.getY() + 1.0, living.getZ(), 8, 0.1, 0.2, 0.1, 0.01);
                }
            }
        }
    }
}
