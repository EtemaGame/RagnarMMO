package com.etema.ragnarmmo.system.economy.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerWalletProvider implements ICapabilitySerializable<CompoundTag> {

    public static Capability<PlayerWallet> PLAYER_WALLET = CapabilityManager.get(new CapabilityToken<PlayerWallet>() {});

    private final PlayerWallet wallet = new PlayerWallet();
    private final LazyOptional<PlayerWallet> optional = LazyOptional.of(() -> wallet);

    /**
     * Helper param accessor
     */
    public static LazyOptional<PlayerWallet> get(Player player) {
        return player.getCapability(PLAYER_WALLET);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_WALLET) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return wallet.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        wallet.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
