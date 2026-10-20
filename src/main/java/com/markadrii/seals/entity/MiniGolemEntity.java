package com.markadrii.seals.entity;

import com.markadrii.seals.bond.BondManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Mini iron golem: una pequeña invocación permanente que protege a su dueño.
 * Si muere, el sello pierde un 50% de estabilidad. Con shift + clic derecho se sube al hombro.
 */
public class MiniGolemEntity extends OwnedGolemEntity {
    public static final float RENDER_SCALE = 0.35F;

    public MiniGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return IronGolem.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    public void setup(Player owner, double health, double attack) {
        setOwner(owner);
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(attack);
        setHealth(getMaxHealth());
    }

    @Override
    protected float followStart() { return 4.0F; }

    @Override
    protected float followStop() { return 1.8F; }

    @Override
    protected boolean canLeap() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        Player owner = getOwner();
        // Si el dueño no está en esta dimensión, desaparece sin penalización (vuelve a aparecer con él)
        if (owner == null || owner.level() != level() || !BondManager.isActiveMini(owner, this)) {
            discard();
            return;
        }
        if (distanceToSqr(owner) > 20 * 20 && getTarget() == null) {
            moveTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
            getNavigation().stop();
        }
    }
}
