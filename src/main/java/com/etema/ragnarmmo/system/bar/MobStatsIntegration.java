package com.etema.ragnarmmo.system.bar;

import com.etema.ragnarmmo.client.MobClientCoexistenceReader;
import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.query.MobClientCoexistenceView;
import com.etema.ragnarmmo.system.mobstats.integration.MobInfoIntegration;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Client-side mob stat resolver for the coexistence period.
 *
 * <p>This resolver prefers the minimal synced new-source projection when present and falls back to
 * legacy client mob info only when needed.</p>
 */
public class MobStatsIntegration implements EntityStatResolver {

    private static final int DEFAULT_PRIMARY_COLOR = 0xFFFFFF;
    private static final int ELITE_PRIMARY_COLOR = 0xFFD966;
    private static final int BOSS_PRIMARY_COLOR = 0xFF7A7A;
    private static final int DEFAULT_SECONDARY_COLOR = 0xD0D0D0;

    private static Optional<MobClientCoexistenceView> getSyncedView(LivingEntity entity) {
        return MobClientCoexistenceReader.get(entity);
    }

    @Override
    public String getDisplayName(LivingEntity e) {
        return e.getName().getString();
    }

    @Override
    public String getLevel(LivingEntity e) {
        var syncedView = getSyncedView(e);
        if (syncedView.isPresent()) {
            return String.valueOf(syncedView.get().level());
        }

        var levelOpt = MobInfoIntegration.getMobLevel(e);
        if (levelOpt.isPresent()) {
            int lvl = levelOpt.getAsInt();
            if (lvl > 0) {
                return String.valueOf(lvl);
            }
        }

        OptionalInt persistedLevel = readPersistentMobLevel(e);
        if (persistedLevel.isPresent() && persistedLevel.getAsInt() > 0) {
            return String.valueOf(persistedLevel.getAsInt());
        }

        return "";
    }

    @Override
    public String getRank(LivingEntity e) {
        var syncedView = getSyncedView(e);
        if (syncedView.isPresent()) {
            return syncedView.get().rank().name();
        }

        return MobInfoIntegration.getLegacyCompatibilityRank(e)
                .map(Enum::name)
                .orElse("");
    }

    @Override
    public String getClazz(LivingEntity e) {
        return "";
    }

    @Override
    public String getSecondaryLabel(LivingEntity e) {
        return getSyncedView(e)
                .map(MobStatsIntegration::formatTaxonomyLine)
                .orElse("");
    }

    @Override
    public int getPrimaryLabelColor(LivingEntity e) {
        return resolveCompatibilityRank(e)
                .map(MobStatsIntegration::colorForRank)
                .orElse(DEFAULT_PRIMARY_COLOR);
    }

    @Override
    public int getSecondaryLabelColor(LivingEntity e) {
        return getSyncedView(e)
                .map(MobStatsIntegration::colorForElement)
                .orElse(DEFAULT_SECONDARY_COLOR);
    }

    private static Optional<MobRank> resolveCompatibilityRank(LivingEntity entity) {
        var syncedView = getSyncedView(entity);
        if (syncedView.isPresent()) {
            return Optional.of(syncedView.get().rank());
        }
        return MobInfoIntegration.getLegacyCompatibilityRank(entity);
    }

    private static int colorForRank(MobRank rank) {
        return switch (rank) {
            case ELITE -> ELITE_PRIMARY_COLOR;
            case BOSS -> BOSS_PRIMARY_COLOR;
            default -> DEFAULT_PRIMARY_COLOR;
        };
    }

    private static int colorForElement(MobClientCoexistenceView view) {
        String element = view.element();
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

    private static String formatTaxonomyLine(MobClientCoexistenceView view) {
        return formatToken(view.race()) + " / "
                + formatToken(view.element()) + " / "
                + formatToken(view.size());
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

    private static OptionalInt readPersistentMobLevel(LivingEntity entity) {
        return getMobStatsTag(entity)
                .filter(tag -> tag.contains("Level", Tag.TAG_INT))
                .map(tag -> OptionalInt.of(Math.max(0, tag.getInt("Level"))))
                .orElse(OptionalInt.empty());
    }

    private static Optional<CompoundTag> getMobStatsTag(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        CompoundTag data = entity.getPersistentData();
        if (data == null || !data.contains("RagnarMobStats", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        return Optional.of(data.getCompound("RagnarMobStats"));
    }
}






