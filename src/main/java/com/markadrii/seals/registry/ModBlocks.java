package com.markadrii.seals.registry;

import com.markadrii.seals.Seals;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Bloques del mundo interior (irrompibles, sin objeto). */
public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Seals.MOD_ID);

    private static BlockBehaviour.Properties spirit() {
        return BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .isValidSpawn((s, l, p, e) -> false)
                .pushReaction(PushReaction.BLOCK);
    }

    /** Suelo negro con ondas de agua moviéndose (siempre visible, brilla un poco). */
    public static final RegistryObject<Block> SPIRIT_FLOOR = BLOCKS.register("spirit_floor",
            () -> new Block(spirit().mapColor(MapColor.COLOR_BLACK).sound(SoundType.AMETHYST)
                    .emissiveRendering((s, l, p) -> true)));

    /** Puerta de luz dorada con barrotes. */
    public static final RegistryObject<Block> SPIRIT_DOOR = BLOCKS.register("spirit_door",
            () -> new Block(spirit().mapColor(MapColor.GOLD).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 15).emissiveRendering((s, l, p) -> true)));

    /** Marco y paredes oscuras de la sala de la puerta. */
    public static final RegistryObject<Block> SPIRIT_FRAME = BLOCKS.register("spirit_frame",
            () -> new Block(spirit().mapColor(MapColor.COLOR_BLACK).sound(SoundType.DEEPSLATE)));

    /** Celosía dorada a los lados de la puerta. */
    public static final RegistryObject<Block> SPIRIT_LATTICE = BLOCKS.register("spirit_lattice",
            () -> new Block(spirit().mapColor(MapColor.GOLD).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 10).emissiveRendering((s, l, p) -> true)));
}
