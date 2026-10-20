package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import com.markadrii.seals.seal.SealVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Manto ferruquímico: cubre la skin del jugador con una capa de hierro gris brillante.
 * Se mantiene la skin debajo (la capa es translúcida) y se añade el brillo de encantamiento.
 */
public class MantoLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = Seals.id("textures/entity/manto_overlay.png");

    public MantoLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !SealVisuals.manto(player)) return;

        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        float alpha = 0.62F + 0.10F * Mth.sin(ageInTicks * 0.12F);

        poseStack.pushPose();
        poseStack.scale(1.02F, 1.02F, 1.02F);

        // Capa de hierro
        VertexConsumer iron = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
        getParentModel().renderToBuffer(poseStack, iron, packedLight, overlay, 0.92F, 0.94F, 0.97F, alpha);

        // Brillo metálico
        VertexConsumer glint = buffer.getBuffer(RenderType.entityGlint());
        getParentModel().renderToBuffer(poseStack, glint, packedLight, overlay, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
