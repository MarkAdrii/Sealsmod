package com.markadrii.seals.client;

import com.markadrii.seals.seal.SealData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/** Copia en el cliente de los datos del sello del jugador local + estado de la interfaz. */
public final class ClientSealData {
    private static final SealData LOCAL = new SealData();
    private static boolean combatMode;
    @Nullable private static Component lastLine;
    private static boolean inInner;
    private static BlockPos innerDoor = BlockPos.ZERO;

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

    /** Última frase que te dijo el mob al pulsar Hablar (se muestra en la U). */
    @Nullable
    public static Component lastLine() {
        return lastLine;
    }

    public static void setLastLine(@Nullable Component line) {
        lastLine = line;
    }

    public static boolean inInner() {
        return inInner;
    }

    public static BlockPos innerDoor() {
        return innerDoor;
    }

    public static void setInner(boolean inside, int x, int y, int z) {
        inInner = inside;
        innerDoor = new BlockPos(x, y, z);
    }

    public static void reset() {
        LOCAL.clearSeal();
        combatMode = false;
        lastLine = null;
        inInner = false;
    }
}
