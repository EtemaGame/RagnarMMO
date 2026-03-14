package com.etema.ragnarmmo.system.skills.assassin;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class GrimtoothSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo:grimtooth");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Grimtooth: Ranged AoE effect, usable only when hiding.
        // We'll check for invisibility or a "hiding" state if it exists.

        if (player.level() instanceof ServerLevel serverLevel) {
            double range = 3.0 + level;
            AABB area = player.getBoundingBox().inflate(range);

            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(), player.getZ(), 50, range, 0.5,
                    range, 0.1);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f,
                    0.5f);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), 5.0f + (level * 2.0f));
            }
        }
    }
}
