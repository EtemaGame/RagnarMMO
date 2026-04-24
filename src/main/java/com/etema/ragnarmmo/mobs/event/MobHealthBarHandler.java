package com.etema.ragnarmmo.mobs.event;

import com.etema.ragnarmmo.RagnarMMO;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = RagnarMMO.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobHealthBarHandler {

    private static final int DISPLAY_TICKS = 60;
    private static final WeakHashMap<LivingEntity, NameData> NAME_DATA = new WeakHashMap<>();

    private MobHealthBarHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMobDamaged(LivingDamageEvent event) {
        // Disabled to prevent conflict with RagnarBar
    }

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide())
            return;
        if (entity instanceof Player)
            return;

        NameData data = NAME_DATA.get(entity);
        if (data == null)
            return;

        if (!entity.isAlive()) {
            NAME_DATA.remove(entity);
            return;
        }

        if (!data.showing)
            return;

        if (entity.tickCount >= data.expireTick) {
            if (data.originalName != null) {
                entity.setCustomName(data.originalName);
            } else {
                entity.setCustomName(null);
            }
            entity.setCustomNameVisible(data.originalVisible);
            data.showing = false;
            NAME_DATA.remove(entity);
        }
    }

    private static NameData createData(LivingEntity entity) {
        Component current = entity.getCustomName();
        Component stored = current != null ? current.copy() : null;
        return new NameData(stored, entity.isCustomNameVisible());
    }

    private static MutableComponent buildHudComponent(
            LivingEntity entity,
            int level,
            float currentHealth,
            float maxHealth,
            Component originalName) {
        float ratio = maxHealth > 0.0F ? currentHealth / maxHealth : 0.0F;
        ratio = Mth.clamp(ratio, 0.0F, 1.0F);

        MutableComponent nameComponent = originalName != null
                ? originalName.copy()
                : entity.getType().getDescription().copy();

        MutableComponent header = Component.literal("")
                .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("Lv " + level).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(nameComponent.withStyle(ChatFormatting.WHITE));

        MutableComponent bar = buildHealthBarComponent(ratio);
        int currentValue = Mth.clamp(Mth.ceil(currentHealth), 0, Integer.MAX_VALUE);
        int maxValue = Mth.clamp(Mth.ceil(maxHealth), 1, Integer.MAX_VALUE);
        MutableComponent numbers = Component.literal(" " + currentValue + "/" + maxValue)
                .withStyle(ChatFormatting.WHITE);

        return Component.literal("")
                .append(header)
                .append(Component.literal("\n").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("HP ").withStyle(ChatFormatting.GRAY))
                .append(bar)
                .append(numbers);
    }

    private static MutableComponent buildHealthBarComponent(float ratio) {
        final int segments = 10;
        int filled = Mth.clamp(Math.round(ratio * segments), 0, segments);

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < segments; i++) {
            builder.append(i < filled ? '█' : '░');
        }
        builder.append(']');

        ChatFormatting color;
        if (ratio >= 0.6F) {
            color = ChatFormatting.GREEN;
        } else if (ratio >= 0.3F) {
            color = ChatFormatting.YELLOW;
        } else {
            color = ChatFormatting.RED;
        }

        return Component.literal(builder.toString()).withStyle(color);
    }

    private static final class NameData {
        private final Component originalName;
        private final boolean originalVisible;
        private int expireTick;
        private boolean showing;

        private NameData(Component originalName, boolean originalVisible) {
            this.originalName = originalName;
            this.originalVisible = originalVisible;
        }
    }
}






