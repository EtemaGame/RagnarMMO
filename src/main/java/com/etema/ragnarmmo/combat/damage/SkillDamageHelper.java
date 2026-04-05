package com.etema.ragnarmmo.combat.damage;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.roitems.runtime.WeaponStatHelper;
import com.etema.ragnarmmo.system.stats.capability.PlayerStats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Central utility for applying skill damage.
 * Refactored to live in the combat domain.
 */
public final class SkillDamageHelper {

    private SkillDamageHelper() {}

    public static boolean dealSkillDamage(LivingEntity target, DamageSource source, float amount) {
        if (!target.isAlive() || amount <= 0)
            return false;

        int savedHurtTime = target.hurtTime;
        int savedHurtDuration = target.hurtDuration;
        int savedInvulnerableTime = target.invulnerableTime;

        target.hurtTime = 0;
        target.hurtDuration = 0;
        target.invulnerableTime = 0;

        boolean hit = target.hurt(source, amount);

        if (target.isAlive()) {
            target.hurtTime = Math.max(target.hurtTime, savedHurtTime);
            target.hurtDuration = Math.max(target.hurtDuration, savedHurtDuration);

            // Skill multihits should not create a fresh 20-tick vanilla cooldown when
            // the target was otherwise hittable. We only preserve a pre-existing
            // invulnerability window if one already existed before this hit.
            if (savedInvulnerableTime <= 0) {
                target.invulnerableTime = 0;
            }
        }

        return hit;
    }

    public static float getPhysicalATK(LivingEntity entity) {
        if (entity instanceof Player player) {
            int str = getSTR(player);
            float weaponDmg = getWeaponBaseDamage(player);
            float strBonus = str / 2.0f + (str * str) / 200.0f;
            return weaponDmg + strBonus;
        }
        var attr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return attr != null ? (float) attr.getValue() : 2.0f;
    }

    public static float scaleByATK(LivingEntity entity, float percentageAtk) {
        return getPhysicalATK(entity) * percentageAtk / 100.0f;
    }

    public static float getMATK(LivingEntity entity) {
        if (entity instanceof Player player) {
            int intStat = getINT(player);
            float weaponMatk = (float) WeaponStatHelper.getDisplayedMagicAttack(player.getMainHandItem());
            return intStat + (intStat * intStat) / 100.0f + weaponMatk;
        }
        var attr = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        return attr != null ? (float) attr.getValue() : 2.0f;
    }

    public static float scaleByMATK(LivingEntity entity, float percentageMAtk) {
        return getMATK(entity) * percentageMAtk / 100.0f;
    }

    public static int getSTR(Player player) {
        return RagnarCoreAPI.get(player)
                .map(s -> s instanceof PlayerStats ps ? ps.getSTR() : 1)
                .orElse(1);
    }

    public static int getDEX(Player player) {
        return RagnarCoreAPI.get(player)
                .map(s -> s instanceof PlayerStats ps ? ps.getDEX() : 1)
                .orElse(1);
    }

    public static int getINT(Player player) {
        return RagnarCoreAPI.get(player)
                .map(s -> s instanceof PlayerStats ps ? ps.getINT() : 1)
                .orElse(1);
    }

    public static float getWeaponBaseDamage(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty())
            return 1.0f;

        var mods = mainHand.getAttributeModifiers(net.minecraft.world.entity.EquipmentSlot.MAINHAND);
        var dmgMods = mods.get(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (dmgMods.isEmpty())
            return 1.0f;

        double total = 0;
        for (var mod : dmgMods) {
            if (mod.getOperation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION) {
                total += mod.getAmount();
            }
        }
        return total > 0 ? (float) total : 1.0f;
    }

    public static int getBaseLevel(Player player) {
        return RagnarCoreAPI.get(player)
                .map(s -> s instanceof PlayerStats ps ? ps.getLevel() : 1)
                .orElse(1);
    }

    public static float getHealAmount(LivingEntity entity, int skillLevel) {
        if (entity instanceof Player player) {
            int baseLv = getBaseLevel(player);
            int intStat = getINT(player);
            double healMultiplier = Math.max(1.0, Math.floor((baseLv + intStat) / 8.0));
            return (float) (healMultiplier * (4 + 8 * skillLevel));
        }
        return 4.0f + (skillLevel * 4.0f);
    }

    public static final float MIN_ATK = 2.0f;
}
