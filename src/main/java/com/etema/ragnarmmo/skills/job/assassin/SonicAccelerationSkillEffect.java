package com.etema.ragnarmmo.skills.job.assassin;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sonic Acceleration — Passive (Assassin)
 * RO: Reduces fixed casting time of Sonic Blow.
 * MC: Reduces the Sonic Blow cooldown by (level * 10)%.
 *     Stored in PersistentData. SonicBlowSkillEffect reads sonic_accel_level
 *     when computing its cooldown before re-allowing the skill.
 */
public class SonicAccelerationSkillEffect implements ISkillEffect {

    private static final ResourceLocation ID = new ResourceLocation("ragnarmmo", "sonic_acceleration");

    @Override
    public ResourceLocation getSkillId() { return ID; }

    @Override
    public void execute(ServerPlayer player, int level) {
        player.getPersistentData().putInt("sonic_accel_level", level);
        if (level > 0) {
            player.sendSystemMessage(Component.literal(
                    "§4✦ Sonic Acceleration §flv." + level
                    + " — Cooldown de Sonic Blow –" + (level * 10) + "%"));
        }
    }
}
