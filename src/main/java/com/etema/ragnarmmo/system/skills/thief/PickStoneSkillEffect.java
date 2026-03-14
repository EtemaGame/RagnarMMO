package com.etema.ragnarmmo.system.skills.thief;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PickStoneSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "pick_stone");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0)
            return;

        // Pick Stone: Adds a stone to inventory.
        // Fails if carrying capacity is full (Minecraft: Inventory is full).
        // Using Cobblestone as the "stone" substitute.

        ItemStack stone = new ItemStack(Items.COBBLESTONE, 1);

        if (player.getInventory().add(stone)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("Inventory full. Cannot pick stone."));
        }
    }
}
