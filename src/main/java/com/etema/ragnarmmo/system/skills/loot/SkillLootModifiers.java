package com.etema.ragnarmmo.system.skills.loot;

import com.etema.ragnarmmo.RagnarMMO;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SkillLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, RagnarMMO.MODID);

    public static final RegistryObject<Codec<com.etema.ragnarmmo.system.loot.RagnarLootModifier>> SKILL_LOOT = LOOT_MODIFIER_SERIALIZERS
            .register("skill_loot", com.etema.ragnarmmo.system.loot.RagnarLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIER_SERIALIZERS.register(bus);
    }
}
