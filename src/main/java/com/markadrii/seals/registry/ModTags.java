package com.markadrii.seals.registry;

import com.markadrii.seals.Seals;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class ModTags {
    /** Mobs que cuentan como boss (+50 de afinidad). Editable con datapacks: data/seals/tags/entity_types/bosses.json */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, Seals.id("bosses"));

    private ModTags() {}
}
