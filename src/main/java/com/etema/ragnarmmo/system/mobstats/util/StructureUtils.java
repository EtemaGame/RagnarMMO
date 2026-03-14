package com.etema.ragnarmmo.system.mobstats.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;
import java.util.List;

public class StructureUtils {

    /**
     * Devuelve el mayor factor de estructura aplicable según la cercanía del mob.
     * 
     * @param configList Lista de strings en formato 'structure_id=radius,factor'
     */
    public static double getNearestStructureFactor(LivingEntity mob, List<? extends String> configList) {
        if (!(mob.level() instanceof ServerLevel sl) || configList.isEmpty())
            return 1.0;

        BlockPos pos = mob.blockPosition();
        StructureManager sm = sl.structureManager();
        double maxFactor = 1.0;

        for (String entry : configList) {
            try {
                String[] parts = entry.split("=");
                if (parts.length < 2)
                    continue;
                String structIdStr = parts[0];
                String[] values = parts[1].split(",");
                if (values.length < 2)
                    continue;

                // double radius = Double.parseDouble(values[0]); // Currently unused, using MC
                // bounding box
                double factor = Double.parseDouble(values[1]);

                ResourceLocation structId = ResourceLocation.parse(structIdStr);
                Registry<Structure> structureRegistry = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
                Structure struct = structureRegistry.get(structId);

                if (struct != null) {
                    // Verificamos si hay alguna pieza de la estructura cerca
                    if (sm.getStructureAt(pos, struct).isValid()) {
                        maxFactor = Math.max(maxFactor, factor);
                    } else {
                        // Opcional: Proximidad real si no estamos 'dentro'
                        // Por ahora, el sistema de MC sm.getStructureAt ya maneja el bounding box
                        // extendido.
                        // Para radios específicos manuales, se requeriría una búsqueda más pesada.
                        // Usaremos el bounding box de la estructura como base por ahora.
                    }
                }
            } catch (Exception e) {
                // Ignore malformed config
            }
        }
        return maxFactor;
    }

    public static Optional<ResourceLocation> getStructureAt(LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel sl))
            return Optional.empty();

        BlockPos pos = mob.blockPosition();
        StructureManager sm = sl.structureManager();
        Registry<Structure> structureRegistry = sl.registryAccess()
                .registryOrThrow(Registries.STRUCTURE);

        for (ResourceLocation id : structureRegistry.keySet()) {
            Structure struct = structureRegistry.get(id);

            if (struct != null && sm.getStructureAt(pos, struct).isValid()) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
