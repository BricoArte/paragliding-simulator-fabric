package com.tgskiv.skydiving.worldgen;

import com.mojang.serialization.MapCodec;
import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class LaunchSiteStructure extends Structure {
    public static final MapCodec<LaunchSiteStructure> CODEC = Structure.createCodec(LaunchSiteStructure::new);

    public LaunchSiteStructure(Structure.Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        if (!cfg.launchSitesEnabled || !cfg.launchSitesWorldgenEnabled) {
            return Optional.empty();
        }

        ChunkPos chunkPos = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        HeightLimitView world = context.world();
        NoiseConfig noiseConfig = context.noiseConfig();

        AttemptResult result = evaluateAttempt(generator, world, noiseConfig, chunkPos, cfg, true, true, true);
        if (!result.success) {
            return Optional.empty();
        }

        int baseY = result.flat != null ? result.flat.minY : result.peak.y;
        BlockPos pos = new BlockPos(result.peak.x, baseY, result.peak.z);
        if (cfg.launchSpacingChunks > 0) {
            SkydivingHandler.reserveLaunchSitePendingArea(new ChunkPos(pos), cfg.launchSpacingChunks);
        }
        int radius = cfg.launchFlatRadius;
        return Optional.of(new StructurePosition(pos, collector -> {
            collector.addPiece(new LaunchSitePiece(pos, radius, result.peak.bestDir));
        }));
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.LAUNCH_SITE;
    }

    public static AttemptResult tryManual(ServerWorld world, ChunkPos chunkPos, boolean place, boolean debugMarkers) {
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = world.getChunkManager().getNoiseConfig();

        AttemptResult result = evaluateAttempt(generator, world, noiseConfig, chunkPos, cfg, true, false, false);
        if (debugMarkers && result.peak != null) {
            placeDebugMarkers(world, result, cfg);
        }

        if (!result.success) {
            return result;
        }

        int baseY = result.flat != null ? result.flat.minY : result.peak.y;
        BlockPos pos = new BlockPos(result.peak.x, baseY, result.peak.z);
        if (LaunchSitePiece.hasFluidOnPlatform(world, pos, cfg.launchFlatRadius)) {
            SkydivingHandler.onLaunchSiteFailFluid();
            return result.withFailure(FailReason.FLUID);
        }

        if (place) {
            long placeStart = System.nanoTime();
            boolean placed = LaunchSitePiece.placeTemplate(world, pos, result.peak.bestDir);
            if (!placed) {
                LaunchSitePiece.buildPlatform(world, pos, cfg.launchFlatRadius);
                LaunchSitePiece.placeWindsock(world, pos, cfg.launchFlatRadius);
            }
            long placeNanos = System.nanoTime() - placeStart;
            SkydivingHandler.onLaunchSitePlaceTime(placeNanos);
            SkydivingHandler.onLaunchSiteProcessTime(placeNanos);
            SkydivingHandler.onLaunchSiteGenerated(world, pos, result.peak.bestDir);
        }

        return result;
    }

    private static boolean passesSpacingFilter(ChunkPos chunkPos, int spacing) {
        if (spacing <= 0) return true;
        return !SkydivingHandler.hasLaunchSiteNear(chunkPos, spacing);
    }

    private static boolean passesChanceFilter(ChunkPos chunkPos, float chance) {
        if (chance >= 1.0f) return true;
        if (chance <= 0.0f) return false;
        long seed = (chunkPos.x * 341873128712L) ^ (chunkPos.z * 132897987541L) ^ 0x9E3779B97F4A7C15L;
        java.util.Random random = new java.util.Random(seed);
        return random.nextFloat() < chance;
    }

    private static AttemptResult evaluateAttempt(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                                 ChunkPos chunkPos, SkydivingServerConfig cfg,
                                                 boolean applyNearFilter, boolean applyChanceFilter,
                                                 boolean applyEvalCache) {
        if (applyEvalCache) {
            SkydivingHandler.onLaunchSiteEvalCandidate();
        }
        if (applyEvalCache && !SkydivingHandler.tryMarkLaunchSiteEvaluated(chunkPos)) {
            SkydivingHandler.onLaunchSiteEvalCacheHit();
            return AttemptResult.fail(FailReason.SPACING, null, null, 0.0, 0.0);
        }
        long startNanos = System.nanoTime();
        SkydivingHandler.onLaunchSiteAttempt();

        SkydivingHandler.LaunchSiteReservation inflightReservation = null;
        try {
            if (applyNearFilter) {
                long nearStart = System.nanoTime();
                boolean nearOk = passesSpacingFilter(chunkPos, cfg.launchSpacingChunks);
                if (!nearOk) {
                    SkydivingHandler.onLaunchSiteNearTime(System.nanoTime() - nearStart);
                    SkydivingHandler.onLaunchSiteFailNearPlaced();
                    SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
                    SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
                    return AttemptResult.fail(FailReason.SPACING, null, null, 0.0, 0.0);
                }
                inflightReservation = SkydivingHandler.tryReserveLaunchSiteInFlight(chunkPos, cfg.launchSpacingChunks);
                SkydivingHandler.onLaunchSiteNearTime(System.nanoTime() - nearStart);
                if (inflightReservation == null) {
                    SkydivingHandler.onLaunchSiteFailNearInflight();
                    SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
                    SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
                    return AttemptResult.fail(FailReason.SPACING, null, null, 0.0, 0.0);
                }
            }
        if (applyChanceFilter) {
            long chanceStart = System.nanoTime();
            boolean chanceOk = passesChanceFilter(chunkPos, cfg.launchAttemptChance);
            SkydivingHandler.onLaunchSiteChanceTime(System.nanoTime() - chanceStart);
            if (!chanceOk) {
                SkydivingHandler.onLaunchSiteFailChance();
                SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
                SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
                return AttemptResult.fail(FailReason.SPACING, null, null, 0.0, 0.0);
            }
        }

        QuickHeightResult quick = getQuickHeightResult(generator, world, noiseConfig, chunkPos, cfg.launchMinHeight);
        if (!quick.pass) {
            SkydivingHandler.onLaunchSiteFailMinHeight();
            SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
            SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
            return AttemptResult.fail(FailReason.MIN_HEIGHT, null, null, 0.0, 0.0);
        }

        long peakStart = System.nanoTime();
        Peak peak = findPeakInChunk(generator, world, noiseConfig, chunkPos, cfg.launchEdgeMargin, cfg.launchSampleStep);
        SkydivingHandler.onLaunchSitePeakTime(System.nanoTime() - peakStart);
        if (peak == null || peak.y < cfg.launchMinHeight) {
            SkydivingHandler.onLaunchSiteFailMinHeight();
            SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
            SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
            return AttemptResult.fail(FailReason.MIN_HEIGHT, peak, null, 0.0, 0.0);
        }
        long forwardStart = System.nanoTime();
        ForwardResult forward = null;
        Peak forwardPeak = null;
        for (Direction dir : quick.dirOrder) {
            Peak candidate = new Peak(peak.x, peak.y, peak.z, 0.0, dir);
            forward = computeForwardChunkDrop(generator, world, noiseConfig, candidate);
            if (forward.pass) {
                forwardPeak = candidate;
                break;
            }
        }
        SkydivingHandler.onLaunchSiteForwardTime(System.nanoTime() - forwardStart);
        if (forward == null || !forward.pass) {
            SkydivingHandler.onLaunchSiteFailDominance();
            SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
            SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
            return AttemptResult.fail(FailReason.FORWARD, peak, null, peak.edgeDrop, forward.minDelta);
        }
        peak = forwardPeak == null ? peak : forwardPeak;

        long flatStart = System.nanoTime();
        FlatInfo flat = getFlatInfo(generator, world, noiseConfig, peak, cfg.launchFlatRadius);
        SkydivingHandler.onLaunchSiteFlatTime(System.nanoTime() - flatStart);
        if (flat.delta() > cfg.launchFlatMaxDelta) {
            SkydivingHandler.onLaunchSiteFailFlat();
            SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
            SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
            return AttemptResult.fail(FailReason.FLAT, peak, flat, peak.edgeDrop, forward.minDelta);
        }

        SkydivingHandler.onLaunchSiteProcessTime(System.nanoTime() - startNanos);
        SkydivingHandler.onLaunchSiteAttemptTime(System.nanoTime() - startNanos);
            return AttemptResult.success(peak, flat, peak.edgeDrop, forward.minDelta);
        } finally {
            SkydivingHandler.releaseLaunchSiteInFlight(inflightReservation);
        }
    }

    private static Peak findPeakInChunk(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                        ChunkPos chunkPos, int edgeMargin, int sampleStep) {
        int startX = chunkPos.getStartX() + edgeMargin;
        int endX = chunkPos.getStartX() + 15 - edgeMargin;
        int startZ = chunkPos.getStartZ() + edgeMargin;
        int endZ = chunkPos.getStartZ() + 15 - edgeMargin;

        Peak best = null;
        for (int x = startX; x <= endX; x += sampleStep) {
            for (int z = startZ; z <= endZ; z += sampleStep) {
                int y = getHeight(generator, world, noiseConfig, x, z);
                if (best == null || y > best.y) {
                    best = new Peak(x, y, z, 0.0, Direction.NORTH);
                }
            }
        }
        return best;
    }

    private static QuickHeightResult getQuickHeightResult(ChunkGenerator generator, HeightLimitView world,
                                                          NoiseConfig noiseConfig, ChunkPos chunkPos, int minHeight) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int endX = startX + 15;
        int endZ = startZ + 15;
        int centerX = startX + 8;
        int centerZ = startZ + 8;

        long start = System.nanoTime();
        long centerStart = System.nanoTime();
        SkydivingHandler.onLaunchSiteHeightQuery();
        int centerY = getHeight(generator, world, noiseConfig, centerX, centerZ);
        SkydivingHandler.onLaunchSiteHeightCenterTime(System.nanoTime() - centerStart);
        if (centerY < (minHeight - 5)) {
            SkydivingHandler.onLaunchSiteFailHeightCenter();
            SkydivingHandler.onLaunchSiteHeightTime(System.nanoTime() - start);
            return QuickHeightResult.fail();
        }

        int okCorners = 0;
        boolean nwOk = false;
        boolean neOk = false;
        boolean seOk = false;
        boolean swOk = false;

        long cornersStart = System.nanoTime();
        SkydivingHandler.onLaunchSiteHeightQuery();
        int upperBound = centerY + 5;
        int nwY = getHeight(generator, world, noiseConfig, startX, startZ);
        if (nwY >= minHeight && nwY <= upperBound) {
            nwOk = true;
            okCorners++;
        }
        SkydivingHandler.onLaunchSiteHeightQuery();
        int neY = getHeight(generator, world, noiseConfig, endX, startZ);
        if (neY >= minHeight && neY <= upperBound) {
            neOk = true;
            okCorners++;
        }
        SkydivingHandler.onLaunchSiteHeightQuery();
        int seY = getHeight(generator, world, noiseConfig, endX, endZ);
        if (seY >= minHeight && seY <= upperBound) {
            seOk = true;
            okCorners++;
        }
        SkydivingHandler.onLaunchSiteHeightQuery();
        int swY = getHeight(generator, world, noiseConfig, startX, endZ);
        if (swY >= minHeight && swY <= upperBound) {
            swOk = true;
            okCorners++;
        }
        SkydivingHandler.onLaunchSiteHeightCornerTime(System.nanoTime() - cornersStart);
        SkydivingHandler.onLaunchSiteHeightTime(System.nanoTime() - start);

        if (okCorners < 2) {
            SkydivingHandler.onLaunchSiteFailHeightCorners();
            return QuickHeightResult.fail();
        }

        boolean hasAdjacentOk = (nwOk && neOk)
                || (neOk && seOk)
                || (seOk && swOk)
                || (swOk && nwOk);
        if (!hasAdjacentOk) {
            SkydivingHandler.onLaunchSiteFailHeightCorners();
            return QuickHeightResult.fail();
        }

        double hN = (nwY + neY) / 2.0;
        double hS = (swY + seY) / 2.0;
        double hW = (nwY + swY) / 2.0;
        double hE = (neY + seY) / 2.0;
        double dNS = Math.abs(hN - hS);
        double dEW = Math.abs(hW - hE);

        Direction[] order = buildDirectionOrder(hN, hS, hW, hE);
        return QuickHeightResult.pass(order);
    }

    private static boolean hasForwardChunkDrop(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                               Peak peak, int forwardChunks, int minDelta) {
        ForwardResult result = computeForwardChunkDrop(generator, world, noiseConfig, peak);
        return result.pass && result.minDelta >= minDelta;
    }

    private static ForwardResult computeForwardChunkDrop(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                                         Peak peak) {
        Direction dir = peak.bestDir;
        int distance = 8;
        int firstDrop = 10;
        int secondDrop = 15;
        int thirdDrop = 20;
        int nextDrop = 45;
        double minDelta = Double.POSITIVE_INFINITY;

        for (int i = 1; i <= distance; i++) {
            int chunkX = (peak.x >> 4) + dir.getOffsetX() * i;
            int chunkZ = (peak.z >> 4) + dir.getOffsetZ() * i;
            int maxHeight = sampleChunkMaxHeight(generator, world, noiseConfig, new ChunkPos(chunkX, chunkZ), dir);
            int requiredDrop;
            if (i == 1) {
                requiredDrop = firstDrop;
            } else if (i == 2) {
                requiredDrop = secondDrop;
            } else if (i == 3) {
                requiredDrop = thirdDrop;
            } else {
                requiredDrop = nextDrop;
            }
            double delta = peak.y - maxHeight;
            minDelta = Math.min(minDelta, delta);
            if (delta < requiredDrop) {
                return new ForwardResult(minDelta, false);
            }
        }
        return new ForwardResult(minDelta, true);
    }

    private static int sampleChunkMaxHeight(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                            ChunkPos chunkPos, Direction dir) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int endX = startX + 15;
        int endZ = startZ + 15;
        int centerX = startX + 8;
        int centerZ = startZ + 8;

        int max = Integer.MIN_VALUE;
        int y = getHeight(generator, world, noiseConfig, centerX, centerZ);
        if (y > max) max = y;

        int farX1;
        int farZ1;
        int farX2;
        int farZ2;
        switch (dir) {
            case NORTH -> {
                farX1 = startX;
                farZ1 = startZ;
                farX2 = endX;
                farZ2 = startZ;
            }
            case SOUTH -> {
                farX1 = startX;
                farZ1 = endZ;
                farX2 = endX;
                farZ2 = endZ;
            }
            case WEST -> {
                farX1 = startX;
                farZ1 = startZ;
                farX2 = startX;
                farZ2 = endZ;
            }
            case EAST -> {
                farX1 = endX;
                farZ1 = startZ;
                farX2 = endX;
                farZ2 = endZ;
            }
            default -> {
                farX1 = startX;
                farZ1 = startZ;
                farX2 = endX;
                farZ2 = endZ;
            }
        }

        y = getHeight(generator, world, noiseConfig, farX1, farZ1);
        if (y > max) max = y;
        y = getHeight(generator, world, noiseConfig, farX2, farZ2);
        if (y > max) max = y;

        return max;
    }

    private static FlatInfo getFlatInfo(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig,
                                        Peak peak, int radius) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        BlockPos minPos = new BlockPos(peak.x, peak.y, peak.z);
        BlockPos maxPos = new BlockPos(peak.x, peak.y, peak.z);
        int minX = peak.x - radius;
        int maxX = peak.x + radius;
        int minZ = peak.z - radius;
        int maxZ = peak.z + radius;

        int y = getHeight(generator, world, noiseConfig, minX, minZ);
        if (y < min) {
            min = y;
            minPos = new BlockPos(minX, y, minZ);
        }
        if (y > max) {
            max = y;
            maxPos = new BlockPos(minX, y, minZ);
        }

        y = getHeight(generator, world, noiseConfig, maxX, minZ);
        if (y < min) {
            min = y;
            minPos = new BlockPos(maxX, y, minZ);
        }
        if (y > max) {
            max = y;
            maxPos = new BlockPos(maxX, y, minZ);
        }

        y = getHeight(generator, world, noiseConfig, maxX, maxZ);
        if (y < min) {
            min = y;
            minPos = new BlockPos(maxX, y, maxZ);
        }
        if (y > max) {
            max = y;
            maxPos = new BlockPos(maxX, y, maxZ);
        }

        y = getHeight(generator, world, noiseConfig, minX, maxZ);
        if (y < min) {
            min = y;
            minPos = new BlockPos(minX, y, maxZ);
        }
        if (y > max) {
            max = y;
            maxPos = new BlockPos(minX, y, maxZ);
        }
        return new FlatInfo(min, max, minPos, maxPos);
    }

    private static int getHeight(ChunkGenerator generator, HeightLimitView world, NoiseConfig noiseConfig, int x, int z) {
        return generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
    }

    private static void placeDebugMarkers(ServerWorld world, AttemptResult result, SkydivingServerConfig cfg) {
        Peak peak = result.peak;
        if (peak == null) {
            return;
        }
        BlockPos peakPos = new BlockPos(peak.x, peak.y + 1, peak.z);
        world.setBlockState(peakPos, Blocks.LIME_WOOL.getDefaultState(), 2);

        if (result.flat != null) {
            world.setBlockState(result.flat.minPos.up(), Blocks.RED_WOOL.getDefaultState(), 2);
            world.setBlockState(result.flat.maxPos.up(), Blocks.BLUE_WOOL.getDefaultState(), 2);
        }

        int markerDistance = 256;
        BlockPos dirPos = peakPos.add(peak.bestDir.getOffsetX() * markerDistance, 0, peak.bestDir.getOffsetZ() * markerDistance);
        world.setBlockState(dirPos, Blocks.YELLOW_WOOL.getDefaultState(), 2);
    }

    public enum FailReason {
        NONE,
        SPACING,
        MIN_HEIGHT,
        EDGE,
        FORWARD,
        FLAT,
        FLUID
    }

    public record AttemptResult(boolean success, FailReason reason, Peak peak, FlatInfo flat,
                                double edgeDrop, double forwardDrop) {
        static AttemptResult success(Peak peak, FlatInfo flat, double edgeDrop, double forwardDrop) {
            return new AttemptResult(true, FailReason.NONE, peak, flat, edgeDrop, forwardDrop);
        }

        static AttemptResult fail(FailReason reason, Peak peak, FlatInfo flat, double edgeDrop, double forwardDrop) {
            return new AttemptResult(false, reason, peak, flat, edgeDrop, forwardDrop);
        }

        AttemptResult withFailure(FailReason newReason) {
            return new AttemptResult(false, newReason, peak, flat, edgeDrop, forwardDrop);
        }
    }

    public record FlatInfo(int minY, int maxY, BlockPos minPos, BlockPos maxPos) {
        int delta() {
            return maxY - minY;
        }
    }

    public record Peak(int x, int y, int z, double edgeDrop, Direction bestDir) {}
    private static Direction[] buildDirectionOrder(double hN, double hS, double hW, double hE) {
        Direction[] dirs = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };
        double[] drops = new double[] {
                hS - hN, // NORTH
                hN - hS, // SOUTH
                hE - hW, // WEST
                hW - hE  // EAST
        };
        for (int i = 0; i < dirs.length - 1; i++) {
            for (int j = i + 1; j < dirs.length; j++) {
                if (drops[j] > drops[i]) {
                    double tmpDrop = drops[i];
                    drops[i] = drops[j];
                    drops[j] = tmpDrop;
                    Direction tmpDir = dirs[i];
                    dirs[i] = dirs[j];
                    dirs[j] = tmpDir;
                }
            }
        }
        return dirs;
    }

    private record QuickHeightResult(boolean pass, Direction[] dirOrder) {
        static QuickHeightResult pass(Direction[] dirOrder) {
            return new QuickHeightResult(true, dirOrder);
        }

        static QuickHeightResult fail() {
            return new QuickHeightResult(false, new Direction[] { Direction.NORTH });
        }
    }

    private record ForwardResult(double minDelta, boolean pass) {}
}
