package com.etema.ragnarmmo.system.stats.party;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Commands for the persistent party system.
 * /party create <name> - Create a party
 * /party invite <player> - Invite a player
 * /party accept - Accept an invite
 * /party decline - Decline an invite
 * /party leave - Leave current party
 * /party kick <player> - Kick a member (leader only)
 * /party promote <player> - Promote to leader
 * /party list - List party members
 * /party info - Show party info
 * /party chat <message> - Send party message
 * /party disband - Disband the party (leader only)
 * /party settings xp <on|off> - Toggle XP sharing
 * /party settings range <blocks> - Set share range
 */
public class PartyCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> createNode() {
        return Commands.literal("party")
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> createParty(ctx, StringArgumentType.getString(ctx, "name")))))

                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommands::invitePlayer)))

                .then(Commands.literal("accept")
                        .executes(PartyCommands::acceptInvite))

                .then(Commands.literal("decline")
                        .executes(PartyCommands::declineInvite))

                .then(Commands.literal("leave")
                        .executes(PartyCommands::leaveParty))

                .then(Commands.literal("kick")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommands::kickMember)))

                .then(Commands.literal("promote")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(PartyCommands::promoteMember)))

                .then(Commands.literal("list")
                        .executes(PartyCommands::listMembers))

                .then(Commands.literal("info")
                        .executes(PartyCommands::partyInfo))

                .then(Commands.literal("chat")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> partyChat(ctx, StringArgumentType.getString(ctx, "message")))))

                .then(Commands.literal("disband")
                        .executes(PartyCommands::disbandParty))

                .then(Commands.literal("settings")
                        .then(Commands.literal("xp")
                                .then(Commands.literal("on")
                                        .executes(ctx -> setXpShare(ctx, true)))
                                .then(Commands.literal("off")
                                        .executes(ctx -> setXpShare(ctx, false))))
                        .then(Commands.literal("range")
                                .then(Commands.argument("blocks", IntegerArgumentType.integer(10, 200))
                                        .executes(ctx -> setShareRange(ctx, IntegerArgumentType.getInteger(ctx, "blocks"))))));
    }

    // === Command Handlers ===

    private static int createParty(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        PartyService.CreateResult result = service.createParty(player, name);

        if (result.success()) {
            player.sendSystemMessage(Component.literal("\u00A7aParty '" + name + "' created!"));
            return 1;
        } else {
            sendError(player, result.messageKey());
            return 0;
        }
    }

    private static int invitePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PartyService service = PartyService.get(player.getServer());

        PartyService.InviteResult result = service.invitePlayer(player, target);

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("\u00A7aInvited " + target.getName().getString() + " to the party."));
                return 1;
            }
            case NOT_IN_PARTY -> sendError(player, "You're not in a party!");
            case NOT_LEADER -> sendError(player, "Only the leader can invite!");
            case TARGET_ALREADY_IN_PARTY -> sendError(player, "That player is already in a party!");
            case PARTY_FULL -> sendError(player, "Party is full!");
            case ALREADY_INVITED -> sendError(player, "That player already has a pending invite!");
            case CANNOT_INVITE_SELF -> sendError(player, "You can't invite yourself!");
            default -> sendError(player, "Could not send invite.");
        }
        return 0;
    }

    private static int acceptInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        PartyService.AcceptResult result = service.acceptInvite(player);

        switch (result) {
            case SUCCESS -> {
                Party party = service.getParty(player);
                String partyName = party != null ? party.getName() : "?";
                player.sendSystemMessage(Component.literal("\u00A7aJoined party '" + partyName + "'!"));
                return 1;
            }
            case NO_INVITE -> sendError(player, "No pending invite!");
            case INVITE_EXPIRED -> sendError(player, "Invite has expired!");
            case PARTY_NOT_FOUND -> sendError(player, "Party no longer exists!");
            case PARTY_FULL -> sendError(player, "Party is full!");
            case ALREADY_IN_PARTY -> sendError(player, "You're already in a party!");
        }
        return 0;
    }

    private static int declineInvite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        if (service.declineInvite(player)) {
            player.sendSystemMessage(Component.literal("\u00A7aInvite declined."));
            return 1;
        }
        sendError(player, "No pending invite!");
        return 0;
    }

    private static int leaveParty(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        PartyService.LeaveResult result = service.leaveParty(player);

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("\u00A7aYou left the party."));
                return 1;
            }
            case DISBANDED -> {
                player.sendSystemMessage(Component.literal("\u00A7aParty disbanded."));
                return 1;
            }
            case NOT_IN_PARTY -> sendError(player, "You're not in a party!");
        }
        return 0;
    }

    private static int kickMember(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PartyService service = PartyService.get(player.getServer());

        PartyService.KickResult result = service.kickMember(player, target.getUUID());

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("\u00A7aKicked " + target.getName().getString() + " from the party."));
                return 1;
            }
            case NOT_IN_PARTY -> sendError(player, "You're not in a party!");
            case NOT_LEADER -> sendError(player, "Only the leader can kick members!");
            case TARGET_NOT_IN_PARTY -> sendError(player, "That player is not in your party!");
            case CANNOT_KICK_SELF -> sendError(player, "You can't kick yourself! Use /party leave.");
        }
        return 0;
    }

    private static int promoteMember(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PartyService service = PartyService.get(player.getServer());

        PartyService.PromoteResult result = service.promoteToLeader(player, target.getUUID());

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("\u00A7aPromoted " + target.getName().getString() + " to leader."));
                return 1;
            }
            case NOT_IN_PARTY -> sendError(player, "You're not in a party!");
            case NOT_LEADER -> sendError(player, "Only the leader can promote members!");
            case TARGET_NOT_IN_PARTY -> sendError(player, "That player is not in your party!");
            case ALREADY_LEADER -> sendError(player, "That player is already the leader!");
        }
        return 0;
    }

    private static int listMembers(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        Party party = service.getParty(player);
        if (party == null) {
            sendError(player, "You're not in a party!");
            return 0;
        }

        player.sendSystemMessage(Component.literal("\u00A76=== " + party.getName() + " ==="));

        for (UUID memberId : party.getMembers()) {
            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
            String status = member != null ? "\u00A7aOnline" : "\u00A77Offline";
            String name = member != null ? member.getName().getString() : memberId.toString().substring(0, 8);
            String leader = party.isLeader(memberId) ? " \u00A7e\u2605" : "";

            player.sendSystemMessage(Component.literal("\u00A77- \u00A7f" + name + leader + " " + status));
        }
        return 1;
    }

    private static int partyInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        Party party = service.getParty(player);
        if (party == null) {
            sendError(player, "You're not in a party!");
            return 0;
        }

        player.sendSystemMessage(Component.literal("\u00A76=== " + party.getName() + " ==="));
        player.sendSystemMessage(Component.literal("\u00A77Members: \u00A7f" + party.getMemberCount() + "/6"));
        player.sendSystemMessage(Component.literal("\u00A77XP Sharing: \u00A7f" +
                (party.getSettings().isXpShareEnabled() ? "Enabled" : "Disabled")));
        player.sendSystemMessage(Component.literal("\u00A77Share Range: \u00A7f" +
                (int) party.getSettings().getShareRange() + " blocks"));

        return 1;
    }

    private static int partyChat(CommandContext<CommandSourceStack> ctx, String message) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        if (service.sendPartyChat(player, message)) {
            return 1;
        }
        sendError(player, "You're not in a party!");
        return 0;
    }

    private static int disbandParty(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        PartyService.DisbandResult result = service.disbandParty(player);

        switch (result) {
            case SUCCESS -> {
                player.sendSystemMessage(Component.literal("\u00A7aParty disbanded."));
                return 1;
            }
            case NOT_IN_PARTY -> sendError(player, "You're not in a party!");
            case NOT_LEADER -> sendError(player, "Only the leader can disband the party!");
        }
        return 0;
    }

    private static int setXpShare(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        Party party = service.getParty(player);
        if (party == null) {
            sendError(player, "You're not in a party!");
            return 0;
        }

        if (!party.isLeader(player.getUUID())) {
            sendError(player, "Only the leader can change settings!");
            return 0;
        }

        party.getSettings().setXpShareEnabled(enabled);
        PartySavedData.get(player.getServer()).markDirty();

        player.sendSystemMessage(Component.literal("\u00A7aXP sharing " + (enabled ? "enabled" : "disabled") + "."));
        return 1;
    }

    private static int setShareRange(CommandContext<CommandSourceStack> ctx, int range) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PartyService service = PartyService.get(player.getServer());

        Party party = service.getParty(player);
        if (party == null) {
            sendError(player, "You're not in a party!");
            return 0;
        }

        if (!party.isLeader(player.getUUID())) {
            sendError(player, "Only the leader can change settings!");
            return 0;
        }

        party.getSettings().setShareRange(range);
        PartySavedData.get(player.getServer()).markDirty();

        player.sendSystemMessage(Component.literal("\u00A7aShare range set to " + range + " blocks."));
        return 1;
    }

    private static void sendError(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("\u00A7c" + message));
    }
}
