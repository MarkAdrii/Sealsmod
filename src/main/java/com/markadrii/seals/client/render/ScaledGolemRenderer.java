package com.markadrii.seals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.IronGolemCrackinessLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;

/** Iron golem normal dibujado a otra escala (mini golem y golem del mundo interior). */
public class ScaledGolemRenderer<T extends IronGolem> extends MobRenderer<T, IronGolemModel<T>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/iron_golem/iron_golem.png");
    private final float scale;

    public ScaledGolemRenderer(EntityRendererProvider.Context context, float scale) {
        super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.7F * scale);
        this.scale = scale;
        addLayer(new IronGolemCrackinessLayer<>(this));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void setupRotations(T entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        if (entity.walkAnimation.speed() >= 0.01) {
            float pos = entity.walkAnimation.position(partialTick) + 6.0F;
            float sway = (Math.abs(pos % 13.0F - 6.5F) - 3.25F) / 3.25F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(6.5F * sway));
        }
    }
}
