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
        buf.writeBoolean(entry.internalPresent());
        buf.writeBoolean(entry.internalEnabled());
        buf.writeBoolean(entry.datapackCoverage());
        buf.writeUtf(entry.effectiveBackend());
        buf.writeBoolean(entry.manualEffective());
    }

    static ManualMobCatalogEntry readCatalogEntry(FriendlyByteBuf buf) {
        return new ManualMobCatalogEntry(
                buf.readUtf(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
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

    static void writeProfile(FriendlyByteBuf buf, com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile p) {
        buf.writeInt(p.level());
        buf.writeEnum(p.rank());
        buf.writeInt(p.maxHp());
        buf.writeInt(p.atkMin());
        buf.writeInt(p.atkMax());
        buf.writeInt(p.def());
        buf.writeInt(p.mdef());
        buf.writeInt(p.hit());
        buf.writeInt(p.flee());
        buf.writeInt(p.crit());
        buf.writeInt(p.aspd());
        buf.writeDouble(p.moveSpeed());
        buf.writeUtf(p.race());
        buf.writeUtf(p.element());
        buf.writeUtf(p.size());
        
        var stats = p.baseCombatStats();
        buf.writeBoolean(stats != null);
        if (stats != null) {
            buf.writeBoolean(stats.str() != null);
            if (stats.str() != null) buf.writeInt(stats.str());
            buf.writeInt(stats.vit());
            buf.writeInt(stats.intelligence());
            buf.writeInt(stats.agi());
            buf.writeInt(stats.luk());
            buf.writeBoolean(stats.dex() != null);
            if (stats.dex() != null) buf.writeInt(stats.dex());
        }
    }

    static com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile readProfile(FriendlyByteBuf buf) {
        int level = buf.readInt();
        MobRank rank = buf.readEnum(MobRank.class);
        int maxHp = buf.readInt();
        int atkMin = buf.readInt();
        int atkMax = buf.readInt();
        int def = buf.readInt();
        int mdef = buf.readInt();
        int hit = buf.readInt();
        int flee = buf.readInt();
        int crit = buf.readInt();
        int aspd = buf.readInt();
        double moveSpeed = buf.readDouble();
        String race = buf.readUtf();
        String element = buf.readUtf();
        String size = buf.readUtf();
        
        com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobBaseStats stats = null;
        if (buf.readBoolean()) {
            Integer str = buf.readBoolean() ? buf.readInt() : null;
            int vit = buf.readInt();
            int int_ = buf.readInt();
            int agi = buf.readInt();
            int luk = buf.readInt();
            Integer dex = buf.readBoolean() ? buf.readInt() : null;
            stats = new com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobBaseStats(
                    str, vit, int_, agi, luk, dex);
        }
        
        return new com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile(
                level, rank, maxHp, atkMin, atkMax, def, mdef, hit, flee, crit, aspd, moveSpeed, stats, race, element, size);
    }

    static void writeDetail(FriendlyByteBuf buf, com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail d) {
        buf.writeBoolean(d.internalEntry() != null);
        if (d.internalEntry() != null) writeInternalEntry(buf, d.internalEntry());
        
        buf.writeBoolean(d.effectiveProfile() != null);
        if (d.effectiveProfile() != null) writeProfile(buf, d.effectiveProfile());
        
        buf.writeBoolean(d.internalPresent());
        buf.writeBoolean(d.internalEnabled());
        buf.writeBoolean(d.datapackCoverage());
        buf.writeUtf(d.effectiveBackend());
        buf.writeBoolean(d.manualEffective());
        buf.writeUtf(d.scalingMode());
        buf.writeUtf(d.backendReason());
        buf.writeBoolean(d.canEdit());
        buf.writeBoolean(d.canDelete());
        buf.writeBoolean(d.canCreate());
    }

    static com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail readDetail(FriendlyByteBuf buf) {
        InternalManualMobEntry internalEntry = buf.readBoolean() ? readInternalEntry(buf) : null;
        com.etema.ragnarmmo.common.api.mobs.runtime.ComputedMobProfile effectiveProfile = buf.readBoolean() ? readProfile(buf) : null;
        
        return new com.etema.ragnarmmo.common.api.mobs.runtime.manual.ManualMobDetail(
                internalEntry,
                effectiveProfile,
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean());
    }
}
