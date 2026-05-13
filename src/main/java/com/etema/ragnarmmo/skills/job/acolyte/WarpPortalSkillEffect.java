package com.etema.ragnarmmo.skills.job.acolyte;

import com.etema.ragnarmmo.common.init.RagnarSounds;
import com.etema.ragnarmmo.entity.aoe.WarpPortalAoe;
import com.etema.ragnarmmo.items.UtilityItems;
import com.etema.ragnarmmo.skills.api.ISkillEffect;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class WarpPortalSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "warp_portal");

    @Override
    public ResourceLocation getSkillId() {
        return ID;
    }

    @Override
    public int getCastTime(int level) {
        return SkillRegistry.get(ID)
                .map(def -> def.getLevelInt("cast_time_ticks", level, 20))
                .orElse(20);
    }

    @Override
    public void execute(ServerPlayer player, int level) {
        if (level <= 0 || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            if (!UtilityItems.consumeBlueGemstone(player)) {
                player.sendSystemMessage(Component.translatable("message.ragnarmmo.no_resource",
                        Component.translatable("item.ragnarmmo.others.utility.blue_gemstone")));
                return;
            }

            WarpPortalHelper.DestinationResolution resolution = WarpPortalHelper.resolveDestination(player, skills,
                    level);
            WarpPortalHelper.WarpDestination destination = resolution.destination();

            int durationTicks = SkillRegistry.get(ID)
                    .map(def -> def.getLevelInt("duration_ticks", level, (5 + level * 5) * 20))
                    .orElse((5 + level * 5) * 20);

            WarpPortalAoe portal = new WarpPortalAoe(serverLevel, player, WarpPortalHelper.PORTAL_RADIUS,
                    durationTicks, destination);
            portal.setPos(player.getX(), player.getY() + 0.02, player.getZ());
            serverLevel.addFreshEntity(portal);

            BlockPos pos = player.blockPosition();
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15);
                double px = pos.getX() + 0.5 + Math.cos(angle) * 1.1;
                double pz = pos.getZ() + 0.5 + Math.sin(angle) * 1.1;
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, px, pos.getY() + 0.1, pz, 1, 0, 0.05, 0,
                        0.0);
                serverLevel.sendParticles(ParticleTypes.WITCH, px, pos.getY() + 0.2, pz, 1, 0, 0.05, 0, 0.0);
            }

            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), RagnarSounds.WARP_PORTAL.get(),
                    SoundSource.PLAYERS, 0.8f, 0.8f);

            if (resolution.notice() != null) {
                player.sendSystemMessage(resolution.notice());
            }

            BlockPos destinationPos = destination.pos();
            player.sendSystemMessage(Component.literal(
                    "§5✦ Warp Portal §fcreado hacia §d" + destination.displayName() + " §7("
                            + destinationPos.getX() + ", " + destinationPos.getY() + ", " + destinationPos.getZ()
                            + ")"));

            serverLevel.players().stream()
                    .filter(other -> other != player && other.distanceTo(player) < 24)
                    .forEach(other -> other.sendSystemMessage(Component.literal(
                            "§5[Warp Portal] §f" + player.getName().getString() + " abrió un portal a §d"
                                    + destination.displayName() + "§f.")));
        });
    }
}
