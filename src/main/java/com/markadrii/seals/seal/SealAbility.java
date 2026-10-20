package com.markadrii.seals.seal;

import com.markadrii.seals.Seals;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Una habilidad de un mob sellado.
 *
 * @param id          nombre interno (se usa para textos e icono)
 * @param unlockLevel nivel de afecto necesario (1 = desde el principio, 2 = primera estrella...)
 * @param kind        cómo se usa
 */
public record SealAbility(String id, int unlockLevel, Kind kind) {

    public enum Kind {
        /** Se enciende y se apaga con su tecla. */
        TOGGLE,
        /** Siempre activa, no hace falta pulsar nada. */
        PASSIVE,
        /** Dura un tiempo y luego tiene recarga. */
        TIMED,
        /** Invoca algo. */
        SUMMON,
        /** Un ataque que se lanza y tiene recarga. */
        ACTIVE
    }

    public Component name() {
        return Component.translatable("ability.seals." + id);
    }

    public Component description() {
        return Component.translatable("ability.seals." + id + ".desc");
    }

    public Component kindName() {
        return Component.translatable("ability.seals.kind." + kind.name().toLowerCase());
    }

    public ResourceLocation icon() {
        return Seals.id("textures/gui/ability/" + id + ".png");
    }

    public boolean unlocked(SealData data) {
        return data.level() >= unlockLevel;
    }
}
