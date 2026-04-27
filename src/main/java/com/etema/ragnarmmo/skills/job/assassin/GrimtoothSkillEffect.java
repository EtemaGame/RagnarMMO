package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "grimtooth");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (player.level() instanceof ServerLevel serverLevel) {
            // Grimtooth: RO: Ranged AoE attack while hidden
            if (!player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cRequires Hiding or Cloaking!"));
                return;
            }

            double range = 4.0 + (level * 0.5); // RO: 3-7 cell range
            AABB area = player.getBoundingBox().inflate(range);

            serverLevel.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY(), player.getZ(), 30, range, 0.2,
                    range, 0.05);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f,
                    1.2f);

            float totalPct = 100f + (80f * level);
            float damage = Math.max(com.etema.ragnarmmo.combat.damage.SkillDamageHelper.MIN_ATK,
                    com.etema.ragnarmmo.combat.damage.SkillDamageHelper.scaleByATK(player, totalPct));

            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e != player && e.isAlive());
            for (LivingEntity target : targets) {
                com.etema.ragnarmmo.combat.damage.SkillDamageHelper.dealSkillDamage(target, 
                        player.damageSources().playerAttack(player), damage);
            }
        }
    }
}
