package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.skill.api.SkillType;
import com.etema.ragnarmmo.common.command.CommandUtil;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.data.progression.SkillProgress;
import com.etema.ragnarmmo.skill.runtime.SkillManager;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.ItemStackHandler;
import javax.annotation.Nonnull;

public class CartCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("cart")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    return PlayerSkillsProvider.get(player).map(skills -> {
                        int level = skills.getSkillLevel(SkillType.CART_STRENGTH);
                        if (level <= 0) {
                            CommandUtil.sendFail(ctx.getSource(),
                                    Component.translatable("commands.ragnarmmo.cart.missing_skill"));
                            return 0;
                        }

                        openCart(player, skills, level);
                        return 1;
                    }).orElse(0);
                });
    }

    private static void openCart(ServerPlayer player, SkillManager skills, int level) {
        int rows = Math.min(6, level);
        ItemStackHandler handler = skills.getCartInventory();
        int cartSlots = rows * 9;

        // Create a temporary SimpleContainer to sync with the Menu
        net.minecraft.world.SimpleContainer inventory = new net.minecraft.world.SimpleContainer(cartSlots);
        for (int i = 0; i < cartSlots; i++) {
            inventory.setItem(i, handler.getStackInSlot(i).copy());
        }

        player.openMenu(new SimpleMenuProvider((id, playerInv, p) -> {
            MenuType<ChestMenu> menuType = switch (rows) {
                case 1 -> MenuType.GENERIC_9x1;
                case 2 -> MenuType.GENERIC_9x2;
                case 3 -> MenuType.GENERIC_9x3;
                case 4 -> MenuType.GENERIC_9x4;
                case 5 -> MenuType.GENERIC_9x5;
                case 6 -> MenuType.GENERIC_9x6;
                default -> MenuType.GENERIC_9x1;
            };

            return new ChestMenu(menuType, id, playerInv, inventory, rows) {
                @Override
                    public void removed(@Nonnull net.minecraft.world.entity.player.Player pPlayer) {
                        super.removed(pPlayer);
                        // Sync back to ItemStackHandler on close
                    for (int i = 0; i < cartSlots; i++) {
                        handler.setStackInSlot(i, inventory.getItem(i).copy());
                    }
                }

                @Override
                public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player pPlayer) {
                    return true;
                }
            };
        }, Component.literal("Pushcart (Lv." + level + ")")));
    }
}
