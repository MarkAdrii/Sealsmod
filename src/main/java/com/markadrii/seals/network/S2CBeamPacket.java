package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → clientes: rayo abisal de un jugador hacia un objetivo (targetId = -1 para pararlo). */
public record S2CBeamPacket(int playerId, int targetId, int ticks) {
    public static void encode(S2CBeamPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.playerId);
        buf.writeVarInt(msg.targetId);
        buf.writeVarInt(msg.ticks);
    }

    public static S2CBeamPacket decode(FriendlyByteBuf buf) {
        return new S2CBeamPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(S2CBeamPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.markadrii.seals.client.fx.BeamFx.receive(msg));
        ctx.get().setPacketHandled(true);
    }
}
