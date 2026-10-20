package com.markadrii.seals.event;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
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
        if (player.tickCount % 20 == 0) {
            SealManager.tickAge(player, data);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad / estabilidad
    // ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

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
        } else if (killer instanceof HolyGolemEntity golem && golem.getOwner() instanceof ServerPlayer owner) {
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
        if (event.getEntity() instanceof ServerPlayer player) SealManager.sync(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SealManager.sync(player);
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SealData data = SealCapability.get(player);
            data.setHolyGolem(null); // la invocación no viaja entre dimensiones
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
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            SealManager.syncVisualTo(target, viewer);
        }
    }
}
