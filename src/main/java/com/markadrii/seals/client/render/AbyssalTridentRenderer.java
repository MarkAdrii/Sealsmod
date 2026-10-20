package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrownTrident;

/** Tridente abisal en vuelo: el modelo del tridente con colores del Elder Guardian. */
public class AbyssalTridentRenderer extends ThrownTridentRenderer {
    private static final ResourceLocation TEXTURE = Seals.id("textures/entity/abyssal_trident.png");

    public AbyssalTridentRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownTrident trident) {
        return TEXTURE;
    }
}
