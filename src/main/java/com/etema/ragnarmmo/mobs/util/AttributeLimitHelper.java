package com.etema.ragnarmmo.mobs.util;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Utility responsible for lifting vanilla attribute caps so mobs can scale
 * beyond the default limits imposed by {@link RangedAttribute} instances.
 *
 * Compatible with Minecraft Forge 1.20.1 (Forge 47.x)
 */
public final class AttributeLimitHelper {
    private AttributeLimitHelper() {}

    /**
     * Called during common setup phase to remove attribute caps asynchronously.
     */
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(AttributeLimitHelper::removeAttributeCaps);
    }

    /**
     * Removes limits from selected vanilla attributes.
     */
    private static void removeAttributeCaps() {
        liftCap(Attributes.MAX_HEALTH, Double.MAX_VALUE);
        liftCap(Attributes.ARMOR, Double.MAX_VALUE);
        liftCap(Attributes.ATTACK_DAMAGE, Double.MAX_VALUE);
        liftCap(Attributes.ARMOR_TOUGHNESS, Double.MAX_VALUE);
    }

    /**
     * Lifts the maximum value limit of a {@link RangedAttribute}.
     *
     * @param attribute Attribute to modify
     * @param newMax    New maximum value
     */
    private static void liftCap(Attribute attribute, double newMax) {
        if (attribute instanceof RangedAttribute ranged) {
            // Prevent redundant reflection if not needed
            if (ranged.getMaxValue() >= newMax) {
                return;
            }

            try {
                // 1.20.1 obfuscated field name for "maxValue"
                String fieldName = "f_22309_";

                // Access and set the field
                var field = ObfuscationReflectionHelper.findField(RangedAttribute.class, fieldName);
                field.setAccessible(true);
                field.set(ranged, newMax);

                System.out.println("[RagnarMMO] Lifted cap for " + attribute.getDescriptionId() +
                        " to " + newMax);
            } catch (Exception e) {
                System.err.println("[RagnarMMO] Failed to lift cap for " +
                        attribute.getDescriptionId() + ": " + e.getMessage());
            }
        }
    }
}






