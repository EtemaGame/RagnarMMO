package com.etema.ragnarmmo.util;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.UUID;

public final class AttrUtil {
    private AttrUtil() {
    }

    public static void upsertTransient(AttributeInstance inst, UUID id, String name, double amount,
            AttributeModifier.Operation op) {
        if (inst == null)
            return;

        // Limpia siempre el anterior
        inst.removeModifier(id);

        // Si no hay bonus, listo (queda limpio)
        if (amount == 0.0D)
            return;

        inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
    }
}
