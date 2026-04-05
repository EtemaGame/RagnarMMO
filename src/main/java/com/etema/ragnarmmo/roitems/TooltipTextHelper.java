package com.etema.ragnarmmo.roitems;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

final class TooltipTextHelper {

    private TooltipTextHelper() {
    }

    static Component displayName(String fallbackKey, String customName) {
        return customName != null && !customName.isBlank()
                ? Component.literal(customName)
                : Component.translatable(fallbackKey);
    }

    static void appendDescription(List<Component> tooltip, String fallbackKey, String customDescription) {
        if (customDescription != null && !customDescription.isBlank()) {
            tooltip.add(Component.literal(customDescription).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        tooltip.add(Component.translatable(fallbackKey + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
