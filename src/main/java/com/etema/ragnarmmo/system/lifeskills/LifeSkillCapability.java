package com.etema.ragnarmmo.system.lifeskills;

import com.etema.ragnarmmo.RagnarMMO;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Capability provider for Life Skills.
 * Stores and persists point-based life skill progression.
 */
@Mod.EventBusSubscriber(modid = RagnarMMO.MODID)
public class LifeSkillCapability implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<LifeSkillManager> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    private static final ResourceLocation ID = new ResourceLocation(RagnarMMO.MODID, "life_skills");

    private final LifeSkillManager manager = new LifeSkillManager();
    private final LazyOptional<LifeSkillManager> optional = LazyOptional.of(() -> manager);

    public LifeSkillCapability(Player player) {
        manager.setPlayer(player);
    }

    /**
     * Get life skill manager for an entity.
     */
    public static LazyOptional<LifeSkillManager> get(Entity entity) {
        if (entity == null) {
            return LazyOptional.empty();
        }
        return entity.getCapability(CAP);
    }

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player player) {
            event.addCapability(ID, new LifeSkillCapability(player));
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // FIX: Revive caps before reading to prevent death reset bug
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
        }
        try {
            event.getOriginal().getCapability(CAP)
                    .ifPresent(oldCap -> event.getEntity().getCapability(CAP).ifPresent(newCap -> {
                        newCap.deserializeNBT(oldCap.serializeNBT());
                    }));
        } finally {
            if (event.isWasDeath()) {
                event.getOriginal().invalidateCaps();
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncToClient(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
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
        get(player).ifPresent(manager -> {
            com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                    new LifeSkillSyncPacket(manager.serializeNBT()));
        });
    }

    /**
     * Sync specific skill to client.
     */
    public static void syncSkillToClient(ServerPlayer player, com.etema.ragnarmmo.common.api.lifeskills.LifeSkillType skillType) {
        get(player).ifPresent(manager -> {
            LifeSkillProgress progress = manager.getSkill(skillType);
            if (progress != null) {
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                        new LifeSkillUpdatePacket(skillType, progress.getLevel(), progress.getPoints()));
            }
        });
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return manager.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        manager.deserializeNBT(nbt);
    }
}
