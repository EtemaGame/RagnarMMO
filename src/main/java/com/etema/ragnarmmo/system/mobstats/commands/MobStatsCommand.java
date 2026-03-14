package com.etema.ragnarmmo.system.mobstats.commands;

import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager.DifficultyReport;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Random;

/**
 * Simple debug command that prints the level, tier and stats of a mob. Useful
 * when tuning configurations or testing mob pack compatibility.
 */
public final class MobStatsCommand {

    private MobStatsCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("mobstats")
                .requires(stack -> stack.hasPermission(2))
                .executes(ctx -> run(ctx, s -> showStats(ctx.getSource(), ctx.getSource().getEntityOrException())))
                .then(Commands.argument("target", EntityArgument.entity())
                        .executes(ctx -> run(ctx,
                                s -> showStats(ctx.getSource(), EntityArgument.getEntity(ctx, "target")))))
                .then(Commands.literal("difficulty")
                        .executes(ctx -> run(ctx, s -> showWorldDifficulty(ctx.getSource()))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, CommandFunction action) {
        try {
            return action.apply(ctx);
        } catch (CommandSyntaxException e) {
            return 0;
        }
    }

    @FunctionalInterface
    private interface CommandFunction {
        int apply(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private static int showStats(CommandSourceStack source, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            source.sendFailure(Component.literal("The selected target is not a valid mob."));
            return 0;
        }

        MobStats stats = MobStatsProvider.get(living).orElse(null);
        if (stats == null || !stats.isInitialized()) {
            source.sendSuccess(() -> Component.literal("The mob has no registered stats."), false);
            return 0;
        }

        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        Component header = Component.literal(String.format(Locale.ROOT,
                "Mob %s - Level %d (%s)",
                id != null ? id.toString() : living.getDisplayName().getString(),
                stats.getLevel(),
                stats.getTier().name().toLowerCase(Locale.ROOT)));
        source.sendSuccess(() -> header, false);

        source.sendSuccess(() -> Component.literal("Stats: " + stats.describeStats()), false);
        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Multipliers -> HP %.2f | DMG %.2f | DEF %.2f | SPD %.2f",
                stats.getHealthMultiplier(),
                stats.getDamageMultiplier(),
                stats.getDefenseMultiplier(),
                stats.getSpeedMultiplier())), false);

        source.sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Total points: %d", stats.getTotalPoints())), false);
        return 1;
    }

    private static int showWorldDifficulty(CommandSourceStack source) {
        ServerLevel serverLevel = source.getLevel();

        // Create a temporary MobLevelManager to gather debug info
        MobLevelManager manager = new MobLevelManager(new Random());
        DifficultyReport report = manager.getDifficultyReport(serverLevel);

        source.sendSuccess(() -> Component.literal("§6=== World Difficulty ==="), false);
        report.lines().forEach(line -> source.sendSuccess(() -> Component.literal(line), false));

        source.sendSuccess(() -> Component.literal("§aActive system: Advanced Zone Scaling (Phase 2)"), false);

        return 1;
    }
}
