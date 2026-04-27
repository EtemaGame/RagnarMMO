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

/**
 * Vending - Active (Merchant).
 * Opens a data-driven personal shop marker over the player's cart inventory.
 */
public class VendingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "vending");

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
        int slotCount = definition != null ? definition.getLevelInt("vendor_slot_count", level, level + 2) : level + 2;
        double radius = definition != null ? definition.getLevelDouble("broadcast_radius", level, 20.0D) : 20.0D;

        player.getPersistentData().putBoolean("vending_active", true);
        player.getPersistentData().putInt("vending_level", level);
        player.getPersistentData().putInt("vending_slot_count", slotCount);

        if (player.level() instanceof ServerLevel serverLevel) {
            double radiusSq = radius * radius;
            serverLevel.players().stream()
                    .filter(other -> other != player && other.distanceToSqr(player) <= radiusSq)
                    .forEach(other -> other.sendSystemMessage(Component.literal(
                            "[Vending] " + player.getName().getString()
                                    + " opened a shop. Use /cart view " + player.getName().getString() + ".")));

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_TRADE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        player.sendSystemMessage(Component.literal(
                "Vending lv." + level + " opened with " + slotCount + " shop slots."));
    }
}
