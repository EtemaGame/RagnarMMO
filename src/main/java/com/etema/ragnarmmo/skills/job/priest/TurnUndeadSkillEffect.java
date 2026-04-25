package com.etema.ragnarmmo.skills.job.priest;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Random;

public class TurnUndeadSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:turn_undead");
    private static final Random RANDOM = new Random();

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Turn Undead: Chance to instant kill Undead monsters.
        LivingEntity target = getClosestTarget(player, 8.0);
        if (target == null)
            return;

        if (target.getMobType() != MobType.UNDEAD)
            return;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.5,
                    0.5, 0.5, 0);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED,
                    SoundSource.PLAYERS, 1.0f, 1.5f);

            // Calc chance: RO Formula: [(20*SkillLv + LUK + INT + BaseLv) / 10] * [1 - HP/MaxHP] + [SkillLv * 2]%
            // For MC, we use a slightly more generous version.
            float matk = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.getMATK(player);
            int intVal = (int) (matk / 5.0f); // Proxy for INT stat
            int baseLv = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.getBaseLevel(player);
            float hpPercent = target.getHealth() / target.getMaxHealth();
            
            float chance = ((level * 20 + 30 + intVal + baseLv) / 10.0f) * (1.0f - hpPercent) + (level * 2.0f);
            chance /= 100.0f;

            if (RANDOM.nextFloat() < chance) {
                target.hurt(player.damageSources().magic(), 9999.0f);
            } else {
                // Fail damage RO: (SkillLv * 10 + INT + LUK + BaseLv)% MATK
                float failDamagePercent = (level * 10 + 30 + intVal + baseLv);
                float failDamage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, failDamagePercent);
                target.hurt(player.damageSources().magic(), failDamage);
            }
        }
    }

    private LivingEntity getClosestTarget(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        if (targets.isEmpty())
            return null;
        return targets.get(0);
    }
}
