package com.etema.ragnarmmo.system.stats.compute;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.events.StatComputeEvent;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;

import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;

/**
 * StatComputer - Calculates derived stats by delegating to CombatMath.
 * All base formulas live in CombatMath (single source of truth).
 * This class adds skill bonuses and extended attribute reads on top.
 */
public final class StatComputer {

    private StatComputer() {
    }

    // Skill IDs
    private static final ResourceLocation SWORD_MASTERY = new ResourceLocation("ragnarmmo", "sword_mastery");
    private static final ResourceLocation DAGGER_MASTERY = new ResourceLocation("ragnarmmo", "dagger_mastery");
    private static final ResourceLocation MACE_MASTERY = new ResourceLocation("ragnarmmo", "mace_mastery");
    private static final ResourceLocation BOW_MASTERY = new ResourceLocation("ragnarmmo", "bow_mastery");
    private static final ResourceLocation WEAPON_TRAINER = new ResourceLocation("ragnarmmo", "weapon_trainer");
    private static final ResourceLocation FAITH = new ResourceLocation("ragnarmmo", "faith");
    private static final ResourceLocation ARCANE_REGENERATION = new ResourceLocation("ragnarmmo", "arcane_regeneration");
    private static final ResourceLocation ACCURACY_TRAINING = new ResourceLocation("ragnarmmo", "accuracy_training");
    private static final ResourceLocation MANA_CONTROL = new ResourceLocation("ragnarmmo", "mana_control");
    private static final ResourceLocation SPEAR_MASTERY = new ResourceLocation("ragnarmmo", "spear_mastery");
    private static final ResourceLocation KATAR_MASTERY = new ResourceLocation("ragnarmmo", "katar_mastery");
    private static final ResourceLocation RIGHTHAND_MASTERY = new ResourceLocation("ragnarmmo", "righthand_mastery");
    private static final ResourceLocation LEFTHAND_MASTERY = new ResourceLocation("ragnarmmo", "lefthand_mastery");
    private static final ResourceLocation SONIC_ACCELERATION = new ResourceLocation("ragnarmmo", "sonic_acceleration");
    private static final ResourceLocation RESEARCH_WEAPONRY = new ResourceLocation("ragnarmmo", "research_weaponry");
    private static final ResourceLocation SKIN_TEMPERING = new ResourceLocation("ragnarmmo", "skin_tempering");

    // Flee display cap (UI-only): flee values above 60% are capped for the HUD.
    // Raw flee is still used in combat calculations (CombatMath.computeHitRate).
    private static final double FLEE_DISPLAY_MAX = 0.60;

    // ========================================================================
    // MAIN COMPUTE METHOD
    // ========================================================================

    public static DerivedStats compute(Player p, IPlayerStats s,
            double weaponATK, double apsArma,
            double spellBase, double armorEff, double baseCast) {

        int STR = (int) Math.round(StatAttributes.getTotal(p, StatKeys.STR));
        int AGI = (int) Math.round(StatAttributes.getTotal(p, StatKeys.AGI));
        int VIT = (int) Math.round(StatAttributes.getTotal(p, StatKeys.VIT));
        int INT = (int) Math.round(StatAttributes.getTotal(p, StatKeys.INT));
        int DEX = (int) Math.round(StatAttributes.getTotal(p, StatKeys.DEX));
        int LUK = (int) Math.round(StatAttributes.getTotal(p, StatKeys.LUK));
        int LVL = s.getLevel();

        // --- Skill Bonuses retrieval ---
        int swordLvl = 0, daggerLvl = 0, maceLvl = 0, bowLvl = 0;
        int weaponTrainerLvl = 0;
        int faithLvl = 0, arcaneRegenLvl = 0, accuracyLvl = 0;
        int manaControlLvl = 0;
        int spearLvl = 0;
        int katarMasteryLvl = 0, rightHandLvl = 0, leftHandLvl = 0, sonicAccelLvl = 0;
        int researchWeaponryLvl = 0, skinTemperingLvl = 0;

        var skillsOpt = PlayerSkillsProvider.get(p);
        if (skillsOpt.isPresent()) {
            var skills = skillsOpt.orElseThrow(() -> new IllegalStateException("SkillManager absent after isPresent()"));
            swordLvl = skills.getSkillLevel(SWORD_MASTERY);
            daggerLvl = skills.getSkillLevel(DAGGER_MASTERY);
            maceLvl = skills.getSkillLevel(MACE_MASTERY);
            bowLvl = skills.getSkillLevel(BOW_MASTERY);
            weaponTrainerLvl = skills.getSkillLevel(WEAPON_TRAINER);
            faithLvl = skills.getSkillLevel(FAITH);
            arcaneRegenLvl = skills.getSkillLevel(ARCANE_REGENERATION);
            accuracyLvl = skills.getSkillLevel(ACCURACY_TRAINING);
            manaControlLvl = skills.getSkillLevel(MANA_CONTROL);
            spearLvl = skills.getSkillLevel(SPEAR_MASTERY);
            katarMasteryLvl = skills.getSkillLevel(KATAR_MASTERY);
            rightHandLvl = skills.getSkillLevel(RIGHTHAND_MASTERY);
            leftHandLvl = skills.getSkillLevel(LEFTHAND_MASTERY);
            sonicAccelLvl = skills.getSkillLevel(SONIC_ACCELERATION);
            researchWeaponryLvl = skills.getSkillLevel(RESEARCH_WEAPONRY);
            skinTemperingLvl = skills.getSkillLevel(SKIN_TEMPERING);
        }

        // --- Physical Attack (delegate to CombatMath + skill bonuses) ---
        boolean isRanged = CombatMath.isRangedWeapon(p.getMainHandItem());
        double statusATK = CombatMath.computeStatusATK(STR, DEX, LUK, LVL, isRanged);

        // Mastery Bonuses
        double masteryBonus = (swordLvl + daggerLvl + maceLvl + bowLvl + spearLvl) * 0.5;

        // Assassin Katar Mastery
        if (isKatar(p.getMainHandItem())) {
            masteryBonus += (katarMasteryLvl * 3.0);
        }

        // Blacksmith Research Weaponry
        if (isAxeOrMace(p.getMainHandItem())) {
            masteryBonus += (researchWeaponryLvl * 2.0);
        }

        double skillATK = masteryBonus + (weaponTrainerLvl * 0.5);

        // Dual Wield Efficiency (Assassin)
        if (isDualWielding(p)) {
            skillATK += (rightHandLvl * 2.0) + (leftHandLvl * 2.0);
        }

        double dmgPhysRaw = CombatMath.computeWeaponATK(weaponATK, STR) + statusATK + skillATK;
        double dmgPhysFloor = CombatMath.computeDamageVarianceFloor(dmgPhysRaw, DEX, LUK);

        // --- Magic Attack ---
        double statusMATK = CombatMath.computeStatusMATK(INT, DEX, LUK, LVL);
        double dmgMagic = spellBase + statusMATK;

        // --- Hit / Flee ---
        double skillHitBonus = (bowLvl * 1.0) + (accuracyLvl * 2.0);
        if (sonicAccelLvl > 0 && isKatar(p.getMainHandItem())) {
            skillHitBonus += (sonicAccelLvl * 5.0);
        }
        if (researchWeaponryLvl > 0 && isAxeOrMace(p.getMainHandItem())) {
            skillHitBonus += (researchWeaponryLvl * 2.0);
        }
        double hitA = CombatMath.computeHIT(DEX, LUK, LVL, skillHitBonus);
        double fleeD = CombatMath.computeFLEE(AGI, LUK, LVL, 0);

        // --- Critical (base + extended attribute bonus) ---
        double extraCrit = getCritChance(p);
        double pCrit = CombatMath.computeCritChance(LUK, DEX, extraCrit);

        // --- Critical Damage (base + extended attribute bonus) ---
        double extraCritDmg = getCritDamage(p);
        double critDmgMult = CombatMath.computeCritDamageMultiplier(LUK, STR) + extraCritDmg;

        // --- Perfect Dodge ---
        double perfectDodge = CombatMath.computePerfectDodge(LUK);

        // --- Attack Speed ---
        boolean hasShield = p.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;

        double aspdBonus = 0;
        if (p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED) && isAxeOrMace(p.getMainHandItem())) {
            // Adrenaline Rush: Approx +5-7 flat ASPD (30% reduction in delay logic)
            aspdBonus += 6.0;
        }

        int aspdRo = CombatMath.computeASPD_RO(CombatMath.getWeaponBaseASPD(p.getMainHandItem()), hasShield, AGI, DEX,
                aspdBonus);
        double aps = CombatMath.convertASPD_ToAPS(aspdRo);

        // --- Cast Time ---
        double castTime = CombatMath.computeCastTime(baseCast, DEX, INT, false);

        // --- Physical Defense ---
        double hardDEF = CombatMath.computeHardDEF(armorEff, VIT);

        // Blacksmith Skin Tempering (Physical DR bonus)
        double extraDR = (skinTemperingLvl * 0.01); // 1% per level
        double drPhys = CombatMath.computePhysDR(hardDEF) + extraDR;

        // --- Magic Defense ---
        double mdefBase = CombatMath.computeMDEF(INT, VIT, DEX, LVL, 0);
        double drMagic = CombatMath.computeMagicDR(mdefBase);

        // --- HP (base + skill bonuses) ---
        double hpMax = CombatMath.computeMaxHP(VIT, LVL, s.getJobId());
        hpMax += (faithLvl * 10.0);

        double hpRegenPerSec = CombatMath.computeHPRegen(VIT, hpMax);

        // --- Mana (base + skill bonuses) ---
        double manaMax = CombatMath.computeMaxMana(INT, LVL, s.getJobId());
        if (manaControlLvl > 0) {
            manaMax *= (1.0 + (manaControlLvl * 0.03));
        }

        double manaRegenPerSec = CombatMath.computeManaRegen(INT, manaMax);
        manaRegenPerSec += (arcaneRegenLvl * 0.1);

        // --- SP (physical stamina resource — separate from Mana) ---
        double spMax = CombatMath.computeMaxSP(VIT, STR, LVL, s.getJobId());
        double spRegenPerSec = CombatMath.computeSPRegen(STR, spMax);

        // --- Life Steal ---
        double lifeSteal = getLifeSteal(p);

        // --- Build result ---
        DerivedStats d = new DerivedStats();
        d.physicalAttack = dmgPhysRaw;
        d.physicalAttackMin = Math.max(0, dmgPhysFloor);
        d.physicalAttackMax = dmgPhysRaw;
        d.magicAttack = Math.max(0, dmgMagic);
        d.accuracy = hitA;
        d.criticalChance = pCrit;
        d.criticalDamageMultiplier = critDmgMult;
        // Flee: cap the display value to 60% for UI; raw value is used in CombatMath
        d.flee = Math.min(fleeD, FLEE_DISPLAY_MAX * 100.0);
        d.perfectDodge = perfectDodge;
        d.attackSpeed = aspdRo;
        d.castTime = castTime;
        d.globalCooldown = aps > 0 ? 1.0 / aps : 0.0;
        d.physicalDamageReduction = drPhys;
        d.magicDamageReduction = drMagic;
        d.maxHealth = hpMax;
        d.healthRegenPerSecond = Math.max(0, hpRegenPerSec);
        d.maxMana = manaMax;
        d.manaRegenPerSecond = manaRegenPerSec;
        d.maxSP = spMax;
        d.spRegenPerSecond = spRegenPerSec;
        d.defense = hardDEF;
        d.magicDefense = mdefBase;
        d.lifeSteal = lifeSteal;

        MinecraftForge.EVENT_BUS.post(new StatComputeEvent(p, s, d));
        return d;
    }

    // ========================================================================
    // EXTENDED ATTRIBUTE READERS (own RagnarAttributes registry)
    // ========================================================================

    private static double getCritChance(Player p) {
        AttributeInstance attr = p.getAttribute(RagnarAttributes.CRIT_CHANCE.get());
        return attr != null ? attr.getValue() : 0.0;
    }

    private static double getCritDamage(Player p) {
        AttributeInstance attr = p.getAttribute(RagnarAttributes.CRIT_DAMAGE.get());
        return attr != null ? Math.max(0, attr.getValue() - 1.5) : 0.0;
    }

    private static double getLifeSteal(Player p) {
        AttributeInstance attr = p.getAttribute(RagnarAttributes.LIFE_STEAL.get());
        return attr != null ? attr.getValue() : 0.0;
    }

    private static boolean isKatar(net.minecraft.world.item.ItemStack stack) {
        return stack.getTags().anyMatch(t -> t.location().getPath().contains("katars"));
    }

    private static boolean isDualWielding(Player p) {
        net.minecraft.world.item.ItemStack main = p.getMainHandItem();
        net.minecraft.world.item.ItemStack off = p.getOffhandItem();
        return !main.isEmpty() && !off.isEmpty()
                && main.getItem() instanceof net.minecraft.world.item.TieredItem
                && off.getItem() instanceof net.minecraft.world.item.TieredItem
                && !(off.getItem() instanceof net.minecraft.world.item.ShieldItem);
    }

    private static boolean isAxeOrMace(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty())
            return false;
        net.minecraft.world.item.Item item = stack.getItem();
        return item instanceof net.minecraft.world.item.AxeItem
                || stack.getTags().anyMatch(t -> t.location().getPath().contains("maces"));
    }

    /**
     * Public accessor for magical ATK calculation (used by skill effects).
     */
    public static double computeMagicalATK(int INT, int DEX, int LUK, int LVL) {
        return CombatMath.computeStatusMATK(INT, DEX, LUK, LVL);
    }
}
