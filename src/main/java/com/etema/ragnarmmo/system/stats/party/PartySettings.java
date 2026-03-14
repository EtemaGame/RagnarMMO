package com.etema.ragnarmmo.system.stats.party;

import net.minecraft.nbt.CompoundTag;

/**
 * Configurable settings for a party.
 * These settings are persisted along with the party.
 */
public class PartySettings {

    private boolean xpShareEnabled;
    private double shareRange;

    // Defaults
    private static final boolean DEFAULT_XP_SHARE_ENABLED = true;
    private static final double DEFAULT_SHARE_RANGE = 50.0;

    public PartySettings() {
        this.xpShareEnabled = DEFAULT_XP_SHARE_ENABLED;
        this.shareRange = DEFAULT_SHARE_RANGE;
    }

    private PartySettings(boolean xpShareEnabled, double shareRange) {
        this.xpShareEnabled = xpShareEnabled;
        this.shareRange = shareRange;
    }

    // === Getters and Setters ===

    public boolean isXpShareEnabled() {
        return xpShareEnabled;
    }

    public void setXpShareEnabled(boolean enabled) {
        this.xpShareEnabled = enabled;
    }

    public double getShareRange() {
        return shareRange;
    }

    public void setShareRange(double range) {
        this.shareRange = Math.max(0, Math.min(200, range)); // Clamp 0-200 blocks
    }

    // === NBT Serialization ===

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("xpShareEnabled", xpShareEnabled);
        tag.putDouble("shareRange", shareRange);
        return tag;
    }

    public static PartySettings load(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return new PartySettings();
        }

        boolean xpShareEnabled = tag.contains("xpShareEnabled")
            ? tag.getBoolean("xpShareEnabled")
            : DEFAULT_XP_SHARE_ENABLED;
        double shareRange = tag.contains("shareRange")
            ? tag.getDouble("shareRange")
            : DEFAULT_SHARE_RANGE;

        return new PartySettings(xpShareEnabled, shareRange);
    }

    @Override
    public String toString() {
        return String.format("PartySettings[xpShare=%s, range=%.1f]", xpShareEnabled, shareRange);
    }
}
