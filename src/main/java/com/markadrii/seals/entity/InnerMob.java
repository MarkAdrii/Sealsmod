package com.markadrii.seals.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Un mob sellado tal como aparece en el mundo interior (golem, lobo...). */
public interface InnerMob {

    /** Prepara al mob al entrar el jugador (vida y daño ya escalados por la edad). */
    void setupInner(Player owner, Vec3 home, double health, double attack);

    @Nullable
    UUID getOwnerId();

    default boolean isFighting() {
        return false;
    }

    default void startFight(Player target) {}

    default void stopFight() {}

    default Mob asMob() {
        return (Mob) this;
    }
}
