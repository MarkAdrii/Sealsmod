package com.markadrii.seals.client.gui;

import com.markadrii.seals.Seals;
import com.markadrii.seals.network.S2CScreenEffectPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

/**
 * Animación demoníaca a pantalla completa:
 * <ul>
 *   <li>SELLAR: la pantalla se oscurece en rojo, un sello demoníaco gira, se encoge sobre ti y se graba.</li>
 *   <li>ROMPER: aparece el sello, se agrieta, estalla con un destello y la pantalla tiembla.</li>
 * </ul>
 */
public final class SealScreenEffect {
    private static final ResourceLocation SEAL = Seals.id("textures/gui/effect/demon_seal.png");
    private static final ResourceLocation CRACKS = Seals.id("textures/gui/effect/seal_cracks.png");
    private static final ResourceLocation VIGNETTE = Seals.id("textures/gui/effect/vignette.png");

    public static final int DURATION = 80;

    @Nullable private static S2CScreenEffectPacket.Effect current;
    private static long startTick;

    public static final IGuiOverlay OVERLAY = SealScreenEffect::render;

    private SealScreenEffect() {}

    public static void start(S2CScreenEffectPacket.Effect effect) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        current = effect;
        startTick = mc.level.getGameTime();
        var sounds = mc.getSoundManager();
        if (effect == S2CScreenEffectPacket.Effect.SEAL) {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 0.5F, 0.35F));
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6F, 0.6F));
        } else {
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 0.4F));
            sounds.play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_ROAR, 0.6F, 0.5F));
        }
    }

    /** Ticks transcurridos (con fracción) o -1 si no hay animación. */
    public static float time(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (current == null || mc.level == null) return -1;
        float t = mc.level.getGameTime() - startTick + partialTick;
        if (t > DURATION || t < 0) {
            current = null;
            return -1;
        }
        return t;
    }

    /** Intensidad del temblor de cámara (0-1). */
    public static float shake(float partialTick) {
        float t = time(partialTick);
        if (t < 0) return 0;
        if (current == S2CScreenEffectPacket.Effect.BREAK) {
            return t < 34 ? t / 34F * 0.4F : Math.max(0, 1F - (t - 34) / 30F);
        }
        return t > 20 && t < 40 ? 0.35F : Math.max(0, 0.2F - Math.abs(t - 30) / 100F);
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        float t = time(partialTick);
        if (t < 0) return;
        boolean seal = current == S2CScreenEffectPacket.Effect.SEAL;
        float fadeOut = Mth.clamp((DURATION - t) / 15F, 0F, 1F);
        float fadeIn = Mth.clamp(t / 6F, 0F, 1F);
        float alpha = fadeIn * fadeOut;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Fondo rojo sangre en toda la pantalla (también en el centro), con bordes más oscuros
        int red = (int) (alpha * 225) << 24;
        g.fill(0, 0, width, height, red | (seal ? 0x6A0008 : 0x55000A));
        RenderSystem.setShaderColor(0.12F, 0.0F, 0.0F, alpha);
        g.blit(VIGNETTE, 0, 0, width, height, 0, 0, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        int cx = width / 2;
        int cy = height / 2;
        int base = Math.min(width, height);

        // Sello que gira
        float size;
        float sealAlpha = alpha;
        float rot;
        if (seal) {
            // Llega desde muy grande, se encoge y se queda latiendo
            float k = Mth.clamp(t / 28F, 0F, 1F);
            float ease = 1F - (1F - k) * (1F - k) * (1F - k);
            size = base * Mth.lerp(ease, 2.6F, 0.7F) * (1F + 0.04F * Mth.sin(t * 0.6F));
            rot = (1F - ease) * 540F + t * 1.5F;
        } else {
            // Aparece, tiembla y estalla hacia fuera
            if (t < 34) {
                size = base * 0.7F;
                rot = t * 4F;
            } else {
                float k = (t - 34) / 20F;
                size = base * (0.7F + k * 1.8F);
                rot = 34 * 4F + (t - 34) * 12F;
                sealAlpha *= Mth.clamp(1F - k, 0F, 1F);
            }
        }
        drawCentered(g, SEAL, cx, cy, size, rot, sealAlpha);

        // Grietas al romperse
        if (!seal && t > 10 && t < 40) {
            float k = Mth.clamp((t - 10) / 22F, 0F, 1F);
            drawCentered(g, CRACKS, cx, cy, base * (0.3F + k * 1.4F), 0F, alpha * Math.min(1F, k * 2F));
        }

        // Destellos
        float flash = 0;
        if (seal && t > 26 && t < 34) flash = 1F - Math.abs(t - 28F) / 6F;
        if (!seal && t > 32 && t < 44) flash = 1F - Math.abs(t - 34F) / 10F;
        if (flash > 0) {
            int a = (int) (Mth.clamp(flash, 0F, 1F) * 200) << 24;
            g.fill(0, 0, width, height, a | (seal ? 0xFF2010 : 0xFFE0D0));
        }

        // Texto
        if (t > 24) {
            Font font = Minecraft.getInstance().font;
            Component text = Component.translatable(seal ? "screen.seals.effect.seal" : "screen.seals.effect.break");
            float textAlpha = Mth.clamp((t - 24) / 6F, 0F, 1F) * fadeOut;
            float jitter = (seal ? 1.5F : 3F) * shake(partialTick);
            float jx = (float) (Math.sin(t * 7.3) * jitter * 4);
            float jy = (float) (Math.cos(t * 5.1) * jitter * 4);
            g.pose().pushPose();
            g.pose().translate(cx + jx, cy + base * 0.36F + jy, 0);
            g.pose().scale(3F, 3F, 1F);
            int color = ((int) (textAlpha * 255) << 24) | 0xE01818;
            if (textAlpha > 0.02F) g.drawString(font, text, -font.width(text) / 2, -4, color, true);
            g.pose().popPose();
        }

        RenderSystem.disableBlend();
    }

    private static void drawCentered(GuiGraphics g, ResourceLocation tex, int cx, int cy, float size, float rotDeg, float alpha) {
        if (alpha <= 0.01F) return;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(rotDeg));
        int s = (int) size;
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        g.blit(tex, -s / 2, -s / 2, s, s, 0, 0, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.pose().popPose();
    }
}
