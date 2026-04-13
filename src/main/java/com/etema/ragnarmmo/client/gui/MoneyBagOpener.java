package com.etema.ragnarmmo.client.gui;

import com.etema.ragnarmmo.client.ui.MoneyBagScreen;
import net.minecraft.client.Minecraft;

public final class MoneyBagOpener {
    public static void open() {
        Minecraft.getInstance().setScreen(new MoneyBagScreen());
    }
}
