package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.common.api.skills.SkillType;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillClientHandler;
import com.etema.ragnarmmo.system.lifeskills.LifeSkillManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * HUD overlay for skill notifications.
 * - Life Skills: Shown on the RIGHT side (lateral)
 * - Combat Skills: Shown CENTER (legacy behavior)
 */
@Mod.EventBusSubscriber(modid = com.etema.ragnarmmo.RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SkillOverlay {

    // === Combat Skills (XP-based, centered) ===
    private static net.minecraft.resources.ResourceLocation lastSkill;
    private static int lastAmount;
    private static long showTime;
    private static final long DURATION = 3000;

    // Level up (combat skills)
    private static net.minecraft.resources.ResourceLocation lastLevelUpSkill;
    private static int lastLevelUpLevel;
    private static long levelUpShowTime;

    // === Life Skills (Points-based, lateral) ===
    private static net.minecraft.resources.ResourceLocation lastLifeSkill;
    private static int lastLifePoints;
    private static long lifePointsShowTime;
    private static final long LIFE_POINTS_DURATION = 2000;

    // Life skill level up
    private static net.minecraft.resources.ResourceLocation lastLifeLevelUpSkill;
    private static int lastLifeLevelUpLevel;
    private static long lifeLevelUpShowTime;
    private static final long LIFE_LEVEL_UP_DURATION = 5000;

    // Margins for lateral display
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 60;

    // === Public methods to trigger displays ===

    public static void showXpGain(net.minecraft.resources.ResourceLocation skillId, int amount) {
        // Check if it's a life skill - redirect to life skill display
        if (isLifeSkill(skillId)) {
            showLifePointsGain(skillId, amount);
            return;
        }
        lastSkill = skillId;
        lastAmount = amount;
        showTime = System.currentTimeMillis();
    }

    public static void showLevelUp(net.minecraft.resources.ResourceLocation skillId, int level) {
        // Check if it's a life skill
        if (isLifeSkill(skillId)) {
            showLifeLevelUp(skillId, level);
            return;
        }
        lastLevelUpSkill = skillId;
        lastLevelUpLevel = level;
        levelUpShowTime = System.currentTimeMillis();
    }

    public static void showLifePointsGain(net.minecraft.resources.ResourceLocation skillId, int points) {
        lastLifeSkill = skillId;
        lastLifePoints = points;
        lifePointsShowTime = System.currentTimeMillis();
    }

    public static void showLifeLevelUp(net.minecraft.resources.ResourceLocation skillId, int level) {
        lastLifeLevelUpSkill = skillId;
        lastLifeLevelUpLevel = level;
        lifeLevelUpShowTime = System.currentTimeMillis();
    }

    // === Overlay Registration ===

    public static final IGuiOverlay HUD_SKILL_XP = (ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight) -> {
        // Combat skills (centered)
        renderXpGain(graphics, screenWidth, screenHeight);
        renderLevelUp(graphics, screenWidth, screenHeight);

        // Life skills (lateral - right side)
        renderLifePointsGain(graphics, screenWidth, screenHeight);
        renderLifeLevelUp(graphics, screenWidth, screenHeight);
    };

    // === Combat Skills Rendering (Centered) ===

    private static void renderXpGain(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (lastSkill == null)
            return;
        if (isLifeSkill(lastSkill))
            return; // Skip life skills here

        long elapsed = System.currentTimeMillis() - showTime;
        if (elapsed > DURATION)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;

        float alpha = calculateAlpha(elapsed, DURATION);
        int x = screenWidth / 2;
        int y = screenHeight / 4;

        RenderSystem.enableBlend();
        int color = (int) (alpha * 255) << 24 | 0xFFFFFF;

        String displayName = getDisplayName(lastSkill);
        String text = String.format("+%d %s XP", lastAmount, displayName);
        int width = mc.font.width(text);

        graphics.drawString(mc.font, Component.literal(text), x - width / 2, y, color);
        RenderSystem.disableBlend();
    }

    private static void renderLevelUp(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (lastLevelUpSkill == null)
            return;
        if (isLifeSkill(lastLevelUpSkill))
            return;

        long elapsed = System.currentTimeMillis() - levelUpShowTime;
        long duration = DURATION + 2000;
        if (elapsed > duration)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;

        float alpha = calculateAlpha(elapsed, duration);
        int x = screenWidth / 2;
        int y = screenHeight / 2 - 20;

        RenderSystem.enableBlend();
        int color = (int) (alpha * 255) << 24 | 0xFFFF00;

        Component title = Component.literal("Level Up!").withStyle(
                net.minecraft.ChatFormatting.BOLD, net.minecraft.ChatFormatting.GOLD);
        String sub = String.format("%s: %d", getDisplayName(lastLevelUpSkill), lastLevelUpLevel);

        int w1 = mc.font.width(title);
        int w2 = mc.font.width(sub);

        graphics.drawString(mc.font, title, x - w1 / 2, y, color);
        graphics.drawString(mc.font, Component.literal(sub).withStyle(net.minecraft.ChatFormatting.YELLOW),
                x - w2 / 2, y + 12, color);
        RenderSystem.disableBlend();
    }

    // === Life Skills Rendering (Lateral - Right Side) ===

    private static void renderLifePointsGain(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (lastLifeSkill == null)
            return;

        long elapsed = System.currentTimeMillis() - lifePointsShowTime;
        if (elapsed > LIFE_POINTS_DURATION)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;

        float alpha = calculateAlpha(elapsed, LIFE_POINTS_DURATION);

        // Position: Right side, below hotbar area
        int y = MARGIN_TOP;

        RenderSystem.enableBlend();

        // Format: "⛏ +5 pts Mining" with icon
        String icon = getSkillIcon(lastLifeSkill);
        String text = String.format("%s +%d pts %s", icon, lastLifePoints, getDisplayName(lastLifeSkill));

        int textWidth = mc.font.width(text);
        int x = screenWidth - MARGIN_RIGHT - textWidth;

        // Background box for readability
        int bgColor = (int) (alpha * 180) << 24;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, bgColor);

        // Text color: greenish for points
        int textColor = (int) (alpha * 255) << 24 | 0x55FF55;
        graphics.drawString(mc.font, Component.literal(text), x, y, textColor);

        RenderSystem.disableBlend();
    }

    private static void renderLifeLevelUp(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (lastLifeLevelUpSkill == null)
            return;

        long elapsed = System.currentTimeMillis() - lifeLevelUpShowTime;
        if (elapsed > LIFE_LEVEL_UP_DURATION)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui)
            return;

        float alpha = calculateAlpha(elapsed, LIFE_LEVEL_UP_DURATION);

        // Position: Right side, higher than points display
        int y = MARGIN_TOP + 20;

        RenderSystem.enableBlend();

        // Format: "⛏ Mining ↑ Nivel 12"
        String icon = getSkillIcon(lastLifeLevelUpSkill);
        String text = String.format("%s %s \u2191 Level %d", icon, getDisplayName(lastLifeLevelUpSkill),
                lastLifeLevelUpLevel);

        int textWidth = mc.font.width(text);
        int x = screenWidth - MARGIN_RIGHT - textWidth;

        // Background box (gold tint for level up)
        int bgColor = (int) (alpha * 200) << 24 | 0x332200;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, bgColor);

        // Text color: gold for level up
        int textColor = (int) (alpha * 255) << 24 | 0xFFD700;
        graphics.drawString(mc.font, Component.literal(text), x, y, textColor);

        RenderSystem.disableBlend();
    }

    // === Helper Methods ===

    private static float calculateAlpha(long elapsed, long duration) {
        float alpha = 1.0f;
        long fadeStart = duration - 1000;
        if (elapsed > fadeStart) {
            alpha = 1.0f - (float) (elapsed - fadeStart) / 1000f;
        }
        return Math.max(0, Math.min(1, alpha));
    }

    private static boolean isLifeSkill(net.minecraft.resources.ResourceLocation skillId) {
        return com.etema.ragnarmmo.system.skills.data.SkillRegistry.get(skillId)
                .map(def -> def.getCategory() == com.etema.ragnarmmo.common.api.skills.SkillCategory.LIFE)
                .orElse(false);
    }

    private static String getDisplayName(net.minecraft.resources.ResourceLocation skillId) {
        return com.etema.ragnarmmo.system.skills.data.SkillRegistry.get(skillId)
                .map(com.etema.ragnarmmo.common.api.skills.ISkillDefinition::getDisplayName)
                .orElse(skillId.getPath());
    }

    private static String getSkillIcon(net.minecraft.resources.ResourceLocation skillId) {
        String path = skillId.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("mining"))
            return "\u26CF"; // Pick
        if (path.contains("woodcutting"))
            return "\uD83E\uDE93"; // Axe
        if (path.contains("excavation") || path.contains("digging"))
            return "\uD83E\uDEA3"; // Shovel
        if (path.contains("farming"))
            return "\uD83C\uDF3E"; // Wheat
        if (path.contains("fishing"))
            return "\uD83C\uDFA3"; // Fishing rod
        if (path.contains("exploration"))
            return "\uD83E\uDDED"; // Compass
        return "\u2B50"; // Star
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("skill_xp", HUD_SKILL_XP);
    }
}
