package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.common.command.RagnarAdminCommands;
import com.etema.ragnarmmo.system.skills.CartCommands;
import com.etema.ragnarmmo.system.stats.commands.ExpCommands;
import com.etema.ragnarmmo.system.stats.commands.StatsCommands;
import com.etema.ragnarmmo.system.stats.party.PartyCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RagnarCommand {

        public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
                // Main Command: /ragnar
                CommandNode<CommandSourceStack> ragnarNode = dispatcher.register(Commands.literal("ragnar")
                                .then(StatsCommands.createNode())
                                .then(CartCommands.createNode())
                                .then(ExpCommands.createExpNode())
                                .then(ExpCommands.createSetNode())
                                .then(PartyCommands.createNode())
                                .then(RagnarAdminCommands.createNode()));

                if (RagnarConfigs.SERVER.commands.enableLegacyAliases.get()) {
                        registerAlias(dispatcher, "stats", ragnarNode.getChild("stats"));
                        registerAlias(dispatcher, "cart", ragnarNode.getChild("cart"));
                        registerAlias(dispatcher, "party", ragnarNode.getChild("party"));
                        // Party chat shortcut: /pc <message> -> /ragnar party chat <message>
                        CommandNode<CommandSourceStack> partyNode = ragnarNode.getChild("party");
                        if (partyNode != null) {
                                registerAlias(dispatcher, "pc", partyNode.getChild("chat"));
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
