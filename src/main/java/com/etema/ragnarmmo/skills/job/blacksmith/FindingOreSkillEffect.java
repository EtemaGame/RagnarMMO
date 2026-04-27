package com.etema.ragnarmmo.skills.job.blacksmith;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Finding Ore — Active (Blacksmith)
 * RO: Increases the hit rate and drop amount of ores when mining.
 * MC: Scans a (3 + level * 2) block radius cube for ore blocks
 *     and marks them with CRIT particles, notifying the player of each find.
 *     Act like a local "ore radar" scan. Cooldown 30s.
 */
public class FindingOreSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "finding_ore");

    private static final Set<Block> ORE_BLOCKS = Set.of(
            Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
            Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
            Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
            Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
            Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS
    );

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;

        int radius = 3 + level * 2;
        BlockPos center = player.blockPosition();
        List<BlockPos> found = new ArrayList<>();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (ORE_BLOCKS.contains(player.level().getBlockState(pos).getBlock())) {
                        found.add(pos);
                    }
                }
            }
        }

        if (found.isEmpty()) {
            player.sendSystemMessage(Component.literal("§7Finding Ore: No se encontraron menas en " + radius + " bloques."));
            return;
        }

        if (player.level() instanceof ServerLevel sl) {
            for (BlockPos pos : found) {
                // CRIT particles at each ore
                sl.sendParticles(ParticleTypes.CRIT,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5, 0.3, 0.3, 0.3, 0.1);
            }
            sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.3f);
        }

        player.sendSystemMessage(Component.literal(
                "§e✦ Finding Ore §f— " + found.size() + " mena(s) encontrada(s) en radio " + radius + "."));

        // List unique ore types
        found.stream()
                .map(pos -> player.level().getBlockState(pos).getBlock().getName().getString())
                .distinct()
                .forEach(name -> player.sendSystemMessage(Component.literal("  §7• §f" + name)));
    }
}
