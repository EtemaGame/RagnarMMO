package com.etema.ragnarmmo.skills.net;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.etema.ragnarmmo.combat.api.CombatActionType;
import com.etema.ragnarmmo.combat.api.CombatRequestContext;
import com.etema.ragnarmmo.combat.api.CombatTargetCandidate;
import com.etema.ragnarmmo.combat.engine.RagnarCombatEngine;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import com.etema.ragnarmmo.skills.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skills.targeting.SkillTargeting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

public class PacketUseSkill {
    private final String skillId;

    public PacketUseSkill(String skillId) {
        this.skillId = skillId;
    }

    public PacketUseSkill(FriendlyByteBuf buf) {
        this.skillId = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                forwardToCombatEngine(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void forwardToCombatEngine(ServerPlayer player) {
        ResourceLocation id = skillId.contains(":")
                ? ResourceLocation.tryParse(skillId)
                : ResourceLocation.fromNamespaceAndPath(com.etema.ragnarmmo.RagnarMMO.MODID, skillId);
        if (id == null || SkillRegistry.get(id).isEmpty()) {
            return;
        }

        PlayerSkillsProvider.get(player).ifPresent(skills -> {
            int level = skills.getSkillLevel(id);
            if (level <= 0) {
                return;
            }

            LivingEntity target = SkillTargeting.findEntityInSight(player, 15.0D);
            List<CombatTargetCandidate> candidates = target == null
                    ? List.of()
                    : List.of(new CombatTargetCandidate(target.getId(), "legacy_packet", 0.0D, false));
            int sequence = (int) Math.min(Integer.MAX_VALUE, player.serverLevel().getGameTime());
            RagnarCombatEngine.get().handleSkillUseRequest(new CombatRequestContext(
                    player,
                    CombatActionType.SKILL,
                    sequence,
                    0,
                    false,
                    player.getInventory().selected,
                    id.toString(),
                    candidates,
                    Map.of("level", level)));
        });
    }
}
