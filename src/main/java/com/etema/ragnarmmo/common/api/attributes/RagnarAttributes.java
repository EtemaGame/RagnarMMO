package com.etema.ragnarmmo.common.api.attributes;

import com.etema.ragnarmmo.common.init.RagnarCore;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RagnarAttributes {
        public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES,
                        RagnarCore.MODID);

        public static final RegistryObject<Attribute> STR = ATTRIBUTES.register("str",
                        () -> buildAttribute("attribute.name.ragnar.str"));
        public static final RegistryObject<Attribute> AGI = ATTRIBUTES.register("agi",
                        () -> buildAttribute("attribute.name.ragnar.agi"));
        public static final RegistryObject<Attribute> VIT = ATTRIBUTES.register("vit",
                        () -> buildAttribute("attribute.name.ragnar.vit"));
        public static final RegistryObject<Attribute> INT = ATTRIBUTES.register("int",
                        () -> buildAttribute("attribute.name.ragnar.int"));
        public static final RegistryObject<Attribute> DEX = ATTRIBUTES.register("dex",
                        () -> buildAttribute("attribute.name.ragnar.dex"));
        public static final RegistryObject<Attribute> LUK = ATTRIBUTES.register("luk",
                        () -> buildAttribute("attribute.name.ragnar.luk"));

        // Resource attributes — used as MaxMana / MaxSP pools
        public static final RegistryObject<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.max_mana", 0.0D, 0.0D, 100000.0D));
        public static final RegistryObject<Attribute> MAX_SP = ATTRIBUTES.register("max_sp",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.max_sp", 0.0D, 0.0D, 100000.0D));

        // Extended combat attributes (previously from AttributesLib/Apotheosis)
        public static final RegistryObject<Attribute> CRIT_CHANCE = ATTRIBUTES.register("crit_chance",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.crit_chance", 0.0D, 0.0D, 1.0D));
        public static final RegistryObject<Attribute> CRIT_DAMAGE = ATTRIBUTES.register("crit_damage",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.crit_damage", 1.5D, 0.0D, 100.0D));
        public static final RegistryObject<Attribute> LIFE_STEAL = ATTRIBUTES.register("life_steal",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.life_steal", 0.0D, 0.0D, 1.0D));
        public static final RegistryObject<Attribute> ARMOR_PIERCE = ATTRIBUTES.register("armor_pierce",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.armor_pierce", 0.0D, 0.0D, 1.0D));
        public static final RegistryObject<Attribute> ARMOR_SHRED = ATTRIBUTES.register("armor_shred",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.armor_shred", 0.0D, 0.0D, 1.0D));
        public static final RegistryObject<Attribute> OVERHEAL = ATTRIBUTES.register("overheal",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.overheal", 0.0D, 0.0D, 100.0D));

        public static final RegistryObject<Attribute> MAGIC_DEFENSE = ATTRIBUTES.register("magic_defense",
                        () -> new SyncableRangedAttribute("attribute.name.ragnar.magic_defense", 0.0D, 0.0D, 1000.0D));

        public static void register(IEventBus bus) {
                ATTRIBUTES.register(bus);
        }

        private static Attribute buildAttribute(String translationKey) {
                return new SyncableRangedAttribute(translationKey, 1.0D, 0.0D, 1000.0D);
        }

        private static final class SyncableRangedAttribute extends RangedAttribute {
                private SyncableRangedAttribute(String translationKey, double defaultValue, double minValue,
                                double maxValue) {
                        super(translationKey, defaultValue, minValue, maxValue);
                }

                @Override
                public boolean isClientSyncable() {
                        return true;
                }
        }
}
