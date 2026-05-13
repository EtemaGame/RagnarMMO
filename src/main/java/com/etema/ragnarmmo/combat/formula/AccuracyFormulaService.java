package com.etema.ragnarmmo.combat.formula;

import com.etema.ragnarmmo.player.stats.compute.RoPreRenewalFormulaService;

public final class AccuracyFormulaService {
    private AccuracyFormulaService() {
    }

    public static double hit(int dex, int level, double bonus) {
        return RoPreRenewalFormulaService.hit(dex, level, bonus);
    }

    public static double flee(int agi, int level, double bonus) {
        return RoPreRenewalFormulaService.flee(agi, level, bonus);
    }

    public static double hitRate(double attackerHit, double defenderFlee) {
        return RoPreRenewalFormulaService.hitRate(attackerHit, defenderFlee);
    }

    public static double perfectDodge(int luk) {
        return RoPreRenewalFormulaService.perfectDodge(luk);
    }

    public static double criticalChance(int luk, double bonus) {
        return RoPreRenewalFormulaService.criticalChance(luk, bonus);
    }
}
