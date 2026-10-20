package com.markadrii.seals.entity;

import com.markadrii.seals.network.S2CFxPacket;
import com.markadrii.seals.seal.SealManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Holy golem: un golem espiritual azul invocado por el jugador.
 * Es más rápido que un golem normal, puede saltar sobre bloques y sobre sus enemigos,
 * protege a su dueño y desaparece cuando se le acaba el tiempo.
 */
public class HolyGolemEntity extends OwnedGolemEntity {
    private int lifetime = 20 * 90;

    public HolyGolemEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.25F);
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
        setOwner(owner);
        this.lifetime = lifetimeTicks;
        var hp = getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(health);
        var dmg = getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(attack);
        setHealth(getMaxHealth());
    }

    public int getLifetime() {
        return lifetime;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Lifetime", lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        lifetime = tag.getInt("Lifetime");
    }
}
