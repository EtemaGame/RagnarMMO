package com.etema.ragnarmmo.system.bar;

import net.minecraft.world.entity.LivingEntity;

public interface EntityStatResolver {
    /** Nombre mostrado (puede incluir displayName personalizado) */
    String getDisplayName(LivingEntity e);

    /** Nivel mostrado; devuelve "" si no hay datos (para ocultar el tag) */
    String getLevel(LivingEntity e);

    /** Rango/tier del mob o rol del player; "" si no aplica */
    String getRank(LivingEntity e);

    /** Clase/especie/ocupación opcional; "" si no aplica */
    String getClazz(LivingEntity e);
}






