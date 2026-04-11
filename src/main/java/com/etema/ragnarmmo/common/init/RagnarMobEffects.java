package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RagnarMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RagnarMMO.MODID);

    public static final RegistryObject<MobEffect> ENDURE = MOB_EFFECTS.register("endure", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFFFF00));

    public static final RegistryObject<MobEffect> BLESSING = MOB_EFFECTS.register("blessing", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFFD700)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.STR.get(), "7107DE5E-7CE8-4030-940E-514C1F160890", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.INT.get(), "7107DE5E-7CE8-4030-940E-514C1F160891", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.DEX.get(), "7107DE5E-7CE8-4030-940E-514C1F160892", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> INCREASE_AGI = MOB_EFFECTS.register("increase_agi", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0x87CEEB)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.AGI.get(), "7107DE5E-7CE8-4030-940E-514C1F160893", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160894", 0.05D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistryObject<MobEffect> ANGELUS = MOB_EFFECTS.register("angelus", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFFFFFF)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, "7107DE5E-7CE8-4030-940E-514C1F160895", 2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> SIGNUM_CRUCIS = MOB_EFFECTS.register("signum_crucis",
            com.etema.ragnarmmo.skill.job.acolyte.SignumCrucisMobEffect::new);

    public static final RegistryObject<MobEffect> MAGNIFICAT = MOB_EFFECTS.register("magnificat", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xE6E6FA));

    public static final RegistryObject<MobEffect> GLORIA = MOB_EFFECTS.register("gloria", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFFDF00)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.LUK.get(), "7107DE5E-7CE8-4030-940E-514C1F160896", 3.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> IMPOSITIO_MANUS = MOB_EFFECTS.register("impositio_manus", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFF4500)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, "7107DE5E-7CE8-4030-940E-514C1F160897", 2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> SUFFRAGIUM = MOB_EFFECTS.register("suffragium", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xADD8E6)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, "7107DE5E-7CE8-4030-940E-514C1F16089B", 0.1D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));

    public static final RegistryObject<MobEffect> ASPERSION = MOB_EFFECTS.register("aspersion", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xF0F8FF)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, "7107DE5E-7CE8-4030-940E-514C1F16089C", 2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> KYRIE_ELEISON = MOB_EFFECTS.register("kyrie_eleison", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xB0C4DE)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR, "7107DE5E-7CE8-4030-940E-514C1F16089D", 4.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS, "7107DE5E-7CE8-4030-940E-514C1F16089E", 2.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> IMPROVE_CONCENTRATION = MOB_EFFECTS.register("improve_concentration", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0x98FB98)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.AGI.get(), "7107DE5E-7CE8-4030-940E-514C1F160898", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.DEX.get(), "7107DE5E-7CE8-4030-940E-514C1F160899", 1.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> CRAZY_UPROAR = MOB_EFFECTS.register("crazy_uproar", 
            () -> new StandardMobEffect(MobEffectCategory.BENEFICIAL, 0xFF6347)
                    .addAttributeModifier(com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.STR.get(), "7107DE5E-7CE8-4030-940E-514C1F16089A", 4.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));

    public static final RegistryObject<MobEffect> MAGNUM_BREAK_FIRE = MOB_EFFECTS.register("magnum_break_fire",
            com.etema.ragnarmmo.skill.job.swordman.MagnumBreakFireMobEffect::new);

    public static final RegistryObject<MobEffect> SIGHT = MOB_EFFECTS.register("sight", 
            com.etema.ragnarmmo.skill.job.mage.SightMobEffect::new);

    public static final RegistryObject<MobEffect> FROZEN = MOB_EFFECTS.register("frozen", 
            com.etema.ragnarmmo.skill.job.mage.FrozenMobEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private static class StandardMobEffect extends MobEffect {
        protected StandardMobEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
