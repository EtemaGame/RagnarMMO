package com.etema.ragnarmmo.mobs.event;

import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MobAttributes {
    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    private static final ResourceLocation PLAYER_ID = new ResourceLocation("minecraft", "player");

    private MobAttributes() {}

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        for (EntityType<?> type : event.getTypes()) {
            if (PLAYER_ID.equals(ForgeRegistries.ENTITY_TYPES.getKey(type))) {
                continue;
            }

            // Ensure we only touch LivingEntity types
            if (!LivingEntity.class.isAssignableFrom(type.getBaseClass())) {
                continue;
            }

            // Register core stats to all living mobs
            addIfMissing(event, type, RagnarAttributes.STR.get());
            addIfMissing(event, type, RagnarAttributes.AGI.get());
            addIfMissing(event, type, RagnarAttributes.VIT.get());
            addIfMissing(event, type, RagnarAttributes.INT.get());
            addIfMissing(event, type, RagnarAttributes.DEX.get());
            addIfMissing(event, type, RagnarAttributes.LUK.get());
            addIfMissing(event, type, RagnarAttributes.MAGIC_DEFENSE.get());
        }
    }

    @SuppressWarnings("unchecked")
    private static void addIfMissing(EntityAttributeModificationEvent event, EntityType<?> type, Attribute attribute) {
        if (attribute == null || type == null) {
            return;
        }

        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        if (!event.has(livingType, attribute)) {
            event.add(livingType, attribute);
        }
    }
}






