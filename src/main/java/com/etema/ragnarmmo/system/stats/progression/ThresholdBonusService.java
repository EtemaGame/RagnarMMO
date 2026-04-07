package com.etema.ragnarmmo.system.stats.progression;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * ThresholdBonusService - Applies bonuses for every N points in a base stat.
 * Port of classic RO "bonuses every 10 points".
 */
public class ThresholdBonusService {

    public static final UUID THRESHOLD_BONUS_STR = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000001");
    public static final UUID THRESHOLD_BONUS_AGI = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000002");
    public static final UUID THRESHOLD_BONUS_VIT = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000003");
    public static final UUID THRESHOLD_BONUS_INT = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000004");
    public static final UUID THRESHOLD_BONUS_DEX = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000005");
    public static final UUID THRESHOLD_BONUS_LUK = UUID.fromString("d0d0d0d0-0000-4000-8000-000000000006");

    /**
     * Threshold bonuses are applied directly to Ragnar MMO attributes as transient modifiers.
     */
    public static void recomputeThresholds(Player player, IPlayerStats stats) {
        if (player == null || stats == null) return;

        // Note: Thresholds usually apply to BASE stats in RO, but here we use total assigned (excluding bonuses)
        // to match the spirit of "assigned points matter".
        
        int baseSTR = stats.getSTR();
        int baseAGI = stats.getAGI();
        int baseVIT = stats.getVIT();
        int baseINT = stats.getINT();
        int baseDEX = stats.getDEX();
        int baseLUK = stats.getLUK();

        // 1. STR Threshold: +1 ATK every 10 STR (Handled by CombatMath formulas usually, 
        // but can be added as a flat bonus if we want it explicit)
        // In this mod, we'll keep it simple for now and only apply what CombatMath doesn't cover natively
        // or what we want shown as a "Threshold Bonus" in a breakdown.
        
        // 2. DEX Threshold: +1 HIT every 10 DEX (Example)
        // Actually, RO usually uses (Base_DEX + Base_LVL + Base_LUK/3 + Bonus)
        // We will implement explicit Threshold-only bonuses here if they are not part of the base formula.
    }

    private static void applyBonus(Player p, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, String name, int amount) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;

        inst.removeModifier(id);
        if (amount != 0) {
            inst.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
