package com.etema.ragnarmmo.system.stats.compute;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.common.api.compute.DerivedStats;
import com.etema.ragnarmmo.common.api.events.StatComputeEvent;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

/**
 * Authoritative derived-stat resolver.
 * The formulas themselves live in {@link CombatMath}; this class wires player
 * state, equipment, and passive skills into that formula layer.
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
    private static final ResourceLocation VULTURES_EYE = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "vultures_eye");
    private static final ResourceLocation IMPROVE_DODGE =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "improve_dodge");

    private record SkillContext(
            int sword, int dagger, int mace, int bow, int weaponTrainer,
            int faith, int arcaneRegen, int accuracy, int manaControl,
            int spear, int katar, int rightHand, int leftHand, int sonicAccel,
            int researchWeaponry, int skinTempering, int criticalShot, int vulturesEye, int improveDodge
    ) {}

    public static DerivedStats compute(Player player, IPlayerStats stats, EquipmentStatSnapshot snapshot) {
        EquipmentStatSnapshot resolvedSnapshot = snapshot != null ? snapshot : EquipmentStatSnapshot.capture(player);

        int str = (int) Math.round(StatAttributes.getTotal(player, StatKeys.STR));
        int agi = (int) Math.round(StatAttributes.getTotal(player, StatKeys.AGI));
        int vit = (int) Math.round(StatAttributes.getTotal(player, StatKeys.VIT));
        int intel = (int) Math.round(StatAttributes.getTotal(player, StatKeys.INT));
        int dex = (int) Math.round(StatAttributes.getTotal(player, StatKeys.DEX));
        int luk = (int) Math.round(StatAttributes.getTotal(player, StatKeys.LUK));
        int level = stats.getLevel();

        SkillContext skillContext = fetchSkillContext(player);
        DerivedStats derived = new DerivedStats();

        applyPhysicalOffense(player, derived, str, dex, luk, level, resolvedSnapshot, skillContext);
        applyMagicalOffense(derived, intel, resolvedSnapshot.weaponMagicAtk());
        applyDefense(derived, vit, agi, intel, luk, level, resolvedSnapshot, skillContext);
        applyResources(derived, str, vit, intel, level, stats, skillContext);
        applyMiscStats(player, derived, agi, dex, resolvedSnapshot);
        applyMinecraftAdaptationStats(derived, skillContext);

        MinecraftForge.EVENT_BUS.post(new StatComputeEvent(player, stats, derived));
        return derived;
    }

    private static SkillContext fetchSkillContext(Player player) {
        var skillsOpt = PlayerSkillsProvider.get(player).resolve();
        if (skillsOpt.isEmpty()) {
            return new SkillContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        var skills = skillsOpt.get();
        return new SkillContext(
                skills.getSkillLevel(SWORD_MASTERY), skills.getSkillLevel(DAGGER_MASTERY),
                skills.getSkillLevel(MACE_MASTERY), skills.getSkillLevel(BOW_MASTERY),
                skills.getSkillLevel(WEAPON_TRAINER), skills.getSkillLevel(FAITH),
                skills.getSkillLevel(ARCANE_REGENERATION), skills.getSkillLevel(ACCURACY_TRAINING),
                skills.getSkillLevel(MANA_CONTROL), skills.getSkillLevel(SPEAR_MASTERY),
                skills.getSkillLevel(KATAR_MASTERY), skills.getSkillLevel(RIGHTHAND_MASTERY),
                skills.getSkillLevel(LEFTHAND_MASTERY), skills.getSkillLevel(SONIC_ACCELERATION),
                skills.getSkillLevel(RESEARCH_WEAPONRY), skills.getSkillLevel(SKIN_TEMPERING),
                skills.getSkillLevel(CRITICAL_SHOT), skills.getSkillLevel(VULTURES_EYE),
                skills.getSkillLevel(IMPROVE_DODGE)
        );
    }

    private static void applyPhysicalOffense(Player player, DerivedStats derived, int str, int dex, int luk, int level,
            EquipmentStatSnapshot snapshot, SkillContext ctx) {
        ItemStack main = player.getMainHandItem();
        boolean isRanged = snapshot.rangedWeapon();
        double statusAtk = CombatMath.computeStatusATK(str, dex, luk, level, isRanged);

        double masteryBonus = 0.0D;
        if (isSword(main)) masteryBonus += ctx.sword * 4.0D;
        else if (isDagger(main)) masteryBonus += ctx.dagger * 4.0D;
        else if (isMace(main)) masteryBonus += ctx.mace * 4.0D;
        else if (isBow(main)) masteryBonus += ctx.bow * 4.0D;
        else if (isSpear(main)) masteryBonus += ctx.spear * 4.0D;
        else if (isKatar(main)) masteryBonus += ctx.katar * 4.0D;

        if (isAxeOrMace(main)) masteryBonus += ctx.researchWeaponry * 3.0D;

        double skillAtk = masteryBonus + (ctx.weaponTrainer * 1.5D);
        if (AttackHandResolver.isDualWielding(player)) {
            skillAtk += (ctx.rightHand * 3.0D) + (ctx.leftHand * 3.0D);
        }

        double physicalAttack = CombatMath.computeWeaponATK(snapshot.weaponAtk(), str, dex, isRanged) + statusAtk + skillAtk;
        derived.physicalAttack = physicalAttack;
        derived.physicalAttackMin = Math.max(0.0D, CombatMath.computeDamageVarianceFloor(physicalAttack, dex, luk));
        derived.physicalAttackMax = physicalAttack;

        double hitBonus = ctx.accuracy * 3.0D;
        if (isBow(main)) hitBonus += ctx.bow * 2.0D;
        if (isBow(main) && ctx.vulturesEye > 0) {
            hitBonus += getSkillLevelDouble(VULTURES_EYE, ctx.vulturesEye, "accuracy_bonus", ctx.vulturesEye);
        }
        if (ctx.sonicAccel > 0 && isKatar(main)) hitBonus += ctx.sonicAccel * 5.0D;
        if (ctx.researchWeaponry > 0 && isAxeOrMace(main)) hitBonus += ctx.researchWeaponry * 2.0D;
        derived.accuracy = CombatMath.computeHIT(dex, luk, level, hitBonus);

        double skillCritChance = 0.0D;
        double skillCritDamage = 0.0D;
        if (isRanged) {
            skillCritChance += ctx.criticalShot * 0.01D;
            skillCritDamage += ctx.criticalShot * 0.01D;
        }

        derived.criticalChance = CombatMath.computeCritChance(luk, dex, getCritChance(player) + skillCritChance);
        derived.criticalDamageMultiplier = CombatMath.computeCritDamageMultiplier(luk, str) + getCritDamage(player) + skillCritDamage;
        derived.perfectDodge = CombatMath.computePerfectDodge(luk);
    }

    private static void applyMagicalOffense(DerivedStats derived, int intel, double weaponMagicAtk) {
        derived.magicAttackMin = Math.max(0.0D, weaponMagicAtk + CombatMath.computeStatusMATKMin(intel));
        derived.magicAttackMax = Math.max(derived.magicAttackMin, weaponMagicAtk + CombatMath.computeStatusMATKMax(intel));
        derived.magicAttack = (derived.magicAttackMin + derived.magicAttackMax) * 0.5D;
    }

    private static void applyDefense(DerivedStats derived, int vit, int agi, int intel, int luk, int level,
            EquipmentStatSnapshot snapshot, SkillContext ctx) {
        double hardDef = CombatMath.computeHardDEF(snapshot.armorHardDef(), vit);
        double softDef = CombatMath.computeSoftDEF(vit, agi, level);
        derived.hardDefense = hardDef;
        derived.softDefense = softDef;
        derived.defense = hardDef + softDef;
        derived.physicalDamageReduction = CombatMath.computePhysDR(hardDef) + (ctx.skinTempering * 0.01D);

        double hardMdef = CombatMath.computeHardMDEF(snapshot.armorHardMdef());
        double softMdef = CombatMath.computeSoftMDEF(intel, vit);
        derived.hardMagicDefense = hardMdef;
        derived.softMagicDefense = softMdef;
        derived.magicDefense = hardMdef + softMdef;
        derived.magicDamageReduction = CombatMath.computeMagicDR(hardMdef);
        double fleeBonus = ctx.improveDodge > 0
                ? getSkillLevelDouble(IMPROVE_DODGE, ctx.improveDodge, "flee_bonus", ctx.improveDodge * 3.0D)
                : 0.0D;
        derived.flee = CombatMath.computeFLEE(agi, luk, level, fleeBonus);
    }

    private static void applyResources(DerivedStats derived, int str, int vit, int intel, int level,
            IPlayerStats stats, SkillContext ctx) {
        double maxHealth = CombatMath.computeMaxHP(vit, level, stats.getJobId()) + (ctx.faith * 10.0D);
        derived.maxHealth = maxHealth;
        derived.healthRegenPerSecond = Math.max(0.0D, CombatMath.computeHPRegen(vit, maxHealth));

        double maxSp = CombatMath.computeMaxSP(intel, level, stats.getJobId());
        if (ctx.manaControl > 0) {
            maxSp *= 1.0D + (ctx.manaControl * 0.03D);
        }
        derived.maxSP = maxSp;
        
        double spRegen = CombatMath.computeSPRegen(intel, maxSp) + (ctx.arcaneRegen * 0.1D);
        derived.spRegenPerSecond = spRegen;
        
        // Mirror to Mana for backwards compatibility
        derived.maxMana = maxSp;
        derived.manaRegenPerSecond = spRegen;
    }

    private static void applyMiscStats(Player player, DerivedStats derived, int agi, int dex,
            EquipmentStatSnapshot snapshot) {
        double aspdBonus = (player.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED) && isAxeOrMace(player.getMainHandItem()))
                ? 6.0D
                : 0.0D;
        int aspdRo = CombatMath.computeASPD_RO(snapshot.weaponBaseAspd(), snapshot.hasShield(), agi, dex, aspdBonus);
        double aps = CombatMath.convertASPD_ToAPS(aspdRo);

        derived.attackSpeed = aspdRo;
        derived.globalCooldown = aps > 0.0D ? 1.0D / aps : 0.0D;
        derived.castTime = CombatMath.computeCastTime(snapshot.baseCastTime(), dex, 0, false);
        derived.lifeSteal = getLifeSteal(player);
    }

    private static void applyMinecraftAdaptationStats(DerivedStats derived, SkillContext ctx) {
        derived.projectileVelocityMult = 1.0D;
        derived.projectileGravityMult = 1.0D;
        derived.projectileSpreadMult = 1.0D;

        if (ctx.vulturesEye > 0) {
            derived.projectileVelocityMult *= getSkillLevelDouble(
                    VULTURES_EYE, ctx.vulturesEye, "projectile_velocity_mult", 1.0D);
            derived.projectileGravityMult *= getSkillLevelDouble(
                    VULTURES_EYE, ctx.vulturesEye, "projectile_gravity_mult", 1.0D);
            derived.projectileSpreadMult *= getSkillLevelDouble(
                    VULTURES_EYE, ctx.vulturesEye, "projectile_spread_mult", 1.0D);
        }
    }

    private static double getSkillLevelDouble(ResourceLocation skillId, int level, String key, double fallback) {
        return SkillRegistry.get(skillId)
                .map(def -> def.getLevelDouble(key, level, fallback))
                .orElse(fallback);
    }

    private static double getCritChance(Player player) {
        AttributeInstance attr = player.getAttribute(RagnarAttributes.CRIT_CHANCE.get());
        return attr != null ? attr.getValue() : 0.0D;
    }

    private static double getCritDamage(Player player) {
        AttributeInstance attr = player.getAttribute(RagnarAttributes.CRIT_DAMAGE.get());
        return attr != null ? Math.max(0.0D, attr.getValue() - 1.5D) : 0.0D;
    }

    private static double getLifeSteal(Player player) {
        AttributeInstance attr = player.getAttribute(RagnarAttributes.LIFE_STEAL.get());
        return attr != null ? attr.getValue() : 0.0D;
    }

    private static boolean isKatar(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("katars"));
    }

    private static boolean isAxeOrMace(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof net.minecraft.world.item.AxeItem
                || stack.getTags().anyMatch(tag -> tag.location().getPath().contains("maces")));
    }

    private static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.SwordItem && !isKatar(stack);
    }

    private static boolean isDagger(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("daggers"));
    }

    private static boolean isMace(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("maces"));
    }

    private static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof net.minecraft.world.item.BowItem;
    }

    private static boolean isSpear(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("spears"));
    }
}
