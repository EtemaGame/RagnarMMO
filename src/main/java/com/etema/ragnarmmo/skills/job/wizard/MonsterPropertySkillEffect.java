package com.etema.ragnarmmo.skills.job.wizard;

import com.etema.ragnarmmo.combat.contract.CombatStrictMode;
import com.etema.ragnarmmo.combat.contract.CombatantProfileResolver;
import com.etema.ragnarmmo.combat.element.CombatPropertyResolver;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.job.mage.MageTargetUtil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class MonsterPropertySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "monster_property");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) {
            return;
        }

        LivingEntity target = MageTargetUtil.raycast(player, 15.0);
        if (target == null) {
            return;
        }

        double defense = resolveRoDefense(player, target);
        String race = describeRace(target);
        String element = CombatPropertyResolver.getElementId(CombatPropertyResolver.getDefensiveElement(target)).toUpperCase();
        String size = CombatPropertyResolver.getSizeId(target).toUpperCase();
        String weakness = describeWeakness(target);

        player.sendSystemMessage(Component.literal("== Monster Property =="));
        player.sendSystemMessage(Component.literal("Name: " + target.getName().getString()));
        player.sendSystemMessage(
                Component.literal("HP: " + (int) target.getHealth() + "/" + (int) target.getMaxHealth()));
        player.sendSystemMessage(Component.literal("DEF: " + (int) defense));
        player.sendSystemMessage(Component.literal("Race: " + race));
        player.sendSystemMessage(Component.literal("Element: " + element));
        player.sendSystemMessage(Component.literal("Size: " + size));
        player.sendSystemMessage(Component.literal("Hint: " + weakness));
        player.sendSystemMessage(Component.literal("======================"));

        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private String describeRace(LivingEntity target) {
        String raceId = CombatPropertyResolver.getRaceId(target);
        if (raceId.isBlank()) {
            return "NORMAL";
        }
        if ("demihuman".equals(raceId)) {
            return "DEMI-HUMAN";
        }
        return raceId.toUpperCase();
    }

    private double resolveRoDefense(ServerPlayer caster, LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            return CombatantProfileResolver.resolvePlayer(player, null)
                    .map(profile -> profile.defense().hardDef())
                    .orElse(0.0D);
        }
        if (target instanceof Mob mob) {
            return CombatantProfileResolver.resolveMob(mob, CombatStrictMode.current())
                    .map(profile -> profile.defense().hardDef())
                    .orElse(0.0D);
        }
        return target == caster ? CombatantProfileResolver.resolvePlayer(caster, null)
                .map(profile -> profile.defense().hardDef())
                .orElse(0.0D) : 0.0D;
    }

    private String describeWeakness(LivingEntity target) {
        return switch (CombatPropertyResolver.getDefensiveElement(target)) {
            case WATER -> "Weak to WIND";
            case EARTH -> "Weak to FIRE";
            case FIRE -> "Weak to WATER";
            case WIND -> "Weak to EARTH";
            case POISON -> "Weak to FIRE or WIND";
            case HOLY -> "Weak to DARK";
            case DARK -> "Weak to HOLY";
            case GHOST -> "Weak to GHOST";
            case UNDEAD -> "Weak to HOLY and FIRE";
            default -> "No major elemental weakness";
        };
    }
}
