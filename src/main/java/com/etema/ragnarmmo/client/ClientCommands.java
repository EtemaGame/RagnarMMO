package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.client.ui.HudOverlayConfigScreen;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ClientCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> rNode = Commands.literal("r")
                .then(Commands.literal("hud")
                        .executes(context -> openHudConfig(context.getSource())));

        var root = dispatcher.register(rNode);
        dispatcher.register(Commands.literal("ragnar").redirect(root));
        dispatcher.register(Commands.literal("hud").redirect(root.getChild("hud")));
    }

    private static int openHudConfig(CommandSourceStack source) {
        if (!RagnarConfigs.CLIENT.hud.enabled.get()) {
            source.sendFailure(Component.translatable("commands.ragnarmmo.hud.disabled"));
            return 0;
        }

        // We must delay the screen opening to the client thread main loop,
        // as commands might run in different contexts.
        Minecraft.getInstance().tell(() -> {
            Minecraft.getInstance().setScreen(new HudOverlayConfigScreen());
        });

        return 1;
    }
}
