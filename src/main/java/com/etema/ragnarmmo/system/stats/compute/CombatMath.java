package com.etema.ragnarmmo.system.stats.compute;

import net.minecraft.world.item.*;

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
    private static final double LUK_TO_ATK_DIVISOR = 3.0; // iROWiki Renewal
    private static final double LEVEL_TO_ATK_MULT = 0.25;

    // MATK
    private static final double DEX_TO_MATK_DIVISOR = 5.0; // iROWiki Renewal
    private static final double LUK_TO_MATK_DIVISOR = 3.0; // iROWiki Renewal
    private static final double LEVEL_TO_MATK_MULT = 0.25;

    // Damage Variance
    private static final double MIN_DAMAGE_ROLL = 0.8;
    private static final double DEX_VARIANCE_DIVISOR = 150.0;
    private static final double LUK_VARIANCE_BONUS = 300.0;

    // HIT/FLEE
    public static final double HIT_BASE = 175.0;
    public static final double DEX_TO_HIT_MULT = 1.0;
    public static final double LUK_TO_HIT_DIVISOR = 3.0;
    public static final double LEVEL_TO_HIT_MULT = 1.0;
    public static final double FLEE_BASE = 100.0;
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
    private static final double MDEF_CONSTANT = 80.0;
    private static final double DR_MAGIC_MAX = 0.7;
    private static final double MDEF_SOFT_REDUCTION = 0.25;

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
        return Math.max(min, Math.min(max, value));
    }

    public static double soft(double value, double constant) {
        return value / (value + constant);
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
        double atk = 0;

        if (isRanged) {
            atk += DEX + Math.pow(Math.floor(DEX / 10.0), 2);
            atk += STR / DEX_TO_ATK_DIVISOR;
        } else {
            atk += STR + Math.pow(Math.floor(STR / 10.0), 2);
            atk += DEX / DEX_TO_ATK_DIVISOR;
        }

        atk += Math.floor(LUK / LUK_TO_ATK_DIVISOR);
        atk += level * LEVEL_TO_ATK_MULT;

        return atk;
    }

    public static double computeWeaponATK(double weaponBase, int STR) {
        return weaponBase * (1.0 + STR / 200.0);
    }

    public static double computeTotalATK(int STR, int DEX, int LUK, int level,
            double weaponATK, double bonusATK, boolean isRanged) {
        double status = computeStatusATK(STR, DEX, LUK, level, isRanged);
        double weapon = computeWeaponATK(weaponATK, STR);
        return status + weapon + bonusATK;
    }

    public static double computeDamageVariance(double baseDamage, int DEX, int LUK,
            java.util.Random rng) {
        double dexFactor = clamp(0, 1, DEX / DEX_VARIANCE_DIVISOR);
        double lukBonus = LUK_VARIANCE_BONUS > 0 ? LUK / LUK_VARIANCE_BONUS : 0;

        double floor = MIN_DAMAGE_ROLL + (1 - MIN_DAMAGE_ROLL) * (dexFactor + lukBonus);
        floor = clamp(MIN_DAMAGE_ROLL, 1.0, floor);

        double variance = floor + rng.nextDouble() * (1.0 - floor);
        return baseDamage * variance;
    }

    /**
     * Compute the minimum damage floor (deterministic, no RNG).
     * Used by StatComputer to show the damage range in UI.
     */
    public static double computeDamageVarianceFloor(double baseDamage, int DEX, int LUK) {
        double dexFactor = clamp(0, 1, DEX / DEX_VARIANCE_DIVISOR);
        double lukBonus = LUK_VARIANCE_BONUS > 0 ? LUK / LUK_VARIANCE_BONUS : 0;

        double floor = MIN_DAMAGE_ROLL + (1 - MIN_DAMAGE_ROLL) * (dexFactor + lukBonus);
        floor = clamp(MIN_DAMAGE_ROLL, 1.0, floor);

        return baseDamage * floor;
    }

    // ========================================
    // MATK (ATAQUE MAGICO)
    // ========================================

    public static double computeStatusMATK(int INT, int DEX, int LUK, int level) {
        double matk = INT * 1.5;

        matk += DEX / DEX_TO_MATK_DIVISOR;
        matk += LUK / LUK_TO_MATK_DIVISOR;
        matk += level * LEVEL_TO_MATK_MULT;

        return matk;
    }

    public static double computeTotalMATK(int INT, int DEX, int LUK, int level,
            double spellBase, double bonusMATK) {
        return computeStatusMATK(INT, DEX, LUK, level) + spellBase + bonusMATK;
    }

    // ========================================
    // HIT / FLEE
    // ========================================

    public static double computeHIT(int DEX, int LUK, int level, double bonus) {
        return HIT_BASE
                + DEX * DEX_TO_HIT_MULT
                + Math.floor(LUK / LUK_TO_HIT_DIVISOR)
                + level * LEVEL_TO_HIT_MULT
                + bonus;
    }

    public static double computeFLEE(int AGI, int LUK, int level, double bonus) {
        return FLEE_BASE
                + AGI * AGI_TO_FLEE_MULT
                + Math.floor(LUK / LUK_TO_FLEE_DIVISOR)
                + level * LEVEL_TO_FLEE_MULT
                + bonus;
    }

    public static double computeHitRate(double attackerHIT, double defenderFLEE) {
        double hitRate = HIT_FLEE_FORMULA_CONSTANT + attackerHIT - defenderFLEE;
        return clamp(0.05, 0.95, hitRate / 100.0);
    }

    public static double computePerfectDodge(int LUK) {
        double pd = (1.0 + Math.floor(LUK / PERFECT_DODGE_DIVISOR)) / 100.0;
        return clamp(0.0, PERFECT_DODGE_MAX, pd);
    }

    // ========================================
    // CRITICO
    // ========================================

    public static double computeCritChance(int LUK, int DEX, double bonus) {
        double crit = (1.0 + Math.floor(LUK / LUK_TO_CRIT_DIVISOR)) / 100.0;

        if (DEX_TO_CRIT_DIVISOR > 0) {
            crit += DEX / DEX_TO_CRIT_DIVISOR / 100.0;
        }

        crit += bonus;
        return clamp(0.0, CRIT_MAX, crit);
    }

    public static double computeCritDamageMultiplier(int LUK, int STR) {
        // Base 1.4x (RO Classic). Add slight scaling for "Premium" feel.
        return CRIT_BASE_MULT + (LUK / 200.0) + (STR / 500.0);
    }

    // ========================================
    // ASPD (VELOCIDAD DE ATAQUE)
    // ========================================

    public static boolean isRangedWeapon(ItemStack weapon) {
        if (weapon.isEmpty())
            return false;
        Item item = weapon.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem;
    }

    public static int getWeaponBaseASPD(ItemStack weapon) {
        if (weapon.isEmpty())
            return 180; // Puño

        Item item = weapon.getItem();
        boolean isDagger = weapon.getTags().anyMatch(t -> t.location().getPath().contains("daggers"));
        boolean isMace = weapon.getTags().anyMatch(t -> t.location().getPath().contains("maces"));
        boolean isStaff = weapon.getTags().anyMatch(t -> t.location().getPath().contains("staves"));
        boolean isWand = weapon.getTags().anyMatch(t -> t.location().getPath().contains("wands"));
        boolean isTwoHanded = weapon.getTags().anyMatch(t -> t.location().getPath().contains("two_handed"));

        if (isDagger)
            return 178;
        if (isWand)
            return 172;
        if (isStaff)
            return 165;
        if (isMace)
            return 160;
        if (item instanceof SwordItem)
            return isTwoHanded ? 158 : 170;
        if (item instanceof AxeItem)
            return isTwoHanded ? 150 : 155;
        if (item instanceof ShieldItem)
            return 150;
        if (item instanceof BowItem || item instanceof CrossbowItem)
            return 170;
        if (item instanceof PickaxeItem)
            return 160;
        if (item instanceof ShovelItem)
            return 165;
        if (item instanceof HoeItem)
            return 175;
        if (item instanceof TridentItem)
            return 150;

        return 170;
    }

    public static int computeASPD_RO(int baseWeaponASPD, boolean hasShield, int AGI, int DEX, double bonus) {
        double aspd = baseWeaponASPD
                + AGI * AGI_TO_ASPD
                + DEX * DEX_TO_ASPD
                + bonus;

        if (hasShield) {
            aspd -= SHIELD_ASPD_PENALTY;
        }

        return (int) clamp(ASPD_RO_MIN, ASPD_RO_MAX, aspd);
    }

    public static double convertASPD_ToAPS(int aspdRO) {
        if (aspdRO >= ASPD_RO_MAX)
            return APS_MAX;
        if (aspdRO <= 0)
            return ASPD_MIN;

        double aps = 50.0 / (200.0 - aspdRO);
        return clamp(ASPD_MIN, APS_MAX, aps);
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
        return Math.floor((VIT + level) / 2.0) + Math.floor(AGI / 5.0);
    }

    public static double computeHardDEF(double armorDEF, int VIT) {
        // In classic, it's armor DEF + a small bonus from VIT (Soft DEF is handled separately)
        return armorDEF + (VIT / 10.0);
    }

    public static double computePhysDR(double hardDEF) {
        return clamp(0, DR_PHYS_MAX, hardDEF * HARD_DEF_REDUCTION_MULT);
    }

    public static double applyPhysicalDefense(double rawDamage, double softDEF,
            double hardDEF, double drPhys) {
        double afterHard = rawDamage * (1.0 - drPhys);
        double afterSoft = Math.max(1.0, afterHard - softDEF);
        return afterSoft;
    }

    // ========================================
    // DEFENSA MAGICA
    // ========================================

    public static double computeMDEF(int INT, int VIT, int DEX, int level, double equipMDEF) {
        double soft = INT + (level / 4.0) + ((VIT + level) / 5.0) + (DEX / 5.0);
        double hard = equipMDEF;
        return soft + hard;
    }

    public static double computeMagicDR(double totalMDEF) {
        double dr = soft(totalMDEF, MDEF_CONSTANT);
        return clamp(0, DR_MAGIC_MAX, dr);
    }

    public static double applyMagicDefense(double rawDamage, int INT, double drMagic) {
        double softReduction = INT * MDEF_SOFT_REDUCTION;
        double afterSoft = Math.max(rawDamage * 0.1, rawDamage - softReduction);
        return afterSoft * (1.0 - drMagic);
    }

    // ========================================
    // CAST TIME
    // ========================================

    public static double computeCastTime(double baseCast, int DEX, int INT,
            boolean useRenewalFormula) {
        double fixed = baseCast * CAST_FIXED_RATIO;
        double variable = baseCast * (1.0 - CAST_FIXED_RATIO);

        double castReduction = Math.sqrt(1.0 - Math.min(1.0, (DEX * 2.0 + INT) / 530.0));
        variable *= castReduction;

        return Math.max(CAST_MIN, fixed + variable);
    }

    public static int computeCastDelay(int baseDelayTicks, net.minecraft.world.entity.player.Player player) {
        if (baseDelayTicks <= 0) return 0;
        
        // Global after-cast delay reduction logic placeholder.
        // e.g., Kiel-D-01 card effect (reduction = 0.3), or Bragi's Poem.
        double reduction = 0.0; 
        
        return Math.max(0, (int) Math.round(baseDelayTicks * (1.0 - Math.min(1.0, reduction))));
    }

    // ========================================
    // HP / MANA
    // ========================================

    private static double getJobHpMultiplier(String jobId) {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        return switch (job) {
            case SWORDSMAN, KNIGHT -> 1.5;
            case THIEF, MERCHANT, ASSASSIN, BLACKSMITH -> 1.2;
            case MAGE, WIZARD -> 0.8;
            default -> 1.0;
        };
    }

    private static double getJobSpMultiplier(String jobId) {
        var job = com.etema.ragnarmmo.common.api.jobs.JobType.fromId(jobId);
        return switch (job) {
            case MAGE, WIZARD, ACOLYTE, PRIEST -> 1.5;
            case ARCHER, HUNTER -> 1.2;
            case THIEF, ASSASSIN -> 0.8;
            case SWORDSMAN, KNIGHT -> 0.7;
            default -> 1.0;
        };
    }

    public static double computeMaxHP(int VIT, int level, String jobId) {
        double jobMult = getJobHpMultiplier(jobId);
        double hpBase = 35 + (level * 5 * jobMult);
        return Math.floor(hpBase * (1.0 + VIT / 100.0));
    }

    public static double computeHPRegen(int VIT, double maxHP) {
        double regen = HP_REGEN_BASE + VIT * VIT_TO_HP_REGEN;
        return Math.min(regen, maxHP * HP_REGEN_MAX_PERCENT);
    }

    public static double computeMaxMana(int INT, int level, String jobId) {
        double jobMult = getJobSpMultiplier(jobId);
        double spBase = 100 + ((level - 1) * 3 * jobMult);
        return Math.floor(spBase * (1.0 + INT / 100.0));
    }

    public static double computeManaRegen(int INT, double maxMana) {
        double regen = maxMana * (MANA_REGEN_BASE_PERCENT + INT * INT_TO_MANA_REGEN);
        return Math.min(regen, maxMana * MANA_REGEN_MAX_PERCENT);
    }

    /**
     * SP máximo para clases físicas.
     * Basado en VIT y STR (stamina física), escalado inversamente al Mana.
     * Clases físicas (Swordman, Thief) tienen más SP; mágicas tienen menos.
     */
    public static double computeMaxSP(int VIT, int STR, int level, String jobId) {
        // Inverse multiplier: physical classes with low mana have high SP
        double jobMult = 2.0 - getJobSpMultiplier(jobId); // ~2.3 for swordsman, ~0.5 for mage
        jobMult = Math.max(0.3, Math.min(2.3, jobMult));
        double spBase = 80 + ((level - 1) * 2 * jobMult);
        return Math.floor(spBase * (1.0 + (VIT * 0.6 + STR * 0.4) / 100.0));
    }

    /**
     * SP regen per second — STR/VIT based for physical classes.
     */
    public static double computeSPRegen(int STR, double maxSP) {
        double regen = maxSP * (MANA_REGEN_BASE_PERCENT + STR * 0.0002);
        return Math.min(regen, maxSP * MANA_REGEN_MAX_PERCENT);
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
            double weaponATK, double bonusATK,
            boolean isCritical, java.util.Random rng, boolean isRanged) {

        double totalATK = computeTotalATK(attackerSTR, attackerDEX, attackerLUK,
                attackerLevel, weaponATK, bonusATK, isRanged);

        double damage = computeDamageVariance(totalATK, attackerDEX, attackerLUK, rng);

        if (isCritical) {
            double critMult = computeCritDamageMultiplier(attackerLUK, attackerSTR);
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

        double mdef = computeMDEF(defenderINT, defenderVIT, attackerDEX, attackerLevel, defenderEquipMDEF);
        double drMagic = computeMagicDR(mdef);

        damage = applyMagicDefense(damage, defenderINT, drMagic);

        return Math.max(1.0, damage);
    }

    // ========================================
    // STATUS AILMENTS (RESISTANCE)
    // ========================================

    public static class TargetStats {
        public final int vit;
        public final int intel;
        public final int luk;
        public final int agi;
        public final int mdef;

        public TargetStats(int vit, int intel, int luk, int agi, int mdef) {
            this.vit = vit;
            this.intel = intel;
            this.luk = luk;
            this.agi = agi;
            this.mdef = mdef;
        }
    }

    public static TargetStats getTargetStats(net.minecraft.world.entity.LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player p) {
            var stats = p.getCapability(com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider.CAP).resolve();
            if (stats.isPresent()) {
                var s = stats.get();
                return new TargetStats(
                    s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.VIT),
                    s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.INT),
                    s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.LUK),
                    s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.AGI),
                    0 // MDEF legacy fallback
                );
            }
        } else {
            var stats = com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider.get(entity).resolve();
            if (stats.isPresent()) {
                var s = stats.get();
                return new TargetStats(s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.VIT), s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.INT), s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.LUK), s.get(com.etema.ragnarmmo.common.api.stats.StatKeys.AGI), 0);
            }
        }
        return new TargetStats(1, 1, 1, 1, 0);
    }

    public static float computeStunChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        // RO formula: (1 - VIT/100)
        double res = 1.0 - (ts.vit / 100.0) - (ts.luk / 300.0);
        res = clamp(0.0, 1.0, res);
        return (float) (baseChance * res);
    }

    public static int computeStunDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        double res = 1.0 - (ts.vit / 100.0);
        res = clamp(0.0, 1.0, res);
        return (int) (baseDurationTicks * res);
    }

    public static float computeSilenceChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        // RO Resistance: INT
        double res = 1.0 - (ts.intel / 100.0) - (ts.luk / 300.0);
        res = clamp(0.0, 1.0, res);
        return (float) (baseChance * res);
    }

    public static int computeSilenceDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        double res = 1.0 - (ts.intel / 100.0);
        res = clamp(0.0, 1.0, res);
        return (int) (baseDurationTicks * res);
    }

    public static float computeFrozenChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        // RO Resistance: MDEF (Hard)
        double res = 1.0 - (ts.mdef / 100.0) - (ts.luk / 300.0);
        res = clamp(0.0, 1.0, res);
        return (float) (baseChance * res);
    }

    public static float computeSleepChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        // RO Resistance: AGI
        double res = 1.0 - (ts.agi / 100.0) - (ts.luk / 300.0);
        res = clamp(0.0, 1.0, res);
        return (float) (baseChance * res);
    }

    public static int computeSleepDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        double res = 1.0 - (ts.agi / 100.0);
        res = clamp(0.0, 1.0, res);
        return (int) (baseDurationTicks * res);
    }
    
    public static float computePoisonChance(float baseChance, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        double res = 1.0 - (ts.vit / 100.0) - (ts.luk / 300.0);
        res = clamp(0.0, 1.0, res);
        return (float) (baseChance * res);
    }

    public static int computePoisonDuration(int baseDurationTicks, net.minecraft.world.entity.LivingEntity target) {
        TargetStats ts = getTargetStats(target);
        double res = 1.0 - (ts.vit / 100.0);
        res = clamp(0.0, 1.0, res);
        return (int) (baseDurationTicks * res);
    }
}
