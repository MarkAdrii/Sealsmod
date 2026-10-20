package com.markadrii.seals.network;

import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.bond.DialogOption;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente → servidor: el jugador elige una respuesta en el diálogo del mundo interior. */
public record C2SDialogOptionPacket(int option) {
    public static void encode(C2SDialogOptionPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.option);
    }

    public static C2SDialogOptionPacket decode(FriendlyByteBuf buf) {
        return new C2SDialogOptionPacket(buf.readVarInt());
    }

    public static void handle(C2SDialogOptionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        DialogOption option = DialogOption.byId(msg.option);
        if (player != null && option != null) BondManager.chooseOption(player, option);
        ctx.get().setPacketHandled(true);
    }
}
