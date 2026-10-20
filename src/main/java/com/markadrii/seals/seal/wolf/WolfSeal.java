package com.markadrii.seals.seal.wolf;

import com.markadrii.seals.Seals;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.registry.ModEntities;
import com.markadrii.seals.registry.ModTags;
import com.markadrii.seals.seal.SealAbility;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.SealTheme;
import com.markadrii.seals.seal.SealVisuals;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Lobo sellado. Solo se puede sellar a un lobo tuyo que lleve al menos 2 días contigo.
 * <ul>
 *   <li>Nivel 1: Instinto primal (velocidad, sientes a las criaturas cercanas, ojos rojos)</li>
 *   <li>Nivel 2 (1 estrella, la única): Hombre lobo, mundo interior y BOUND</li>
 * </ul>
 */
public class WolfSeal extends SealDefinition {

    /** Día en que se domesticó al lobo (se guarda en el propio lobo). */
    public static final String TAMED_DAY_TAG = "seals_tamed_day";
    public static final int TAMED_DAYS_NEEDED = 2;

    // --- Valores base (se multiplican por la edad) ---
    public static final double INSTINCT_SPEED = 0.25;      // +25% de velocidad
    public static final double INSTINCT_RANGE = 24.0;      // bloques a los que sientes criaturas
    public static final double WEREWOLF_ARMOR = 5.0;
    public static final double WEREWOLF_ATTACK = 4.0;
    public static final double WEREWOLF_SPEED = 0.10;
    public static final int WEREWOLF_DURATION = 20 * 90;   // 90 s
    public static final int WEREWOLF_COOLDOWN = 20 * 30;   // 30 s

    // --- Afinidad y estabilidad ---
    public static final int AFFINITY_HUNT = 1;
    public static final int AFFINITY_HOSTILE = 1;
    public static final int AFFINITY_BOSS = 50;
    public static final int AFFINITY_MEAT = 1;

    public static final int SLOT_INSTINCT = 0;
    public static final int SLOT_WEREWOLF = 1;

    private static final UUID INSTINCT_SPEED_ID = UUID.fromString("7a1d0c2e-4b6f-4d8a-9c3e-2f1a0b9c8d01");
    private static final UUID WEREWOLF_ARMOR_ID = UUID.fromString("7a1d0c2e-4b6f-4d8a-9c3e-2f1a0b9c8d02");
    private static final UUID WEREWOLF_ATTACK_ID = UUID.fromString("7a1d0c2e-4b6f-4d8a-9c3e-2f1a0b9c8d03");
    private static final UUID WEREWOLF_SPEED_ID = UUID.fromString("7a1d0c2e-4b6f-4d8a-9c3e-2f1a0b9c8d04");

    private final List<SealAbility> abilities = List.of(
            new SealAbility("instinto_primal", 1, SealAbility.Kind.TOGGLE),
            new SealAbility("hombre_lobo", 2, SealAbility.Kind.TIMED)
    );

    private final SealTheme theme = new SealTheme(
            0xFF1C2027,  // fondo: noche
            0xFF101216,  // cabecera
            0xFFE6E2DA,  // texto cabecera
            0xF0A69E92,  // panel: pelaje gris
            0xFFCFC8BB,  // panel claro
            0xFF3E444C,  // bordes
            0xFF1F1F1F,  // texto
            0xFF4E4A44,  // texto suave
            0xFF8C2F2F,  // acento: sangre
            0xFFB03030,  // afinidad
            0xFF4F6B8A,  // estabilidad: luna
            0xFF8A7F70,  // edad
            0xFFF2C744,  // estrella encendida
            0xFF5E5E5E,  // estrella apagada
            Seals.id("textures/gui/seal/wolf_emblem.png"),
            Seals.id("textures/gui/seal/wolf_glyphs.png")
    );

    public WolfSeal() {
        super(new ResourceLocation("minecraft", "wolf"));
    }

    @Override public EntityType<?> entityType() { return EntityType.WOLF; }
    /** Solo una estrella. */
    @Override public int maxLevel() { return 2; }
    @Override public int minAge() { return 1; }
    @Override public int maxAge() { return 12; }
    @Override public List<SealAbility> abilities() { return abilities; }
    @Override public SealTheme theme() { return theme; }

    // ---------------------------------------------------------------------------------------
    // Sellado: tiene que ser tu lobo y llevar 2 días contigo
    // ---------------------------------------------------------------------------------------

    @Override
    public Component sealProblem(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof Wolf wolf) || !wolf.isTame() || !wolf.isOwnedBy(player)) {
            return Component.translatable("message.seals.wolf.not_yours");
        }
        long today = SealManager.currentDay(player.server);
        var tag = wolf.getPersistentData();
        if (!tag.contains(TAMED_DAY_TAG)) {
            // Lobos domesticados antes de instalar el mod: empiezan a contar ahora
            tag.putLong(TAMED_DAY_TAG, today);
        }
        long left = tag.getLong(TAMED_DAY_TAG) + TAMED_DAYS_NEEDED - today;
        if (left > 0) return Component.translatable("message.seals.wolf.wait", left);
        return null;
    }

    // ---------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------

    @Override
    public void tick(ServerPlayer player, SealData data) {
        float s = data.ageScale();
        boolean changed = false;

        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, INSTINCT_SPEED_ID, "Instinto primal",
                data.instinct() ? round(INSTINCT_SPEED * s) : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        SealManager.setModifier(player, Attributes.ARMOR, WEREWOLF_ARMOR_ID, "Hombre lobo",
                data.werewolf() ? round(WEREWOLF_ARMOR * s) : 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.ATTACK_DAMAGE, WEREWOLF_ATTACK_ID, "Hombre lobo",
                data.werewolf() ? round(WEREWOLF_ATTACK * s) : 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, WEREWOLF_SPEED_ID, "Hombre lobo",
                data.werewolf() ? round(WEREWOLF_SPEED * s) : 0, AttributeModifier.Operation.MULTIPLY_BASE);

        if (data.werewolf()) {
            data.setWerewolfTicks(data.werewolfTicks() - 1);
            if (data.werewolfTicks() <= 0) {
                endWerewolf(player, data);
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
        if (data.instinct() || data.werewolf()) f |= SealVisuals.EYES;
        if (data.werewolf()) f |= SealVisuals.WEREWOLF;
        return f;
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad y estabilidad
    // ---------------------------------------------------------------------------------------

    @Override
    public void onKill(ServerPlayer player, SealData data, LivingEntity victim, boolean byPlayer) {
        if (victim.getType().is(ModTags.BOSSES)) {
            SealManager.addAffinity(player, data, AFFINITY_BOSS, "affinity.seals.reason.boss");
        } else if (victim instanceof Sheep || victim instanceof Rabbit || victim instanceof Fox
                || victim instanceof Chicken || victim instanceof AbstractSkeleton) {
            // Las presas de un lobo (y los esqueletos, sus enemigos de siempre)
            SealManager.addAffinity(player, data, AFFINITY_HUNT, "affinity.seals.reason.hunt");
        } else if (victim instanceof Enemy) {
            SealManager.addAffinity(player, data, AFFINITY_HOSTILE, "affinity.seals.reason.hostile");
        }

        // El lobo nunca se enfada contigo: no hay pérdida de estabilidad
    }

    /** Su sello solo se puede romper en paz, desde el mundo interior. */
    @Override
    public boolean stabilityLocked() {
        return true;
    }

    /** Comer carne le gusta al lobo. */
    @Override
    public void onEat(ServerPlayer player, SealData data, ItemStack stack) {
        var food = stack.getFoodProperties(player);
        if (food != null && food.isMeat()) {
            SealManager.addAffinity(player, data, AFFINITY_MEAT, "affinity.seals.reason.meat");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Combate
    // ---------------------------------------------------------------------------------------

    /** Garras: los golpes a mano desnuda del hombre lobo desgarran. */
    @Override
    public float onMeleeHit(ServerPlayer player, SealData data, LivingEntity target, float damage) {
        if (!data.werewolf() || !player.getMainHandItem().isEmpty()) return damage;
        player.level().playSound(null, target.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 0.8F, 0.7F);
        SealManager.fx(target, S2CFxPacket.Type.CLAW);
        return damage + 2.0F * data.ageScale();
    }

    @Override
    public void activate(ServerPlayer player, SealData data, int slot) {
        SealAbility ability = abilities.get(slot);
        if (!ability.unlocked(data)) {
            player.displayClientMessage(Component.translatable("message.seals.ability_locked",
                    ability.name(), ability.unlockLevel() - 1).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (slot == SLOT_INSTINCT) {
            data.setInstinct(!data.instinct());
            if (data.instinct()) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 0.6F, 1.1F);
                SealManager.fx(player, S2CFxPacket.Type.INSTINCT);
            } else {
                player.level().playSound(null, player.blockPosition(), SoundEvents.WOLF_WHINE, SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            SealManager.sync(player);
        } else if (slot == SLOT_WEREWOLF) {
            if (data.werewolf()) {
                endWerewolf(player, data);
                SealManager.sync(player);
                return;
            }
            if (data.werewolfCooldown() > 0) {
                player.displayClientMessage(Component.translatable("message.seals.ability_cooldown",
                        ability.name(), (data.werewolfCooldown() + 19) / 20).withStyle(ChatFormatting.GRAY), true);
                return;
            }
            data.setWerewolf(true);
            data.setWerewolfTicks((int) (WEREWOLF_DURATION * data.ageScale()));
            player.level().playSound(null, player.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5F, 0.6F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6F, 1.4F);
            SealManager.fx(player, S2CFxPacket.Type.WEREWOLF);
            SealManager.anim(player, "golem_roar");
            SealManager.sync(player);
        }
    }

    private void endWerewolf(ServerPlayer player, SealData data) {
        data.setWerewolf(false);
        data.setWerewolfTicks(0);
        data.setWerewolfCooldown(WEREWOLF_COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.WOLF_WHINE, SoundSource.PLAYERS, 0.8F, 0.8F);
        SealManager.fx(player, S2CFxPacket.Type.WEREWOLF);
    }

    @Override
    public void deactivateAll(ServerPlayer player, SealData data) {
        data.setInstinct(false);
        data.setWerewolf(false);
        data.setWerewolfTicks(0);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, INSTINCT_SPEED_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
        SealManager.setModifier(player, Attributes.ARMOR, WEREWOLF_ARMOR_ID, "", 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.ATTACK_DAMAGE, WEREWOLF_ATTACK_ID, "", 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, WEREWOLF_SPEED_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    // ---------------------------------------------------------------------------------------
    // Vínculo
    // ---------------------------------------------------------------------------------------

    @Override public int talkLines() { return 10; }
    @Override public float likeChance() { return 0.9F; }
    @Override public Item favoriteFood() { return Items.BEEF; }
    @Override public double innerHealth() { return 40.0; }
    @Override public double innerAttack() { return 6.0; }
    /** Con su única estrella ya puedes usar BOUND. No hay pelea. */
    @Override public int bondLevel() { return 2; }

    @Override
    public Mob createInnerMob(ServerLevel level) {
        return ModEntities.INNER_WOLF.get().create(level);
    }

    /** Sale contigo y vuelve a ser tu lobo. */
    @Override
    public void onPeacefulRelease(ServerPlayer player, SealData data) {
        ServerLevel level = player.serverLevel();
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) return;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        wolf.moveTo(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5, player.getYRot() + 180, 0);
        wolf.tame(player);
        wolf.setPersistenceRequired();
        level.addFreshEntity(wolf);
        level.playSound(null, wolf.blockPosition(), SoundEvents.WOLF_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.0F);
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
            case SLOT_INSTINCT -> List.of(
                    stat("speed", "+" + Math.round(INSTINCT_SPEED * s * 100) + "%"),
                    stat("sense", (int) INSTINCT_RANGE + " " + Component.translatable("stat.seals.blocks").getString()));
            case SLOT_WEREWOLF -> List.of(
                    stat("armor", "+" + fmt(WEREWOLF_ARMOR * s)),
                    stat("damage", "+" + fmt(WEREWOLF_ATTACK * s)),
                    stat("speed", "+" + Math.round(WEREWOLF_SPEED * s * 100) + "%"),
                    stat("duration", (int) (WEREWOLF_DURATION * s / 20) + " s"),
                    stat("cooldown", WEREWOLF_COOLDOWN / 20 + " s"));
            default -> List.of();
        };
    }

    @Override
    public boolean abilityActive(SealData data, int index) {
        return index == SLOT_INSTINCT ? data.instinct() : index == SLOT_WEREWOLF && data.werewolf();
    }

    @Override
    public int abilityCooldown(SealData data, int index) {
        return index == SLOT_WEREWOLF && !data.werewolf() ? data.werewolfCooldown() : 0;
    }

    @Override
    public int abilityMaxCooldown(int index) {
        return index == SLOT_WEREWOLF ? WEREWOLF_COOLDOWN : 0;
    }

    private static Component stat(String key, String value) {
        return Component.translatable("stat.seals." + key, value);
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** ¿Siente este jugador a las criaturas cercanas? (lo usa el cliente). */
    public static boolean senses(Player player) {
        return SealVisuals.eyes(player);
    }
}
