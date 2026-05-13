package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.common.command.RagnarAdminCommands;
import com.etema.ragnarmmo.skills.job.merchant.CartCommands;
import com.etema.ragnarmmo.skills.job.acolyte.MemoCommands;
import com.etema.ragnarmmo.player.command.ExpCommands;
import com.etema.ragnarmmo.player.command.PartyCommands;
import com.etema.ragnarmmo.player.command.StatsCommands;
import com.etema.ragnarmmo.skills.runtime.SkillCommands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RagnarCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> rNode = dispatcher.register(Commands.literal("r")
                .then(StatsCommands.createNode())
                .then(SkillCommands.createNode())
                .then(CartCommands.createNode())
                .then(MemoCommands.createNode())
                .then(ExpCommands.createLevelNode())
                .then(ExpCommands.createJobNode())
                .then(ExpCommands.createExpNode())
                .then(ExpCommands.createSetNode())
                .then(PartyCommands.createNode())
                .then(RagnarAdminCommands.createDebugNode())
                .then(RagnarAdminCommands.createResetNode())
                .then(RagnarAdminCommands.createUnlockNode())
                .then(RagnarAdminCommands.createNode()));

        registerAlias(dispatcher, "ragnar", rNode);
        registerAlias(dispatcher, "stats", rNode.getChild("stats"));
        registerAlias(dispatcher, "skills", rNode.getChild("skills"));
        registerAlias(dispatcher, "cart", rNode.getChild("cart"));
        registerAlias(dispatcher, "party", rNode.getChild("party"));
        registerAlias(dispatcher, "memo", rNode.getChild("memo"));
        registerAlias(dispatcher, "lv", rNode.getChild("lv"));
        registerAlias(dispatcher, "job", rNode.getChild("job"));
        registerAlias(dispatcher, "exp", rNode.getChild("exp"));
        registerAlias(dispatcher, "set", rNode.getChild("set"));
        registerAlias(dispatcher, "unlock", rNode.getChild("unlock"));
        registerAlias(dispatcher, "reset", rNode.getChild("reset"));
        registerAlias(dispatcher, "debug", rNode.getChild("debug"));
        registerAlias(dispatcher, "admin", rNode.getChild("admin"));

        CommandNode<CommandSourceStack> partyNode = rNode.getChild("party");
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
