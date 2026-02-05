package com.tgskiv.skydiving.flight;




import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;


public final class ParagliderForces {

    private ParagliderForces() {}

    private static final double THERMAL_START_X = 1835.0;
    private static final double THERMAL_START_Z = 365.0;
    private static final double THERMAL_RADIUS = 7.0;
    private static final double THERMAL_MAX_UPDRAFT = 0.12;
    private static final double THERMAL_SIZE_MIN = 0.7;
    private static final double THERMAL_SIZE_MAX = 1.4;
    private static final double THERMAL_SIZE_VISUAL_MIN = 0.7;
    private static final double THERMAL_SIZE_VISUAL_MAX = 2.0;
    private static final double THERMAL_DRIFT_FACTOR = 0.1;
    private static final double THERMAL_CLOUD_FADE = 10.0;
    private static final double THERMAL_MIN_HEIGHT = 40.0;
    private static final double THERMAL_MAX_HEIGHT = 80.0;
    private static final double THERMAL_HEIGHT_REF_Y = 60.0;
    private static final double THERMAL_HEIGHT_SLOPE = 0.5;
    private static final long THERMAL_LIFETIME_TICKS = 3600;
    private static final double THERMAL_RAMP_FRACTION = 0.2;
    private static final int THERMAL_MIN_CLEARANCE = 7;
    private static final int THERMAL_MAX_ACTIVE = 100;
    private static final int THERMAL_MAX_LAVA_ACTIVE = 30;
    private static final int THERMAL_SPAWN_INTERVAL_TICKS = 150;
    private static final double THERMAL_SPAWN_CHANCE = 0.00198;
    private static final long THERMAL_SPAWN_DAY_START = 1000;
    private static final long THERMAL_SPAWN_DAY_END = 12000;
    private static final long THERMAL_LAVA_GRACE_TICKS = 200;
    private static final int THERMAL_LAVA_SCAN_COOLDOWN_DEFAULT = 300;
    private static final double THERMAL_DAILY_HEIGHT_MIN = 0.8;
    private static final double THERMAL_DAILY_HEIGHT_MAX = 1.3;
    private static final double THERMAL_DAILY_SIZE_MIN = 0.85;
    private static final double THERMAL_DAILY_SIZE_MAX = 1.3;

    private static final Object THERMAL_LOCK = new Object();
    private static final Map<ChunkPos, ThermalData> THERMALS = new HashMap<>();
    private static volatile List<ThermalRenderData> clientThermalSnapshot = null;
    private static long lastSpawnTick = -1;
    private static int lastSpawnCandidates = 0;
    private static int lastSpawnAdded = 0;
    private static int lastSpawnCandidatesNormal = 0;
    private static int lastSpawnCandidatesLava = 0;
    private static int lastLavaScans = 0;
    private static long lastThermalTickNanos = 0;
    private static Set<ChunkPos> cachedActiveChunks = Set.of();
    private static Set<ChunkPos> cachedLavaChunks = Set.of();
    private static final Map<ChunkPos, LavaScan> LAVA_SCAN_CACHE = new HashMap<>();
    private static int lavaScanBudget = 0;
    private static int lavaScanCooldownTicks = THERMAL_LAVA_SCAN_COOLDOWN_DEFAULT;
    private static long cachedThermalDay = Long.MIN_VALUE;
    private static double cachedDailyHeightFactor = 1.0;
    private static double cachedDailySizeFactor = 1.0;
    private static boolean overrideDailyFactors = false;
    private static double overrideDailyHeightFactor = 1.0;
    private static double overrideDailySizeFactor = 1.0;

    private static int getMaxActive(SkydivingServerConfig cfg) {
        return Math.max(0, (int) Math.round(THERMAL_MAX_ACTIVE * cfg.getThermalAmountActiveMultiplier()));
    }

    private static int getMaxLavaActive(SkydivingServerConfig cfg) {
        return Math.max(0, (int) Math.round(THERMAL_MAX_LAVA_ACTIVE * cfg.getThermalAmountActiveMultiplier()));
    }

    private record LavaScan(long nextScanTick, boolean hasLava) {}

    private static final class ThermalData {
        private final ChunkPos chunk;
        private double centerX;
        private double centerZ;
        private double cloudY;
        private double sizeFactor;
        private double strengthFactor;
        private long startTick;
        private long lifetimeTicks;
        private boolean constant;
        private boolean staticDrift;
        private long lavaMissingSince;

        private ThermalData(ChunkPos chunk, double centerX, double centerZ, double cloudY, double sizeFactor, double strengthFactor,
                            long startTick, long lifetimeTicks,
                            boolean constant, boolean staticDrift) {
            this.chunk = chunk;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.cloudY = cloudY;
            this.sizeFactor = sizeFactor;
            this.strengthFactor = strengthFactor;
            this.startTick = startTick;
            this.lifetimeTicks = lifetimeTicks;
            this.constant = constant;
            this.staticDrift = staticDrift;
            this.lavaMissingSince = 0;
        }
    }

    public record ThermalRenderData(double centerX, double centerZ, double cloudY, double strengthFactor, double sizeFactor) {}
    private record DailyThermalFactors(double heightFactor, double sizeFactor) {}

    public static Vec3d yawToHorizontalDir(float yawDeg) {
        double yawRad = Math.toRadians(yawDeg);
        double x = -Math.sin(yawRad);
        double z =  Math.cos(yawRad);
        return new Vec3d(x, 0, z).normalize();
    }

    public static Vec3d windPush(Entity e, Vec3d windDirection, double windSpeed) {
        if (windSpeed <= 0 || windDirection.equals(Vec3d.ZERO)) return Vec3d.ZERO;

        int blocksBelow = getBlocksBelow(e);
        double compensated = windSpeed;

        // Misma lÃ³gica que FlightUtils (0.3 si <=5, 0.6 si <=10). :contentReference[oaicite:8]{index=8}
        if (blocksBelow >= 0 && blocksBelow <= 5) compensated *= 0.3;
        else if (blocksBelow >= 0 && blocksBelow <= 10) compensated *= 0.6;

        return windDirection.multiply(compensated);
    }

    private static int getBlocksBelow(Entity e) {
        World world = e.getWorld();
        Vec3d start = e.getPos().add(0, 0.5, 0);
        Vec3d end = start.add(0, -256, 0);

        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                e
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            double distance = start.y - hit.getPos().y;
            return (int) Math.floor(distance);
        }
        return -1;
    }

    // Cache para reutilizar el muestreo de terreno mientras la entidad siga en el mismo bloque
    private static final Map<Entity, TerrainCache> TERRAIN_CACHE = new WeakHashMap<>();

    private record TerrainCache(BlockPos pos, Vec2f slopeDir, float min, float max) {}

    public static void tickThermals(ServerWorld world, Vec3d windDirection, double windSpeed) {
        long tickStart = System.nanoTime();
        long worldTime = world.getTime();
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        if (!cfg.thermalsEnabled) {
            synchronized (THERMAL_LOCK) {
                THERMALS.clear();
            }
            return;
        }
        boolean lavaEnabled = cfg.lavaThermalsEnabled;
        if (worldTime % THERMAL_SPAWN_INTERVAL_TICKS == 0) {
            lavaScanCooldownTicks = Math.max(1, cfg.lavaScanCooldownTicks);
            lavaScanBudget = lavaEnabled ? Math.max(0, cfg.lavaMaxScansPerInterval) : 0;
            cachedActiveChunks = getNormalThermalCandidateChunks(world, cfg);
            cachedLavaChunks = lavaEnabled
                    ? (cfg.lavaUseRenderDistance ? getRenderedChunks(world, cfg.lavaScanMaxDistanceChunks) : cachedActiveChunks)
                    : Set.of();
            lastSpawnCandidatesNormal = cachedActiveChunks.size();
            lastSpawnCandidatesLava = lavaEnabled ? cachedLavaChunks.size() : 0;
            lastLavaScans = 0;
        }
        Set<ChunkPos> activeChunks = cachedActiveChunks;
        Set<ChunkPos> lavaChunks = lavaEnabled ? cachedLavaChunks : Set.of();
        synchronized (THERMAL_LOCK) {
            THERMALS.entrySet().removeIf(entry -> {
                ThermalData thermal = entry.getValue();
                return thermal.constant ? !lavaChunks.contains(entry.getKey()) : !activeChunks.contains(entry.getKey());
            });
            THERMALS.entrySet().removeIf(entry -> isExpired(entry.getValue(), worldTime));
            for (ThermalData thermal : THERMALS.values()) {
                if (lavaEnabled && thermal.constant) {
                    boolean lavaPresent = hasLavaInChunk(world, thermal.chunk);
                    if (lavaPresent) {
                        thermal.lavaMissingSince = 0;
                    } else if (thermal.lavaMissingSince == 0) {
                        thermal.lavaMissingSince = worldTime;
                    } else if (worldTime - thermal.lavaMissingSince >= THERMAL_LAVA_GRACE_TICKS) {
                        thermal.constant = false;
                        thermal.staticDrift = false;
                        thermal.lifetimeTicks = THERMAL_LIFETIME_TICKS;
                        thermal.startTick = worldTime - (long) (THERMAL_LIFETIME_TICKS * (1.0 - THERMAL_RAMP_FRACTION));
                        thermal.lavaMissingSince = 0;
                    }
                }
                if (thermal.staticDrift) continue;
                if (windSpeed > 0 && !windDirection.equals(Vec3d.ZERO)) {
                    thermal.centerX += windDirection.x * windSpeed * THERMAL_DRIFT_FACTOR;
                    thermal.centerZ += windDirection.z * windSpeed * THERMAL_DRIFT_FACTOR;
                }
            }
        }
        if (worldTime % THERMAL_SPAWN_INTERVAL_TICKS != 0) return;
        long dayTime = world.getTimeOfDay() % 24000;
        trySpawnThermals(world, worldTime, dayTime, activeChunks, lavaChunks);
        lastThermalTickNanos = System.nanoTime() - tickStart;
    }

    public static double getThermalRadius() { return THERMAL_RADIUS; }
    public static boolean isThermalSpawnTime(long worldTime) { return isWithinThermalSpawnTime(worldTime); }
    public static int getActiveThermalCount() {
        synchronized (THERMAL_LOCK) {
            return THERMALS.size();
        }
    }
    public static int getActiveNonLavaThermalCount() {
        synchronized (THERMAL_LOCK) {
            int count = 0;
            for (ThermalData thermal : THERMALS.values()) {
                if (!thermal.constant) count++;
            }
            return count;
        }
    }
    public static long getLastSpawnTick() { return lastSpawnTick; }
    public static int getLastSpawnCandidates() { return lastSpawnCandidates; }
    public static int getLastSpawnAdded() { return lastSpawnAdded; }
    public static int getLastSpawnCandidatesNormal() { return lastSpawnCandidatesNormal; }
    public static int getLastSpawnCandidatesLava() { return lastSpawnCandidatesLava; }
    public static int getLastLavaScans() { return lastLavaScans; }
    public static long getLastThermalTickNanos() { return lastThermalTickNanos; }
    public static boolean isDailyThermalOverrideActive() { return overrideDailyFactors; }
    public static double getDailyThermalHeightFactor() {
        return overrideDailyFactors ? overrideDailyHeightFactor : cachedDailyHeightFactor;
    }
    public static double getDailyThermalSizeFactor() {
        return overrideDailyFactors ? overrideDailySizeFactor : cachedDailySizeFactor;
    }

    public static void setClientThermalSnapshot(List<ThermalRenderData> snapshot) {
        clientThermalSnapshot = snapshot;
    }

    public static void clearClientThermalSnapshot() {
        clientThermalSnapshot = null;
    }

    public static void setDailyThermalOverride(double heightFactor, double sizeFactor) {
        overrideDailyHeightFactor = MathHelper.clamp(heightFactor, 0.1, 5.0);
        overrideDailySizeFactor = MathHelper.clamp(sizeFactor, 0.1, 5.0);
        overrideDailyFactors = true;
    }

    public static void clearDailyThermalOverride() {
        overrideDailyFactors = false;
    }
    public static List<ThermalRenderData> getThermalsForRender(long worldTime) {
        List<ThermalRenderData> snapshot = clientThermalSnapshot;
        if (snapshot != null && !snapshot.isEmpty()) return snapshot;
        return buildThermalRenderList(worldTime);
    }

    public static List<ThermalRenderData> getThermalsForSync(long worldTime) {
        return buildThermalRenderList(worldTime);
    }

    private static List<ThermalRenderData> buildThermalRenderList(long worldTime) {
        List<ThermalRenderData> result = new ArrayList<>();
        synchronized (THERMAL_LOCK) {
            for (ThermalData thermal : THERMALS.values()) {
                double factor = getThermalPhaseFactor(thermal, worldTime);
                if (factor <= 0.0) continue;
                result.add(new ThermalRenderData(thermal.centerX, thermal.centerZ, thermal.cloudY, factor, thermal.sizeFactor));
            }
        }
        return result;
    }
    public static double getThermalUpdraftForEntity(Entity e) { return computeThermalUpdraft(e); }

    public static double computeUpdraftStrength(Entity e, Vec3d windDirection, double windSpeed) {
        double thermalStrength = computeThermalUpdraft(e);
        if (windDirection.equals(Vec3d.ZERO)) return thermalStrength;

        World world = e.getWorld();
        BlockPos origin = e.getBlockPos();

        TerrainCache cache = TERRAIN_CACHE.get(e);
        Vec2f slopeDir;
        float min;
        float max;

        if (cache != null && cache.pos.equals(origin)) {
            slopeDir = cache.slopeDir;
            min = cache.min;
            max = cache.max;
        } else {
            float[][] heights = TerrainAirflowUtils.sampleHeightsAround(TerrainAirflowUtils.size, origin, world);

            // Calcular gradiente promedio (similar a TerrainAirflowUtils.getSlopeWindDot2 pero reutilizable)
            float dX = 0f;
            float dZ = 0f;
            int count = 0;

            for (int z = 1; z < TerrainAirflowUtils.size - 1; z++) {
                for (int x = 1; x < TerrainAirflowUtils.size - 1; x++) {
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

            slopeDir = new Vec2f(dX, dZ);
            if (slopeDir.lengthSquared() != 0) {
                slopeDir = slopeDir.normalize();
            }

            min = Float.MAX_VALUE;
            max = Float.MIN_VALUE;
            for (float[] row : heights) {
                for (float h : row) {
                    min = Math.min(min, h);
                    max = Math.max(max, h);
                }
            }

            TERRAIN_CACHE.put(e, new TerrainCache(origin, slopeDir, min, max));
        }

        float dot = 0f;
        if (slopeDir != null && slopeDir.lengthSquared() != 0) {
            Vec2f windDir = new Vec2f((float) windDirection.x, (float) windDirection.z).normalize();
            dot = windDir.dot(slopeDir);
        }

        float slopeDifference = max - min;

        // Refuerza pendientes pronunciadas: escala mÃ¡s rÃ¡pido y sube el lÃ­mite
        float slopeStrengthInfluence = Math.min(slopeDifference / 6f, 2.5f);
        if (slopeDifference < 1.5f) slopeStrengthInfluence = 0;

        double heightAboveMaxTerrain = e.getY() - max;
        if (heightAboveMaxTerrain > 20) slopeStrengthInfluence = 0;

        float altitudeFactor;
        if (heightAboveMaxTerrain <= 10) altitudeFactor = 1f;
        else altitudeFactor = (float) (1 - (heightAboveMaxTerrain - 10) / 10.0);

        // Factor por fuerza de viento: normalizamos contra 0.1 (max por defecto) y limitamos a 0..1
        double speedFactor = Math.min(Math.max(windSpeed / 0.1, 0.0), 1.0);

        double updraftStrength = dot * altitudeFactor * slopeStrengthInfluence * 0.08 * speedFactor;
        double total = updraftStrength + thermalStrength;
        if (Math.abs(total) < 0.001) return 0;
        return total;
    }

    private static double computeThermalUpdraft(Entity e) {
        if (!hasClearSkyAbove(e)) return 0;
        int blocksBelow = getBlocksBelow(e);
        if (blocksBelow >= 0 && blocksBelow < THERMAL_MIN_CLEARANCE) return 0;

        Vec3d pos = e.getPos();
        double height = pos.y;
        long worldTime = e.getWorld().getTime();
        double best = 0.0;
        synchronized (THERMAL_LOCK) {
            for (ThermalData thermal : THERMALS.values()) {
                double dx = pos.x - thermal.centerX;
                double dz = pos.z - thermal.centerZ;
                double radius = THERMAL_RADIUS * thermal.sizeFactor;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) continue;
                if (height >= thermal.cloudY) continue;
                double heightFactor = 1.0;
                double fadeStart = thermal.cloudY - THERMAL_CLOUD_FADE;
                if (height > fadeStart) {
                    heightFactor = (thermal.cloudY - height) / THERMAL_CLOUD_FADE;
                }
                double t = 1.0 - (dist / radius);
                double lifeFactor = getThermalPhaseFactor(thermal, worldTime);
                if (lifeFactor <= 0.0) continue;
                double strength = (THERMAL_MAX_UPDRAFT * thermal.strengthFactor) * t * heightFactor * lifeFactor;
                if (strength > best) best = strength;
            }
        }
        return best;
    }

    private static boolean hasClearSkyAbove(Entity e) {
        World world = e.getWorld();
        Vec3d start = e.getPos().add(0, 0.5, 0);
        Vec3d end = start.add(0, 256, 0);
        BlockHitResult hit = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.ANY,
                e
        ));
        return hit.getType() != HitResult.Type.BLOCK;
    }

    private static void trySpawnThermals(ServerWorld world, long worldTime, long dayTime,
                                         Set<ChunkPos> normalCandidates, Set<ChunkPos> lavaCandidates) {
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        DailyThermalFactors dailyFactors = getDailyThermalFactors(world);
        double amountChanceMul = cfg.getThermalAmountChanceMultiplier();
        double amountActiveMul = cfg.getThermalAmountActiveMultiplier();
        double intensityMul = cfg.getThermalIntensityMultiplier();
        double sizePresetMul = cfg.getThermalSizeMultiplier();
        double heightPresetMul = cfg.getThermalHeightMultiplier();
        if (amountChanceMul <= 0.0 || amountActiveMul <= 0.0) {
            return;
        }
        synchronized (THERMAL_LOCK) {
            if (THERMALS.size() >= getMaxActive(cfg)) return;
        }
        int hashSeed = (int)(worldTime / THERMAL_SPAWN_INTERVAL_TICKS);
        lastSpawnTick = worldTime;
        lastSpawnCandidates = normalCandidates.size() + lavaCandidates.size();
        lastSpawnAdded = 0;
        lastSpawnCandidatesNormal = 0;
        lastSpawnCandidatesLava = 0;

        int lavaActive;
        synchronized (THERMAL_LOCK) {
            lavaActive = 0;
            for (ThermalData thermal : THERMALS.values()) {
                if (thermal.constant) lavaActive++;
            }
        }

        int lavaCandidateLimit = Math.max(0, cfg.lavaMaxCandidatesPerInterval);
        int lavaProcessed = 0;
        for (ChunkPos chunkPos : lavaCandidates) {
            if (lavaCandidateLimit > 0 && lavaProcessed >= lavaCandidateLimit) {
                break;
            }
            lavaProcessed++;
            if (!passesSpawnFilter(chunkPos, hashSeed)) continue;
            lastSpawnCandidatesLava++;
            synchronized (THERMAL_LOCK) {
                if (THERMALS.size() >= getMaxActive(cfg)) return;
            }
            synchronized (THERMAL_LOCK) {
                if (THERMALS.containsKey(chunkPos) || hasAdjacentThermal(chunkPos)) continue;
            }

            boolean lavaThermal = hasLavaInChunk(world, chunkPos);
            if (!lavaThermal) continue;
            if (lavaActive >= getMaxLavaActive(cfg)) continue;

            double centerX = chunkPos.getStartX() + 8.0;
            double centerZ = chunkPos.getStartZ() + 8.0;
            double groundY = sampleChunkSurfaceCeiling(world, chunkPos);
            double sizeFactorDaily = dailyFactors.sizeFactor * sizePresetMul;
            double sizeMin = THERMAL_SIZE_VISUAL_MIN * sizeFactorDaily;
            double sizeMax = THERMAL_SIZE_VISUAL_MAX * sizeFactorDaily;
            double sizeFactor = lavaThermal ? sizeFactorDaily : (sizeMin + (world.getRandom().nextDouble() * (sizeMax - sizeMin)));
            double strengthMin = THERMAL_SIZE_MIN * dailyFactors.sizeFactor * intensityMul;
            double strengthMax = THERMAL_SIZE_MAX * dailyFactors.sizeFactor * intensityMul;
            double strengthFactor = lavaThermal ? (dailyFactors.sizeFactor * intensityMul)
                    : (strengthMin + (world.getRandom().nextDouble() * (strengthMax - strengthMin)));
            double targetHeight = THERMAL_MAX_HEIGHT - ((groundY - THERMAL_HEIGHT_REF_Y) * THERMAL_HEIGHT_SLOPE);
            double minHeight = THERMAL_MIN_HEIGHT * dailyFactors.heightFactor * heightPresetMul;
            double maxHeight = THERMAL_MAX_HEIGHT * dailyFactors.heightFactor * heightPresetMul;
            targetHeight = MathHelper.clamp(targetHeight * dailyFactors.heightFactor * heightPresetMul, minHeight, maxHeight);
            double cloudY = Math.max(groundY + THERMAL_MIN_CLEARANCE, groundY + (targetHeight * sizeFactor));
            ThermalData thermal = new ThermalData(
                    chunkPos,
                    centerX,
                    centerZ,
                    cloudY,
                    sizeFactor,
                    strengthFactor,
                    worldTime,
                    lavaThermal ? Long.MAX_VALUE : THERMAL_LIFETIME_TICKS,
                    lavaThermal,
                    lavaThermal
            );
            synchronized (THERMAL_LOCK) {
                if (!THERMALS.containsKey(chunkPos) && !hasAdjacentThermal(chunkPos)) {
                    THERMALS.put(chunkPos, thermal);
                    lastSpawnAdded++;
                    lavaActive++;
                }
            }
        }

        for (ChunkPos chunkPos : normalCandidates) {
            if (!passesSpawnFilter(chunkPos, hashSeed)) continue;
            lastSpawnCandidatesNormal++;
            synchronized (THERMAL_LOCK) {
                if (THERMALS.size() >= getMaxActive(cfg)) return;
            }
            synchronized (THERMAL_LOCK) {
                if (THERMALS.containsKey(chunkPos) || hasAdjacentThermal(chunkPos)) continue;
            }
            if (!isChunkBiomeAllowed(world, chunkPos)) continue;
            if (!isWithinThermalSpawnTime(dayTime)) continue;
            if (world.getRandom().nextDouble() > THERMAL_SPAWN_CHANCE * amountChanceMul * getBiomeChanceFactor(world, chunkPos)) continue;

            double centerX = chunkPos.getStartX() + 8.0;
            double centerZ = chunkPos.getStartZ() + 8.0;
            double groundY = sampleChunkSurfaceCeiling(world, chunkPos);
            double sizeFactorDaily = dailyFactors.sizeFactor * sizePresetMul;
            double sizeMin = THERMAL_SIZE_VISUAL_MIN * sizeFactorDaily;
            double sizeMax = THERMAL_SIZE_VISUAL_MAX * sizeFactorDaily;
            double sizeFactor = sizeMin + (world.getRandom().nextDouble() * (sizeMax - sizeMin));
            double strengthMin = THERMAL_SIZE_MIN * dailyFactors.sizeFactor * intensityMul;
            double strengthMax = THERMAL_SIZE_MAX * dailyFactors.sizeFactor * intensityMul;
            double strengthFactor = strengthMin + (world.getRandom().nextDouble() * (strengthMax - strengthMin));
            double targetHeight = THERMAL_MAX_HEIGHT - ((groundY - THERMAL_HEIGHT_REF_Y) * THERMAL_HEIGHT_SLOPE);
            double minHeight = THERMAL_MIN_HEIGHT * dailyFactors.heightFactor * heightPresetMul;
            double maxHeight = THERMAL_MAX_HEIGHT * dailyFactors.heightFactor * heightPresetMul;
            targetHeight = MathHelper.clamp(targetHeight * dailyFactors.heightFactor * heightPresetMul, minHeight, maxHeight);
            double cloudY = Math.max(groundY + THERMAL_MIN_CLEARANCE, groundY + (targetHeight * sizeFactor));
            ThermalData thermal = new ThermalData(
                    chunkPos,
                    centerX,
                    centerZ,
                    cloudY,
                    sizeFactor,
                    strengthFactor,
                    worldTime,
                    THERMAL_LIFETIME_TICKS,
                    false,
                    false
            );
            synchronized (THERMAL_LOCK) {
                if (!THERMALS.containsKey(chunkPos) && !hasAdjacentThermal(chunkPos)) {
                    THERMALS.put(chunkPos, thermal);
                    lastSpawnAdded++;
                }
            }
        }
    }

    private static boolean isExpired(ThermalData thermal, long worldTime) {
        return !thermal.constant && (worldTime - thermal.startTick) >= thermal.lifetimeTicks;
    }

    private static Set<ChunkPos> getSimulatedChunks(ServerWorld world) {
        int simDistance = world.getServer().getPlayerManager().getSimulationDistance();
        return getChunksAroundPlayers(world, simDistance);
    }

    private static Set<ChunkPos> getNormalThermalCandidateChunks(ServerWorld world, SkydivingServerConfig cfg) {
        return switch (cfg.thermalGenerationDistancePreset) {
            case SIMULATION_DISTANCE -> getSimulatedChunks(world);
            case RENDER_DISTANCE -> getRenderedChunks(world, 0);
        };
    }

    private static Set<ChunkPos> getRenderedChunks(ServerWorld world, int maxDistance) {
        int viewDistance = world.getServer().getPlayerManager().getViewDistance();
        int distance = maxDistance > 0 ? Math.min(viewDistance, maxDistance) : viewDistance;
        return getChunksAroundPlayers(world, distance);
    }

    private static Set<ChunkPos> getChunksAroundPlayers(ServerWorld world, int distance) {
        Set<ChunkPos> candidates = new HashSet<>();
        for (var player : world.getPlayers()) {
            ChunkPos base = player.getChunkPos();
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    int cx = base.x + dx;
                    int cz = base.z + dz;
                    // Avoid spawning thermals in chunks that are not loaded yet.
                    if (!world.isChunkLoaded(cx, cz)) continue;
                    candidates.add(new ChunkPos(cx, cz));
                }
            }
        }
        return candidates;
    }

    private static boolean hasAdjacentThermal(ChunkPos chunkPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (THERMALS.containsKey(new ChunkPos(chunkPos.x + dx, chunkPos.z + dz))) return true;
            }
        }
        return false;
    }

    private static double sampleChunkSurfaceCeiling(ServerWorld world, ChunkPos chunkPos) {
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int endX = startX + 15;
        int endZ = startZ + 15;

        int maxY = Integer.MIN_VALUE;
        int[][] samples = new int[][]{
                {startX + 8, startZ + 8},
                {startX + 2, startZ + 2},
                {startX + 2, endZ - 2},
                {endX - 2, startZ + 2},
                {endX - 2, endZ - 2}
        };
        for (int[] sample : samples) {
            int y = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, new BlockPos(sample[0], 0, sample[1])).getY();
            if (y > maxY) {
                maxY = y;
            }
        }
        return maxY;
    }

    private static boolean isChunkBiomeAllowed(ServerWorld world, ChunkPos chunkPos) {
        int x = chunkPos.getStartX() + 8;
        int z = chunkPos.getStartZ() + 8;
        BlockPos pos = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, new BlockPos(x, 0, z));
        RegistryEntry<net.minecraft.world.biome.Biome> biome = world.getBiome(pos);
        if (biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_RIVER)) return false;
        return true;
    }

    private static double getBiomeChanceFactor(ServerWorld world, ChunkPos chunkPos) {
        int x = chunkPos.getStartX() + 8;
        int z = chunkPos.getStartZ() + 8;
        BlockPos pos = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, new BlockPos(x, 0, z));
        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        float temperature = biomeEntry.value().getTemperature();
        if (temperature >= 1.5f) return 1.8;
        if (temperature <= 0.15f) return 0.4;
        return 1.0;
    }

    private static boolean hasLavaInChunk(ServerWorld world, ChunkPos chunkPos) {
        long now = world.getTime();
        LavaScan cached = LAVA_SCAN_CACHE.get(chunkPos);
        if (cached != null && now < cached.nextScanTick()) {
            return cached.hasLava();
        }
        if (lavaScanBudget <= 0) {
            return false;
        }
        lavaScanBudget--;
        lastLavaScans++;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        boolean found = false;
        for (int dx = 2; dx <= 14; dx += 8) {
            for (int dz = 2; dz <= 14; dz += 8) {
                BlockPos surface = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, new BlockPos(startX + dx, 0, startZ + dz));
                for (int dy = 0; dy >= -6; dy--) {
                    BlockPos check = surface.add(0, dy, 0);
                    var state = world.getBlockState(check);
                    FluidState fluid = state.getFluidState();
                    if (state.isOf(Blocks.FIRE) || fluid.isIn(FluidTags.LAVA)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (found) break;
        }
        LAVA_SCAN_CACHE.put(chunkPos, new LavaScan(now + lavaScanCooldownTicks, found));
        return found;
    }

    private static boolean passesSpawnFilter(ChunkPos chunkPos, int seed) {
        int hash = chunkPos.x * 734287 + chunkPos.z * 912931 + seed * 31;
        return (hash & 1) == 0;
    }

    private static DailyThermalFactors getDailyThermalFactors(ServerWorld world) {
        if (overrideDailyFactors) {
            return new DailyThermalFactors(overrideDailyHeightFactor, overrideDailySizeFactor);
        }
        long day = world.getTimeOfDay() / 24000L;
        if (day != cachedThermalDay) {
            long seed = world.getSeed() ^ (day * 0x9E3779B97F4A7C15L);
            Random random = new Random(seed);
            cachedDailyHeightFactor = THERMAL_DAILY_HEIGHT_MIN
                    + (random.nextDouble() * (THERMAL_DAILY_HEIGHT_MAX - THERMAL_DAILY_HEIGHT_MIN));
            cachedDailySizeFactor = THERMAL_DAILY_SIZE_MIN
                    + (random.nextDouble() * (THERMAL_DAILY_SIZE_MAX - THERMAL_DAILY_SIZE_MIN));
            cachedThermalDay = day;
        }
        return new DailyThermalFactors(cachedDailyHeightFactor, cachedDailySizeFactor);
    }

    private static boolean isWithinThermalSpawnTime(long dayTime) {
        return dayTime >= THERMAL_SPAWN_DAY_START && dayTime <= THERMAL_SPAWN_DAY_END;
    }

    private static double getThermalPhaseFactor(ThermalData thermal, long worldTime) {
        if (thermal.constant) return 1.0;
        long age = worldTime - thermal.startTick;
        if (age < 0 || age >= thermal.lifetimeTicks) return 0.0;
        double phase = age / (double) thermal.lifetimeTicks;
        if (phase < THERMAL_RAMP_FRACTION) {
            return phase / THERMAL_RAMP_FRACTION;
        }
        if (phase < 1.0 - THERMAL_RAMP_FRACTION) {
            return 1.0;
        }
        double down = (phase - (1.0 - THERMAL_RAMP_FRACTION)) / THERMAL_RAMP_FRACTION;
        return 1.0 - down;
    }
}
