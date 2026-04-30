package com.etema.ragnarmmo.skills.job.hunter;

import com.etema.ragnarmmo.skills.api.ISkillEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import com.etema.ragnarmmo.common.init.RagnarCore;

@Mod.EventBusSubscriber(modid = RagnarCore.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BeastBaneSkillEffect implements ISkillEffect {

    private static final String SKILL_ID = "ragnarmmo:beast_bane";

    @Override
    public void execute(ServerPlayer player, int currentLevel) {
        // Passive skill, does nothing on cast
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.ragnarmmo.skill_passive"));
    }
}
