package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: entras/sales del mundo interior y dónde está la puerta (para la niebla dorada). */
public record S2CInnerWorldPacket(boolean inside, int doorX, int doorY, int doorZ) {
    public static void encode(S2CInnerWorldPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inside);
        buf.writeInt(msg.doorX);
        buf.writeInt(msg.doorY);
        buf.writeInt(msg.doorZ);
    }

    public static S2CInnerWorldPacket decode(FriendlyByteBuf buf) {
        return new S2CInnerWorldPacket(buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(S2CInnerWorldPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.innerWorld(msg));
        ctx.get().setPacketHandled(true);
    }
}
