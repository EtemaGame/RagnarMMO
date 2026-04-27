package com.etema.ragnarmmo.player.stats.network;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.player.RoPlayerDataAccess;
import com.etema.ragnarmmo.common.api.stats.ChangeReason;
import com.etema.ragnarmmo.common.config.RagnarConfigs;
import com.etema.ragnarmmo.skills.api.SkillTier;
import com.etema.ragnarmmo.skills.runtime.SkillManager;
import com.etema.ragnarmmo.skills.data.SkillDefinition;
import com.etema.ragnarmmo.skills.data.SkillRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketChangeJob {
    private static final ResourceLocation BASIC_SKILL = ResourceLocation.fromNamespaceAndPath("ragnarmmo", "basic_skill");
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

            JobType requestedJob = JobType.fromId(jobId);
            RoPlayerDataAccess.get(player).ifPresent(data -> {
                var stats = data.getStats();
                SkillManager skills = (SkillManager) data.getSkills();
                JobType currentJob = JobType.fromId(stats.getJobId());

                if (!currentJob.canPromoteTo(requestedJob)) {
                    player.sendSystemMessage(Component.translatable("message.ragnarmmo.invalid_job_change"));
                    return;
                }

                // Validate requirements (Novice Job Level 10)
                if (currentJob == JobType.NOVICE && stats.getJobLevel() < 10) {
                    player.sendSystemMessage(Component.translatable("message.ragnarmmo.low_job_level"));
                    return;
                }

                if (currentJob == JobType.NOVICE) {
                    int basicSkillLevel = skills.getSkillLevel(BASIC_SKILL);
                    if (basicSkillLevel < 9) {
                        player.sendSystemMessage(Component.translatable("message.ragnarmmo.low_basic_skill"));
                        return;
                    }
                }

                if (requestedJob.getTier() == 2) {
                    int requiredJobLevel = RagnarConfigs.SERVER.progression.secondJobChangeMinJobLevel.get();
                    if (stats.getJobLevel() < requiredJobLevel) {
                        player.sendSystemMessage(
                                Component.translatable("message.ragnarmmo.low_second_job_level", requiredJobLevel));
                        return;
                    }
                }

                // Validate Skill Points (Must be 0)
                if (stats.getSkillPoints() > 0) {
                    player.sendSystemMessage(Component.translatable("message.ragnarmmo.unspent_skill_points"));
                    return;
                }

                // Reset skills if changing from Novice
                if (currentJob == JobType.NOVICE) {
                    for (ResourceLocation skillId : SkillRegistry.getAllIds()) {
                        SkillDefinition def = SkillRegistry.require(skillId);
                        // Clear skills that aren't Novice or Life skills
                        if (def.getTier() != SkillTier.NOVICE && def.getTier() != SkillTier.LIFE) {
                            skills.setSkillLevel(skillId, 0, ChangeReason.SYSTEM);
                        }
                    }
                }

                stats.setJobId(requestedJob.getId());
                stats.setJobLevel(1);
                stats.setJobExp(0);

                // Sync changes to both stats and skills
                PlayerStatsSyncService.sync(player, stats);
                com.etema.ragnarmmo.common.net.Network.sendToPlayer(player,
                        new com.etema.ragnarmmo.player.stats.network.ClientboundSkillSyncPacket(skills.serializeNBT()));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
