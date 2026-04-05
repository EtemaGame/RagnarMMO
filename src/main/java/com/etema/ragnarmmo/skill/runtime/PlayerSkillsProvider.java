package com.etema.ragnarmmo.skill.runtime;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Capability provider for player skills.
 * Persists skill levels and XP across sessions.
 */
@Mod.EventBusSubscriber(modid = "ragnarmmo")
public class PlayerSkillsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<SkillManager> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });
    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "player_skills");

    private final SkillManager skillManager = new SkillManager();
    private final LazyOptional<SkillManager> optional = LazyOptional.of(() -> skillManager);

    public PlayerSkillsProvider(Player player) {
        skillManager.setPlayer(player);
    }

    public static LazyOptional<SkillManager> get(Entity entity) {
        if (entity == null) {
            return LazyOptional.empty();
        }
        return entity.getCapability(CAP);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            event.addCapability(ID, new PlayerSkillsProvider(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // FIX CRÍTICO: Al morir, las capabilities del original pueden estar invalidadas.
        // Debemos revivirlas temporalmente para copiar los datos.
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
        }
        try {
            event.getOriginal().getCapability(CAP)
                    .ifPresent(oldCap -> event.getEntity().getCapability(CAP).ifPresent(newCap -> {
                        newCap.deserializeNBT(oldCap.serializeNBT());
                    }));
        } finally {
            // Invalidar de nuevo las caps del original si fue muerte
            if (event.isWasDeath()) {
                event.getOriginal().invalidateCaps();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
            SkillEffectHandler.refreshPassiveEffects(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
            SkillEffectHandler.refreshPassiveEffects(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
            SkillEffectHandler.refreshPassiveEffects(player);
        }
    }

    private static void syncToClient(ServerPlayer player) {
        get(player).ifPresent(skills -> {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                    new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(skills.serializeNBT()));
        });
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return skillManager.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        skillManager.deserializeNBT(nbt);
    }
}
