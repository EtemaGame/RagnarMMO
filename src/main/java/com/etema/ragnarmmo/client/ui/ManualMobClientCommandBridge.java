package com.etema.ragnarmmo.client.ui;

import net.minecraft.client.Minecraft;

public final class ManualMobClientCommandBridge {

    private ManualMobClientCommandBridge() {
    }

    public static boolean sendCommand(String commandWithoutSlash) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) {
            return false;
        }
        return mc.player.connection.sendCommand(commandWithoutSlash);
    }
}
