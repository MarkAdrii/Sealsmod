package com.markadrii.seals.network;

import com.markadrii.seals.Seals;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String VERSION = "2";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Seals.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private ModNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(S2CSyncSealPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncSealPacket::encode).decoder(S2CSyncSealPacket::decode)
                .consumerMainThread(S2CSyncSealPacket::handle).add();
        CHANNEL.messageBuilder(S2CVisualPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CVisualPacket::encode).decoder(S2CVisualPacket::decode)
                .consumerMainThread(S2CVisualPacket::handle).add();
        CHANNEL.messageBuilder(S2CFxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CFxPacket::encode).decoder(S2CFxPacket::decode)
                .consumerMainThread(S2CFxPacket::handle).add();
        CHANNEL.messageBuilder(S2CAnimPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CAnimPacket::encode).decoder(S2CAnimPacket::decode)
                .consumerMainThread(S2CAnimPacket::handle).add();
        CHANNEL.messageBuilder(C2SAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SAbilityPacket::encode).decoder(C2SAbilityPacket::decode)
                .consumerMainThread(C2SAbilityPacket::handle).add();
        CHANNEL.messageBuilder(C2SSealActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSealActionPacket::encode).decoder(C2SSealActionPacket::decode)
                .consumerMainThread(C2SSealActionPacket::handle).add();
        CHANNEL.messageBuilder(C2SDialogOptionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SDialogOptionPacket::encode).decoder(C2SDialogOptionPacket::decode)
                .consumerMainThread(C2SDialogOptionPacket::handle).add();
        CHANNEL.messageBuilder(S2CDialogPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CDialogPacket::encode).decoder(S2CDialogPacket::decode)
                .consumerMainThread(S2CDialogPacket::handle).add();
        CHANNEL.messageBuilder(S2CSealLinePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSealLinePacket::encode).decoder(S2CSealLinePacket::decode)
                .consumerMainThread(S2CSealLinePacket::handle).add();
        CHANNEL.messageBuilder(S2CInnerWorldPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CInnerWorldPacket::encode).decoder(S2CInnerWorldPacket::decode)
                .consumerMainThread(S2CInnerWorldPacket::handle).add();
    }
}
