package com.markadrii.seals.registry;

import com.markadrii.seals.Seals;
import com.markadrii.seals.item.SealingScrollItem;
import com.markadrii.seals.item.AbyssalTridentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Seals.MOD_ID);

    public static final RegistryObject<Item> SEALING_SCROLL = ITEMS.register("sealing_scroll",
            () -> new SealingScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** Tridente abisal: lo da el Elder Guardian con 4 estrellas (no se craftea). */
    public static final RegistryObject<Item> ABYSSAL_TRIDENT = ITEMS.register("abyssal_trident",
            () -> new AbyssalTridentItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));
}
