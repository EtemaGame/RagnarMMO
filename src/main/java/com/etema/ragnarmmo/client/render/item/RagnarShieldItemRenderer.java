package com.etema.ragnarmmo.client.render.item;

import com.etema.ragnarmmo.RagnarMMO;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RagnarShieldItemRenderer extends BlockEntityWithoutLevelRenderer
        implements ResourceManagerReloadListener {

    public static final RagnarShieldItemRenderer INSTANCE = new RagnarShieldItemRenderer();

    private static final Material FALLBACK_MATERIAL = new Material(Sheets.SHIELD_SHEET,
            ResourceLocation.fromNamespaceAndPath("minecraft", "entity/shield_base_nopattern"));

    private final Map<Item, Material> materialCache = new IdentityHashMap<>();
    private ShieldModel shieldModel;

    private RagnarShieldItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        shieldModel = null;
        materialCache.clear();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ShieldModel model = shieldModel();
        Material material = materialFor(stack.getItem());

        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);

        VertexConsumer consumer = material.sprite().wrap(ItemRenderer.getFoilBufferDirect(
                bufferSource,
                model.renderType(material.atlasLocation()),
                true,
                stack.hasFoil()));

        model.handle().render(poseStack, consumer, packedLight, packedOverlay);
        model.plate().render(poseStack, consumer, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private ShieldModel shieldModel() {
        if (shieldModel == null) {
            shieldModel = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        }
        return shieldModel;
    }

    private Material materialFor(Item item) {
        return materialCache.computeIfAbsent(item, RagnarShieldItemRenderer::createMaterial);
    }

    private static Material createMaterial(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) {
            return FALLBACK_MATERIAL;
        }

        String path = key.getPath();
        if (!path.startsWith("shield/") && !path.startsWith("shields/")) {
            return FALLBACK_MATERIAL;
        }

        String shieldId = path.startsWith("shield/")
                ? path.substring("shield/".length())
                : path.substring("shields/".length());
        return new Material(Sheets.SHIELD_SHEET,
                ResourceLocation.fromNamespaceAndPath(RagnarMMO.MODID, "entity/shield/" + shieldId));
    }
}
