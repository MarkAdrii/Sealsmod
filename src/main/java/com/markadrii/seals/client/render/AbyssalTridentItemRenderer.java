package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.TridentModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Dibuja el Tridente abisal en 3D en la mano (en el inventario se ve el icono plano). */
public class AbyssalTridentItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ResourceLocation TEXTURE = Seals.id("textures/entity/abyssal_trident.png");

    @Nullable private static AbyssalTridentItemRenderer instance;
    @Nullable private TridentModel model;

    private AbyssalTridentItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static AbyssalTridentItemRenderer get() {
        if (instance == null) instance = new AbyssalTridentItemRenderer();
        return instance;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        model = new TridentModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.TRIDENT));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffer, int light, int overlay) {
        if (model == null) onResourceManagerReload(Minecraft.getInstance().getResourceManager());
        poseStack.pushPose();
        poseStack.scale(1.0F, -1.0F, -1.0F);
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffer, model.renderType(TEXTURE), false, stack.hasFoil());
        model.renderToBuffer(poseStack, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
