package com.etema.ragnarmmo.system.loot;

import com.etema.ragnarmmo.RagnarMMO;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for RagnarMMO global loot modifiers.
 */
public final class RagnarLootModifiers {

    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RagnarMMO.MODID);

    public static final RegistryObject<Codec<RagnarLootModifier>> RAGNAR_LOOT =
            LOOT_MODIFIER_SERIALIZERS.register("ragnar_loot", RagnarLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIER_SERIALIZERS.register(bus);
    }

    private RagnarLootModifiers() {
    }
}
