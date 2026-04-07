package com.etema.ragnarmmo.common.api.stats;

import com.etema.ragnarmmo.common.api.stats.ChangeReason;

import java.util.function.IntUnaryOperator;

public interface IPlayerStats {
    int getSTR();
    void setSTR(int v);

    int getAGI();
    void setAGI(int v);

    int getVIT();
    void setVIT(int v);

    int getINT();
    void setINT(int v);

    int getDEX();
    void setDEX(int v);

    int getLUK();
    void setLUK(int v);

    int get(StatKeys key);
    void set(StatKeys key, int value);

    void setStat(StatKeys key, int value, ChangeReason reason);
    void addStat(StatKeys key, int delta, ChangeReason reason);

    int getBonus(StatKeys key);
    void addBonus(StatKeys key, int delta);
    void setBonus(StatKeys key, int value);

    double getMana();
    double getManaMax();
    void setMana(double v);
    void addMana(double dv);
    boolean consumeMana(double amount);

    double getSP();
    double getSPMax();
    void setSP(double v);
    void addSP(double dv);
    boolean consumeSP(double amount);

    void setManaMaxClient(double v);
    void setSPMaxClient(double v);

    int getLevel();
    void setLevel(int lvl);
    void setLevel(int lvl, ChangeReason reason);

    int getExp();
    void setExp(int exp);

    int getStatPoints();
    void setStatPoints(int pts);

    String getJobId();
    void setJobId(String jobId);
    void setJobId(String jobId, ChangeReason reason);

    int getJobLevel();
    void setJobLevel(int lvl);
    void setJobLevel(int lvl, ChangeReason reason);

    int getJobExp();
    void setJobExp(int exp);

    int getSkillPoints();
    void setSkillPoints(int pts);

    void setBaseStatPointsGranted(boolean granted);
    void ensureBaseStatBaseline(int baseline);

    int addExpAndProcessLevelUps(int expToAdd, int pointsPerLevel,
                                 IntUnaryOperator expToNextFunc);

    int addJobExpAndProcessLevelUps(int expToAdd,
                                    IntUnaryOperator expToNextFunc);

    void resetAll(ChangeReason reason);

    void markDirty();
    boolean consumeDirty();
    int consumeDirtyMask();

    net.minecraft.nbt.CompoundTag serializeNBT();
    void deserializeNBT(net.minecraft.nbt.CompoundTag nbt);

    boolean consumeResource(double amount);
    void addResource(double amount);
    double getCurrentResource();
    double getMaxResource();
}
