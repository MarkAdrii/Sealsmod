package com.markadrii.seals.seal;

import com.markadrii.seals.Seals;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SealCapability {
    public static final Capability<SealData> SEAL = CapabilityManager.get(new CapabilityToken<>() {});

    /** Datos del sello del jugador (nunca null en el servidor). */
    public static SealData get(Player player) {
        return player.getCapability(SEAL).orElseGet(SealData::new);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {
        private final SealData data = new SealData();
        private final LazyOptional<SealData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == SEAL ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.load(tag);
        }
    }

    @Mod.EventBusSubscriber(modid = Seals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBus {
        @SubscribeEvent
        public static void register(RegisterCapabilitiesEvent event) {
            event.register(SealData.class);
        }
    }

    @Mod.EventBusSubscriber(modid = Seals.MOD_ID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(Seals.id("seal"), new Provider());
            }
        }

        /** El sello se conserva al morir y al volver del End. */
        @SubscribeEvent
        public static void clone(PlayerEvent.Clone event) {
            Player original = event.getOriginal();
            original.reviveCaps();
            original.getCapability(SEAL).ifPresent(old ->
                    event.getEntity().getCapability(SEAL).ifPresent(now -> {
                        now.copyFrom(old);
                        if (event.isWasDeath()) {
                            // Al morir se apagan las transformaciones
                            now.setManto(false);
                            now.setGolemForm(false);
                            now.setGolemFormTicks(0);
                            now.setHolyGolem(null);
                        }
                    }));
            original.invalidateCaps();
        }
    }
}
