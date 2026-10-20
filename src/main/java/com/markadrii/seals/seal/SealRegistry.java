package com.markadrii.seals.seal;

import com.markadrii.seals.seal.irongolem.IronGolemSeal;
import com.markadrii.seals.seal.wolf.WolfSeal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Lista de mobs que se pueden sellar. Para añadir uno nuevo, regístralo aquí. */
public final class SealRegistry {
    private static final Map<ResourceLocation, SealDefinition> SEALS = new LinkedHashMap<>();

    public static final IronGolemSeal IRON_GOLEM = register(new IronGolemSeal());
    public static final WolfSeal WOLF = register(new WolfSeal());

    private SealRegistry() {}

    private static <T extends SealDefinition> T register(T def) {
        SEALS.put(def.id(), def);
        return def;
    }

    @Nullable
    public static SealDefinition get(ResourceLocation id) {
        return SEALS.get(id);
    }

    @Nullable
    public static SealDefinition forEntity(EntityType<?> type) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return key == null ? null : SEALS.get(key);
    }

    public static Collection<SealDefinition> all() {
        return SEALS.values();
    }
}
