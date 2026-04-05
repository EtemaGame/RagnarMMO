package com.etema.ragnarmmo.system.mobstats.core.capability;

import com.etema.ragnarmmo.system.mobstats.RagnarMobStats;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Capability that stores all generated stats for a mob. It is attached to every
 * living entity that is not a player so levels and tiers survive chunk unloads
 * and world saves.
 */
@Mod.EventBusSubscriber(modid = RagnarMobStats.MOD_ID)
public final class MobStatsProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<MobStats> CAP = CapabilityManager.get(new CapabilityToken<>() {
    });

    private final MobStats stats = new MobStats();
    private final LazyOptional<MobStats> optional = LazyOptional.of(() -> stats);

    public MobStatsProvider(LivingEntity entity) {
        // living entity reference reserved for future features (loot integration).
        stats.setInitialized(false);
    }

    public static LazyOptional<MobStats> get(Entity entity) {
        if (entity == null) {
            return LazyOptional.empty();
        }
        return entity.getCapability(CAP);
    }

    @SuppressWarnings("removal") // ResourceLocation constructor deprecated in 1.20.4+, valid for 1.20.1
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof LivingEntity living && !(entity instanceof Player)) {
            event.addCapability(new ResourceLocation(RagnarMobStats.MOD_ID, "mob_stats"), new MobStatsProvider(living));
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof LivingEntity living) || living instanceof Player) {
            return;
        }
        get(living).ifPresent(stats -> {
            // Ensure defaults when loading from disk without explicit initialization flag.
            if (!stats.isInitialized()) {
                stats.setTier(stats.getTier());
                stats.setHealthMultiplier(Math.max(0.0D, stats.getHealthMultiplier()));
                stats.setDamageMultiplier(Math.max(0.0D, stats.getDamageMultiplier()));
                stats.setDefenseMultiplier(Math.max(0.0D, stats.getDefenseMultiplier()));
                stats.setSpeedMultiplier(Math.max(0.0D, stats.getSpeedMultiplier()));
                for (StatKeys key : StatKeys.values()) {
                    stats.set(key, Math.max(0, stats.get(key)));
                }
            }

            // Previously mirrored basic mob info to persistent entity data, which is no
            // longer required.
        });
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == CAP ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", stats.getLevel());
        tag.putString("Tier", stats.getTier().name());
        tag.putBoolean("Initialized", stats.isInitialized());
        tag.putDouble("HealthMultiplier", stats.getHealthMultiplier());
        tag.putDouble("DamageMultiplier", stats.getDamageMultiplier());
        tag.putDouble("DefenseMultiplier", stats.getDefenseMultiplier());
        tag.putDouble("SpeedMultiplier", stats.getSpeedMultiplier());

        CompoundTag statsTag = new CompoundTag();
        for (StatKeys key : StatKeys.values()) {
            statsTag.putInt(key.id(), stats.get(key));
        }
        tag.put("Stats", statsTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        stats.setLevel(nbt.contains("Level") ? nbt.getInt("Level") : 1);
        String tierName = nbt.contains("Tier") ? nbt.getString("Tier") : "";
        try {
            stats.setTier(tierName == null || tierName.isEmpty()
                    ? stats.getTier()
                    : com.etema.ragnarmmo.common.api.mobs.MobTier.valueOf(tierName));
        } catch (IllegalArgumentException ex) {
            stats.setTier(com.etema.ragnarmmo.common.api.mobs.MobTier.NORMAL);
        }
        stats.setInitialized(nbt.getBoolean("Initialized"));
        if (nbt.contains("HealthMultiplier")) {
            stats.setHealthMultiplier(nbt.getDouble("HealthMultiplier"));
        }
        if (nbt.contains("DamageMultiplier")) {
            stats.setDamageMultiplier(nbt.getDouble("DamageMultiplier"));
        }
        if (nbt.contains("DefenseMultiplier")) {
            stats.setDefenseMultiplier(nbt.getDouble("DefenseMultiplier"));
        }
        if (nbt.contains("SpeedMultiplier")) {
            stats.setSpeedMultiplier(nbt.getDouble("SpeedMultiplier"));
        }

        stats.resetStats();
        if (nbt.contains("Stats")) {
            CompoundTag statsTag = nbt.getCompound("Stats");
            for (StatKeys key : StatKeys.values()) {
                String id = key.id();
                if (statsTag.contains(id)) {
                    stats.set(key, statsTag.getInt(id));
                }
            }
        }
    }
}






