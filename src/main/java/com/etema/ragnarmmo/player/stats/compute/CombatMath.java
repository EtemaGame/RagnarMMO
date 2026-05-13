package com.etema.ragnarmmo.player.stats.compute;

import com.etema.ragnarmmo.combat.formula.AccuracyFormulaService;
import com.etema.ragnarmmo.combat.formula.AspdFormulaService;
import com.etema.ragnarmmo.combat.formula.DamageFormulaService;
import com.etema.ragnarmmo.combat.formula.DefenseFormulaService;
import com.etema.ragnarmmo.combat.formula.FormulaUtil;
import com.etema.ragnarmmo.combat.formula.ResourceFormulaService;
import com.etema.ragnarmmo.combat.formula.StatusResistanceService;
import com.etema.ragnarmmo.combat.resolver.MobCombatProfileResolver;
import com.etema.ragnarmmo.combat.resolver.TargetCombatProfileResolver;
import com.etema.ragnarmmo.combat.resolver.WeaponClassificationService;
import net.minecraft.world.item.*;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Formulas de combate basadas en Ragnarok Online (pre-renewal).
 * Todos los valores son constantes hardcodeadas usando las formulas RO
 * clasicas.
 *
 * Referencia: https://irowiki.org/wiki/Attacks
 */
public final class CombatMath {

    private CombatMath() {
    }

    // ========================================
    // CONSTANTES RO (hardcodeadas)
    // ========================================

    // ATK
    private static final double DEX_TO_ATK_DIVISOR = 5.0;
    private static final double STR_TO_RANGED_ATK_DIVISOR = 5.0;
    private static final double LUK_TO_ATK_DIVISOR = 5.0;

    // Damage Variance
    private static final double MIN_DAMAGE_ROLL = 0.8;
    private static final double DEX_VARIANCE_DIVISOR = 150.0;
    private static final double LUK_VARIANCE_BONUS = 300.0;

    // HIT/FLEE
    public static final double HIT_BASE = RoPreRenewalFormulaService.HIT_BASE;
    public static final double DEX_TO_HIT_MULT = 1.0;
    public static final double LUK_TO_HIT_DIVISOR = 3.0;
    public static final double LEVEL_TO_HIT_MULT = 1.0;
    public static final double FLEE_BASE = RoPreRenewalFormulaService.FLEE_BASE;
    public static final double AGI_TO_FLEE_MULT = 1.0;
    public static final double LUK_TO_FLEE_DIVISOR = 5.0;
    public static final double LEVEL_TO_FLEE_MULT = 1.0;
    public static final double HIT_FLEE_FORMULA_CONSTANT = 80.0;
    private static final double PERFECT_DODGE_DIVISOR = 10.0;
    private static final double PERFECT_DODGE_MAX = 1.0;

    // Critical
    private static final double CRIT_BASE_MULT = 1.4;
    private static final double LUK_TO_CRIT_DIVISOR = 3.0;
    private static final double DEX_TO_CRIT_DIVISOR = 0.0; // disabled for classic
    private static final double CRIT_MAX = 1.0;

    // ASPD
    private static final double AGI_TO_ASPD = 0.25;
    private static final double DEX_TO_ASPD = 0.1;
    private static final double ASPD_RO_MIN = 50.0;
    private static final double ASPD_RO_MAX = 190.0;
    private static final double ASPD_MIN = 0.25;
    private static final double APS_MAX = 5.0;

    public static final int SHIELD_ASPD_PENALTY = 5; // iROWiki flat penalty

    // Defense (Pre-Renewal)
    private static final double HARD_DEF_REDUCTION_MULT = 0.01; // 1% per point
    private static final double DR_PHYS_MAX = 0.99;

    // MDEF
    private static final double DR_MAGIC_MAX = 0.99;

    // Cast Time
    private static final double CAST_FIXED_RATIO = 0.2;
    private static final double CAST_MIN = 0.0;

    // HP
    private static final double HP_REGEN_BASE = 1.0;
    private static final double VIT_TO_HP_REGEN = 0.2;
    private static final double HP_REGEN_MAX_PERCENT = 0.02;

    // Mana
    private static final double MANA_REGEN_BASE_PERCENT = 0.01;
    private static final double INT_TO_MANA_REGEN = 0.002;
    private static final double MANA_REGEN_MAX_PERCENT = 0.05;

    // ========================================
    // UTILIDADES
    // ========================================

    public static double clamp(double min, double max, double value) {
        return FormulaUtil.clamp(min, max, value);
    }

    public static double soft(double value, double constant) {
        return FormulaUtil.soft(value, constant);
    }

    // ========================================
    // SIZE PENALTY
    // ========================================

    public enum MobSize {
        SMALL, MEDIUM, LARGE
    }

    public static double getWeaponSizePenalty(ItemStack weapon, MobSize size) {
        if (weapon.isEmpty()) return 1.0;
        Item item = weapon.getItem();
        boolean isDagger = weapon.getTags().anyMatch(t -> t.location().getPath().contains("daggers"));
        boolean isMace = weapon.getTags().anyMatch(t -> t.location().getPath().contains("maces"));
        boolean isStaff = weapon.getTags().anyMatch(t -> t.location().getPath().contains("staves"));
        boolean isWand = weapon.getTags().anyMatch(t -> t.location().getPath().contains("wands"));
        boolean isTwoHanded = weapon.getTags().anyMatch(t -> t.location().getPath().contains("two_handed"));
        boolean isSpear = item instanceof net.minecraft.world.item.TridentItem || weapon.getTags().anyMatch(t -> t.location().getPath().contains("spears"));
        boolean isKatar = weapon.getTags().anyMatch(t -> t.location().getPath().contains("katars"));

        // RO-accurate penalties (Pre-Renewal Style)
        if (isDagger) {
            return switch (size) {
                case SMALL -> 1.0;
                case MEDIUM -> 0.75;
                case LARGE -> 0.5;
            };
        }
        if (isSpear) {
            return switch (size) {
                case SMALL -> 0.75;
                case MEDIUM -> 0.75;
                case LARGE -> 1.0;
            };
        }
        if (isMace || isStaff || isWand) {
            return switch (size) {
                case SMALL -> 0.75;
                case MEDIUM -> 1.0;
                case LARGE -> 1.0;
            };
        }
        if (isKatar) {
            return switch (size) {
                case SMALL -> 0.75;
                case MEDIUM -> 1.0;
                case LARGE -> 0.75;
            };
        }
        if (item instanceof net.minecraft.world.item.SwordItem) {
            if (isTwoHanded) {
                return switch (size) {
                    case SMALL -> 0.75;
                    case MEDIUM -> 0.75;
                    case LARGE -> 1.0;
                };
            }
            return switch (size) {
                case SMALL -> 0.75;
                case MEDIUM -> 1.0;
                case LARGE -> 0.75;
            };
        }
        if (item instanceof net.minecraft.world.item.AxeItem) {
            return switch (size) {
                case SMALL -> 0.5;
                case MEDIUM -> 0.75;
                case LARGE -> 1.0;
            };
        }
        if (item instanceof net.minecraft.world.item.BowItem || item instanceof net.minecraft.world.item.CrossbowItem) {
            return switch (size) {
                case SMALL -> 1.0;
                case MEDIUM -> 1.0;
                case LARGE -> 0.75;
            };
        }

        return 1.0;
    }
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // ========================================
    // ATK (ATAQUE FISICO)
    // ========================================

    public static double computeStatusATK(int STR, int DEX, int LUK, int level, boolean isRanged) {
        return DamageFormulaService.statusAtk(STR, DEX, LUK, isRanged);
    }

    public static double computeWeaponATK(double weaponBase, int STR, int DEX, boolean isRanged) {
        return DamageFormulaService.weaponAtk(weaponBase);
    }
    
    public static int computeRangedDrawTicks(int baseDrawTicks, int agi) {
        return DamageFormulaService.rangedDrawTicks(baseDrawTicks, agi);
    }

    public static double computeTotalATK(int STR, int DEX, int LUK, int level,
            double weaponATK, double bonusATK, boolean isRanged) {
        return DamageFormulaService.totalAtk(STR, DEX, LUK, weaponATK, bonusATK, isRanged);
    }

    public static double computeDamageVariance(double baseDamage, int DEX, int LUK,
            java.util.Random rng) {
        return DamageFormulaService.damageVariance(baseDamage, DEX, LUK, rng);
    }

    /**
     * Compute the minimum damage floor (deterministic, no RNG).
     * Used by StatComputer to show the damage range in UI.
     */
    public static double computeDamageVarianceFloor(double baseDamage, int DEX, int LUK) {
        return DamageFormulaService.damageVarianceFloor(baseDamage, DEX, LUK);
    }

    // ========================================
    // MATK (ATAQUE MAGICO)
    // ========================================

    public static double computeStatusMATKMin(int INT) {
        return DamageFormulaService.statusMatkMin(INT);
    }

    public static double computeStatusMATKMax(int INT) {
        return DamageFormulaService.statusMatkMax(INT);
    }

    public static double computeStatusMATK(int INT, int DEX, int LUK, int level) {
        return DamageFormulaService.statusMatk(INT);
    }

    public static double computeTotalMATK(int INT, int DEX, int LUK, int level,
            double spellBase, double bonusMATK) {
        return DamageFormulaService.totalMatk(INT, spellBase, bonusMATK);
    }

    // ========================================
    // HIT / FLEE
    // ========================================

    public static double computeHIT(int DEX, int LUK, int level, double bonus) {
        return AccuracyFormulaService.hit(DEX, level, bonus);
    }

    public static double computeFLEE(int AGI, int LUK, int level, double bonus) {
        return AccuracyFormulaService.flee(AGI, level, bonus);
    }

    public static double computeHitRate(double attackerHIT, double defenderFLEE) {
        return AccuracyFormulaService.hitRate(attackerHIT, defenderFLEE);
    }

    public static double computePerfectDodge(int LUK) {
        return AccuracyFormulaService.perfectDodge(LUK);
    }

    // ========================================
    // CRITICO
    // ========================================

    public static double computeCritChance(int LUK, int DEX, double bonus) {
        return AccuracyFormulaService.criticalChance(LUK, bonus);
    }

    public static double computeCritDamageMultiplier(int LUK, int STR) {
        return DamageFormulaService.critDamageMultiplier();
    }

    // ========================================
    // ASPD (VELOCIDAD DE ATAQUE)
    // ========================================

    public static boolean isRangedWeapon(ItemStack weapon) {
        return WeaponClassificationService.isRangedWeapon(weapon);
    }

    public static int getWeaponBaseASPD(ItemStack weapon) {
        return WeaponClassificationService.baseAspd(weapon);
    }

    public static int computeASPD_RO(int baseWeaponASPD, boolean hasShield, int AGI, int DEX, double bonus) {
        return AspdFormulaService.aspdRo(baseWeaponASPD, hasShield, AGI, DEX, bonus);
    }

    public static double convertASPD_ToAPS(int aspdRO) {
        return AspdFormulaService.attacksPerSecond(aspdRO);
    }

    /**
     * @param mainHand Main hand item.
     * @param offHand Off hand item.
     * @param isOffHandAttack If the current attack originates from the off-hand.
     */
    public static double computeAPSForAttack(ItemStack mainHand, ItemStack offHand, boolean isOffHandAttack, int AGI, int DEX, double bonus) {
        ItemStack activeWeapon = isOffHandAttack ? offHand : mainHand;
        boolean hasShield = (!isOffHandAttack && offHand.getItem() instanceof net.minecraft.world.item.ShieldItem);
        
        int baseASPD = getWeaponBaseASPD(activeWeapon);
        int aspdRO = computeASPD_RO(baseASPD, hasShield, AGI, DEX, bonus);
        return convertASPD_ToAPS(aspdRO);
    }

    public static double computeAPS(ItemStack weapon, boolean hasShield, int AGI, int DEX, double bonus) {
        int baseASPD = getWeaponBaseASPD(weapon);
        int aspdRO = computeASPD_RO(baseASPD, hasShield, AGI, DEX, bonus);
        return convertASPD_ToAPS(aspdRO);
    }

    // ========================================
    // DEFENSA FISICA
    // ========================================

    public static double computeSoftDEF(int VIT, int AGI, int level) {
        return DefenseFormulaService.softDef(VIT);
    }

    public static double computeHardDEF(double armorDEF, int VIT) {
        return DefenseFormulaService.hardDef(armorDEF);
    }

    public static double computePhysDR(double hardDEF) {
        return DefenseFormulaService.physicalDamageReduction(hardDEF);
    }

    public static double applyPhysicalDefense(double rawDamage, double softDEF,
            double hardDEF, double drPhys) {
        return DefenseFormulaService.applyPhysicalDefense(rawDamage, softDEF, drPhys);
    }

    // ========================================
    // DEFENSA MAGICA
    // ========================================

    public static double computeSoftMDEF(int INT, int VIT) {
        return DefenseFormulaService.softMdef(INT, VIT);
    }

    public static double computeHardMDEF(double equipMDEF) {
        return DefenseFormulaService.hardMdef(equipMDEF);
    }

    public static double computeMDEF(int INT, int VIT, int DEX, int level, double equipMDEF) {
        return computeSoftMDEF(INT, VIT) + computeHardMDEF(equipMDEF);
    }

    public static double computeMagicDR(double hardMDEF) {
        return DefenseFormulaService.magicDamageReduction(hardMDEF);
    }

    public static double applyMagicDefense(double rawDamage, double softMDEF, double hardMDEF) {
        return DefenseFormulaService.applyMagicDefense(rawDamage, softMDEF, hardMDEF);
    }

    // ========================================
    // CAST TIME
    // ========================================

    public static double computeCastTime(double baseCast, int DEX, int INT,
            boolean useRenewalFormula) {
        return ResourceFormulaService.variableCastSeconds(baseCast, DEX);
    }

    public static int computeCastDelay(int baseDelayTicks, net.minecraft.world.entity.player.Player player) {
        if (baseDelayTicks <= 0) return 0;
        
        double reduction = 0.0; 
        
        return Math.max(0, (int) Math.round(baseDelayTicks * (1.0 - Math.min(1.0, reduction))));
    }

    // ========================================
    // HP / MANA
    // ========================================

    public static double computeMaxHP(int VIT, int level, String jobId) {
        return ResourceFormulaService.maxHp(VIT, level, jobId);
    }

    public static double computeHPRegen(int VIT, double maxHP) {
        return ResourceFormulaService.hpRegen(VIT, maxHP);
    }

    public static double computeMaxSP(int INT, int level, String jobId) {
        return ResourceFormulaService.maxSp(INT, level, jobId);
    }

    public static double computeSPRegen(int INT, double maxSP) {
        return ResourceFormulaService.spRegen(INT, maxSP);
    }

    // ========================================
    // UTILIDADES DE COMBATE
    // ========================================

    public static boolean rollCritical(double critChance, net.minecraft.util.RandomSource rng) {
        return rng.nextDouble() < critChance;
    }

    public static boolean rollHit(double hitRate, net.minecraft.util.RandomSource rng) {
        return rng.nextDouble() < hitRate;
    }

    public static boolean rollPerfectDodge(double perfectDodge, net.minecraft.util.RandomSource rng) {
        return rng.nextDouble() < perfectDodge;
    }

    public static double calculatePhysicalDamage(
            int attackerSTR, int attackerDEX, int attackerLUK, int attackerLevel,
            int defenderVIT, int defenderAGI, int defenderSTR, double defenderArmorDEF,
            double weaponATK, double bonusATK, double critChance, double critMultBonus,
            java.util.Random rng, boolean isRanged) {

        double totalATK = computeTotalATK(attackerSTR, attackerDEX, attackerLUK,
                attackerLevel, weaponATK, bonusATK, isRanged);

        double damage = computeDamageVariance(totalATK, attackerDEX, attackerLUK, rng);

        boolean isCritical = rng.nextDouble() < critChance;
        if (isCritical) {
            double critMult = computeCritDamageMultiplier(attackerLUK, attackerSTR) + critMultBonus;
            damage *= critMult;
        }

        double softDEF = computeSoftDEF(defenderVIT, defenderAGI, attackerLevel);
        double hardDEF = computeHardDEF(defenderArmorDEF, defenderVIT);
        double drPhys = computePhysDR(hardDEF);

        if (isCritical) {
            softDEF = 0;
        }

        damage = applyPhysicalDefense(damage, softDEF, hardDEF, drPhys);
        return Math.max(1.0, damage);
    }

    public static double calculateMagicDamage(
            int attackerINT, int attackerDEX, int attackerLUK, int attackerLevel,
            int defenderINT, int defenderVIT, double defenderEquipMDEF,
            double spellBase, double bonusMATK,
            java.util.Random rng) {

        double totalMATK = computeTotalMATK(attackerINT, attackerDEX, attackerLUK,
                attackerLevel, spellBase, bonusMATK);

        double variance = 0.9 + rng.nextDouble() * 0.1;
        double damage = totalMATK * variance;

        double softMdef = computeSoftMDEF(defenderINT, defenderVIT);
        double hardMdef = computeHardMDEF(defenderEquipMDEF);
        damage = applyMagicDefense(damage, softMdef, hardMdef);

        return Math.max(1.0, damage);
    }

    // ========================================
    // STATUS AILMENTS (RESISTANCE)
    // ========================================

    public static class TargetStats {
        public final int str;
        public final int dex;
        public final int vit;
        public final int intel;
        public final int luk;
        public final int agi;
        public final int mdef;

        public TargetStats(int str, int dex, int vit, int intel, int luk, int agi, int mdef) {
            this.str = str;
            this.dex = dex;
            this.vit = vit;
            this.intel = intel;
            this.luk = luk;
            this.agi = agi;
            this.mdef = mdef;
        }
    }

    /**
     * Returns a normalized target level when a safe source exists.
     *
     * <p>For mobs, this prefers the shared read surface. Callers that need a vanilla estimate
     * should handle their own neutral estimate path.</p>
     */
    public static OptionalInt tryGetTargetLevel(net.minecraft.world.entity.LivingEntity entity) {
        return TargetCombatProfileResolver.tryGetTargetLevel(entity);
    }

    /**
     * Returns a normalized final HIT value for resolved mob profiles when that source exposes it
     * directly. Callers should keep their existing formula-based estimate when this is empty.
     */
    public static OptionalInt tryGetResolvedMobHit(net.minecraft.world.entity.LivingEntity entity) {
        return MobCombatProfileResolver.tryGetResolvedMobHit(entity);
    }

    /**
     * Returns a normalized final FLEE value for resolved mob profiles when that source exposes it
     * directly. Callers should keep their existing formula-based estimate when this is empty.
     */
    public static OptionalInt tryGetResolvedMobFlee(net.minecraft.world.entity.LivingEntity entity) {
        return MobCombatProfileResolver.tryGetResolvedMobFlee(entity);
    }

    /**
     * Returns a normalized final crit chance for resolved mob profiles when that source exposes it
     * directly. The value is expressed as a 0..1 chance like the rest of the runtime combat layer.
     */
    public static OptionalDouble tryGetResolvedMobCritChance(net.minecraft.world.entity.LivingEntity entity) {
        return MobCombatProfileResolver.tryGetResolvedMobCritChance(entity);
    }

    /**
     * Returns the final resolved ASPD in RO scale when that source exposes it directly.
     */
    public static OptionalInt tryGetResolvedMobAspd(net.minecraft.world.entity.LivingEntity entity) {
        return MobCombatProfileResolver.tryGetResolvedMobAspd(entity);
    }

    /**
     * Converts resolved mob ASPD into a melee attack interval in ticks for vanilla mob AI.
     */
    public static OptionalInt tryGetResolvedMobAttackIntervalTicks(net.minecraft.world.entity.LivingEntity entity) {
        return MobCombatProfileResolver.tryGetResolvedMobAttackIntervalTicks(entity);
    }

    public static TargetStats getTargetStats(net.minecraft.world.entity.LivingEntity entity) {
        var stats = TargetCombatProfileResolver.getTargetStats(entity);
        return new TargetStats(stats.str(), stats.dex(), stats.vit(), stats.intel(), stats.luk(), stats.agi(), stats.mdef());
    }

    public static float computeStunChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.chanceByVitAndLuk(baseChance, ts.vit, ts.luk);
    }

    public static int computeStunDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.durationByVit(baseDurationTicks, ts.vit);
    }

    public static float computeSilenceChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.chanceByIntAndLuk(baseChance, ts.intel, ts.luk);
    }

    public static int computeSilenceDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.durationByInt(baseDurationTicks, ts.intel);
    }

    public static float computeFrozenChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.chanceByMdefAndLuk(baseChance, ts.mdef, ts.luk);
    }

    public static float computeSleepChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.chanceByAgiAndLuk(baseChance, ts.agi, ts.luk);
    }

    public static int computeSleepDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.durationByAgi(baseDurationTicks, ts.agi);
    }
    
    public static float computePoisonChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.chanceByVitAndLuk(baseChance, ts.vit, ts.luk);
    }

    public static int computePoisonDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        return StatusResistanceService.durationByVit(baseDurationTicks, ts.vit);
    }
}
