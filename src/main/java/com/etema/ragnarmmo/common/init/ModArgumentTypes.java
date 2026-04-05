package com.etema.ragnarmmo.common.init;

import com.etema.ragnarmmo.RagnarMMO;
import com.etema.ragnarmmo.skill.runtime.SkillArgumentType;

import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModArgumentTypes {

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.COMMAND_ARGUMENT_TYPE)) {
            RagnarMMO.LOGGER.info("Registering RagnarMMO ArgumentTypes via RegisterEvent");
            ArgumentTypeInfos.registerByClass(
                    SkillArgumentType.class,
                    SingletonArgumentInfo.contextFree(SkillArgumentType::skill));
            ArgumentTypeInfos.registerByClass(
                    com.etema.ragnarmmo.common.command.JobArgumentType.class,
                    SingletonArgumentInfo.contextFree(com.etema.ragnarmmo.common.command.JobArgumentType::job));
            ArgumentTypeInfos.registerByClass(
                    com.etema.ragnarmmo.system.stats.commands.StatKeyArgumentType.class,
                    SingletonArgumentInfo
                            .contextFree(com.etema.ragnarmmo.system.stats.commands.StatKeyArgumentType::stat));
        }
    }
}
