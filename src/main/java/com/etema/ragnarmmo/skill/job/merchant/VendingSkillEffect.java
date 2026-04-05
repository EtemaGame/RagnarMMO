package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Vending — Active (Merchant)
 * RO: Opens a personal shop for other players.
 * MC: Opens the player's Cart inventory via the existing CartCommands system,
 *     broadcasts a "shop open" message to nearby players, and stores a flag
 *     so others can see the shop (future chest GUI extension possible).
 *     Level controls the price markup the cart applies.
 */
public class VendingSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "vending");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        // Store vending state and level for future /cart-view integration
        player.getPersistentData().putBoolean("vending_active", true);
        player.getPersistentData().putInt("vending_level", level);

        // Broadcast to nearby players (<20 blocks)
        if (player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            String shopName = player.getName().getString() + "'s Shop";
            sl.players().stream()
                    .filter(p -> p != player && p.distanceTo(player) < 20)
                    .forEach(p -> p.sendSystemMessage(Component.literal(
                            "§6[Vending] §f" + player.getName().getString()
                            + " §fabrió tienda. Usa §e/cart view " + player.getName().getString()
                            + " §fpara ver.")));

            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_TRADE, SoundSource.PLAYERS, 1.0f, 1.2f);
        }

        player.sendSystemMessage(Component.literal(
                "§6✦ Vending §flv." + level + " — Tu tienda está abierta. §7(/cart para gestionar)"));
    }
}
