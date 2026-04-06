package com.etema.ragnarmmo.mixin;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getTicksUsingItem", at = @At("RETURN"), cancellable = true)
    private void ragnarmmo_getTicksUsingItem(CallbackInfoReturnable<Integer> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Only apply AGI bonus for Bows and Crossbows
        if (entity instanceof Player player && (entity.getUseItem().getItem() instanceof BowItem || entity.getUseItem().getItem() instanceof CrossbowItem)) {
            RagnarCoreAPI.get(player).ifPresent(stats -> {
                int agi = stats.getAGI();
                // Formula: each AGI point improves draw speed by 0.7%.
                // 100 AGI = 70% faster total progress multiplier (1.7x).
                // This makes bows feel much more responsive as requested.
                if (agi > 1) {
                    double multiplier = 1.0 + (agi * 0.007);
                    cir.setReturnValue((int) (cir.getReturnValueI() * multiplier));
                }
            });
        }
    }
}
