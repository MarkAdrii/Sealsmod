package com.markadrii.seals.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor → cliente: datos completos del sello del jugador. */
public record S2CSyncSealPacket(CompoundTag tag) {
    public static void encode(S2CSyncSealPacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.tag);
    }

    public static S2CSyncSealPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new S2CSyncSealPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(S2CSyncSealPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.syncSeal(msg));
        ctx.get().setPacketHandled(true);
    }
}
