package com.etema.ragnarmmo.combat.profile;

import java.util.Optional;

import com.etema.ragnarmmo.combat.hand.AttackHandResolver;
import com.etema.ragnarmmo.combat.timing.AttackCadenceCalculator;
import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.items.runtime.RangedWeaponStatsHelper;
import com.etema.ragnarmmo.items.runtime.RoRefineMath;
import com.etema.ragnarmmo.items.runtime.RoRequirementChecker;
import com.etema.ragnarmmo.items.runtime.RoItemRuleResolver;
import com.etema.ragnarmmo.items.runtime.WeaponStatHelper;
import com.etema.ragnarmmo.common.config.access.RoItemsConfigAccess;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.player.stats.compute.CombatMath;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Resolves the offensive stat profile for the actual hand used by a basic attack.
 */
public final class HandAttackProfileResolver {
    private static final ResourceLocation SWORD_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sword_mastery");
    private static final ResourceLocation TWO_HAND_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "two_hand_mastery");
    private static final ResourceLocation DAGGER_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "dagger_mastery");
    private static final ResourceLocation MACE_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "mace_mastery");
    private static final ResourceLocation BOW_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "bow_mastery");
    private static final ResourceLocation WEAPON_TRAINER =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "weapon_trainer");
    private static final ResourceLocation SPEAR_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "spear_mastery");
    private static final ResourceLocation KATAR_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "katar_mastery");
    private static final ResourceLocation RIGHTHAND_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "righthand_mastery");
    private static final ResourceLocation LEFTHAND_MASTERY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "lefthand_mastery");
    private static final ResourceLocation SONIC_ACCELERATION =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "sonic_acceleration");
    private static final ResourceLocation RESEARCH_WEAPONRY =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "research_weaponry");
    private static final ResourceLocation CRITICAL_SHOT =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "critical_shot");
    private static final ResourceLocation ACCURACY_TRAINING =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "accuracy_training");
    private static final ResourceLocation VULTURES_EYE =
            ResourceLocation.fromNamespaceAndPath("ragnarmmo", "vultures_eye");
    private static final TagKey<Item> TWO_HANDED_TAG =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("ragnarmmo", "two_handed"));

    private HandAttackProfileResolver() {
    }

    public static Optional<HandAttackProfile> resolve(Player player, boolean offHand) {
        if (!AttackHandResolver.isValidAttackHand(player, offHand)) {
            return Optional.empty();
        }

        var statsOpt = RagnarCoreAPI.get(player);
        if (statsOpt.isEmpty()) {
            return Optional.empty();
        }

        ItemStack weapon = offHand ? player.getOffhandItem() : player.getMainHandItem();
        SkillContext skills = fetchSkillContext(player);
        int str = (int) Math.round(StatAttributes.getTotal(player, StatKeys.STR));
        int agi = (int) Math.round(StatAttributes.getTotal(player, StatKeys.AGI));
        int dex = (int) Math.round(StatAttributes.getTotal(player, StatKeys.DEX));
        int luk = (int) Math.round(StatAttributes.getTotal(player, StatKeys.LUK));
        int level = statsOpt.get().getLevel();
        boolean ranged = CombatMath.isRangedWeapon(weapon);

        double weaponBaseAttack = applyRequirementPenalty(player, weapon, resolveWeaponBaseAttack(weapon, player, ranged));
        double statusAtk = CombatMath.computeStatusATK(str, dex, luk, level, ranged);
        double skillAtk = computeSkillAttack(player, weapon, offHand, skills);
        double physicalAttack = CombatMath.computeWeaponATK(weaponBaseAttack, str, dex, ranged) + statusAtk + skillAtk;
        double accuracy = CombatMath.computeHIT(dex, luk, level, computeHitBonus(weapon, skills));
        double critChance = CombatMath.computeCritChance(luk, dex, getCritChance(player)
                + (ranged ? skills.criticalShot() * 0.01D : 0.0D));
        double critDamage = CombatMath.computeCritDamageMultiplier(luk, str)
                + getCritDamage(player)
                + (ranged ? skills.criticalShot() * 0.01D : 0.0D);

        int weaponBaseAspd = resolveWeaponBaseAspd(weapon, ranged);
        boolean hasShield = !offHand && isShield(player.getOffhandItem());
        int aspdRo = CombatMath.computeASPD_RO(weaponBaseAspd, hasShield, agi, dex, 0.0D);
        double aps = AttackCadenceCalculator.computeAPS(player, offHand);

        return Optional.of(new HandAttackProfile(
                offHand,
                weapon.copy(),
                weaponBaseAttack,
                aspdRo,
                aps,
                physicalAttack,
                accuracy,
                critChance,
                critDamage));
    }

    private static SkillContext fetchSkillContext(Player player) {
        var skillsOpt = PlayerSkillsProvider.get(player).resolve();
        if (skillsOpt.isEmpty()) {
            return SkillContext.empty();
        }

        var skills = skillsOpt.get();
        return new SkillContext(
                skills.getSkillLevel(SWORD_MASTERY),
                skills.getSkillLevel(TWO_HAND_MASTERY),
                skills.getSkillLevel(DAGGER_MASTERY),
                skills.getSkillLevel(MACE_MASTERY),
                skills.getSkillLevel(BOW_MASTERY),
                skills.getSkillLevel(WEAPON_TRAINER),
                skills.getSkillLevel(SPEAR_MASTERY),
                skills.getSkillLevel(KATAR_MASTERY),
                skills.getSkillLevel(RIGHTHAND_MASTERY),
                skills.getSkillLevel(LEFTHAND_MASTERY),
                skills.getSkillLevel(SONIC_ACCELERATION),
                skills.getSkillLevel(RESEARCH_WEAPONRY),
                skills.getSkillLevel(CRITICAL_SHOT),
                skills.getSkillLevel(ACCURACY_TRAINING),
                skills.getSkillLevel(VULTURES_EYE));
    }

    private static double resolveWeaponBaseAttack(ItemStack weapon, Player player, boolean ranged) {
        if (ranged) {
            var rangedStats = RangedWeaponStatsHelper.resolve(weapon);
            if (rangedStats.isPresent()) {
                return rangedStats.get().weaponAtk();
            }
        }

        double configuredAttack = WeaponStatHelper.getConfiguredPhysicalAttackBase(weapon);
        if (configuredAttack > 0.0D) {
            return configuredAttack
                    + EnchantmentHelper.getDamageBonus(weapon, MobType.UNDEFINED)
                    + RoRefineMath.getAttackBonus(weapon);
        }

        // P0 combat must not read vanilla attack attributes as balance input.
        // Unauthored weapons keep only refinement/enchantment compatibility until
        // they receive an RO item rule.
        return Math.max(1.0D,
                1.0D + EnchantmentHelper.getDamageBonus(weapon, MobType.UNDEFINED)
                        + RoRefineMath.getAttackBonus(weapon));
    }

    private static double applyRequirementPenalty(Player player, ItemStack weapon, double attack) {
        if (weapon.isEmpty()
                || !RoItemsConfigAccess.isEnabled()
                || !RoItemsConfigAccess.reduceDamageOnRestriction()) {
            return attack;
        }

        var rule = RoItemRuleResolver.resolve(weapon);
        if (!rule.hasRequirements()) {
            return attack;
        }

        var result = RoRequirementChecker.check(player, rule);
        if (result == RoRequirementChecker.CheckResult.OK
                || result == RoRequirementChecker.CheckResult.NO_STATS_DATA) {
            return attack;
        }
        return Math.max(0.0D, RoItemsConfigAccess.getPenaltyDamage());
    }

    private static int resolveWeaponBaseAspd(ItemStack weapon, boolean ranged) {
        if (ranged) {
            var rangedStats = RangedWeaponStatsHelper.resolve(weapon);
            if (rangedStats.isPresent()) {
                return rangedStats.get().baseAspd();
            }
        }
        return CombatMath.getWeaponBaseASPD(weapon);
    }

    private static double computeSkillAttack(Player player, ItemStack weapon, boolean offHand, SkillContext ctx) {
        double masteryBonus = 0.0D;
        if (isTwoHandedSword(weapon)) masteryBonus += ctx.twoHand() * 4.0D;
        else if (isSword(weapon)) masteryBonus += ctx.sword() * 4.0D;
        else if (isDagger(weapon)) masteryBonus += ctx.dagger() * 4.0D;
        else if (isMace(weapon)) masteryBonus += ctx.mace() * 4.0D;
        else if (isBow(weapon)) masteryBonus += ctx.bow() * 4.0D;
        else if (isSpear(weapon)) masteryBonus += ctx.spear() * 4.0D;
        else if (isKatar(weapon)) masteryBonus += ctx.katar() * 4.0D;

        if (isAxeOrMace(weapon)) {
            masteryBonus += ctx.researchWeaponry() * 3.0D;
        }

        double skillAtk = masteryBonus + (ctx.weaponTrainer() * 1.5D);
        if (AttackHandResolver.isDualWielding(player)) {
            skillAtk += offHand ? ctx.leftHand() * 3.0D : ctx.rightHand() * 3.0D;
        }
        return skillAtk;
    }

    private static double computeHitBonus(ItemStack weapon, SkillContext ctx) {
        double hitBonus = ctx.accuracy() * 3.0D;
        if (isBow(weapon)) {
            hitBonus += ctx.bow() * 2.0D;
            if (ctx.vulturesEye() > 0) {
                hitBonus += getSkillLevelDouble(VULTURES_EYE, ctx.vulturesEye(), "accuracy_bonus", ctx.vulturesEye());
            }
        }
        if (ctx.sonicAccel() > 0 && isKatar(weapon)) {
            hitBonus += ctx.sonicAccel() * 5.0D;
        }
        if (ctx.researchWeaponry() > 0 && isAxeOrMace(weapon)) {
            hitBonus += ctx.researchWeaponry() * 2.0D;
        }
        return hitBonus;
    }

    private static double getSkillLevelDouble(ResourceLocation skillId, int level, String key, double defaultValue) {
        return SkillRegistry.get(skillId)
                .map(def -> def.getLevelDouble(key, level, defaultValue))
                .orElse(defaultValue);
    }

    private static double getCritChance(Player player) {
        AttributeInstance attr = player.getAttribute(RagnarAttributes.CRIT_CHANCE.get());
        return attr != null ? attr.getValue() : 0.0D;
    }

    private static double getCritDamage(Player player) {
        AttributeInstance attr = player.getAttribute(RagnarAttributes.CRIT_DAMAGE.get());
        return attr != null ? Math.max(0.0D, attr.getValue() - 1.5D) : 0.0D;
    }

    private static boolean isKatar(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("katars"));
    }

    private static boolean isAxeOrMace(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof AxeItem
                || stack.getTags().anyMatch(tag -> tag.location().getPath().contains("maces")));
    }

    private static boolean isSword(ItemStack stack) {
        return stack.getItem() instanceof SwordItem && !isKatar(stack);
    }

    private static boolean isDagger(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("daggers"));
    }

    private static boolean isMace(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("maces"));
    }

    private static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    private static boolean isSpear(ItemStack stack) {
        return stack.getTags().anyMatch(tag -> tag.location().getPath().contains("spears"));
    }

    private static boolean isShield(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)
                        || stack.getItem() instanceof ShieldItem);
    }

    private record SkillContext(
            int sword,
            int twoHand,
            int dagger,
            int mace,
            int bow,
            int weaponTrainer,
            int spear,
            int katar,
            int rightHand,
            int leftHand,
            int sonicAccel,
            int researchWeaponry,
            int criticalShot,
            int accuracy,
            int vulturesEye) {

        private static SkillContext empty() {
            return new SkillContext(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private static boolean isTwoHandedSword(ItemStack stack) {
        return isSword(stack) && stack.is(TWO_HANDED_TAG);
    }
}
