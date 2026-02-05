package com.tgskiv.skydiving.registry;


import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.block.WindsockBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

import static com.tgskiv.skydiving.block.WindsockBlock.ITEM_KEY;

public class ModBlocks {

    // Create and register the Windsock block instance
    public static final Block WINDSOCK = registerBlock(
            WindsockBlock.NAME,
            new WindsockBlock(AbstractBlock.Settings
                    .create()
                    .solid()
                    .strength(2.0f) // Adjust hardness
                    .sounds(BlockSoundGroup.WOOD) // Adjust sounds
                    .nonOpaque()
                    .registryKey(WindsockBlock.BLOCK_KEY)
    ));


    public static final Item WINDSOCK_ITEM = registerBlockItem(
            WindsockBlock.NAME,
            new BlockItem(WINDSOCK, new Item.Settings().registryKey(ITEM_KEY))
    );



    // Helper method to register block and associated item
    private static Block registerBlock(String name, Block block) {
        return Registry.register(
                Registries.BLOCK,
                Identifier.of(ParaglidingSimulator.MOD_ID, name),
                block
        );
    }

    private static Item registerBlockItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(ParaglidingSimulator.MOD_ID, name),
                item
        );
    }

    // Called from your main mod initializer
    public static void registerModBlocks() {
        ParaglidingSimulator.LOGGER.info("Registering ModBlocks for " + ParaglidingSimulator.MOD_ID);
        // AÃ±adir al inventario creativo
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> entries.add(WINDSOCK_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(WINDSOCK_ITEM));
    }
}
