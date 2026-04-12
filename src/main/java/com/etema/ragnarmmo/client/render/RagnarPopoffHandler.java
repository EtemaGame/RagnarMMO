package com.etema.ragnarmmo.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = com.etema.ragnarmmo.RagnarMMO.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RagnarPopoffHandler {

    private static final Map<Integer, List<Popoff>> popoffs = new ConcurrentHashMap<>();

    public static void addPopoff(int entityId, String text, int color) {
        popoffs.computeIfAbsent(entityId, k -> new ArrayList<>())
               .add(new Popoff(text, color, System.currentTimeMillis()));
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<LivingEntity, ?> e) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = e.getEntity();
        
        List<Popoff> entityPopoffs = popoffs.get(entity.getId());
        if (entityPopoffs == null || entityPopoffs.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(0.0D, entity.getBbHeight() + 1.2D, 0.0D);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        
        // Scale appropriately for text
        float scale = -0.025F * 0.82F;
        ps.scale(scale, scale, -scale); // Inverse Z to avoid mirroring
        
        Font font = mc.font;
        
        Iterator<Popoff> it = entityPopoffs.iterator();
        while (it.hasNext()) {
            Popoff p = it.next();
            long age = now - p.startTime;
            if (age > 1500) { // 1.5 second duration
                it.remove();
                continue;
            }

            // Animate floating upwards
            float yOffset = -((float)age / 1500.0f) * 20.0f;
            
            // Fade out
            int alpha = 255;
            if (age > 1000) {
                alpha = (int)(255 * (1.0f - (age - 1000) / 500.0f));
            }
            int color = (p.color & 0x00FFFFFF) | (alpha << 24);
            
            ps.pushPose();
            ps.translate(0, yOffset, 0);
            
            font.drawInBatch(p.text, -font.width(p.text) / 2f, 0, color, false,
                    ps.last().pose(), e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
            
            ps.popPose();
        }
        
        ps.popPose();
    }

    private static class Popoff {
        String text;
        int color;
        long startTime;

        Popoff(String text, int color, long startTime) {
            this.text = text;
            this.color = color;
            this.startTime = startTime;
        }
    }
}
