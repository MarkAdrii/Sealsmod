package com.markadrii.seals.client;

import com.markadrii.seals.Seals;
import com.markadrii.seals.client.fx.SealFx;
import com.markadrii.seals.client.gui.SealScreen;
import com.markadrii.seals.client.gui.SealScreenEffect;
import com.markadrii.seals.client.render.WerewolfRenderer;
import com.markadrii.seals.seal.wolf.WolfSeal;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.HashSet;
import java.util.Set;
import com.markadrii.seals.network.C2SAbilityPacket;
import com.markadrii.seals.network.C2SSealActionPacket;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
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
            tickInstinct(mc);
            com.markadrii.seals.client.fx.BeamFx.tick();
            // Estela roja de los ojos
            for (Player p : mc.level.players()) {
                if (!SealVisuals.eyes(p) || p.isInvisible()) continue;
                if (p == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
                SealFx.eyeTrail(p);
            }
            // Mundo interior: niebla dorada y ondas al caminar
            if (mc.level.dimension() == InnerWorld.KEY && ClientSealData.inInner()) {
                SealFx.innerAmbient(mc.level, ClientSealData.innerDoor(), mc.player);
                boolean walking = mc.player.onGround() && mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.002;
                if (walking && mc.player.tickCount % 6 == 0) SealFx.footRipple(mc.player);
            }
        }
    }

    // --- Instinto primal: sientes a las criaturas cercanas ------------------------------

    private static final Set<Integer> SENSED = new HashSet<>();

    private static void tickInstinct(Minecraft mc) {
        boolean on = ClientSealData.get().hasSeal() && ClientSealData.get().instinct();
        Set<Integer> now = new HashSet<>();
        if (on) {
            double r = WolfSeal.INSTINCT_RANGE;
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player || !(e instanceof LivingEntity) || !e.isAlive()) continue;
                if (e.distanceToSqr(mc.player) > r * r) continue;
                now.add(e.getId());
                EntityFlagsAccess.setLocalGlow(e, true);
            }
        }
        for (int id : SENSED) {
            if (now.contains(id)) continue;
            Entity e = mc.level.getEntity(id);
            if (e != null) EntityFlagsAccess.setLocalGlow(e, false);
        }
        SENSED.clear();
        SENSED.addAll(now);
    }

    /** Con el instinto activo ves el nombre de las criaturas que sientes. */
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (SENSED.contains(event.getEntity().getId())) {
            event.setResult(Event.Result.ALLOW);
        }
    }

    // --- Mundo interior: todo negro con niebla ------------------------------------------

    /** Niebla: negra con un toque dorado, para que lo lejano se pierda en una bruma cálida. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (event.getCamera().getEntity().level().dimension() == InnerWorld.KEY) {
            event.setRed(0.075F);
            event.setGreen(0.052F);
            event.setBlue(0.018F);
        }
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        if (event.getCamera().getEntity().level().dimension() == InnerWorld.KEY) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(64.0F);
            event.setCanceled(true);
        }
    }

    /** Temblor de cámara durante la animación demoníaca. */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        float shake = SealScreenEffect.shake((float) event.getPartialTick());
        if (shake <= 0) return;
        float t = (float) (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() + event.getPartialTick() : 0);
        event.setRoll(event.getRoll() + (float) Math.sin(t * 2.7) * 4F * shake);
        event.setPitch(event.getPitch() + (float) Math.cos(t * 3.3) * 2F * shake);
        event.setYaw(event.getYaw() + (float) Math.sin(t * 2.1) * 2F * shake);
    }

    // --- Mini golem: shift + clic derecho en el aire con la mano vacía lo baja del hombro ---

    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player p = event.getEntity();
        if (event.getHand() == InteractionHand.MAIN_HAND && p.isShiftKeyDown() && p.getMainHandItem().isEmpty()
                && SealVisuals.shoulder(p)) {
            ModNetwork.CHANNEL.sendToServer(new C2SSealActionPacket(C2SSealActionPacket.Action.MINI_DOWN));
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

    // --- Hombre lobo: se dibuja con su propio modelo ------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderWerewolf(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || !SealVisuals.werewolf(player)) return;
        event.setCanceled(true);
        float yaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());
        WerewolfRenderer.get().render(player, yaw, event.getPartialTick(), event.getPoseStack(),
                event.getMultiBufferSource(), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        if (!SealVisuals.werewolf(event.getPlayer())) return;
        event.setCanceled(true);
        WerewolfRenderer.get().renderArm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(),
                event.getPlayer(), event.getArm());
    }

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
        SENSED.clear();
        com.markadrii.seals.client.fx.BeamFx.clear();
    }
}
