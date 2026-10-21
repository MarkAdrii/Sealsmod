package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → clientes: reproducir un efecto visual (partículas Lodestone). */
public record S2CFxPacket(Type type, double x, double y, double z, int entityId) {

    public enum Type {
        SEAL,          // un mob es sellado
        SEAL_BREAK,    // el sello se rompe
        LEVEL_UP,      // nueva estrella
        MANTO_ON,
        MANTO_OFF,
        IRON_FEET,     // aterrizaje con pies de hierro
        GOLEM_FORM_ON,
        GOLEM_FORM_OFF,
        GOLEM_PUNCH,
        HOLY_SUMMON,
        HOLY_VANISH,
        DOOR_DUST,     // la puerta del mundo interior se abre
        TOUCH_GROUND,  // el jugador toca el suelo para invocar al mini golem
        BOND,          // se fortalece el vínculo (BOUND)
        RELEASE,       // el sello se rompe en paz
        INSTINCT,      // lobo: se activa el instinto primal
        WEREWOLF,      // lobo: transformación en hombre lobo
        CLAW,          // lobo: zarpazo
        BEAM_HIT,      // elder: impacto del rayo
        ABYSSAL,       // elder: forma abisal / tridente
        CAT_SIGHT,     // gato: se activan los ojos felinos
        CAT_FORM       // gato: transformación en gato negro
    }

    public static void encode(S2CFxPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.type);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
        buf.writeVarInt(msg.entityId);
    }

    public static S2CFxPacket decode(FriendlyByteBuf buf) {
        return new S2CFxPacket(buf.readEnum(Type.class), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt());
    }

    public static void handle(S2CFxPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.fx(msg));
        ctx.get().setPacketHandled(true);
    }
}
