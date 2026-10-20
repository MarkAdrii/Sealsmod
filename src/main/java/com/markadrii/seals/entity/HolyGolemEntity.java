package com.markadrii.seals.entity;

import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.seal.SealManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Holy golem: un golem espiritual azul invocado por el jugador.
 * Es más rápido que un golem normal, puede saltar sobre bloques y sobre sus enemigos,
 * protege a su dueño y desaparece cuando se le acaba el tiempo.
 */
public class HolyGolemEntity extends IronGolem {
    @Nullable private UUID ownerId;
    private int lifetime = 20 * 90;

    public HolyGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.25F);
        this.setPlayerCreated(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Configura la invocación al crearla (los valores ya vienen escalados por la edad). */
    public void setup(Player owner, int lifetimeTicks, double health, double attack) {
        this.ownerId = owner.getUUID();
        this.lifetime = lifetimeTicks;
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(attack);
        setHealth(getMaxHealth());
    }

    @Nullable
    public Player getOwner() {
        return ownerId == null ? null : level().getPlayerByUUID(ownerId);
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public int getLifetime() {
        return lifetime;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.55F));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 6.0F, 2.5F));
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
        if (target == getOwner()) return false;
        if (target instanceof HolyGolemEntity other && ownerId != null && ownerId.equals(other.ownerId)) return false;
        if (target instanceof Player p && getOwner() != null && getOwner().isAlliedTo(p)) return false;
        return super.canAttack(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.markadrii.seals.client.fx.SealFx.holyAmbient(this));
            return;
        }

        Player owner = getOwner();
        if (--lifetime <= 0 || owner == null || !owner.isAlive() || owner.level() != level()) {
            vanish();
            return;
        }
        // Si se queda muy atrás, aparece junto a su dueño
        if (distanceToSqr(owner) > 24 * 24 && getTarget() == null) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
            getNavigation().stop();
            SealManager.fx(this, S2CFxPacket.Type.HOLY_SUMMON);
        }
    }

    /** Desaparece con efecto (sin soltar nada). */
    public void vanish() {
        if (!level().isClientSide && isAlive()) {
            SealManager.fx(this, S2CFxPacket.Type.HOLY_VANISH);
        }
        discard();
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hitByPlayer) {
        // El espíritu no suelta nada
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        lifetime = tag.getInt("Lifetime");
    }

    // ---------------------------------------------------------------------------------------
    // IA
    // ---------------------------------------------------------------------------------------

    /** Sigue al dueño cuando no está luchando. */
    static class FollowOwnerGoal extends Goal {
        private final HolyGolemEntity golem;
        private final double speed;
        private final float startDistance;
        private final float stopDistance;
        private int recalc;

        FollowOwnerGoal(HolyGolemEntity golem, double speed, float startDistance, float stopDistance) {
            this.golem = golem;
            this.speed = speed;
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            return owner != null && golem.getTarget() == null && golem.distanceToSqr(owner) > startDistance * startDistance;
        }

        @Override
        public boolean canContinueToUse() {
            Player owner = golem.getOwner();
            return owner != null && golem.getTarget() == null && !golem.getNavigation().isDone()
                    && golem.distanceToSqr(owner) > stopDistance * stopDistance;
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

    /** Ataca a quien hiera al dueño. */
    static class DefendOwnerGoal extends TargetGoal {
        private final HolyGolemEntity golem;
        private LivingEntity attacker;
        private int timestamp;

        DefendOwnerGoal(HolyGolemEntity golem) {
            super(golem, false);
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            if (owner == null) return false;
            attacker = owner.getLastHurtByMob();
            int ts = owner.getLastHurtByMobTimestamp();
            return ts != timestamp && canAttack(attacker, TargetingConditions.DEFAULT) && golem.canAttack(attacker);
        }

        @Override
        public void start() {
            mob.setTarget(attacker);
            Player owner = golem.getOwner();
            if (owner != null) timestamp = owner.getLastHurtByMobTimestamp();
            super.start();
        }
    }

    /** Ataca a lo que ataque el dueño. */
    static class HelpOwnerGoal extends TargetGoal {
        private final HolyGolemEntity golem;
        private LivingEntity victim;
        private int timestamp;

        HelpOwnerGoal(HolyGolemEntity golem) {
            super(golem, false);
            this.golem = golem;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player owner = golem.getOwner();
            if (owner == null) return false;
            victim = owner.getLastHurtMob();
            int ts = owner.getLastHurtMobTimestamp();
            return ts != timestamp && canAttack(victim, TargetingConditions.DEFAULT) && golem.canAttack(victim);
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
