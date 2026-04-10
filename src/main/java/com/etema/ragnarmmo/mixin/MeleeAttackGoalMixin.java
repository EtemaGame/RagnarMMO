package com.etema.ragnarmmo.mixin;

import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {

    @Shadow
    @Final
    protected PathfinderMob mob;

    @Inject(method = "getAttackInterval", at = @At("RETURN"), cancellable = true)
    private void ragnarmmo_getAttackInterval(CallbackInfoReturnable<Integer> cir) {
        CombatMath.tryGetResolvedMobAttackIntervalTicks(this.mob)
                .ifPresent(cir::setReturnValue);
    }
}
