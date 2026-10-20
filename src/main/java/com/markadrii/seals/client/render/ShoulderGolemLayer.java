package com.markadrii.seals.client.render;

import com.markadrii.seals.seal.SealVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.IronGolem;

/** El mini golem sentado en el hombro izquierdo del jugador. */
public class ShoulderGolemLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/iron_golem/iron_golem.png");
    private static final float SCALE = 0.22F;
    private final IronGolemModel<IronGolem> model;

    public ShoulderGolemLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        this.model = new IronGolemModel<>(models.bakeLayer(ModelLayers.IRON_GOLEM));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !SealVisuals.shoulder(player)) return;

        poseStack.pushPose();
        // Pies del golem justo encima del hombro izquierdo (el modelo mide 24 px desde el origen hasta los pies)
        poseStack.translate(0.42F, (player.isCrouching() ? 0.2F : 0.0F) - 1.5F * SCALE, player.isCrouching() ? 0.1F : 0.0F);
        poseStack.scale(SCALE, SCALE, SCALE);

        float idle = Mth.sin(ageInTicks * 0.1F) * 0.05F;
        ModelPart root = model.root();
        root.getChild("head").xRot = idle;
        root.getChild("right_arm").xRot = -0.3F + idle;
        root.getChild("left_arm").xRot = -0.3F - idle;
        root.getChild("right_leg").xRot = 0.0F;
        root.getChild("left_leg").xRot = 0.0F;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
