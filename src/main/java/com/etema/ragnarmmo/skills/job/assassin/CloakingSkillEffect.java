package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class CloakingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "cloaking");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        // Cloaking: Advanced Hiding. 
        // RO: Level 1-2 only near walls. Level 3+ anywhere.
        String tag = "ragnarmmo_cloaking";
        
        if (player.getTags().contains(tag)) {
            player.removeTag(tag);
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cCloaking OFF"));
        } else {
            // Speed penalty: -75% at Lv1 to +25% at Lv10 (RO logic)
            // MC: Slowdown 4 is approx -60%. 
            int slowLevel = Math.max(0, 4 - (level / 2));
            
            player.addTag(tag);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12000, 0, false, false));
            if (slowLevel > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12000, slowLevel - 1, false, false));
            }
            
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aCloaking ON"));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 0.1f);
            
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
}
