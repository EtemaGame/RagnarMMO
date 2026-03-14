package com.etema.ragnarmmo.system.skills.hunter;

import com.etema.ragnarmmo.system.skills.ISkillEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Talkie Box — Active Utility Trap
 * RO: Alert system — notifies the Hunter when an enemy triggers the box.
 *
 * Minecraft:
 *  - Non-damaging trap: sends owner a chat message + applies GLOWING to the intruder.
 *  - 5-minute duration; triggers once then is removed.
 */
public class TalkieBoxSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "talkie_box");
    private final HunterTrapManager.TrapDefinition definition;

    public TalkieBoxSkillEffect() {
        this.definition = new HunterTrapManager.TrapDefinition(
                "ragnarmmo:talkie_box",
                2.0,
                20 * 300,
                ParticleTypes.END_ROD,
                ParticleTypes.ENCHANT,
                (trap, target) -> {
                    if (trap.owner instanceof ServerPlayer ownerPlayer) {
                        ownerPlayer.sendSystemMessage(Component.literal(
                                "§c[Talkie Box] §fEnemy detected at §e" +
                                trap.position.toShortString() + "§f: §c" + target.getName().getString()));
                    }
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 10, 0, false, false, false));

                    trap.level.sendParticles(ParticleTypes.ENCHANT,
                            trap.position.getX() + 0.5, trap.position.getY() + 1, trap.position.getZ() + 0.5,
                            15, 0.3, 0.5, 0.3, 0.05);
                    trap.level.playSound(null, trap.position,
                            SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 2.0f);
                });
    }

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0) return;
        BlockPos pos = getTargetBlock(player);
        if (pos == null) { player.sendSystemMessage(Component.literal("§cNo block target.")); return; }
        if (!(player.level() instanceof ServerLevel sl)) return;

        HunterTrapManager.placeTrap(player, sl, pos.above(), definition);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7f, 1.5f);
        player.sendSystemMessage(Component.literal("§eTalkie Box placed at §f" + pos.above().toShortString()));
    }

    private BlockPos getTargetBlock(ServerPlayer player) {
        var h = player.pick(6.0, 0, false);
        return h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? ((net.minecraft.world.phys.BlockHitResult) h).getBlockPos() : null;
    }
}
