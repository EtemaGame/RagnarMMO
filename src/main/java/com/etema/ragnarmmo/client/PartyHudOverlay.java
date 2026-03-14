package com.etema.ragnarmmo.client;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.system.stats.party.PartyClientData;
import com.etema.ragnarmmo.system.stats.party.net.PartyMemberData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Compact RO-style party HUD.
 * Shows only: Name / Lv / HP (SP/Mana reserved for future, not rendered here).
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = RagnarMMO.MODID)
public class PartyHudOverlay {

    private static final int MAX_MEMBERS = 6;

    // Layout (base/unscaled)
    private static final int OVERLAY_WIDTH = 112;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 3;
    private static final int NAME_LEVEL_GAP = 4;
    private static final int TEXT_BAR_GAP = 1;
    private static final int ROW_GAP = 1;
    private static final int HP_BAR_HEIGHT = 5;
    @SuppressWarnings("unused") // Future SP/Mana support (do not render without server data)
    private static final int SP_BAR_HEIGHT = 3;

    // Colors (fixed, RO-inspired)
    private static final int COLOR_BG = 0x40000000; // dark, low alpha
    private static final int COLOR_BAR_BG = 0xA0202020;
    private static final int COLOR_BAR_BG_OFFLINE = 0x60202020;
    private static final int COLOR_NAME_ONLINE = 0xFFFFFFFF;
    private static final int COLOR_NAME_OFFLINE = 0xFF888888;
    private static final int COLOR_LEVEL_ONLINE = 0xFFBFBFBF;
    private static final int COLOR_LEVEL_OFFLINE = 0xFF6F6F6F;

    // Cache: avoid per-frame string work
    private static List<CachedMember> cachedMembers = List.of();
    private static boolean cachedShowSelf = false;
    private static Font cachedFont = null;

    public static final IGuiOverlay PARTY_HUD = (ForgeGui gui, GuiGraphics graphics, float partialTick,
            int screenWidth, int screenHeight) -> render(graphics, screenWidth, screenHeight);

    private static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) return;

        if (!RagnarConfigs.CLIENT.partyHud.enabled.get()) return;
        if (!PartyClientData.hasParty()) {
            cachedMembers = List.of();
            return;
        }

        Font font = mc.font;
        Layout layout = Layout.get(font);
        List<CachedMember> members = getCachedMembers(mc, layout);
        if (members.isEmpty()) return;

        int visibleCount = Math.min(MAX_MEMBERS, members.size());
        int contentHeight = visibleCount * layout.entryHeight + Math.max(0, visibleCount - 1) * ROW_GAP;
        int overlayHeight = PADDING_Y * 2 + contentHeight;

        float scale = (float) (double) RagnarConfigs.CLIENT.partyHud.scale.get();
        scale = Mth.clamp(scale, 0.75f, 1.5f);

        int realWidth = (int) Math.ceil(OVERLAY_WIDTH * scale);
        int realHeight = (int) Math.ceil(overlayHeight * scale);

        int xOffset = RagnarConfigs.CLIENT.partyHud.xOffset.get();
        int yOffset = RagnarConfigs.CLIENT.partyHud.yOffset.get();

        RagnarConfigs.Client.HudAnchor anchor = RagnarConfigs.CLIENT.partyHud.anchor.get();
        int x = switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> xOffset;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - realWidth - xOffset;
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> yOffset;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - realHeight - yOffset;
        };

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        graphics.fill(0, 0, OVERLAY_WIDTH, overlayHeight, COLOR_BG);

        int currentY = PADDING_Y;
        for (int i = 0; i < visibleCount; i++) {
            renderMember(graphics, font, layout, members.get(i), PADDING_X, currentY);
            currentY += layout.entryHeight + ROW_GAP;
        }

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }

    private static void renderMember(GuiGraphics graphics, Font font, Layout layout, CachedMember member, int x,
            int y) {
        int nameColor = member.online ? COLOR_NAME_ONLINE : COLOR_NAME_OFFLINE;
        int levelColor = member.online ? COLOR_LEVEL_ONLINE : COLOR_LEVEL_OFFLINE;

        graphics.drawString(font, member.displayName, x, y, nameColor, false);
        graphics.drawString(font, member.levelText, x + layout.innerWidth - member.levelTextWidth, y, levelColor,
                false);

        // HP bar (SP/Mana reserved for future; do not render without server data)
        int barX = x;
        int barY = y + font.lineHeight + TEXT_BAR_GAP;
        int barBg = member.online ? COLOR_BAR_BG : COLOR_BAR_BG_OFFLINE;
        graphics.fill(barX, barY, barX + layout.innerWidth, barY + HP_BAR_HEIGHT, barBg);

        if (!member.online) return;
        float hp = Mth.clamp(member.hpProgress, 0f, 1f);
        int filled = Mth.clamp((int) Math.ceil(layout.innerWidth * hp), 0, layout.innerWidth);
        if (filled > 0) {
            graphics.fill(barX, barY, barX + filled, barY + HP_BAR_HEIGHT, getHpColor(hp));
        }
    }

    private static int getHpColor(float hpProgress) {
        // RO-style: green -> yellow -> red. Fixed palette, no user customization.
        float t = Mth.clamp(hpProgress, 0f, 1f);

        int a = 0xE0;
        int r;
        int g;
        int b = 0x00;

        if (t >= 0.5f) {
            // Yellow (255,255,0) to Green (0,255,0)
            float u = (t - 0.5f) / 0.5f;
            r = (int) Mth.lerp(u, 255f, 0f);
            g = 255;
        } else {
            // Red (255,0,0) to Yellow (255,255,0)
            float u = t / 0.5f;
            r = 255;
            g = (int) Mth.lerp(u, 0f, 255f);
        }

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static List<CachedMember> getCachedMembers(Minecraft mc, Layout layout) {
        boolean showSelf = RagnarConfigs.CLIENT.partyHud.showSelf.get();
        if (!PartyClientData.isDirty() && cachedFont == mc.font && cachedShowSelf == showSelf) {
            return cachedMembers;
        }

        cachedFont = mc.font;
        cachedShowSelf = showSelf;

        UUID selfId = mc.player != null ? mc.player.getUUID() : null;
        List<PartyMemberData> members = PartyClientData.getMembersSortedLeaderFirst();

        ArrayList<CachedMember> rebuilt = new ArrayList<>(Math.min(MAX_MEMBERS, members.size()));
        for (PartyMemberData member : members) {
            if (!showSelf && selfId != null && selfId.equals(member.uuid())) continue;

            String levelText = "Lv " + member.level();
            int levelTextWidth = mc.font.width(levelText);

            int maxNameWidth = layout.innerWidth - levelTextWidth - NAME_LEVEL_GAP;
            String displayName = (member.isLeader() ? ">" : "") + member.name();
            displayName = ellipsize(mc.font, displayName, Math.max(0, maxNameWidth));

            rebuilt.add(new CachedMember(displayName, levelText, levelTextWidth, member.getHealthProgress(),
                    member.isOnline()));

            if (rebuilt.size() >= MAX_MEMBERS) break;
        }

        cachedMembers = List.copyOf(rebuilt);
        PartyClientData.consumeDirty();
        return cachedMembers;
    }

    private static String ellipsize(Font font, String text, int maxWidth) {
        if (maxWidth <= 0 || text == null || text.isEmpty()) return "";
        if (font.width(text) <= maxWidth) return text;

        final String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return font.plainSubstrByWidth(text, maxWidth);
        }

        return font.plainSubstrByWidth(text, maxWidth - ellipsisWidth) + ellipsis;
    }

    private record CachedMember(
            String displayName,
            String levelText,
            int levelTextWidth,
            float hpProgress,
            boolean online
    ) {}

    private static final class Layout {
        final int entryHeight;
        final int innerWidth;

        private Layout(int entryHeight, int innerWidth) {
            this.entryHeight = entryHeight;
            this.innerWidth = innerWidth;
        }

        private static Layout cached;
        private static int cachedLineHeight = -1;

        static Layout get(Font font) {
            int lineHeight = font.lineHeight;
            if (cached == null || cachedLineHeight != lineHeight) {
                cachedLineHeight = lineHeight;
                int innerWidth = OVERLAY_WIDTH - PADDING_X * 2;
                int entryHeight = lineHeight + TEXT_BAR_GAP + HP_BAR_HEIGHT;
                cached = new Layout(entryHeight, innerWidth);
            }
            return cached;
        }
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("party_hud", PARTY_HUD);
    }
}
