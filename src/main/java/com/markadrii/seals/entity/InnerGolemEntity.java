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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * El Iron Golem sellado, tal como aparece en el mundo interior: grande, tranquilo y con quien se puede hablar.
 * Solo se le puede hacer daño durante una pelea, y nunca muere: al bajar al 15% de vida, la pelea acaba.
 */
public class InnerGolemEntity extends IronGolem {
    public static final float RENDER_SCALE = 1.6F;

    @Nullable private UUID ownerId;
    private Vec3 home = Vec3.ZERO;
    private boolean fighting;

    public InnerGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setPlayerCreated(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 150.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    public void setup(Player owner, Vec3 home, double health, double attack) {
        this.ownerId = owner.getUUID();
        this.home = home;
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(attack);
        setHealth(getMaxHealth());
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
    }

    // --- Pelea ---------------------------------------------------------------------------

    public boolean isFighting() {
        return fighting;
    }

    public void startFight(Player target) {
        fighting = true;
        setHealth(getMaxHealth());
        setTarget(target);
    }

    public void stopFight() {
        fighting = false;
        setTarget(null);
        getNavigation().stop();
        setHealth(getMaxHealth());
        moveTo(home.x, home.y, home.z, 180.0F, 0.0F);
        setYHeadRot(180.0F);
        setYBodyRot(180.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!fighting || level().isClientSide) return false;
        if (source.getEntity() == null || !source.getEntity().getUUID().equals(ownerId)) return false;

        float floor = getMaxHealth() * 0.15F;
        if (getHealth() - amount <= floor) {
            super.hurt(source, Math.max(0, getHealth() - floor));
            setHealth(floor);
            if (getOwner() instanceof ServerPlayer owner) BondManager.endFight(owner, this, true);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return fighting && target.getUUID().equals(ownerId);
    }

    // --- Hablar --------------------------------------------------------------------------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getUUID().equals(ownerId)) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp && !fighting) {
            BondManager.openDialog(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        // Si nadie le está visitando (servidor reiniciado, jugador fuera...), desaparece
        if (ownerId == null || !InnerWorld.isSessionGolem(ownerId, getUUID())) {
            discard();
            return;
        }
        if (!fighting && position().distanceToSqr(home) > 1.0) {
            moveTo(home.x, home.y, home.z, 180.0F, 0.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
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
