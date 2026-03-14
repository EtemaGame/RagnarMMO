package com.etema.ragnarmmo.system.skills.wizard;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import com.etema.ragnarmmo.system.skills.mage.MageTargetUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Monster Property — Active (Knowledge/Info skill)
 * RO: Allows the party to see the elemental property of a monster,
 *     useful for choosing the correct spells.
 *
 * Minecraft:
 *  - Extended Sense: shows MobType, armor value, and elemental weakness hint
 *    based on MobType and biome context.
 *  - Similar to Wizard's Sense skill, but includes a basic "weakness" analysis.
 */
public class MonsterPropertySkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "monster_property");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        LivingEntity target = MageTargetUtil.raycast(player, 15.0);
        if (target == null) return;

        // Build property info based on MobType (Java 17 compatible if/else)
        net.minecraft.world.entity.MobType mt = target.getMobType();
        String mobTypeStr;
        if (mt == net.minecraft.world.entity.MobType.UNDEAD) {
            mobTypeStr = "§5Undead §7(Weak to Holy/Fire)";
        } else if (mt == net.minecraft.world.entity.MobType.ARTHROPOD) {
            mobTypeStr = "§2Arthropod §7(Weak to Bane of Arthropods)";
        } else if (mt == net.minecraft.world.entity.MobType.ILLAGER) {
            mobTypeStr = "§fHuman §7(No elemental weakness)";
        } else {
            mobTypeStr = "§7Normal §7(Neutral properties)";
        }

        // Estimate armor as DEF
        double armor = target.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        boolean onFire = target.isOnFire();
        boolean inWater = target.isInWater();

        String waterStr = inWater ? " §b[WET]" : "";
        String fireStr  = onFire  ? " §c[BURNING]" : "";

        player.sendSystemMessage(Component.literal("§e══ Monster Property ══"));
        player.sendSystemMessage(Component.literal("§7Name: §f" + target.getName().getString()));
        player.sendSystemMessage(Component.literal("§7HP: §a" + (int)target.getHealth() + "/" + (int)target.getMaxHealth()));
        player.sendSystemMessage(Component.literal("§7DEF: §e" + (int)armor));
        player.sendSystemMessage(Component.literal("§7Type: " + mobTypeStr + waterStr + fireStr));
        player.sendSystemMessage(Component.literal("§e══════════════════"));

        // Visual: info pulse
        player.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.3, 0.5, 0.3, 0.05);
        }
    }
}
