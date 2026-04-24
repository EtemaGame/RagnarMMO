package com.etema.ragnarmmo.player.stats.network;

import com.etema.ragnarmmo.skill.api.SkillCategory;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpgradeSkill {
    private final ResourceLocation skillId;

    public PacketUpgradeSkill(ResourceLocation skillId) {
        this.skillId = skillId;
    }

    public PacketUpgradeSkill(FriendlyByteBuf buf) {
        this.skillId = buf.readResourceLocation();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(skillId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // Check if skill exists in registry.
            var defOpt = SkillRegistry.get(skillId);
            if (defOpt.isEmpty()) {
                return;
            }
            var def = defOpt.get();

            // Life skills use point-based progression, not skill points.
            if (def.getCategory() == SkillCategory.LIFE) {
                return;
            }

            PlayerSkillsProvider.get(player).ifPresent(manager -> manager.tryUpgradeSkill(skillId));
        });
        ctx.get().setPacketHandled(true);
    }
}
