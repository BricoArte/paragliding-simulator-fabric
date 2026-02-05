package com.tgskiv.skydiving.worldgen;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.util.math.random.Random;

public class LaunchSiteCorridorPiece extends StructurePiece {
    private static final int CORRIDOR_START = 6;
    private static final int SEGMENT_LENGTH = 16;
    public static final int SEGMENT_COUNT = 3;
    private static final int CORRIDOR_LEFT = 3;
    private static final int CORRIDOR_RIGHT = 2;
    private static final int CORRIDOR_UP = 10;

    private final BlockPos center;
    private final Direction forward;
    private final int segmentIndex;
    private final int baseY;

    public LaunchSiteCorridorPiece(BlockPos center, Direction forward, int baseY, int segmentIndex) {
        super(ModStructures.LAUNCH_SITE_CORRIDOR_PIECE, 0, createBounds(center, forward, baseY, segmentIndex));
        this.center = center.toImmutable();
        this.forward = forward;
        this.segmentIndex = segmentIndex;
        this.baseY = baseY;
    }

    public LaunchSiteCorridorPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.LAUNCH_SITE_CORRIDOR_PIECE, nbt);
        this.center = new BlockPos(nbt.getInt("CenterX"), nbt.getInt("CenterY"), nbt.getInt("CenterZ"));
        this.forward = Direction.byId(nbt.getInt("ForwardDir"));
        this.segmentIndex = nbt.getInt("SegmentIndex");
        this.baseY = nbt.getInt("BaseY");
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("CenterX", center.getX());
        nbt.putInt("CenterY", center.getY());
        nbt.putInt("CenterZ", center.getZ());
        nbt.putInt("ForwardDir", forward.getId());
        nbt.putInt("SegmentIndex", segmentIndex);
        nbt.putInt("BaseY", baseY);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor accessor, ChunkGenerator generator, Random random,
                         BlockBox box, net.minecraft.util.math.ChunkPos chunkPos, BlockPos pivot) {
        clearBoundsInChunk(world, getBoundingBox(), box);
    }

    private static BlockBox createBounds(BlockPos center, Direction forward, int baseY, int segmentIndex) {
        int distStart = CORRIDOR_START + segmentIndex * SEGMENT_LENGTH;
        int distEnd = distStart + SEGMENT_LENGTH - 1;
        Direction right = forward.rotateYClockwise();
        Direction left = forward.rotateYCounterclockwise();

        BlockPos p1 = offset(center, forward, distStart, left, CORRIDOR_LEFT);
        BlockPos p2 = offset(center, forward, distStart, right, CORRIDOR_RIGHT);
        BlockPos p3 = offset(center, forward, distEnd, left, CORRIDOR_LEFT);
        BlockPos p4 = offset(center, forward, distEnd, right, CORRIDOR_RIGHT);

        int minX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
        int maxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
        int minZ = Math.min(Math.min(p1.getZ(), p2.getZ()), Math.min(p3.getZ(), p4.getZ()));
        int maxZ = Math.max(Math.max(p1.getZ(), p2.getZ()), Math.max(p3.getZ(), p4.getZ()));

        int minY = baseY - (segmentIndex + 1);
        int maxY = baseY + CORRIDOR_UP;
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static BlockPos offset(BlockPos center, Direction forward, int dist, Direction lateral, int lateralDist) {
        return center.add(forward.getOffsetX() * dist + lateral.getOffsetX() * lateralDist,
                0,
                forward.getOffsetZ() * dist + lateral.getOffsetZ() * lateralDist);
    }

    public static void clearAllSegments(net.minecraft.server.world.ServerWorld world, BlockPos center, Direction forward, int baseY) {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            BlockBox bounds = createBounds(center, forward, baseY, i);
            for (int x = bounds.getMinX(); x <= bounds.getMaxX(); x++) {
                for (int z = bounds.getMinZ(); z <= bounds.getMaxZ(); z++) {
                    for (int y = bounds.getMinY(); y <= bounds.getMaxY(); y++) {
                        world.setBlockState(new BlockPos(x, y, z), net.minecraft.block.Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    public static void clearLoadedSegments(net.minecraft.server.world.ServerWorld world, BlockPos center, Direction forward, int baseY) {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            BlockBox bounds = createBounds(center, forward, baseY, i);
            int minChunkX = bounds.getMinX() >> 4;
            int maxChunkX = bounds.getMaxX() >> 4;
            int minChunkZ = bounds.getMinZ() >> 4;
            int maxChunkZ = bounds.getMaxZ() >> 4;
            for (int cx = minChunkX; cx <= maxChunkX; cx++) {
                for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                    if (!world.getChunkManager().isChunkLoaded(cx, cz)) {
                        continue;
                    }
                    BlockBox chunkBox = getChunkBox(world, cx, cz);
                    clearBoundsInChunk(world, bounds, chunkBox);
                }
            }
        }
    }

    public static void clearSegmentsInChunk(net.minecraft.server.world.ServerWorld world, BlockPos center, Direction forward, int baseY, net.minecraft.util.math.ChunkPos chunkPos) {
        BlockBox chunkBox = getChunkBox(world, chunkPos);
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            BlockBox bounds = createBounds(center, forward, baseY, i);
            clearBoundsInChunk(world, bounds, chunkBox);
        }
    }

    public static BlockBox getChunkBox(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.ChunkPos chunkPos) {
        return getChunkBox(world, chunkPos.x, chunkPos.z);
    }

    public static BlockBox getChunkBox(net.minecraft.server.world.ServerWorld world, int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;
        int minY = world.getBottomY();
        int maxY = world.getBottomY() + world.getDimension().height() - 1;
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockBox getCorridorBounds(BlockPos center, Direction forward, int baseY) {
        BlockBox bounds = createBounds(center, forward, baseY, 0);
        int minX = bounds.getMinX();
        int minY = bounds.getMinY();
        int minZ = bounds.getMinZ();
        int maxX = bounds.getMaxX();
        int maxY = bounds.getMaxY();
        int maxZ = bounds.getMaxZ();
        for (int i = 1; i < SEGMENT_COUNT; i++) {
            BlockBox segment = createBounds(center, forward, baseY, i);
            minX = Math.min(minX, segment.getMinX());
            minY = Math.min(minY, segment.getMinY());
            minZ = Math.min(minZ, segment.getMinZ());
            maxX = Math.max(maxX, segment.getMaxX());
            maxY = Math.max(maxY, segment.getMaxY());
            maxZ = Math.max(maxZ, segment.getMaxZ());
        }
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static void clearBoundsInChunk(StructureWorldAccess world, BlockBox bounds, BlockBox chunkBox) {
        int minX = Math.max(bounds.getMinX(), chunkBox.getMinX());
        int maxX = Math.min(bounds.getMaxX(), chunkBox.getMaxX());
        int minY = Math.max(bounds.getMinY(), chunkBox.getMinY());
        int maxY = Math.min(bounds.getMaxY(), chunkBox.getMaxY());
        int minZ = Math.max(bounds.getMinZ(), chunkBox.getMinZ());
        int maxZ = Math.min(bounds.getMaxZ(), chunkBox.getMaxZ());
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.setBlockState(new BlockPos(x, y, z), net.minecraft.block.Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
    }
}
