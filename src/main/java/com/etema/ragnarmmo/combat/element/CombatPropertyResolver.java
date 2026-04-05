package com.etema.ragnarmmo.combat.element;

import com.etema.ragnarmmo.common.tags.RagnarTags;
import com.etema.ragnarmmo.entity.aoe.FireWallAoe;
import com.etema.ragnarmmo.entity.aoe.HeavensDriveAoe;
import com.etema.ragnarmmo.entity.aoe.NapalmBeatAoe;
import com.etema.ragnarmmo.entity.aoe.SanctuaryAoe;
import com.etema.ragnarmmo.entity.aoe.StormGustAoe;
import com.etema.ragnarmmo.entity.projectile.FireBallProjectile;
import com.etema.ragnarmmo.entity.projectile.FireBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.IceBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.LightningBoltProjectile;
import com.etema.ragnarmmo.entity.projectile.SoulStrikeProjectile;
import com.etema.ragnarmmo.system.stats.compute.CombatMath;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class CombatPropertyResolver {

    public static final String TEMP_WEAPON_ELEMENT_TAG = "ragnarmmo_weapon_element";
    public static final String TEMP_WEAPON_ELEMENT_UNTIL_TAG = "ragnarmmo_weapon_element_until";
    public static final String TEMP_ARMOR_ELEMENT_TAG = "ragnarmmo_armor_element";
    public static final String TEMP_ARMOR_ELEMENT_UNTIL_TAG = "ragnarmmo_armor_element_until";
    public static final String WEAPON_PERFECTION_UNTIL_TAG = "ragnarmmo_weapon_perfection_until";

    private CombatPropertyResolver() {
    }

    public static void applyTemporaryWeaponElement(LivingEntity entity, ElementType element, long untilTick) {
        CompoundTag data = entity.getPersistentData();
        data.putString(TEMP_WEAPON_ELEMENT_TAG, element.name());
        data.putLong(TEMP_WEAPON_ELEMENT_UNTIL_TAG, untilTick);
    }

    public static void applyWeaponPerfection(LivingEntity entity, long untilTick) {
        entity.getPersistentData().putLong(WEAPON_PERFECTION_UNTIL_TAG, untilTick);
    }

    public static void applyTemporaryArmorElement(LivingEntity entity, ElementType element, long untilTick) {
        CompoundTag data = entity.getPersistentData();
        data.putString(TEMP_ARMOR_ELEMENT_TAG, element.name());
        data.putLong(TEMP_ARMOR_ELEMENT_UNTIL_TAG, untilTick);
    }

    public static ElementType getOffensiveElement(Player player) {
        ElementType temporary = getTemporaryWeaponElement(player);
        if (temporary != null) {
            return temporary;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(RagnarTags.Items.ELEMENT_WATER)) return ElementType.WATER;
        if (mainHand.is(RagnarTags.Items.ELEMENT_EARTH)) return ElementType.EARTH;
        if (mainHand.is(RagnarTags.Items.ELEMENT_FIRE)) return ElementType.FIRE;
        if (mainHand.is(RagnarTags.Items.ELEMENT_WIND)) return ElementType.WIND;
        if (mainHand.is(RagnarTags.Items.ELEMENT_POISON)) return ElementType.POISON;
        if (mainHand.is(RagnarTags.Items.ELEMENT_HOLY)) return ElementType.HOLY;
        if (mainHand.is(RagnarTags.Items.ELEMENT_DARK)) return ElementType.DARK;
        if (mainHand.is(RagnarTags.Items.ELEMENT_GHOST)) return ElementType.GHOST;
        if (mainHand.is(RagnarTags.Items.ELEMENT_NEUTRAL)) return ElementType.NEUTRAL;
        return ElementType.NEUTRAL;
    }

    public static boolean hasWeaponPerfection(Player player) {
        CompoundTag data = player.getPersistentData();
        long until = data.getLong(WEAPON_PERFECTION_UNTIL_TAG);
        if (until <= 0) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            data.remove(WEAPON_PERFECTION_UNTIL_TAG);
            return false;
        }
        return true;
    }

    public static ElementType getDefensiveElement(LivingEntity entity) {
        ElementType temporary = getTemporaryArmorElement(entity);
        if (temporary != null) {
            return temporary;
        }

        var type = entity.getType();
        if (type.is(RagnarTags.Entities.ELEMENT_UNDEAD)) return ElementType.UNDEAD;
        if (type.is(RagnarTags.Entities.ELEMENT_GHOST)) return ElementType.GHOST;
        if (type.is(RagnarTags.Entities.ELEMENT_DARK)) return ElementType.DARK;
        if (type.is(RagnarTags.Entities.ELEMENT_SCARLET)) return ElementType.HOLY;
        if (type.is(RagnarTags.Entities.ELEMENT_POISON)) return ElementType.POISON;
        if (type.is(RagnarTags.Entities.ELEMENT_WIND)) return ElementType.WIND;
        if (type.is(RagnarTags.Entities.ELEMENT_FIRE)) return ElementType.FIRE;
        if (type.is(RagnarTags.Entities.ELEMENT_EARTH)) return ElementType.EARTH;
        if (type.is(RagnarTags.Entities.ELEMENT_WATER)) return ElementType.WATER;
        if (type.is(RagnarTags.Entities.ELEMENT_NEUTRAL)) return ElementType.NEUTRAL;
        return ElementType.NEUTRAL;
    }

    public static String getRaceId(LivingEntity entity) {
        var type = entity.getType();
        if (type.is(RagnarTags.Entities.RACE_UNDEAD)) return "undead";
        if (type.is(RagnarTags.Entities.RACE_DEMON)) return "demon";
        if (type.is(RagnarTags.Entities.RACE_INSECT)) return "insect";
        if (type.is(RagnarTags.Entities.RACE_FISH)) return "fish";
        if (type.is(RagnarTags.Entities.RACE_BRUTE)) return "brute";
        if (type.is(RagnarTags.Entities.RACE_DRAGON)) return "dragon";
        if (type.is(RagnarTags.Entities.RACE_FORMLESS)) return "formless";
        if (type.is(RagnarTags.Entities.RACE_DEMIHUMAN)) return "demihuman";
        if (type.is(RagnarTags.Entities.RACE_ANGEL)) return "angel";
        if (type.is(RagnarTags.Entities.RACE_PLANT)) return "plant";
        return "";
    }

    public static CombatMath.MobSize getEntitySize(LivingEntity entity) {
        if (entity instanceof EnderDragon
                || entity instanceof WitherBoss
                || entity instanceof ElderGuardian
                || entity instanceof Ravager) {
            return CombatMath.MobSize.LARGE;
        }

        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float playerReference = 1.8f;
        float maxDimension = Math.max(width, height);
        if (maxDimension < playerReference - 1.0f) return CombatMath.MobSize.SMALL;
        if (maxDimension > playerReference + 3.0f) return CombatMath.MobSize.LARGE;
        return CombatMath.MobSize.MEDIUM;
    }

    public static String getSizeId(LivingEntity entity) {
        return switch (getEntitySize(entity)) {
            case SMALL -> "small";
            case MEDIUM -> "medium";
            case LARGE -> "large";
        };
    }

    public static ElementType getMagicElement(Entity directEntity) {
        if (directEntity == null) return ElementType.NEUTRAL;
        if (directEntity instanceof FireBoltProjectile
                || directEntity instanceof FireBallProjectile
                || directEntity instanceof FireWallAoe) return ElementType.FIRE;
        if (directEntity instanceof IceBoltProjectile
                || directEntity instanceof StormGustAoe) return ElementType.WATER;
        if (directEntity instanceof LightningBoltProjectile) return ElementType.WIND;
        if (directEntity instanceof HeavensDriveAoe) return ElementType.EARTH;
        if (directEntity instanceof SoulStrikeProjectile
                || directEntity instanceof NapalmBeatAoe) return ElementType.GHOST;
        if (directEntity instanceof SanctuaryAoe) return ElementType.HOLY;
        return ElementType.NEUTRAL;
    }

    public static String getElementId(ElementType element) {
        return element.name().toLowerCase(Locale.ROOT);
    }

    public static double getElementalModifier(ElementType attack, ElementType defense) {
        return switch (attack) {
            case NEUTRAL -> switch (defense) {
                case GHOST -> 0.25;
                default -> 1.0;
            };
            case WATER -> switch (defense) {
                case WATER -> 0.25;
                case FIRE -> 0.5;
                case WIND -> 1.75;
                default -> 1.0;
            };
            case EARTH -> switch (defense) {
                case FIRE -> 1.5;
                case WIND -> 0.5;
                case POISON -> 1.25;
                default -> 1.0;
            };
            case FIRE -> switch (defense) {
                case WATER -> 1.5;
                case EARTH -> 0.5;
                case FIRE -> 0.25;
                case POISON -> 1.25;
                default -> 1.0;
            };
            case WIND -> switch (defense) {
                case WATER -> 0.5;
                case EARTH -> 1.5;
                case WIND -> 0.25;
                case POISON -> 1.25;
                default -> 1.0;
            };
            case POISON -> switch (defense) {
                case POISON -> 0.0;
                case DARK, UNDEAD -> 0.5;
                default -> 1.0;
            };
            case HOLY -> switch (defense) {
                case WATER, EARTH, FIRE, WIND, POISON, GHOST -> 0.75;
                case HOLY -> 0.0;
                case DARK -> 1.25;
                default -> 1.0;
            };
            case DARK -> switch (defense) {
                case POISON -> 0.5;
                case HOLY -> 1.25;
                case DARK, UNDEAD -> 0.0;
                case GHOST -> 0.75;
                default -> 1.0;
            };
            case GHOST -> switch (defense) {
                case NEUTRAL -> 0.25;
                case GHOST -> 1.25;
                default -> 1.0;
            };
            case UNDEAD -> switch (defense) {
                case FIRE -> 1.25;
                case POISON, DARK -> -0.25;
                case HOLY -> 1.5;
                case UNDEAD -> 0.0;
                default -> 1.0;
            };
        };
    }

    private static ElementType getTemporaryWeaponElement(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        String raw = readExpiringString(data, entity, TEMP_WEAPON_ELEMENT_TAG, TEMP_WEAPON_ELEMENT_UNTIL_TAG);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return ElementType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            data.remove(TEMP_WEAPON_ELEMENT_TAG);
            data.remove(TEMP_WEAPON_ELEMENT_UNTIL_TAG);
            return null;
        }
    }

    private static ElementType getTemporaryArmorElement(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        String raw = readExpiringString(data, entity, TEMP_ARMOR_ELEMENT_TAG, TEMP_ARMOR_ELEMENT_UNTIL_TAG);
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return ElementType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            data.remove(TEMP_ARMOR_ELEMENT_TAG);
            data.remove(TEMP_ARMOR_ELEMENT_UNTIL_TAG);
            return null;
        }
    }

    private static String readExpiringString(CompoundTag data, LivingEntity entity, String valueTag, String untilTag) {
        long until = data.getLong(untilTag);
        if (until <= 0) {
            return null;
        }
        if (entity.level().getGameTime() >= until) {
            data.remove(valueTag);
            data.remove(untilTag);
            return null;
        }

        String raw = data.getString(valueTag);
        if (raw == null || raw.isBlank()) {
            data.remove(valueTag);
            data.remove(untilTag);
            return null;
        }
        return raw;
    }
}
