package com.markadrii.seals.seal.elder;

import com.markadrii.seals.Seals;
import com.markadrii.seals.item.AbyssalTridentItem;
import com.markadrii.seals.network.ModNetwork;
import com.markadrii.seals.network.S2CBeamPacket;
import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.registry.ModEntities;
import com.markadrii.seals.registry.ModItems;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elder Guardian sellado. Se sella dejando que te mate mientras llevas el pergamino en la mano secundaria.
 * <ul>
 *   <li>Al sellarlo: Hijo del mar (respiras bajo el agua, nadas más rápido, el mar no te ataca)</li>
 *   <li>1 estrella: Rayo abisal + mundo interior</li>
 *   <li>2 estrellas: Piel de las profundidades (puedes estar fuera del agua sin que se seque)</li>
 *   <li>3 estrellas: Forma abisal (escamas, pinchos, cola, resistencia, armadura y espinas)</li>
 *   <li>4 estrellas: Tridente abisal</li>
 * </ul>
 */
public class ElderGuardianSeal extends SealDefinition {

    // --- Hijo del mar ---
    public static final double SWIM_BONUS = 0.35;
    // --- Rayo abisal ---
    public static final double BEAM_RANGE = 24.0;
    public static final int BEAM_CHARGE = 40;          // 2 s de carga, como el del guardián
    public static final int BEAM_COOLDOWN = 20 * 15;
    public static final float BEAM_DAMAGE = 10.0F;
    // --- Forma abisal ---
    public static final double FORM_ARMOR = 15.0;
    public static final double FORM_WATER_ARMOR = 10.0;
    public static final double FORM_SPEED = 0.20;
    public static final double FORM_SWIM = 0.60;
    public static final float THORNS = 0.20F;
    // --- Agua ---
    public static final int WATER_TICKS_NEEDED = 20 * 60 * 5;   // 5 minutos al día
    public static final int DRY_DAMAGE_INTERVAL = 20 * 30;       // pierde estabilidad cada 30 s
    public static final int DRY_DAMAGE = 2;
    public static final int WATER_HEAL_INTERVAL = 20 * 10;       // recupera 1 cada 10 s bajo el agua
    public static final int SEA_AFFINITY_INTERVAL = 20 * 120;    // +1 afinidad cada 2 min bajo el agua
    // --- Estabilidad por muertes ---
    public static final int STABILITY_LAND = 2;
    public static final int STABILITY_SEA_HEAL = 3;

    public static final int SLOT_SEA = 0;
    public static final int SLOT_BEAM = 1;
    public static final int SLOT_SKIN = 2;
    public static final int SLOT_FORM = 3;
    public static final int SLOT_TRIDENT = 4;

    private static final UUID SWIM_ID = UUID.fromString("5e7d1c3a-8f2b-4a6e-9d1c-3b4a5f6e7d01");
    private static final UUID FORM_ARMOR_ID = UUID.fromString("5e7d1c3a-8f2b-4a6e-9d1c-3b4a5f6e7d02");
    private static final UUID FORM_SPEED_ID = UUID.fromString("5e7d1c3a-8f2b-4a6e-9d1c-3b4a5f6e7d03");
    private static final UUID FORM_SWIM_ID = UUID.fromString("5e7d1c3a-8f2b-4a6e-9d1c-3b4a5f6e7d04");

    /** Objetivo del rayo que se está cargando (jugador → entidad). */
    private static final Map<UUID, Integer> BEAM_TARGETS = new ConcurrentHashMap<>();

    private final List<SealAbility> abilities = List.of(
            new SealAbility("hijo_del_mar", 1, SealAbility.Kind.PASSIVE),
            new SealAbility("rayo_abisal", 2, SealAbility.Kind.ACTIVE),
            new SealAbility("piel_profunda", 3, SealAbility.Kind.PASSIVE),
            new SealAbility("forma_abisal", 4, SealAbility.Kind.TOGGLE),
            new SealAbility("tridente_abisal", 5, SealAbility.Kind.SUMMON)
    );

    private final SealTheme theme = new SealTheme(
            0xFF0E2427,  // fondo: fondo marino
            0xFF081719,  // cabecera
            0xFFD9EFEA,  // texto cabecera
            0xF08EB3AC,  // panel: prismarina
            0xFFC4DCD5,  // panel claro
            0xFF2B5859,  // bordes
            0xFF14211F,  // texto
            0xFF3E5653,  // texto suave
            0xFF2E8C82,  // acento
            0xFF3FC9BA,  // afinidad
            0xFF3A7BD5,  // estabilidad
            0xFF8A9FA0,  // edad
            0xFFF2C744,  // estrella encendida
            0xFF4F5E5E,  // estrella apagada
            Seals.id("textures/gui/seal/elder_guardian_emblem.png"),
            Seals.id("textures/gui/seal/elder_guardian_glyphs.png")
    );

    public ElderGuardianSeal() {
        super(new ResourceLocation("minecraft", "elder_guardian"));
    }

    @Override public EntityType<?> entityType() { return EntityType.ELDER_GUARDIAN; }
    /** Cuatro estrellas. */
    @Override public int maxLevel() { return 5; }
    @Override public int minAge() { return 120; }
    @Override public int maxAge() { return 250; }
    @Override public List<SealAbility> abilities() { return abilities; }
    @Override public SealTheme theme() { return theme; }

    /** Las criaturas del mar (no te atacan, y matarlas recupera el sello). */
    public static boolean isMarine(Entity e) {
        return e instanceof WaterAnimal || e instanceof Guardian || e instanceof Drowned
                || e instanceof Axolotl || e instanceof Turtle;
    }

    /** ¿Lleva este jugador al Elder Guardian sellado? */
    public static boolean carries(Player player) {
        SealDefinition def = com.markadrii.seals.seal.SealCapability.get(player).definition();
        return def instanceof ElderGuardianSeal;
    }

    // ---------------------------------------------------------------------------------------
    // Sellado: no con clic derecho, sino dejando que te mate
    // ---------------------------------------------------------------------------------------

    @Override
    public Component sealProblem(ServerPlayer player, LivingEntity target) {
        return Component.translatable("message.seals.elder.how");
    }

    // ---------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------

    @Override
    public void tick(ServerPlayer player, SealData data) {
        float s = data.ageScale();
        boolean underwater = player.isUnderWater();
        boolean inWater = player.isInWater();
        boolean changed = false;

        // Hijo del mar
        if (underwater) player.setAirSupply(player.getMaxAirSupply());
        SealManager.setModifier(player, ForgeMod.SWIM_SPEED.get(), SWIM_ID, "Hijo del mar",
                SWIM_BONUS, AttributeModifier.Operation.MULTIPLY_BASE);

        // Forma abisal
        boolean form = data.abyssal();
        double armor = form ? FORM_ARMOR * s + (inWater ? FORM_WATER_ARMOR * s : 0) : 0;
        SealManager.setModifier(player, Attributes.ARMOR, FORM_ARMOR_ID, "Forma abisal", round(armor), AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED_ID, "Forma abisal",
                form ? round(FORM_SPEED * s) : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        SealManager.setModifier(player, ForgeMod.SWIM_SPEED.get(), FORM_SWIM_ID, "Forma abisal",
                form ? round(FORM_SWIM * s) : 0, AttributeModifier.Operation.MULTIPLY_BASE);
        if (form && player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }

        // Agua: cuenta el tiempo sumergido, recupera el sello y da afinidad
        if (underwater) {
            data.setSubmergedTicks(data.submergedTicks() + 1);
            if (data.dry()) {
                data.setDry(false);
                player.displayClientMessage(Component.translatable("message.seals.elder.wet").withStyle(ChatFormatting.AQUA), true);
                changed = true;
            }
            if (data.submergedTicks() % WATER_HEAL_INTERVAL == 0 && data.stability() < 100) {
                data.setStability(data.stability() + 1);
                changed = true;
            }
            if (data.submergedTicks() % SEA_AFFINITY_INTERVAL == 0) {
                SealManager.addAffinity(player, data, 1, "affinity.seals.reason.sea");
            }
        }

        // Cada día se comprueba si estuvo 5 minutos en el agua (con 2 estrellas ya no hace falta)
        long today = SealManager.currentDay(player.server);
        if (data.dryDay() < 0 || today < data.dryDay()) {
            data.setDryDay(today);
        } else if (today > data.dryDay()) {
            if (data.level() < 3 && data.submergedTicks() < WATER_TICKS_NEEDED) {
                data.setDry(true);
                player.sendSystemMessage(Component.translatable("message.seals.elder.dry_start").withStyle(ChatFormatting.RED));
            }
            data.setSubmergedTicks(0);
            data.setDryDay(today);
            changed = true;
        }
        if (data.level() >= 3 && data.dry()) {
            data.setDry(false);
            changed = true;
        }
        if (data.dry() && player.tickCount % DRY_DAMAGE_INTERVAL == 0) {
            player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.7F, 1.0F);
            player.displayClientMessage(Component.translatable("message.seals.elder.drying").withStyle(ChatFormatting.RED), true);
            SealManager.damageStability(player, data, DRY_DAMAGE, "stability.seals.reason.dry");
            if (!data.hasSeal()) return; // el sello se ha roto
        }

        // Rayo abisal
        if (data.beamTicks() > 0) {
            tickBeam(player, data);
        } else if (data.beamCooldown() > 0) {
            data.setBeamCooldown(data.beamCooldown() - 1);
        }

        // Afinidad por usar habilidades
        if (anyAbilityActive(player, data)) {
            data.setAbilityUseTicks(data.abilityUseTicks() + 1);
            if (data.abilityUseTicks() >= SealManager.ABILITY_AFFINITY_TICKS) {
                data.setAbilityUseTicks(0);
                SealManager.addAffinity(player, data, 1, "affinity.seals.reason.ability");
            }
        }

        boolean timers = data.beamTicks() > 0 || data.beamCooldown() > 0;
        if (changed || (timers && player.tickCount % 20 == 0)) SealManager.sync(player);
    }

    @Override
    public boolean anyAbilityActive(ServerPlayer player, SealData data) {
        return data.abyssal() || data.beamTicks() > 0;
    }

    @Override
    public int visualFlags(SealData data) {
        return data.abyssal() ? SealVisuals.ABYSSAL : 0;
    }

    @Override
    public Component extraPhase(SealData data) {
        return data.dry() ? Component.translatable("screen.seals.phase.dry").withStyle(ChatFormatting.RED) : null;
    }

    // ---------------------------------------------------------------------------------------
    // Afinidad y estabilidad
    // ---------------------------------------------------------------------------------------

    @Override
    public void onKill(ServerPlayer player, SealData data, LivingEntity victim, boolean byPlayer) {
        if (victim.getType().is(ModTags.BOSSES)) {
            SealManager.addAffinity(player, data, 50, "affinity.seals.reason.boss");
        }
        if (!byPlayer) return;
        if (isMarine(victim)) {
            // Las criaturas del mar refuerzan el sello
            if (data.stability() < 100) {
                data.setStability(data.stability() + STABILITY_SEA_HEAL);
                player.displayClientMessage(Component.translatable("message.seals.stability_gain", STABILITY_SEA_HEAL)
                        .withStyle(ChatFormatting.AQUA), true);
                SealManager.sync(player);
            }
        } else if (!(victim instanceof Player)) {
            SealManager.damageStability(player, data, STABILITY_LAND, "stability.seals.reason.land");
        }
    }

    /** Forma abisal: quien te golpea recibe parte del daño. */
    @Override
    public float onPlayerHurt(ServerPlayer player, SealData data, DamageSource source, float amount) {
        if (data.abyssal() && source.getEntity() instanceof LivingEntity attacker && attacker != player
                && !source.is(net.minecraft.tags.DamageTypeTags.AVOIDS_GUARDIAN_THORNS)) {
            float reflected = amount * THORNS * data.ageScale();
            if (reflected > 0) attacker.hurt(player.damageSources().thorns(player), reflected);
        }
        return amount;
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
            case SLOT_SEA, SLOT_SKIN -> player.displayClientMessage(Component.translatable("message.seals.ability_passive",
                    ability.name()).withStyle(ChatFormatting.GRAY), true);
            case SLOT_BEAM -> startBeam(player, data, ability);
            case SLOT_FORM -> toggleForm(player, data);
            case SLOT_TRIDENT -> giveTrident(player, data, true);
            default -> {}
        }
    }

    private void startBeam(ServerPlayer player, SealData data, SealAbility ability) {
        if (data.beamTicks() > 0) return;
        if (data.beamCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.seals.ability_cooldown",
                    ability.name(), (data.beamCooldown() + 19) / 20).withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LivingEntity target = findTarget(player);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.seals.elder.no_target").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BEAM_TARGETS.put(player.getUUID(), target.getId());
        data.setBeamTicks(BEAM_CHARGE);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.2F, 0.8F);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CBeamPacket(player.getId(), target.getId(), BEAM_CHARGE));
        SealManager.sync(player);
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(BEAM_RANGE));
        AABB box = player.getBoundingBox().expandTowards(look.scale(BEAM_RANGE)).inflate(1.5);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && e != player, BEAM_RANGE * BEAM_RANGE);
        if (hit == null) return null;
        // No atraviesa bloques
        var block = player.level().clip(new ClipContext(eye, hit.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.MISS && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
            return null;
        }
        return (LivingEntity) hit.getEntity();
    }

    private void tickBeam(ServerPlayer player, SealData data) {
        Integer id = BEAM_TARGETS.get(player.getUUID());
        Entity e = id == null ? null : player.level().getEntity(id);
        if (!(e instanceof LivingEntity target) || !target.isAlive() || !player.hasLineOfSight(target)
                || target.distanceTo(player) > BEAM_RANGE * 1.25) {
            stopBeam(player, data);
            return;
        }
        data.setBeamTicks(data.beamTicks() - 1);
        if (data.beamTicks() <= 0) {
            float s = data.ageScale();
            target.hurt(player.damageSources().indirectMagic(player, player), BEAM_DAMAGE * s);
            player.level().playSound(null, target.blockPosition(), SoundEvents.ELDER_GUARDIAN_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
            SealManager.fx(target, S2CFxPacket.Type.BEAM_HIT);
            stopBeam(player, data);
        }
    }

    private void stopBeam(ServerPlayer player, SealData data) {
        BEAM_TARGETS.remove(player.getUUID());
        data.setBeamTicks(0);
        data.setBeamCooldown(BEAM_COOLDOWN);
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new S2CBeamPacket(player.getId(), -1, 0));
        SealManager.sync(player);
    }

    private void toggleForm(ServerPlayer player, SealData data) {
        data.setAbyssal(!data.abyssal());
        if (data.abyssal()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.PLAYERS, 1.2F, 0.7F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.2F);
            SealManager.fx(player, S2CFxPacket.Type.ABYSSAL);
            SealManager.anim(player, "golem_roar");
        } else {
            player.level().playSound(null, player.blockPosition(), SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.0F);
            SealManager.fx(player, S2CFxPacket.Type.ABYSSAL);
        }
        SealManager.sync(player);
    }

    /** Da el tridente abisal (si no lo tiene ya). */
    public static void giveTrident(ServerPlayer player, SealData data, boolean announce) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof AbyssalTridentItem) {
                if (announce) player.displayClientMessage(Component.translatable("message.seals.elder.trident_have")
                        .withStyle(ChatFormatting.GRAY), true);
                return;
            }
        }
        if (player.getOffhandItem().getItem() instanceof AbyssalTridentItem) return;
        ItemStack trident = AbyssalTridentItem.create(player);
        if (!player.getInventory().add(trident)) player.drop(trident, false);
        player.level().playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 0.6F);
        SealManager.fx(player, S2CFxPacket.Type.ABYSSAL);
        player.sendSystemMessage(Component.translatable("message.seals.elder.trident").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void onLevelUp(ServerPlayer player, SealData data, int newLevel) {
        if (newLevel >= 5) giveTrident(player, data, false);
    }

    @Override
    public void deactivateAll(ServerPlayer player, SealData data) {
        data.setAbyssal(false);
        BEAM_TARGETS.remove(player.getUUID());
        data.setBeamTicks(0);
        SealManager.setModifier(player, ForgeMod.SWIM_SPEED.get(), SWIM_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
        SealManager.setModifier(player, Attributes.ARMOR, FORM_ARMOR_ID, "", 0, AttributeModifier.Operation.ADDITION);
        SealManager.setModifier(player, Attributes.MOVEMENT_SPEED, FORM_SPEED_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
        SealManager.setModifier(player, ForgeMod.SWIM_SPEED.get(), FORM_SWIM_ID, "", 0, AttributeModifier.Operation.MULTIPLY_BASE);
    }

    /** El sello se rompe: el Elder Guardian sale furioso con el doble de vida. */
    @Override
    public void onSealBroken(ServerPlayer player, SealData data) {
        ServerLevel level = player.serverLevel();
        ElderGuardian elder = EntityType.ELDER_GUARDIAN.create(level);
        if (elder == null) return;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        elder.moveTo(player.getX() + look.x * 3, player.getY() + 1, player.getZ() + look.z * 3, player.getYRot() + 180, 0);
        var hp = elder.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(hp.getBaseValue() * 2);
        elder.setHealth(elder.getMaxHealth());
        elder.setCustomName(Component.translatable("entity.seals.freed_elder").withStyle(ChatFormatting.DARK_RED));
        elder.setPersistenceRequired();
        level.addFreshEntity(elder);
        elder.setTarget(player);
        level.playSound(null, elder.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    // ---------------------------------------------------------------------------------------
    // Vínculo
    // ---------------------------------------------------------------------------------------

    @Override public int talkLines() { return 10; }
    @Override public int ignoreLines() { return 3; }
    @Override public Item favoriteFood() { return Items.TROPICAL_FISH; }
    @Override public Item innerKeyItem() { return Items.HEART_OF_THE_SEA; }
    @Override public double innerHealth() { return 80.0; }
    /** No hay pelea ni BOUND con el Elder Guardian. */
    @Override public int bondLevel() { return 99; }

    /** 66% de contestarte bien, sube con la edad; con 2 estrellas siempre te contesta. */
    @Override
    public float talkChance(SealData data) {
        if (data.level() >= 3) return 1.0F;
        return Math.min(0.98F, 0.66F + data.ageYears() * 0.0012F);
    }

    @Override
    public float likeChance(SealData data) {
        return talkChance(data);
    }

    @Override
    public Mob createInnerMob(ServerLevel level) {
        return ModEntities.INNER_ELDER.get().create(level);
    }

    /** Vuelve al océano: no se queda contigo. */
    @Override
    public void onPeacefulRelease(ServerPlayer player, SealData data) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.2F);
        SealManager.fx(player, S2CFxPacket.Type.ABYSSAL);
    }

    @Override
    public void resetCooldowns(SealData data) {
        data.setBeamCooldown(0);
    }

    // ---------------------------------------------------------------------------------------
    // Interfaz
    // ---------------------------------------------------------------------------------------

    @Override
    public List<Component> abilityStats(SealData data, int index) {
        float s = data.ageScale();
        return switch (index) {
            case SLOT_SEA -> List.of(
                    stat("breath", Component.translatable("stat.seals.infinite").getString()),
                    stat("swim", "+" + Math.round(SWIM_BONUS * 100) + "%"),
                    stat("sea_peace", Component.translatable("stat.seals.yes").getString()));
            case SLOT_BEAM -> List.of(
                    stat("damage", fmt(BEAM_DAMAGE * s)),
                    stat("range", (int) BEAM_RANGE + " " + Component.translatable("stat.seals.blocks").getString()),
                    stat("charge", BEAM_CHARGE / 20 + " s"),
                    stat("cooldown", BEAM_COOLDOWN / 20 + " s"));
            case SLOT_SKIN -> List.of(stat("dry_free", Component.translatable("stat.seals.yes").getString()));
            case SLOT_FORM -> List.of(
                    stat("armor", "+" + fmt(FORM_ARMOR * s) + " (+" + fmt(FORM_WATER_ARMOR * s) + " " + Component.translatable("stat.seals.in_water").getString() + ")"),
                    stat("thorns", Math.round(THORNS * s * 100) + "%"),
                    stat("speed", "+" + Math.round(FORM_SPEED * s * 100) + "%"),
                    stat("resistance", "I"));
            case SLOT_TRIDENT -> List.of(
                    stat("damage", fmt(AbyssalTridentItem.MELEE_DAMAGE)),
                    stat("thrown", "+" + fmt(AbyssalTridentItem.THROWN_BONUS)),
                    stat("enchants", Component.translatable("stat.seals.loyalty_riptide").getString()));
            default -> List.of();
        };
    }

    @Override
    public boolean abilityActive(SealData data, int index) {
        return switch (index) {
            case SLOT_SEA -> true;
            case SLOT_SKIN -> data.level() >= 3;
            case SLOT_BEAM -> data.beamTicks() > 0;
            case SLOT_FORM -> data.abyssal();
            default -> false;
        };
    }

    @Override
    public int abilityCooldown(SealData data, int index) {
        return index == SLOT_BEAM && data.beamTicks() == 0 ? data.beamCooldown() : 0;
    }

    @Override
    public int abilityMaxCooldown(int index) {
        return index == SLOT_BEAM ? BEAM_COOLDOWN : 0;
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

    // Para que ModItems no quede sin usar en este archivo si se reorganiza
    @SuppressWarnings("unused")
    private static Item tridentItem() {
        return ModItems.ABYSSAL_TRIDENT.get();
    }
}
