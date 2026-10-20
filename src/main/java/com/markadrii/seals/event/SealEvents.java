package com.markadrii.seals.event;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.entity.InnerMob;
import com.markadrii.seals.item.SealingScrollItem;
import com.markadrii.seals.entity.InnerElderEntity;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.network.S2CScreenEffectPacket;
import com.markadrii.seals.registry.ModBlocks;
import com.markadrii.seals.seal.elder.ElderGuardianSeal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import com.markadrii.seals.seal.SealRegistry;
import net.minecraft.world.item.ItemStack;
import com.markadrii.seals.entity.MiniGolemEntity;
import com.markadrii.seals.entity.OwnedGolemEntity;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
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

    /**
     * Elder Guardian: se sella dejando que te mate mientras llevas el pergamino en la mano secundaria.
     * En ese momento no mueres: recuperas toda la vida y queda sellado dentro de ti.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onElderSeal(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getSource().getEntity() instanceof ElderGuardian elder) || elder instanceof InnerElderEntity) return;
        ItemStack offhand = player.getOffhandItem();
        if (!(offhand.getItem() instanceof SealingScrollItem)) return;
        SealData data = SealCapability.get(player);
        if (data.hasSeal()) return;

        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.setAirSupply(player.getMaxAirSupply());
        if (!player.getAbilities().instabuild) offhand.shrink(1);

        ElderGuardianSeal def = SealRegistry.ELDER_GUARDIAN;
        int age = def.minAge() + player.getRandom().nextInt(def.maxAge() - def.minAge() + 1);
        data.seal(def.id(), age, SealManager.currentDay(player.server));

        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            SealManager.fx(level, S2CFxPacket.Type.SEAL, elder.getX(), elder.getY() + elder.getBbHeight() / 2,
                    elder.getZ(), player.getId());
            level.playSound(null, elder.blockPosition(), SoundEvents.ELDER_GUARDIAN_DEATH, SoundSource.HOSTILE, 1.5F, 0.6F);
            level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        elder.discard();
        SealManager.screenEffect(player, S2CScreenEffectPacket.Effect.SEAL);
        player.sendSystemMessage(Component.translatable("message.seals.sealed", def.name(), age).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("message.seals.elder.sealed").withStyle(ChatFormatting.AQUA));
        SealManager.sync(player);
    }

    /** Hijo del mar: las criaturas marinas no te eligen como objetivo... */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player p && ElderGuardianSeal.isMarine(event.getEntity())
                && ElderGuardianSeal.carries(p)) {
            event.setCanceled(true);
        }
    }

    /** ...ni te hacen daño (pinchos, pez globo, rayos). */
    @SubscribeEvent
    public static void onMarineAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getSource().getEntity() != null
                && ElderGuardianSeal.isMarine(event.getSource().getEntity()) && ElderGuardianSeal.carries(p)) {
            event.setCanceled(true);
        }
    }

    /** Daño recibido: cada mob sellado puede reaccionar (por ejemplo, las espinas de la forma abisal). */
    @SubscribeEvent
    public static void onSealedPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null) event.setAmount(def.onPlayerHurt(player, data, event.getSource(), event.getAmount()));
    }

    /** Mundo interior: clic derecho en la puerta cerrada para que el mob te pida la llave. */
    @SubscribeEvent
    public static void onDoorClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !InnerWorld.isInside(player)) return;
        var block = player.level().getBlockState(event.getPos()).getBlock();
        if (block == ModBlocks.SPIRIT_DOOR.get() || block == ModBlocks.SPIRIT_LATTICE.get() || block == ModBlocks.SPIRIT_FRAME.get()) {
            BondManager.promptKey(player);
        }
    }

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
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def != null) def.onEat(player, data, event.getItem());
    }

    /** Guarda el día en que se domesticó un animal (el lobo necesita 2 días contigo para sellarlo). */
    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            event.getAnimal().getPersistentData().putLong(com.markadrii.seals.seal.wolf.WolfSeal.TAMED_DAY_TAG,
                    SealManager.currentDay(player.server));
        }
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
            InnerMob golem = InnerWorld.golemOf(player);
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

    /**
     * El pergamino tiene prioridad sobre la interacción normal del mob
     * (por ejemplo, tu lobo se sentaría en vez de dejarse sellar).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScrollInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SealingScrollItem scroll)) return;
        if (!(event.getTarget() instanceof LivingEntity target) || SealRegistry.forEntity(target.getType()) == null) return;
        InteractionResult result = scroll.interactLivingEntity(stack, event.getEntity(), target, event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

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
