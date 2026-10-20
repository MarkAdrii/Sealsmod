package com.markadrii.seals.event;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.entity.InnerGolemEntity;
import com.markadrii.seals.entity.MiniGolemEntity;
import com.markadrii.seals.entity.OwnedGolemEntity;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.OldestOneData;
import com.markadrii.seals.seal.SealVisuals;
import com.markadrii.seals.seal.irongolem.IronGolemSeal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Conecta los eventos del juego con el mob sellado de cada jugador. */
@Mod.EventBusSubscriber(modid = Seals.MOD_ID)
public final class SealEvents {

    private SealEvents() {}

    // ---------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null) return;

        def.tick(player, data);
        BondManager.tickBoost(player, data);
        if (player.tickCount % 20 == 0) {
            SealManager.tickAge(player, data);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) InnerWorld.tick(event.getServer());
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad / estabilidad
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        // Si muere la mini invocación, el sello pierde estabilidad
        if (victim instanceof MiniGolemEntity mini && mini.getOwner() instanceof ServerPlayer owner) {
            BondManager.onMiniDeath(owner, mini);
            return;
        }

        // Si muere quien lleva el sello, se apaga todo
        if (victim instanceof ServerPlayer dead) {
            SealData data = SealCapability.get(dead);
            if (data.definition() != null) data.definition().deactivateAll(dead, data);
        }

        Entity killer = event.getSource().getEntity();
        ServerPlayer player = null;
        boolean byPlayer = false;
        if (killer instanceof ServerPlayer p) {
            player = p;
            byPlayer = true;
        } else if (killer instanceof OwnedGolemEntity golem && golem.getOwner() instanceof ServerPlayer owner) {
            player = owner;
        }
        if (player == null || player == victim) return;

        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null) def.onKill(player, data, victim, byPlayer);
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null) def.onTrade(player, data, event.getAbstractVillager());
    }

    // ---------------------------------------------------------------------------------------
    // Habilidades pasivas / de combate
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null && def.onFall(player, data, event.getDistance())) {
            event.setCanceled(true);
        }
    }

    /** En la pelea del mundo interior el jugador nunca muere: si iba a morir, pierde la pelea. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        if (!data.fighting() || !InnerWorld.isInside(player)) return;
        if (event.getAmount() >= player.getHealth() - 1.0F) {
            event.setCanceled(true);
            InnerGolemEntity golem = InnerWorld.golemOf(player);
            if (golem != null) BondManager.endFight(player, golem, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null) {
            event.setAmount(def.onMeleeHit(player, data, event.getEntity(), event.getAmount()));
        }
    }

    // ---------------------------------------------------------------------------------------
    // Mini golem: shift + clic derecho para subirlo o bajarlo del hombro
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof MiniGolemEntity mini) || !event.getEntity().isShiftKeyDown()) return;
        if (!mini.isOwnedBy(event.getEntity()) || !event.getEntity().getMainHandItem().isEmpty()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (event.getEntity() instanceof ServerPlayer player) BondManager.miniUp(player, mini);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player p = event.getEntity();
        if (!p.isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND || !p.getMainHandItem().isEmpty()) return;
        if (!SealVisuals.shoulder(p)) return;
        if (p instanceof ServerPlayer player) {
            Vec3 at = Vec3.atBottomCenterOf(event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace()));
            BondManager.miniDown(player, at);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ---------------------------------------------------------------------------------------
    // Mundo interior: no se puede romper ni poner nada
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() != null && InnerWorld.isInside(event.getPlayer()) && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && InnerWorld.isInside(p) && !p.isCreative()) {
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Tamaño del jugador en "Yo soy el golem"
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onSize(EntityEvent.Size event) {
        if (!(event.getEntity() instanceof Player player)) return;
        try {
            if (!SealVisuals.golemForm(player)) return;
        } catch (Exception e) {
            return; // el jugador aún se está creando
        }
        event.setNewSize(event.getNewSize().scale(IronGolemSeal.FORM_SCALE));
        event.setNewEyeHeight(event.getNewEyeHeight() * IronGolemSeal.FORM_SCALE);
    }

    // ---------------------------------------------------------------------------------------
    // Sincronización
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Si el título de Oldest One se reseteó mientras estaba desconectado, se le quita
            // El título de Oldest One manda el mundo: se quita si se reseteó y se da si lo heredó desconectado
            SealData data = SealCapability.get(player);
            boolean holder = player.getUUID().equals(OldestOneData.get(player.server).holder());
            data.setOldestOne(holder && data.hasSeal());
            // Si salió del juego dentro del mundo interior, vuelve a donde estaba
            if (data.inInner()) InnerWorld.exit(player, data);
            BondManager.respawnMiniIfNeeded(player);
            SealManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SealData data = SealCapability.get(player);
            if (data.inInner()) {
                InnerWorld.cleanup(player.server, player.getUUID());
                data.setInInner(false);
                data.setFighting(false);
            }
            BondManager.respawnMiniIfNeeded(player);
            SealManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SealData data = SealCapability.get(player);
            data.setHolyGolem(null); // la invocación no viaja entre dimensiones
            // Salió del mundo interior por otro medio (comando, portal...)
            if (event.getFrom() == InnerWorld.KEY && data.inInner()) {
                InnerWorld.cleanup(player.server, player.getUUID());
                data.setInInner(false);
                data.setFighting(false);
            }
            // El mini se queda donde estaba y reaparece con el jugador (salvo en el mundo interior)
            data.setMiniId(null);
            BondManager.respawnMiniIfNeeded(player);
            SealManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SealData data = SealCapability.get(player);
            if (data.holyGolem() != null && player.serverLevel().getEntity(data.holyGolem()) instanceof HolyGolemEntity golem) {
                golem.vanish();
            }
            data.setHolyGolem(null);
            // El mini desaparece al salir (volverá al entrar) y la visita al mundo interior termina
            BondManager.despawnMini(player, data);
            InnerWorld.cleanup(player.server, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            SealManager.syncVisualTo(target, viewer);
        }
    }
}
