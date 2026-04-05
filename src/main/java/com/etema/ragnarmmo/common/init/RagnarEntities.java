package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.entity.projectile.MagicProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class RagnarEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RagnarMMO.MODID);

    public static final RegistryObject<EntityType<MagicProjectileEntity>> MAGIC_PROJECTILE = 
            ENTITIES.register("magic_projectile", () -> EntityType.Builder.<MagicProjectileEntity>of(MagicProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("magic_projectile"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile>> SOUL_STRIKE_PROJECTILE = 
            ENTITIES.register("soul_strike_projectile", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile>of(com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("soul_strike_projectile"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.NapalmBeatAoe>> NAPALM_BEAT_AOE = 
            ENTITIES.register("napalm_beat_aoe", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.NapalmBeatAoe>of(com.etema.ragnarmmo.entity.aoe.NapalmBeatAoe::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("napalm_beat_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.projectile.FireBoltProjectile>> FIRE_BOLT_PROJECTILE = 
            ENTITIES.register("fire_bolt_projectile", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.projectile.FireBoltProjectile>of(com.etema.ragnarmmo.entity.projectile.FireBoltProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("fire_bolt_projectile"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.projectile.IceBoltProjectile>> ICE_BOLT_PROJECTILE = 
            ENTITIES.register("ice_bolt_projectile", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.projectile.IceBoltProjectile>of(com.etema.ragnarmmo.entity.projectile.IceBoltProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("ice_bolt_projectile"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile>> LIGHTNING_BOLT_PROJECTILE = 
            ENTITIES.register("lightning_bolt_projectile", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile>of(com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("lightning_bolt_projectile"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.HeavensDriveAoe>> HEAVENS_DRIVE_AOE = 
            ENTITIES.register("heavens_drive_aoe", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.HeavensDriveAoe>of(com.etema.ragnarmmo.entity.aoe.HeavensDriveAoe::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("heavens_drive_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.StormGustAoe>> STORM_GUST_AOE = 
            ENTITIES.register("storm_gust_aoe", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.StormGustAoe>of(com.etema.ragnarmmo.entity.aoe.StormGustAoe::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("storm_gust_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.FireWallAoe>> FIRE_WALL_AOE = 
            ENTITIES.register("fire_wall_aoe", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.FireWallAoe>of(com.etema.ragnarmmo.entity.aoe.FireWallAoe::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("fire_wall_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.SanctuaryAoe>> SANCTUARY_AOE = 
            ENTITIES.register("sanctuary_aoe", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.SanctuaryAoe>of(com.etema.ragnarmmo.entity.aoe.SanctuaryAoe::new, MobCategory.MISC)
                    .sized(2.0f, 1.0f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("sanctuary_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.WarpPortalAoe>> WARP_PORTAL_AOE =
            ENTITIES.register("warp_portal_aoe",
                    () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.WarpPortalAoe>of(
                                    com.etema.ragnarmmo.entity.aoe.WarpPortalAoe::new, MobCategory.MISC)
                            .sized(2.5f, 1.0f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("warp_portal_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.aoe.PneumaAoe>> PNEUMA_AOE =
            ENTITIES.register("pneuma_aoe",
                    () -> EntityType.Builder.<com.etema.ragnarmmo.entity.aoe.PneumaAoe>of(
                                    com.etema.ragnarmmo.entity.aoe.PneumaAoe::new, MobCategory.MISC)
                            .sized(3.0f, 1.0f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("pneuma_aoe"));

    public static final RegistryObject<EntityType<com.etema.ragnarmmo.entity.effect.StatusOverlayEntity>> STATUS_OVERLAY =
            ENTITIES.register("status_overlay", () -> EntityType.Builder.<com.etema.ragnarmmo.entity.effect.StatusOverlayEntity>of(
                            com.etema.ragnarmmo.entity.effect.StatusOverlayEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build("status_overlay"));

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
