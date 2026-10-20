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
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CSyncSealPacket(data.save()));
        syncVisual(player);
    }

    /** Envía a todos los que ven al jugador si lleva el manto o la forma de golem. */
    public static void syncVisual(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CVisualPacket(player.getId(), data.manto(), data.golemForm()));
        player.refreshDimensions();
    }

    public static void syncVisualTo(ServerPlayer target, ServerPlayer viewer) {
        SealData data = SealCapability.get(target);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
                new S2CVisualPacket(target.getId(), data.manto(), data.golemForm()));
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
        int level = data.level();
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
            def.abilities().stream()
                    .filter(a -> a.unlockLevel() == level)
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
        if (def == null || amount <= 0) return;

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
        if (def == null) return;
        def.deactivateAll(player, data);
        player.sendSystemMessage(Component.translatable("message.seals.seal_broken", def.name())
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        fx(player, S2CFxPacket.Type.SEAL_BREAK);
        def.onSealBroken(player, data);
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
        for (int i = 0; i < passed; i++) {
            data.setAgeMonths(data.ageMonths() + 1);
            if (data.ageMonths() % 12 == 0) {
                player.sendSystemMessage(Component.translatable("message.seals.birthday", def.name(), data.ageYears())
                        .withStyle(ChatFormatting.GREEN));
                addAffinity(player, data, 70, "affinity.seals.reason.birthday");
            }
        }
        sync(player);
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
