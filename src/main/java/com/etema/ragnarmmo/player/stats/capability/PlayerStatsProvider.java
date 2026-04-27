package com.etema.ragnarmmo.player.stats.capability;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.debug.RagnarDebugLog;
import com.etema.ragnarmmo.player.stats.PlayerStatsModule;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.etema.ragnarmmo.player.stats.compute.StatResolutionService;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "ragnarmmo")
public class PlayerStatsProvider
        implements ICapabilityProvider, net.minecraftforge.common.util.INBTSerializable<CompoundTag> {
    public static final Capability<IPlayerStats> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation PLAYER_STATS_ID = ResourceLocation.fromNamespaceAndPath(PlayerStatsModule.MOD_ID, "player_stats");

    private final PlayerStats impl;
    private final LazyOptional<IPlayerStats> opt;

    public PlayerStatsProvider(Player owner) {
        this.impl = new PlayerStats();
        this.impl.bind(owner);
        this.opt = LazyOptional.of(() -> impl);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player player) {
            RagnarDebugLog.playerData("ATTACH_CAP player={}", playerName(player));
            evt.addCapability(PLAYER_STATS_ID, new PlayerStatsProvider(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (e.isWasDeath()) {
            e.getOriginal().reviveCaps();
        }
        
        try {
            var oldOpt = e.getOriginal().getCapability(CAP);
            var newOpt = e.getEntity().getCapability(CAP);
            
            if (oldOpt.isPresent() && newOpt.isPresent()) {
                IPlayerStats old = oldOpt.resolve().get();
                IPlayerStats cur = newOpt.resolve().get();
                cur.deserializeNBT(old.serializeNBT());
                cur.markDirty();
                RagnarDebugLog.playerData(
                        "CLONE player={} wasDeath={} baseLv={} jobLv={} exp={} jobExp={}",
                        playerName(e.getEntity()),
                        e.isWasDeath(),
                        cur.getLevel(),
                        cur.getJobLevel(),
                        cur.getExp(),
                        cur.getJobExp());
            } else {
                com.etema.ragnarmmo.RagnarMMO.LOGGER.error("Failed to clone PlayerStats: caps missing! (Original: {}, Current: {})",
                    oldOpt.isPresent(), newOpt.isPresent());
            }
        } catch (Exception ex) {
            com.etema.ragnarmmo.RagnarMMO.LOGGER.error("Error during PlayerStats cloning: ", ex);
        } finally {
            if (e.isWasDeath()) {
                e.getOriginal().invalidateCaps();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resolveAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target) {
            target.getCapability(CAP).ifPresent(stats -> {
                com.etema.ragnarmmo.player.stats.network.PlayerStatsSyncService.sync(target, stats);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(CAP).ifPresent(stats -> {
                // Forzar el recálculo y aplicación de MaxHealth antes de rellenar.
                var derived = com.etema.ragnarmmo.player.stats.compute.StatComputer.compute(
                        player, stats, com.etema.ragnarmmo.player.stats.compute.EquipmentStatSnapshot.capture(player));

                var maxHealthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(derived.maxHealth);
                }

                // Regenerar vida y comida como vanilla
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(5.0f);
            });

            resolveAndSync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            resolveAndSync(player);
        }
    }

    private static void resolveAndSync(ServerPlayer player) {
        player.getCapability(CAP).ifPresent(stats -> {
            RagnarDebugLog.playerData("RESOLVE_SYNC_TRIGGER player={} reason=provider_hook", playerName(player));
            StatResolutionService.resolve(player, stats);
        });
    }

    private static String playerName(Player player) {
        if (player == null) {
            return "null";
        }

        var profile = player.getGameProfile();
        if (profile != null) {
            String name = profile.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }

        String uuid = player.getStringUUID();
        return (uuid == null || uuid.isBlank()) ? "unknown" : uuid;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? opt.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return impl.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        impl.deserializeNBT(nbt);
    }
}
