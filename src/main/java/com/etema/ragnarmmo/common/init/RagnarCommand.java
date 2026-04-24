package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.command.RagnarAdminCommands;
import com.etema.ragnarmmo.skill.job.merchant.CartCommands;
import com.etema.ragnarmmo.skill.job.acolyte.MemoCommands;
import com.etema.ragnarmmo.player.command.ExpCommands;
import com.etema.ragnarmmo.player.command.PartyCommands;
import com.etema.ragnarmmo.player.command.StatsCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RagnarCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main Command: /ragnar
        LiteralCommandNode<CommandSourceStack> ragnarNode = dispatcher.register(Commands.literal("ragnar")
                .then(StatsCommands.createNode())
                .then(CartCommands.createNode())
                .then(MemoCommands.createNode())
                .then(ExpCommands.createExpNode())
                .then(ExpCommands.createSetNode())
                .then(PartyCommands.createNode())
                .then(RagnarAdminCommands.createNode()));

        // Register root-level abbreviations (aliases)
        registerAlias(dispatcher, "stats", ragnarNode.getChild("stats"));
        registerAlias(dispatcher, "cart", ragnarNode.getChild("cart"));
        registerAlias(dispatcher, "party", ragnarNode.getChild("party"));
        registerAlias(dispatcher, "memo", ragnarNode.getChild("memo"));

        // Party chat shortcut: /pc <message> -> /ragnar party chat <message>
        CommandNode<CommandSourceStack> partyNode = ragnarNode.getChild("party");
        if (partyNode != null) {
            CommandNode<CommandSourceStack> chatNode = partyNode.getChild("chat");
            if (chatNode != null) {
                registerAlias(dispatcher, "pc", chatNode);
            }
        }
    }

    private static void registerAlias(
            CommandDispatcher<CommandSourceStack> dispatcher,
            String alias,
            CommandNode<CommandSourceStack> target) {
        if (target == null) {
            return;
        }
        dispatcher.register(Commands.literal(alias).redirect(target));
    }
}
