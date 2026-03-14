package com.etema.ragnarmmo.roitems.commands;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public final class RoDebugCommands {

    private RoDebugCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roitems")
                .requires(source -> source.hasPermission(2)) // Operator level 2
                .then(Commands.literal("dump_held_item")
                        .executes(RoDebugCommands::dumpHeldItem))
                .then(Commands.literal("template")
                        .executes(RoDebugCommands::generateTemplate)));
    }

    private static int dumpHeldItem(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No item in main hand."));
                return 0;
            }

            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            context.getSource().sendSuccess(() -> Component.literal("Held Item ID: " + id), false);

            RoItemRule rule = RoItemRuleResolver.resolve(stack.getItem());
            if (rule != null && !rule.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("Has RO Rule: YES"), false);
            } else {
                context.getSource().sendSuccess(() -> Component.literal("Has RO Rule: NO"), false);
            }

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int generateTemplate(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No item in main hand."));
                return 0;
            }

            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            String displayName = stack.getHoverName().getString();

            String template = String.format(
                    "{\n" +
                            "  \"%s\": {\n" +
                            "    \"displayName\": \"%s\",\n" +
                            "    \"requiredBaseLevel\": 1,\n" +
                            "    \"allowedJobs\": [],\n" +
                            "    \"attributeBonuses\": {\n" +
                            "      \"str\": 0\n" +
                            "    },\n" +
                            "    \"cardSlots\": 0\n" +
                            "  }\n" +
                            "}",
                    id, displayName);

            RagnarMMO.LOGGER.info("RO Item Template for {}:\n{}", id, template);
            context.getSource().sendSuccess(() -> Component.literal("Template logged to console."), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
