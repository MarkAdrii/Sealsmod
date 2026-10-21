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
    private boolean oldestOne;     // rango oculto: primer jugador del mundo en llegar a 800 años

    // --- Habilidades ---
    private boolean manto;
    private boolean golemForm;
    private int golemFormTicks;
    private int golemFormCooldown;
    @Nullable private UUID holyGolem;
    private int holyCooldown;
    private int abilityUseTicks;

    // --- Habilidades del lobo ---
    private boolean instinct;          // Instinto primal
    private boolean werewolf;          // Hombre lobo
    private int werewolfTicks;
    private int werewolfCooldown;
    private long lastGiftDay = -100;   // Gato: último regalo matutino

    // --- Elder Guardian ---
    private int submergedTicks;        // ticks bajo el agua hoy
    private long dryDay = -1;          // último día comprobado
    private boolean dry;               // se está secando: pierde estabilidad
    private boolean abyssal;           // Forma abisal
    private int beamTicks;             // carga del rayo
    private int beamCooldown;

    // --- Vínculo (hablar, mundo interior, boost) ---
    private long lastTalkDay = -100;
    private long lastEnterDay = -100;
    private long lastBondDay = -100;
    private int visitFlags;            // opciones ya usadas en la visita actual al mundo interior
    private boolean inInner;           // está dentro del mundo interior
    private String returnDim = "";
    private double returnX, returnY, returnZ;
    private float returnYaw;
    private int boostTicks;            // boost del vínculo (BOUND)
    private boolean fighting;          // peleando con el mob en el mundo interior
    private boolean innerKeyGiven;     // ya entregó lo que pide el mob para abrir la puerta

    // --- Mini golem ---
    private boolean miniOut;           // el mini golem está invocado (en el mundo o en el hombro)
    private boolean miniOnShoulder;
    @Nullable private UUID miniId;

    /** Solo para el cliente: el día actual de Minecraft (lo rellena el servidor al sincronizar). */
    private long today;

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

    /** Quita el sello y resetea todo menos los huecos desbloqueados (el título se gestiona en SealManager.onSealLost). */
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
        submergedTicks = 0;
        dryDay = -1;
        dry = false;
        abyssal = false;
        beamTicks = 0;
        beamCooldown = 0;
        innerKeyGiven = false;
        instinct = false;
        werewolf = false;
        werewolfTicks = 0;
        werewolfCooldown = 0;
        lastGiftDay = -100;
        lastTalkDay = -100;
        lastEnterDay = -100;
        lastBondDay = -100;
        visitFlags = 0;
        boostTicks = 0;
        fighting = false;
        miniOut = false;
        miniOnShoulder = false;
        miniId = null;
        // inInner y el punto de vuelta se conservan: hacen falta para sacar al jugador
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
        tag.putBoolean("oldestOne", oldestOne);
        tag.putBoolean("manto", manto);
        tag.putBoolean("golemForm", golemForm);
        tag.putInt("golemFormTicks", golemFormTicks);
        tag.putInt("golemFormCooldown", golemFormCooldown);
        if (holyGolem != null) tag.putUUID("holyGolem", holyGolem);
        tag.putInt("holyCooldown", holyCooldown);
        tag.putInt("abilityUseTicks", abilityUseTicks);
        tag.putInt("submergedTicks", submergedTicks);
        tag.putLong("dryDay", dryDay);
        tag.putBoolean("dry", dry);
        tag.putBoolean("abyssal", abyssal);
        tag.putInt("beamTicks", beamTicks);
        tag.putInt("beamCooldown", beamCooldown);
        tag.putBoolean("innerKeyGiven", innerKeyGiven);
        tag.putBoolean("instinct", instinct);
        tag.putBoolean("werewolf", werewolf);
        tag.putInt("werewolfTicks", werewolfTicks);
        tag.putInt("werewolfCooldown", werewolfCooldown);
        tag.putLong("lastGiftDay", lastGiftDay);
        tag.putLong("lastTalkDay", lastTalkDay);
        tag.putLong("lastEnterDay", lastEnterDay);
        tag.putLong("lastBondDay", lastBondDay);
        tag.putInt("visitFlags", visitFlags);
        tag.putBoolean("inInner", inInner);
        tag.putString("returnDim", returnDim);
        tag.putDouble("returnX", returnX);
        tag.putDouble("returnY", returnY);
        tag.putDouble("returnZ", returnZ);
        tag.putFloat("returnYaw", returnYaw);
        tag.putInt("boostTicks", boostTicks);
        tag.putBoolean("fighting", fighting);
        tag.putBoolean("miniOut", miniOut);
        tag.putBoolean("miniOnShoulder", miniOnShoulder);
        if (miniId != null) tag.putUUID("miniId", miniId);
        tag.putLong("today", today);
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
        oldestOne = tag.getBoolean("oldestOne");
        manto = tag.getBoolean("manto");
        golemForm = tag.getBoolean("golemForm");
        golemFormTicks = tag.getInt("golemFormTicks");
        golemFormCooldown = tag.getInt("golemFormCooldown");
        holyGolem = tag.hasUUID("holyGolem") ? tag.getUUID("holyGolem") : null;
        holyCooldown = tag.getInt("holyCooldown");
        abilityUseTicks = tag.getInt("abilityUseTicks");
        submergedTicks = tag.getInt("submergedTicks");
        dryDay = tag.contains("dryDay") ? tag.getLong("dryDay") : -1;
        dry = tag.getBoolean("dry");
        abyssal = tag.getBoolean("abyssal");
        beamTicks = tag.getInt("beamTicks");
        beamCooldown = tag.getInt("beamCooldown");
        innerKeyGiven = tag.getBoolean("innerKeyGiven");
        instinct = tag.getBoolean("instinct");
        werewolf = tag.getBoolean("werewolf");
        werewolfTicks = tag.getInt("werewolfTicks");
        werewolfCooldown = tag.getInt("werewolfCooldown");
        lastGiftDay = tag.contains("lastGiftDay") ? tag.getLong("lastGiftDay") : -100;
        lastTalkDay = tag.contains("lastTalkDay") ? tag.getLong("lastTalkDay") : -100;
        lastEnterDay = tag.contains("lastEnterDay") ? tag.getLong("lastEnterDay") : -100;
        lastBondDay = tag.contains("lastBondDay") ? tag.getLong("lastBondDay") : -100;
        visitFlags = tag.getInt("visitFlags");
        inInner = tag.getBoolean("inInner");
        returnDim = tag.getString("returnDim");
        returnX = tag.getDouble("returnX");
        returnY = tag.getDouble("returnY");
        returnZ = tag.getDouble("returnZ");
        returnYaw = tag.getFloat("returnYaw");
        boostTicks = tag.getInt("boostTicks");
        fighting = tag.getBoolean("fighting");
        miniOut = tag.getBoolean("miniOut");
        miniOnShoulder = tag.getBoolean("miniOnShoulder");
        miniId = tag.hasUUID("miniId") ? tag.getUUID("miniId") : null;
        today = tag.getLong("today");
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
    public void setAgeMonths(int ageMonths) {
        this.ageMonths = Math.max(0, Math.min(AgeStage.MAX_YEARS * 12, ageMonths));
    }

    public AgeStage ageStage() {
        return AgeStage.of(ageYears(), oldestOne);
    }

    public boolean oldestOne() { return oldestOne; }
    public void setOldestOne(boolean value) { this.oldestOne = value; }
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

    public int submergedTicks() { return submergedTicks; }
    public void setSubmergedTicks(int t) { this.submergedTicks = t; }
    public long dryDay() { return dryDay; }
    public void setDryDay(long d) { this.dryDay = d; }
    public boolean dry() { return dry; }
    public void setDry(boolean v) { this.dry = v; }
    public boolean abyssal() { return abyssal; }
    public void setAbyssal(boolean v) { this.abyssal = v; }
    public int beamTicks() { return beamTicks; }
    public void setBeamTicks(int t) { this.beamTicks = t; }
    public int beamCooldown() { return beamCooldown; }
    public void setBeamCooldown(int t) { this.beamCooldown = t; }
    public boolean innerKeyGiven() { return innerKeyGiven; }
    public void setInnerKeyGiven(boolean v) { this.innerKeyGiven = v; }

    public boolean instinct() { return instinct; }
    public void setInstinct(boolean v) { this.instinct = v; }
    public boolean werewolf() { return werewolf; }
    public void setWerewolf(boolean v) { this.werewolf = v; }
    public int werewolfTicks() { return werewolfTicks; }
    public void setWerewolfTicks(int t) { this.werewolfTicks = t; }
    public int werewolfCooldown() { return werewolfCooldown; }
    public void setWerewolfCooldown(int t) { this.werewolfCooldown = t; }

    public long lastGiftDay() { return lastGiftDay; }
    public void setLastGiftDay(long d) { this.lastGiftDay = d; }

    public long lastTalkDay() { return lastTalkDay; }
    public void setLastTalkDay(long d) { this.lastTalkDay = d; }
    public long lastEnterDay() { return lastEnterDay; }
    public void setLastEnterDay(long d) { this.lastEnterDay = d; }
    public long lastBondDay() { return lastBondDay; }
    public void setLastBondDay(long d) { this.lastBondDay = d; }
    public int visitFlags() { return visitFlags; }
    public void setVisitFlags(int f) { this.visitFlags = f; }
    public boolean visitDone(int bit) { return (visitFlags & bit) != 0; }
    public void markVisit(int bit) { this.visitFlags |= bit; }
    public boolean inInner() { return inInner; }
    public void setInInner(boolean v) { this.inInner = v; }
    public String returnDim() { return returnDim; }
    public double returnX() { return returnX; }
    public double returnY() { return returnY; }
    public double returnZ() { return returnZ; }
    public float returnYaw() { return returnYaw; }
    public void setReturn(String dim, double x, double y, double z, float yaw) {
        this.returnDim = dim;
        this.returnX = x;
        this.returnY = y;
        this.returnZ = z;
        this.returnYaw = yaw;
    }
    public int boostTicks() { return boostTicks; }
    public void setBoostTicks(int t) { this.boostTicks = t; }
    public boolean fighting() { return fighting; }
    public void setFighting(boolean v) { this.fighting = v; }
    public boolean miniOut() { return miniOut; }
    public void setMiniOut(boolean v) { this.miniOut = v; }
    public boolean miniOnShoulder() { return miniOnShoulder; }
    public void setMiniOnShoulder(boolean v) { this.miniOnShoulder = v; }
    @Nullable public UUID miniId() { return miniId; }
    public void setMiniId(@Nullable UUID id) { this.miniId = id; }
    public long today() { return today; }
    public void setToday(long d) { this.today = d; }
}
