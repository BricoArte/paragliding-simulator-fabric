package com.tgskiv.skydiving.registry;

import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.entity.ParagliderEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final Identifier PARAGLIDER_ID =
            Identifier.of(ParaglidingSimulator.MOD_ID, "paraglider");

    // Esto debe ser wildcard en 1.21.x
    public static final RegistryKey<EntityType<?>> PARAGLIDER_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, PARAGLIDER_ID);

    @SuppressWarnings("unchecked")
    public static final EntityType<ParagliderEntity> PARAGLIDER =
            (EntityType<ParagliderEntity>)(Object) Registry.register(
                    Registries.ENTITY_TYPE,
                    PARAGLIDER_ID,
                    FabricEntityTypeBuilder.<ParagliderEntity>create(SpawnGroup.MISC, ParagliderEntity::new)
                            .dimensions(EntityDimensions.fixed(1.5f, 0.65625f))
                            .trackRangeBlocks(128)
                            .trackedUpdateRate(1)
                            .build(PARAGLIDER_KEY)
            );

    public static void register() {
        ParaglidingSimulator.LOGGER.info("Registering ModEntities for " + ParaglidingSimulator.MOD_ID);
    }
}
