package com.etema.ragnarmmo.system.stats.compute;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.events.StatComputeEvent;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import com.etema.ragnarmmo.roitems.runtime.RoItemNbtHelper;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;

/**
 * StatComputer - Calculates derived stats by delegating to CombatMath.
 * All base formulas live in CombatMath (single source of truth).
 */
public final class StatComputer {

    private StatComputer() {}

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
    private static final ResourceLocation CRITICAL_SHOT = new ResourceLocation("ragnarmmo", "critical_shot");
    private static final ResourceLocation SKIN_TEMPERING = new ResourceLocation("ragnarmmo", "skin_tempering");

    private static final double FLEE_DISPLAY_MAX = 0.60;

    private record SkillContext(
            int sword, int dagger, int mace, int bow, int weaponTrainer,
            int faith, int arcaneRegen, int accuracy, int manaControl,
            int spear, int katar, int rightHand, int leftHand, int sonicAccel,
            int researchWeaponry, int skinTempering, int criticalShot
    ) {}

    public static DerivedStats compute(Player p, IPlayerStats s,
            double weaponATK, double weaponAps,
            double spellBase, double armorEff, double baseCast) {
        double equipMDEF = getEquipMagicDefense(p);
        return computeInternal(p, s, weaponATK, weaponAps, spellBase, armorEff, equipMDEF, baseCast);
    }

    private static DerivedStats computeInternal(Player p, IPlayerStats s,
            double weaponATK, double weaponAps,
            double spellBase, double armorEff, double equipMDEF, double baseCast) {
        int STR = (int) Math.round(StatAttributes.getTotal(p, StatKeys.STR));
        int AGI = (int) Math.round(StatAttributes.getTotal(p, StatKeys.AGI));
        int VIT = (int) Math.round(StatAttributes.getTotal(p, StatKeys.VIT));
        int INT = (int) Math.round(StatAttributes.getTotal(p, StatKeys.INT));
        int DEX = (int) Math.round(StatAttributes.getTotal(p, StatKeys.DEX));
        int LUK = (int) Math.round(StatAttributes.getTotal(p, StatKeys.LUK));
        int LVL = s.getLevel();

        SkillContext ctx = fetchSkillContext(p);
        DerivedStats d = new DerivedStats();

        applyPhysicalOffense(p, d, STR, DEX, LUK, LVL, weaponATK, ctx);
        applyMagicalOffense(p, d, INT, DEX, LUK, LVL, spellBase);
        applyDefense(p, d, VIT, AGI, INT, DEX, LUK, LVL, armorEff, equipMDEF, ctx);
        
        applyResources(p, d, STR, VIT, INT, LVL, s, ctx);
        applyMiscStats(p, d, AGI, DEX, INT, LUK, STR, baseCast);

        MinecraftForge.EVENT_BUS.post(new StatComputeEvent(p, s, d));
        return d;
    }

    /**
     * Compatibility overload for legacy callers using 8 parameters.
     */
    public static DerivedStats compute(Player p, IPlayerStats s,
            double weaponATK, double weaponAps,
            double spellBase, double armorEff, double equipMdef, double legacyScale) {
        return computeInternal(p, s, weaponATK, weaponAps, spellBase, armorEff, equipMdef, legacyScale);
    }

    private static double getEquipMagicDefense(Player p) {
        double equipMDEF = 0.0;
        AttributeInstance attr = p.getAttribute(RagnarAttributes.MAGIC_DEFENSE.get());
        if (attr != null) {
            equipMDEF += attr.getValue();
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = p.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int refine = RoItemNbtHelper.getRefineLevel(stack);
            if (refine >= 5) {
                equipMDEF += (refine - 4);
            }
        }

        return equipMDEF;
    }

    private static SkillContext fetchSkillContext(Player p) {
        var skillsOpt = PlayerSkillsProvider.get(p).resolve();
        if (skillsOpt.isEmpty()) {
            return new SkillContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        var sk = skillsOpt.get();
        return new SkillContext(
                sk.getSkillLevel(SWORD_MASTERY), sk.getSkillLevel(DAGGER_MASTERY),
                sk.getSkillLevel(MACE_MASTERY), sk.getSkillLevel(BOW_MASTERY),
                sk.getSkillLevel(WEAPON_TRAINER), sk.getSkillLevel(FAITH),
                sk.getSkillLevel(ARCANE_REGENERATION), sk.getSkillLevel(ACCURACY_TRAINING),
                sk.getSkillLevel(MANA_CONTROL), sk.getSkillLevel(SPEAR_MASTERY),
                sk.getSkillLevel(KATAR_MASTERY), sk.getSkillLevel(RIGHTHAND_MASTERY),
                sk.getSkillLevel(LEFTHAND_MASTERY), sk.getSkillLevel(SONIC_ACCELERATION),
                sk.getSkillLevel(RESEARCH_WEAPONRY), sk.getSkillLevel(SKIN_TEMPERING),
                sk.getSkillLevel(CRITICAL_SHOT)
        );
    }

    private static void applyPhysicalOffense(Player p, DerivedStats d, int STR, int DEX, int LUK, int LVL, double weaponATK, SkillContext ctx) {
        ItemStack main = p.getMainHandItem();
        boolean isRanged = CombatMath.isRangedWeapon(main);
        double statusATK = CombatMath.computeStatusATK(STR, DEX, LUK, LVL, isRanged);
        
        // STRICT MASTERY FILTERING
        double masteryBonus = 0;
        if (isSword(main)) masteryBonus += ctx.sword * 4.0;
        else if (isDagger(main)) masteryBonus += ctx.dagger * 4.0;
        else if (isMace(main)) masteryBonus += ctx.mace * 4.0;
        else if (isBow(main)) masteryBonus += ctx.bow * 4.0;
        else if (isSpear(main)) masteryBonus += ctx.spear * 4.0;
        else if (isKatar(main)) masteryBonus += ctx.katar * 4.0;
        
        if (isAxeOrMace(main)) masteryBonus += (ctx.researchWeaponry * 3.0);
        
        double skillATK = masteryBonus + (ctx.weaponTrainer * 1.5);
        if (isDualWielding(p)) skillATK += (ctx.rightHand * 3.0) + (ctx.leftHand * 3.0);
        
        double dmgPhysRaw = CombatMath.computeWeaponATK(weaponATK, STR, DEX, isRanged) + statusATK + skillATK;
        d.physicalAttack = dmgPhysRaw;
        d.physicalAttackMin = Math.max(0, CombatMath.computeDamageVarianceFloor(dmgPhysRaw, DEX, LUK));
        d.physicalAttackMax = dmgPhysRaw;
        
        double skillHitBonus = (ctx.accuracy * 3.0);
        if (isBow(main)) skillHitBonus += (ctx.bow * 2.0);
        if (ctx.sonicAccel > 0 && isKatar(main)) skillHitBonus += (ctx.sonicAccel * 5.0);
        if (ctx.researchWeaponry > 0 && isAxeOrMace(main)) skillHitBonus += (ctx.researchWeaponry * 2.0);
        
        d.accuracy = CombatMath.computeHIT(DEX, LUK, LVL, skillHitBonus);
        
        double skillCritChance = 0;
        double skillCritDamage = 0;
        if (isRanged) {
            skillCritChance += ctx.criticalShot * 0.01;
            skillCritDamage += ctx.criticalShot * 0.01;
        }
        
        d.criticalChance = CombatMath.computeCritChance(LUK, DEX, getCritChance(p) + skillCritChance);
        d.criticalDamageMultiplier = CombatMath.computeCritDamageMultiplier(LUK, STR) + getCritDamage(p) + skillCritDamage;
        d.perfectDodge = CombatMath.computePerfectDodge(LUK);
    }

    private static void applyMagicalOffense(Player p, DerivedStats d, int INT, int DEX, int LUK, int LVL, double spellBase) {
        d.magicAttack = Math.max(0, spellBase + CombatMath.computeStatusMATK(INT, DEX, LUK, LVL));
    }

    private static void applyDefense(Player p, DerivedStats d, int VIT, int AGI, int INT, int DEX, int LUK, int LVL, double armorEff, double equipMDEF, SkillContext ctx) {
        double hardDEF = CombatMath.computeHardDEF(armorEff, VIT);
        d.physicalDamageReduction = CombatMath.computePhysDR(hardDEF) + (ctx.skinTempering * 0.01);
        d.defense = hardDEF;
        double mdefBase = CombatMath.computeMDEF(INT, VIT, DEX, LVL, equipMDEF);
        d.magicDamageReduction = CombatMath.computeMagicDR(mdefBase);
        d.magicDefense = mdefBase;
        d.flee = Math.min(CombatMath.computeFLEE(AGI, LUK, LVL, 0), FLEE_DISPLAY_MAX * 100.0);
    }

    private static void applyResources(Player p, DerivedStats d, int STR, int VIT, int INT, int LVL, IPlayerStats s, SkillContext ctx) {
        double hpMax = CombatMath.computeMaxHP(VIT, LVL, s.getJobId()) + (ctx.faith * 10.0);
        d.maxHealth = hpMax;
        d.healthRegenPerSecond = Math.max(0, CombatMath.computeHPRegen(VIT, hpMax));
        double manaMax = CombatMath.computeMaxMana(INT, LVL, s.getJobId());
        if (ctx.manaControl > 0) manaMax *= (1.0 + (ctx.manaControl * 0.03));
        d.maxMana = manaMax;
        d.manaRegenPerSecond = CombatMath.computeManaRegen(INT, manaMax) + (ctx.arcaneRegen * 0.1);
        double spMax = CombatMath.computeMaxSP(VIT, STR, LVL, s.getJobId());
        d.maxSP = spMax;
        d.spRegenPerSecond = CombatMath.computeSPRegen(STR, spMax);
    }

    private static void applyMiscStats(Player p, DerivedStats d, int AGI, int DEX, int INT, int LUK, int STR, double baseCast) {
        boolean hasShield = p.getOffhandItem().canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK) || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem;
        double aspdBonus = (p.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED) && isAxeOrMace(p.getMainHandItem())) ? 6.0 : 0;
        int aspdRo = CombatMath.computeASPD_RO(CombatMath.getWeaponBaseASPD(p.getMainHandItem()), hasShield, AGI, DEX, aspdBonus);
        double aps = CombatMath.convertASPD_ToAPS(aspdRo);
        d.attackSpeed = aspdRo;
        d.globalCooldown = aps > 0 ? 1.0 / aps : 0.0;
        d.castTime = CombatMath.computeCastTime(baseCast, DEX, INT, false);
        d.lifeSteal = getLifeSteal(p);
    }

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
        return !p.getMainHandItem().isEmpty() && !p.getOffhandItem().isEmpty() && p.getMainHandItem().getItem() instanceof net.minecraft.world.item.TieredItem && p.getOffhandItem().getItem() instanceof net.minecraft.world.item.TieredItem && !(p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem);
    }

    private static boolean isAxeOrMace(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof net.minecraft.world.item.AxeItem || stack.getTags().anyMatch(t -> t.location().getPath().contains("maces")));
    }

    private static boolean isSword(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.SwordItem && !isKatar(stack);
    }

    private static boolean isDagger(net.minecraft.world.item.ItemStack stack) {
        return stack.getTags().anyMatch(t -> t.location().getPath().contains("daggers"));
    }

    private static boolean isMace(net.minecraft.world.item.ItemStack stack) {
        return stack.getTags().anyMatch(t -> t.location().getPath().contains("maces"));
    }

    private static boolean isBow(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BowItem;
    }

    private static boolean isSpear(net.minecraft.world.item.ItemStack stack) {
        return stack.getTags().anyMatch(t -> t.location().getPath().contains("spears"));
    }
}
