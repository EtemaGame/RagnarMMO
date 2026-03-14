package com.etema.ragnarmmo.common.command;

import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RagnarAdminCommands {
        private RagnarAdminCommands() {
        }

        public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
                return Commands.literal("admin")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("player")
                                                .then(Commands.literal("reset")
                                                                .then(Commands.argument("player", EntityArgument.player())
                                                                                .then(Commands.literal("stats")
                                                                                                .executes(ctx -> resetStats(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(ctx, "player"))))
                                                                                .then(Commands.literal("skills")
                                                                                                .executes(ctx -> resetSkills(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(ctx, "player"))))
                                                                                .then(Commands.literal("all")
                                                                                                .executes(ctx -> resetAllNeedsConfirm(
                                                                                                                ctx.getSource(),
                                                                                                                EntityArgument.getPlayer(ctx, "player")))
                                                                                                .then(Commands.literal("confirm")
                                                                                                                .executes(ctx -> resetAll(
                                                                                                                                ctx.getSource(),
                                                                                                                                EntityArgument.getPlayer(ctx, "player"))))))));
        }

        private static int resetStats(CommandSourceStack source, ServerPlayer target) {
                CommandUtil.getStats(target).ifPresent(stats -> stats.resetAll(ChangeReason.ADMIN_COMMAND));
                source.sendSuccess(() -> Component.literal("§aStats reset for " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);
                target.sendSystemMessage(Component.literal("§eYour stats have been reset by an admin."));
                return 1;
        }

        private static int resetSkills(CommandSourceStack source, ServerPlayer target) {
                CommandUtil.getSkills(target).ifPresent(skills -> skills.resetAll(ChangeReason.ADMIN_COMMAND));
                source.sendSuccess(() -> Component.literal("§aSkills reset for " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);
                target.sendSystemMessage(Component.literal("§eYour skills have been reset by an admin."));
                return 1;
        }

        private static int resetAllNeedsConfirm(CommandSourceStack source, ServerPlayer target) {
                source.sendFailure(Component.literal("§cAdd 'confirm' to reset everything for " + target.getName().getString() + ".").withStyle(ChatFormatting.RED));
                return 0;
        }

        private static int resetAll(CommandSourceStack source, ServerPlayer target) {
                CommandUtil.getStats(target).ifPresent(stats -> stats.resetAll(ChangeReason.ADMIN_COMMAND));
                CommandUtil.getSkills(target).ifPresent(skills -> skills.resetAll(ChangeReason.ADMIN_COMMAND));
                source.sendSuccess(() -> Component.literal("§aFull reset done for " + target.getName().getString()).withStyle(ChatFormatting.GREEN), true);
                target.sendSystemMessage(Component.literal("§eYour character has been fully reset by an admin."));
                return 1;
        }
}
