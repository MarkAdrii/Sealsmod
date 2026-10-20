package com.markadrii.seals.client;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Truco para leer el campo protegido de "flags" de las entidades (brillo, fuego, sigilo...).
 * Nunca se crea: solo sirve para acceder a {@code DATA_SHARED_FLAGS_ID} desde una subclase.
 */
public abstract class EntityFlagsAccess extends Entity {
    private static final int GLOWING_BIT = 1 << 6;

    private EntityFlagsAccess(EntityType<?> type, Level level) {
        super(type, level);
    }

    private static EntityDataAccessor<Byte> flags() {
        return DATA_SHARED_FLAGS_ID;
    }

    /** Hace brillar (o no) a una entidad solo en este cliente. */
    public static void setLocalGlow(Entity entity, boolean glow) {
        byte current = entity.getEntityData().get(flags());
        byte next = (byte) (glow ? current | GLOWING_BIT : current & ~GLOWING_BIT);
        if (next != current) entity.getEntityData().set(flags(), next);
    }
}
