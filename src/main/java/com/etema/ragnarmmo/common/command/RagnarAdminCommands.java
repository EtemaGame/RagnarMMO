package com.etema.ragnarmmo.common.command;

import com.etema.ragnarmmo.achievements.capability.PlayerAchievementsProvider;
import com.etema.ragnarmmo.achievements.data.AchievementDefinition;
import com.etema.ragnarmmo.achievements.data.AchievementRegistry;
import com.etema.ragnarmmo.achievements.network.SyncAchievementsPacket;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.debug.RagnarDebugChannel;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.player.stats.service.CharacterResetService;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
                .then(createDebugNode())
                .then(createUnlockNode())
                .then(createResetNode())
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

    public static LiteralArgumentBuilder<CommandSourceStack> createDebugNode() {
        return Commands.literal("debug")
                .then(Commands.literal("status")
                        .executes(ctx -> debugStatus(ctx.getSource())))
                .then(createDebugChannelNode("all", null))
                .then(createDebugChannelNode(RagnarDebugChannel.MASTER.commandName(), RagnarDebugChannel.MASTER))
                .then(createDebugChannelNode(RagnarDebugChannel.COMBAT.commandName(), RagnarDebugChannel.COMBAT))
                .then(createDebugChannelNode(RagnarDebugChannel.PLAYER_DATA.commandName(), RagnarDebugChannel.PLAYER_DATA))
                .then(createDebugChannelNode(RagnarDebugChannel.MOB_SPAWNS.commandName(), RagnarDebugChannel.MOB_SPAWNS))
                .then(createDebugChannelNode(RagnarDebugChannel.BOSS_WORLD.commandName(), RagnarDebugChannel.BOSS_WORLD));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createResetNode() {
        return Commands.literal("reset")
                .then(Commands.literal("stats")
                        .executes(ctx -> resetSelfStats(ctx.getSource())))
                .then(Commands.literal("skills")
                        .executes(ctx -> resetSelfSkills(ctx.getSource())))
                .then(Commands.literal("all")
                        .executes(ctx -> resetSelfAllNeedsConfirm(ctx.getSource()))
                        .then(Commands.literal("confirm")
                                .executes(ctx -> resetSelfAll(ctx.getSource()))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createUnlockNode() {
        return Commands.literal("unlock")
                .then(Commands.literal("skills")
                        .then(Commands.literal("all")
                                .executes(ctx -> unlockAllSkills(ctx.getSource()))))
                .then(Commands.literal("achievements")
                        .then(Commands.literal("all")
                                .executes(ctx -> unlockAllAchievements(ctx.getSource()))))
                .then(Commands.literal("all")
                        .executes(ctx -> unlockAll(ctx.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createDebugChannelNode(String literal,
            RagnarDebugChannel channel) {
        return Commands.literal(literal)
                .then(Commands.literal("on")
                        .executes(ctx -> setDebug(ctx.getSource(), channel, DebugAction.ON)))
                .then(Commands.literal("off")
                        .executes(ctx -> setDebug(ctx.getSource(), channel, DebugAction.OFF)))
                .then(Commands.literal("reset")
                        .executes(ctx -> setDebug(ctx.getSource(), channel, DebugAction.RESET)));
    }

    private static int unlockAll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int skillsChanged = unlockAllSkillsInternal(player);
        int achievementsChanged = unlockAllAchievementsInternal(player);
        source.sendSuccess(() -> Component.literal("Unlocked everything: skills=" + skillsChanged
                + ", achievements=" + achievementsChanged).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int unlockAllSkills(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int changed = unlockAllSkillsInternal(player);
        source.sendSuccess(() -> Component.literal("Unlocked all skills: " + changed + " changed.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int unlockAllAchievements(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int changed = unlockAllAchievementsInternal(player);
        source.sendSuccess(() -> Component.literal("Unlocked all achievements: " + changed + " changed.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int unlockAllSkillsInternal(ServerPlayer player) {
        return PlayerSkillsProvider.get(player).map(skills -> skills.unlockAllSkills(ChangeReason.ADMIN_COMMAND)).orElse(0);
    }

    private static int unlockAllAchievementsInternal(ServerPlayer player) {
        return player.getCapability(PlayerAchievementsProvider.PLAYER_ACHIEVEMENTS).map(cap -> {
            int before = cap.getUnlockedAchievements().size();
            for (AchievementDefinition def : AchievementRegistry.getInstance().getAll().values()) {
                cap.unlockAchievement(def.id(), def.points());
            }
            int changed = cap.getUnlockedAchievements().size() - before;
            SyncAchievementsPacket packet = new SyncAchievementsPacket(player.getId(),
                    (net.minecraft.nbt.CompoundTag) cap.serializeNBT());
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player, packet);
            return changed;
        }).orElse(0);
    }

    private static int setDebug(CommandSourceStack source, RagnarDebugChannel channel, DebugAction action) {
        if (channel == null) {
            switch (action) {
                case ON -> RagnarDebugLog.enableAll();
                case OFF -> RagnarDebugLog.disableAll();
                case RESET -> RagnarDebugLog.resetAll();
            }
        } else {
            switch (action) {
                case ON -> RagnarDebugLog.enable(channel);
                case OFF -> RagnarDebugLog.disable(channel);
                case RESET -> RagnarDebugLog.reset(channel);
            }
        }

        String target = channel == null ? "all" : channel.commandName();
        source.sendSuccess(
                () -> Component.literal("Debug " + target + " -> " + action.name().toLowerCase()
                        + " | " + RagnarDebugLog.describeStatus()).withStyle(ChatFormatting.YELLOW),
                false);
        return 1;
    }

    private static int debugStatus(CommandSourceStack source) {
        source.sendSuccess(
                () -> Component.literal("Debug status: " + RagnarDebugLog.describeStatus())
                        .withStyle(ChatFormatting.YELLOW),
                false);
        return 1;
    }

    private static int resetSelfStats(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterResetService.resetAllocatedStats(player);
        source.sendSuccess(() -> Component.literal("Your stats were reset.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int resetSelfSkills(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterResetService.resetLearnedSkills(player);
        source.sendSuccess(() -> Component.literal("Your skills were reset.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int resetSelfAllNeedsConfirm(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendFailure(Component.literal("Add 'confirm' to wipe your character: " + player.getName().getString())
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int resetSelfAll(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CharacterResetService.wipeCharacter(player);
        source.sendSuccess(() -> Component.literal("Your character was wiped.").withStyle(ChatFormatting.GREEN),
                true);
        return 1;
    }

    private static int resetStats(CommandSourceStack source, ServerPlayer target) {
        CharacterResetService.resetAllocatedStats(target);
        source.sendSuccess(
                () -> Component.literal("Stats reset for " + target.getName().getString()).withStyle(ChatFormatting.GREEN),
                true);
        target.sendSystemMessage(Component.literal("Your stats have been reset by an admin."));
        return 1;
    }

    private static int resetSkills(CommandSourceStack source, ServerPlayer target) {
        CharacterResetService.resetLearnedSkills(target);
        source.sendSuccess(
                () -> Component.literal("Skills reset for " + target.getName().getString()).withStyle(ChatFormatting.GREEN),
                true);
        target.sendSystemMessage(Component.literal("Your skills have been reset by an admin."));
        return 1;
    }

    private static int resetAllNeedsConfirm(CommandSourceStack source, ServerPlayer target) {
        source.sendFailure(Component.literal("Add 'confirm' to reset everything for " + target.getName().getString() + ".")
                .withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int resetAll(CommandSourceStack source, ServerPlayer target) {
        CharacterResetService.wipeCharacter(target);
        source.sendSuccess(
                () -> Component.literal("Full character wipe done for " + target.getName().getString())
                        .withStyle(ChatFormatting.GREEN),
                true);
        target.sendSystemMessage(Component.literal("Your character has been fully wiped by an admin."));
        return 1;
    }

    private enum DebugAction {
        ON,
        OFF,
        RESET
    }
}
