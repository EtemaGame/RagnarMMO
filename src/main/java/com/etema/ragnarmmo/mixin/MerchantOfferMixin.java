package com.etema.ragnarmmo.mixin;

import com.etema.ragnarmmo.items.ZenyItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    @Shadow(aliases = {"f_45310_", "baseCostA"}) @Final @Mutable
    private ItemStack baseCostA;
    @Shadow(aliases = {"f_45311_", "costB"}) @Final @Mutable
    private ItemStack costB;
    @Shadow(aliases = {"f_45312_", "result"}) @Final @Mutable
    private ItemStack result;

    /**
     * 5-argument constructor (Used for simple 1-item trades like Coal -> Emerald).
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;IIF)V",
        at = @At("RETURN")
    )
    private void onInit5(ItemStack p_45325_, ItemStack p_45326_, int p_45327_, int p_45328_, float p_45329_, CallbackInfo ci) {
        replaceAll();
    }

    /**
     * 6-argument constructor.
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;IIF)V",
        at = @At("RETURN")
    )
    private void onInit6(ItemStack p_45325_, ItemStack p_45326_, ItemStack p_45327_, int p_45328_, int p_45329_, float p_45330_, CallbackInfo ci) {
        replaceAll();
    }

    /**
     * 7-argument constructor.
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;IIIF)V",
        at = @At("RETURN")
    )
    private void onInit7(ItemStack p_45332_, ItemStack p_45333_, ItemStack p_45334_, int p_45335_, int p_45336_, int p_45337_, float p_45338_, CallbackInfo ci) {
        replaceAll();
    }

    /**
     * 8-argument constructor.
     */
    @Inject(
        method = "<init>(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;IIIFI)V",
        at = @At("RETURN")
    )
    private void onInit8(ItemStack p_45340_, ItemStack p_45341_, ItemStack p_45342_, int p_45343_, int p_45344_, int p_45345_, float p_45346_, int p_45347_, CallbackInfo ci) {
        replaceAll();
    }

    /**
     * NBT loading.
     */
    @Inject(
        method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At("RETURN")
    )
    private void onNBTInit(CompoundTag tag, CallbackInfo ci) {
        replaceAll();
    }

    private void replaceAll() {
        this.baseCostA = replaceEmerald(this.baseCostA);
        this.costB = replaceEmerald(this.costB);
        this.result = replaceEmerald(this.result);
    }

    private static ItemStack replaceEmerald(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() == Items.EMERALD) {
            return new ItemStack(ZenyItems.GOLD_ZENY.get(), stack.getCount());
        }
        return stack;
    }
}
