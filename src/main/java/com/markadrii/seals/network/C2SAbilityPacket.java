package com.markadrii.seals.network;

import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: el jugador ha pulsado la tecla de una habilidad (0-8) en modo combate. */
public record C2SAbilityPacket(int slot) {
    public static void encode(C2SAbilityPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot);
    }

    public static C2SAbilityPacket decode(FriendlyByteBuf buf) {
        return new C2SAbilityPacket(buf.readVarInt());
    }

    public static void handle(C2SAbilityPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && msg.slot >= 0 && msg.slot < 9 && player.isAlive()) {
            SealData data = SealCapability.get(player);
            SealDefinition def = data.definition();
            if (def != null && msg.slot < def.abilities().size()) {
                def.activate(player, data, msg.slot);
            }
        }
        ctx.get().setPacketHandled(true);
    }
}
