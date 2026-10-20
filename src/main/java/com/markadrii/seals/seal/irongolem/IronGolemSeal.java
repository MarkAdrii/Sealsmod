package com.markadrii.seals.seal.irongolem;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.registry.ModEntities;
import com.markadrii.seals.registry.ModTags;
import com.markadrii.seals.seal.SealAbility;
import com.markadrii.seals.seal.SealData;
import com.markadrii.seals.seal.SealDefinition;
import com.markadrii.seals.seal.SealManager;
import com.markadrii.seals.seal.SealTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Iron Golem sellado.
 * <ul>
 *   <li>Nivel 1: Manto ferruquímico</li>
 *   <li>Nivel 2 (1 estrella): Pies de hierro</li>
 *   <li>Nivel 3 (2 estrellas): Yo soy el golem</li>
 *   <li>Nivel 4 (3 estrellas): Holy golem</li>
 * </ul>
 */
public class IronGolemSeal extends SealDefinition {

    // --- Valores base (se multiplican por la edad: 20 años = x1.10, 100 años = x1.50) ---
    public static final double MANTO_ARMOR = 20.0;
    public static final double FORM_ARMOR = 10.0;
    public static final float FORM_PUNCH = 6.0F;
    public static final float FORM_SCALE = 1.5F;
    public static final int FORM_DURATION = 20 * 60;   // 60 s
    public static final int FORM_COOLDOWN = 20 * 20;   // 20 s
    public static final int HOLY_DURATION = 20 * 90;   // 90 s
    public static final int HOLY_COOLDOWN = 20 * 120;  // 120 s
    public static final double HOLY_HEALTH = 100.0;
    public static final double HOLY_ATTACK = 15.0;
    public static final float FEET_MIN_FALL = 3.0F;

    // --- Estabilidad ---
    public static final int STABILITY_VILLAGER = 10;
    public static final int STABILITY_PLAYER = 20;
    public static final int STABILITY_ALLY = 5;

    // --- Afinidad ---
    public static final int AFFINITY_TRADE = 1;
    public static final int AFFINITY_HOSTILE = 1;
    public static final int AFFINITY_BOSS = 50;

    // Slots de la hotbar de combate
    public static final int SLOT_MANTO = 0;
    public static final int SLOT_FEET = 1;
    public static final int SLOT_FORM = 2;
    public static final int SLOT_HOLY = 3;

    private static final UUID MANTO_ARMOR_ID = UUID.fromString("6f0c6a4e-1b7d-4f5e-9a51-1c2d3e4f5a01");
    private static final UUID MANTO_KB_ID = UUID.fromString("6f0c6a4e-1b7d-4f5e-9a51-1c2d3e4f5a02");
    private static final UUID FORM_ARMOR_ID = UUID.fromString("6f0c6a4e-1b7d-4f5e-9a51-1c2d3e4f5a03");

    private final List<SealAbility> abilities = List.of(
            new SealAbility("manto_ferruquimico", 1, SealAbility.Kind.TOGGLE),
            new SealAbility("pies_de_hierro", 2, SealAbility.Kind.PASSIVE),
            new SealAbility("yo_soy_el_golem", 3, SealAbility.Kind.TIMED),
            new SealAbility("holy_golem", 4, SealAbility.Kind.SUMMON)
    );

    private final SealTheme theme = new SealTheme(
            0xFF26292C,  // fondo: hierro oscuro
            0xFF17191B,  // cabecera
            0xFFE8E3D6,  // texto cabecera
            0xF0BDB8AC,  // panel: piedra/hierro claro
            0xFFD8D3C6,  // panel claro
            0xFF4E5458,  // bordes
            0xFF232323,  // texto
            0xFF55534D,  // texto suave
            0xFF8E4A2C,  // acento: óxido
            0xFFC8322B,  // afinidad: rojo amapola
            0xFF5D7A3A,  // estabilidad: verde enredadera
            0xFF7D868C,  // edad: hierro
            0xFFF2C744,  // estrella encendida
            0xFF5E5E5E,  // estrella apagada
            Seals.id("textures/gui/seal/iron_golem_emblem.png"),
            Seals.id("textures/gui/seal/iron_golem_glyphs.png")
    );

    public IronGolemSeal() {
        super(new ResourceLocation("minecraft", "iron_golem"));
    }

    @Override public EntityType<?> entityType() { return EntityType.IRON_GOLEM; }
    @Override public int maxLevel() { return 4; }
    @Override public int minAge() { return 20; }
    @Override public int maxAge() { return 100; }
    @Override public List<SealAbility> abilities() { return abilities; }
    @Override public SealTheme theme() { return theme; }

    @Override
    public boolean canSeal(LivingEntity target) {
        return !(target instanceof HolyGolemEntity) && super.canSeal(target);
    }

    // ---------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------

    @Override
    public void tick(ServerPlayer player, SealData data) {
        float scale = data.ageScale();
        boolean changed = false;

        // Atributos según lo que esté activo
        SealManager.setModifier(player, Attributes.ARMOR, MANTO_ARMOR_ID, "Manto ferruquimico",
                data.manto() ? round(MANTO_ARMOR * scale) : 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, MANTO_KB_ID, "Manto ferruquimico",
                data.manto() ? 1.0 : 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.ARMOR, FORM_ARMOR_ID, "Yo soy el golem",
                data.golemForm() ? round(FORM_ARMOR * scale) : 0, AttributeModifier.Operation.ADDITION);

        // Duración de la forma de golem
        if (data.golemForm()) {
            data.setGolemFormTicks(data.golemFormTicks() - 1);
            if (data.golemFormTicks() <= 0) {
                endGolemForm(player, data);
                changed = true;
            }
        } else if (data.golemFormCooldown() > 0) {
            data.setGolemFormCooldown(data.golemFormCooldown() - 1);
        }

        // Holy golem: comprobar si sigue vivo
        if (data.holyGolem() != null) {
            Entity holy = player.serverLevel().getEntity(data.holyGolem());
            if (!(holy instanceof HolyGolemEntity golem) || !golem.isAlive()) {
                data.setHolyGolem(null);
                changed = true;
            }
        }
        if (data.holyCooldown() > 0) data.setHolyCooldown(data.holyCooldown() - 1);

        // +1 de afinidad cada minuto usando habilidades
        if (anyAbilityActive(player, data)) {
            data.setAbilityUseTicks(data.abilityUseTicks() + 1);
            if (data.abilityUseTicks() >= SealManager.ABILITY_AFFINITY_TICKS) {
                data.setAbilityUseTicks(0);
                SealManager.addAffinity(player, data, 1, "affinity.seals.reason.ability");
            }
        }

        // Sincronizar temporizadores una vez por segundo mientras corren
        boolean timers = data.golemForm() || data.golemFormCooldown() > 0 || data.holyCooldown() > 0;
        if (changed || (timers && player.tickCount % 20 == 0)) {
            SealManager.sync(player);
        }
    }

    @Override
    public boolean anyAbilityActive(ServerPlayer player, SealData data) {
        return data.manto() || data.golemForm() || data.holyGolem() != null;
    }

    // ---------------------------------------------------------------------------------------
    // Información para la interfaz
    // ---------------------------------------------------------------------------------------

    @Override
    public List<Component> abilityStats(SealData data, int index) {
        float s = data.ageScale();
        return switch (index) {
            case SLOT_MANTO -> List.of(
                    stat("armor", "+" + fmt(MANTO_ARMOR * s)),
                    stat("knockback", "100%"));
            case SLOT_FEET -> List.of(
                    stat("fall", Component.translatable("stat.seals.fall_value", (int) FEET_MIN_FALL).getString()));
            case SLOT_FORM -> List.of(
                    stat("armor", "+" + fmt(FORM_ARMOR * s)),
                    stat("punch", "+" + fmt(FORM_PUNCH * s)),
                    stat("size", "x" + fmt(FORM_SCALE)),
                    stat("duration", (int) (FORM_DURATION * s / 20) + " s"),
                    stat("cooldown", FORM_COOLDOWN / 20 + " s"));
            case SLOT_HOLY -> List.of(
                    stat("health", fmt(HOLY_HEALTH * s)),
                    stat("damage", fmt(HOLY_ATTACK * s)),
                    stat("duration", (int) (HOLY_DURATION * s / 20) + " s"),
                    stat("cooldown", HOLY_COOLDOWN / 20 + " s"));
            default -> List.of();
        };
    }

    @Override
    public boolean abilityActive(SealData data, int index) {
        return switch (index) {
            case SLOT_MANTO -> data.manto();
            case SLOT_FEET -> data.level() >= abilities.get(SLOT_FEET).unlockLevel();
            case SLOT_FORM -> data.golemForm();
            case SLOT_HOLY -> data.holyGolem() != null;
            default -> false;
        };
    }

    @Override
    public int abilityCooldown(SealData data, int index) {
        return switch (index) {
            case SLOT_FORM -> data.golemForm() ? 0 : data.golemFormCooldown();
            case SLOT_HOLY -> data.holyGolem() != null ? 0 : data.holyCooldown();
            default -> 0;
        };
    }

    @Override
    public int abilityMaxCooldown(int index) {
        return switch (index) {
            case SLOT_FORM -> FORM_COOLDOWN;
            case SLOT_HOLY -> HOLY_COOLDOWN;
            default -> 0;
        };
    }

    private static Component stat(String key, String value) {
        return Component.translatable("stat.seals." + key, value);
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad y estabilidad
    // ---------------------------------------------------------------------------------------

    @Override
    public void onTrade(ServerPlayer player, SealData data, AbstractVillager villager) {
        SealManager.addAffinity(player, data, AFFINITY_TRADE, "affinity.seals.reason.trade");
    }

    @Override
    public void onKill(ServerPlayer player, SealData data, LivingEntity victim, boolean byPlayer) {
        // Afinidad (también cuenta lo que mata tu Holy golem)
        if (victim.getType().is(ModTags.BOSSES)) {
            SealManager.addAffinity(player, data, AFFINITY_BOSS, "affinity.seals.reason.boss");
        } else if (victim instanceof Enemy) {
            SealManager.addAffinity(player, data, AFFINITY_HOSTILE, "affinity.seals.reason.hostile");
        }

        if (!byPlayer) return;

        // Estabilidad: al golem no le gusta que hagas daño a los suyos
        if (victim instanceof Villager) {
            SealManager.damageStability(player, data, STABILITY_VILLAGER, "stability.seals.reason.villager");
        } else if (victim instanceof Player other) {
            boolean teammate = player.getTeam() != null && player.isAlliedTo(other);
            SealManager.damageStability(player, data, teammate ? STABILITY_ALLY : STABILITY_PLAYER,
                    teammate ? "stability.seals.reason.teammate" : "stability.seals.reason.player");
        } else if (victim instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID())) {
            SealManager.damageStability(player, data, STABILITY_ALLY, "stability.seals.reason.pet");
        } else if (victim instanceof IronGolem && !(victim instanceof HolyGolemEntity)) {
            SealManager.damageStability(player, data, STABILITY_ALLY, "stability.seals.reason.golem");
        }
    }

    // ---------------------------------------------------------------------------------------
    // Pasivas
    // ---------------------------------------------------------------------------------------

    /** Pies de hierro: sin daño por caída desde nivel 2. */
    @Override
    public boolean onFall(ServerPlayer player, SealData data, float distance) {
        if (data.level() < abilities.get(SLOT_FEET).unlockLevel() || distance < FEET_MIN_FALL) return false;
        float volume = Math.min(1.0F, 0.3F + distance / 30.0F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, volume, 1.4F);
        SealManager.fx(player, S2CFxPacket.Type.IRON_FEET);
        return true;
    }

    /** Yo soy el golem: puñetazos más fuertes a mano desnuda que lanzan al enemigo por los aires. */
    @Override
    public float onMeleeHit(ServerPlayer player, SealData data, LivingEntity target, float damage) {
        if (!data.golemForm() || !player.getMainHandItem().isEmpty()) return damage;
        float scale = data.ageScale();
        target.setDeltaMovement(target.getDeltaMovement().add(0, 0.45 * scale, 0));
        target.hurtMarked = true;
        player.level().playSound(null, target.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0F, 1.0F);
        SealManager.fx(target, S2CFxPacket.Type.GOLEM_PUNCH);
        return damage + FORM_PUNCH * scale;
    }

    // ---------------------------------------------------------------------------------------
    // Activación (teclas 1-9 en modo combate)
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
            case SLOT_MANTO -> toggleManto(player, data);
            case SLOT_FEET -> player.displayClientMessage(Component.translatable("message.seals.ability_passive",
                    ability.name()).withStyle(ChatFormatting.GRAY), true);
            case SLOT_FORM -> toggleGolemForm(player, data);
            case SLOT_HOLY -> toggleHolyGolem(player, data);
            default -> {}
        }
    }

    private void toggleManto(ServerPlayer player, SealData data) {
        data.setManto(!data.manto());
        if (data.manto()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.8F);
            SealManager.fx(player, S2CFxPacket.Type.MANTO_ON);
            SealManager.anim(player, "manto");
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.PLAYERS, 0.6F, 1.2F);
            SealManager.fx(player, S2CFxPacket.Type.MANTO_OFF);
        }
        SealManager.sync(player);
    }

    private void toggleGolemForm(ServerPlayer player, SealData data) {
        if (data.golemForm()) {
            endGolemForm(player, data);
            SealManager.sync(player);
            return;
        }
        if (data.golemFormCooldown() > 0) {
            cooldownMessage(player, abilities.get(SLOT_FORM), data.golemFormCooldown());
            return;
        }
        data.setGolemForm(true);
        data.setGolemFormTicks((int) (FORM_DURATION * data.ageScale()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.PLAYERS, 1.2F, 0.5F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8F, 0.6F);
        SealManager.fx(player, S2CFxPacket.Type.GOLEM_FORM_ON);
        SealManager.anim(player, "golem_roar");
        SealManager.sync(player);
    }

    private void endGolemForm(ServerPlayer player, SealData data) {
        data.setGolemForm(false);
        data.setGolemFormTicks(0);
        data.setGolemFormCooldown(FORM_COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_DEATH, SoundSource.PLAYERS, 0.6F, 1.4F);
        SealManager.fx(player, S2CFxPacket.Type.GOLEM_FORM_OFF);
    }

    private void toggleHolyGolem(ServerPlayer player, SealData data) {
        ServerLevel level = player.serverLevel();
        if (data.holyGolem() != null) {
            if (level.getEntity(data.holyGolem()) instanceof HolyGolemEntity golem) golem.vanish();
            data.setHolyGolem(null);
            SealManager.sync(player);
            return;
        }
        if (data.holyCooldown() > 0) {
            cooldownMessage(player, abilities.get(SLOT_HOLY), data.holyCooldown());
            return;
        }

        HolyGolemEntity golem = ModEntities.HOLY_GOLEM.get().create(level);
        if (golem == null) return;
        float scale = data.ageScale();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 pos = player.position().add(look.scale(2.0));
        golem.moveTo(pos.x, player.getY(), pos.z, player.getYRot(), 0);
        golem.setup(player, (int) (HOLY_DURATION * scale), HOLY_HEALTH * scale, HOLY_ATTACK * scale);
        golem.finalizeSpawn(level, level.getCurrentDifficultyAt(golem.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        level.addFreshEntity(golem);

        data.setHolyGolem(golem.getUUID());
        data.setHolyCooldown(HOLY_COOLDOWN);
        level.playSound(null, golem.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0F, 0.6F);
        SealManager.fx(golem, S2CFxPacket.Type.HOLY_SUMMON);
        SealManager.anim(player, "summon");
        SealManager.sync(player);
    }

    private static void cooldownMessage(ServerPlayer player, SealAbility ability, int ticks) {
        player.displayClientMessage(Component.translatable("message.seals.ability_cooldown",
                ability.name(), (ticks + 19) / 20).withStyle(ChatFormatting.GRAY), true);
    }

    @Override
    public void deactivateAll(ServerPlayer player, SealData data) {
        data.setManto(false);
        data.setGolemForm(false);
        data.setGolemFormTicks(0);
        if (data.holyGolem() != null && player.serverLevel().getEntity(data.holyGolem()) instanceof HolyGolemEntity golem) {
            golem.vanish();
        }
        data.setHolyGolem(null);
        SealManager.setModifier(player, Attributes.ARMOR, MANTO_ARMOR_ID, "", 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.KNOCKBACK_RESISTANCE, MANTO_KB_ID, "", 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.ARMOR, FORM_ARMOR_ID, "", 0, AttributeModifier.Operation.ADDITION);
    }

    // ---------------------------------------------------------------------------------------
    // Ruptura del sello: sale un golem furioso con el doble de vida
    // ---------------------------------------------------------------------------------------

    @Override
    public void onSealBroken(ServerPlayer player, SealData data) {
        ServerLevel level = player.serverLevel();
        IronGolem golem = EntityType.IRON_GOLEM.create(level);
        if (golem == null) return;

        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        golem.moveTo(player.getX() + look.x * 2, player.getY(), player.getZ() + look.z * 2, player.getYRot() + 180, 0);
        var maxHealth = golem.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(maxHealth.getBaseValue() * 2);
        golem.setHealth(golem.getMaxHealth());
        golem.setPlayerCreated(false);
        golem.setCustomName(Component.translatable("entity.seals.freed_golem").withStyle(ChatFormatting.DARK_RED));
        golem.setPersistenceRequired();
        level.addFreshEntity(golem);
        golem.setPersistentAngerTarget(player.getUUID());
        golem.startPersistentAngerTimer();
        golem.setTarget(player);

        level.playSound(null, golem.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.HOSTILE, 1.5F, 0.4F);
        level.playSound(null, golem.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 0.8F, 0.6F);
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
