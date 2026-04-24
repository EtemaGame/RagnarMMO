package com.etema.ragnarmmo.player.stats.progression;

import com.etema.ragnarmmo.common.api.jobs.JobType;
import com.etema.ragnarmmo.common.api.stats.IPlayerStats;
import com.etema.ragnarmmo.common.api.stats.Stats6;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class JobBonusService {

    public static final UUID JOB_BONUS_STR = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000001");
    public static final UUID JOB_BONUS_AGI = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000002");
    public static final UUID JOB_BONUS_VIT = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000003");
    public static final UUID JOB_BONUS_INT = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000004");
    public static final UUID JOB_BONUS_DEX = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000005");
    public static final UUID JOB_BONUS_LUK = UUID.fromString("c0c0c0c0-0000-4000-8000-000000000006");

    private JobBonusService() {
    }

    public static Stats6 getJobBonus(JobType job, int jobLevel) {
        if (job == null) {
            return Stats6.ZERO;
        }
        return JobBonusData.getBonus(job, jobLevel);
    }

    public static void recomputeStats(Player player, IPlayerStats stats) {
        if (player == null || stats == null) {
            return;
        }

        JobType job = JobType.fromId(stats.getJobId());
        int jobLevel = stats.getJobLevel();
        Stats6 bonus = getJobBonus(job, jobLevel);

        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.STR.get(), JOB_BONUS_STR,
                "Job Bonus STR", bonus.str());
        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.AGI.get(), JOB_BONUS_AGI,
                "Job Bonus AGI", bonus.agi());
        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.VIT.get(), JOB_BONUS_VIT,
                "Job Bonus VIT", bonus.vit());
        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.INT.get(), JOB_BONUS_INT,
                "Job Bonus INT", bonus.int_());
        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.DEX.get(), JOB_BONUS_DEX,
                "Job Bonus DEX", bonus.dex());
        applyBonus(player, com.etema.ragnarmmo.common.api.attributes.RagnarAttributes.LUK.get(), JOB_BONUS_LUK,
                "Job Bonus LUK", bonus.luk());
    }

    private static void applyBonus(Player player, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id,
            String name, int amount) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) {
            return;
        }

        inst.removeModifier(id);
        if (amount != 0) {
            inst.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
