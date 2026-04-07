package com.etema.ragnarmmo.system.stats.capability;

import com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.stats.StatContainer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import com.etema.ragnarmmo.system.stats.compute.StatResolutionService;
import com.etema.ragnarmmo.system.stats.progression.StatPointProgression;

public class PlayerStats implements IPlayerStats {

    private final StatContainer<StatKeys> stats = new StatContainer<>(StatKeys.class);

    private static final Map<StatKeys, UUID> BONUS_IDS = Map.of(
            StatKeys.STR, UUID.fromString("1ab0d08f-781c-43ea-9ad5-a78eb6231728"),
            StatKeys.AGI, UUID.fromString("b1c47f1d-8ea0-4e5f-a362-6c5c82cc0608"),
            StatKeys.VIT, UUID.fromString("4a8533de-5f6d-41c9-9f97-72b23aa5ce9d"),
            StatKeys.INT, UUID.fromString("8d35f17d-8e2b-4a9f-8b5d-90f40f6a28ab"),
            StatKeys.DEX, UUID.fromString("7630a31f-8df5-4f2d-92d5-7f84cbe9835d"),
            StatKeys.LUK, UUID.fromString("99167815-6883-4e4d-81b8-2d2e69366b9f"));

    private static final Map<StatKeys, String> BONUS_NAMES = new EnumMap<>(StatKeys.class);

    static {
        for (var key : StatKeys.values()) {
            if (key != StatKeys.LEVEL) {
                BONUS_NAMES.put(key, "ragnarmmo_bonus_" + key.name().toLowerCase());
            }
        }
    }

    private Player owner;
    private double mana = 100, manaMax = 100;
    private double sp = 100, spMax = 100;
    private int level = 1;
    private int exp = 0;
    private int statPoints = 0;
    private int jobLevel = 1;
    private int jobExp = 0;
    private int skillPoints = 0;
    private String jobId = "ragnarmmo:novice";
    private boolean baseStatPointsGranted = false;
    private int dirtyMask = RoPlayerSyncDomain.allMask();

    public PlayerStats() {
        for (var key : StatKeys.values()) {
            stats.set(key, 1);
        }
    }

    void bind(Player player) {
        this.owner = player;
    }

    // Helper for easier access to owner's attributes
    public AttributeInstance getInstance(StatKeys key) {
        if (owner == null) return null;
        Attribute attr = StatAttributes.get(key);
        return attr != null ? owner.getAttribute(attr) : null;
    }

    @Override
    public int getSTR() { return get(StatKeys.STR); }
    @Override
    public void setSTR(int v) { set(StatKeys.STR, v); }
    @Override
    public int getAGI() { return get(StatKeys.AGI); }
    @Override
    public void setAGI(int v) { set(StatKeys.AGI, v); }
    @Override
    public int getVIT() { return get(StatKeys.VIT); }
    @Override
    public void setVIT(int v) { set(StatKeys.VIT, v); }
    @Override
    public int getINT() { return get(StatKeys.INT); }
    @Override
    public void setINT(int v) { set(StatKeys.INT, v); }
    @Override
    public int getDEX() { return get(StatKeys.DEX); }
    @Override
    public void setDEX(int v) { set(StatKeys.DEX, v); }
    @Override
    public int getLUK() { return get(StatKeys.LUK); }
    @Override
    public void setLUK(int v) { set(StatKeys.LUK, v); }

    @Override
    public int get(StatKeys key) { return stats.get(key); }

    @Override
    public void setStat(StatKeys key, int value, ChangeReason reason) { set(key, value); }
    @Override
    public void addStat(StatKeys key, int delta, ChangeReason reason) { set(key, get(key) + delta); }

    @Override
    public void set(StatKeys key, int value) {
        int maxValue = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxStatValue.get();
        int clamped = Mth.clamp(value, 1, Math.max(1, maxValue));
        if (get(key) != clamped) {
            stats.set(key, clamped);
            syncAttribute(key, clamped);
            markDirty(RoPlayerSyncDomain.STATS);
            StatResolutionService.resolve(owner, this);
        }
    }

    private void syncAttribute(StatKeys key, int value) {
        AttributeInstance inst = getInstance(key);
        if (inst == null) return;
        double clamped = value;
        if (inst.getAttribute() instanceof RangedAttribute ranged) {
            clamped = Mth.clamp(value, (float) ranged.getMinValue(), (float) ranged.getMaxValue());
        }
        if (Double.compare(inst.getBaseValue(), clamped) != 0) {
            inst.setBaseValue(clamped);
        }
    }

    @Override
    public int getBonus(StatKeys key) {
        AttributeInstance inst = getInstance(key);
        if (inst == null) return 0;
        AttributeModifier mod = inst.getModifier(BONUS_IDS.get(key));
        return mod == null ? 0 : (int) Math.round(mod.getAmount());
    }

    @Override
    public void addBonus(StatKeys key, int d) { setBonus(key, getBonus(key) + d); }

    @Override
    public void setBonus(StatKeys key, int v) {
        AttributeInstance inst = getInstance(key);
        if (inst == null) return;
        UUID id = BONUS_IDS.get(key);
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (existing.getAmount() == v) return;
            inst.removeModifier(existing);
        }
        if (v != 0) {
            inst.addTransientModifier(new AttributeModifier(id, BONUS_NAMES.get(key), v, AttributeModifier.Operation.ADDITION));
        }
        markDirty(RoPlayerSyncDomain.STATS);
        StatResolutionService.resolve(owner, this);
    }

    @Override
    public double getMana() { return mana; }
    @Override
    public double getManaMax() { return manaMax; }
    @Override
    public void setMana(double v) {
        mana = Mth.clamp(v, 0, manaMax);
        markDirty(RoPlayerSyncDomain.RESOURCES);
    }
    @Override
    public void addMana(double dv) { setMana(mana + dv); }
    @Override
    public boolean consumeMana(double amount) {
        if (mana < amount) return false;
        mana -= amount;
        markDirty(RoPlayerSyncDomain.RESOURCES);
        return true;
    }

    @Override
    public double getSP() { return sp; }
    @Override
    public double getSPMax() { return spMax; }
    @Override
    public void setSP(double v) {
        sp = Mth.clamp(v, 0, spMax);
        markDirty(RoPlayerSyncDomain.RESOURCES);
    }
    @Override
    public void addSP(double dv) { setSP(sp + dv); }
    @Override
    public boolean consumeSP(double amount) {
        if (sp < amount) return false;
        sp -= amount;
        markDirty(RoPlayerSyncDomain.RESOURCES);
        return true;
    }

    @Override
    public void setManaMaxClient(double v) {
        this.manaMax = v;
        this.mana = Math.min(mana, manaMax);
        markDirty(RoPlayerSyncDomain.RESOURCES);
    }

    @Override
    public void setSPMaxClient(double v) {
        this.spMax = v;
        this.sp = Math.min(sp, spMax);
        markDirty(RoPlayerSyncDomain.RESOURCES);
    }

    @Override
    public int getLevel() { return level; }
    @Override
    public void setLevel(int lvl) {
        level = clampLevel(lvl);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }
    @Override
    public void setLevel(int lvl, ChangeReason reason) { setLevel(lvl); }

    @Override
    public int getExp() { return exp; }
    @Override
    public void setExp(int e) {
        exp = Math.max(0, e);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }

    @Override
    public int getStatPoints() { return statPoints; }
    @Override
    public void setStatPoints(int pts) {
        statPoints = Math.max(0, pts);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }

    @Override
    public int getJobLevel() { return jobLevel; }
    @Override
    public void setJobLevel(int lvl) {
        jobLevel = clampJobLevel(lvl);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }
    @Override
    public void setJobLevel(int lvl, ChangeReason reason) { setJobLevel(lvl); }

    @Override
    public int getJobExp() { return jobExp; }
    @Override
    public void setJobExp(int e) {
        jobExp = Math.max(0, e);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }

    @Override
    public int getSkillPoints() { return skillPoints; }
    @Override
    public void setSkillPoints(int pts) {
        skillPoints = Math.max(0, pts);
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }

    public boolean isBaseStatPointsGranted() {
        return baseStatPointsGranted;
    }

    @Override
    public void setBaseStatPointsGranted(boolean granted) {
        this.baseStatPointsGranted = granted;
        markDirty(RoPlayerSyncDomain.PROGRESSION);
    }

    @Override
    public void ensureBaseStatBaseline(int baseline) {
        if (!baseStatPointsGranted) {
            setStatPoints(getStatPoints() + baseline);
            setBaseStatPointsGranted(true);
        }
    }

    @Override
    public String getJobId() { return jobId; }
    @Override
    public void setJobId(String id) {
        this.jobId = id;
        markDirty(RoPlayerSyncDomain.PROGRESSION);
        StatResolutionService.resolve(owner, this);
    }
    @Override
    public void setJobId(String id, ChangeReason reason) { setJobId(id); }

    private int clampLevel(int lvl) {
        int max = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxLevel.get();
        return Mth.clamp(lvl, 1, max);
    }

    private int clampJobLevel(int lvl) {
        int max = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxJobLevel.get();
        return Mth.clamp(lvl, 1, max);
    }

    @Override
    public void resetAll(ChangeReason reason) {
        jobId = "ragnarmmo:novice";
        level = 1;
        jobLevel = 1;
        exp = 0;
        jobExp = 0;
        statPoints = 0;
        skillPoints = 0;
        baseStatPointsGranted = false;
        for (StatKeys k : StatKeys.values()) stats.set(k, 1);
        markDirty();
        StatResolutionService.resolve(owner, this);
    }

    @Override
    public int addExpAndProcessLevelUps(int add, int ptsPerLvl, IntUnaryOperator nextFunc) {
        if (add <= 0) return 0;
        exp += add;
        int gained = 0;
        while (exp >= nextFunc.applyAsInt(level)) {
            exp -= nextFunc.applyAsInt(level);
            level++;
            statPoints += StatPointProgression.pointsForLevelUp(level, ptsPerLvl);
            gained++;
        }
        markDirty(RoPlayerSyncDomain.PROGRESSION);
        return gained;
    }

    @Override
    public int addJobExpAndProcessLevelUps(int add, IntUnaryOperator nextFunc) {
        if (add <= 0) return 0;
        jobExp += add;
        int gained = 0;
        while (jobExp >= nextFunc.applyAsInt(jobLevel)) {
            jobExp -= nextFunc.applyAsInt(jobLevel);
            jobLevel++;
            skillPoints++;
            gained++;
        }
        markDirty(RoPlayerSyncDomain.PROGRESSION);
        return gained;
    }

    @Override
    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putInt("Level", level);
        nbt.putInt("Exp", exp);
        nbt.putInt("StatPoints", statPoints);
        nbt.putInt("JobLevel", jobLevel);
        nbt.putInt("JobExp", jobExp);
        nbt.putInt("SkillPoints", skillPoints);
        nbt.putString("JobId", jobId);
        nbt.putBoolean("BaseStatPointsGranted", baseStatPointsGranted);
        nbt.putDouble("Mana", mana);
        nbt.putDouble("ManaMax", manaMax);
        nbt.putDouble("SP", sp);
        nbt.putDouble("SPMax", spMax);
        net.minecraft.nbt.CompoundTag s = new net.minecraft.nbt.CompoundTag();
        for (StatKeys k : StatKeys.values()) s.putInt(k.id(), get(k));
        nbt.put("Stats", s);
        return nbt;
    }

    @Override
    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        level = nbt.getInt("Level");
        exp = nbt.getInt("Exp");
        statPoints = nbt.getInt("StatPoints");
        jobLevel = nbt.getInt("JobLevel");
        jobExp = nbt.getInt("JobExp");
        skillPoints = nbt.getInt("SkillPoints");
        jobId = nbt.getString("JobId");
        baseStatPointsGranted = nbt.getBoolean("BaseStatPointsGranted");
        mana = nbt.getDouble("Mana");
        manaMax = nbt.getDouble("ManaMax");
        sp = nbt.getDouble("SP");
        spMax = nbt.getDouble("SPMax");
        if (nbt.contains("Stats")) {
            net.minecraft.nbt.CompoundTag s = nbt.getCompound("Stats");
            for (StatKeys k : StatKeys.values()) if (s.contains(k.id())) set(k, s.getInt(k.id()));
        }
    }

    @Override
    public void markDirty() { dirtyMask = RoPlayerSyncDomain.allMask(); }
    @Override
    public boolean consumeDirty() {
        return consumeDirtyMask() != 0;
    }

    @Override
    public int consumeDirtyMask() {
        int m = dirtyMask;
        dirtyMask = 0;
        return m;
    }

    public void markDirty(RoPlayerSyncDomain... d) {
        if (d == null || d.length == 0) { markDirty(); return; }
        for (RoPlayerSyncDomain dm : d) if (dm != null) dirtyMask |= dm.bit();
    }

    /**
     * Applies a server snapshot to this capability instance.
     * Used ONLY on the client to synchronize state without triggering domain logic,
     * recalculations, or dirty flagging.
     *
     * @param msg The synchronization packet from the server.
     */
    public void applyMirrorState(com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket msg) {
        int mask = msg.syncMask;

        if (com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.includes(mask, com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.STATS)) {
            // Apply base stat values directly
            this.stats.set(StatKeys.STR, msg.str);
            this.stats.set(StatKeys.AGI, msg.agi);
            this.stats.set(StatKeys.VIT, msg.vit);
            this.stats.set(StatKeys.INT, msg.intelligence);
            this.stats.set(StatKeys.DEX, msg.dex);
            this.stats.set(StatKeys.LUK, msg.luk);

            // Synchronize Forge attributes WITHOUT dirtying
            syncAttribute(StatKeys.STR, msg.str);
            syncAttribute(StatKeys.AGI, msg.agi);
            syncAttribute(StatKeys.VIT, msg.vit);
            syncAttribute(StatKeys.INT, msg.intelligence);
            syncAttribute(StatKeys.DEX, msg.dex);
            syncAttribute(StatKeys.LUK, msg.luk);
        }

        if (com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.includes(mask, com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.RESOURCES)) {
            this.mana = msg.mana;
            this.manaMax = msg.manaMax;
            this.sp = msg.sp;
            this.spMax = msg.spMax;
        }

        if (com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.includes(mask, com.etema.ragnarmmo.common.api.player.RoPlayerSyncDomain.PROGRESSION)) {
            this.level = msg.level;
            this.exp = msg.exp;
            this.statPoints = msg.statPoints;
            this.jobLevel = msg.jobLevel;
            this.jobExp = msg.jobExp;
            this.skillPoints = msg.skillPoints;
            this.jobId = msg.jobId;
            this.baseStatPointsGranted = msg.baseStatPointsGranted;
        }
    }

    @Override
    public double getCurrentResource() {
        return com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId).isMagical() ? mana : sp;
    }
    @Override
    public double getMaxResource() {
        return com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId).isMagical() ? manaMax : spMax;
    }
    @Override
    public boolean consumeResource(double amount) {
        return com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId).isMagical() ? consumeMana(amount) : consumeSP(amount);
    }
    @Override
    public void addResource(double amount) {
        if (com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId).isMagical()) addMana(amount); else addSP(amount);
    }
}
