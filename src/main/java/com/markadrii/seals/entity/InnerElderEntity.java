package com.markadrii.seals.entity;

import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * El Elder Guardian en el mundo interior: flota tras la puerta, te mira y habla contigo.
 * No ataca ni lanza la maldición de fatiga.
 */
public class InnerElderEntity extends ElderGuardian implements InnerMob {
    @Nullable private UUID ownerId;
    private Vec3 home = Vec3.ZERO;

    public InnerElderEntity(EntityType<? extends ElderGuardian> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ElderGuardian.createAttributes();
    }

    @Override
    public void setupInner(Player owner, Vec3 home, double health, double attack) {
        this.ownerId = owner.getUUID();
        this.home = home.add(0, 2.0, 0); // flota sobre el agua
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        setHealth(getMaxHealth());
    }

    @Override
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 24.0F));
    }

    /** Sin maldición de fatiga minera. */
    @Override
    protected void customServerAiStep() {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getUUID().equals(ownerId)) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            BondManager.openDialog(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (ownerId == null || !InnerWorld.isSessionGolem(ownerId, getUUID())) {
            discard();
            return;
        }
        // Se queda flotando en su sitio (fuera del agua los guardianes dan saltos)
        setDeltaMovement(Vec3.ZERO);
        if (position().distanceToSqr(home) > 0.04) setPos(home.x, home.y, home.z);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
