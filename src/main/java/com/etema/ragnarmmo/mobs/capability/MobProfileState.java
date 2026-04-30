package com.etema.ragnarmmo.mobs.capability;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.stats.RoBaseStats;
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
        tag.putInt("BaseStr", profile.baseStats().str());
        tag.putInt("BaseAgi", profile.baseStats().agi());
        tag.putInt("BaseVit", profile.baseStats().vit());
        tag.putInt("BaseInt", profile.baseStats().intel());
        tag.putInt("BaseDex", profile.baseStats().dex());
        tag.putInt("BaseLuk", profile.baseStats().luk());
        tag.putInt("MaxHp", profile.maxHp());
        tag.putInt("AtkMin", profile.atkMin());
        tag.putInt("AtkMax", profile.atkMax());
        tag.putInt("MatkMin", profile.matkMin());
        tag.putInt("MatkMax", profile.matkMax());
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
        int matkMin = Math.max(0, nbt.getInt("MatkMin"));
        int matkMax = Math.max(matkMin, nbt.getInt("MatkMax"));
        profile = new MobProfile(
                Math.max(1, nbt.getInt("Level")),
                rank,
                tier,
                readBaseStats(nbt),
                Math.max(1, nbt.getInt("MaxHp")),
                atkMin,
                atkMax,
                matkMin,
                matkMax,
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

    private static RoBaseStats readBaseStats(CompoundTag nbt) {
        if (!nbt.contains("BaseStr")
                && !nbt.contains("BaseAgi")
                && !nbt.contains("BaseVit")
                && !nbt.contains("BaseInt")
                && !nbt.contains("BaseDex")
                && !nbt.contains("BaseLuk")) {
            return RoBaseStats.novice();
        }
        return new RoBaseStats(
                nbt.getInt("BaseStr"),
                nbt.getInt("BaseAgi"),
                nbt.getInt("BaseVit"),
                nbt.getInt("BaseInt"),
                nbt.getInt("BaseDex"),
                nbt.getInt("BaseLuk"));
    }

    private static String readTokenOrDefault(CompoundTag nbt, String key, String defaultValue) {
        String value = nbt.getString(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static MobProfile defaultProfile() {
        return new MobProfile(1, MobRank.NORMAL, MobTier.NORMAL, 20, 2, 4, 0, 0, 0, 0, 10, 5, 1, 150, 0.2D,
                1, 1, "unknown", "neutral", "medium");
    }
}
