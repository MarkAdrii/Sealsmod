package com.markadrii.seals.seal;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * Etapas de edad de un mob sellado (la edad máxima es 1000 años).
 * OLDEST_ONE es un rango oculto: solo lo tiene el primer jugador del mundo cuyo mob llegue a 800 años.
 */
public enum AgeStage {
    JOVEN(0, 20, 0xFF6DBE45, false),
    ADULTO(21, 55, 0xFF4A90D9, false),
    ANCIANO(56, 105, 0xFF9B6BD1, false),
    GRAN_ANCIANO(106, 220, 0xFFE08A2E, false),
    ANCESTRAL(221, 500, 0xFFC8322B, false),
    PRIMIGENIO(501, 1000, 0xFFF2C744, false),
    OLDEST_ONE(800, 1000, 0xFFB54BFF, true);

    public static final int MAX_YEARS = 1000;
    public static final int OLDEST_ONE_YEARS = 800;

    private final int minYears;
    private final int maxYears;
    private final int color;
    private final boolean hidden;

    AgeStage(int minYears, int maxYears, int color, boolean hidden) {
        this.minYears = minYears;
        this.maxYears = maxYears;
        this.color = color;
        this.hidden = hidden;
    }

    /** Etapa normal según los años (nunca devuelve el rango oculto). */
    public static AgeStage of(int years) {
        for (AgeStage stage : values()) {
            if (!stage.hidden && years <= stage.maxYears) return stage;
        }
        return PRIMIGENIO;
    }

    /** Etapa teniendo en cuenta si el jugador es el Oldest One del mundo. */
    public static AgeStage of(int years, boolean oldestOne) {
        if (oldestOne && years >= OLDEST_ONE_YEARS) return OLDEST_ONE;
        return of(years);
    }

    public int minYears() { return minYears; }
    public int maxYears() { return maxYears; }
    public int color() { return color; }
    public boolean hidden() { return hidden; }

    /** Progreso dentro de la etapa (0-1), con meses incluidos. */
    public float progress(int ageMonths) {
        float years = ageMonths / 12F;
        float span = maxYears + 1 - minYears;
        return Math.max(0F, Math.min(1F, (years - minYears) / span));
    }

    public MutableComponent displayName() {
        return Component.translatable("age_stage.seals." + name().toLowerCase())
                .withStyle(s -> s.withColor(TextColor.fromRgb(color & 0xFFFFFF)));
    }
}
