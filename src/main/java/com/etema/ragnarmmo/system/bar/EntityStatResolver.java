package com.etema.ragnarmmo.system.bar;

import net.minecraft.world.entity.LivingEntity;

public interface EntityStatResolver {
    /** Nombre mostrado (puede incluir displayName personalizado) */
    String getDisplayName(LivingEntity e);

    /** Nivel mostrado; devuelve "" si no hay datos (para ocultar el tag) */
    String getLevel(LivingEntity e);

    /** Rango/tier del mob o rol del player; "" si no aplica */
    String getRank(LivingEntity e);

    /** Clase/especie/ocupacion opcional; "" si no aplica */
    String getClazz(LivingEntity e);

    /**
     * Optional secondary line for compact overlays.
     *
     * <p>This is kept separate from {@link #getClazz(LivingEntity)} so existing inline labels can
     * preserve their meaning while selected consumers opt into a second, smaller line.</p>
     */
    default String getSecondaryLabel(LivingEntity e) {
        return "";
    }

    /**
     * Primary label color for compact overlays.
     *
     * <p>Resolvers may tint the main line when they have normalized semantic data available.</p>
     */
    default int getPrimaryLabelColor(LivingEntity e) {
        return 0xFFFFFF;
    }

    /**
     * Secondary label color for compact overlays.
     *
     * <p>Resolvers may tint the secondary line when they have normalized taxonomy available.</p>
     */
    default int getSecondaryLabelColor(LivingEntity e) {
        return 0xD0D0D0;
    }
}
