package com.etema.ragnarmmo.skills.job.merchant;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class BuyingStoreSkillEffect implements ISkillEffect {
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "buying_store");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        SkillDefinition definition = SkillRegistry.get(ID).orElse(null);
        int orderSlots = definition != null ? definition.getLevelInt("buy_order_slots", level, 5) : 5;
        double radius = definition != null ? definition.getLevelDouble("broadcast_radius", level, 20.0D) : 20.0D;

        player.getPersistentData().putBoolean("buying_store_active", true);
        player.getPersistentData().putInt("buying_store_slots", orderSlots);

        if (player.level() instanceof ServerLevel serverLevel) {
            double radiusSq = radius * radius;
            serverLevel.players().stream()
                    .filter(other -> other != player && other.distanceToSqr(player) <= radiusSq)
                    .forEach(other -> other.sendSystemMessage(Component.literal(
                            "[Buying Store] " + player.getName().getString() + " is looking for items.")));

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.9f, 1.1f);
        }

        player.sendSystemMessage(Component.literal(
                "Buying Store opened with " + orderSlots + " order slots."));
    }
}
