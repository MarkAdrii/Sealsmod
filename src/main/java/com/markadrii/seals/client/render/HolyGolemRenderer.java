package com.markadrii.seals.client.render;

import com.markadrii.seals.entity.HolyGolemEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

/** Dibuja el Holy golem como un espíritu azul translúcido que brilla en la oscuridad. */
public class HolyGolemRenderer extends MobRenderer<HolyGolemEntity, IronGolemModel<HolyGolemEntity>> {
    private static final ResourceLocation GOLEM_TEXTURE = new ResourceLocation("minecraft", "textures/entity/iron_golem/iron_golem.png");
    private static final ResourceLocation AURA_TEXTURE = new ResourceLocation("minecraft", "textures/entity/creeper/creeper_armor.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public HolyGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.0F);
        addLayer(new SpiritLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(HolyGolemEntity entity) {
        return GOLEM_TEXTURE;
    }

    /** El cuerpo normal no se dibuja: lo dibuja la capa de espíritu. */
    @Override
    protected @Nullable RenderType getRenderType(HolyGolemEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return null;
    }

    /** Balanceo al andar, igual que el golem normal. */
    @Override
    protected void setupRotations(HolyGolemEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        if (entity.walkAnimation.speed() >= 0.01) {
            float pos = entity.walkAnimation.position(partialTick) + 6.0F;
            float sway = (Math.abs(pos % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }

    static class SpiritLayer extends RenderLayer<HolyGolemEntity, IronGolemModel<HolyGolemEntity>> {
        SpiritLayer(RenderLayerParent<HolyGolemEntity, IronGolemModel<HolyGolemEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, HolyGolemEntity entity,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            float flicker = 0.55F + 0.08F * Mth.sin(ageInTicks * 0.25F);
            int overlay = OverlayTexture.pack(0, entity.hurtTime > 0 ? 3 : 10);

            // Cuerpo azul translúcido
            VertexConsumer body = buffer.getBuffer(RenderType.entityTranslucentEmissive(GOLEM_TEXTURE));
            getParentModel().renderToBuffer(poseStack, body, FULL_BRIGHT, overlay, 0.45F, 0.75F, 1.0F, flicker);

            // Aura que se mueve alrededor
            float t = ageInTicks * 0.01F;
            poseStack.pushPose();
            poseStack.scale(1.06F, 1.06F, 1.06F);
            VertexConsumer aura = buffer.getBuffer(RenderType.energySwirl(AURA_TEXTURE, t % 1.0F, (t * 0.7F) % 1.0F));
            getParentModel().renderToBuffer(poseStack, aura, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0.35F, 0.65F, 1.0F, 1.0F);
            poseStack.popPose();
        }
    }
}
