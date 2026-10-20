package com.markadrii.seals.seal;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado visual de cada jugador (manto / forma de golem), válido en servidor y cliente.
 * En el servidor se lee de los datos del sello; en el cliente, de lo que envía el servidor.
 */
public final class SealVisuals {
    public record Flags(boolean manto, boolean golemForm) {}

    /** Solo se usa en el cliente: id de entidad → estado. */
    private static final Map<Integer, Flags> CLIENT = new ConcurrentHashMap<>();

    private SealVisuals() {}

    public static void setClient(int entityId, boolean manto, boolean golemForm) {
        if (!manto && !golemForm) CLIENT.remove(entityId);
        else CLIENT.put(entityId, new Flags(manto, golemForm));
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static boolean manto(Player player) {
        if (player.level().isClientSide) {
            Flags f = CLIENT.get(player.getId());
            return f != null && f.manto();
        }
        return SealCapability.get(player).manto();
    }

    public static boolean golemForm(Player player) {
        if (player.level().isClientSide) {
            Flags f = CLIENT.get(player.getId());
            return f != null && f.golemForm();
        }
        return SealCapability.get(player).golemForm();
    }
}
