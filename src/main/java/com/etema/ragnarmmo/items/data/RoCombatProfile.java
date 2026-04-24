package com.etema.ragnarmmo.items.data;

import java.util.Locale;
import java.util.Set;

/**
 * Optional combat metadata for RO item rules.
 * Lets datapacks manually classify external weapons and expose combat stats
 * even when another mod uses non-vanilla item classes or custom attributes.
 */
public record RoCombatProfile(
        WeaponMode weaponMode,
        double atk,
        double matk,
        int aspd,
        double range,
        int drawTicks,
        float projectileVelocity,
        Set<String> atkAttributeIds,
        Set<String> matkAttributeIds,
        Set<String> aspdAttributeIds,
        Set<String> rangeAttributeIds
) {

    public static final RoCombatProfile EMPTY = new RoCombatProfile(
            WeaponMode.UNSPECIFIED,
            0.0D,
            0.0D,
            0,
            0.0D,
            0,
            0.0F,
            Set.of(),
            Set.of(),
            Set.of(),
            Set.of());

    public RoCombatProfile {
        weaponMode = weaponMode != null ? weaponMode : WeaponMode.UNSPECIFIED;
        atkAttributeIds = atkAttributeIds != null ? Set.copyOf(atkAttributeIds) : Set.of();
        matkAttributeIds = matkAttributeIds != null ? Set.copyOf(matkAttributeIds) : Set.of();
        aspdAttributeIds = aspdAttributeIds != null ? Set.copyOf(aspdAttributeIds) : Set.of();
        rangeAttributeIds = rangeAttributeIds != null ? Set.copyOf(rangeAttributeIds) : Set.of();
    }

    public boolean isRanged() {
        return weaponMode == WeaponMode.RANGED;
    }

    public boolean hasStatOverrides() {
        return atk > 0.0D
                || matk > 0.0D
                || aspd > 0
                || range > 0.0D
                || drawTicks > 0
                || projectileVelocity > 0.0F
                || !atkAttributeIds.isEmpty()
                || !matkAttributeIds.isEmpty()
                || !aspdAttributeIds.isEmpty()
                || !rangeAttributeIds.isEmpty();
    }

    public boolean isEmpty() {
        return weaponMode == WeaponMode.UNSPECIFIED && !hasStatOverrides();
    }

    public static RoCombatProfile merge(RoCombatProfile base, RoCombatProfile override) {
        if (base == null || base.isEmpty()) {
            return override != null ? override : EMPTY;
        }
        if (override == null || override.isEmpty()) {
            return base;
        }

        return new RoCombatProfile(
                override.weaponMode() != WeaponMode.UNSPECIFIED ? override.weaponMode() : base.weaponMode(),
                override.atk() > 0.0D ? override.atk() : base.atk(),
                override.matk() > 0.0D ? override.matk() : base.matk(),
                override.aspd() > 0 ? override.aspd() : base.aspd(),
                override.range() > 0.0D ? override.range() : base.range(),
                override.drawTicks() > 0 ? override.drawTicks() : base.drawTicks(),
                override.projectileVelocity() > 0.0F ? override.projectileVelocity() : base.projectileVelocity(),
                !override.atkAttributeIds().isEmpty() ? override.atkAttributeIds() : base.atkAttributeIds(),
                !override.matkAttributeIds().isEmpty() ? override.matkAttributeIds() : base.matkAttributeIds(),
                !override.aspdAttributeIds().isEmpty() ? override.aspdAttributeIds() : base.aspdAttributeIds(),
                !override.rangeAttributeIds().isEmpty() ? override.rangeAttributeIds() : base.rangeAttributeIds());
    }

    public enum WeaponMode {
        UNSPECIFIED,
        MELEE,
        RANGED;

        public static WeaponMode fromString(String raw) {
            if (raw == null || raw.isBlank()) {
                return UNSPECIFIED;
            }
            try {
                return valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return UNSPECIFIED;
            }
        }
    }
}
