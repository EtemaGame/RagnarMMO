package com.etema.ragnarmmo.system.stats.capability;

import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.system.stats.RagnarStats;

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

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = RagnarStats.MOD_ID)
public class PlayerStatsProvider
        implements ICapabilityProvider, net.minecraftforge.common.util.INBTSerializable<CompoundTag> {
    public static final Capability<IPlayerStats> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final PlayerStats impl;
    private final LazyOptional<IPlayerStats> opt;

    public PlayerStatsProvider(Player owner) {
        this.impl = new PlayerStats();
        this.impl.bind(owner);
        this.opt = LazyOptional.of(() -> impl);
    }

    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof Player player) {
            evt.addCapability(new ResourceLocation(RagnarStats.MOD_ID, "player_stats"),
                    new PlayerStatsProvider(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        // FIX CRÍTICO: Al morir, las capabilities del original pueden estar
        // invalidadas.
        // Debemos revivirlas temporalmente para copiar los datos.
        if (e.isWasDeath()) {
            e.getOriginal().reviveCaps();
        }
        try {
            e.getOriginal().getCapability(CAP).ifPresent(old -> e.getEntity().getCapability(CAP).ifPresent(cur -> {
                cur.deserializeNBT(old.serializeNBT());
                cur.markDirty();
            }));
        } finally {
            // Invalidar de nuevo las caps del original si fue muerte
            if (e.isWasDeath()) {
                e.getOriginal().invalidateCaps();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(CAP).ifPresent(stats -> {
                // Forzar el recálculo y aplicación de MaxHealth antes de rellenar.
                var derived = com.etema.ragnarmmo.system.stats.compute.StatComputer.compute(
                        player, stats, 0, 1.0, 0, player.getArmorValue(), 1.0);

                var maxHealthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(derived.maxHealth);
                }

                // Regenerar vida y comida como vanilla
                player.setHealth(player.getMaxHealth());
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(5.0f);
            });

            syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    private static void syncToClient(ServerPlayer player) {
        player.getCapability(CAP).ifPresent(stats -> {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                    new com.etema.ragnarmmo.system.stats.net.PlayerStatsSyncPacket(stats));
            // Also sync derived stats
            var derived = com.etema.ragnarmmo.system.stats.compute.StatComputer.compute(
                    player, stats, 0, 1.0, 0, player.getArmorValue(), 1.0);
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                    new com.etema.ragnarmmo.system.stats.net.DerivedStatsSyncPacket(derived));
        });
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
