package com.markadrii.seals.network;

import com.markadrii.seals.bond.BondManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: botones de la interfaz del vínculo y acciones del mini golem. */
public record C2SSealActionPacket(Action action) {

    public enum Action {
        TALK,          // botón Hablar
        ENTER,         // botón Entrar (mundo interior)
        TOGGLE_MINI,   // botón Invocar / Desinvocar
        MINI_DOWN      // shift + clic derecho en el aire: bajar al mini golem del hombro
    }

    public static void encode(C2SSealActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
    }

    public static C2SSealActionPacket decode(FriendlyByteBuf buf) {
        return new C2SSealActionPacket(buf.readEnum(Action.class));
    }

    public static void handle(C2SSealActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && player.isAlive()) {
            switch (msg.action) {
                case TALK -> BondManager.talk(player);
                case ENTER -> BondManager.enterInner(player);
                case TOGGLE_MINI -> BondManager.toggleMini(player);
                case MINI_DOWN -> BondManager.miniDown(player, null);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
