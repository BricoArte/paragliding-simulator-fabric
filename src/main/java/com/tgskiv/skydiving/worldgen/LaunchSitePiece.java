package com.tgskiv.skydiving.worldgen;

import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.SkydivingHandler;
import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import com.tgskiv.skydiving.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;

public class LaunchSitePiece extends StructurePiece {
    private static final Identifier LAUNCH_SITE_TEMPLATE = Identifier.of(ParaglidingSimulator.MOD_ID, "despegue");
    private static final Identifier FALLBACK_TEMPLATE = Identifier.of("minecraft", "despegue");
    private static final Identifier BASE_TEMPLATE = Identifier.of(ParaglidingSimulator.MOD_ID, "despegue_base");
    private static final Identifier BASE_FALLBACK_TEMPLATE = Identifier.of("minecraft", "despegue_base");
    private static final Identifier LEGACY_TEMPLATE = Identifier.of(ParaglidingSimulator.MOD_ID, "despegue");
    private static final Identifier LEGACY_FALLBACK = Identifier.of("minecraft", "despegue");
    // private static final int TEMPLATE_SIZE_X = 59;
    // private static final int TEMPLATE_SIZE_Y = 11;
    // private static final int TEMPLATE_SIZE_Z = 11;
    // private static final int TEMPLATE_PIVOT_X = 53;
    // private static final int TEMPLATE_PIVOT_Z = 5;
    private static final int TEMPLATE_SIZE_X = 59;
    private static final int TEMPLATE_SIZE_Y = 11;
    private static final int TEMPLATE_SIZE_Z = 11;
    private static final int TEMPLATE_PIVOT_X = 53;
    private static final int TEMPLATE_PIVOT_Z = 5;
    private static final int BASE_SIZE_X = 9;
    private static final int BASE_SIZE_Y = 1;
    private static final int BASE_SIZE_Z = 9;
    private static final BlockPos CHEST_LOCAL_POS = new BlockPos(56, 1, 2);
    private final BlockPos center;
    private final int radius;
    private final Direction bestDir;
    private boolean platformChecked = false;
    private boolean platformValid = true;
    private boolean placementNotified = false;

    public LaunchSitePiece(BlockPos center, int radius, Direction bestDir) {
        super(ModStructures.LAUNCH_SITE_PIECE, 0, createBounds(center, bestDir));
        this.center = center.toImmutable();
        this.radius = radius;
        this.bestDir = bestDir;
    }

    public LaunchSitePiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.LAUNCH_SITE_PIECE, nbt);
        this.center = new BlockPos(nbt.getInt("CenterX"), nbt.getInt("CenterY"), nbt.getInt("CenterZ"));
        this.radius = nbt.getInt("Radius");
        this.bestDir = Direction.byId(nbt.getInt("BestDir"));
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putInt("CenterX", center.getX());
        nbt.putInt("CenterY", center.getY());
        nbt.putInt("CenterZ", center.getZ());
        nbt.putInt("Radius", radius);
        nbt.putInt("BestDir", bestDir.getId());
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor accessor, ChunkGenerator generator, Random random,
                         BlockBox box, ChunkPos chunkPos, BlockPos pivot) {
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        if (!cfg.launchSitesEnabled) return;

        if (!platformChecked) {
            platformValid = !hasFluidOnPlatform(world, center, radius);
            platformChecked = true;
            if (!platformValid) {
                SkydivingHandler.onLaunchSiteFailFluid();
                if (world instanceof ServerWorld serverWorld && cfg.launchSitesDebug) {
                    SkydivingHandler.broadcastLaunchSiteDebug(serverWorld);
                }
                return;
            }
        } else if (!platformValid) {
            return;
        }

        long placeStart = System.nanoTime();
        boolean placedBase = placeBaseTemplate(world, center, bestDir, box);
        if (!placedBase) {
            return;
        }
        boolean placed;
        if (world instanceof ServerWorld serverWorld) {
            SkydivingHandler.scheduleLaunchSiteTemplatePlacement(serverWorld, center, bestDir);
            placed = true;
        } else {
            // During worldgen we must clip to the current chunk box to avoid far-chunk writes.
            placed = placeTemplate(world, center, bestDir, box);
        }
        if (!placed) {
            return;
        }
        if (!placementNotified && box.contains(center)) {
            placementNotified = true;
            SkydivingHandler.onLaunchSitePlaced(center, bestDir);
            long placeNanos = System.nanoTime() - placeStart;
            SkydivingHandler.onLaunchSitePlaceTime(placeNanos);
            SkydivingHandler.onLaunchSiteProcessTime(placeNanos);

            if (world instanceof ServerWorld serverWorld) {
                SkydivingHandler.broadcastLaunchSiteDebug(serverWorld);
                if (cfg.launchSitesDebug) {
                    ParaglidingSimulator.LOGGER.info("Launch site placed at {},{} (y={})",
                            center.getX(), center.getZ(), center.getY());
                }
            }
        }
    }


    static BlockBox createBounds(BlockPos center, Direction bestDir) {
        BlockPos start = computeBaseStart(center);
        int sizeX = BASE_SIZE_X;
        int sizeZ = BASE_SIZE_Z;
        int minX = start.getX();
        int minZ = start.getZ();
        int maxX = minX + sizeX - 1;
        int maxZ = minZ + sizeZ - 1;
        int minY = center.getY();
        int maxY = center.getY() + BASE_SIZE_Y - 1;
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static boolean hasFluidOnPlatform(StructureWorldAccess world, BlockPos center, int radius) {
        long start = System.nanoTime();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = center.add(dx, 0, dz);
                if (world.getFluidState(pos).isIn(FluidTags.WATER) || world.getFluidState(pos).isIn(FluidTags.LAVA)) {
                    long nanos = System.nanoTime() - start;
                    SkydivingHandler.onLaunchSiteFluidTime(nanos);
                    SkydivingHandler.onLaunchSiteProcessTime(nanos);
                    return true;
                }
                if (world.getFluidState(pos.down()).isIn(FluidTags.WATER) || world.getFluidState(pos.down()).isIn(FluidTags.LAVA)) {
                    long nanos = System.nanoTime() - start;
                    SkydivingHandler.onLaunchSiteFluidTime(nanos);
                    SkydivingHandler.onLaunchSiteProcessTime(nanos);
                    return true;
                }
            }
        }
        long nanos = System.nanoTime() - start;
        SkydivingHandler.onLaunchSiteFluidTime(nanos);
        SkydivingHandler.onLaunchSiteProcessTime(nanos);
        return false;
    }

    public static void buildPlatform(StructureWorldAccess world, BlockPos center, int radius) {
        BlockState topState = Blocks.DIRT_PATH.getDefaultState();
        int peakY = center.getY();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = center.getX() + dx;
                int z = center.getZ() + dz;
                int surfaceY = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, x, z);
                for (int y = surfaceY + 1; y <= peakY; y++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.getDefaultState(), 2);
                }
                for (int y = surfaceY; y > peakY; y--) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), 2);
                }
                world.setBlockState(new BlockPos(x, peakY, z), topState, 2);
            }
        }
    }

    public static void placeWindsock(StructureWorldAccess world, BlockPos center, int radius) {
        Direction best = Direction.NORTH;
        double bestDrop = Double.NEGATIVE_INFINITY;
        int peakY = center.getY();

        for (Direction dir : Direction.Type.HORIZONTAL) {
            double drop = averageDrop(world, center, peakY, dir, 6);
            if (drop > bestDrop) {
                bestDrop = drop;
                best = dir;
            }
        }

        int sockX = center.getX() + best.getOffsetX() * radius;
        int sockZ = center.getZ() + best.getOffsetZ() * radius;
        BlockPos sockPos = new BlockPos(sockX, peakY + 1, sockZ);
        if (world.getBlockState(sockPos).isAir()) {
            world.setBlockState(sockPos, ModBlocks.WINDSOCK.getDefaultState(), 2);
        }
    }


    public static boolean placeTemplate(StructureWorldAccess world, BlockPos center, Direction bestDir) {
        return placeTemplate(world, center, bestDir, null);
    }

    public static boolean placeTemplate(StructureWorldAccess world, BlockPos center, Direction bestDir, BlockBox bounds) {
        if (world.getServer() == null) {
            return false;
        }
        StructureTemplateManager templates = world.getServer().getStructureTemplateManager();
        java.util.Optional<StructureTemplate> templateOpt = templates.getTemplate(LAUNCH_SITE_TEMPLATE);
        if (templateOpt.isEmpty()) {
            templateOpt = templates.getTemplate(FALLBACK_TEMPLATE);
        }
        if (templateOpt.isEmpty()) {
            templateOpt = templates.getTemplate(LEGACY_TEMPLATE);
        }
        if (templateOpt.isEmpty()) {
            templateOpt = templates.getTemplate(LEGACY_FALLBACK);
        }
        if (templateOpt.isEmpty()) {
            RegistryEntryLookup<net.minecraft.block.Block> blocks =
                    world.getServer().getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
            templateOpt = loadTemplateFromResource(
                    world.getServer().getResourceManager(),
                    Identifier.of(ParaglidingSimulator.MOD_ID, "structures/despegue.nbt"),
                    blocks
            );
        }
        if (templateOpt.isEmpty()) {
            RegistryEntryLookup<net.minecraft.block.Block> blocks =
                    world.getServer().getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
            templateOpt = loadTemplateFromResource(
                    world.getServer().getResourceManager(),
                    Identifier.of("minecraft", "structures/despegue.nbt"),
                    blocks
            );
        }
        if (templateOpt.isEmpty()) {
            ParaglidingSimulator.LOGGER.warn("Launch site template not found ({} or {})",
                    LAUNCH_SITE_TEMPLATE, FALLBACK_TEMPLATE);
            return false;
        }
        StructureTemplate template = templateOpt.get();
        SkydivingServerConfig cfg = SkydivingHandler.getServerConfig();
        if (cfg.launchSitesDebug) {
            ParaglidingSimulator.LOGGER.info("Placing launch site template size {}", template.getSize());
        }
        BlockRotation rotation = rotationFor(bestDir);
        BlockPos start = computeStart(center, bestDir);




        StructurePlacementData placement = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(false);
        if (bounds != null) {
            placement.setBoundingBox(bounds);
        }
        template.place(world, start, start, placement, world.getRandom(), 2);
        applyLaunchSiteLoot(world, start, rotation);
        return true;
    }

    public static boolean placeBaseTemplate(StructureWorldAccess world, BlockPos center, Direction bestDir, BlockBox bounds) {
        if (world.getServer() == null) {
            return false;
        }
        StructureTemplateManager templates = world.getServer().getStructureTemplateManager();
        java.util.Optional<StructureTemplate> templateOpt = templates.getTemplate(BASE_TEMPLATE);
        if (templateOpt.isEmpty()) {
            templateOpt = templates.getTemplate(BASE_FALLBACK_TEMPLATE);
        }
        if (templateOpt.isEmpty()) {
            RegistryEntryLookup<net.minecraft.block.Block> blocks =
                    world.getServer().getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
            templateOpt = loadTemplateFromResource(
                    world.getServer().getResourceManager(),
                    Identifier.of(ParaglidingSimulator.MOD_ID, "structures/despegue_base.nbt"),
                    blocks
            );
        }
        if (templateOpt.isEmpty()) {
            RegistryEntryLookup<net.minecraft.block.Block> blocks =
                    world.getServer().getRegistryManager().getOrThrow(RegistryKeys.BLOCK);
            templateOpt = loadTemplateFromResource(
                    world.getServer().getResourceManager(),
                    Identifier.of("minecraft", "structures/despegue_base.nbt"),
                    blocks
            );
        }
        if (templateOpt.isEmpty()) {
            ParaglidingSimulator.LOGGER.warn("Launch base template not found ({} or {})",
                    BASE_TEMPLATE, BASE_FALLBACK_TEMPLATE);
            return false;
        }
        StructureTemplate template = templateOpt.get();
        BlockRotation rotation = rotationFor(bestDir);
        BlockPos start = computeBaseStart(center);

        StructurePlacementData placement = new StructurePlacementData()
                .setMirror(BlockMirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(true);
        if (bounds != null) {
            placement.setBoundingBox(bounds);
        }
        template.place(world, start, start, placement, world.getRandom(), 2);
        return true;
    }

    private static BlockRotation rotationFor(Direction bestDir) {
        return switch (bestDir) {
            case EAST -> BlockRotation.CLOCKWISE_180;
            case SOUTH -> BlockRotation.COUNTERCLOCKWISE_90;
            case WEST -> BlockRotation.NONE;
            case NORTH -> BlockRotation.CLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    private static BlockPos rotatePivot(BlockRotation rotation, int sizeX, int sizeZ, int pivotX, int pivotZ) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(pivotZ, 0, sizeX - 1 - pivotX);
            case CLOCKWISE_180 -> new BlockPos(sizeX - 1 - pivotX, 0, sizeZ - 1 - pivotZ);
            case COUNTERCLOCKWISE_90 -> new BlockPos(sizeZ - 1 - pivotZ, 0, pivotX);
            default -> new BlockPos(pivotX, 0, pivotZ);
        };
    }

    private static BlockPos rotateLocal(BlockRotation rotation, int sizeX, int sizeZ, BlockPos local) {
        int x = local.getX();
        int z = local.getZ();
        int rx;
        int rz;
        switch (rotation) {
            case CLOCKWISE_90 -> {
                rx = z;
                rz = sizeX - 1 - x;
            }
            case CLOCKWISE_180 -> {
                rx = sizeX - 1 - x;
                rz = sizeZ - 1 - z;
            }
            case COUNTERCLOCKWISE_90 -> {
                rx = sizeZ - 1 - z;
                rz = x;
            }
            default -> {
                rx = x;
                rz = z;
            }
        }
        return new BlockPos(rx, local.getY(), rz);
    }

    private static BlockPos computeStart(BlockPos center, Direction bestDir) {
        return switch (bestDir) {
            case SOUTH -> center.add(-5, -1, 53);
            case WEST -> center.add(-53, -1, -5);
            case NORTH -> center.add(5, -1, -53);
            case EAST -> center.add(53, -1, 5);
            default -> center.add(-5, -1, 53);
        };
    }

    // Poster is placed via structure NBT entities (ignoreEntities = false).

    private static BlockPos computeBaseStart(BlockPos center) {
        return center.add(-4, 0, -4);
    }

    private static void applyLaunchSiteLoot(StructureWorldAccess world, BlockPos start, BlockRotation rotation) {
        BlockPos rotated = rotateLocal(rotation, TEMPLATE_SIZE_X, TEMPLATE_SIZE_Z, CHEST_LOCAL_POS);
        BlockPos chestPos = start.add(rotated);
        net.minecraft.block.entity.BlockEntity be = world.getBlockEntity(chestPos);
        if (be instanceof net.minecraft.block.entity.LootableContainerBlockEntity lootable) {
            lootable.setLootTable(
                    RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(ParaglidingSimulator.MOD_ID, "chests/launch_site"))
            );
            lootable.setLootTableSeed(world.getRandom().nextLong());
        }
    }

    public static BlockBox getTemplateBounds(BlockPos center, Direction bestDir) {
        BlockRotation rotation = rotationFor(bestDir);
        BlockPos start = computeStart(center, bestDir);
        boolean swap = rotation == BlockRotation.CLOCKWISE_90 || rotation == BlockRotation.COUNTERCLOCKWISE_90;
        int sizeX = swap ? TEMPLATE_SIZE_Z : TEMPLATE_SIZE_X;
        int sizeZ = swap ? TEMPLATE_SIZE_X : TEMPLATE_SIZE_Z;
        int minX = start.getX();
        int minZ = start.getZ();
        int maxX = minX + sizeX - 1;
        int maxZ = minZ + sizeZ - 1;
        int minY = center.getY();
        int maxY = center.getY() + TEMPLATE_SIZE_Y - 1;
        return new BlockBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static BlockBox getCorridorBounds(BlockPos center, Direction bestDir) {
        BlockRotation rotation = rotationFor(bestDir);
        BlockPos start = computeStart(center, bestDir);
        int sizeX = TEMPLATE_SIZE_X;
        int sizeZ = TEMPLATE_SIZE_Z;

        int minX = 0;
        int maxX = 47;
        int minZ = 2;
        int maxZ = 7;

        int[] xs = new int[] { minX, maxX, minX, maxX };
        int[] zs = new int[] { minZ, minZ, maxZ, maxZ };

        int worldMinX = Integer.MAX_VALUE;
        int worldMaxX = Integer.MIN_VALUE;
        int worldMinZ = Integer.MAX_VALUE;
        int worldMaxZ = Integer.MIN_VALUE;

        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            int z = zs[i];
            int rx;
            int rz;
            switch (rotation) {
                case CLOCKWISE_90 -> {
                    rx = z;
                    rz = sizeX - 1 - x;
                }
                case CLOCKWISE_180 -> {
                    rx = sizeX - 1 - x;
                    rz = sizeZ - 1 - z;
                }
                case COUNTERCLOCKWISE_90 -> {
                    rx = sizeZ - 1 - z;
                    rz = x;
                }
                default -> {
                    rx = x;
                    rz = z;
                }
            }
            int wx = start.getX() + rx;
            int wz = start.getZ() + rz;
            if (wx < worldMinX) worldMinX = wx;
            if (wx > worldMaxX) worldMaxX = wx;
            if (wz < worldMinZ) worldMinZ = wz;
            if (wz > worldMaxZ) worldMaxZ = wz;
        }

        int minY = center.getY() - 3;
        int maxY = center.getY() + 10;
        return new BlockBox(worldMinX, minY, worldMinZ, worldMaxX, maxY, worldMaxZ);
    }

    public static void clearCorridorVegetationInChunk(ServerWorld world, BlockPos center, Direction bestDir, ChunkPos chunkPos) {
        BlockBox corridor = getCorridorBounds(center, bestDir);
        int minX = Math.max(corridor.getMinX(), chunkPos.getStartX());
        int maxX = Math.min(corridor.getMaxX(), chunkPos.getEndX());
        int minZ = Math.max(corridor.getMinZ(), chunkPos.getStartZ());
        int maxZ = Math.min(corridor.getMaxZ(), chunkPos.getEndZ());
        int minY = Math.max(corridor.getMinY(), world.getBottomY());
        int maxY = Math.min(corridor.getMaxY(), world.getBottomY() + world.getDimension().height() - 1);

        if (minX > maxX || minZ > maxZ || minY > maxY) {
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (isVegetation(state)) {
                        world.setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private static boolean isVegetation(BlockState state) {
        return state.isIn(BlockTags.LEAVES)
                || state.isIn(BlockTags.LOGS)
                || state.isIn(BlockTags.SAPLINGS)
                || state.isIn(BlockTags.FLOWERS)
                || state.isIn(BlockTags.REPLACEABLE_BY_TREES);
    }


    private static java.util.Optional<StructureTemplate> loadTemplateFromResource(
            ResourceManager resourceManager,
            Identifier id,
            RegistryEntryLookup<net.minecraft.block.Block> blockLookup
    ) {
        java.util.Optional<Resource> resource = resourceManager.getResource(id);
        if (resource.isEmpty()) {
            return java.util.Optional.empty();
        }
        try (java.io.InputStream input = resource.get().getInputStream()) {
            byte[] data = input.readAllBytes();
            boolean isGzip = data.length >= 2 && data[0] == (byte) 0x1f && data[1] == (byte) 0x8b;
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            NbtCompound nbt;
            if (isGzip) {
                nbt = NbtIo.readCompressed(bais, NbtSizeTracker.ofUnlimitedBytes());
            } else {
                net.minecraft.nbt.NbtElement element =
                        NbtIo.read(new java.io.DataInputStream(bais), NbtSizeTracker.ofUnlimitedBytes());
                if (!(element instanceof NbtCompound compound)) {
                    ParaglidingSimulator.LOGGER.warn("Launch site template {} is not a compound NBT", id);
                    return java.util.Optional.empty();
                }
                nbt = compound;
            }
            StructureTemplate template = new StructureTemplate();
            template.readNbt(blockLookup, nbt);
            return java.util.Optional.of(template);
        } catch (Exception ex) {
            ParaglidingSimulator.LOGGER.warn("Failed to load launch site template resource {}", id, ex);
            return java.util.Optional.empty();
        }
    }

    private static double averageDrop(StructureWorldAccess world, BlockPos center, int baseY, Direction dir, int dist) {
        double sum = 0.0;
        for (int i = 1; i <= dist; i++) {
            int x = center.getX() + dir.getOffsetX() * i;
            int z = center.getZ() + dir.getOffsetZ() * i;
            int y = world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE_WG, x, z);
            sum += (baseY - y);
        }
        return sum / dist;
    }
}
