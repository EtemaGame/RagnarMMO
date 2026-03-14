package com.etema.ragnarmmo.system.skills.priest;

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

public class SanctuarySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sanctuary");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Sanctuary: Heals friends in area and damages Undead/Demons.
        AABB area = player.getBoundingBox().inflate(5.0);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(), player.getZ(), 100,
                    4.0, 0.1, 4.0, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.0f, 1.0f);

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e.isAlive());
            for (LivingEntity target : targets) {
                if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) {
                    target.hurt(player.damageSources().indirectMagic(null, player), 5.0f + level);
                } else {
                    target.heal(2.0f + level);
                }
            }
        }
    }
}
