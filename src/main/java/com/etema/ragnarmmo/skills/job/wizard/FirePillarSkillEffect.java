package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class FirePillarSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "fire_pillar");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        // Fire Pillar: Places a trap-like pillar of fire on the ground.
        // RO: Deals 50% MATK per hit, multiple hits. (Simplified for now)
        float damage = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 50.0f + (level * 5.0f));

        BlockPos pos = player.blockPosition().relative(player.getDirection(), 2);
        AABB area = new AABB(pos).inflate(1.5, 2.0, 1.5);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 50,
                    0.2, 1.0, 0.2, 0.05);
            serverLevel.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().magic(), damage);
                target.setRemainingFireTicks(40);
            }
        }
    }
}
