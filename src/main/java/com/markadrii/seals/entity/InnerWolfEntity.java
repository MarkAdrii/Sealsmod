package com.markadrii.seals.entity;

import com.markadrii.seals.bond.BondManager;
import com.markadrii.seals.world.InnerWorld;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** El lobo sellado en el mundo interior: enorme, sentado, esperándote detrás de la puerta. */
public class InnerWolfEntity extends Wolf implements InnerMob {
    public static final float RENDER_SCALE = 2.6F;

    private Vec3 home = Vec3.ZERO;

    public InnerWolfEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    public void setupInner(Player owner, Vec3 home, double health, double attack) {
        this.home = home;
        tame(owner);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        setHealth(getMaxHealth());
        setOrderedToSit(true);
        setInSittingPose(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isOwnedBy(player)) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            BondManager.openDialog(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (getOwnerUUID() == null || !InnerWorld.isSessionGolem(getOwnerUUID(), getUUID())) {
            discard();
            return;
        }
        if (!isInSittingPose()) {
            setOrderedToSit(true);
            setInSittingPose(true);
        }
        if (position().distanceToSqr(home) > 1.0) {
            moveTo(home.x, home.y, home.z, 180.0F, 0.0F);
        }
    }

    @Override
    public java.util.UUID getOwnerId() {
        return getOwnerUUID();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
