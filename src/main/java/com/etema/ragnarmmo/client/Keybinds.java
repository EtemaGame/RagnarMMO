package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.system.stats.RagnarStats;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Keybinds {
    private static final String CATEGORY = "key.categories.ragnarmmo";

    public static KeyMapping OPEN_STATS = new KeyMapping("key.ragnarmmo.open", GLFW.GLFW_KEY_V,
            CATEGORY);

    public static KeyMapping OPEN_SKILLS = new KeyMapping("key.ragnarmmo.open_skills", GLFW.GLFW_KEY_K,
            CATEGORY);

    public static KeyMapping OPEN_ACHIEVEMENTS = new KeyMapping("key.ragnarmmo.open_achievements", GLFW.GLFW_KEY_Y,
            CATEGORY);

    public static KeyMapping TOGGLE_COMBAT_MODE = new KeyMapping("key.ragnarmmo.toggle_combat",
            GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    // Skill hotbar keys — 1 to 6 (intercepted in combat mode)
    public static final KeyMapping[] HOTBAR_KEYS = new KeyMapping[6];

    static {
        for (int i = 0; i < 6; i++) {
            HOTBAR_KEYS[i] = new KeyMapping("key.ragnarmmo.skill_hotbar." + (i + 1),
                    GLFW.GLFW_KEY_1 + i, CATEGORY);
        }
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(OPEN_STATS);
        e.register(OPEN_SKILLS);
        e.register(OPEN_ACHIEVEMENTS);
        e.register(TOGGLE_COMBAT_MODE);
        for (KeyMapping key : HOTBAR_KEYS) {
            e.register(key);
        }
    }
}
