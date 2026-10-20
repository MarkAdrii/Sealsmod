package com.markadrii.seals.entity;

import com.markadrii.seals.item.AbyssalTridentItem;
import com.markadrii.seals.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tridente abisal lanzado. Pega más que uno normal y siempre vuelve a su dueño (lealtad),
 * tanto si da a algo como si se clava en el suelo.
 */
public class AbyssalTridentEntity extends ThrownTrident {
    private ItemStack item = ItemStack.EMPTY;
    private boolean returning;
    private boolean returnSound;

    public AbyssalTridentEntity(EntityType<? extends ThrownTrident> type, Level level) {
        super(type, level);
    }

    public AbyssalTridentEntity(Level level, Player owner, ItemStack stack) {
        this(ModEntities.ABYSSAL_TRIDENT.get(), level);
        setOwner(owner);
        setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        this.item = stack.copy();
        this.pickup = Pickup.ALLOWED;
    }

    @Override
    protected ItemStack getPickupItem() {
        return item.copy();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        returning = true;
        if (!level().isClientSide && result.getEntity() instanceof LivingEntity target && target.isAlive()) {
            Entity owner = getOwner();
            target.invulnerableTime = 0;
            target.hurt(damageSources().trident(this, owner == null ? this : owner), AbyssalTridentItem.THROWN_BONUS);
        }
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) returning = true;
        Entity owner = getOwner();
        if (returning && owner != null && !level().isClientSide) {
            if (!owner.isAlive() || (owner instanceof ServerPlayer sp && sp.isSpectator())) {
                if (pickup == Pickup.ALLOWED) spawnAtLocation(getPickupItem(), 0.1F);
                discard();
                return;
            }
            // Lealtad III: vuelve volando a los ojos del dueño
            setNoPhysics(true);
            Vec3 toOwner = owner.getEyePosition().subtract(position());
            setPosRaw(getX(), getY() + toOwner.y * 0.015 * 3, getZ());
            setDeltaMovement(getDeltaMovement().scale(0.95).add(toOwner.normalize().scale(0.05 * 3)));
            if (!returnSound) {
                playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 0.8F);
                returnSound = true;
            }
        }
        super.tick();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("AbyssalItem", item.save(new CompoundTag()));
        tag.putBoolean("Returning", returning);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AbyssalItem")) item = ItemStack.of(tag.getCompound("AbyssalItem"));
        returning = tag.getBoolean("Returning");
    }
}
