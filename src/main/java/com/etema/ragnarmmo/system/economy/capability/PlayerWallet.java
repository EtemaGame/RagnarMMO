package com.etema.ragnarmmo.system.economy.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

public class PlayerWallet implements INBTSerializable<CompoundTag> {

    private long zeny = 0;
    private boolean isDirty = true; // Force sync on init

    public long getZeny() {
        return zeny;
    }

    public void setZeny(long amount) {
        if (this.zeny != amount) {
            this.zeny = Math.max(0, amount);
            this.isDirty = true;
        }
    }

    public void addZeny(long amount) {
        if (amount > 0) {
            setZeny(this.zeny + amount);
        }
    }

    public boolean consumeZeny(long amount) {
        if (amount <= 0) return true;
        if (this.zeny >= amount) {
            setZeny(this.zeny - amount);
            return true;
        }
        return false;
    }

    public boolean consumeDirty() {
        if (isDirty) {
            isDirty = false;
            return true;
        }
        return false;
    }

    public void markDirty() {
        this.isDirty = true;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Zeny", this.zeny);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Zeny")) {
            this.zeny = nbt.getLong("Zeny");
        }
    }
}
