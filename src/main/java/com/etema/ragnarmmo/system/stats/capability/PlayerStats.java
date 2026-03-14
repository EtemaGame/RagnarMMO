package com.etema.ragnarmmo.system.stats.capability;

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
import com.etema.ragnarmmo.system.stats.progression.JobBonusService;

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
            BONUS_NAMES.put(key, "ragnarmmo_bonus_" + key.name().toLowerCase());
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
    private boolean dirty = true;

    public PlayerStats() {
        for (var key : StatKeys.values()) {
            stats.set(key, 1);
        }
    }

    void bind(Player player) {
        this.owner = player;
    }

    @Override
    public int getSTR() {
        return get(StatKeys.STR);
    }

    @Override
    public void setSTR(int v) {
        set(StatKeys.STR, v);
    }

    @Override
    public int getAGI() {
        return get(StatKeys.AGI);
    }

    @Override
    public void setAGI(int v) {
        set(StatKeys.AGI, v);
    }

    @Override
    public int getVIT() {
        return get(StatKeys.VIT);
    }

    @Override
    public void setVIT(int v) {
        set(StatKeys.VIT, v);
    }

    @Override
    public int getINT() {
        return get(StatKeys.INT);
    }

    @Override
    public void setINT(int v) {
        set(StatKeys.INT, v);
    }

    @Override
    public int getDEX() {
        return get(StatKeys.DEX);
    }

    @Override
    public void setDEX(int v) {
        set(StatKeys.DEX, v);
    }

    @Override
    public int getLUK() {
        return get(StatKeys.LUK);
    }

    @Override
    public void setLUK(int v) {
        set(StatKeys.LUK, v);
    }

    public int get(StatKeys key) {
        return stats.get(key);
    }

    @Override
    public void setStat(StatKeys key, int value, ChangeReason reason) {
        set(key, value);
    }

    @Override
    public void addStat(StatKeys key, int delta, ChangeReason reason) {
        set(key, get(key) + delta);
    }

    public void set(StatKeys key, int value) {
        int maxValue = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxStatValue.get();
        int clamped = Mth.clamp(value, 1, Math.max(1, maxValue));
        if (get(key) != clamped) {
            stats.set(key, clamped);
            syncAttribute(key, clamped);
            dirty = true;
        }
    }

    private void syncAttribute(StatKeys key, int value) {
        AttributeInstance inst = getInstance(key);
        if (inst == null)
            return;

        double clamped = value;
        if (inst.getAttribute() instanceof RangedAttribute ranged) {
            clamped = Mth.clamp(value, ranged.getMinValue(), ranged.getMaxValue());
        }

        if (Double.compare(inst.getBaseValue(), clamped) != 0) {
            inst.setBaseValue(clamped);
        }
    }

    @Override
    public int getBonus(StatKeys key) {
        AttributeInstance inst = getInstance(key);
        if (inst == null)
            return 0;
        AttributeModifier mod = inst.getModifier(BONUS_IDS.get(key));
        return mod == null ? 0 : (int) Math.round(mod.getAmount());
    }

    @Override
    public void addBonus(StatKeys key, int d) {
        setBonus(key, getBonus(key) + d);
    }

    @Override
    public void setBonus(StatKeys key, int v) {
        AttributeInstance inst = getInstance(key);
        if (inst == null)
            return;
        UUID id = BONUS_IDS.get(key);
        AttributeModifier existing = inst.getModifier(id);
        if (existing != null) {
            if (existing.getAmount() == v) {
                return;
            }
            inst.removeModifier(existing);
        }
        if (v != 0) {
            inst.addTransientModifier(new AttributeModifier(id, BONUS_NAMES.get(key), v,
                    AttributeModifier.Operation.ADDITION));
        }
        dirty = true;
    }

    @Override
    public double getMana() {
        return mana;
    }

    @Override
    public double getManaMax() {
        return manaMax;
    }

    @Override
    public void setMana(double v) {
        mana = Math.max(0, Math.min(v, manaMax));
    }

    @Override
    public void addMana(double dv) {
        setMana(mana + dv);
    }

    @Override
    public void setManaMaxClient(double v) {
        boolean wasFull = this.mana >= this.manaMax - 1e-4;
        this.manaMax = v;
        if (wasFull || mana > manaMax)
            mana = manaMax;
    }

    // ═══════════════════════════════════════════════
    // SP Resource (Physical classes)
    // ═══════════════════════════════════════════════
    public double getSP() {
        return sp;
    }

    public double getSPMax() {
        return spMax;
    }

    public void setSP(double v) {
        sp = Math.max(0, Math.min(v, spMax));
    }

    public void addSP(double dv) {
        setSP(sp + dv);
    }

    public void setSPMaxClient(double v) {
        boolean wasFull = this.sp >= this.spMax - 1e-4;
        this.spMax = v;
        if (wasFull || sp > spMax)
            sp = spMax;
    }

    // ═══════════════════════════════════════════════
    // Unified Resource Access
    // ═══════════════════════════════════════════════
    /**
     * /**
     * Returns the current resource value for the player's job type.
     * Magical jobs use Mana, physical jobs use SP.
     */
    public double getCurrentResource() {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        return job.isMagical() ? mana : sp;
    }

    /**
     * Returns the max resource value for the player's job type.
     */
    public double getMaxResource() {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        return job.isMagical() ? manaMax : spMax;
    }

    /**
     * Consumes resource for the player's job type.
     * 
     * @return true if enough resource was available and consumed.
     */
    public boolean consumeResource(double amount) {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        if (job.isMagical()) {
            if (mana < amount)
                return false;
            mana -= amount;
            dirty = true;
            return true;
        } else {
            if (sp < amount)
                return false;
            sp -= amount;
            dirty = true;
            return true;
        }
    }

    /**
     * Adds resource to the player's job type (for regeneration).
     */
    public void addResource(double amount) {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        if (job.isMagical()) {
            addMana(amount);
        } else {
            addSP(amount);
        }
        dirty = true;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int lvl) {
        int clamped = clampLevel(lvl);
        if (level != clamped) {
            level = clamped;
            dirty = true;
        }
    }

    @Override
    public void setLevel(int lvl, ChangeReason reason) {
        setLevel(lvl);
    }

    @Override
    public int getExp() {
        return exp;
    }

    @Override
    public void setExp(int e) {
        exp = Math.max(0, e);
        dirty = true;
    }

    @Override
    public int getStatPoints() {
        return statPoints;
    }

    @Override
    public void setStatPoints(int pts) {
        statPoints = Math.max(0, pts);
        dirty = true;
    }

    @Override
    public int getJobLevel() {
        return jobLevel;
    }

    @Override
    public void setJobLevel(int lvl) {
        int clamped = clampJobLevel(lvl);
        if (jobLevel != clamped) {
            jobLevel = clamped;
            dirty = true;
            JobBonusService.recomputeStats(owner, this);
        }
    }

    @Override
    public void setJobLevel(int lvl, ChangeReason reason) {
        setJobLevel(lvl);
    }

    @Override
    public int getJobExp() {
        return jobExp;
    }

    @Override
    public void setJobExp(int e) {
        jobExp = Math.max(0, e);
        dirty = true;
    }

    @Override
    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public void setSkillPoints(int pts) {
        skillPoints = Math.max(0, pts);
        dirty = true;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    @Override
    public void setJobId(String jobId) {
        String normalized = jobId == null ? "" : jobId.trim();
        if (normalized.isBlank()) {
            normalized = "ragnarmmo:novice";
        } else if (normalized.contains(":")) {
            int idx = normalized.indexOf(':');
            String ns = normalized.substring(0, idx).toLowerCase(Locale.ROOT);
            String path = normalized.substring(idx + 1).toLowerCase(Locale.ROOT);
            normalized = ns + ":" + path;
        } else {
            normalized = "ragnarmmo:" + normalized.toLowerCase(Locale.ROOT);
        }

        if (!normalized.equals(this.jobId)) {
            this.jobId = normalized;
            // Re-clamp caps that depend on job.
            setLevel(level);
            setJobLevel(jobLevel);
            dirty = true;
            JobBonusService.recomputeStats(owner, this);
        }
    }

    @Override
    public void setJobId(String jobId, ChangeReason reason) {
        setJobId(jobId);
    }

    @Override
    public void resetAll(ChangeReason reason) {
        setJobId("ragnarmmo:novice");
        setLevel(1);
        setJobLevel(1);
        setExp(0);
        setJobExp(0);
        setStatPoints(0);
        setSkillPoints(0);
        for (StatKeys key : StatKeys.values()) {
            set(key, 1);
        }
        setBaseStatPointsGranted(false);
        markDirty();
        JobBonusService.recomputeStats(owner, this);
    }

    @Override
    public int addExpAndProcessLevelUps(int add, int pointsPerLevel, IntUnaryOperator expToNext) {
        if (add <= 0)
            return 0;
        exp += add;
        int gained = 0;

        int maxLvl = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxLevel.get();
        if ("ragnarmmo:novice".equals(jobId)) {
            maxLvl = Math.min(maxLvl, com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.noviceMaxLevel.get());
        }

        while (exp >= expToNext.applyAsInt(level)) {
            if (level >= maxLvl) {
                exp = expToNext.applyAsInt(level) - 1; // Cap at max - 1 exp
                break;
            }
            exp -= expToNext.applyAsInt(level);
            level++;
            statPoints += Math.max(0, pointsPerLevel);
            gained++;
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.etema.ragnarmmo.system.achievements.AchievementTriggerHandler.onPlayerLevelUp(sp, level, jobLevel);
            }
        }
        dirty = true;
        return gained;
    }

    @Override
    public int addJobExpAndProcessLevelUps(int add, IntUnaryOperator expToNext) {
        if (add <= 0)
            return 0;
        jobExp += add;
        int gained = 0;

        int maxJobLvl = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxJobLevel.get();
        if ("ragnarmmo:novice".equals(jobId)) {
            maxJobLvl = Math.min(maxJobLvl,
                    com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.noviceMaxJobLevel.get());
        }

        while (jobExp >= expToNext.applyAsInt(jobLevel)) {
            if (jobLevel >= maxJobLvl) {
                jobExp = expToNext.applyAsInt(jobLevel) - 1;
                break;
            }
            jobExp -= expToNext.applyAsInt(jobLevel);
            jobLevel++;
            skillPoints++;
            gained++;
            if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.etema.ragnarmmo.system.achievements.AchievementTriggerHandler.onPlayerLevelUp(sp, level, jobLevel);
            }
        }
        if (gained > 0 || add > 0) {
            dirty = true;
        }
        return gained;
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public void ensureBaseStatBaseline(int basePoints) {
        if (!baseStatPointsGranted) {
            statPoints = Math.max(statPoints, Math.max(0, basePoints));
            baseStatPointsGranted = true;
            dirty = true;
        }
    }

    public boolean areBaseStatPointsGranted() {
        return baseStatPointsGranted;
    }

    public void setBaseStatPointsGranted(boolean granted) {
        this.baseStatPointsGranted = granted;
    }

    @Override
    public net.minecraft.nbt.CompoundTag serializeNBT() {
        net.minecraft.nbt.CompoundTag n = new net.minecraft.nbt.CompoundTag();
        n.putInt("STR", getSTR());
        n.putInt("AGI", getAGI());
        n.putInt("VIT", getVIT());
        n.putInt("INT", getINT());
        n.putInt("DEX", getDEX());
        n.putInt("LUK", getLUK());
        n.putDouble("Mana", getMana());
        n.putDouble("ManaMax", getManaMax());
        n.putDouble("SP", getSP());
        n.putDouble("SPMax", getSPMax());
        n.putInt("Level", getLevel());
        n.putInt("Exp", getExp());
        n.putInt("StatPoints", getStatPoints());
        n.putInt("JobLevel", getJobLevel());
        n.putInt("JobExp", getJobExp());
        n.putInt("SkillPoints", getSkillPoints());
        n.putString("JobId", getJobId());
        n.putBoolean("BaseStatPointsGranted", areBaseStatPointsGranted());
        return n;
    }

    @Override
    public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
        // IMPORTANT: Load JobId FIRST so level/jobLevel clamping uses the correct job's
        // caps
        setJobId(nbt.contains("JobId") ? nbt.getString("JobId") : "ragnarmmo:novice");

        setSTR(nbt.getInt("STR"));
        setAGI(nbt.getInt("AGI"));
        setVIT(nbt.getInt("VIT"));
        setINT(nbt.getInt("INT"));
        setDEX(nbt.getInt("DEX"));
        setLUK(nbt.getInt("LUK"));
        setMana(nbt.getDouble("Mana"));
        if (nbt.contains("ManaMax")) {
            setManaMaxClient(nbt.getDouble("ManaMax"));
        }
        setSP(nbt.contains("SP") ? nbt.getDouble("SP") : 0);
        if (nbt.contains("SPMax")) {
            setSPMaxClient(nbt.getDouble("SPMax"));
        }
        setLevel(nbt.contains("Level") ? nbt.getInt("Level") : 1);
        setExp(nbt.getInt("Exp"));
        setStatPoints(nbt.getInt("StatPoints"));
        setJobLevel(nbt.contains("JobLevel") ? nbt.getInt("JobLevel") : 1);
        setJobExp(nbt.getInt("JobExp"));
        setSkillPoints(nbt.getInt("SkillPoints"));
        boolean granted = nbt.contains("BaseStatPointsGranted")
                ? nbt.getBoolean("BaseStatPointsGranted")
                : !nbt.isEmpty();
        setBaseStatPointsGranted(granted);
        JobBonusService.recomputeStats(owner, this);
    }

    private AttributeInstance getInstance(StatKeys key) {
        if (owner == null)
            return null;
        Attribute attr = StatAttributes.getAttribute(key);
        return attr == null ? null : owner.getAttribute(attr);
    }

    private boolean isNoviceJobId(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return true;
        }
        String id = jobId;
        if (id.contains(":")) {
            id = id.substring(id.indexOf(':') + 1);
        }
        return "novice".equalsIgnoreCase(id);
    }

    private int clampLevel(int level) {
        int clamped = Math.max(1, level);
        int max = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxLevel.get();
        if (isNoviceJobId(jobId)) {
            max = Math.min(max, com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.noviceMaxLevel.get());
        }
        return Math.min(clamped, Math.max(1, max));
    }

    private int clampJobLevel(int level) {
        int clamped = Math.max(1, level);
        com.etema.ragnarmmo.common.api.jobs.JobType job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);

        int max;
        if (job == com.etema.ragnarmmo.common.api.jobs.JobType.NOVICE) {
            max = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.noviceMaxJobLevel.get();
        } else {
            max = com.etema.ragnarmmo.common.config.RagnarConfigs.SERVER.caps.maxJobLevel.get();
        }

        return Math.min(clamped, Math.max(1, max));
    }

}
