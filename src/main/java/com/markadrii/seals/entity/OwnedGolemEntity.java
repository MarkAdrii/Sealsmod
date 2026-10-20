package com.markadrii.seals.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Golem que pertenece a un jugador: le sigue, le defiende, ataca a lo que él ataca y a los monstruos.
 * No suelta nada al morir. Base del Holy golem y del Mini golem.
 */
public abstract class OwnedGolemEntity extends IronGolem {
    @Nullable protected UUID ownerId;

    protected OwnedGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setPlayerCreated(true);
        this.xpReward = 0;
    }

    public void setOwner(Player owner) {
        this.ownerId = owner.getUUID();
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public boolean isOwnedBy(Player player) {
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    /** Distancia a la que empieza a seguir al dueño y a la que para. */
    protected float followStart() { return 6.0F; }
    protected float followStop() { return 2.5F; }
    protected boolean canLeap() { return true; }

    @Override
    protected void registerGoals() {
        if (canLeap()) this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.55F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new DefendOwnerGoal(this));
        this.targetSelector.addGoal(2, new HelpOwnerGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Mob.class, 5, false, false,
                e -> e instanceof Enemy && !(e instanceof Creeper)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        Player owner = getOwner();
        if (target == owner) return false;
        if (target instanceof OwnedGolemEntity other && ownerId != null && ownerId.equals(other.ownerId)) return false;
        if (target instanceof Player p && owner != null && owner.isAlliedTo(p)) return false;
        return super.canAttack(target);
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        // No suelta nada
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

    // ---------------------------------------------------------------------------------------
    // IA
    // ---------------------------------------------------------------------------------------

    static class FollowOwnerGoal extends Goal {
        private final OwnedGolemEntity golem;
        private final double speed;
        private int recalc;

        FollowOwnerGoal(OwnedGolemEntity golem, double speed) {
            this.golem = golem;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            float d = golem.followStart();
            return owner != null && golem.getTarget() == null && golem.distanceToSqr(owner) > d * d;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = golem.getOwner();
            float d = golem.followStop();
            return owner != null && golem.getTarget() == null && !golem.getNavigation().isDone()
                    && golem.distanceToSqr(owner) > d * d;
        }

        @Override
        public void start() {
            recalc = 0;
        }

        @Override
        public void stop() {
            golem.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player owner = golem.getOwner();
            if (owner == null) return;
            golem.getLookControl().setLookAt(owner, 10.0F, golem.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = 10;
                golem.getNavigation().moveTo(owner, speed);
            }
        }
    }

    static class DefendOwnerGoal extends TargetGoal {
        private final OwnedGolemEntity golem;
        private LivingEntity attacker;
        private int timestamp;

        DefendOwnerGoal(OwnedGolemEntity golem) {
            super(golem, false);
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            if (owner == null) return false;
            attacker = owner.getLastHurtByMob();
            return owner.getLastHurtByMobTimestamp() != timestamp
                    && canAttack(attacker, TargetingConditions.DEFAULT) && golem.canAttack(attacker);
        }

        @Override
        public void start() {
            mob.setTarget(attacker);
            Player owner = golem.getOwner();
            if (owner != null) timestamp = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    static class HelpOwnerGoal extends TargetGoal {
        private final OwnedGolemEntity golem;
        private LivingEntity victim;
        private int timestamp;

        HelpOwnerGoal(OwnedGolemEntity golem) {
            super(golem, false);
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            if (owner == null) return false;
            victim = owner.getLastHurtMob();
            return owner.getLastHurtMobTimestamp() != timestamp
                    && canAttack(victim, TargetingConditions.DEFAULT) && golem.canAttack(victim);
        }

        @Override
        public void start() {
            mob.setTarget(victim);
            Player owner = golem.getOwner();
            if (owner != null) timestamp = owner.getLastHurtMobTimestamp();
            super.start();
        }
    }
}
