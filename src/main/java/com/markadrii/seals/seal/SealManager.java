package com.markadrii.seals.seal;

import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.network.S2CAnimPacket;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.network.S2CSyncSealPacket;
import com.markadrii.seals.network.S2CVisualPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

/** Lógica común de todos los sellos: afinidad, estabilidad, edad y sincronización. */
public final class SealManager {
    /** Ticks de habilidad en uso necesarios para +1 de afinidad (60 segundos). */
    public static final int ABILITY_AFFINITY_TICKS = 20 * 60;

    private SealManager() {}

    // ---------------------------------------------------------------------------------------
    // Sincronización
    // ---------------------------------------------------------------------------------------

    public static void sync(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        updateRecord(player, data);
        data.setToday(currentDay(player.server));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CSyncSealPacket(data.save()));
        syncVisual(player);
    }

    /** Envía a todos los que ven al jugador si lleva el manto o la forma de golem. */
    public static void syncVisual(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CVisualPacket(player.getId(), SealVisuals.flags(data)));
        player.refreshDimensions();
    }

    public static void syncVisualTo(ServerPlayer target, ServerPlayer viewer) {
        SealData data = SealCapability.get(target);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new S2CVisualPacket(target.getId(), SealVisuals.flags(data)));
    }

    public static void fx(ServerLevel level, S2CFxPacket.Type type, double x, double y, double z, int entityId) {
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() ->
                        new PacketDistributor.TargetPoint(x, y, z, 64, level.dimension())),
                new S2CFxPacket(type, x, y, z, entityId));
    }

    public static void fx(Entity entity, S2CFxPacket.Type type) {
        if (entity.level() instanceof ServerLevel level) {
            fx(level, type, entity.getX(), entity.getY(), entity.getZ(), entity.getId());
        }
    }

    /** Animación demoníaca a pantalla completa (solo la ve ese jugador). */
    public static void screenEffect(ServerPlayer player, com.markadrii.seals.network.S2CScreenEffectPacket.Effect effect) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.markadrii.seals.network.S2CScreenEffectPacket(effect));
    }

    public static void anim(ServerPlayer player, String animation) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CAnimPacket(player.getId(), animation));
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad
    // ---------------------------------------------------------------------------------------

    /**
     * Suma afinidad. Cada 100 puntos sube un nivel de afecto (una estrella) hasta el máximo del mob.
     *
     * @param reason clave de traducción del motivo (se muestra en la barra de acción) o null
     */
    public static void addAffinity(ServerPlayer player, SealData data, int amount, String reason) {
        SealDefinition def = data.definition();
        if (def == null || amount <= 0) return;

        int max = def.maxLevel();
        if (data.level() >= max && data.affinity() >= 100) return; // ya está al máximo

        int affinity = data.affinity() + amount;
        final int oldLevel = data.level();
        int level = oldLevel;
        boolean leveled = false;
        while (affinity >= 100 && level < max) {
            affinity -= 100;
            level++;
            leveled = true;
        }
        if (level >= max) affinity = Math.min(affinity, 100);

        data.setAffinity(affinity);
        data.setLevel(level);

        if (reason != null) {
            player.displayClientMessage(Component.translatable("message.seals.affinity_gain", amount,
                    Component.translatable(reason)).withStyle(ChatFormatting.GOLD), true);
        }
        if (leveled) {
            int stars = level - 1;
            player.sendSystemMessage(Component.translatable("message.seals.level_up", def.name(), stars)
                    .withStyle(ChatFormatting.YELLOW));
            final int newLevel = level;
            def.abilities().stream()
                    .filter(a -> a.unlockLevel() > oldLevel && a.unlockLevel() <= newLevel)
                    .forEach(a -> player.sendSystemMessage(Component.translatable("message.seals.ability_unlocked",
                            a.name()).withStyle(ChatFormatting.AQUA)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            fx(player, S2CFxPacket.Type.LEVEL_UP);
        }
        sync(player);
    }

    // ---------------------------------------------------------------------------------------
    // Estabilidad
    // ---------------------------------------------------------------------------------------

    public static void damageStability(ServerPlayer player, SealData data, int amount, String reason) {
        SealDefinition def = data.definition();
        if (def == null || amount <= 0 || def.stabilityLocked()) return;

        data.setStability(data.stability() - amount);
        player.displayClientMessage(Component.translatable("message.seals.stability_loss", amount,
                Component.translatable(reason)).withStyle(ChatFormatting.RED), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);

        if (data.stability() <= 0) {
            breakSeal(player, data);
        } else {
            if (data.stability() <= 30) {
                player.sendSystemMessage(Component.translatable("message.seals.stability_warning", def.name())
                        .withStyle(ChatFormatting.DARK_RED));
            }
            sync(player);
        }
    }

    public static void breakSeal(ServerPlayer player, SealData data) {
        SealDefinition def = data.definition();
        if (def == null || def.stabilityLocked()) return;
        if (data.inInner()) com.markadrii.seals.world.InnerWorld.exit(player, data);
        com.markadrii.seals.bond.BondManager.despawnMini(player, data);
        def.deactivateAll(player, data);
        player.sendSystemMessage(Component.translatable("message.seals.seal_broken", def.name())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        fx(player, S2CFxPacket.Type.SEAL_BREAK);
        screenEffect(player, com.markadrii.seals.network.S2CScreenEffectPacket.Effect.BREAK);
        def.onSealBroken(player, data);
        onSealLost(player, data);
        data.clearSeal();
        sync(player);
    }

    // ---------------------------------------------------------------------------------------
    // Edad: cada día de Minecraft = 1 mes, 12 días = 1 año
    // ---------------------------------------------------------------------------------------

    public static long currentDay(MinecraftServer server) {
        return server.overworld().getDayTime() / 24000L;
    }

    public static void tickAge(ServerPlayer player, SealData data) {
        SealDefinition def = data.definition();
        if (def == null) return;

        long today = currentDay(player.server);
        if (data.lastDay() < 0 || today < data.lastDay()) {
            data.setLastDay(today);
            return;
        }
        if (today == data.lastDay()) return;

        int passed = (int) Math.min(today - data.lastDay(), 12 * 50); // tope por si alguien usa /time
        data.setLastDay(today);
        addMonths(player, data, passed);
    }

    /**
     * Envejece al mob sellado mes a mes (máximo 1000 años).
     * Cada año cumplido da +70 de afinidad y avisa si cambia de etapa (Joven, Adulto, Anciano...).
     */
    public static void addMonths(ServerPlayer player, SealData data, int months) {
        SealDefinition def = data.definition();
        if (def == null || months <= 0) return;
        int maxMonths = AgeStage.MAX_YEARS * 12;
        for (int i = 0; i < months && data.ageMonths() < maxMonths; i++) {
            AgeStage before = data.ageStage();
            data.setAgeMonths(data.ageMonths() + 1);
            if (data.ageMonths() % 12 == 0) {
                player.sendSystemMessage(Component.translatable("message.seals.birthday", def.name(), data.ageYears())
                        .withStyle(ChatFormatting.GREEN));
                addAffinity(player, data, 70, "affinity.seals.reason.birthday");
                checkOldestOne(player, data);
                AgeStage after = data.ageStage();
                if (after != before && after != AgeStage.OLDEST_ONE) {
                    player.sendSystemMessage(Component.translatable("message.seals.age_stage", def.name(),
                            after.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                            SoundSource.PLAYERS, 1.0F, 0.8F);
                    fx(player, S2CFxPacket.Type.LEVEL_UP);
                }
            }
        }
        sync(player);
    }

    /**
     * Rango oculto: el primer jugador del mundo cuyo mob sellado llegue a 800 años se convierte en el Oldest One.
     * Se anuncia a todo el servidor. Nadie más puede conseguirlo después.
     */
    public static void checkOldestOne(ServerPlayer player, SealData data) {
        SealDefinition def = data.definition();
        if (def == null || data.oldestOne() || data.ageYears() < AgeStage.OLDEST_ONE_YEARS) return;
        OldestOneData world = OldestOneData.get(player.server);
        if (world.isTaken()) {
            // Por si el título se reseteó y este jugador lo tenía: solo vale el del mundo
            if (player.getUUID().equals(world.holder())) data.setOldestOne(true);
            return;
        }
        world.claim(player.getUUID(), player.getGameProfile().getName());
        data.setOldestOne(true);

        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.seals.oldest_one",
                player.getDisplayName(), def.name(), AgeStage.OLDEST_ONE.displayName())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.6F, 0.6F);
        }
        fx(player, S2CFxPacket.Type.LEVEL_UP);
        fx(player, S2CFxPacket.Type.SEAL);
        sync(player);
    }

    /** Guarda en el mundo la edad del mob sellado de este jugador (para heredar el título). */
    public static void updateRecord(ServerPlayer player, SealData data) {
        OldestOneData world = OldestOneData.get(player.server);
        if (data.hasSeal()) {
            world.setRecord(player.getUUID(), player.getGameProfile().getName(), data.ageMonths());
        } else {
            world.removeRecord(player.getUUID());
        }
    }

    /**
     * El jugador pierde su mob sellado (sello roto, quitado o sustituido).
     * Llamar ANTES de {@link SealData#clearSeal()}. Si era el Oldest One, el título pasa al siguiente.
     */
    public static void onSealLost(ServerPlayer player, SealData data) {
        OldestOneData world = OldestOneData.get(player.server);
        world.removeRecord(player.getUUID());
        boolean wasHolder = data.oldestOne() || player.getUUID().equals(world.holder());
        data.setOldestOne(false);
        if (wasHolder) transferOldestOne(player.server, player.getUUID(), player.getDisplayName());
    }

    /**
     * Pasa el título de Oldest One al jugador con el mob sellado más antiguo (800 años o más).
     * Si nadie llega a 800, el título queda libre para el siguiente que llegue.
     */
    public static void transferOldestOne(MinecraftServer server, UUID formerHolder, Component formerName) {
        OldestOneData world = OldestOneData.get(server);
        // Datos al día de los jugadores conectados
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.getUUID().equals(formerHolder)) updateRecord(p, SealCapability.get(p));
        }

        var heir = world.oldest(AgeStage.OLDEST_ONE_YEARS * 12, formerHolder);
        if (heir == null) {
            world.reset();
            server.getPlayerList().broadcastSystemMessage(Component.translatable("message.seals.oldest_one.lost",
                    formerName, AgeStage.OLDEST_ONE.displayName()).withStyle(ChatFormatting.DARK_PURPLE), false);
            return;
        }

        world.claim(heir.getKey(), heir.getValue().name());
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.seals.oldest_one.inherited",
                heir.getValue().name(), AgeStage.OLDEST_ONE.displayName(), formerName)
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), false);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 0.5F, 0.8F);
        }

        // Si el heredero está conectado se le da ya; si no, al entrar
        ServerPlayer online = server.getPlayerList().getPlayer(heir.getKey());
        if (online != null) {
            SealCapability.get(online).setOldestOne(true);
            fx(online, S2CFxPacket.Type.LEVEL_UP);
            sync(online);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------

    /** Pone (o quita si amount = 0) un modificador temporal de atributo sin duplicarlo. */
    public static void setModifier(Player player, Attribute attribute, UUID id, String name, double amount,
                                   AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attribute);
        if (inst == null) return;
        AttributeModifier current = inst.getModifier(id);
        if (amount == 0) {
            if (current != null) inst.removeModifier(id);
            return;
        }
        if (current != null) {
            if (current.getAmount() == amount && current.getOperation() == op) return;
            inst.removeModifier(id);
        }
        inst.addTransientModifier(new AttributeModifier(id, name, amount, op));
    }
}
