package com.markadrii.seals.client.gui;

import com.markadrii.seals.seal.SealTheme;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Pequeñas utilidades de dibujo compartidas por la GUI y la hotbar de combate. */
public final class GuiDraw {
    private GuiDraw() {}

    /** Icono de 16x16 con transparencia opcional. */
    public static void icon(GuiGraphics g, ResourceLocation texture, int x, int y, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(texture, x, y, 0F, 0F, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Dibuja un trozo de una textura con transparencia. */
    public static void sprite(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                              int u, int v, int texW, int texH, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(texture, x, y, w, h, u, v, w, h, texW, texH);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    /** Remaches en las cuatro esquinas, como una placa de hierro. */
    public static void rivets(GuiGraphics g, int x, int y, int w, int h, SealTheme theme) {
        rivet(g, x + 2, y + 2, theme);
        rivet(g, x + w - 5, y + 2, theme);
        rivet(g, x + 2, y + h - 5, theme);
        rivet(g, x + w - 5, y + h - 5, theme);
    }

    public static void rivet(GuiGraphics g, int x, int y, SealTheme theme) {
        g.fill(x, y, x + 3, y + 3, theme.border());
        g.fill(x, y, x + 2, y + 2, 0xFFB9BEC2);
    }

    /** Candado pequeño (8x9). */
    public static void lock(GuiGraphics g, int x, int y) {
        g.fill(x + 2, y, x + 6, y + 1, 0xFFDDDDDD);
        g.fill(x + 1, y + 1, x + 2, y + 4, 0xFFDDDDDD);
        g.fill(x + 6, y + 1, x + 7, y + 4, 0xFFDDDDDD);
        g.fill(x, y + 4, x + 8, y + 9, 0xFFE0B040);
        g.fill(x + 3, y + 5, x + 5, y + 8, 0xFF5A3A10);
    }

    /** Barra con borde, fondo y relleno. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, float progress, int color, SealTheme theme) {
        g.fill(x, y, x + w, y + h, theme.border());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF2E3033);
        int fill = (int) ((w - 2) * Math.max(0F, Math.min(1F, progress)));
        if (fill > 0) {
            g.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, color);
            // brillo superior
            g.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x40FFFFFF);
        }
    }

    /** Estrella de 7x7 píxeles. */
    public static void star(GuiGraphics g, int x, int y, int color) {
        g.fill(x + 3, y, x + 4, y + 2, color);
        g.fill(x, y + 2, x + 7, y + 3, color);
        g.fill(x + 1, y + 3, x + 6, y + 4, color);
        g.fill(x + 2, y + 4, x + 5, y + 5, color);
        g.fill(x + 1, y + 5, x + 3, y + 6, color);
        g.fill(x + 4, y + 5, x + 6, y + 6, color);
        g.fill(x + 1, y + 6, x + 2, y + 7, color);
        g.fill(x + 5, y + 6, x + 6, y + 7, color);
    }

    /** Rectángulo hueco. */
    public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
