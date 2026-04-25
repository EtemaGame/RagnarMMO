package com.etema.ragnarmmo.client.hud;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.capability.MobProfileProvider;
import com.etema.ragnarmmo.mobs.capability.MobProfileState;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import net.minecraft.world.entity.LivingEntity;

import java.util.Locale;
import java.util.Optional;

public class MobProfileHudIntegration implements EntityStatResolver {

    private static final int DEFAULT_PRIMARY_COLOR = 0xFFFFFF;
    private static final int ELITE_PRIMARY_COLOR = 0xFFD966;
    private static final int BOSS_PRIMARY_COLOR = 0xFF7A7A;
    private static final int DEFAULT_SECONDARY_COLOR = 0xD0D0D0;
    private static final int DEFAULT_BAR_FRAME_COLOR = 0xAA202020;

    private static Optional<MobProfile> getCanonicalProfile(LivingEntity entity) {
        return MobProfileProvider.get(entity)
                .resolve()
                .filter(MobProfileState::isInitialized)
                .map(MobProfileState::profile);
    }

    @Override
    public String getDisplayName(LivingEntity e) {
        return e.getName().getString();
    }

    @Override
    public String getLevel(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfile::level)
                .filter(level -> level > 0)
                .map(String::valueOf)
                .orElse("");
    }

    @Override
    public String getRank(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfile::rank)
                .map(Enum::name)
                .orElse("");
    }

    @Override
    public String getClazz(LivingEntity e) {
        return "";
    }

    @Override
    public String getPrimaryLabelPrefix(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfile::rank)
                .map(rank -> switch (rank) {
                    case ELITE, MINI_BOSS -> "* ";
                    case BOSS, MVP -> "! ";
                    default -> "";
                })
                .orElse("");
    }

    @Override
    public String getSecondaryLabel(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfileHudIntegration::formatTaxonomyLine)
                .orElse("");
    }

    @Override
    public int getPrimaryLabelColor(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfile::rank)
                .map(MobProfileHudIntegration::colorForRank)
                .orElse(DEFAULT_PRIMARY_COLOR);
    }

    @Override
    public int getSecondaryLabelColor(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfileHudIntegration::colorForElement)
                .orElse(DEFAULT_SECONDARY_COLOR);
    }

    @Override
    public int getHealthBarFrameColor(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfile::rank)
                .map(MobProfileHudIntegration::colorForRank)
                .map(MobProfileHudIntegration::withAlphaFrame)
                .orElse(DEFAULT_BAR_FRAME_COLOR);
    }

    @Override
    public int getHealthBarAccentColor(LivingEntity e) {
        return getCanonicalProfile(e)
                .map(MobProfileHudIntegration::colorForElement)
                .map(MobProfileHudIntegration::withAlphaAccent)
                .orElse(0);
    }

    private static int colorForRank(MobRank rank) {
        return switch (rank) {
            case ELITE, MINI_BOSS -> ELITE_PRIMARY_COLOR;
            case BOSS, MVP -> BOSS_PRIMARY_COLOR;
            default -> DEFAULT_PRIMARY_COLOR;
        };
    }

    private static int colorForElement(MobProfile profile) {
        String element = profile.element();
        if (element == null || element.isBlank()) {
            return DEFAULT_SECONDARY_COLOR;
        }

        return switch (element.trim().toUpperCase(Locale.ROOT)) {
            case "FIRE" -> 0xFFB26B;
            case "WATER" -> 0x8EC9FF;
            case "WIND", "AIR" -> 0xA7E8B5;
            case "EARTH" -> 0xC7A772;
            case "HOLY" -> 0xF3E7A2;
            case "SHADOW", "DARK" -> 0xC3B1E1;
            case "GHOST" -> 0xC7C7F9;
            case "POISON" -> 0xA9D36E;
            case "UNDEAD" -> 0xB9C48B;
            default -> DEFAULT_SECONDARY_COLOR;
        };
    }

    private static int withAlphaFrame(int color) {
        return (0xCC << 24) | (color & 0x00FFFFFF);
    }

    private static int withAlphaAccent(int color) {
        return (0xE0 << 24) | (color & 0x00FFFFFF);
    }

    private static String formatTaxonomyLine(MobProfile profile) {
        return formatToken(profile.race()) + " / "
                + formatToken(profile.element()) + " / "
                + formatToken(profile.size());
    }

    private static String formatToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String normalized = raw.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(normalized.length());
        boolean capitalizeNext = true;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                out.append(c);
                continue;
            }
            out.append(capitalizeNext ? Character.toUpperCase(c) : c);
            capitalizeNext = false;
        }
        return out.toString();
    }
}
