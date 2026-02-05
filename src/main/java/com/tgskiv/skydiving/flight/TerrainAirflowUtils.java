package com.tgskiv.skydiving.flight;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TerrainAirflowUtils {


    public final static int radius = 5;
    public final static int size = radius * 2 + 1;

    /**
     * Samples terrain slope and compares it with wind direction.
     * @param windDirection The current wind direction (Vec3d).
     * @return dot product between terrain slope direction and wind direction, in range [-1, 1].
     */
    public static float getSlopeWindDot2(float[][] heights, Vec3d windDirection) {


        // Compute gradient using central difference
        float dX = 0f;
        float dZ = 0f;
        int count = 0;

        for (int z = 1; z < size - 1; z++) {
            for (int x = 1; x < size - 1; x++) {
                float dzForward = heights[z + 1][x] - heights[z - 1][x];
                float dxSide = heights[z][x + 1] - heights[z][x - 1];

                dX += dxSide;
                dZ += dzForward;
                count++;
            }
        }

        if (count == 0) return 0;

        dX /= count;
        dZ /= count;

        // Convert to 2D vector pointing downslope
        Vec2f slopeDir = new Vec2f(dX, dZ);
        if (slopeDir.lengthSquared() == 0) return 0;
        slopeDir = slopeDir.normalize();

        Vec2f windDir = new Vec2f((float) windDirection.x, (float) windDirection.z).normalize();

        return windDir.dot(slopeDir);
    }


    // Sample terrain heights in a grid centered around the origin, aligned to XZ
    public static float[][] sampleHeightsAround(int size, BlockPos origin, World world) {
        float[][] heights = new float[size][size];

        int px = origin.getX();
        int pz = origin.getZ();

        int halfWidth = size / 2;
        int halfHeight = size / 2;

        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                int sx = px + (x - halfWidth);
                int sz = pz + (z - halfHeight);

                int sy = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, sx, sz);
                heights[z][x] = sy;
            }
        }

        return heights;
    }


}
