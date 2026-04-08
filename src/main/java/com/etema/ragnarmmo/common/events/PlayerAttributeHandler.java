package com.etema.ragnarmmo.common.events;

import com.etema.ragnarmmo.common.init.RagnarCore;
import com.etema.ragnarmmo.common.api.attributes.RagnarAttributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod.EventBusSubscriber(modid = RagnarCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PlayerAttributeHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    private static final ResourceLocation PLAYER_ID = new ResourceLocation("minecraft", "player");

    private PlayerAttributeHandler() {
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        EntityType<? extends LivingEntity> playerType = findPlayerType(event);
        if (playerType == null) {
            LOGGER.error("Unable to locate the minecraft:player entity type while attaching Ragnar attributes.");
            return;
        }

        event.add(playerType, RagnarAttributes.STR.get());
        event.add(playerType, RagnarAttributes.AGI.get());
        event.add(playerType, RagnarAttributes.VIT.get());
        event.add(playerType, RagnarAttributes.INT.get());
        event.add(playerType, RagnarAttributes.DEX.get());
        event.add(playerType, RagnarAttributes.LUK.get());
        event.add(playerType, RagnarAttributes.MAX_MANA.get());
        event.add(playerType, RagnarAttributes.MAX_SP.get());
        event.add(playerType, RagnarAttributes.CRIT_CHANCE.get());
        event.add(playerType, RagnarAttributes.CRIT_DAMAGE.get());
        event.add(playerType, RagnarAttributes.LIFE_STEAL.get());
        event.add(playerType, RagnarAttributes.ARMOR_PIERCE.get());
        event.add(playerType, RagnarAttributes.ARMOR_SHRED.get());
        event.add(playerType, RagnarAttributes.OVERHEAL.get());
        event.add(playerType, RagnarAttributes.MAGIC_DEFENSE.get());
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends LivingEntity> findPlayerType(EntityAttributeModificationEvent event) {
        for (EntityType<?> type : event.getTypes()) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (PLAYER_ID.equals(id)) {
                return (EntityType<? extends LivingEntity>) type;
            }
        }

        EntityType<?> registryType = ForgeRegistries.ENTITY_TYPES.getValue(PLAYER_ID);
        if (registryType != null && LivingEntity.class.isAssignableFrom(registryType.getBaseClass())) {
            return (EntityType<? extends LivingEntity>) registryType;
        }

        return null;
    }
}
