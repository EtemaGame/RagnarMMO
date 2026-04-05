package com.etema.ragnarmmo.skill.job.priest;

import com.etema.ragnarmmo.common.init.RagnarMobEffects;
import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class KyrieEleisonSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "kyrie_eleison");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        
        LivingEntity target = getClosestTarget(player, 10.0);
        if (target == null)
            target = player;

        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 30, 0.5,
                    1.0, 0.5, 0.05);
            serverLevel.playSound(null, target.blockPosition(), RagnarSounds.KYRIE_ELEISON.get(), SoundSource.PLAYERS, 1.0f,
                    1.0f);

            // Kyrie Eleison: Barrier that blocks physical damage.
            // We use Absorption for the shield and custom KYRIE_ELEISON for Armor/Icon.
            target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, level));
            target.addEffect(new MobEffectInstance(RagnarMobEffects.KYRIE_ELEISON.get(), 600, level - 1));
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
