package com.markadrii.seals.seal.cat;

import com.markadrii.seals.Seals;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.registry.ModEntities;
import com.markadrii.seals.registry.ModTags;
import com.markadrii.seals.seal.SealAbility;
import com.markadrii.seals.seal.SealCapability;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.SealTheme;
import com.markadrii.seals.seal.SealVisuals;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Gato sellado. Igual que el lobo: solo tu gato, tras 2 días domesticado, y su sello solo se rompe en paz.
 * <ul>
 *   <li>Nivel 1: Ojos felinos (ves a los enemigos resaltados con su nombre, visión nocturna, ojos brillantes)
 *       y Siete vidas (nunca te haces daño al caer)</li>
 *   <li>Nivel 2 (1 estrella, la única): Forma de gato negro (muchísima velocidad, saltos altos, trepar 5 bloques),
 *       mundo interior y BOUND</li>
 * </ul>
 * Reutiliza los campos del lobo en SealData: {@code instinct} = Ojos felinos, {@code werewolf} = Forma de gato.
 */
public class CatSeal extends SealDefinition {
    public static final int TAMED_DAYS_NEEDED = 2;

    // --- Valores base (se multiplican por la edad) ---
    public static final double SIGHT_RANGE = 32.0;
    public static final double FORM_SPEED = 0.70;       // +70% de velocidad
    public static final double JUMP_BOOST = 0.30;       // impulso extra al saltar (≈ salto III)
    public static final int CLIMB_BLOCKS = 5;
    public static final int FORM_DURATION = 20 * 120;   // 2 min
    public static final int FORM_COOLDOWN = 20 * 30;

    public static final int SLOT_SIGHT = 0;
    public static final int SLOT_LIVES = 1;
    public static final int SLOT_FORM = 2;

    private static final UUID FORM_SPEED_ID = UUID.fromString("3c9e2b1d-7a4f-4e6b-8d2c-1f0a9b8c7d01");

    private final List<SealAbility> abilities = List.of(
            new SealAbility("ojos_felinos", 1, SealAbility.Kind.TOGGLE),
            new SealAbility("siete_vidas", 1, SealAbility.Kind.PASSIVE),
            new SealAbility("forma_felina", 2, SealAbility.Kind.TIMED)
    );

    private final SealTheme theme = new SealTheme(
            0xFF17141C,  // fondo: noche
            0xFF0C0A10,  // cabecera
            0xFFE9E4F0,  // texto cabecera
            0xF0A49CB0,  // panel: gris lavanda
            0xFFD3CCDD,  // panel claro
            0xFF3B3446,  // bordes
            0xFF1C1A20,  // texto
            0xFF4D4757,  // texto suave
            0xFF6C8F2F,  // acento: verde de ojos de gato
            0xFFB7D63A,  // afinidad
            0xFF5E5A9E,  // estabilidad
            0xFF8A8296,  // edad
            0xFFF2C744,  // estrella encendida
            0xFF5A5662,  // estrella apagada
            Seals.id("textures/gui/seal/cat_emblem.png"),
            Seals.id("textures/gui/seal/cat_glyphs.png")
    );

    public CatSeal() {
        super(new ResourceLocation("minecraft", "cat"));
    }

    @Override public EntityType<?> entityType() { return EntityType.CAT; }
    @Override public int maxLevel() { return 2; }
    @Override public int minAge() { return 1; }
    @Override public int maxAge() { return 15; }
    @Override public List<SealAbility> abilities() { return abilities; }
    @Override public SealTheme theme() { return theme; }

    /** ¿Lleva este jugador al gato sellado? */
    public static boolean carries(Player player) {
        return SealCapability.get(player).definition() instanceof CatSeal;
    }

    /** ¿Está en forma de gato? (cliente y servidor) */
    public static boolean inForm(Player player) {
        return SealVisuals.catForm(player);
    }

    // ---------------------------------------------------------------------------------------
    // Sellado: tu gato, 2 días domesticado (igual que el lobo)
    // ---------------------------------------------------------------------------------------

    @Override
    public Component sealProblem(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Cat cat) || !cat.isTame() || !cat.isOwnedBy(player)) {
            return Component.translatable("message.seals.cat.not_yours");
        }
        long today = SealManager.currentDay(player.server);
        var tag = cat.getPersistentData();
        String key = com.markadrii.seals.seal.wolf.WolfSeal.TAMED_DAY_TAG;
        if (!tag.contains(key)) tag.putLong(key, today);
        long left = tag.getLong(key) + TAMED_DAYS_NEEDED - today;
        if (left > 0) return Component.translatable("message.seals.cat.wait", left);
        return null;
    }

    /** El gato nunca se enfada contigo: su sello solo se rompe en paz. */
    @Override
    public boolean stabilityLocked() {
        return true;
    }

    // ---------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------

    @Override
    public void tick(ServerPlayer player, SealData data) {
        float s = data.ageScale();
        boolean changed = false;

        // Ojos felinos: visión nocturna mientras esté activo
        if (data.instinct()) {
            MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
            if (nv == null || nv.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false, true));
            }
        }

        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED_ID, "Forma felina",
                data.werewolf() ? round(FORM_SPEED * s) : 0, AttributeModifier.Operation.MULTIPLY_BASE);

        if (data.werewolf()) {
            data.setWerewolfTicks(data.werewolfTicks() - 1);
            if (data.werewolfTicks() <= 0) {
                endForm(player, data);
                changed = true;
            }
        } else if (data.werewolfCooldown() > 0) {
            data.setWerewolfCooldown(data.werewolfCooldown() - 1);
        }

        if (anyAbilityActive(player, data)) {
            data.setAbilityUseTicks(data.abilityUseTicks() + 1);
            if (data.abilityUseTicks() >= SealManager.ABILITY_AFFINITY_TICKS) {
                data.setAbilityUseTicks(0);
                SealManager.addAffinity(player, data, 1, "affinity.seals.reason.ability");
            }
        }

        boolean timers = data.werewolf() || data.werewolfCooldown() > 0;
        if (changed || (timers && player.tickCount % 20 == 0)) SealManager.sync(player);
    }

    @Override
    public boolean anyAbilityActive(ServerPlayer player, SealData data) {
        return data.instinct() || data.werewolf();
    }

    @Override
    public int visualFlags(SealData data) {
        int f = 0;
        if (data.instinct()) f |= SealVisuals.CAT_EYES;
        if (data.werewolf()) f |= SealVisuals.CAT_FORM;
        return f;
    }

    // ---------------------------------------------------------------------------------------
    // Pasivas
    // ---------------------------------------------------------------------------------------

    /** Siete vidas: los gatos siempre caen de pie. */
    @Override
    public boolean onFall(ServerPlayer player, SealData data, float distance) {
        if (distance > 4.0F) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 0.5F, 1.2F);
        }
        return distance > 0.0F;
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad
    // ---------------------------------------------------------------------------------------

    @Override
    public void onKill(ServerPlayer player, SealData data, LivingEntity victim, boolean byPlayer) {
        if (victim.getType().is(ModTags.BOSSES)) {
            SealManager.addAffinity(player, data, 50, "affinity.seals.reason.boss");
        } else if (victim instanceof Rabbit || victim instanceof Chicken || victim instanceof Phantom) {
            // Las presas de un gato (y los phantoms, a los que los gatos espantan)
            SealManager.addAffinity(player, data, 1, "affinity.seals.reason.hunt");
        } else if (victim instanceof Enemy) {
            SealManager.addAffinity(player, data, 1, "affinity.seals.reason.hostile");
        }
    }

    /** Comer pescado le gusta al gato. */
    @Override
    public void onEat(ServerPlayer player, SealData data, ItemStack stack) {
        if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.COOKED_COD) || stack.is(Items.COOKED_SALMON)
                || stack.is(Items.TROPICAL_FISH)) {
            SealManager.addAffinity(player, data, 1, "affinity.seals.reason.fish");
        }
    }

    /**
     * Regalo matutino: al despertarte por la mañana, tu gato te deja algo (como los gatos de verdad).
     * Una vez por día.
     */
    public static void morningGift(ServerPlayer player, SealData data) {
        long today = SealManager.currentDay(player.server);
        if (data.lastGiftDay() == today) return;
        data.setLastGiftDay(today);
        ServerLevel level = player.serverLevel();
        var table = player.server.getLootData().getLootTable(BuiltInLootTables.CAT_MORNING_GIFT);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.GIFT);
        for (ItemStack gift : table.getRandomItems(params)) {
            if (!player.getInventory().add(gift)) player.drop(gift, false);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.seals.cat.gift").withStyle(ChatFormatting.LIGHT_PURPLE));
        SealManager.addAffinity(player, data, 2, "affinity.seals.reason.gift");
    }

    // ---------------------------------------------------------------------------------------
    // Habilidades
    // ---------------------------------------------------------------------------------------

    @Override
    public void activate(ServerPlayer player, SealData data, int slot) {
        SealAbility ability = abilities.get(slot);
        if (!ability.unlocked(data)) {
            player.displayClientMessage(Component.translatable("message.seals.ability_locked",
                    ability.name(), ability.unlockLevel() - 1).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        switch (slot) {
            case SLOT_SIGHT -> {
                data.setInstinct(!data.instinct());
                if (data.instinct()) {
                    player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_HISS, SoundSource.PLAYERS, 0.6F, 1.2F);
                    SealManager.fx(player, S2CFxPacket.Type.CAT_SIGHT);
                } else {
                    MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
                    if (nv != null && nv.isAmbient()) player.removeEffect(MobEffects.NIGHT_VISION);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_PURR, SoundSource.PLAYERS, 0.6F, 1.0F);
                }
                SealManager.sync(player);
            }
            case SLOT_LIVES -> player.displayClientMessage(Component.translatable("message.seals.ability_passive",
                    ability.name()).withStyle(ChatFormatting.GRAY), true);
            case SLOT_FORM -> {
                if (data.werewolf()) {
                    endForm(player, data);
                    SealManager.sync(player);
                    return;
                }
                if (data.werewolfCooldown() > 0) {
                    player.displayClientMessage(Component.translatable("message.seals.ability_cooldown",
                            ability.name(), (data.werewolfCooldown() + 19) / 20).withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                data.setWerewolf(true);
                data.setWerewolfTicks((int) (FORM_DURATION * data.ageScale()));
                player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 1.2F, 0.8F);
                SealManager.fx(player, S2CFxPacket.Type.CAT_FORM);
                SealManager.sync(player);
            }
            default -> {}
        }
    }

    private void endForm(ServerPlayer player, SealData data) {
        data.setWerewolf(false);
        data.setWerewolfTicks(0);
        data.setWerewolfCooldown(FORM_COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.9F);
        SealManager.fx(player, S2CFxPacket.Type.CAT_FORM);
    }

    @Override
    public void deactivateAll(ServerPlayer player, SealData data) {
        data.setInstinct(false);
        data.setWerewolf(false);
        data.setWerewolfTicks(0);
        MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
        if (nv != null && nv.isAmbient()) player.removeEffect(MobEffects.NIGHT_VISION);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    // ---------------------------------------------------------------------------------------
    // Vínculo
    // ---------------------------------------------------------------------------------------

    @Override public int talkLines() { return 10; }
    @Override public int ignoreLines() { return 3; }
    /** Los gatos van a su aire: a veces no te hacen caso. */
    @Override public float talkChance(SealData data) { return 0.8F; }
    @Override public float likeChance(SealData data) { return 0.85F; }
    @Override public Item favoriteFood() { return Items.COD; }
    @Override public double innerHealth() { return 20.0; }
    @Override public int bondLevel() { return 2; }

    @Override
    public Mob createInnerMob(ServerLevel level) {
        return ModEntities.INNER_CAT.get().create(level);
    }

    /** Sale contigo y vuelve a ser tu gato. */
    @Override
    public void onPeacefulRelease(ServerPlayer player, SealData data) {
        ServerLevel level = player.serverLevel();
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        cat.moveTo(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5, player.getYRot() + 180, 0);
        cat.setVariant(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.ALL_BLACK));
        cat.tame(player);
        cat.setPersistenceRequired();
        level.addFreshEntity(cat);
        level.playSound(null, cat.blockPosition(), SoundEvents.CAT_PURREOW, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    @Override
    public void resetCooldowns(SealData data) {
        data.setWerewolfCooldown(0);
        if (data.werewolf()) data.setWerewolfTicks(Math.max(data.werewolfTicks(), 40));
    }

    // ---------------------------------------------------------------------------------------
    // Interfaz
    // ---------------------------------------------------------------------------------------

    @Override
    public List<Component> abilityStats(SealData data, int index) {
        float s = data.ageScale();
        return switch (index) {
            case SLOT_SIGHT -> List.of(
                    stat("sense", (int) SIGHT_RANGE + " " + Component.translatable("stat.seals.blocks").getString()),
                    stat("night_vision", Component.translatable("stat.seals.yes").getString()));
            case SLOT_LIVES -> List.of(stat("fall", Component.translatable("stat.seals.never").getString()));
            case SLOT_FORM -> List.of(
                    stat("speed", "+" + Math.round(FORM_SPEED * s * 100) + "%"),
                    stat("jump", "+" + Math.round(JUMP_BOOST * 100 / 0.42) + "%"),
                    stat("climb", CLIMB_BLOCKS + " " + Component.translatable("stat.seals.blocks").getString()),
                    stat("duration", (int) (FORM_DURATION * s / 20) + " s"),
                    stat("cooldown", FORM_COOLDOWN / 20 + " s"));
            default -> List.of();
        };
    }

    @Override
    public boolean abilityActive(SealData data, int index) {
        return switch (index) {
            case SLOT_SIGHT -> data.instinct();
            case SLOT_LIVES -> true;
            case SLOT_FORM -> data.werewolf();
            default -> false;
        };
    }

    @Override
    public int abilityCooldown(SealData data, int index) {
        return index == SLOT_FORM && !data.werewolf() ? data.werewolfCooldown() : 0;
    }

    @Override
    public int abilityMaxCooldown(int index) {
        return index == SLOT_FORM ? FORM_COOLDOWN : 0;
    }

    private static Component stat(String key, String value) {
        return Component.translatable("stat.seals." + key, value);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
