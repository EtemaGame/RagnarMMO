package com.etema.ragnarmmo.player.stats.network;

import java.util.function.Supplier;

import com.etema.ragnarmmo.common.api.compute.DerivedStats;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Syncs authoritative derived stats from server to client.
 */
public class DerivedStatsSyncPacket {
    private final DerivedStats stats;

    public DerivedStatsSyncPacket(DerivedStats stats) {
        this.stats = copy(stats);
    }

    public static void encode(DerivedStatsSyncPacket message, FriendlyByteBuf buf) {
        DerivedStats d = message.stats;
        buf.writeDouble(d.physicalAttack);
        buf.writeDouble(d.physicalAttackMin);
        buf.writeDouble(d.physicalAttackMax);
        buf.writeDouble(d.magicAttack);
        buf.writeDouble(d.magicAttackMin);
        buf.writeDouble(d.magicAttackMax);
        buf.writeDouble(d.accuracy);
        buf.writeDouble(d.criticalChance);
        buf.writeDouble(d.criticalDamageMultiplier);
        buf.writeDouble(d.flee);
        buf.writeDouble(d.perfectDodge);
        buf.writeDouble(d.attackSpeed);
        buf.writeDouble(d.castTime);
        buf.writeDouble(d.globalCooldown);
        buf.writeDouble(d.physicalDamageReduction);
        buf.writeDouble(d.magicDamageReduction);
        buf.writeDouble(d.maxHealth);
        buf.writeDouble(d.healthRegenPerSecond);
        buf.writeDouble(d.maxMana);
        buf.writeDouble(d.manaRegenPerSecond);
        buf.writeDouble(d.maxSP);
        buf.writeDouble(d.spRegenPerSecond);
        buf.writeDouble(d.projectileVelocityMult);
        buf.writeDouble(d.projectileGravityMult);
        buf.writeDouble(d.projectileSpreadMult);
        buf.writeDouble(d.castInterruptResist);
        buf.writeDouble(d.skillStatusPower);
        buf.writeDouble(d.threatGenerationBonus);
        buf.writeDouble(d.revealRadiusBonus);
        buf.writeDouble(d.defense);
        buf.writeDouble(d.magicDefense);
        buf.writeDouble(d.hardDefense);
        buf.writeDouble(d.softDefense);
        buf.writeDouble(d.hardMagicDefense);
        buf.writeDouble(d.softMagicDefense);
        buf.writeDouble(d.lifeSteal);
        buf.writeDouble(d.armorPierce);
        buf.writeDouble(d.armorShred);
        buf.writeDouble(d.overheal);
    }

    public static DerivedStatsSyncPacket decode(FriendlyByteBuf buf) {
        DerivedStats d = new DerivedStats();
        d.physicalAttack = buf.readDouble();
        d.physicalAttackMin = buf.readDouble();
        d.physicalAttackMax = buf.readDouble();
        d.magicAttack = buf.readDouble();
        d.magicAttackMin = buf.readDouble();
        d.magicAttackMax = buf.readDouble();
        d.accuracy = buf.readDouble();
        d.criticalChance = buf.readDouble();
        d.criticalDamageMultiplier = buf.readDouble();
        d.flee = buf.readDouble();
        d.perfectDodge = buf.readDouble();
        d.attackSpeed = buf.readDouble();
        d.castTime = buf.readDouble();
        d.globalCooldown = buf.readDouble();
        d.physicalDamageReduction = buf.readDouble();
        d.magicDamageReduction = buf.readDouble();
        d.maxHealth = buf.readDouble();
        d.healthRegenPerSecond = buf.readDouble();
        d.maxMana = buf.readDouble();
        d.manaRegenPerSecond = buf.readDouble();
        d.maxSP = buf.readDouble();
        d.spRegenPerSecond = buf.readDouble();
        d.projectileVelocityMult = buf.readDouble();
        d.projectileGravityMult = buf.readDouble();
        d.projectileSpreadMult = buf.readDouble();
        d.castInterruptResist = buf.readDouble();
        d.skillStatusPower = buf.readDouble();
        d.threatGenerationBonus = buf.readDouble();
        d.revealRadiusBonus = buf.readDouble();
        d.defense = buf.readDouble();
        d.magicDefense = buf.readDouble();
        d.hardDefense = buf.readDouble();
        d.softDefense = buf.readDouble();
        d.hardMagicDefense = buf.readDouble();
        d.softMagicDefense = buf.readDouble();
        d.lifeSteal = buf.readDouble();
        d.armorPierce = buf.readDouble();
        d.armorShred = buf.readDouble();
        d.overheal = buf.readDouble();
        return new DerivedStatsSyncPacket(d);
    }

    public static void handle(DerivedStatsSyncPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.etema.ragnarmmo.client.ClientPacketHandler.handleDerivedStatsSync(msg)));
        ctx.setPacketHandled(true);
    }

    public DerivedStats toDerivedStats() {
        return copy(stats);
    }

    private static DerivedStats copy(DerivedStats source) {
        DerivedStats d = new DerivedStats();
        if (source == null) {
            return d;
        }

        d.physicalAttack = source.physicalAttack;
        d.physicalAttackMin = source.physicalAttackMin;
        d.physicalAttackMax = source.physicalAttackMax;
        d.magicAttack = source.magicAttack;
        d.magicAttackMin = source.magicAttackMin;
        d.magicAttackMax = source.magicAttackMax;
        d.accuracy = source.accuracy;
        d.criticalChance = source.criticalChance;
        d.criticalDamageMultiplier = source.criticalDamageMultiplier;
        d.flee = source.flee;
        d.perfectDodge = source.perfectDodge;
        d.attackSpeed = source.attackSpeed;
        d.castTime = source.castTime;
        d.globalCooldown = source.globalCooldown;
        d.physicalDamageReduction = source.physicalDamageReduction;
        d.magicDamageReduction = source.magicDamageReduction;
        d.maxHealth = source.maxHealth;
        d.healthRegenPerSecond = source.healthRegenPerSecond;
        d.maxMana = source.maxMana;
        d.manaRegenPerSecond = source.manaRegenPerSecond;
        d.maxSP = source.maxSP;
        d.spRegenPerSecond = source.spRegenPerSecond;
        d.projectileVelocityMult = source.projectileVelocityMult;
        d.projectileGravityMult = source.projectileGravityMult;
        d.projectileSpreadMult = source.projectileSpreadMult;
        d.castInterruptResist = source.castInterruptResist;
        d.skillStatusPower = source.skillStatusPower;
        d.threatGenerationBonus = source.threatGenerationBonus;
        d.revealRadiusBonus = source.revealRadiusBonus;
        d.defense = source.defense;
        d.magicDefense = source.magicDefense;
        d.hardDefense = source.hardDefense;
        d.softDefense = source.softDefense;
        d.hardMagicDefense = source.hardMagicDefense;
        d.softMagicDefense = source.softMagicDefense;
        d.lifeSteal = source.lifeSteal;
        d.armorPierce = source.armorPierce;
        d.armorShred = source.armorShred;
        d.overheal = source.overheal;
        return d;
    }
}
