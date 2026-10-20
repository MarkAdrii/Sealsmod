package com.markadrii.seals.seal;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;

import java.util.List;

/**
 * Describe un mob que se puede sellar: niveles, edad, habilidades, colores y reglas.
 * Cada mob nuevo (Wither, Warden...) será una subclase de esta.
 */
public abstract class SealDefinition {
    private final ResourceLocation id;

    protected SealDefinition(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation id() {
        return id;
    }

    public abstract EntityType<?> entityType();

    /** Nivel máximo de afecto (nivel 1 = sin estrellas). Iron Golem: 4 = tres estrellas. */
    public abstract int maxLevel();

    public int maxStars() {
        return maxLevel() - 1;
    }

    /** Rango de edad al sellarlo, en años. */
    public abstract int minAge();

    public abstract int maxAge();

    public abstract List<SealAbility> abilities();

    public abstract SealTheme theme();

    public Component name() {
        return entityType().getDescription();
    }

    /** Clave base para los textos de este sello: "seal.seals.iron_golem". */
    public String textKey() {
        return "seal.seals." + id.getPath();
    }

    /** Vida máxima (en %) a la que hay que bajar al mob para poder sellarlo. */
    public float sealHealthThreshold() {
        return 0.25F;
    }

    public boolean canSeal(LivingEntity target) {
        return target.getHealth() <= target.getMaxHealth() * sealHealthThreshold();
    }

    // ---------------------------------------------------------------------------------------
    // Ganchos de juego (solo servidor). Cada mob sellado decide qué hace en cada caso.
    // ---------------------------------------------------------------------------------------

    public void tick(ServerPlayer player, SealData data) {}

    /** El jugador (o su invocación, si {@code byPlayer} es false) ha matado a algo. */
    public void onKill(ServerPlayer player, SealData data, LivingEntity victim, boolean byPlayer) {}

    public void onTrade(ServerPlayer player, SealData data, AbstractVillager villager) {}

    /** @return true si hay que cancelar el daño por caída. */
    public boolean onFall(ServerPlayer player, SealData data, float distance) {
        return false;
    }

    /** Daño extra al golpear (puede devolver el mismo daño). */
    public float onMeleeHit(ServerPlayer player, SealData data, LivingEntity target, float damage) {
        return damage;
    }

    /** Se ha pulsado la tecla 1-9 en modo combate. */
    public void activate(ServerPlayer player, SealData data, int slot) {}

    /** Apaga todo lo activo (al morir, al perder el sello...). */
    public void deactivateAll(ServerPlayer player, SealData data) {}

    /** La estabilidad ha llegado a 0: el mob se libera. */
    public void onSealBroken(ServerPlayer player, SealData data) {}

    // ---------------------------------------------------------------------------------------
    // Información para la interfaz
    // ---------------------------------------------------------------------------------------

    /** Números de la habilidad ya escalados por la edad (armadura, duración...). */
    public List<Component> abilityStats(SealData data, int index) {
        return List.of();
    }

    /** ¿Está encendida/en uso la habilidad? (para resaltarla en la interfaz). */
    public boolean abilityActive(SealData data, int index) {
        return false;
    }

    /** Recarga restante en ticks (0 = lista). */
    public int abilityCooldown(SealData data, int index) {
        return 0;
    }

    /** Duración total de la recarga en ticks (para dibujar la barra de recarga). */
    public int abilityMaxCooldown(int index) {
        return 0;
    }

    /** ¿Hay alguna habilidad en uso? (para la afinidad por uso). */
    public boolean anyAbilityActive(ServerPlayer player, SealData data) {
        return false;
    }
}
