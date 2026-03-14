package com.etema.ragnarmmo.system.skills.blacksmith;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class MaximizePowerSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "maximize_power");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Maximize Power: Toggles maximum damage variance.
        // We'll simulate by giving a significant Strength/Luck boost.
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.5, player.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS,
                    1.0f, 1.5f);

            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1 + (level / 2)));
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 600, 2));
        }
    }
}
