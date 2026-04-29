package com.etema.ragnarmmo.skills.job.novice;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class SurvivalInstinctSkillEffect implements ISkillEffect {

    // Tag en NBT del jugador para cooldown
    private static final String TAG_CD_UNTIL = "ragnarmmo_survival_cd_until";

    @Override
    public ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "survival_instinct");
    }

    @Override
    public void onDefensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        // Combat damage is resolved by RagnarCombatEngine via SkillCombatSpec.
    }
}
