package com.etema.ragnarmmo.mobs.capability;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
import com.etema.ragnarmmo.mobs.profile.MobTier;
import net.minecraft.nbt.CompoundTag;

public final class MobProfileState {
    private MobProfile profile = defaultProfile();
    private boolean initialized;

    public MobProfile profile() {
        return profile;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setProfile(MobProfile profile) {
        this.profile = profile == null ? defaultProfile() : profile;
        this.initialized = profile != null;
    }

    public void clearProfile() {
        this.profile = defaultProfile();
        this.initialized = false;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Initialized", initialized);
        tag.putInt("Level", profile.level());
        tag.putString("Rank", profile.rank().name());
        tag.putString("Tier", profile.tier().name());
        tag.putInt("MaxHp", profile.maxHp());
        tag.putInt("AtkMin", profile.atkMin());
        tag.putInt("AtkMax", profile.atkMax());
        tag.putInt("Def", profile.def());
        tag.putInt("MDef", profile.mdef());
        tag.putInt("Hit", profile.hit());
        tag.putInt("Flee", profile.flee());
        tag.putInt("Crit", profile.crit());
        tag.putInt("Aspd", profile.aspd());
        tag.putDouble("MoveSpeed", profile.moveSpeed());
        tag.putInt("BaseExp", profile.baseExp());
        tag.putInt("JobExp", profile.jobExp());
        tag.putString("Race", profile.race());
        tag.putString("Element", profile.element());
        tag.putString("Size", profile.size());
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        MobRank rank;
        try {
            rank = MobRank.valueOf(nbt.getString("Rank"));
        } catch (IllegalArgumentException ex) {
            rank = MobRank.NORMAL;
        }
        MobTier tier;
        try {
            tier = MobTier.valueOf(nbt.getString("Tier"));
        } catch (IllegalArgumentException ex) {
            tier = MobTier.fromRank(rank);
        }
        initialized = nbt.getBoolean("Initialized");
        int atkMin = Math.max(0, nbt.getInt("AtkMin"));
        int atkMax = Math.max(atkMin, nbt.getInt("AtkMax"));
        profile = new MobProfile(
                Math.max(1, nbt.getInt("Level")),
                rank,
                tier,
                Math.max(1, nbt.getInt("MaxHp")),
                atkMin,
                atkMax,
                Math.max(0, nbt.getInt("Def")),
                Math.max(0, nbt.getInt("MDef")),
                Math.max(0, nbt.getInt("Hit")),
                Math.max(0, nbt.getInt("Flee")),
                Math.max(0, nbt.getInt("Crit")),
                Math.max(1, nbt.getInt("Aspd")),
                Math.max(0.0001D, nbt.getDouble("MoveSpeed")),
                Math.max(0, nbt.getInt("BaseExp")),
                Math.max(0, nbt.getInt("JobExp")),
                readTokenOrDefault(nbt, "Race", "unknown"),
                readTokenOrDefault(nbt, "Element", "neutral"),
                readTokenOrDefault(nbt, "Size", "medium"));
    }

    private static String readTokenOrDefault(CompoundTag nbt, String key, String defaultValue) {
        String value = nbt.getString(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static MobProfile defaultProfile() {
        return new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 0, 0, 10, 5, 1, 150, 0.2D,
                1, 1, "unknown", "neutral", "medium");
    }
}
