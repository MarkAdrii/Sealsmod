package com.markadrii.seals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.animal.Cat;

/** Gato negro gigante del mundo interior. */
public class ScaledCatRenderer extends CatRenderer {
    private final float scale;

    public ScaledCatRenderer(EntityRendererProvider.Context context, float scale) {
        super(context);
        this.scale = scale;
        this.shadowRadius = 0.4F * scale;
    }

    @Override
    protected void scale(Cat cat, PoseStack poseStack, float partialTick) {
        super.scale(cat, poseStack, partialTick);
        poseStack.scale(scale, scale, scale);
    }
}
