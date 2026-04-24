package com.etema.ragnarmmo.player.command;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.command.CommandUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

public final class StatsCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("stats")
                .executes(StatsCommands::showSelf);
    }

    private static int showSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = CommandUtil.requirePlayer(ctx);
        return RagnarCoreAPI.get(player).map(stats -> showStats(ctx.getSource(), stats)).orElseGet(() -> {
            CommandUtil.sendFail(ctx.getSource(),
                    Component.translatable("commands.ragnarmmo.player.missing_stats").withStyle(net.minecraft.ChatFormatting.RED));
            return 0;
        });
    }

    private static int showStats(CommandSourceStack source, IPlayerStats stats) {
        JobType job = JobType.fromId(stats.getJobId());
        Component jobName = Component.translatable("job.ragnarmmo." + job.getId());

        source.sendSuccess(() -> Component.translatable("commands.ragnarmmo.stats.summary",
                stats.getLevel(), stats.getExp(), stats.getStatPoints(),
                jobName, stats.getJobLevel(), stats.getJobExp(), stats.getSkillPoints(),
                stats.getSTR(), stats.getAGI(), stats.getVIT(), stats.getINT(), stats.getDEX(), stats.getLUK(),
                String.format(Locale.ROOT, "%.1f", stats.getMana())), false);
        return 1;
    }
}
