package com.tgskiv.skydiving.ui;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.List;

public final class LaunchSiteDebugState {
    private static final long EVAL_WINDOW_MS = 60_000L;
    private static int totalCount = 0;
    private static int attempts = 0;
    private static int chunkLoads = 0;
    private static int chunkLoadsWindow = 0;
    private static int evalCandidates = 0;
    private static int evalCacheHits = 0;
    private static int heightCallsPerSecond = 0;
    private static double lastAttemptMs = 0.0;
    private static double avgAttemptMs = 0.0;
    private static double processTotalSeconds = 0.0;
    private static double gameTotalSeconds = 0.0;
    private static double heightSeconds = 0.0;
    private static double heightCenterSeconds = 0.0;
    private static double heightCornerSeconds = 0.0;
    private static double peakSeconds = 0.0;
    private static double edgeSeconds = 0.0;
    private static double forwardSeconds = 0.0;
    private static double flatSeconds = 0.0;
    private static double fluidSeconds = 0.0;
    private static double placeSeconds = 0.0;
    private static double cacheSeconds = 0.0;
    private static double nearSeconds = 0.0;
    private static double chanceSeconds = 0.0;
    private static int failChance = 0;
    private static int failNearPlaced = 0;
    private static int failNearInflight = 0;
    private static int failMinHeight = 0;
    private static int failHeightCenter = 0;
    private static int failHeightCorners = 0;
    private static int failDominance = 0;
    private static int failFlat = 0;
    private static int failFluid = 0;
    private static List<BlockPos> recent = Collections.emptyList();
    private static boolean hasLastAttempt = false;
    private static BlockPos lastAttemptPos = BlockPos.ORIGIN;
    private static int lastAttemptReason = 0;
    private static int lastPeakY = 0;
    private static double lastEdgeDrop = 0.0;
    private static double lastForwardDrop = 0.0;
    private static int lastFlatMinY = 0;
    private static int lastFlatMaxY = 0;
    private static long evalWindowStartMs = 0L;
    private static int evalWindowAttemptsBase = 0;
    private static int evalWindowChunksBase = 0;
    private static int evalWindowCandidatesBase = 0;
    private static int evalWindowCacheHitsBase = 0;
    private static int evalWindowAttempts = 0;
    private static int evalWindowChunks = 0;
    private static int evalWindowCandidates = 0;
    private static int evalWindowCacheHits = 0;

    private LaunchSiteDebugState() {}

    public static void update(int count,
                              int attemptsValue,
                              int chunkLoadsValue,
                              int chunkLoadsWindowValue,
                              int evalCandidatesValue,
                              int evalCacheHitsValue,
                              int heightCallsPerSecondValue,
                              double lastAttemptMsValue,
                              double avgAttemptMsValue,
                              double processTotalSecondsValue,
                              double gameTotalSecondsValue,
                              double heightSecondsValue,
                              double heightCenterSecondsValue,
                              double heightCornerSecondsValue,
                              double peakSecondsValue,
                              double edgeSecondsValue,
                              double forwardSecondsValue,
                              double flatSecondsValue,
                              double fluidSecondsValue,
                              double placeSecondsValue,
                              double cacheSecondsValue,
                              double nearSecondsValue,
                              double chanceSecondsValue,
                              int chance,
                              int nearPlaced,
                              int nearInflight,
                              int minHeight,
                              int heightCenter,
                              int heightCorners,
                              int dominance,
                              int flat,
                              int fluid,
                              List<BlockPos> recentPositions,
                              boolean hasLast,
                              BlockPos lastPos,
                              int lastReason,
                              int peakY,
                              double edgeDrop,
                              double forwardDrop,
                              int flatMinY,
                              int flatMaxY) {
        long nowMs = System.currentTimeMillis();
        if (evalWindowStartMs == 0L) {
            evalWindowStartMs = nowMs;
            evalWindowAttemptsBase = attemptsValue;
            evalWindowChunksBase = chunkLoadsValue;
            evalWindowCandidatesBase = evalCandidatesValue;
            evalWindowCacheHitsBase = evalCacheHitsValue;
        } else if (nowMs - evalWindowStartMs >= EVAL_WINDOW_MS) {
            evalWindowStartMs = nowMs;
            evalWindowAttemptsBase = attemptsValue;
            evalWindowChunksBase = chunkLoadsValue;
            evalWindowCandidatesBase = evalCandidatesValue;
            evalWindowCacheHitsBase = evalCacheHitsValue;
        }
        evalWindowAttempts = Math.max(0, attemptsValue - evalWindowAttemptsBase);
        evalWindowChunks = Math.max(0, chunkLoadsValue - evalWindowChunksBase);
        evalWindowCandidates = Math.max(0, evalCandidatesValue - evalWindowCandidatesBase);
        evalWindowCacheHits = Math.max(0, evalCacheHitsValue - evalWindowCacheHitsBase);

        totalCount = count;
        attempts = attemptsValue;
        chunkLoads = chunkLoadsValue;
        chunkLoadsWindow = chunkLoadsWindowValue;
        evalCandidates = evalCandidatesValue;
        evalCacheHits = evalCacheHitsValue;
        heightCallsPerSecond = heightCallsPerSecondValue;
        lastAttemptMs = lastAttemptMsValue;
        avgAttemptMs = avgAttemptMsValue;
        processTotalSeconds = processTotalSecondsValue;
        gameTotalSeconds = gameTotalSecondsValue;
        heightSeconds = heightSecondsValue;
        heightCenterSeconds = heightCenterSecondsValue;
        heightCornerSeconds = heightCornerSecondsValue;
        peakSeconds = peakSecondsValue;
        edgeSeconds = edgeSecondsValue;
        forwardSeconds = forwardSecondsValue;
        flatSeconds = flatSecondsValue;
        fluidSeconds = fluidSecondsValue;
        placeSeconds = placeSecondsValue;
        cacheSeconds = cacheSecondsValue;
        nearSeconds = nearSecondsValue;
        chanceSeconds = chanceSecondsValue;
        failChance = chance;
        failNearPlaced = nearPlaced;
        failNearInflight = nearInflight;
        failMinHeight = minHeight;
        failHeightCenter = heightCenter;
        failHeightCorners = heightCorners;
        failDominance = dominance;
        failFlat = flat;
        failFluid = fluid;
        recent = List.copyOf(recentPositions);
        hasLastAttempt = hasLast;
        lastAttemptPos = lastPos;
        lastAttemptReason = lastReason;
        lastPeakY = peakY;
        lastEdgeDrop = edgeDrop;
        lastForwardDrop = forwardDrop;
        lastFlatMinY = flatMinY;
        lastFlatMaxY = flatMaxY;
    }

    public static int getTotalCount() {
        return totalCount;
    }

    public static int getAttempts() {
        return attempts;
    }

    public static int getChunkLoads() {
        return chunkLoads;
    }

    public static int getChunkLoadsWindow() {
        return chunkLoadsWindow;
    }

    public static int getEvalCandidates() {
        return evalCandidates;
    }

    public static int getEvalCacheHits() {
        return evalCacheHits;
    }

    public static int getEvalWindowAttempts() {
        return evalWindowAttempts;
    }

    public static int getEvalWindowChunks() {
        return evalWindowChunks;
    }

    public static int getEvalWindowCandidates() {
        return evalWindowCandidates;
    }

    public static int getEvalWindowCacheHits() {
        return evalWindowCacheHits;
    }

    public static int getHeightCallsPerSecond() {
        return heightCallsPerSecond;
    }

    public static double getLastAttemptMs() {
        return lastAttemptMs;
    }

    public static double getAvgAttemptMs() {
        return avgAttemptMs;
    }

    public static double getProcessTotalSeconds() {
        return processTotalSeconds;
    }

    public static double getGameTotalSeconds() {
        return gameTotalSeconds;
    }

    public static double getHeightSeconds() {
        return heightSeconds;
    }

    public static double getHeightCenterSeconds() {
        return heightCenterSeconds;
    }

    public static double getHeightCornerSeconds() {
        return heightCornerSeconds;
    }

    public static double getPeakSeconds() {
        return peakSeconds;
    }

    public static double getEdgeSeconds() {
        return edgeSeconds;
    }

    public static double getForwardSeconds() {
        return forwardSeconds;
    }

    public static double getFlatSeconds() {
        return flatSeconds;
    }

    public static double getFluidSeconds() {
        return fluidSeconds;
    }

    public static double getPlaceSeconds() {
        return placeSeconds;
    }

    public static double getCacheSeconds() {
        return cacheSeconds;
    }

    public static double getNearSeconds() {
        return nearSeconds;
    }

    public static double getChanceSeconds() {
        return chanceSeconds;
    }

    public static int getFailChance() {
        return failChance;
    }

    public static int getFailNearPlaced() {
        return failNearPlaced;
    }

    public static int getFailNearInflight() {
        return failNearInflight;
    }

    public static int getFailMinHeight() {
        return failMinHeight;
    }

    public static int getFailHeightCenter() {
        return failHeightCenter;
    }

    public static int getFailHeightCorners() {
        return failHeightCorners;
    }

    public static int getFailDominance() {
        return failDominance;
    }

    public static int getFailFlat() {
        return failFlat;
    }

    public static int getFailFluid() {
        return failFluid;
    }

    public static List<BlockPos> getRecent() {
        return recent;
    }

    public static boolean hasLastAttempt() {
        return hasLastAttempt;
    }

    public static BlockPos getLastAttemptPos() {
        return lastAttemptPos;
    }

    public static int getLastAttemptReason() {
        return lastAttemptReason;
    }

    public static int getLastPeakY() {
        return lastPeakY;
    }

    public static double getLastEdgeDrop() {
        return lastEdgeDrop;
    }

    public static double getLastForwardDrop() {
        return lastForwardDrop;
    }

    public static int getLastFlatMinY() {
        return lastFlatMinY;
    }

    public static int getLastFlatMaxY() {
        return lastFlatMaxY;
    }
}
