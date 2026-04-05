package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
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

public class MagnusExorcismusSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "magnus_exorcismus");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        // Magnus Exorcismus: Large holy area that damages Undead/Demons.
        AABB area = player.getBoundingBox().inflate(8.0);

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.5f);

            // Magnus Exorcismus: Multiple waves of holy damage.
            // RO: Deals 1 hit per Skill Level (approx). Lvl 10 = 10 hits.
            int hits = level * 2; // Extra hits for better feel in MC
            float damagePerHit = com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByMATK(player, 100.0f);

            for (int i = 0; i < hits; i++) {
                int delay = i * 5; // Every 0.25s
                com.etema.ragnarmmo.skill.runtime.SkillSequencer.schedule(delay, () -> {
                    serverLevel.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 0.1, player.getZ(), 5, 4.0, 0.1, 4.0, 0.05);
                    serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.1, player.getZ(), 20, 6.0, 0.5, 6.0, 0.01);
                    
                    List<LivingEntity> waveTargets = serverLevel.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive());
                    for (LivingEntity target : waveTargets) {
                        if (target.getMobType() == MobType.UNDEAD) {
                            com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, player.damageSources().indirectMagic(null, player), damagePerHit);
                            serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.5, 0.2, 0.05);
                        }
                    }
                });
            }
        }
    }
}
