package com.tgskiv;

import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.menu.ModScreenHandlers;
import com.tgskiv.skydiving.network.LaunchSiteDebugPayload;
import com.tgskiv.skydiving.network.ToggleAirflowDebugPayload;
import com.tgskiv.skydiving.network.ThermalSyncPayload;
import com.tgskiv.skydiving.network.WindConfigSyncPayload;
import com.tgskiv.skydiving.network.WindSyncPayload;
import com.tgskiv.skydiving.registry.ModBlockEntities;
import com.tgskiv.skydiving.registry.ModBlocks;
import com.tgskiv.skydiving.worldgen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tgskiv.skydiving.registry.ModEntities;
import com.tgskiv.skydiving.registry.ModItems;

public class ParaglidingSimulator implements ModInitializer {
    public static final String MOD_ID = "paraglidingsimulator";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world (main)!");

        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModEntities.register();
        ModItems.register();
        ModScreenHandlers.register();
        ModWorldGen.register();

        SkydivingHandler.register();
        PayloadTypeRegistry.playS2C().register(WindSyncPayload.PAYLOAD_ID, WindSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ToggleAirflowDebugPayload.PAYLOAD_ID, ToggleAirflowDebugPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WindConfigSyncPayload.PAYLOAD_ID, WindConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LaunchSiteDebugPayload.PAYLOAD_ID, LaunchSiteDebugPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ThermalSyncPayload.PAYLOAD_ID, ThermalSyncPayload.CODEC);
    }
}
