package com.etema.ragnarmmo.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getTicksUsingItem", at = @At("RETURN"), cancellable = true)
    private void ragnarmmo_getTicksUsingItem(CallbackInfoReturnable<Integer> cir) {
        // Bow draw acceleration is resolved authoritatively inside RagnarBowWeaponItem.
        // Leaving vanilla ticks untouched here avoids client/server double counting.
    }
}
