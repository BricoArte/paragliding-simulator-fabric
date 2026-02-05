package com.tgskiv.skydiving.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record LaunchSiteDebugPayload(int totalCount,
                                     int attempts,
                                     int chunkLoads,
                                     int chunkLoadsWindow,
                                     int evalCandidates,
                                     int evalCacheHits,
                                     int heightCallsPerSecond,
                                     double lastAttemptMs,
                                     double avgAttemptMs,
                                     double processTotalSeconds,
                                     double gameTotalSeconds,
                                     double heightSeconds,
                                     double heightCenterSeconds,
                                     double heightCornerSeconds,
                                     double peakSeconds,
                                     double edgeSeconds,
                                     double forwardSeconds,
                                     double flatSeconds,
                                     double fluidSeconds,
                                     double placeSeconds,
                                     double cacheSeconds,
                                     double nearSeconds,
                                     double chanceSeconds,
                                     int failChance,
                                     int failNearPlaced,
                                     int failNearInflight,
                                     int failMinHeight,
                                     int failHeightCenter,
                                     int failHeightCorners,
                                     int failDominance,
                                     int failFlat,
                                     int failFluid,
                                     List<BlockPos> recent,
                                     boolean hasLastAttempt,
                                     BlockPos lastAttemptPos,
                                     int lastAttemptReason,
                                     int lastPeakY,
                                     double lastEdgeDrop,
                                     double lastForwardDrop,
                                     int lastFlatMinY,
                                     int lastFlatMaxY) implements CustomPayload {

    public static final Identifier ID = Identifier.of("paraglidingsimulator", "launch_site_debug");
    public static final CustomPayload.Id<LaunchSiteDebugPayload> PAYLOAD_ID = new CustomPayload.Id<>(ID);

    public static final PacketCodec<RegistryByteBuf, LaunchSiteDebugPayload> CODEC =
            CustomPayload.codecOf(LaunchSiteDebugPayload::write, LaunchSiteDebugPayload::new);

    public LaunchSiteDebugPayload(RegistryByteBuf buf) {
        this(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                readPositions(buf),
                buf.readBoolean(),
                buf.readBlockPos(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    private static List<BlockPos> readPositions(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return positions;
    }

    public void write(RegistryByteBuf buf) {
        buf.writeVarInt(this.totalCount);
        buf.writeVarInt(this.attempts);
        buf.writeVarInt(this.chunkLoads);
        buf.writeVarInt(this.chunkLoadsWindow);
        buf.writeVarInt(this.evalCandidates);
        buf.writeVarInt(this.evalCacheHits);
        buf.writeVarInt(this.heightCallsPerSecond);
        buf.writeDouble(this.lastAttemptMs);
        buf.writeDouble(this.avgAttemptMs);
        buf.writeDouble(this.processTotalSeconds);
        buf.writeDouble(this.gameTotalSeconds);
        buf.writeDouble(this.heightSeconds);
        buf.writeDouble(this.heightCenterSeconds);
        buf.writeDouble(this.heightCornerSeconds);
        buf.writeDouble(this.peakSeconds);
        buf.writeDouble(this.edgeSeconds);
        buf.writeDouble(this.forwardSeconds);
        buf.writeDouble(this.flatSeconds);
        buf.writeDouble(this.fluidSeconds);
        buf.writeDouble(this.placeSeconds);
        buf.writeDouble(this.cacheSeconds);
        buf.writeDouble(this.nearSeconds);
        buf.writeDouble(this.chanceSeconds);
        buf.writeVarInt(this.failChance);
        buf.writeVarInt(this.failNearPlaced);
        buf.writeVarInt(this.failNearInflight);
        buf.writeVarInt(this.failMinHeight);
        buf.writeVarInt(this.failHeightCenter);
        buf.writeVarInt(this.failHeightCorners);
        buf.writeVarInt(this.failDominance);
        buf.writeVarInt(this.failFlat);
        buf.writeVarInt(this.failFluid);
        buf.writeVarInt(this.recent.size());
        for (BlockPos pos : this.recent) {
            buf.writeBlockPos(pos);
        }
        buf.writeBoolean(this.hasLastAttempt);
        buf.writeBlockPos(this.lastAttemptPos);
        buf.writeVarInt(this.lastAttemptReason);
        buf.writeVarInt(this.lastPeakY);
        buf.writeDouble(this.lastEdgeDrop);
        buf.writeDouble(this.lastForwardDrop);
        buf.writeVarInt(this.lastFlatMinY);
        buf.writeVarInt(this.lastFlatMaxY);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PAYLOAD_ID;
    }
}
