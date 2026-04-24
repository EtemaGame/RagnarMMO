package com.etema.ragnarmmo.mobs.capability;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.mobs.profile.MobProfile;
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

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Initialized", initialized);
        tag.putInt("Level", profile.level());
        tag.putString("Rank", profile.rank().name());
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
        initialized = nbt.getBoolean("Initialized");
        profile = new MobProfile(
                Math.max(1, nbt.getInt("Level")),
                rank,
                Math.max(1, nbt.getInt("MaxHp")),
                Math.max(0, nbt.getInt("AtkMin")),
                Math.max(0, nbt.getInt("AtkMax")),
                Math.max(0, nbt.getInt("Def")),
                Math.max(0, nbt.getInt("MDef")),
                Math.max(0, nbt.getInt("Hit")),
                Math.max(0, nbt.getInt("Flee")),
                Math.max(0, nbt.getInt("Crit")),
                Math.max(0, nbt.getInt("Aspd")),
                nbt.getDouble("MoveSpeed"),
                readToken(nbt, "Race", "unknown"),
                readToken(nbt, "Element", "neutral"),
                readToken(nbt, "Size", "medium"));
    }

    private static String readToken(CompoundTag nbt, String key, String fallbackValue) {
        String value = nbt.getString(key);
        return value == null || value.isBlank() ? fallbackValue : value;
    }

    public static MobProfile defaultProfile() {
        return new MobProfile(1, MobRank.NORMAL, 20, 2, 4, 0, 0, 10, 5, 1, 150, 0.2D, "unknown", "neutral",
                "medium");
    }
}
