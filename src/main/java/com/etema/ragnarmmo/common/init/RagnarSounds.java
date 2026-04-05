package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RagnarSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RagnarMMO.MODID);

    public static final RegistryObject<SoundEvent> BASH = registerSound("skill.bash");
    public static final RegistryObject<SoundEvent> MAGNUM_BREAK = registerSound("skill.magnum_break");
    public static final RegistryObject<SoundEvent> ENDURE = registerSound("skill.endure");
    public static final RegistryObject<SoundEvent> PROVOKE = registerSound("skill.provoke");

    // New Skill Sounds
    public static final RegistryObject<SoundEvent> BLESSING = registerSound("skill.blessing");
    public static final RegistryObject<SoundEvent> INCREASE_AGI = registerSound("skill.increase_agi");
    public static final RegistryObject<SoundEvent> INC_AGI_DEX = registerSound("skill.inc_agi_dex");
    public static final RegistryObject<SoundEvent> ANGELUS = registerSound("skill.angelus");
    public static final RegistryObject<SoundEvent> MAGNIFICAT = registerSound("skill.magnificat");
    public static final RegistryObject<SoundEvent> GLORIA = registerSound("skill.gloria");
    public static final RegistryObject<SoundEvent> KYRIE_ELEISON = registerSound("skill.kyrie_eleison");
    public static final RegistryObject<SoundEvent> IMPOSITIO_MANUS = registerSound("skill.impositio_manus");
    public static final RegistryObject<SoundEvent> SUFFRAGIUM = registerSound("skill.suffragium");
    public static final RegistryObject<SoundEvent> ASPERSION = registerSound("skill.aspersion");
    public static final RegistryObject<SoundEvent> CONCENTRATION = registerSound("skill.concentration");
    public static final RegistryObject<SoundEvent> HIDING = registerSound("skill.hiding");
    public static final RegistryObject<SoundEvent> CAST = registerSound("skill.cast");
    public static final RegistryObject<SoundEvent> WARP_PORTAL = registerSound("skill.warp_portal");
    public static final RegistryObject<SoundEvent> CURE = registerSound("skill.cure");
    public static final RegistryObject<SoundEvent> DILECTIO_HEAL = registerSound("skill.dilectio_heal");
    public static final RegistryObject<SoundEvent> TELEPORTATION = registerSound("skill.teleportation");

    // Mage Skill Sounds
    public static final RegistryObject<SoundEvent> SIGHT = registerSound("skill.sight");
    public static final RegistryObject<SoundEvent> FIRE_BALL = registerSound("skill.fire_ball");
    public static final RegistryObject<SoundEvent> FIRE_WALL = registerSound("skill.fire_wall");
    public static final RegistryObject<SoundEvent> LIGHT_BOLT = registerSound("skill.light_bolt");
    public static final RegistryObject<SoundEvent> THUNDER_STORM = registerSound("skill.thunder_storm");
    public static final RegistryObject<SoundEvent> SOUL_STRIKE = registerSound("skill.soul_strike");
    public static final RegistryObject<SoundEvent> NAPALM_BEAT = registerSound("skill.napalm_beat");
    public static final RegistryObject<SoundEvent> STONE_CURSE = registerSound("skill.stone_curse");
    public static final RegistryObject<SoundEvent> GLASS_WALL = registerSound("skill.glass_wall");
    public static final RegistryObject<SoundEvent> BOLT_HIT = registerSound("skill.bolt_hit");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(RagnarMMO.MODID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
