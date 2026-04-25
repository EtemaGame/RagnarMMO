package com.etema.ragnarmmo.skills.job.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.event.TickEvent;

import java.util.Set;
import java.util.UUID;

/**
 * Effect handler for Fishing skill.
 * Provides luck bonus when holding a fishing rod for better catches.
 */
public class FishingSkillEffect implements ISkillEffect {

    private static final UUID FISHING_LUCK_UUID = UUID.fromString("d4e5f6a7-4b5c-6d7e-8f9a-0b1c2d3e4f5a");

    @Override
    public ResourceLocation getSkillId() {
        return new ResourceLocation("ragnarmmo", "fishing");
    }

    @Override
    public Set<TriggerType> getSupportedTriggers() {
        return Set.of(TriggerType.PERIODIC_TICK);
    }

    /**
     * Applies passive luck bonus when holding a fishing rod.
     */
    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (player.level().isClientSide)
            return;
        if (event != null && event.phase != TickEvent.Phase.END)
            return;

        if (level <= 0) {
            removeAttributeModifier(player, Attributes.LUCK, FISHING_LUCK_UUID);
            return;
        }

        // Apply fishing luck bonus when holding a fishing rod
        if (player.getMainHandItem().getItem() instanceof FishingRodItem) {
            // Fishing luck bonus: +0.1 per level
            double luckBonus = level * 0.1D;
            applyAttributeModifier(player, Attributes.LUCK, FISHING_LUCK_UUID,
                    "Fishing Luck", luckBonus, AttributeModifier.Operation.ADDITION);
        } else {
            removeAttributeModifier(player, Attributes.LUCK, FISHING_LUCK_UUID);
        }
    }

    /**
     * Helper method to apply attribute modifiers (TRANSIENT).
     * Avoids permanent/NBT-stuck modifiers.
     */
    private void applyAttributeModifier(ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid, String name, double amount,
            AttributeModifier.Operation operation) {
        var attributeInstance = player.getAttribute(attribute);
        if (attributeInstance == null)
            return;

        AttributeModifier existing = attributeInstance.getModifier(uuid);

        // If it's already the same amount/op, do nothing.
        if (existing != null && existing.getAmount() == amount && existing.getOperation() == operation) {
            return;
        }

        // Remove old (if any) and add transient (non-persistent)
        if (existing != null) {
            attributeInstance.removeModifier(uuid);
        }

        if (amount == 0.0D)
            return;

        attributeInstance.addTransientModifier(new AttributeModifier(uuid, name, amount, operation));
    }

    /**
     * Helper method to remove attribute modifiers.
     */
    private void removeAttributeModifier(ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid) {
        var attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(uuid);
        }
    }
}
