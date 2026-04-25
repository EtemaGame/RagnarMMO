package com.etema.ragnarmmo.skills.job.archer;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.TickEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Owl's Eye - passive DEX bonus. Tuning lives in level_data.dex_bonus.
 */
public class OwlsEyeSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "owls_eye");
    private static final UUID DEX_BONUS_ID = UUID.fromString("2e83bd5d-e2a8-4c2f-a1d1-516d10f24701");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK);
    }

    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        int dexBonus = SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("dex_bonus", level, level))
                .orElse(level);
        var dex = player.getAttribute(RagnarAttributes.DEX.get());
        if (dex == null) {
            return;
        }

        AttributeModifier existing = dex.getModifier(DEX_BONUS_ID);
        if (existing != null && Math.abs(existing.getAmount() - dexBonus) <= 1.0E-4D) {
            return;
        }
        if (existing != null) {
            dex.removeModifier(existing);
        }
        dex.addTransientModifier(new AttributeModifier(
                DEX_BONUS_ID, "ragnarmmo_owls_eye_dex", dexBonus, AttributeModifier.Operation.ADDITION));
    }
}
