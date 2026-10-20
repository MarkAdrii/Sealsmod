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
        SealVisuals.setClient(msg.entityId(), msg.manto(), msg.golemForm());
        if (Minecraft.getInstance().level != null) {
            Entity entity = Minecraft.getInstance().level.getEntity(msg.entityId());
            if (entity != null) entity.refreshDimensions();
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
