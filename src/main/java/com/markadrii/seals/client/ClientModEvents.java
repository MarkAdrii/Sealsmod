package com.markadrii.seals.client;

import com.markadrii.seals.Seals;
import com.markadrii.seals.client.anim.SealAnimations;
import com.markadrii.seals.client.gui.CombatHud;
import com.markadrii.seals.client.gui.SealScreenEffect;
import com.markadrii.seals.client.render.HolyGolemRenderer;
import com.markadrii.seals.client.render.MantoLayer;
import com.markadrii.seals.client.render.ScaledGolemRenderer;
import com.markadrii.seals.client.render.ShoulderGolemLayer;
import com.markadrii.seals.client.render.InnerWorldEffects;
import com.markadrii.seals.entity.InnerGolemEntity;
import com.markadrii.seals.entity.MiniGolemEntity;
import com.markadrii.seals.world.InnerWorld;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import com.markadrii.seals.registry.ModEntities;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registro de cosas del cliente (teclas, renderers, capas, overlays, animaciones). */
@Mod.EventBusSubscriber(modid = Seals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(SealAnimations::init);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KeyBindings.OPEN_SEAL);
        event.register(KeyBindings.COMBAT_MODE);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLY_GOLEM.get(), HolyGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.MINI_GOLEM.get(),
                ctx -> new ScaledGolemRenderer<>(ctx, MiniGolemEntity.RENDER_SCALE));
        event.registerEntityRenderer(ModEntities.INNER_GOLEM.get(),
                ctx -> new ScaledGolemRenderer<>(ctx, InnerGolemEntity.RENDER_SCALE));
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            LivingEntityRenderer<?, ?> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                playerRenderer.addLayer(new MantoLayer(playerRenderer));
                playerRenderer.addLayer(new ShoulderGolemLayer(playerRenderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(InnerWorld.KEY.location(), new InnerWorldEffects());
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "combat_bar", CombatHud.OVERLAY);
        event.registerAboveAll("seal_effect", SealScreenEffect.OVERLAY);
    }
}
