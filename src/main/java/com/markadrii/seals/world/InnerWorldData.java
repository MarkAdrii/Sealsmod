package com.markadrii.seals.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Asigna a cada jugador su propia zona dentro del mundo interior (para que no se crucen). */
public class InnerWorldData extends SavedData {
    private static final String NAME = "seals_inner_world";

    private final Map<UUID, Integer> regions = new HashMap<>();
    private int next;

    public static InnerWorldData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(InnerWorldData::load, InnerWorldData::new, NAME);
    }

    private static InnerWorldData load(CompoundTag tag) {
        InnerWorldData data = new InnerWorldData();
        data.next = tag.getInt("next");
        ListTag list = tag.getList("regions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            if (c.hasUUID("id")) data.regions.put(c.getUUID("id"), c.getInt("index"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("next", next);
        ListTag list = new ListTag();
        regions.forEach((id, index) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putInt("index", index);
            list.add(c);
        });
        tag.put("regions", list);
        return tag;
    }

    public int regionOf(UUID player) {
        Integer index = regions.get(player);
        if (index == null) {
            index = next++;
            regions.put(player, index);
            setDirty();
        }
        return index;
    }
}
