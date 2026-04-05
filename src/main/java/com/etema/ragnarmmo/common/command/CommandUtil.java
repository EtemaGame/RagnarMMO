package com.etema.ragnarmmo.common.command;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.skill.api.IPlayerSkills;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return ctx.getSource().getPlayerOrException();
    }

    public static void sendOk(CommandSourceStack source, Component message) {
        sendOk(source, message, false);
    }

    public static void sendOk(CommandSourceStack source, Component message, boolean broadcastToOps) {
        source.sendSuccess(() -> message, broadcastToOps);
    }

    public static void sendFail(CommandSourceStack source, Component message) {
        source.sendFailure(message);
    }

    public static Optional<IPlayerStats> getStats(ServerPlayer player) {
        return player == null ? Optional.empty() : RagnarCoreAPI.get(player);
    }

    public static Optional<IPlayerSkills> getSkills(ServerPlayer player) {
        if (player == null) {
            return Optional.empty();
        }
        return PlayerSkillsProvider.get(player).resolve().map(skills -> (IPlayerSkills) skills);
    }

    public static boolean withStats(ServerPlayer player, Consumer<IPlayerStats> action) {
        return getStats(player).map(stats -> {
            action.accept(stats);
            return true;
        }).orElse(false);
    }

    public static int withStats(ServerPlayer player, ToIntFunction<IPlayerStats> action) {
        return getStats(player).map(action::applyAsInt).orElse(0);
    }

    public static boolean withSkills(ServerPlayer player, Consumer<IPlayerSkills> action) {
        return getSkills(player).map(skills -> {
            action.accept(skills);
            return true;
        }).orElse(false);
    }

    public static int withSkills(ServerPlayer player, ToIntFunction<IPlayerSkills> action) {
        return getSkills(player).map(action::applyAsInt).orElse(0);
    }
}

