package com.etema.ragnarmmo.mixin;

import com.etema.ragnarmmo.common.api.mobs.runtime.store.ManualMobProfileRuntimeStore;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractSkeleton.class)
public abstract class SkeletonAccuracyMixin {

    @ModifyVariable(method = "performRangedAttack", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float updateAccuracy(float error, LivingEntity target, float distanceFactor) {
        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;
        boolean hasNormalizedDex = ManualMobProfileRuntimeStore.get(skeleton)
                .map(profile -> profile.baseCombatStats() != null && profile.baseCombatStats().dex() != null)
                .orElse(false);
        boolean hasLegacyStats = MobStatsProvider.get(skeleton).isPresent();
        if (!hasNormalizedDex && !hasLegacyStats) {
            return error;
        }

        int dex = CombatMath.getTargetStats(skeleton).dex;
        // Reduced error based on DEX. Standard Minecraft error is roughly 1.0 to 10.0 depending on difficulty.
        // We reduce it by a percentage based on DEX.
        float reduction = (float) Math.min(0.95, dex / 150.0);
        return error * (1.0f - reduction);
    }
}
