package com.etema.ragnarmmo.common.api.mobs.runtime.manual;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;

public record InternalManualMobEntry(
        ResourceLocation entityTypeId,
        boolean enabled,
        int level,
        MobRank rank,
        String race,
        String element,
        String size,
        int maxHp,
        int atkMin,
        int atkMax,
        int def,
        int mdef,
        int hit,
        int flee,
        int crit,
        int aspd,
        double moveSpeed,
        String notes,
        String lastEditedBy,
        long lastEditedAt) {

    public InternalManualMobEntry {
        Objects.requireNonNull(entityTypeId, "entityTypeId");
        Objects.requireNonNull(rank, "rank");
        race = sanitizeText(race, "demi_human");
        element = sanitizeText(element, "neutral");
        size = sanitizeText(size, "medium");
        notes = notes == null ? "" : notes;
        lastEditedBy = lastEditedBy == null ? "system" : lastEditedBy;
    }

    public static InternalManualMobEntry createDefault(ResourceLocation entityTypeId, String editorName) {
        return new InternalManualMobEntry(
                entityTypeId,
                true,
                1,
                MobRank.NORMAL,
                "demi_human",
                "neutral",
                "medium",
                50,
                5,
                8,
                0,
                0,
                10,
                5,
                1,
                140,
                0.23D,
                "",
                editorName == null ? "system" : editorName,
                System.currentTimeMillis());
    }

    public ComputedMobProfile toComputedMobProfile() {
        return new ComputedMobProfile(
                Math.max(1, level),
                rank,
                Math.max(1, maxHp),
                Math.max(0, atkMin),
                Math.max(Math.max(0, atkMin), atkMax),
                Math.max(0, def),
                Math.max(0, mdef),
                Math.max(0, hit),
                Math.max(0, flee),
                Math.max(0, crit),
                Math.max(1, aspd),
                Math.max(0.01D, moveSpeed),
                null,
                race,
                element,
                size);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("entityTypeId", entityTypeId.toString());
        tag.putBoolean("enabled", enabled);
        tag.putInt("level", level);
        tag.putString("rank", rank.name());
        tag.putString("race", race);
        tag.putString("element", element);
        tag.putString("size", size);
        tag.putInt("maxHp", maxHp);
        tag.putInt("atkMin", atkMin);
        tag.putInt("atkMax", atkMax);
        tag.putInt("def", def);
        tag.putInt("mdef", mdef);
        tag.putInt("hit", hit);
        tag.putInt("flee", flee);
        tag.putInt("crit", crit);
        tag.putInt("aspd", aspd);
        tag.putDouble("moveSpeed", moveSpeed);
        tag.putString("notes", notes);
        tag.putString("lastEditedBy", lastEditedBy);
        tag.putLong("lastEditedAt", lastEditedAt);
        return tag;
    }

    public static InternalManualMobEntry load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("entityTypeId"));
        if (id == null) {
            throw new IllegalArgumentException("Invalid or missing entityTypeId");
        }
        MobRank rank = parseRank(tag.getString("rank"));
        return new InternalManualMobEntry(
                id,
                tag.getBoolean("enabled"),
                tag.getInt("level"),
                rank,
                tag.getString("race"),
                tag.getString("element"),
                tag.getString("size"),
                tag.getInt("maxHp"),
                tag.getInt("atkMin"),
                tag.getInt("atkMax"),
                tag.getInt("def"),
                tag.getInt("mdef"),
                tag.getInt("hit"),
                tag.getInt("flee"),
                tag.getInt("crit"),
                tag.getInt("aspd"),
                tag.getDouble("moveSpeed"),
                tag.getString("notes"),
                tag.getString("lastEditedBy"),
                tag.getLong("lastEditedAt"));
    }

    private static MobRank parseRank(String rankName) {
        if (rankName == null || rankName.isBlank()) {
            return MobRank.NORMAL;
        }
        try {
            return MobRank.valueOf(rankName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return MobRank.NORMAL;
        }
    }

    private static String sanitizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
