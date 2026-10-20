package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → clientes cercanos: estado visual de un jugador (bits de SealVisuals). */
public record S2CVisualPacket(int entityId, int flags) {
    public static void encode(S2CVisualPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeVarInt(msg.flags);
    }

    public static S2CVisualPacket decode(FriendlyByteBuf buf) {
        return new S2CVisualPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(S2CVisualPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.visual(msg));
        ctx.get().setPacketHandled(true);
    }
}
