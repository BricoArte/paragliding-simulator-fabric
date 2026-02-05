package com.tgskiv.skydiving.menu;

public class SkydivingClientConfig {
    public static int ticksPerWindChange = 1200;
    public static double windRotationDegrees = 20.0;
    public static double maxSpeedDelta = 0.025;
    public static double maxWindSpeed = 0.1;
    // 10 km/h -> 10/720 â‰ˆ 0.0139 en unidades internas
    public static double minWindSpeed = 0.014;

    public static double varioDescentThreshold = -0.8;
    public static double varioStrongDescentThreshold = -4.0;
    public static float varioVolume = 0.6f;

    public static double helmetDescentThreshold = -0.8;
    public static double helmetStrongDescentThreshold = -4.0;
    public static float helmetVolume = 0.6f;

    public static boolean hudShowWind = true;
    public static boolean hudShowHeading = true;
    public static boolean hudShowAltitude = true;
    public static boolean hudShowSpeed = true;
    public static boolean hudShowVario = true;
    public static boolean hudShowThermal = true;
    public static boolean hudShowThermalDebug = false;
    public static float hudScale = 1.0f;

}
