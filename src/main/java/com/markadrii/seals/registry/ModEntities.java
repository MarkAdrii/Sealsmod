package com.markadrii.seals.registry;

import com.markadrii.seals.Seals;
import com.markadrii.seals.entity.HolyGolemEntity;
import com.markadrii.seals.entity.InnerGolemEntity;
import com.markadrii.seals.entity.MiniGolemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Seals.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Seals.MOD_ID);

    public static final RegistryObject<EntityType<HolyGolemEntity>> HOLY_GOLEM = ENTITIES.register("holy_golem",
            () -> EntityType.Builder.<HolyGolemEntity>of(HolyGolemEntity::new, MobCategory.MISC)
                    .sized(1.4F, 2.7F)
                    .clientTrackingRange(10)
                    .build(Seals.id("holy_golem").toString()));

    public static final RegistryObject<EntityType<MiniGolemEntity>> MINI_GOLEM = ENTITIES.register("mini_golem",
            () -> EntityType.Builder.<MiniGolemEntity>of(MiniGolemEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.95F)
                    .clientTrackingRange(10)
                    .build(Seals.id("mini_golem").toString()));

    public static final RegistryObject<EntityType<InnerGolemEntity>> INNER_GOLEM = ENTITIES.register("inner_golem",
            () -> EntityType.Builder.<InnerGolemEntity>of(InnerGolemEntity::new, MobCategory.MISC)
                    .sized(1.4F * InnerGolemEntity.RENDER_SCALE, 2.7F * InnerGolemEntity.RENDER_SCALE)
                    .clientTrackingRange(10)
                    .build(Seals.id("inner_golem").toString()));

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLY_GOLEM.get(), HolyGolemEntity.createAttributes().build());
        event.put(MINI_GOLEM.get(), MiniGolemEntity.createAttributes().build());
        event.put(INNER_GOLEM.get(), InnerGolemEntity.createAttributes().build());
    }
}
