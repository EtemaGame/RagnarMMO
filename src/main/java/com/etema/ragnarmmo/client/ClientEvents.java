package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.system.stats.RagnarStats;
import com.etema.ragnarmmo.client.ui.StatsScreen;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    private static boolean isCombatMode = false;

    public static boolean isCombatMode() {
        return isCombatMode;
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        // Toggle Combat Mode
        if (Keybinds.TOGGLE_COMBAT_MODE.consumeClick()) {
            isCombatMode = !isCombatMode;
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Combat Mode: " + (isCombatMode ? "§aON" : "§cOFF")),
                    true);
        }

        // Open Stats Screen (V)
        if (Keybinds.OPEN_STATS.consumeClick()) {
            if (mc.screen instanceof StatsScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new StatsScreen());
            }
        }

        // Open Skills Screen (K)
        if (Keybinds.OPEN_SKILLS.consumeClick()) {
            if (mc.screen instanceof com.etema.ragnarmmo.client.ui.SkillsScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new com.etema.ragnarmmo.client.ui.SkillsScreen(null));
            }
        }

        // Open Achievements Screen (Y)
        if (Keybinds.OPEN_ACHIEVEMENTS.consumeClick()) {
            if (mc.screen instanceof com.etema.ragnarmmo.client.ui.AchievementScreen) {
                mc.setScreen(null);
            } else if (mc.screen == null) {
                mc.setScreen(new com.etema.ragnarmmo.client.ui.AchievementScreen());
            }
        }

        // Skill Hotbar Keys (1-6)
        // If in Combat Mode and no screen is open, intercept keys 1-6 for skills
        // Also intercept 7-9 to prevent slot changes while in combat mode
        InputConstants.Key key = InputConstants.getKey(e.getKey(), e.getScanCode());
        int keyCode = key.getValue();
        boolean isNumberKey = keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9;

        if (isNumberKey && isCombatMode && mc.screen == null) {
            // In Forge 1.20.1, InputEvent.Key is NOT cancelable.
            // Instead of canceling, we "steal" the input by consuming the vanilla key mappings.
            int slotIndex = keyCode - GLFW.GLFW_KEY_1;
            if (slotIndex >= 0 && slotIndex < 9) {
                mc.options.keyHotbarSlots[slotIndex].consumeClick();
            }

            if (e.getAction() == GLFW.GLFW_PRESS) {
                if (slotIndex < 6) { // Only keys 1-6 cast skills
                    castSkill(mc, slotIndex);
                }
            }
            return;
        }

        // Handle skill assignment in Skills Screen or fallback casting
        for (int i = 0; i < Keybinds.HOTBAR_KEYS.length; i++) {
            if (Keybinds.HOTBAR_KEYS[i].isActiveAndMatches(key)) {
                if (e.getAction() == GLFW.GLFW_PRESS) {
                    if (mc.screen instanceof com.etema.ragnarmmo.client.ui.SkillsScreen) {
                        assignSkill(mc, i);
                    }
                }
            }
        }
    }

    private static void castSkill(Minecraft mc, int slot) {
        if (!isCombatMode) return; // double check

        com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider.get(mc.player).ifPresent(skills -> {
            String[] hotbar = skills.getHotbar();
            if (slot < hotbar.length) {
                String skillId = hotbar[slot];
                if (skillId != null && !skillId.isEmpty()) {
                    com.etema.ragnarmmo.common.net.Network.sendToServer(
                            new com.etema.ragnarmmo.skill.net.PacketUseSkill(skillId));
                }
            }
        });
    }

    private static void assignSkill(Minecraft mc, int i) {
        com.etema.ragnarmmo.client.ui.SkillsScreen skillsScreen = (com.etema.ragnarmmo.client.ui.SkillsScreen) mc.screen;
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        com.etema.ragnarmmo.client.ui.SkillTreeAdapter.SkillNodeWrapper hovered = skillsScreen.getHoveredSkill(mx, my);
        if (hovered != null) {
            if (hovered.getDefinition().isActive()) {
                com.etema.ragnarmmo.common.net.Network.sendToServer(
                        new com.etema.ragnarmmo.skill.net.PacketSetHotbarSlot(i,
                                hovered.getSkillId().toString()));
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.ragnarmmo.hotbar_set", (i + 1), hovered.getDefinition().getDisplayName()),
                        true);
            } else {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component
                                .translatable("message.ragnarmmo.passive_skill_assign_error")
                                .withStyle(net.minecraft.ChatFormatting.RED),
                        true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            com.etema.ragnarmmo.client.ClientCastManager.getInstance().tick();
        }
    }
}
