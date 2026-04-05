package com.etema.ragnarmmo.roitems.commands;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.roitems.data.RoItemRule;
import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.roitems.runtime.RoRefineMath;
import com.etema.ragnarmmo.roitems.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.roitems.runtime.RoRefineService;
import com.etema.ragnarmmo.roitems.runtime.RoItemTextHelper;
import com.etema.ragnarmmo.roitems.runtime.ZenyWalletHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                .then(Commands.literal("refine")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, RoItemNbtHelper.MAX_REFINE_LEVEL))
                                .executes(RoDebugCommands::setHeldRefine)))
                .then(Commands.literal("refine_info")
                        .executes(RoDebugCommands::showRefineInfo))
                .then(Commands.literal("try_refine")
                        .executes(RoDebugCommands::tryRefineHeldItem))
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

            int refine = RoItemNbtHelper.getRefineLevel(stack);
            context.getSource().sendSuccess(() -> Component.literal("Refine: +" + refine), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int setHeldRefine(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No item in main hand."));
                return 0;
            }

            if (!RoRefineMath.isRefinable(stack)) {
                context.getSource().sendFailure(Component.literal("Held item is not refinable."));
                return 0;
            }

            int level = IntegerArgumentType.getInteger(context, "level");
            RoItemNbtHelper.setRefineLevel(stack, level);
            context.getSource().sendSuccess(() -> Component.literal("Refine set: " + RoItemTextHelper.getDisplayNameString(stack)),
                    false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int showRefineInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No item in main hand."));
                return 0;
            }

            RoRefineService.RefineQuote quote = RoRefineService.quote(player, stack);
            switch (quote.outcome()) {
                case DISABLED -> context.getSource().sendFailure(Component.literal("Refine system is disabled in config."));
                case INVALID_ITEM -> context.getSource().sendFailure(Component.literal("Held item is not refinable."));
                case MAX_REACHED -> context.getSource().sendFailure(Component.literal(
                        RoItemTextHelper.getDisplayNameString(stack) + " is already at max refine."));
                default -> {
                    String chanceText = quote.safe()
                            ? "SAFE"
                            : Math.round(quote.successChance() * 100.0) + "%";
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Refine +" + quote.currentLevel() + " -> +" + quote.targetLevel()
                                    + " | Material: " + quote.material().getHoverName().getString() + " x" + quote.materialCount()
                                    + " (" + quote.availableMaterial() + " owned)"
                                    + " | Cost: " + ZenyWalletHelper.formatZeny(quote.zenyCost())
                                    + " (" + ZenyWalletHelper.formatZeny(quote.availableZeny()) + " owned)"
                                    + " | Chance: " + chanceText),
                            false);
                }
            }
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }

    private static int tryRefineHeldItem(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No item in main hand."));
                return 0;
            }

            RoRefineService.RefineResult result = RoRefineService.attempt(player, stack);
            RoRefineService.RefineQuote quote = result.quote();
            switch (result.outcome()) {
                case SUCCESS -> context.getSource().sendSuccess(() -> Component.literal(
                        "Refine success: " + RoItemTextHelper.getDisplayNameString(stack)
                                + " (spent " + ZenyWalletHelper.formatZeny(quote.zenyCost())
                                + " + " + quote.material().getHoverName().getString() + " x" + quote.materialCount() + ")"),
                        false);
                case FAILURE_DOWNGRADE -> context.getSource().sendFailure(Component.literal(
                        "Refine failed: " + RoItemTextHelper.getDisplayNameString(stack)
                                + " after spending " + ZenyWalletHelper.formatZeny(quote.zenyCost()) + "."));
                case FAILURE_STABLE -> context.getSource().sendFailure(Component.literal(
                        "Refine failed: " + RoItemTextHelper.getDisplayNameString(stack) + "."));
                case MISSING_MATERIAL -> context.getSource().sendFailure(Component.literal(
                        "Missing material: need " + quote.material().getHoverName().getString() + " x" + quote.materialCount()
                                + " but only have " + quote.availableMaterial() + "."));
                case MISSING_ZENY -> context.getSource().sendFailure(Component.literal(
                        "Missing zeny: need " + ZenyWalletHelper.formatZeny(quote.zenyCost())
                                + " but only have " + ZenyWalletHelper.formatZeny(quote.availableZeny()) + "."));
                case MAX_REACHED -> context.getSource().sendFailure(Component.literal(
                        RoItemTextHelper.getDisplayNameString(stack) + " is already at max refine."));
                case DISABLED -> context.getSource().sendFailure(Component.literal("Refine system is disabled in config."));
                default -> context.getSource().sendFailure(Component.literal("Held item is not refinable."));
            }
            return result.outcome() == RoRefineService.RefineOutcome.SUCCESS ? 1 : 0;
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
