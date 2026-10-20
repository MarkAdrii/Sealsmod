package com.markadrii.seals.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.world.entity.animal.Wolf;

/** Lobo normal dibujado más grande (el lobo del mundo interior). */
public class ScaledWolfRenderer extends WolfRenderer {
    private final float scale;

    public ScaledWolfRenderer(EntityRendererProvider.Context context, float scale) {
        super(context);
        this.scale = scale;
        this.shadowRadius = 0.5F * scale;
    }

    @Override
    protected void scale(Wolf wolf, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }
}
