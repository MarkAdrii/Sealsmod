package com.markadrii.seals.client;

import com.markadrii.seals.Seals;
import com.markadrii.seals.client.fx.SealFx;
import com.markadrii.seals.client.gui.SealScreen;
import com.markadrii.seals.network.C2SAbilityPacket;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.seal.SealVisuals;
import com.markadrii.seals.seal.irongolem.IronGolemSeal;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Teclas, modo combate, escala del jugador en forma de golem y efectos continuos. */
@Mod.EventBusSubscriber(modid = Seals.MOD_ID, value = Dist.CLIENT)
public final class ClientForgeEvents {
    private static boolean scaledPlayer;

    private ClientForgeEvents() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (event.phase == TickEvent.Phase.START) {
            handleKeys(mc);
        } else {
            // Destellos de los jugadores con el manto puesto
            for (Player p : mc.level.players()) {
                if (SealVisuals.manto(p) && !p.isInvisible()) SealFx.mantoAmbient(p);
            }
        }
    }

    private static void handleKeys(Minecraft mc) {
        boolean hasSeal = ClientSealData.get().hasSeal();

        while (KeyBindings.OPEN_SEAL.consumeClick()) {
            if (mc.screen != null) continue;
            if (hasSeal) {
                mc.setScreen(new SealScreen());
            } else {
                mc.player.displayClientMessage(Component.translatable("message.seals.no_seal").withStyle(ChatFormatting.GRAY), true);
            }
        }

        while (KeyBindings.COMBAT_MODE.consumeClick()) {
            if (!hasSeal) {
                mc.player.displayClientMessage(Component.translatable("message.seals.no_seal").withStyle(ChatFormatting.GRAY), true);
                continue;
            }
            boolean on = !ClientSealData.combatMode();
            ClientSealData.setCombatMode(on);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(on ? SoundEvents.ARMOR_EQUIP_IRON : SoundEvents.ARMOR_EQUIP_CHAIN, 1.0F));
        }

        if (!hasSeal) ClientSealData.setCombatMode(false);

        // En modo combate, las teclas 1-9 lanzan habilidades en lugar de cambiar de hueco
        if (ClientSealData.combatMode() && mc.screen == null) {
            for (int i = 0; i < 9; i++) {
                while (mc.options.keyHotbarSlots[i].consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new C2SAbilityPacket(i));
                }
            }
        }
    }

    /** En modo combate se oculta la hotbar normal (la sustituye la de habilidades). */
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!ClientSealData.combatMode()) return;
        var id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.HOTBAR.id()) || id.equals(VanillaGuiOverlay.ITEM_NAME.id())) {
            event.setCanceled(true);
        }
    }

    // --- "Yo soy el golem": el jugador se dibuja más grande ------------------------------

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        scaledPlayer = SealVisuals.golemForm(event.getEntity());
        if (scaledPlayer) {
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            float s = IronGolemSeal.FORM_SCALE;
            pose.scale(s, s, s);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (scaledPlayer) {
            event.getPoseStack().popPose();
            scaledPlayer = false;
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientSealData.reset();
        SealVisuals.clearClient();
    }
}
