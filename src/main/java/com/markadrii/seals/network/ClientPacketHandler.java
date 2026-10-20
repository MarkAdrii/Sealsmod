package com.markadrii.seals.network;

import com.markadrii.seals.client.ClientSealData;
import com.markadrii.seals.client.anim.SealAnimations;
import com.markadrii.seals.client.fx.SealFx;
import com.markadrii.seals.seal.SealVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;

/** Manejo de paquetes en el cliente (esta clase solo se carga en el cliente). */
public final class ClientPacketHandler {
    private ClientPacketHandler() {}

    public static void syncSeal(S2CSyncSealPacket msg) {
        ClientSealData.get().load(msg.tag());
    }

    public static void visual(S2CVisualPacket msg) {
        SealVisuals.setClient(msg.entityId(), msg.flags());
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId());
            if (entity != null) entity.refreshDimensions();
        }
    }

    public static void dialog(S2CDialogPacket msg) {
        com.markadrii.seals.client.gui.InnerDialogScreen.receive(msg);
    }

    public static void sealLine(S2CSealLinePacket msg) {
        ClientSealData.setLastLine(msg.line());
    }

    public static void innerWorld(S2CInnerWorldPacket msg) {
        ClientSealData.setInner(msg.inside(), msg.doorX(), msg.doorY(), msg.doorZ());
        if (!msg.inside() && Minecraft.getInstance().screen instanceof com.markadrii.seals.client.gui.InnerDialogScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    public static void fx(S2CFxPacket msg) {
        SealFx.play(msg);
    }

    public static void anim(S2CAnimPacket msg) {
        if (Minecraft.getInstance().level == null) return;
        if (Minecraft.getInstance().level.getEntity(msg.entityId()) instanceof AbstractClientPlayer player) {
            SealAnimations.play(player, msg.animation());
        }
    }
}
