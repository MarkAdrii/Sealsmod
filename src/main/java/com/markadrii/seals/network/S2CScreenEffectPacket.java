package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: animación demoníaca a pantalla completa (solo para ese jugador). */
public record S2CScreenEffectPacket(Effect effect) {

    public enum Effect {
        SEAL,    // un mob queda sellado dentro de ti
        BREAK    // el sello se rompe
    }

    public static void encode(S2CScreenEffectPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.effect);
    }

    public static S2CScreenEffectPacket decode(FriendlyByteBuf buf) {
        return new S2CScreenEffectPacket(buf.readEnum(Effect.class));
    }

    public static void handle(S2CScreenEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.markadrii.seals.client.gui.SealScreenEffect.start(msg.effect()));
        ctx.get().setPacketHandled(true);
    }
}
