package com.tgskiv.skydiving.configuration;


public class SkydivingServerConfig  {

    public enum ThermalAmountPreset { NONE, LOW, STANDARD, HIGH }
    public enum ThermalIntensityPreset { SOFT, STANDARD, STRONG }
    public enum ThermalSizePreset { SMALL, STANDARD, LARGE }
    public enum ThermalHeightPreset { LOW, STANDARD, HIGH }
    public enum ThermalGenerationDistancePreset { RENDER_DISTANCE, SIMULATION_DISTANCE }
    public enum LaunchGenerationPreset { NONE, LOW, STANDARD, HIGH }

    public int ticksPerWindChange = 1200;
    public double windRotationDegrees = 15.0;
    public double maxSpeedDelta = 0.025;
    public double maxWindSpeed = 0.112;
    // 10 km/h -> 10/720 â‰ˆ 0.0139 en unidades internas
    public double minWindSpeed = 0.014;


    public int FORECAST_MIN_SIZE = 5;
    public int FORECAST_DISPLAY_COUNT = 5;
    public boolean lavaThermalsEnabled = true;
    public boolean lavaUseRenderDistance = true;
    public int lavaScanCooldownTicks = 1200;
    public int lavaMaxScansPerInterval = 200;
    public int lavaScanMaxDistanceChunks = 12;
    public int lavaMaxCandidatesPerInterval = 300;
    public boolean thermalsEnabled = true;

    // Launch site (despegue) generation
    public boolean launchSitesEnabled = true;
    public boolean launchSitesWorldgenEnabled = true;
    public boolean launchSitesDebug = false;
    public boolean launchSitesDebugMarkers = false;
    public int launchMinHeight = 120;
    public int launchSpacingChunks = 10;
    public float launchAttemptChance = 1f;
    public int launchSampleStep = 3;
    public int launchEdgeMargin = 5;
    public int launchFlatRadius = 2;
    public int launchFlatMaxDelta = 500;

    public ThermalAmountPreset thermalAmountPreset = ThermalAmountPreset.STANDARD;
    public ThermalIntensityPreset thermalIntensityPreset = ThermalIntensityPreset.STANDARD;
    public ThermalSizePreset thermalSizePreset = ThermalSizePreset.STANDARD;
    public ThermalHeightPreset thermalHeightPreset = ThermalHeightPreset.STANDARD;
    public ThermalGenerationDistancePreset thermalGenerationDistancePreset = ThermalGenerationDistancePreset.RENDER_DISTANCE;
    public LaunchGenerationPreset launchGenerationPreset = LaunchGenerationPreset.STANDARD;

    public double getThermalAmountChanceMultiplier() {
        return switch (thermalAmountPreset) {
            case NONE -> 0.0;
            case LOW -> 0.5;
            case STANDARD -> 1.0;
            case HIGH -> 1.5;
        };
    }

    public double getThermalAmountActiveMultiplier() {
        return switch (thermalAmountPreset) {
            case NONE -> 0.0;
            case LOW -> 0.6;
            case STANDARD -> 1.0;
            case HIGH -> 1.5;
        };
    }

    public double getThermalIntensityMultiplier() {
        return switch (thermalIntensityPreset) {
            case SOFT -> 0.7;
            case STANDARD -> 1.0;
            case STRONG -> 1.3;
        };
    }

    public double getThermalSizeMultiplier() {
        return switch (thermalSizePreset) {
            case SMALL -> 0.85;
            case STANDARD -> 1.0;
            case LARGE -> 1.2;
        };
    }

    public double getThermalHeightMultiplier() {
        return switch (thermalHeightPreset) {
            case LOW -> 0.85;
            case STANDARD -> 1.0;
            case HIGH -> 1.2;
        };
    }

}
