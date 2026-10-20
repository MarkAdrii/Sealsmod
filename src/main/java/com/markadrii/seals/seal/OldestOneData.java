package com.markadrii.seals.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda en el mundo quién es el Oldest One (solo puede haber uno) y la edad del mob sellado
 * de cada jugador, para poder pasar el título al siguiente más antiguo aunque esté desconectado.
 */
public class OldestOneData extends SavedData {
    private static final String NAME = "seals_oldest_one";

    public record Record(String name, int ageMonths) {}

    @Nullable private UUID holder;
    private String holderName = "";
    private final Map<UUID, Record> records = new HashMap<>();

    public static OldestOneData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OldestOneData::load, OldestOneData::new, NAME);
    }

    private static OldestOneData load(CompoundTag tag) {
        OldestOneData data = new OldestOneData();
        if (tag.hasUUID("holder")) data.holder = tag.getUUID("holder");
        data.holderName = tag.getString("holderName");
        ListTag list = tag.getList("records", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompound(i);
            if (r.hasUUID("id")) data.records.put(r.getUUID("id"), new Record(r.getString("name"), r.getInt("age")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (holder != null) tag.putUUID("holder", holder);
        tag.putString("holderName", holderName);
        ListTag list = new ListTag();
        records.forEach((id, r) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", id);
            c.putString("name", r.name());
            c.putInt("age", r.ageMonths());
            list.add(c);
        });
        tag.put("records", list);
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

    /** Actualiza la edad registrada del mob sellado de un jugador (solo marca cambios si algo cambió). */
    public void setRecord(UUID id, String name, int ageMonths) {
        Record old = records.get(id);
        if (old != null && old.ageMonths() == ageMonths && old.name().equals(name)) return;
        records.put(id, new Record(name, ageMonths));
        setDirty();
    }

    public void removeRecord(UUID id) {
        if (records.remove(id) != null) setDirty();
    }

    /** El jugador con el mob sellado más antiguo de al menos {@code minMonths}, sin contar a {@code exclude}. */
    @Nullable
    public Map.Entry<UUID, Record> oldest(int minMonths, @Nullable UUID exclude) {
        Map.Entry<UUID, Record> best = null;
        for (Map.Entry<UUID, Record> e : records.entrySet()) {
            if (e.getKey().equals(exclude) || e.getValue().ageMonths() < minMonths) continue;
            if (best == null || e.getValue().ageMonths() > best.getValue().ageMonths()) best = e;
        }
        return best;
    }
}
