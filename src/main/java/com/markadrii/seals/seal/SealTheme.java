package com.markadrii.seals.seal;

import net.minecraft.resources.ResourceLocation;

/**
 * Colores y glifos de la interfaz de cada mob sellado. Todos los colores en ARGB (0xAARRGGBB).
 */
public record SealTheme(
        int background,     // fondo general
        int header,         // barra superior
        int headerText,
        int panel,          // paneles de contenido
        int panelLight,     // paneles claros (botones, listas)
        int border,
        int text,
        int textMuted,
        int accent,         // títulos y detalles
        int affinityBar,
        int stabilityBar,
        int ageBar,
        int starOn,
        int starOff,
        ResourceLocation emblem,   // icono grande del mob (arriba a la izquierda)
        ResourceLocation glyphs    // hoja de glifos decorativos (64x16, 4 glifos de 16x16)
) {
}
