package com.etema.ragnarmmo.system.mobstats.events;

import com.etema.ragnarmmo.common.api.mobs.MobTier;
import com.etema.ragnarmmo.system.mobstats.config.MobConfig;
import com.etema.ragnarmmo.system.mobstats.config.SpeciesConfig;
import com.etema.ragnarmmo.system.mobstats.core.MobStatDistributor;
import com.etema.ragnarmmo.system.mobstats.core.MobStats;
import com.etema.ragnarmmo.common.api.stats.StatKeys;
import com.etema.ragnarmmo.system.mobstats.core.capability.MobStatsProvider;
import com.etema.ragnarmmo.system.mobstats.level.MobLevelManager;
import com.etema.ragnarmmo.system.mobstats.mobs.MobClass;
import com.etema.ragnarmmo.common.net.Network;
import com.etema.ragnarmmo.system.mobstats.network.SyncMobStatsPacket;
import com.etema.ragnarmmo.system.mobstats.util.ConfigUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MobSpawnHandler {

    private final Random rng = new Random();
    private final MobLevelManager levelMgr = new MobLevelManager(rng);
    private final MobStatDistributor distributor = new MobStatDistributor(rng);

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void onMobJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;
        if (!(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player)
            return;

        MobStatsProvider.get(mob).ifPresent(stats -> {
            if (stats.isInitialized())
                return;

            MobCategory cat = mob.getType().getCategory();
            // Removed exclusion for CREATURE, AMBIENT, WATER_CREATURE to handle them as Lv
            // 1

            ResourceLocation mobId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (mobId != null && MobConfig.MOB_EXCLUDE_LIST.get().contains(mobId.toString()))
                return;

            SpeciesConfig.SpeciesSettings species = mobId != null ? SpeciesConfig.get(mobId)
                    : SpeciesConfig.SpeciesSettings.EMPTY;

            boolean isPassive = cat == MobCategory.CREATURE || cat == MobCategory.AMBIENT
                    || cat == MobCategory.WATER_CREATURE;
            generateStats(mob, stats, species, isPassive);
            stats.setInitialized(true);

            applyAttributes(mob, stats);

            // Sincroniza con clientes que ven a este mob (y el propio invocador)
            Network.sendTrackingEntityAndSelf(mob, new SyncMobStatsPacket(mob.getId(), stats));
        });
    }

    private void generateStats(LivingEntity mob, MobStats stats, SpeciesConfig.SpeciesSettings species,
            boolean isPassive) {
        if (isPassive) {
            stats.setTier(MobTier.NORMAL);
            stats.setLevel(1);
            stats.setMobClass(null); // Passives don't have classes

            // Basic multipliers for passives (can be tweaked if needed, but 1.0 is
            // standard)
            stats.setHealthMultiplier(1.0);
            stats.setDamageMultiplier(1.0);
            stats.setDefenseMultiplier(1.0);
            stats.setSpeedMultiplier(1.0);

            // No attribute distribution for passives, they stay vanilla-ish but tracked
            return;
        }

        MobTier tier = determineTier(species);
        stats.setTier(tier);

        int level = levelMgr.computeLevel(mob, species, tier);
        stats.setLevel(level);

        int ppl = species.pointsPerLevel().orElse(MobConfig.pointsPerLevel(tier));
        int base = MobConfig.basePoints(tier);
        int total = Math.max(0, base + level * ppl);

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        Map<String, String> e2c = ConfigUtils.parseKeyEqualsValue(MobConfig.ENTITY_TO_CLASS.get());
        MobClass mobClass = null;

        if (MobConfig.ENABLE_CLASS_SYSTEM.get()) {
            if (species.statWeights().isEmpty() || species.randomDistribution()) {
                String clsName = e2c.getOrDefault(id.toString(), null);
                if (clsName != null) {
                    try {
                        mobClass = MobClass.valueOf(clsName.toUpperCase(Locale.ROOT));
                    } catch (Exception ignore) {
                    }
                }
            }

            // SMART CLASS VARIATION (Fallback)
            if (mobClass == null && tier != MobTier.NORMAL) {
                // Elites and Bosses without a fixed class get a logical one or random
                mobClass = determineSmartClass(mob);
            }
        }

        stats.setMobClass(mobClass);

        if (mobClass != null) {
            Map<String, Double> growthOverride = ConfigUtils
                    .parseClassVector(MobConfig.CLASS_GROWTH_OVERRIDES.get())
                    .getOrDefault(mobClass.name(), Map.of());
            distributor.distributeWithWeights(stats, total, mobClass.growthWeights(), growthOverride);
        } else {
            distributor.distribute(stats, total, species);
        }

        double hm = MobConfig.healthMultiplier(tier) * species.healthMultiplier().orElse(1.0);
        double dm = MobConfig.damageMultiplier(tier) * species.damageMultiplier().orElse(1.0);
        double df = MobConfig.defenseMultiplier(tier) * species.defenseMultiplier().orElse(1.0);
        double sp = species.speedMultiplier().orElse(1.0);

        // PARTY SCALING
        if (tier != MobTier.NORMAL) {
            double radius = MobConfig.PARTY_SCALING_RADIUS.get();
            long playerCount = mob.level().players().stream()
                    .filter(p -> p.distanceToSqr(mob) < radius * radius)
                    .count();

            if (playerCount > 1) {
                double extraPlayers = playerCount - 1;
                hm *= (1.0 + extraPlayers * MobConfig.PARTY_HP_MULTIPLIER.get());
                dm *= (1.0 + extraPlayers * MobConfig.PARTY_ATK_MULTIPLIER.get());
            }
        }

        stats.setHealthMultiplier(hm);
        stats.setDamageMultiplier(dm);
        stats.setDefenseMultiplier(df);
        stats.setSpeedMultiplier(sp);

        if (mobClass != null
                && species.healthMultiplier().isEmpty()
                && species.damageMultiplier().isEmpty()
                && species.defenseMultiplier().isEmpty()
                && species.speedMultiplier().isEmpty()) {

            var mult = mobClass.multipliers();
            Map<String, Double> multOverride = ConfigUtils
                    .parseClassVector(MobConfig.CLASS_MULT_OVERRIDES.get())
                    .getOrDefault(mobClass.name(), Map.of());

            double ohm = multOverride.getOrDefault("health", mult.getOrDefault(StatKeys.VIT, 1.0));
            double odm = multOverride.getOrDefault("damage", mult.getOrDefault(StatKeys.STR, 1.0));
            double odf = multOverride.getOrDefault("defense", mult.getOrDefault(StatKeys.VIT, 1.0));
            double osp = multOverride.getOrDefault("speed", mult.getOrDefault(StatKeys.AGI, 1.0));

            stats.setHealthMultiplier(ohm * stats.getHealthMultiplier());
            stats.setDamageMultiplier(odm * stats.getDamageMultiplier());
            stats.setDefenseMultiplier(odf * stats.getDefenseMultiplier());
            stats.setSpeedMultiplier(osp * stats.getSpeedMultiplier());
        }
    }

    private MobTier determineTier(SpeciesConfig.SpeciesSettings species) {
        if (species != null && species.forcedTier().isPresent())
            return species.forcedTier().get();
        double roll = rng.nextDouble();
        if (roll < MobConfig.BOSS_CHANCE.get())
            return MobTier.BOSS;
        if (roll < MobConfig.BOSS_CHANCE.get() + MobConfig.ELITE_CHANCE.get())
            return MobTier.ELITE;
        return MobTier.NORMAL;
    }

    private MobClass determineSmartClass(LivingEntity mob) {
        // 1. Logic based on entity properties
        if (mob instanceof net.minecraft.world.entity.monster.RangedAttackMob)
            return MobClass.ARCHER;
        if (mob instanceof net.minecraft.world.entity.monster.Enemy
                && (mob instanceof net.minecraft.world.entity.FlyingMob
                        || mob instanceof net.minecraft.world.entity.monster.Blaze))
            return MobClass.MAGE;
        if (mob.getBbWidth() < 0.8f && mob.getBbHeight() < 1.0f)
            return MobClass.ROGUE;
        if (mob.isInvertedHealAndHarm())
            return MobClass.UNDEAD;

        // 2. Random weighted distribution for variety
        double roll = rng.nextDouble();
        if (roll < 0.4)
            return MobClass.WARRIOR;
        if (roll < 0.6)
            return MobClass.BEAST;
        if (roll < 0.7)
            return MobClass.ARCHER;
        if (roll < 0.8)
            return MobClass.ROGUE;
        if (roll < 0.9)
            return MobClass.MAGE;
        return MobClass.UNDEAD;
    }

    private void applyAttributes(LivingEntity mob, MobStats stats) {
        int level = stats.getLevel();
        int str = stats.get(StatKeys.STR);
        int agi = stats.get(StatKeys.AGI);
        int vit = stats.get(StatKeys.VIT);
        int intel = stats.get(StatKeys.INT);
        int dex = stats.get(StatKeys.DEX);
        int luk = stats.get(StatKeys.LUK);

        AttributeInstance maxHealth = get(mob, Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double hp = (MobConfig.HP_BASE.get()
                    + vit * MobConfig.VIT_TO_HP.get()
                    + level * MobConfig.LEVEL_TO_HP.get()) * stats.getHealthMultiplier();
            hp = Math.max(1.0D, hp);
            maxHealth.setBaseValue(hp);
            mob.setHealth((float) hp);
        }

        AttributeInstance attackDamage = get(mob, Attributes.ATTACK_DAMAGE);
        if (attackDamage != null) {
            double atk = (MobConfig.ATK_BASE.get()
                    + str * MobConfig.STR_TO_ATK.get()
                    + dex * MobConfig.DEX_TO_ATK.get()
                    + luk * MobConfig.LUK_TO_ATK.get()) * stats.getDamageMultiplier();
            attackDamage.setBaseValue(Math.max(0.0D, atk));
        }

        AttributeInstance armor = get(mob, Attributes.ARMOR);
        if (armor != null) {
            double armorValue = (MobConfig.ARMOR_BASE.get()
                    + vit * MobConfig.VIT_TO_ARMOR.get()
                    + intel * MobConfig.INT_TO_ARMOR.get()) * stats.getDefenseMultiplier();
            armor.setBaseValue(Math.max(0.0D, armorValue));
        }

        AttributeInstance movementSpeed = get(mob, Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            double base = movementSpeed.getBaseValue();
            double spd = (base + agi * MobConfig.AGI_TO_SPEED.get()) * stats.getSpeedMultiplier();
            movementSpeed.setBaseValue(spd);
        }

        AttributeInstance knockback = get(mob, Attributes.KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            double kb = Math.min(1.0D, luk * MobConfig.LUK_TO_KB_RESIST.get());
            knockback.setBaseValue(kb);
        }
    }

    private AttributeInstance get(LivingEntity e, Attribute a) {
        return e != null && a != null ? e.getAttribute(a) : null;
    }
}
