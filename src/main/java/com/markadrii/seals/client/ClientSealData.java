package com.markadrii.seals.client;

import com.markadrii.seals.seal.SealData;

/** Copia en el cliente de los datos del sello del jugador local + estado del modo combate. */
public final class ClientSealData {
    private static final SealData LOCAL = new SealData();
    private static boolean combatMode;

    private ClientSealData() {}

    public static SealData get() {
        return LOCAL;
    }

    public static boolean combatMode() {
        return combatMode && LOCAL.hasSeal();
    }

    public static void setCombatMode(boolean value) {
        combatMode = value;
    }

    public static void reset() {
        LOCAL.clearSeal();
        combatMode = false;
    }
}
