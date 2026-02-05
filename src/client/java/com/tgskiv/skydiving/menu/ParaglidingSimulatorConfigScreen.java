package com.tgskiv.skydiving.menu;

import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import com.tgskiv.skydiving.network.WindConfigSyncPayload;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.LoggerFactory;

public final class ParaglidingSimulatorConfigScreen {
    private ParaglidingSimulatorConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("menu.paraglidingsimulator.title"));

        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("menu.paraglidingsimulator.wind_settings"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder.startIntField(Text.translatable("menu.paraglidingsimulator.ticks_per_wind_change"), SkydivingClientConfig.ticksPerWindChange)
                .setDefaultValue(300)
                .setSaveConsumer(newValue -> SkydivingClientConfig.ticksPerWindChange = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Text.translatable("menu.paraglidingsimulator.wind_rotation_degrees"), SkydivingClientConfig.windRotationDegrees)
                .setDefaultValue(20.0)
                .setSaveConsumer(newValue -> SkydivingClientConfig.windRotationDegrees = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Text.translatable("menu.paraglidingsimulator.max_speed_delta"), SkydivingClientConfig.maxSpeedDelta)
                .setDefaultValue(0.025)
                .setSaveConsumer(newValue -> SkydivingClientConfig.maxSpeedDelta = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Text.translatable("menu.paraglidingsimulator.max_wind_speed"), SkydivingClientConfig.maxWindSpeed)
                .setDefaultValue(0.1)
                .setSaveConsumer(newValue -> SkydivingClientConfig.maxWindSpeed = newValue)
                .build());

        general.addEntry(entryBuilder.startDoubleField(Text.translatable("menu.paraglidingsimulator.min_wind_speed"), SkydivingClientConfig.minWindSpeed)
                .setDefaultValue(0.0)
                .setSaveConsumer(newValue -> SkydivingClientConfig.minWindSpeed = newValue)
                .build());

        SkydivingServerConfig serverCfg = (MinecraftClient.getInstance().getServer() != null)
                ? SkydivingHandler.getServerConfig()
                : new SkydivingServerConfig();

        final SkydivingServerConfig.ThermalAmountPreset[] thermalAmount =
                { serverCfg.thermalAmountPreset };
        final SkydivingServerConfig.ThermalIntensityPreset[] thermalIntensity =
                { serverCfg.thermalIntensityPreset };
        final SkydivingServerConfig.ThermalSizePreset[] thermalSize =
                { serverCfg.thermalSizePreset };
        final SkydivingServerConfig.ThermalHeightPreset[] thermalHeight =
                { serverCfg.thermalHeightPreset };
        final SkydivingServerConfig.ThermalGenerationDistancePreset[] thermalGenerationDistance =
                { serverCfg.thermalGenerationDistancePreset };
        final SkydivingServerConfig.LaunchGenerationPreset[] launchGeneration =
                { serverCfg.launchGenerationPreset };
        final boolean[] thermalsEnabled = { serverCfg.thermalsEnabled };

        ConfigCategory thermals = builder.getOrCreateCategory(
                Text.translatable("menu.paraglidingsimulator.thermal_settings")
        );
        thermals.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("menu.paraglidingsimulator.thermals_enabled"),
                        thermalsEnabled[0])
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> thermalsEnabled[0] = newValue)
                .build());
        thermals.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.thermal_intensity"),
                        SkydivingServerConfig.ThermalIntensityPreset.class,
                        thermalIntensity[0])
                .setDefaultValue(SkydivingServerConfig.ThermalIntensityPreset.STANDARD)
                .setEnumNameProvider(value -> Text.translatable("menu.paraglidingsimulator.thermal_intensity." + value.name().toLowerCase()))
                .setSaveConsumer(newValue -> thermalIntensity[0] = newValue)
                .build());
        thermals.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.thermal_size"),
                        SkydivingServerConfig.ThermalSizePreset.class,
                        thermalSize[0])
                .setDefaultValue(SkydivingServerConfig.ThermalSizePreset.STANDARD)
                .setEnumNameProvider(value -> Text.translatable("menu.paraglidingsimulator.thermal_size." + value.name().toLowerCase()))
                .setSaveConsumer(newValue -> thermalSize[0] = newValue)
                .build());
        thermals.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.thermal_height"),
                        SkydivingServerConfig.ThermalHeightPreset.class,
                        thermalHeight[0])
                .setDefaultValue(SkydivingServerConfig.ThermalHeightPreset.STANDARD)
                .setEnumNameProvider(value -> Text.translatable("menu.paraglidingsimulator.thermal_height." + value.name().toLowerCase()))
                .setSaveConsumer(newValue -> thermalHeight[0] = newValue)
                .build());

        ConfigCategory performance = builder.getOrCreateCategory(
                Text.translatable("menu.paraglidingsimulator.performance_settings")
        );
        performance.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.launch_generation"),
                        SkydivingServerConfig.LaunchGenerationPreset.class,
                        launchGeneration[0])
                .setDefaultValue(SkydivingServerConfig.LaunchGenerationPreset.STANDARD)
                .setEnumNameProvider(value -> Text.translatable("menu.paraglidingsimulator.launch_generation." + value.name().toLowerCase()))
                .setTooltip(Text.translatable("menu.paraglidingsimulator.launch_generation.tip"))
                .setSaveConsumer(newValue -> launchGeneration[0] = newValue)
                .build());
        performance.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.thermal_amount"),
                        SkydivingServerConfig.ThermalAmountPreset.class,
                        thermalAmount[0])
                .setDefaultValue(SkydivingServerConfig.ThermalAmountPreset.STANDARD)
                .setEnumNameProvider(value -> Text.translatable("menu.paraglidingsimulator.thermal_amount." + value.name().toLowerCase()))
                .setTooltip(Text.translatable("menu.paraglidingsimulator.thermal_amount.tip"))
                .setSaveConsumer(newValue -> thermalAmount[0] = newValue)
                .build());
        performance.addEntry(entryBuilder.startEnumSelector(
                        Text.translatable("menu.paraglidingsimulator.thermal_generation_distance"),
                        SkydivingServerConfig.ThermalGenerationDistancePreset.class,
                        thermalGenerationDistance[0])
                .setDefaultValue(SkydivingServerConfig.ThermalGenerationDistancePreset.RENDER_DISTANCE)
                .setEnumNameProvider(value -> {
                    if (value == SkydivingServerConfig.ThermalGenerationDistancePreset.SIMULATION_DISTANCE) {
                        return Text.translatable("menu.paraglidingsimulator.thermal_generation_distance.simulation_short");
                    }
                    return Text.translatable("menu.paraglidingsimulator.thermal_generation_distance.render_short");
                })
                .setTooltip(
                        Text.translatable("menu.paraglidingsimulator.thermal_generation_distance.render", getCurrentChunkDistances().renderDistance()),
                        Text.translatable("menu.paraglidingsimulator.thermal_generation_distance.simulation", getCurrentChunkDistances().simulationDistance())
                )
                .setSaveConsumer(newValue -> thermalGenerationDistance[0] = newValue)
                .build());

        builder.setSavingRunnable(() -> {
            if (MinecraftClient.getInstance().world == null) {
                LoggerFactory.getLogger("paraglidingsimulator").warn("Failed to send configuration to server: No connection available");
                return;
            }
            try {
                ClientPlayNetworking.send(new WindConfigSyncPayload(
                        SkydivingClientConfig.ticksPerWindChange,
                        SkydivingClientConfig.windRotationDegrees,
                        SkydivingClientConfig.maxSpeedDelta,
                        SkydivingClientConfig.maxWindSpeed,
                        SkydivingClientConfig.minWindSpeed
                ));
            } catch (Exception e) {
                LoggerFactory.getLogger("paraglidingsimulator").warn("Failed to send message to server: No connection available", e);
            }

            if (MinecraftClient.getInstance().isInSingleplayer() && MinecraftClient.getInstance().getServer() != null) {
                MinecraftClient.getInstance().getServer().execute(() -> {
                    SkydivingHandler.applyThermalsEnabled(thermalsEnabled[0]);
                    SkydivingHandler.applyThermalPresets(
                            thermalAmount[0],
                            thermalIntensity[0],
                            thermalSize[0],
                            thermalHeight[0]
                    );
                    SkydivingHandler.applyThermalGenerationDistancePreset(thermalGenerationDistance[0]);
                    SkydivingHandler.applyLaunchGenerationPreset(launchGeneration[0]);
                });
            }
        });

        LoggerFactory.getLogger("paraglidingsimulator").info("Building the paraglidingsimulator settings screen");

        return builder.build();
    }

    private record ChunkDistances(int renderDistance, int simulationDistance) {
    }

    private static ChunkDistances getCurrentChunkDistances() {
        MinecraftClient client = MinecraftClient.getInstance();
        int render = client.options.getViewDistance().getValue();
        int simulation = client.options.getSimulationDistance().getValue();
        if (client.getServer() != null) {
            render = client.getServer().getPlayerManager().getViewDistance();
            simulation = client.getServer().getPlayerManager().getSimulationDistance();
        }
        return new ChunkDistances(render, simulation);
    }
}
