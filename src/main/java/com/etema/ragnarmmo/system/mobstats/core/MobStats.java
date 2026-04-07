package com.etema.ragnarmmo.system.mobstats.core;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.combat.element.ElementType;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import com.etema.ragnarmmo.system.stats.Multipliers;
import com.etema.ragnarmmo.system.stats.StatContainer;


import java.util.Map;

/**
 * Runtime data container for mob stats that can be persisted through the
 * {@code MobStatsCapability}. A mob always keeps track of its level, tier and
 * the value of each classic Ragnarok stat.
 */
public class MobStats {

    private final StatContainer<StatKeys> stats = new StatContainer<>(StatKeys.class);
    private final Multipliers multipliers = new Multipliers();

    private int level = 1;
    private MobTier tier = MobTier.NORMAL;
    private MobClass mobClass = MobClass.NOVICE;
    private ElementType element = ElementType.NEUTRAL;
    private boolean initialized;

    private double mana;
    private double sp;
    private double maxMana = 100.0;
    private double maxSp = 100.0;

    public MobStats() {
    }

    public void resetStats() {
        stats.resetStats();
    }

    // Mob Properties
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public MobTier getTier() {
        return tier;
    }

    public void setTier(MobTier tier) {
        this.tier = tier != null ? tier : MobTier.NORMAL;
        // Auto-update multipliers based on tier
        this.setHealthMultiplier(this.tier.getHpMultiplier());
        this.setDamageMultiplier(this.tier.getDamageMultiplier());
        this.setDefenseMultiplier(this.tier.getDefenseMultiplier());
    }

    public MobClass getMobClass() {
        return mobClass;
    }

    public void setMobClass(MobClass mobClass) {
        this.mobClass = mobClass != null ? mobClass : MobClass.NOVICE;
    }

    public ElementType getElement() {
        return element;
    }

    public void setElement(ElementType element) {
        this.element = element != null ? element : ElementType.NEUTRAL;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    // Mana & SP
    public double getMana() { return mana; }
    public void setMana(double mana) { this.mana = Math.min(mana, maxMana); }
    public double getMaxMana() { return maxMana; }
    public void setMaxMana(double maxMana) { this.maxMana = maxMana; }
    
    public double getSP() { return sp; }
    public void setSP(double sp) { this.sp = Math.min(sp, maxSp); }
    public double getMaxSP() { return maxSp; }
    public void setMaxSP(double maxSp) { this.maxSp = maxSp; }

    public boolean consumeMana(int amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }

    public boolean consumeSP(int amount) {
        if (sp >= amount) {
            sp -= amount;
            return true;
        }
        return false;
    }

    public void addMana(double amount) { setMana(mana + amount); }
    public void addSP(double amount) { setSP(sp + amount); }

    // Multipliers
    public Multipliers getMultipliers() {
        return multipliers;
    }

    public double getHealthMultiplier() {
        return multipliers.health();
    }

    public void setHealthMultiplier(double value) {
        multipliers.setHealth(value);
    }

    public double getDamageMultiplier() {
        return multipliers.damage();
    }

    public void setDamageMultiplier(double value) {
        multipliers.setDamage(value);
    }

    public double getDefenseMultiplier() {
        return multipliers.defense();
    }

    public void setDefenseMultiplier(double value) {
        multipliers.setDefense(value);
    }

    public double getSpeedMultiplier() {
        return multipliers.speed();
    }

    public void setSpeedMultiplier(double value) {
        multipliers.setSpeed(value);
    }

    // Stats
    public int get(StatKeys key) {
        return stats.get(key);
    }

    public int get(String key) {
        return StatKeys.fromId(key).map(this::get).orElse(0);
    }

    public void set(StatKeys key, int value) {
        stats.set(key, value);
    }

    public void set(String key, int value) {
        StatKeys.fromId(key).ifPresent(k -> set(k, value));
    }

    public void add(StatKeys key, int amount) {
        stats.add(key, amount);
    }

    public void add(String key, int amount) {
        StatKeys.fromId(key).ifPresent(k -> add(k, amount));
    }

    public Map<StatKeys, Integer> view() {
        return stats.view();
    }

    public int getTotalPoints() {
        return stats.total();
    }

    public String describeStats() {
        return stats.describe();
    }
}
