package com.markadrii.seals.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor → cliente: lo que dice el mob en el mundo interior y las respuestas disponibles.
 * Si {@code close} es true, el diálogo se cierra (por ejemplo, al empezar una pelea).
 */
public record S2CDialogPacket(Component speaker, Component line, List<Integer> options, boolean close) {
    public static void encode(S2CDialogPacket msg, FriendlyByteBuf buf) {
        buf.writeComponent(msg.speaker);
        buf.writeComponent(msg.line);
        buf.writeVarInt(msg.options.size());
        for (int o : msg.options) buf.writeVarInt(o);
        buf.writeBoolean(msg.close);
    }

    public static S2CDialogPacket decode(FriendlyByteBuf buf) {
        Component speaker = buf.readComponent();
        Component line = buf.readComponent();
        int n = buf.readVarInt();
        List<Integer> options = new ArrayList<>(n);
        for (int i = 0; i < n; i++) options.add(buf.readVarInt());
        return new S2CDialogPacket(speaker, line, options, buf.readBoolean());
    }

    public static void handle(S2CDialogPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.dialog(msg));
        ctx.get().setPacketHandled(true);
    }
}
