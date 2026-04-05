package com.etema.ragnarmmo.system.mobstats.mobs;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import java.util.EnumMap;
import java.util.Map;

public enum MobClass {
    NOVICE(1.0, 1.0, 1.0, 1.0, 10, 10, 10, 10, 10, 10),
    SWORDMAN(1.5, 1.4, 1.5, 0.9, 10, 15, 15, 8, 8, 8),
    THIEF(1.2, 1.6, 1.1, 1.8, 8, 12, 10, 20, 15, 8),
    ACOLYTE(1.3, 1.2, 1.4, 1.0, 8, 8, 12, 10, 10, 15),
    MAGE(1.0, 1.8, 0.9, 1.1, 5, 8, 8, 10, 15, 18),
    ARCHER(1.2, 1.5, 1.1, 1.3, 8, 10, 8, 25, 15, 8),
    MERCHANT(1.4, 1.3, 1.4, 1.0, 12, 12, 12, 8, 8, 12);

    private final double hpMult, dmgMult, defMult, spdMult;
    private final Map<StatKeys, Integer> weights = new EnumMap<>(StatKeys.class);

    MobClass(double hp, double dmg, double def, double spd,
             int vit, int str, int intWeight, int dex, int agi, int luk) {
        this.hpMult = hp;
        this.dmgMult = dmg;
        this.defMult = def;
        this.spdMult = spd;
        weights.put(StatKeys.VIT, vit);
        weights.put(StatKeys.STR, str);
        weights.put(StatKeys.INT, intWeight);
        weights.put(StatKeys.DEX, dex);
        weights.put(StatKeys.AGI, agi);
        weights.put(StatKeys.LUK, luk);
    }

    public double getHpMult() { return hpMult; }
    public double getDmgMult() { return dmgMult; }
    public double getDefMult() { return defMult; }
    public double getSpdMult() { return spdMult; }
    public Map<StatKeys, Integer> getWeights() { return weights; }
}
