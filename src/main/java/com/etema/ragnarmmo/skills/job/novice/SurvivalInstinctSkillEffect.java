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
        return new ResourceLocation("ragnarmmo", "survival_instinct");
    }

    @Override
    public void onDefensiveHurt(LivingHurtEvent event, ServerPlayer player, int level) {
        if (level <= 0)
            return;
        if (player.level().isClientSide)
            return;

        float max = player.getMaxHealth();
        if (max <= 0.0f)
            return;

        float pct = player.getHealth() / max;

        // Rebalance:
        // Base: 1%/lvl (lvl5 = 5%)
        float reduction = level * 0.01f;

        // Bajo 30%: +2%/lvl extra (lvl5 => +10%), total 15%
        if (pct < 0.30f) {
            reduction += level * 0.02f;
        }

        // Cap global moderado para que no se vuelva inmortal con sinergias futuras
        reduction = Math.min(reduction, 0.35f);

        float newAmount = event.getAmount() * (1.0f - reduction);
        event.setAmount(Math.max(0.5f, newAmount));

        // “Pánico” <15% si lvl>=5: Resistance I 2s con CD 20s
        if (pct < 0.15f && level >= 5) {
            int now = player.tickCount;
            int cdUntil = player.getPersistentData().getInt(TAG_CD_UNTIL);

            if (now >= cdUntil) {
                player.getPersistentData().putInt(TAG_CD_UNTIL, now + (20 * 20)); // 20s
                player.addEffect(new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        20 * 2, // 2s
                        0, // 0 = Resistance I
                        false,
                        false));
            }
        }
    }
}
