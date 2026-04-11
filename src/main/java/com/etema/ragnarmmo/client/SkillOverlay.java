package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.client.hud.HudConfigSerializer;
import com.etema.ragnarmmo.client.hud.HudLayoutManager;
import com.etema.ragnarmmo.client.hud.HudWidgetState;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SkillOverlay {

    private static ResourceLocation lastSkill;
    private static int lastAmount;
    private static long showTime;
    private static final long DURATION = 3000L;

    private static ResourceLocation lastLevelUpSkill;
    private static int lastLevelUpLevel;
    private static long levelUpShowTime;

    private static ResourceLocation lastLifeSkill;
    private static int lastLifePoints;
    private static long lifePointsShowTime;
    private static final long LIFE_POINTS_DURATION = 2000L;

    private static ResourceLocation lastLifeLevelUpSkill;
    private static int lastLifeLevelUpLevel;
    private static long lifeLevelUpShowTime;
    private static final long LIFE_LEVEL_UP_DURATION = 5000L;

    private static final int NOTIFICATION_WIDTH = 190;
    private static final int NOTIFICATION_HEIGHT = 58;

    public static void showXpGain(ResourceLocation skillId, int amount) {
        if (isLifeSkill(skillId)) {
            showLifePointsGain(skillId, amount);
            return;
        }
        lastSkill = skillId;
        lastAmount = amount;
        showTime = System.currentTimeMillis();
    }

    public static void showLevelUp(ResourceLocation skillId, int level) {
        if (isLifeSkill(skillId)) {
            showLifeLevelUp(skillId, level);
            return;
        }
        lastLevelUpSkill = skillId;
        lastLevelUpLevel = level;
        levelUpShowTime = System.currentTimeMillis();
    }

    public static void showLifePointsGain(ResourceLocation skillId, int points) {
        lastLifeSkill = skillId;
        lastLifePoints = points;
        lifePointsShowTime = System.currentTimeMillis();
    }

    public static void showLifeLevelUp(ResourceLocation skillId, int level) {
        lastLifeLevelUpSkill = skillId;
        lastLifeLevelUpLevel = level;
        lifeLevelUpShowTime = System.currentTimeMillis();
    }

    public static final IGuiOverlay HUD_SKILL_XP = (ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null
                || mc.options.hideGui
                || !RagnarConfigs.CLIENT.hud.enabled.get()
                || !RagnarConfigs.CLIENT.hud.notifications.enabled.get()
                || !hasVisibleNotification()) {
            return;
        }

        HudWidgetState state = HudConfigSerializer.read(RagnarConfigs.CLIENT.hud.notifications);
        HudLayoutManager.HudBounds bounds = HudLayoutManager.bounds(
                state, getWidth(), getHeight(), screenWidth, screenHeight);

        RenderSystem.enableBlend();
        HudLayoutManager.renderBackground(graphics, state, bounds);
        HudLayoutManager.pushWidgetTransform(graphics, bounds);
        renderXpGain(graphics);
        renderLevelUp(graphics);
        renderLifePointsGain(graphics);
        renderLifeLevelUp(graphics);
        HudLayoutManager.popWidgetTransform(graphics);
        RenderSystem.disableBlend();
    };

    public static int getWidth() {
        return NOTIFICATION_WIDTH;
    }

    public static int getHeight() {
        return NOTIFICATION_HEIGHT;
    }

    public static int renderPreview(GuiGraphics graphics, Font font) {
        Component title = Component.translatable("screen.ragnarmmo.notifications.level_up")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
        int titleWidth = font.width(title);
        graphics.drawString(font, title, (NOTIFICATION_WIDTH - titleWidth) / 2, 0, 0xFFFFD700, true);

        Component xp = Component.translatable("screen.ragnarmmo.notifications.skill_xp", 32, "Bash");
        int xpWidth = font.width(xp);
        graphics.drawString(font, xp, (NOTIFICATION_WIDTH - xpWidth) / 2, 14, 0xFFFFFFFF, true);

        Component life = Component.translatable("screen.ragnarmmo.notifications.life_points", "*", 5, "Mining");
        int lifeWidth = font.width(life);
        graphics.drawString(font, life, NOTIFICATION_WIDTH - lifeWidth, 32, 0xFF55FF55, true);
        return NOTIFICATION_HEIGHT;
    }

    private static void renderXpGain(GuiGraphics graphics) {
        if (lastSkill == null || isLifeSkill(lastSkill)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - showTime;
        if (elapsed > DURATION) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float alpha = calculateAlpha(elapsed, DURATION);
        int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
        Component text = Component.translatable(
                "screen.ragnarmmo.notifications.skill_xp", lastAmount, getDisplayName(lastSkill));
        int width = mc.font.width(text);
        graphics.drawString(mc.font, text, (NOTIFICATION_WIDTH - width) / 2, 0, color);
    }

    private static void renderLevelUp(GuiGraphics graphics) {
        if (lastLevelUpSkill == null || isLifeSkill(lastLevelUpSkill)) {
            return;
        }

        long elapsed = System.currentTimeMillis() - levelUpShowTime;
        long duration = DURATION + 2000L;
        if (elapsed > duration) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float alpha = calculateAlpha(elapsed, duration);
        int color = ((int) (alpha * 255) << 24) | 0xFFFF00;

        Component title = Component.translatable("screen.ragnarmmo.notifications.level_up")
                .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
        Component sub = Component.translatable(
                "screen.ragnarmmo.notifications.skill_level", getDisplayName(lastLevelUpSkill), lastLevelUpLevel)
                .withStyle(ChatFormatting.YELLOW);

        int titleWidth = mc.font.width(title);
        int subWidth = mc.font.width(sub);
        graphics.drawString(mc.font, title, (NOTIFICATION_WIDTH - titleWidth) / 2, 12, color);
        graphics.drawString(mc.font, sub, (NOTIFICATION_WIDTH - subWidth) / 2, 24, color);
    }

    private static void renderLifePointsGain(GuiGraphics graphics) {
        if (lastLifeSkill == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - lifePointsShowTime;
        if (elapsed > LIFE_POINTS_DURATION) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float alpha = calculateAlpha(elapsed, LIFE_POINTS_DURATION);
        int y = 34;
        Component text = Component.translatable(
                "screen.ragnarmmo.notifications.life_points",
                getSkillIcon(lastLifeSkill),
                lastLifePoints,
                getDisplayName(lastLifeSkill));

        int textWidth = mc.font.width(text);
        int x = NOTIFICATION_WIDTH - textWidth;
        int bgColor = (int) (alpha * 180) << 24;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, bgColor);

        int textColor = ((int) (alpha * 255) << 24) | 0x55FF55;
        graphics.drawString(mc.font, text, x, y, textColor);
    }

    private static void renderLifeLevelUp(GuiGraphics graphics) {
        if (lastLifeLevelUpSkill == null) {
            return;
        }

        long elapsed = System.currentTimeMillis() - lifeLevelUpShowTime;
        if (elapsed > LIFE_LEVEL_UP_DURATION) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        float alpha = calculateAlpha(elapsed, LIFE_LEVEL_UP_DURATION);
        int y = 46;
        Component text = Component.translatable(
                "screen.ragnarmmo.notifications.life_level",
                getSkillIcon(lastLifeLevelUpSkill),
                getDisplayName(lastLifeLevelUpSkill),
                lastLifeLevelUpLevel);

        int textWidth = mc.font.width(text);
        int x = NOTIFICATION_WIDTH - textWidth;
        int bgColor = ((int) (alpha * 200) << 24) | 0x332200;
        graphics.fill(x - 4, y - 2, x + textWidth + 4, y + 10, bgColor);

        int textColor = ((int) (alpha * 255) << 24) | 0xFFD700;
        graphics.drawString(mc.font, text, x, y, textColor);
    }

    private static boolean hasVisibleNotification() {
        long now = System.currentTimeMillis();
        return (lastSkill != null && !isLifeSkill(lastSkill) && now - showTime <= DURATION)
                || (lastLevelUpSkill != null && !isLifeSkill(lastLevelUpSkill) && now - levelUpShowTime <= DURATION + 2000L)
                || (lastLifeSkill != null && now - lifePointsShowTime <= LIFE_POINTS_DURATION)
                || (lastLifeLevelUpSkill != null && now - lifeLevelUpShowTime <= LIFE_LEVEL_UP_DURATION);
    }

    private static float calculateAlpha(long elapsed, long duration) {
        float alpha = 1.0F;
        long fadeStart = duration - 1000L;
        if (elapsed > fadeStart) {
            alpha = 1.0F - (float) (elapsed - fadeStart) / 1000.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, alpha));
    }

    private static boolean isLifeSkill(ResourceLocation skillId) {
        return com.etema.ragnarmmo.skill.data.SkillRegistry.get(skillId)
                .map(def -> def.getCategory() == com.etema.ragnarmmo.skill.api.SkillCategory.LIFE)
                .orElse(false);
    }

    private static Component getDisplayName(ResourceLocation skillId) {
        return com.etema.ragnarmmo.skill.data.SkillRegistry.get(skillId)
                .map(com.etema.ragnarmmo.skill.api.ISkillDefinition::getTranslatedName)
                .orElseGet(() -> Component.literal(skillId.getPath()));
    }

    private static String getSkillIcon(ResourceLocation skillId) {
        String path = skillId.getPath().toLowerCase(java.util.Locale.ROOT);
        if (path.contains("mining")) {
            return "\u26CF";
        }
        if (path.contains("woodcutting")) {
            return "Axe";
        }
        if (path.contains("excavation") || path.contains("digging")) {
            return "Shovel";
        }
        if (path.contains("farming")) {
            return "Wheat";
        }
        if (path.contains("fishing")) {
            return "Fishing";
        }
        if (path.contains("exploration")) {
            return "Explore";
        }
        return "*";
    }
}
