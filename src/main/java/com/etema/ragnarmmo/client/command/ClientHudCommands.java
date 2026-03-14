package com.etema.ragnarmmo.client.command;

import com.etema.ragnarmmo.client.ui.HudOverlayConfigScreen;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side commands for HUD configuration.
 * Registers /ragnar hud command to open the HUD configuration screen.
 */
@OnlyIn(Dist.CLIENT)
public class ClientHudCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ragnar")
                .then(Commands.literal("hud")
                        .executes(ctx -> {
                            // Schedule screen opening on the main render thread
                            Minecraft.getInstance().tell(() ->
                                    Minecraft.getInstance().setScreen(new HudOverlayConfigScreen()));
                            return 1;
                        })));
    }
}
