package com.markadrii.seals.seal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Todo lo que el jugador lleva sellado dentro.
 * Por ahora solo hay un hueco de sello; {@link #maxSlots} queda preparado para el futuro (hasta 3).
 */
public class SealData {
    // --- Sello ---
    @Nullable private ResourceLocation sealType;
    private int affinity;          // 0-100 dentro del nivel actual
    private int level = 1;         // nivel de afecto (1 = sin estrellas)
    private int stability = 100;   // 0-100
    private int ageMonths;         // edad del mob sellado en meses
    private long lastDay = -1;     // último día de Minecraft contado
    private int maxSlots = 1;      // huecos de sello desbloqueados (futuro: hasta 3)

    // --- Habilidades ---
    private boolean manto;
    private boolean golemForm;
    private int golemFormTicks;
    private int golemFormCooldown;
    @Nullable private UUID holyGolem;
    private int holyCooldown;
    private int abilityUseTicks;

    public boolean hasSeal() {
        return sealType != null;
    }

    @Nullable
    public SealDefinition definition() {
        return sealType == null ? null : SealRegistry.get(sealType);
    }

    /** Empieza un sello nuevo desde cero. */
    public void seal(ResourceLocation type, int ageYears, long currentDay) {
        clearSeal();
        this.sealType = type;
        this.ageMonths = ageYears * 12;
        this.lastDay = currentDay;
    }

    /** Quita el sello y resetea todo menos los huecos desbloqueados. */
    public void clearSeal() {
        sealType = null;
        affinity = 0;
        level = 1;
        stability = 100;
        ageMonths = 0;
        lastDay = -1;
        manto = false;
        golemForm = false;
        golemFormTicks = 0;
        golemFormCooldown = 0;
        holyGolem = null;
        holyCooldown = 0;
        abilityUseTicks = 0;
    }

    public int ageYears() {
        return ageMonths / 12;
    }

    /** Multiplicador por edad: 20 años = x1.10, 100 años = x1.50. */
    public float ageScale() {
        return 1.0F + ageYears() / 200.0F;
    }

    public void copyFrom(SealData other) {
        load(other.save());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (sealType != null) tag.putString("type", sealType.toString());
        tag.putInt("affinity", affinity);
        tag.putInt("level", level);
        tag.putInt("stability", stability);
        tag.putInt("ageMonths", ageMonths);
        tag.putLong("lastDay", lastDay);
        tag.putInt("maxSlots", maxSlots);
        tag.putBoolean("manto", manto);
        tag.putBoolean("golemForm", golemForm);
        tag.putInt("golemFormTicks", golemFormTicks);
        tag.putInt("golemFormCooldown", golemFormCooldown);
        if (holyGolem != null) tag.putUUID("holyGolem", holyGolem);
        tag.putInt("holyCooldown", holyCooldown);
        tag.putInt("abilityUseTicks", abilityUseTicks);
        return tag;
    }

    public void load(CompoundTag tag) {
        sealType = tag.contains("type") ? ResourceLocation.tryParse(tag.getString("type")) : null;
        affinity = tag.getInt("affinity");
        level = Math.max(1, tag.getInt("level"));
        stability = tag.contains("stability") ? tag.getInt("stability") : 100;
        ageMonths = tag.getInt("ageMonths");
        lastDay = tag.contains("lastDay") ? tag.getLong("lastDay") : -1;
        maxSlots = Math.max(1, tag.getInt("maxSlots"));
        manto = tag.getBoolean("manto");
        golemForm = tag.getBoolean("golemForm");
        golemFormTicks = tag.getInt("golemFormTicks");
        golemFormCooldown = tag.getInt("golemFormCooldown");
        holyGolem = tag.hasUUID("holyGolem") ? tag.getUUID("holyGolem") : null;
        holyCooldown = tag.getInt("holyCooldown");
        abilityUseTicks = tag.getInt("abilityUseTicks");
    }

    // --- Getters / setters ---
    @Nullable public ResourceLocation sealType() { return sealType; }
    public int affinity() { return affinity; }
    public void setAffinity(int affinity) { this.affinity = affinity; }
    public int level() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int stability() { return stability; }
    public void setStability(int stability) { this.stability = Math.max(0, Math.min(100, stability)); }
    public int ageMonths() { return ageMonths; }
    public void setAgeMonths(int ageMonths) { this.ageMonths = ageMonths; }
    public long lastDay() { return lastDay; }
    public void setLastDay(long lastDay) { this.lastDay = lastDay; }
    public int maxSlots() { return maxSlots; }
    public boolean manto() { return manto; }
    public void setManto(boolean manto) { this.manto = manto; }
    public boolean golemForm() { return golemForm; }
    public void setGolemForm(boolean golemForm) { this.golemForm = golemForm; }
    public int golemFormTicks() { return golemFormTicks; }
    public void setGolemFormTicks(int t) { this.golemFormTicks = t; }
    public int golemFormCooldown() { return golemFormCooldown; }
    public void setGolemFormCooldown(int t) { this.golemFormCooldown = t; }
    @Nullable public UUID holyGolem() { return holyGolem; }
    public void setHolyGolem(@Nullable UUID id) { this.holyGolem = id; }
    public int holyCooldown() { return holyCooldown; }
    public void setHolyCooldown(int t) { this.holyCooldown = t; }
    public int abilityUseTicks() { return abilityUseTicks; }
    public void setAbilityUseTicks(int t) { this.abilityUseTicks = t; }
}
