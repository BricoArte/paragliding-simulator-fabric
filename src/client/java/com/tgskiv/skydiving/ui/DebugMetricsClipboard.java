package com.tgskiv.skydiving.ui;

import com.tgskiv.skydiving.flight.ParagliderForces;
import com.tgskiv.skydiving.render.ThermalCloudRenderer;
import net.minecraft.client.MinecraftClient;

public final class DebugMetricsClipboard {
    private DebugMetricsClipboard() {}

    public static String buildMetricsText(MinecraftClient mc) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("PARAGLIDING_SIMULATOR_METRICS\n");

        int attempts = LaunchSiteDebugState.getAttempts();
        int chunks = LaunchSiteDebugState.getChunkLoads();
        double evalPct = chunks > 0 ? (attempts * 100.0) / chunks : 0.0;

        double totalSeconds = LaunchSiteDebugState.getProcessTotalSeconds();
        double gameSeconds = LaunchSiteDebugState.getGameTotalSeconds();
        double gamePct = (totalSeconds > 0.0 && gameSeconds > 0.0) ? (totalSeconds * 100.0) / gameSeconds : 0.0;

        sb.append("launch.attempts=").append(attempts).append('\n');
        sb.append("launch.chunks=").append(chunks).append('\n');
        sb.append("launch.evalPct=").append(format1(evalPct)).append('\n');
        sb.append("launch.evalCandidates=").append(LaunchSiteDebugState.getEvalCandidates()).append('\n');
        sb.append("launch.evalCacheHits=").append(LaunchSiteDebugState.getEvalCacheHits()).append('\n');
        sb.append("launch.totalSeconds=").append(format2(totalSeconds)).append('\n');
        sb.append("launch.totalGamePct=").append(format2(gamePct)).append('\n');

        if (totalSeconds > 0.0) {
        sb.append("launch.heightSeconds=").append(format2(LaunchSiteDebugState.getHeightSeconds()))
                .append(" (").append(format1(LaunchSiteDebugState.getHeightSeconds() * 100.0 / totalSeconds)).append("%)\n");
        sb.append("launch.heightCenterSeconds=").append(format2(LaunchSiteDebugState.getHeightCenterSeconds()))
                .append(" (").append(format1(LaunchSiteDebugState.getHeightCenterSeconds() * 100.0 / totalSeconds)).append("%)\n");
        sb.append("launch.heightCornerSeconds=").append(format2(LaunchSiteDebugState.getHeightCornerSeconds()))
                .append(" (").append(format1(LaunchSiteDebugState.getHeightCornerSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.peakSeconds=").append(format2(LaunchSiteDebugState.getPeakSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getPeakSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.edgeSeconds=").append(format2(LaunchSiteDebugState.getEdgeSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getEdgeSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.forwardSeconds=").append(format2(LaunchSiteDebugState.getForwardSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getForwardSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.flatSeconds=").append(format2(LaunchSiteDebugState.getFlatSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getFlatSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.fluidSeconds=").append(format2(LaunchSiteDebugState.getFluidSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getFluidSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.placeSeconds=").append(format2(LaunchSiteDebugState.getPlaceSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getPlaceSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.cacheSeconds=").append(format2(LaunchSiteDebugState.getCacheSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getCacheSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.nearSeconds=").append(format2(LaunchSiteDebugState.getNearSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getNearSeconds() * 100.0 / totalSeconds)).append("%)\n");
            sb.append("launch.chanceSeconds=").append(format2(LaunchSiteDebugState.getChanceSeconds()))
                    .append(" (").append(format1(LaunchSiteDebugState.getChanceSeconds() * 100.0 / totalSeconds)).append("%)\n");
        }

        sb.append("launch.heightCallsPerSecond=").append(LaunchSiteDebugState.getHeightCallsPerSecond()).append('\n');
        sb.append("launch.fail.chance=").append(LaunchSiteDebugState.getFailChance()).append('\n');
        sb.append("launch.fail.nearPlaced=").append(LaunchSiteDebugState.getFailNearPlaced()).append('\n');
        sb.append("launch.fail.nearInflight=").append(LaunchSiteDebugState.getFailNearInflight()).append('\n');
        sb.append("launch.fail.minHeight=").append(LaunchSiteDebugState.getFailMinHeight()).append('\n');
        sb.append("launch.fail.heightCenter=").append(LaunchSiteDebugState.getFailHeightCenter()).append('\n');
        sb.append("launch.fail.heightCorners=").append(LaunchSiteDebugState.getFailHeightCorners()).append('\n');
        sb.append("launch.fail.edge=").append(LaunchSiteDebugState.getFailDominance()).append('\n');
        sb.append("launch.fail.flat=").append(LaunchSiteDebugState.getFailFlat()).append('\n');
        sb.append("launch.fail.fluid=").append(LaunchSiteDebugState.getFailFluid()).append('\n');

        int chunkSamples = ChunkLoadDebugState.getSampleCount();
        sb.append("chunk.samples=").append(chunkSamples).append('\n');
        sb.append("chunk.avgMs=").append(chunkSamples > 0 ? format1(ChunkLoadDebugState.getAvgMs()) : "-").append('\n');
        sb.append("chunk.lastMs=").append(chunkSamples > 0 ? format1(ChunkLoadDebugState.getLastMs()) : "-").append('\n');
        sb.append("chunk.maxMs1m=").append(chunkSamples > 0 ? format1(ChunkLoadDebugState.getMaxMs()) : "-").append('\n');

        sb.append("thermal.tickMs=").append(format2(ParagliderForces.getLastThermalTickNanos() / 1_000_000.0)).append('\n');
        sb.append("thermal.renderMs=").append(format2(ThermalCloudRenderer.getLastRenderNanos() / 1_000_000.0)).append('\n');

        int fps = mc.getCurrentFps();
        sb.append("client.fps=").append(fps).append('\n');

        return sb.toString();
    }

    private static String format1(double value) {
        return String.format("%.1f", value);
    }

    private static String format2(double value) {
        return String.format("%.2f", value);
    }
}
