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
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** El gato sellado en el mundo interior: un gato negro enorme, sentado, esperándote detrás de la puerta. */
public class InnerCatEntity extends Cat implements InnerMob {
    public static final float RENDER_SCALE = 3.0F;

    private Vec3 home = Vec3.ZERO;

    public InnerCatEntity(EntityType<? extends Cat> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Cat.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    public void setupInner(Player owner, Vec3 home, double health, double attack) {
        this.home = home;
        setVariant(BuiltInRegistries.CAT_VARIANT.getOrThrow(CatVariant.ALL_BLACK));
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

    /** Nunca desaparece solo (ni en dificultad Pacífico): lo quita el mod al salir. */
    @Override
    public void checkDespawn() {
    }
}
