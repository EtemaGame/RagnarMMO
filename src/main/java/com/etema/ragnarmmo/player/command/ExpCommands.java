package com.etema.ragnarmmo.player.command;

import com.etema.ragnarmmo.common.command.CommandUtil;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.mobs.companion.CompanionProfileService;
import com.etema.ragnarmmo.player.stats.capability.PlayerStatsProvider;
import com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService;
import com.etema.ragnarmmo.player.progression.PlayerProgressionService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /ragnar exp lv <amount>     — Add base XP (OP only)
 * /ragnar exp joblv <amount>  — Add job XP (OP only)
 * /ragnar set lv <level>      — Set base level (OP only)
 * /ragnar set joblv <level>   — Set job level (OP only)
 */
public final class ExpCommands {

    private ExpCommands() {}

    // ─── /ragnar exp ────────────────────────────────────────────────────────────

    public static LiteralArgumentBuilder<CommandSourceStack> createExpNode() {
        return Commands.literal("exp")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("lv")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10_000_000))
                                .executes(ctx -> addBaseExp(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))

                .then(Commands.literal("joblv")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10_000_000))
                                .executes(ctx -> addJobExp(ctx, IntegerArgumentType.getInteger(ctx, "amount")))));
    }

    // ─── /ragnar set ─────────────────────────────────────────────────────────────

    public static LiteralArgumentBuilder<CommandSourceStack> createSetNode() {
        return Commands.literal("set")
                .requires(src -> src.hasPermission(2))

                .then(Commands.literal("lv")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 9999))
                                .executes(ctx -> setLevel(ctx, IntegerArgumentType.getInteger(ctx, "level")))))

                .then(Commands.literal("joblv")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 9999))
                                .executes(ctx -> setJobLevel(ctx, IntegerArgumentType.getInteger(ctx, "level")))));
    }

    // ─── Handlers ────────────────────────────────────────────────────────────────

    private static int addBaseExp(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        int[] result = {0};
        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            int lvBefore = stats.getLevel();
            PlayerProgressionService progressionService = PlayerProgressionService
                    .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()));
            stats.addExpAndProcessLevelUps(amount, RagnarConfigs.SERVER.progression.pointsPerLevel.get(),
                    progressionService::baseExpToNext);
            sync(player, stats);
            if (stats.getLevel() > lvBefore) {
                CompanionProfileService.refreshOwnedCompanions(player);
                sendOk(player, "✦ +" + amount + " base XP  →  Level " + stats.getLevel()
                        + " (+" + (stats.getLevel() - lvBefore) + " levels)");
            } else {
                sendOk(player, "✦ +" + amount + " base XP  (Level " + stats.getLevel()
                        + ", " + stats.getExp() + "/" + progressionService.baseExpToNext(stats.getLevel()) + " XP)");
            }
            result[0] = 1;
        });
        if (result[0] == 0) sendErr(player, "Missing stats capability!");
        return result[0];
    }

    private static int addJobExp(CommandContext<CommandSourceStack> ctx, int amount) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        int[] result = {0};
        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            int lvBefore = stats.getJobLevel();
            PlayerProgressionService progressionService = PlayerProgressionService
                    .forJobId(net.minecraft.resources.ResourceLocation.tryParse(stats.getJobId()));
            stats.addJobExpAndProcessLevelUps(amount, progressionService::jobExpToNext);
            sync(player, stats);
            if (stats.getJobLevel() > lvBefore) {
                sendOk(player, "✦ +" + amount + " job XP  →  Job Level " + stats.getJobLevel()
                        + " (+" + (stats.getJobLevel() - lvBefore) + " levels)");
            } else {
                sendOk(player, "✦ +" + amount + " job XP  (Job Level " + stats.getJobLevel()
                        + ", " + stats.getJobExp() + "/" + progressionService.jobExpToNext(stats.getJobLevel()) + " XP)");
            }
            result[0] = 1;
        });
        if (result[0] == 0) sendErr(player, "Missing stats capability!");
        return result[0];
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        int[] result = {0};
        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            int lvBefore = stats.getLevel();
            stats.setLevel(level);
            stats.setExp(0);
            sync(player, stats);
            if (stats.getLevel() != lvBefore) {
                CompanionProfileService.refreshOwnedCompanions(player);
            }
            sendOk(player, "✦ Base level set to " + stats.getLevel());
            result[0] = 1;
        });
        if (result[0] == 0) sendErr(player, "Missing stats capability!");
        return result[0];
    }

    private static int setJobLevel(CommandContext<CommandSourceStack> ctx, int level) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        int[] result = {0};
        player.getCapability(PlayerStatsProvider.CAP).ifPresent(stats -> {
            stats.setJobLevel(level);
            stats.setJobExp(0);
            sync(player, stats);
            sendOk(player, "✦ Job level set to " + stats.getJobLevel());
            result[0] = 1;
        });
        if (result[0] == 0) sendErr(player, "Missing stats capability!");
        return result[0];
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static void sync(ServerPlayer player,
            com.etema.ragnarmmo.common.api.stats.IPlayerStats stats) {
        PlayerStatsSyncService.sync(player, stats);
    }

    private static void sendOk(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal("§a" + text));
    }

    private static void sendErr(ServerPlayer player, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.RED));
    }
}
