package com.etema.ragnarmmo.system.skills.priest;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
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

            // Calc chance: (Level * 2) + (INT / 10) + (LUK / 10) % (simplified)
            float chance = (level * 5) / 100.0f;
            if (RANDOM.nextFloat() < chance) {
                target.hurt(player.damageSources().magic(), 9999.0f);
            } else {
                target.hurt(player.damageSources().magic(), 10.0f + (level * 5));
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
