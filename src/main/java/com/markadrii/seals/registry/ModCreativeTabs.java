package com.markadrii.seals.registry;

import com.markadrii.seals.Seals;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Seals.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SEALS_TAB = TABS.register("seals", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.seals"))
            .icon(() -> new ItemStack(ModItems.SEALING_SCROLL.get()))
            .displayItems((params, output) -> output.accept(ModItems.SEALING_SCROLL.get()))
            .build());
}
