package com.markadrii.seals.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Guarda en el mundo quién es el Oldest One (solo puede haber uno). */
public class OldestOneData extends SavedData {
    private static final String NAME = "seals_oldest_one";

    @Nullable private UUID holder;
    private String holderName = "";

    public static OldestOneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OldestOneData::load, OldestOneData::new, NAME);
    }

    private static OldestOneData load(CompoundTag tag) {
        OldestOneData data = new OldestOneData();
        if (tag.hasUUID("holder")) data.holder = tag.getUUID("holder");
        data.holderName = tag.getString("holderName");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (holder != null) tag.putUUID("holder", holder);
        tag.putString("holderName", holderName);
        return tag;
    }

    @Nullable public UUID holder() { return holder; }
    public String holderName() { return holderName; }

    public boolean isTaken() {
        return holder != null;
    }

    public void claim(UUID id, String name) {
        holder = id;
        holderName = name;
        setDirty();
    }

    public void reset() {
        holder = null;
        holderName = "";
        setDirty();
    }
}
