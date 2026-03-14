package com.etema.ragnarmmo.system.mobstats.mobs;

import com.etema.ragnarmmo.common.api.stats.StatKeys;
import java.util.EnumMap;
import java.util.Map;

public enum MobClass {
    WARRIOR (1.5, 0.8, 1.7, 0.5, 1.0, 0.6,  // str,agi,vit,int,dex,luk multipliers
            3, 1, 5, 1, 2, 1),            // growth weights por stat (str..luk)
    ROGUE   (1.2, 2.0, 0.8, 0.7, 1.5, 1.0,
            2, 4, 1, 1, 3, 2),
    MAGE    (0.5, 0.6, 0.7, 2.0, 1.5, 0.9,
            1, 1, 1, 5, 3, 2),
    ARCHER  (1.0, 1.3, 1.0, 1.0, 1.8, 1.0,
            2, 2, 2, 2, 5, 2),
    BEAST   (1.4, 1.2, 1.2, 0.6, 0.8, 0.7,
            3, 3, 3, 1, 1, 1),
    UNDEAD  (1.0, 0.7, 1.5, 1.0, 0.8, 0.5,
            2, 1, 4, 2, 1, 1);

    public final double strM, agiM, vitM, intM, dexM, lukM;
    private final int wSTR, wAGI, wVIT, wINT, wDEX, wLUK;

    MobClass(double strM, double agiM, double vitM, double intM, double dexM, double lukM,
             int wSTR, int wAGI, int wVIT, int wINT, int wDEX, int wLUK) {
        this.strM=strM; this.agiM=agiM; this.vitM=vitM; this.intM=intM; this.dexM=dexM; this.lukM=lukM;
        this.wSTR=wSTR; this.wAGI=wAGI; this.wVIT=wVIT; this.wINT=wINT; this.wDEX=wDEX; this.wLUK=wLUK;
    }

    public Map<StatKeys,Integer> growthWeights() {
        Map<StatKeys,Integer> m = new EnumMap<>(StatKeys.class);
        m.put(StatKeys.STR, wSTR); m.put(StatKeys.AGI, wAGI); m.put(StatKeys.VIT, wVIT);
        m.put(StatKeys.INT, wINT); m.put(StatKeys.DEX, wDEX); m.put(StatKeys.LUK, wLUK);
        return m;
    }
    public Map<StatKeys,Double> multipliers() {
        Map<StatKeys,Double> m = new EnumMap<>(StatKeys.class);
        m.put(StatKeys.STR, strM); m.put(StatKeys.AGI, agiM); m.put(StatKeys.VIT, vitM);
        m.put(StatKeys.INT, intM); m.put(StatKeys.DEX, dexM); m.put(StatKeys.LUK, lukM);
        return m;
    }
}






