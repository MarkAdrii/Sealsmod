package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: lo que te ha dicho el mob sellado al pulsar Hablar (se muestra en la interfaz de la U). */
public record S2CSealLinePacket(Component line) {
    public static void encode(S2CSealLinePacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.line);
    }

    public static S2CSealLinePacket decode(FriendlyByteBuf buf) {
        return new S2CSealLinePacket(buf.readComponent());
    }

    public static void handle(S2CSealLinePacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.sealLine(msg));
        ctx.get().setPacketHandled(true);
    }
}
