package com.tgskiv.skydiving.registry;

import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.item.ParagliderItem;
import com.tgskiv.skydiving.item.PosterItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import com.tgskiv.skydiving.item.VarioHelmetItem;
import com.tgskiv.skydiving.item.VarioItem;
import net.minecraft.item.equipment.ArmorMaterials;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Identifier PARAGLIDER_ID = Identifier.of(ParaglidingSimulator.MOD_ID, "paraglider");
    public static final RegistryKey<Item> PARAGLIDER_KEY =
            RegistryKey.of(RegistryKeys.ITEM, PARAGLIDER_ID);

    public static final Item PARAGLIDER_ITEM = Registry.register(
            Registries.ITEM,
            PARAGLIDER_ID,
            new ParagliderItem(new Item.Settings().registryKey(PARAGLIDER_KEY).maxCount(1))
    );
    public static final Identifier VARIO_ID = Identifier.of(ParaglidingSimulator.MOD_ID, "vario");
    public static final RegistryKey<Item> VARIO_KEY =
            RegistryKey.of(RegistryKeys.ITEM, VARIO_ID);
    public static final Item VARIO_ITEM = Registry.register(
            Registries.ITEM,
            VARIO_ID,
            new VarioItem(new Item.Settings().registryKey(VARIO_KEY).maxCount(1))
    );

    public static final Identifier POSTER_ID = Identifier.of(ParaglidingSimulator.MOD_ID, "poster");
    public static final RegistryKey<Item> POSTER_KEY =
            RegistryKey.of(RegistryKeys.ITEM, POSTER_ID);
    public static final RegistryKey<PaintingVariant> POSTER_VARIANT_KEY =
            RegistryKey.of(RegistryKeys.PAINTING_VARIANT, Identifier.of(ParaglidingSimulator.MOD_ID, "poster_1"));
    public static final Item POSTER_ITEM = Registry.register(
            Registries.ITEM,
            POSTER_ID,
            new PosterItem(POSTER_VARIANT_KEY, new Item.Settings().registryKey(POSTER_KEY).maxCount(64))
    );

    public static final Identifier VARIO_HELMET_ID = Identifier.of(ParaglidingSimulator.MOD_ID, "vario_helmet");
    public static final RegistryKey<Item> VARIO_HELMET_KEY =
            RegistryKey.of(RegistryKeys.ITEM, VARIO_HELMET_ID);
    public static final Item VARIO_HELMET = Registry.register(
            Registries.ITEM,
            VARIO_HELMET_ID,
            new VarioHelmetItem(ArmorMaterials.IRON, EquipmentType.HELMET, new Item.Settings().registryKey(VARIO_HELMET_KEY))
    );

    public static void register() {
        ParaglidingSimulator.LOGGER.info("Registering ModItems for " + ParaglidingSimulator.MOD_ID);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(PARAGLIDER_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(VARIO_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(VARIO_HELMET));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(POSTER_ITEM));
    }
}
