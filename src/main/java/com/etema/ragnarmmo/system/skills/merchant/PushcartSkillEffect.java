package com.etema.ragnarmmo.system.skills.merchant;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class PushcartSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "pushcart");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Pushcart: Allows the Merchant to rent a Pushcart.
        // In Ragnarok, you rent it from a Kafra. In Minecraft, this active skill could
        // toggle its visibility or spawn a cart entity.
        // The cart provides 8000 extra weight limit. We could simulate this by opening
        // an Ender Chest-like inventory.

        // For this skeleton, we will play a sound and send a message.
        // Actual implementation would involve opening a custom Container/Menu.

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.PLAYERS, 1.0f, 0.8f);

        player.sendSystemMessage(Component.literal("Opened Pushcart Storage (Level " + level + ")."));
    }
}
