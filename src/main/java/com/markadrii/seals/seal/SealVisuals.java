package com.markadrii.seals.seal;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado visual de cada jugador (lo que ven los demás), válido en servidor y cliente.
 * En el servidor se calcula de los datos del sello; en el cliente, llega del servidor.
 */
public final class SealVisuals {
    public static final int MANTO = 1;        // Iron Golem: manto ferruquímico
    public static final int GOLEM_FORM = 2;   // Iron Golem: yo soy el golem
    public static final int SHOULDER = 4;     // mini golem en el hombro
    public static final int WEREWOLF = 8;     // Lobo: hombre lobo
    public static final int EYES = 16;        // Lobo: ojos rojos (instinto primal)
    public static final int ABYSSAL = 32;     // Elder Guardian: forma abisal

    /** Solo se usa en el cliente: id de entidad → bits. */
    private static final Map<Integer, Integer> CLIENT = new ConcurrentHashMap<>();

    private SealVisuals() {}

    /** Bits que corresponden a los datos del sello (servidor). */
    public static int flags(SealData data) {
        SealDefinition def = data.definition();
        int f = def == null ? 0 : def.visualFlags(data);
        if (data.miniOut() && data.miniOnShoulder()) f |= SHOULDER;
        return f;
    }

    public static void setClient(int entityId, int flags) {
        if (flags == 0) CLIENT.remove(entityId);
        else CLIENT.put(entityId, flags);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    public static boolean has(Player player, int bit) {
        int f;
        if (player.level().isClientSide) {
            f = CLIENT.getOrDefault(player.getId(), 0);
        } else {
            f = flags(SealCapability.get(player));
        }
        return (f & bit) != 0;
    }

    public static boolean manto(Player player) { return has(player, MANTO); }
    public static boolean golemForm(Player player) { return has(player, GOLEM_FORM); }
    public static boolean shoulder(Player player) { return has(player, SHOULDER); }
    public static boolean werewolf(Player player) { return has(player, WEREWOLF); }
    public static boolean eyes(Player player) { return has(player, EYES); }
    public static boolean abyssal(Player player) { return has(player, ABYSSAL); }
}
