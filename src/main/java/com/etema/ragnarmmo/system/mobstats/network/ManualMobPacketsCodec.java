package com.etema.ragnarmmo.system.mobstats.network;

import com.etema.ragnarmmo.common.api.mobs.MobRank;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.InternalManualMobEntry;
import com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobCatalogEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

final class ManualMobPacketsCodec {

    private ManualMobPacketsCodec() {
    }

    static void writeCatalogEntry(FriendlyByteBuf buf, ManualMobCatalogEntry entry) {
        buf.writeUtf(entry.entityTypeId());
        buf.writeUtf(entry.namespace());
        buf.writeUtf(entry.displayName());
        buf.writeBoolean(entry.internalCoverage());
        buf.writeBoolean(entry.datapackCoverage());
        buf.writeUtf(entry.effectiveBackend());
        buf.writeBoolean(entry.enabled());
    }

    static ManualMobCatalogEntry readCatalogEntry(FriendlyByteBuf buf) {
        return new ManualMobCatalogEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBoolean());
    }

    static void writeInternalEntry(FriendlyByteBuf buf, InternalManualMobEntry e) {
        buf.writeResourceLocation(e.entityTypeId());
        buf.writeBoolean(e.enabled());
        buf.writeInt(e.level());
        buf.writeEnum(e.rank());
        buf.writeUtf(e.race());
        buf.writeUtf(e.element());
        buf.writeUtf(e.size());
        buf.writeInt(e.maxHp());
        buf.writeInt(e.atkMin());
        buf.writeInt(e.atkMax());
        buf.writeInt(e.def());
        buf.writeInt(e.mdef());
        buf.writeInt(e.hit());
        buf.writeInt(e.flee());
        buf.writeInt(e.crit());
        buf.writeInt(e.aspd());
        buf.writeDouble(e.moveSpeed());
        buf.writeUtf(e.notes());
        buf.writeUtf(e.lastEditedBy());
        buf.writeLong(e.lastEditedAt());
    }

    static InternalManualMobEntry readInternalEntry(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        return new InternalManualMobEntry(
                id,
                buf.readBoolean(),
                buf.readInt(),
                buf.readEnum(MobRank.class),
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readDouble(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readLong());
    }
}
