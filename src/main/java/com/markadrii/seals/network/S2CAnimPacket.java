package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → clientes: reproducir una animación de Player Animator en un jugador. */
public record S2CAnimPacket(int entityId, String animation) {
    public static void encode(S2CAnimPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeUtf(msg.animation, 64);
    }

    public static S2CAnimPacket decode(FriendlyByteBuf buf) {
        return new S2CAnimPacket(buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(S2CAnimPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.anim(msg));
        ctx.get().setPacketHandled(true);
    }
}
