package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
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
        // Maximize Power: While active, damage is always the maximum possible.
        // We use a tag that CommonEvents will check during Damage Variance calculation.
        String tag = "ragnarmmo_maximize_power";
        if (player.getTags().contains(tag)) {
            player.removeTag(tag);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cMaximize Power OFF"));
        } else {
            player.addTag(tag);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aMaximize Power ON"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0f, 1.5f);
        }
        
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.5, player.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0);
        }
    }
}
