package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class PushcartSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "pushcart");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0f, 0.8f);

        var skillsOpt = PlayerSkillsProvider.get(player).resolve();
        if (skillsOpt.isPresent()) {
            CartCommands.openCart(player, skillsOpt.get(), level);
        } else {
            player.sendSystemMessage(Component.literal("Pushcart data is not available."));
        }
    }
}
