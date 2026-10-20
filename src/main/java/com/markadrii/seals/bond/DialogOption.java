package com.markadrii.seals.bond;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Respuestas que el jugador puede elegir al hablar con su mob en el mundo interior. */
public enum DialogOption {
    HOW(1),            // ¿Cómo estás?
    FEEL(2),           // ¿Cómo te sientes?
    FOOD_ASK(4),       // ¿Cuál es tu comida favorita?
    FOOD_OFFER(8),     // Ofrecer su comida favorita
    FIGHT(16),         // Pelear (2 estrellas)
    BOND(32),          // BOUND: fortalecer el vínculo (3 estrellas)
    BREAK(0),          // Romper el sello en paz
    BREAK_CONFIRM(0),  // Confirmar
    EXIT(0),           // Salir del mundo interior
    BYE(0);            // Cerrar la conversación

    /** Bit en SealData.visitFlags (0 = se puede repetir). */
    public final int bit;

    DialogOption(int bit) {
        this.bit = bit;
    }

    public Component label() {
        return Component.translatable("dialog.seals.option." + name().toLowerCase());
    }

    @Nullable
    public static DialogOption byId(int id) {
        DialogOption[] values = values();
        return id >= 0 && id < values.length ? values[id] : null;
    }
}
