package com.etema.ragnarmmo.skill.job.merchant;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.common.WeightConstants;
import com.etema.ragnarmmo.items.ZenyItems;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.execution.EconomicSkillHelper;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.system.stats.capability.PlayerStatsProvider;
import com.etema.ragnarmmo.util.AttrUtil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemStackHandler;

import java.util.UUID;

/**
 *  - If the player can't pay → trade is cancelled.
 *  - Overcharge gives extra Zeny instead of extra emeralds when selling.
 *  - Emeralds are replaced by Gold Zeny in all offers.
 *
 * Encumbrance system unchanged.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class MerchantSkillEvents {

    // ── Skill ResourceLocations ──────────────────────────────────────────────
    private static final ResourceLocation DISCOUNT   = new ResourceLocation("ragnarmmo", "discount");
    private static final ResourceLocation OVERCHARGE = new ResourceLocation("ragnarmmo", "overcharge");
    private static final ResourceLocation ENLARGE_WEIGHT_LIMIT = new ResourceLocation("ragnarmmo", "enlarge_weight_limit");
    private static final ResourceLocation PUSHCART = new ResourceLocation("ragnarmmo", "pushcart");

    // ── Encumbrance ──────────────────────────────────────────────────────────
    private static final UUID ENCUMBRANCE_SPEED_UUID =
            UUID.fromString("1f6a9e28-8dd6-4fb8-9ed3-0f6df2f2c001");

    // ═══════════════════════════════════════════════════════════════════════
    // Encumbrance (unchanged from original)
    // ═══════════════════════════════════════════════════════════════════════
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // Perform weight calculation only once every second (20 ticks) to optimize performance
        if (player.tickCount % 20 != 0) return;

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int weightLimitLevel = skills.getSkillLevel(ENLARGE_WEIGHT_LIMIT);
            int pushcartLevel = skills.getSkillLevel(PUSHCART);
            int str = getStrength(player);

            double capacity = WeightConstants.BASE_CAPACITY
                    + (str * WeightConstants.STR_CAPACITY_PER_POINT)
                    + capacityBonusFromEnlargeWeightLimit(weightLimitLevel);

            double totalWeight = 0.0D;
            for (ItemStack s : player.getInventory().items) {
                totalWeight += computeWeight(s);
            }

            ItemStackHandler cartInv = skills.getCartInventory();
            double cartWeight = 0.0D;
            if (cartInv != null) {
                for (int i = 0; i < cartInv.getSlots(); i++) {
                    cartWeight += computeWeight(cartInv.getStackInSlot(i));
                }
            }

            double cartReduction = cartWeightReduction(pushcartLevel);
            totalWeight += cartWeight * (1.0D - cartReduction);

            double overweight = totalWeight - capacity;
            if (overweight <= 0.0D) {
                AttrUtil.upsertTransient(player.getAttribute(Attributes.MOVEMENT_SPEED),
                        ENCUMBRANCE_SPEED_UUID, "Encumbrance", 0.0D,
                        AttributeModifier.Operation.MULTIPLY_BASE);
                return;
            }

            double penalty = (overweight / WeightConstants.OVERWEIGHT_TO_MAX) * WeightConstants.MAX_SPEED_PENALTY;
            penalty = Math.min(WeightConstants.MAX_SPEED_PENALTY, Math.max(0.0D, penalty));
            AttrUtil.upsertTransient(player.getAttribute(Attributes.MOVEMENT_SPEED),
                    ENCUMBRANCE_SPEED_UUID, "Encumbrance", -penalty,
                    AttributeModifier.Operation.MULTIPLY_BASE);
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Merchant Skills: Discount & Overcharge (Emerald Economy)
    // ═══════════════════════════════════════════════════════════════════════
    @SubscribeEvent
    public static void onOpenMerchantMenu(net.minecraftforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (event.getContainer() instanceof net.minecraft.world.inventory.MerchantMenu menu) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            PlayerSkillsProvider.get(player).ifPresent(skills -> {
                int discountLvl = skills.getSkillLevel(DISCOUNT);
                int overchargeLvl = skills.getSkillLevel(OVERCHARGE);

                if (discountLvl <= 0 && overchargeLvl <= 0) return;

                double discountRate = discountLvl > 0
                        ? SkillRegistry.get(DISCOUNT)
                                .map(def -> EconomicSkillHelper.vendorBuyDiscount(def, discountLvl))
                                .orElse(0.0D)
                        : 0.0D;
                double overchargeRate = overchargeLvl > 0
                        ? SkillRegistry.get(OVERCHARGE)
                                .map(def -> EconomicSkillHelper.vendorSellBonus(def, overchargeLvl))
                                .orElse(0.0D)
                        : 0.0D;

                double discountFactor = 1.0D - clampRate(discountRate);
                double overchargeFactor = 1.0D + clampRate(overchargeRate);

                var offers = menu.getOffers();
                for (MerchantOffer offer : offers) {
                    if (discountRate > 0.0D) {
                        applyModifier(offer.getCostA(), discountFactor, true);
                        applyModifier(offer.getCostB(), discountFactor, true);
                    }

                    if (overchargeRate > 0.0D && offer.getResult().is(ZenyItems.GOLD_ZENY.get())) {
                        applyModifier(offer.getResult(), overchargeFactor, false);
                    }
                }
            });
        }
    }

    public static double capacityBonusFromEnlargeWeightLimit(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return SkillRegistry.get(ENLARGE_WEIGHT_LIMIT)
                .map(def -> def.getLevelDouble("weight_limit_bonus", level, level * 200.0D))
                .orElse(level * 200.0D);
    }

    public static double cartWeightReduction(int level) {
        if (level <= 0) {
            return 0.0D;
        }
        double fallback = Math.min(WeightConstants.CART_WEIGHT_REDUCTION_CAP,
                level * WeightConstants.CART_WEIGHT_REDUCTION_PER_LEVEL);
        double configured = SkillRegistry.get(PUSHCART)
                .map(def -> def.getLevelDouble("cart_weight_reduction", level, fallback))
                .orElse(fallback);
        return Math.min(WeightConstants.CART_WEIGHT_REDUCTION_CAP, Math.max(0.0D, configured));
    }

    private static double clampRate(double rate) {
        return Math.min(0.95D, Math.max(0.0D, rate));
    }

    private static void applyModifier(ItemStack stack, double factor, boolean isDiscount) {
        if (stack.isEmpty() || !stack.is(ZenyItems.GOLD_ZENY.get())) return;
        
        int original = stack.getCount();
        int modified;
        if (isDiscount) {
            modified = (int) Math.max(1, Math.floor(original * factor));
        } else {
            modified = (int) Math.min(64, Math.ceil(original * factor)); // Zeny stacks to 64
        }
        
        if (modified != original) {
            stack.setCount(modified);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Weight helpers (unchanged)
    // ═══════════════════════════════════════════════════════════════════════

    public static double computeWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0.0D;
        return getBaseWeight(stack) * stack.getCount();
    }

    public static double computeDisplayWeight(ItemStack stack) {
        return stack.isEmpty() ? 0.0D : getBaseWeight(stack);
    }

    public static boolean isStackable(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() > 1;
    }

    private static int getStrength(ServerPlayer player) {
        return player.getCapability(PlayerStatsProvider.CAP)
                .map(stats -> stats.getSTR())
                .orElse(0);
    }

    private static double getBaseWeight(ItemStack stack) {
        if (stack.isEmpty()) return 0.0D;
        if (stack.is(WeightConstants.WEIGHT_TITANIC))  return WeightConstants.WEIGHT_VAL_TITANIC;
        if (stack.is(WeightConstants.WEIGHT_MASSIVE))  return WeightConstants.WEIGHT_VAL_MASSIVE;
        if (stack.is(WeightConstants.WEIGHT_HEAVY))    return WeightConstants.WEIGHT_VAL_HEAVY;
        if (stack.is(WeightConstants.WEIGHT_DENSE))    return WeightConstants.WEIGHT_VAL_DENSE;
        if (stack.is(WeightConstants.WEIGHT_MEDIUM))   return WeightConstants.WEIGHT_VAL_MEDIUM;
        if (stack.is(WeightConstants.WEIGHT_COMMON))   return WeightConstants.WEIGHT_VAL_COMMON;
        if (stack.is(WeightConstants.WEIGHT_LIGHT))    return WeightConstants.WEIGHT_VAL_LIGHT;
        if (stack.is(WeightConstants.WEIGHT_FEATHER))  return WeightConstants.WEIGHT_VAL_FEATHER;
        if (stack.is(WeightConstants.WEIGHT_EPHEMERAL)) return WeightConstants.WEIGHT_VAL_EPHEMERAL;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_NETHERITE)) return WeightConstants.WEIGHT_VAL_ARMOR_NETHERITE;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_GOLD))      return WeightConstants.WEIGHT_VAL_ARMOR_GOLD;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_IRON))      return WeightConstants.WEIGHT_VAL_ARMOR_IRON;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_DIAMOND))   return WeightConstants.WEIGHT_VAL_ARMOR_DIAMOND;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_CHAIN))     return WeightConstants.WEIGHT_VAL_ARMOR_CHAIN;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_TURTLE))    return WeightConstants.WEIGHT_VAL_ARMOR_TURTLE;
        if (stack.is(WeightConstants.WEIGHT_ARMOR_LEATHER))   return WeightConstants.WEIGHT_VAL_ARMOR_LEATHER;
        if (stack.is(WeightConstants.WEIGHT_TOOL_NETHERITE))  return WeightConstants.WEIGHT_VAL_TOOL_NETHERITE;
        if (stack.is(WeightConstants.WEIGHT_TOOL_GOLD))       return WeightConstants.WEIGHT_VAL_TOOL_GOLD;
        if (stack.is(WeightConstants.WEIGHT_TOOL_IRON))       return WeightConstants.WEIGHT_VAL_TOOL_IRON;
        if (stack.is(WeightConstants.WEIGHT_TOOL_STONE))      return WeightConstants.WEIGHT_VAL_TOOL_STONE;
        if (stack.is(WeightConstants.WEIGHT_TOOL_DIAMOND))    return WeightConstants.WEIGHT_VAL_TOOL_DIAMOND;
        if (stack.is(WeightConstants.WEIGHT_TOOL_WOOD))       return WeightConstants.WEIGHT_VAL_TOOL_WOOD;
        Item item = stack.getItem();
        if (item instanceof ShieldItem)   return WeightConstants.WEIGHT_SHIELD;
        if (item instanceof ElytraItem)   return WeightConstants.WEIGHT_ELYTRA;
        if (item instanceof TridentItem)  return WeightConstants.WEIGHT_TRIDENT;
        if (item instanceof CrossbowItem) return WeightConstants.WEIGHT_CROSSBOW;
        if (item instanceof BowItem)      return WeightConstants.WEIGHT_BOW;
        if (item instanceof TieredItem tiered) return getWeightForTier(tiered);
        if (item instanceof ArmorItem armor)   return getWeightForArmor(armor);
        return stack.getMaxStackSize() > 1
                ? WeightConstants.WEIGHT_FALLBACK_STACKABLE
                : WeightConstants.WEIGHT_FALLBACK_UNSTACKABLE;
    }

    private static double getWeightForTier(TieredItem tiered) {
        var tier = tiered.getTier();
        if (tier == Tiers.WOOD)      return WeightConstants.WEIGHT_VAL_TOOL_WOOD;
        if (tier == Tiers.STONE)     return WeightConstants.WEIGHT_VAL_TOOL_STONE;
        if (tier == Tiers.GOLD)      return WeightConstants.WEIGHT_VAL_TOOL_GOLD;
        if (tier == Tiers.IRON)      return WeightConstants.WEIGHT_VAL_TOOL_IRON;
        if (tier == Tiers.DIAMOND)   return WeightConstants.WEIGHT_VAL_TOOL_DIAMOND;
        if (tier == Tiers.NETHERITE) return WeightConstants.WEIGHT_VAL_TOOL_NETHERITE;
        int ml = tier.getLevel();
        if (ml <= 0) return WeightConstants.WEIGHT_VAL_TOOL_WOOD;
        if (ml == 1) return WeightConstants.WEIGHT_VAL_TOOL_STONE;
        if (ml == 2) return WeightConstants.WEIGHT_VAL_TOOL_IRON;
        if (ml == 3) return WeightConstants.WEIGHT_VAL_TOOL_DIAMOND;
        return WeightConstants.WEIGHT_VAL_TOOL_NETHERITE;
    }

    private static double getWeightForArmor(ArmorItem armor) {
        var mat = armor.getMaterial();
        if (mat == ArmorMaterials.LEATHER)   return WeightConstants.WEIGHT_VAL_ARMOR_LEATHER;
        if (mat == ArmorMaterials.CHAIN)     return WeightConstants.WEIGHT_VAL_ARMOR_CHAIN;
        if (mat == ArmorMaterials.GOLD)      return WeightConstants.WEIGHT_VAL_ARMOR_GOLD;
        if (mat == ArmorMaterials.IRON)      return WeightConstants.WEIGHT_VAL_ARMOR_IRON;
        if (mat == ArmorMaterials.DIAMOND)   return WeightConstants.WEIGHT_VAL_ARMOR_DIAMOND;
        if (mat == ArmorMaterials.NETHERITE) return WeightConstants.WEIGHT_VAL_ARMOR_NETHERITE;
        if (mat == ArmorMaterials.TURTLE)    return WeightConstants.WEIGHT_VAL_ARMOR_TURTLE;
        int def = mat.getDefenseForType(armor.getType());
        if (def <= 1) return WeightConstants.WEIGHT_VAL_ARMOR_LEATHER;
        if (def <= 3) return WeightConstants.WEIGHT_VAL_ARMOR_CHAIN;
        if (def <= 5) return WeightConstants.WEIGHT_VAL_ARMOR_IRON;
        if (def <= 7) return WeightConstants.WEIGHT_VAL_ARMOR_DIAMOND;
        return WeightConstants.WEIGHT_VAL_ARMOR_NETHERITE;
    }
}
