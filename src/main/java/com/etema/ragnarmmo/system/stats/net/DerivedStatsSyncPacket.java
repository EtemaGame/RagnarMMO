package com.etema.ragnarmmo.system.stats.net;

import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Syncs derived/computed stats from server to client.
 * These stats are calculated server-side and sent to the client
 * so the UI can display accurate values without local recalculation.
 */
public class DerivedStatsSyncPacket {
    public final double physicalAttack;
    public final double physicalAttackMin;
    public final double physicalAttackMax;
    public final double magicAttack;
    public final double magicAttackMin;
    public final double magicAttackMax;
    public final double accuracy;
    public final double criticalChance;
    public final double criticalDamageMultiplier;
    public final double flee;
    public final double perfectDodge;
    public final double attackSpeed;
    public final double castTime;
    public final double globalCooldown;
    public final double physicalDamageReduction;
    public final double magicDamageReduction;
    public final double maxHealth;
    public final double healthRegenPerSecond;
    public final double maxMana;
    public final double manaRegenPerSecond;
    public final double defense;
    public final double magicDefense;
    public final double hardDefense;
    public final double softDefense;
    public final double hardMagicDefense;
    public final double softMagicDefense;

    public DerivedStatsSyncPacket(DerivedStats d) {
        this.physicalAttack = d.physicalAttack;
        this.physicalAttackMin = d.physicalAttackMin;
        this.physicalAttackMax = d.physicalAttackMax;
        this.magicAttack = d.magicAttack;
        this.magicAttackMin = d.magicAttackMin;
        this.magicAttackMax = d.magicAttackMax;
        this.accuracy = d.accuracy;
        this.criticalChance = d.criticalChance;
        this.criticalDamageMultiplier = d.criticalDamageMultiplier;
        this.flee = d.flee;
        this.perfectDodge = d.perfectDodge;
        this.attackSpeed = d.attackSpeed;
        this.castTime = d.castTime;
        this.globalCooldown = d.globalCooldown;
        this.physicalDamageReduction = d.physicalDamageReduction;
        this.magicDamageReduction = d.magicDamageReduction;
        this.maxHealth = d.maxHealth;
        this.healthRegenPerSecond = d.healthRegenPerSecond;
        this.maxMana = d.maxMana;
        this.manaRegenPerSecond = d.manaRegenPerSecond;
        this.defense = d.defense;
        this.magicDefense = d.magicDefense;
        this.hardDefense = d.hardDefense;
        this.softDefense = d.softDefense;
        this.hardMagicDefense = d.hardMagicDefense;
        this.softMagicDefense = d.softMagicDefense;
    }

    public DerivedStatsSyncPacket(
            double physicalAttack, double physicalAttackMin, double physicalAttackMax,
            double magicAttack, double magicAttackMin, double magicAttackMax,
            double accuracy, double criticalChance,
            double criticalDamageMultiplier, double flee, double perfectDodge, double attackSpeed,
            double castTime, double globalCooldown, double physicalDamageReduction,
            double magicDamageReduction, double maxHealth, double healthRegenPerSecond,
            double maxMana, double manaRegenPerSecond, double defense, double magicDefense,
            double hardDefense, double softDefense, double hardMagicDefense, double softMagicDefense) {
        this.physicalAttack = physicalAttack;
        this.physicalAttackMin = physicalAttackMin;
        this.physicalAttackMax = physicalAttackMax;
        this.magicAttack = magicAttack;
        this.magicAttackMin = magicAttackMin;
        this.magicAttackMax = magicAttackMax;
        this.accuracy = accuracy;
        this.criticalChance = criticalChance;
        this.criticalDamageMultiplier = criticalDamageMultiplier;
        this.flee = flee;
        this.perfectDodge = perfectDodge;
        this.attackSpeed = attackSpeed;
        this.castTime = castTime;
        this.globalCooldown = globalCooldown;
        this.physicalDamageReduction = physicalDamageReduction;
        this.magicDamageReduction = magicDamageReduction;
        this.maxHealth = maxHealth;
        this.healthRegenPerSecond = healthRegenPerSecond;
        this.maxMana = maxMana;
        this.manaRegenPerSecond = manaRegenPerSecond;
        this.defense = defense;
        this.magicDefense = magicDefense;
        this.hardDefense = hardDefense;
        this.softDefense = softDefense;
        this.hardMagicDefense = hardMagicDefense;
        this.softMagicDefense = softMagicDefense;
    }

    public static void encode(DerivedStatsSyncPacket m, FriendlyByteBuf buf) {
        buf.writeDouble(m.physicalAttack);
        buf.writeDouble(m.physicalAttackMin);
        buf.writeDouble(m.physicalAttackMax);
        buf.writeDouble(m.magicAttack);
        buf.writeDouble(m.magicAttackMin);
        buf.writeDouble(m.magicAttackMax);
        buf.writeDouble(m.accuracy);
        buf.writeDouble(m.criticalChance);
        buf.writeDouble(m.criticalDamageMultiplier);
        buf.writeDouble(m.flee);
        buf.writeDouble(m.perfectDodge);
        buf.writeDouble(m.attackSpeed);
        buf.writeDouble(m.castTime);
        buf.writeDouble(m.globalCooldown);
        buf.writeDouble(m.physicalDamageReduction);
        buf.writeDouble(m.magicDamageReduction);
        buf.writeDouble(m.maxHealth);
        buf.writeDouble(m.healthRegenPerSecond);
        buf.writeDouble(m.maxMana);
        buf.writeDouble(m.manaRegenPerSecond);
        buf.writeDouble(m.defense);
        buf.writeDouble(m.magicDefense);
        buf.writeDouble(m.hardDefense);
        buf.writeDouble(m.softDefense);
        buf.writeDouble(m.hardMagicDefense);
        buf.writeDouble(m.softMagicDefense);
    }

    public static DerivedStatsSyncPacket decode(FriendlyByteBuf buf) {
        return new DerivedStatsSyncPacket(
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(),
                buf.readDouble());
    }

    public static void handle(DerivedStatsSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleDerivedStatsSync(msg)));
        ctx.setPacketHandled(true);
    }

    /**
     * Creates a DerivedStats object from this packet's data.
     */
    public DerivedStats toDerivedStats() {
        DerivedStats d = new DerivedStats();
        d.physicalAttack = this.physicalAttack;
        d.physicalAttackMin = this.physicalAttackMin;
        d.physicalAttackMax = this.physicalAttackMax;
        d.magicAttack = this.magicAttack;
        d.magicAttackMin = this.magicAttackMin;
        d.magicAttackMax = this.magicAttackMax;
        d.accuracy = this.accuracy;
        d.criticalChance = this.criticalChance;
        d.criticalDamageMultiplier = this.criticalDamageMultiplier;
        d.flee = this.flee;
        d.perfectDodge = this.perfectDodge;
        d.attackSpeed = this.attackSpeed;
        d.castTime = this.castTime;
        d.globalCooldown = this.globalCooldown;
        d.physicalDamageReduction = this.physicalDamageReduction;
        d.magicDamageReduction = this.magicDamageReduction;
        d.maxHealth = this.maxHealth;
        d.healthRegenPerSecond = this.healthRegenPerSecond;
        d.maxMana = this.maxMana;
        d.manaRegenPerSecond = this.manaRegenPerSecond;
        d.defense = this.defense;
        d.magicDefense = this.magicDefense;
        d.hardDefense = this.hardDefense;
        d.softDefense = this.softDefense;
        d.hardMagicDefense = this.hardMagicDefense;
        d.softMagicDefense = this.softMagicDefense;
        return d;
    }
}
