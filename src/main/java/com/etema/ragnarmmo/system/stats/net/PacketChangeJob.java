package com.etema.ragnarmmo.system.stats.net;

import com.etema.ragnarmmo.common.api.RagnarCoreAPI;
import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.skill.api.SkillTier;
import com.etema.ragnarmmo.skill.runtime.PlayerSkillsProvider;
import com.etema.ragnarmmo.skill.data.SkillDefinition;
import com.etema.ragnarmmo.skill.data.SkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeJob {
    private final String jobId;

    public PacketChangeJob(String jobId) {
        this.jobId = jobId;
    }

    public PacketChangeJob(FriendlyByteBuf buf) {
        this.jobId = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(jobId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;

            JobType job = JobType.fromId(jobId);
            RagnarCoreAPI.get(player).ifPresent(stats -> {
                // Validate requirements (Novice Job Level 10)
                if ("ragnarmmo:novice".equals(stats.getJobId()) && stats.getJobLevel() < 10) {
                    player.sendSystemMessage(Component.translatable("message.ragnarmmo.low_job_level"));
                    return;
                }

                // Validate Skill Points (Must be 0)
                if (stats.getSkillPoints() > 0) {
                    player.sendSystemMessage(Component.translatable("message.ragnarmmo.unspent_skill_points"));
                    return;
                }

                // Reset skills if changing from Novice
                if ("ragnarmmo:novice".equals(stats.getJobId())) {
                    PlayerSkillsProvider.get(player).ifPresent(skills -> {
                        for (ResourceLocation skillId : SkillRegistry.getAllIds()) {
                            SkillDefinition def = SkillRegistry.require(skillId);
                            // Clear skills that aren't Novice or Life skills
                            if (def.getTier() != SkillTier.NOVICE && def.getTier() != SkillTier.LIFE) {
                                skills.setSkillLevel(skillId, 0, ChangeReason.SYSTEM);
                            }
                        }
                    });
                }

                stats.setJobId(job.getId());
                // Reset levels to 1 as per RO mechanics
                stats.setLevel(1);
                stats.setJobLevel(1);
                stats.setExp(0);
                stats.setJobExp(0);

                // Sync changes to both stats and skills
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                        new PlayerStatsSyncPacket(stats));
                PlayerSkillsProvider.get(player).ifPresent(skills -> {
                    com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                            new com.etema.ragnarmmo.system.stats.net.ClientboundSkillSyncPacket(skills.serializeNBT()));
                });
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
