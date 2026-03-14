package com.etema.ragnarmmo.system.skills.life;

import net.minecraft.resources.ResourceLocation;
import com.etema.ragnarmmo.system.skills.ISkillEffect;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;

import java.util.UUID;

/**
 * Effect handler for Exploration skill.
 * Provides movement speed bonus for faster exploration and travel.
 */
public class ExplorationSkillEffect implements ISkillEffect {

    private static final UUID EXPLORATION_SPEED_UUID = UUID.fromString("a1b2c3d4-9e8f-7a6b-5c4d-3e2f1a0b9c8d");

    @Override
    public ResourceLocation getSkillId() {
        return ResourceLocation.fromNamespaceAndPath("ragnarmmo", "exploration");
    }

    /**
     * Applies passive movement speed bonus.
     */
    @Override
    public void onPeriodicTick(TickEvent.PlayerTickEvent event, ServerPlayer player, int level) {
        if (player.level().isClientSide)
            return;
        if (event != null && event.phase != TickEvent.Phase.END)
            return;

        if (level <= 0) {
            removeAttributeModifier(player, Attributes.MOVEMENT_SPEED, EXPLORATION_SPEED_UUID);
            return;
        }

        // Keep original balance: +1% per level (MULTIPLY_TOTAL)
        double speedBonus = level * 0.01D;

        applyAttributeModifier(player, Attributes.MOVEMENT_SPEED, EXPLORATION_SPEED_UUID,
                "Exploration Speed", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL);
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

    private void removeAttributeModifier(ServerPlayer player,
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid) {
        var attributeInstance = player.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(uuid);
        }
    }
}
