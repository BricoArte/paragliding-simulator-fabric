package com.tgskiv.skydiving;

import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import net.minecraft.util.math.Vec3d;

public class WindUtils {
    public static double clampSpeed(SkydivingServerConfig config, double speed) {
        return Math.max(config.minWindSpeed, Math.min(config.maxWindSpeed, speed));
    }

    public static String vectorToCompass(double x, double z) {
        double angle = Math.toDegrees(Math.atan2(-x, z));
        angle = (angle + 360) % 360;

        String[] directions = {
                "N", "NNE", "NE", "ENE",
                "E", "ESE", "SE", "SSE",
                "S", "SSW", "SW", "WSW",
                "W", "WNW", "NW", "NNW"
        };

        int index = (int) Math.round(angle / 22.5) % 16;
        return directions[index];
    }


    public static String windToString(Vec3d direction, double speed, SkydivingServerConfig config) {
        String strDirection = vectorToCompass(direction.x, direction.z);

        double minWind = config.minWindSpeed;
        double maxWind = config.maxWindSpeed;
        double range = Math.max(1.0e-6, maxWind - minWind);
        double t = Math.max(0.0, Math.min(1.0, (speed - minWind) / range));
        double displayScale = 1.0 - 0.5 * t;

        // Conversion a km/h: el factor interno se usa como m/s = speed*200, asi que km/h = *3.6
        double speedKmh = speed * displayScale * 200 * 3.6;
        return String.format("Speed: %.1f km/h | Direction: %s", speedKmh, strDirection);
    }

}


