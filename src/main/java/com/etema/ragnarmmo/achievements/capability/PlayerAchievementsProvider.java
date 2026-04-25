package com.etema.ragnarmmo.achievements.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerAchievementsProvider implements ICapabilitySerializable<CompoundTag> {

    public static final Capability<IPlayerAchievements> PLAYER_ACHIEVEMENTS = CapabilityManager
            .get(new CapabilityToken<>() {
            });

    private IPlayerAchievements achievements = null;
    private final LazyOptional<IPlayerAchievements> optional = LazyOptional.of(this::createAchievements);

    private IPlayerAchievements createAchievements() {
        if (this.achievements == null) {
            this.achievements = new PlayerAchievements();
        }
        return this.achievements;
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_ACHIEVEMENTS) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createAchievements().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createAchievements().deserializeNBT(nbt);
    }
}
