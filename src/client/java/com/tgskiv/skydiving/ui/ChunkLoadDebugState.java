package com.tgskiv.skydiving.ui;

public final class ChunkLoadDebugState {
    private static final int WINDOW = 64;
    private static final double[] samples = new double[WINDOW];
    private static int sampleCount = 0;
    private static int sampleIndex = 0;
    private static long lastLoadNs = 0L;
    private static double lastMs = 0.0;
    private static double avgMs = 0.0;
    private static double maxMs = 0.0;
    private static long maxWindowStartNs = 0L;

    private ChunkLoadDebugState() {}

    public static void recordChunkLoad() {
        long now = System.nanoTime();
        if (maxWindowStartNs == 0L) {
            maxWindowStartNs = now;
        }
        if (lastLoadNs != 0L) {
            double ms = (now - lastLoadNs) / 1_000_000.0;
            lastMs = ms;
            if (ms > maxMs) {
                maxMs = ms;
            }
            samples[sampleIndex] = ms;
            sampleIndex = (sampleIndex + 1) % WINDOW;
            if (sampleCount < WINDOW) {
                sampleCount++;
            }
            double sum = 0.0;
            for (int i = 0; i < sampleCount; i++) {
                sum += samples[i];
            }
            avgMs = sum / sampleCount;
        }
        lastLoadNs = now;
    }

    public static void tick() {
        long now = System.nanoTime();
        if (maxWindowStartNs == 0L) {
            maxWindowStartNs = now;
            return;
        }
        if (now - maxWindowStartNs >= 60_000_000_000L) {
            maxMs = 0.0;
            maxWindowStartNs = now;
        }
    }

    public static double getLastMs() {
        return lastMs;
    }

    public static double getAvgMs() {
        return avgMs;
    }

    public static double getMaxMs() {
        return maxMs;
    }

    public static int getSampleCount() {
        return sampleCount;
    }
}
