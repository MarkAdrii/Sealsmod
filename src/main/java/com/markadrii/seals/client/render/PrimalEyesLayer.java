package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import com.markadrii.seals.seal.SealVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Instinto primal: los ojos del jugador brillan en rojo (se ven en la oscuridad). */
public class PrimalEyesLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation EYES = Seals.id("textures/entity/primal_eyes.png");
    private static final RenderType TYPE = RenderType.eyes(EYES);
    private static final RenderType CAT_TYPE = RenderType.eyes(Seals.id("textures/entity/cat_eyes.png"));

    public PrimalEyesLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) return;
        RenderType type = SealVisuals.eyes(player) ? TYPE : SealVisuals.catEyes(player) ? CAT_TYPE : null;
        if (type == null) return;
        getParentModel().renderToBuffer(poseStack, buffer.getBuffer(type), 0xF000F0, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);
    }
}
