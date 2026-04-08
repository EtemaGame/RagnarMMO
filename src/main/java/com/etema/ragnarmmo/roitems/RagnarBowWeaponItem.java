package com.etema.ragnarmmo.roitems;

import com.etema.ragnarmmo.common.api.stats.StatAttributes;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.roitems.runtime.RagnarRangedWeaponStats;
import com.etema.ragnarmmo.roitems.runtime.RoRefineMath;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RagnarBowWeaponItem extends BowItem implements RagnarRangedWeaponStats {

    private final String displayName;
    private final String description;
    private final double rangedWeaponAtk;
    private final int baseRangedAspd;
    private final int baseDrawTicks;
    private final float projectileVelocity;

    public RagnarBowWeaponItem(
            Properties properties,
            @Nullable String displayName,
            @Nullable String description,
            double rangedWeaponAtk,
            int baseRangedAspd,
            int baseDrawTicks,
            float projectileVelocity
    ) {
        super(properties);
        this.displayName = displayName;
        this.description = description;
        this.rangedWeaponAtk = rangedWeaponAtk;
        this.baseRangedAspd = baseRangedAspd;
        this.baseDrawTicks = baseDrawTicks;
        this.projectileVelocity = projectileVelocity;
    }

    @Override
    public double getRangedWeaponAtk(ItemStack stack) {
        return rangedWeaponAtk;
    }

    @Override
    public int getBaseRangedAspd(ItemStack stack) {
        return baseRangedAspd;
    }

    @Override
    public int getBaseDrawTicks(ItemStack stack) {
        return baseDrawTicks;
    }

    @Override
    public float getProjectileVelocity(ItemStack stack) {
        return projectileVelocity;
    }

    public int getEffectiveDrawTicks(Player player) {
        int agi = (int) Math.round(StatAttributes.getTotal(player, StatKeys.AGI));
        return CombatMath.computeRangedDrawTicks(baseDrawTicks, agi);
    }

    public float getPowerForTicks(int heldTicks, int effectiveDrawTicks) {
        float f = (float) heldTicks / (float) effectiveDrawTicks;
        f = (f * f + f * 2.0F) / 3.0F;
        if (f > 1.0F) f = 1.0F;
        return f;
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
        if (pEntityLiving instanceof Player player) {
            boolean flag = player.getAbilities().instabuild || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.INFINITY_ARROWS, pStack) > 0;
            ItemStack itemstack = player.getProjectile(pStack);

            int i = this.getUseDuration(pStack) - pTimeLeft;
            i = net.minecraftforge.event.ForgeEventFactory.onArrowLoose(pStack, pLevel, player, i, !itemstack.isEmpty() || flag);
            if (i < 0) return;

            if (!itemstack.isEmpty() || flag) {
                if (itemstack.isEmpty()) {
                    itemstack = new ItemStack(Items.ARROW);
                }

                int effectiveDraw = getEffectiveDrawTicks(player);
                float f = getPowerForTicks(i, effectiveDraw);

                if (!((double) f < 0.1D)) {
                    boolean flag1 = player.getAbilities().instabuild || (itemstack.getItem() instanceof ArrowItem && ((ArrowItem) itemstack.getItem()).isInfinite(itemstack, pStack, player));
                    if (!pLevel.isClientSide) {
                        ArrowItem arrowitem = (ArrowItem) (itemstack.getItem() instanceof ArrowItem ? itemstack.getItem() : Items.ARROW);
                        AbstractArrow abstractarrow = arrowitem.createArrow(pLevel, itemstack, player);
                        abstractarrow = customArrow(abstractarrow);
                        abstractarrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, f * 3.0F * projectileVelocity, 1.0F);
                        
                        // SNAPSHOT LOGIC
                        if (abstractarrow.getOwner() != null) {
                            snapshotStats(abstractarrow, player, pStack, f);
                        }
                        
                        // Disable vanilla crit to prevent double-dipping
                        abstractarrow.setCritArrow(false);

                        // Note: Power enchantment is handled inside common math but vanilla also modifies baseDamage.
                        // We will ignore vanilla's baseDamage modification in onHurt if snapshot is present.
                        int punchLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.PUNCH_ARROWS, pStack);
                        if (punchLevel > 0) {
                            abstractarrow.setKnockback(punchLevel);
                        }

                        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FLAMING_ARROWS, pStack) > 0) {
                            abstractarrow.setSecondsOnFire(100);
                        }

                        pStack.hurtAndBreak(1, player, (p_40665_) -> p_40665_.broadcastBreakEvent(player.getUsedItemHand()));
                        
                        if (flag1 || player.getAbilities().instabuild && (itemstack.is(Items.SPECTRAL_ARROW) || itemstack.is(Items.TIPPED_ARROW))) {
                            abstractarrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                        }

                        pLevel.addFreshEntity(abstractarrow);
                    }

                    pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F / (pLevel.getRandom().nextFloat() * 0.4F + 1.2F) + f * 0.5F);
                    if (!flag1 && !player.getAbilities().instabuild) {
                        itemstack.shrink(1);
                        if (itemstack.isEmpty()) {
                            player.getInventory().removeItem(itemstack);
                        }
                    }

                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
        }
    }

    private void snapshotStats(AbstractArrow arrow, Player player, ItemStack bow, float drawRatio) {
        CompoundTag snapshot = new CompoundTag();
        snapshot.putInt("version", 2);
        snapshot.putString("family", "bow");
        
        com.etema.ragnarmmo.common.api.RagnarCoreAPI.get(player).ifPresent(stats -> {
            // Include Power enchantment in the resolved ATK
            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, bow);
            double bonusAtk = (powerLevel > 0) ? (powerLevel * 0.5D + 0.5D) * 10.0 : 0; // Arbitrary scale for RO context

            var baseSnapshot = com.etema.ragnarmmo.system.stats.compute.EquipmentStatSnapshot.capture(player);
            var rangedSnapshot = new com.etema.ragnarmmo.system.stats.compute.EquipmentStatSnapshot(
                    rangedWeaponAtk + RoRefineMath.getAttackBonus(bow) + bonusAtk,
                    baseSnapshot.weaponMagicAtk(),
                    baseSnapshot.weaponBaseAspd(),
                    baseSnapshot.armorHardDef(),
                    baseSnapshot.armorHardMdef(),
                    baseSnapshot.hasShield(),
                    true,
                    baseSnapshot.baseCastTime());
            var derived = com.etema.ragnarmmo.system.stats.compute.StatComputer.compute(player, stats, rangedSnapshot);
            snapshot.putDouble("atk", derived.physicalAttack);
            snapshot.putInt("dex", (int) StatAttributes.getTotal(player, StatKeys.DEX));
            snapshot.putInt("luk", (int) StatAttributes.getTotal(player, StatKeys.LUK));
            snapshot.putDouble("crit_chance", derived.criticalChance);
            snapshot.putDouble("crit_damage", derived.criticalDamageMultiplier);
            snapshot.putDouble("draw_ratio", drawRatio);
            snapshot.putString("element", com.etema.ragnarmmo.combat.element.CombatPropertyResolver.getOffensiveElement(player).name());
            snapshot.putUUID("shooter_uuid", player.getUUID());
            
            arrow.getPersistentData().put("ragnarmmo_snapshot", snapshot);
        });
    }

    @Override
    public Component getName(ItemStack stack) {
        return TooltipTextHelper.displayName(this.getDescriptionId(), displayName);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        TooltipTextHelper.appendDescription(tooltip, this.getDescriptionId(), description);
        TooltipTextHelper.appendRangedStats(tooltip, this, stack);
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
