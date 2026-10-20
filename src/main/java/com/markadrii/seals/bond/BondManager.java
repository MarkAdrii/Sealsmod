package com.markadrii.seals.bond;

import com.markadrii.seals.entity.InnerMob;
import com.markadrii.seals.entity.MiniGolemEntity;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.network.S2CDialogPacket;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.network.S2CSealLinePacket;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * El vínculo con el mob sellado: hablar (1 vez al día), visitar su mundo interior (cada 4 días),
 * hablar con él allí, pelear, fortalecer el vínculo (BOUND) y su mini invocación.
 */
public final class BondManager {
    // --- Afinidad ---
    public static final int TALK_AFFINITY = 10;
    public static final int CHAT_AFFINITY = 2;
    public static final int FOOD_AFFINITY = 15;
    public static final int FIGHT_AFFINITY = 20;

    // --- Esperas (en días de Minecraft) ---
    public static final int TALK_DAYS = 1;
    public static final int ENTER_DAYS = 4;
    public static final int BOND_DAYS = 5;

    // --- Niveles necesarios (nivel 2 = 1 estrella) ---
    public static final int ENTER_LEVEL = 2;

    /** Boost del vínculo: 10 minutos. */
    public static final int BOOST_TICKS = 20 * 60 * 10;

    /** Estabilidad que se pierde si muere la mini invocación. */
    public static final int MINI_DEATH_STABILITY = 50;

    private BondManager() {}

    private static long today(ServerPlayer player) {
        return SealManager.currentDay(player.server);
    }

    /** Días que faltan para poder volver a hacer algo (0 = ya se puede). */
    public static long daysLeft(long today, long lastDay, int cooldown) {
        return Math.max(0, lastDay + cooldown - today);
    }

    // ---------------------------------------------------------------------------------------
    // Hablar (botón de la U)
    // ---------------------------------------------------------------------------------------

    public static void talk(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null) return;
        long left = daysLeft(today(player), data.lastTalkDay(), TALK_DAYS);
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.seals.talk.wait").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        data.setLastTalkDay(today(player));

        // Algunos mobs no siempre te contestan (el Elder Guardian, por ejemplo)
        if (player.getRandom().nextFloat() >= def.talkChance(data)) {
            Component ignored = def.ignoreLines() > 0
                    ? Component.translatable(def.textKey() + ".talk.ignore." + player.getRandom().nextInt(def.ignoreLines()))
                    : Component.translatable("message.seals.talk.silent");
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CSealLinePacket(ignored));
            player.sendSystemMessage(Component.literal("[").append(def.name()).append("] ").withStyle(ChatFormatting.DARK_AQUA)
                    .append(ignored.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            SealManager.sync(player);
            return;
        }

        Component line = def.talkLines() > 0
                ? Component.translatable(def.textKey() + ".talk." + player.getRandom().nextInt(def.talkLines()))
                : Component.translatable("message.seals.talk.silent");
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CSealLinePacket(line));
        player.sendSystemMessage(Component.literal("[").append(def.name()).append("] ").withStyle(ChatFormatting.GOLD)
                .append(line.copy().withStyle(ChatFormatting.WHITE)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 0.6F, 1.2F);
        SealManager.addAffinity(player, data, TALK_AFFINITY, "affinity.seals.reason.talk");
        SealManager.sync(player);
    }

    // ---------------------------------------------------------------------------------------
    // Mundo interior
    // ---------------------------------------------------------------------------------------

    public static void enterInner(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        if (data.definition() == null || data.inInner()) return;
        if (data.level() < ENTER_LEVEL) return;
        long left = daysLeft(today(player), data.lastEnterDay(), ENTER_DAYS);
        if (left > 0) {
            player.displayClientMessage(Component.translatable("message.seals.inner.wait", left).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        // La mini invocación se queda fuera
        despawnMini(player, data);
        if (InnerWorld.enter(player, data)) {
            data.setLastEnterDay(today(player));
            SealManager.sync(player);
        }
    }

    public static void openDialog(ServerPlayer player, InnerMob golem) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null || !data.inInner()) return;
        String k = def.textKey() + ".inner.";
        sendDialog(player, def, Component.translatable(k + "greet." + player.getRandom().nextInt(4)), options(data, def), false);
    }

    private static List<Integer> options(SealData data, SealDefinition def) {
        List<Integer> list = new ArrayList<>();
        add(list, data, DialogOption.HOW);
        add(list, data, DialogOption.FEEL);
        add(list, data, DialogOption.FOOD_ASK);
        if (data.visitDone(DialogOption.FOOD_ASK.bit)) add(list, data, DialogOption.FOOD_OFFER);
        if (def.fightLevel() > 0 && data.level() >= def.fightLevel()) add(list, data, DialogOption.FIGHT);
        if (data.level() >= def.bondLevel()) add(list, data, DialogOption.BOND);
        list.add(DialogOption.BREAK.ordinal());
        list.add(DialogOption.EXIT.ordinal());
        list.add(DialogOption.BYE.ordinal());
        return list;
    }

    private static void add(List<Integer> list, SealData data, DialogOption option) {
        if (option.bit == 0 || !data.visitDone(option.bit)) list.add(option.ordinal());
    }

    private static void sendDialog(ServerPlayer player, SealDefinition def, Component line, List<Integer> options, boolean close) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new S2CDialogPacket(def.name(), line, options, close));
    }

    public static void chooseOption(ServerPlayer player, DialogOption option) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null || !data.inInner() || data.fighting()) return;
        InnerMob innerMob = InnerWorld.golemOf(player);
        Mob golem = innerMob == null ? null : innerMob.asMob();
        if (option != DialogOption.EXIT && option != DialogOption.BYE && option != DialogOption.GIVE_KEY
                && (golem == null || golem.distanceToSqr(player) > 12 * 12)) return;
        if (option.bit != 0 && data.visitDone(option.bit)) return;

        String k = def.textKey() + ".inner.";
        var random = player.getRandom();

        switch (option) {
            case HOW, FEEL -> {
                data.markVisit(option.bit);
                String q = option == DialogOption.HOW ? "how" : "feel";
                boolean likes = random.nextFloat() < def.likeChance(data);
                if (likes) {
                    sendDialog(player, def, Component.translatable(k + q + ".yes." + random.nextInt(3)), options(data, def), false);
                    SealManager.addAffinity(player, data, CHAT_AFFINITY, "affinity.seals.reason.chat");
                } else {
                    sendDialog(player, def, Component.translatable(k + q + ".no." + random.nextInt(2)), options(data, def), false);
                }
            }
            case FOOD_ASK -> {
                data.markVisit(option.bit);
                sendDialog(player, def, Component.translatable(k + "food.ask"), options(data, def), false);
            }
            case FOOD_OFFER -> {
                Item food = def.favoriteFood();
                if (food == null || !takeOne(player, food)) {
                    sendDialog(player, def, Component.translatable(k + "food.none"), options(data, def), false);
                    return;
                }
                data.markVisit(option.bit);
                player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 1.0F);
                SealManager.fx(golem, S2CFxPacket.Type.LEVEL_UP);
                sendDialog(player, def, Component.translatable(k + "food.thanks"), options(data, def), false);
                SealManager.addAffinity(player, data, FOOD_AFFINITY, "affinity.seals.reason.food");
            }
            case FIGHT -> {
                if (def.fightLevel() < 0 || data.level() < def.fightLevel() || innerMob == null) return;
                data.markVisit(option.bit);
                data.setFighting(true);
                sendDialog(player, def, Component.translatable(k + "fight.start"), List.of(), true);
                player.sendSystemMessage(Component.literal("[").append(def.name()).append("] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(k + "fight.start").withStyle(ChatFormatting.WHITE)));
                innerMob.startFight(player);
                player.level().playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 1.5F, 0.5F);
                SealManager.sync(player);
            }
            case BOND -> {
                if (data.level() < def.bondLevel()) return;
                long left = daysLeft(today(player), data.lastBondDay(), BOND_DAYS);
                if (left > 0) {
                    sendDialog(player, def, Component.translatable(k + "bond.wait", left), options(data, def), false);
                    return;
                }
                data.markVisit(option.bit);
                data.setLastBondDay(today(player));
                startBoost(player, data);
                sendDialog(player, def, Component.translatable(k + "bond.yes"), options(data, def), false);
            }
            case BREAK -> sendDialog(player, def, Component.translatable(k + "break.ask"),
                    List.of(DialogOption.BREAK_CONFIRM.ordinal(), DialogOption.BYE.ordinal()), false);
            case BREAK_CONFIRM -> peacefulRelease(player, data, def);
            case EXIT -> {
                sendDialog(player, def, Component.translatable(k + "bye"), List.of(), true);
                InnerWorld.exit(player, data);
            }
            case BYE -> {
                // El cliente cierra la ventana; no hace falta nada más
            }
            case GIVE_KEY -> {
                Item key = def.innerKeyItem();
                if (key == null || data.innerKeyGiven()) return;
                if (!takeOne(player, key)) {
                    sendDialog(player, def, Component.translatable(k + "door.nokey"), keyOptions(), false);
                    return;
                }
                data.setInnerKeyGiven(true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
                sendDialog(player, def, Component.translatable(k + "door.accepted"), List.of(), true);
                player.sendSystemMessage(Component.literal("[").append(def.name()).append("] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(k + "door.accepted").withStyle(ChatFormatting.WHITE)));
                InnerWorld.openDoor(player);
            }
        }
        SealManager.sync(player);
    }

    /** La puerta está cerrada: el mob te pide lo que necesita para abrirla. */
    public static void promptKey(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null || !data.inInner() || def.innerKeyItem() == null || data.innerKeyGiven()) return;
        sendDialog(player, def, Component.translatable(def.textKey() + ".inner.door.locked"), keyOptions(), false);
    }

    private static List<Integer> keyOptions() {
        return List.of(DialogOption.GIVE_KEY.ordinal(), DialogOption.EXIT.ordinal(), DialogOption.BYE.ordinal());
    }

    private static boolean takeOne(Player player, Item item) {
        if (player.getAbilities().instabuild) return true;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Fin de la pelea en el mundo interior. */
    public static void endFight(ServerPlayer player, InnerMob golem, boolean playerWon) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null || !data.fighting()) return;
        data.setFighting(false);
        golem.stopFight();
        String k = def.textKey() + ".inner.";
        if (playerWon) {
            int gain = Math.round(FIGHT_AFFINITY * data.ageScale());
            player.level().playSound(null, golem.asMob().blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.7F);
            sendDialog(player, def, Component.translatable(k + "fight.win"), options(data, def), false);
            SealManager.addAffinity(player, data, gain, "affinity.seals.reason.fight");
        } else {
            player.setHealth(Math.max(player.getHealth(), 4.0F));
            sendDialog(player, def, Component.translatable(k + "fight.lose"), options(data, def), false);
        }
        SealManager.sync(player);
    }

    /** BOUND: todas las habilidades sin recarga y efectos de fuerza durante 10 minutos. */
    private static void startBoost(ServerPlayer player, SealData data) {
        data.setBoostTicks(BOOST_TICKS);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 0.8F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.2F);
        SealManager.fx(player, S2CFxPacket.Type.BOND);
        player.sendSystemMessage(Component.translatable("message.seals.bond.start", BOOST_TICKS / 1200)
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /** Se llama cada tick desde el tick del jugador. */
    public static void tickBoost(ServerPlayer player, SealData data) {
        if (data.boostTicks() <= 0) return;
        data.setBoostTicks(data.boostTicks() - 1);
        SealDefinition def = data.definition();
        if (def != null) def.resetCooldowns(data);
        if (data.boostTicks() % 100 == 0) {
            int dur = Math.min(data.boostTicks(), 220);
            if (dur > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, dur, 1, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, dur, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 0, true, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, dur, 0, true, false, true));
            }
        }
        if (data.boostTicks() == 0) {
            player.displayClientMessage(Component.translatable("message.seals.bond.end").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Romper el sello en paz: los dos salís del mundo interior y el mob queda libre y amistoso. */
    private static void peacefulRelease(ServerPlayer player, SealData data, SealDefinition def) {
        sendDialog(player, def, Component.translatable(def.textKey() + ".inner.break.done"), List.of(), true);
        InnerWorld.exit(player, data);
        def.deactivateAll(player, data);
        despawnMini(player, data);
        SealManager.fx(player, S2CFxPacket.Type.RELEASE);
        def.onPeacefulRelease(player, data);
        player.sendSystemMessage(Component.translatable("message.seals.release", def.name()).withStyle(ChatFormatting.AQUA));
        SealManager.onSealLost(player, data);
        data.clearSeal();
        SealManager.sync(player);
    }

    // ---------------------------------------------------------------------------------------
    // Mini invocación
    // ---------------------------------------------------------------------------------------

    public static boolean isActiveMini(Player owner, Entity mini) {
        SealData data = SealCapability.get(owner);
        return data.miniOut() && !data.miniOnShoulder() && mini.getUUID().equals(data.miniId());
    }

    @Nullable
    private static Entity findMini(ServerPlayer player, SealData data) {
        if (data.miniId() == null) return null;
        return player.serverLevel().getEntity(data.miniId());
    }

    /** Botón Invocar / Desinvocar de la U. */
    public static void toggleMini(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        SealDefinition def = data.definition();
        if (def == null || def.miniUnlockLevel() < 0) return;
        if (data.miniOut()) {
            despawnMini(player, data);
            data.setMiniOut(false);
            data.setMiniOnShoulder(false);
            player.displayClientMessage(Component.translatable("message.seals.mini.dismissed").withStyle(ChatFormatting.GRAY), true);
            SealManager.sync(player);
            return;
        }
        if (data.level() < def.miniUnlockLevel() || data.inInner()) return;
        SealManager.anim(player, "touch_ground");
        if (spawnMini(player, data, null)) {
            data.setMiniOut(true);
            data.setMiniOnShoulder(false);
            SealManager.sync(player);
        }
    }

    /** Hace aparecer al mini delante del jugador (o en {@code at}). */
    public static boolean spawnMini(ServerPlayer player, SealData data, @Nullable Vec3 at) {
        SealDefinition def = data.definition();
        if (def == null || InnerWorld.isInside(player)) return false;
        Mob mini = def.createMini(player, data);
        if (mini == null) return false;
        Vec3 pos = at;
        if (pos == null) {
            Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
            pos = player.position().add(look.scale(1.5));
        }
        mini.moveTo(pos.x, player.getY(), pos.z, player.getYRot(), 0);
        player.serverLevel().addFreshEntity(mini);
        data.setMiniId(mini.getUUID());
        SealManager.fx(player.serverLevel(), S2CFxPacket.Type.TOUCH_GROUND, pos.x, player.getY(), pos.z, mini.getId());
        player.level().playSound(null, BlockPos.containing(pos), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    /** Quita al mini del mundo sin penalización (desinvocar, subir al hombro, cambiar de dimensión...). */
    public static void despawnMini(ServerPlayer player, SealData data) {
        Entity mini = findMini(player, data);
        if (mini != null) {
            SealManager.fx(mini, S2CFxPacket.Type.TOUCH_GROUND);
            mini.discard();
        }
        data.setMiniId(null);
    }

    /** Shift + clic derecho sobre el mini: sube al hombro. */
    public static void miniUp(ServerPlayer player, MiniGolemEntity mini) {
        SealData data = SealCapability.get(player);
        if (!isActiveMini(player, mini)) return;
        mini.discard();
        data.setMiniId(null);
        data.setMiniOnShoulder(true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 1.4F);
        SealManager.sync(player);
    }

    /** Shift + clic derecho con la mano vacía: baja del hombro. */
    public static void miniDown(ServerPlayer player, @Nullable Vec3 at) {
        SealData data = SealCapability.get(player);
        if (!data.miniOut() || !data.miniOnShoulder() || !player.getMainHandItem().isEmpty()) return;
        if (spawnMini(player, data, at)) {
            data.setMiniOnShoulder(false);
            SealManager.sync(player);
        }
    }

    /** El mini ha muerto: pierde la mitad de la estabilidad del sello. */
    public static void onMiniDeath(ServerPlayer owner, MiniGolemEntity mini) {
        SealData data = SealCapability.get(owner);
        if (!mini.getUUID().equals(data.miniId())) return;
        data.setMiniId(null);
        data.setMiniOut(false);
        data.setMiniOnShoulder(false);
        SealManager.damageStability(owner, data, MINI_DEATH_STABILITY, "stability.seals.reason.mini");
        SealManager.sync(owner);
    }

    /** Tras cambiar de dimensión o volver a entrar: si el mini estaba fuera, reaparece con el jugador. */
    public static void respawnMiniIfNeeded(ServerPlayer player) {
        SealData data = SealCapability.get(player);
        if (!data.miniOut() || data.miniOnShoulder() || InnerWorld.isInside(player)) return;
        if (findMini(player, data) instanceof MiniGolemEntity existing && existing.isAlive()) return;
        spawnMini(player, data, null);
    }

    @SuppressWarnings("unused")
    private static ServerLevel levelOf(ServerPlayer p) {
        return p.serverLevel();
    }
}
