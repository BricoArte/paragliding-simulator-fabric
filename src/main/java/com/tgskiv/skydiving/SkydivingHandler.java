package com.tgskiv.skydiving;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.tgskiv.ParaglidingSimulator;
import com.tgskiv.skydiving.configuration.SkydivingServerConfig;
import com.tgskiv.skydiving.configuration.StateSaverAndLoader;
import com.tgskiv.skydiving.entity.ParagliderEntity;
import com.tgskiv.skydiving.flight.ParagliderForces;
import com.tgskiv.skydiving.network.LaunchSiteDebugPayload;
import com.tgskiv.skydiving.network.ParagliderControlPayload;
import com.tgskiv.skydiving.network.ThermalSyncPayload;
import com.tgskiv.skydiving.network.ToggleAirflowDebugPayload;
import com.tgskiv.skydiving.network.WindConfigSyncPayload;
import com.tgskiv.skydiving.network.WindSyncPayload;
import com.tgskiv.skydiving.worldgen.LaunchSitePiece;
import com.tgskiv.skydiving.worldgen.LaunchSiteStructure;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import static com.tgskiv.skydiving.WindUtils.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class SkydivingHandler {

    private static int ticksUntilWindChange = 0;
    private static int thermalSyncTicks = 0;

    private static StateSaverAndLoader state;
    private static WindForecast windForecast;

    // Start Codigo nuestro
    private static Vec3d currentWindDirection = Vec3d.ZERO;
    private static double currentWindSpeed = 0.0;
    private static Vec3d fromWindDirection = Vec3d.ZERO;
    private static Vec3d targetWindDirection = Vec3d.ZERO;
    private static double pastWindSpeed = 0.0;
    private static double targetWindSpeed = 0.0;
    private static int ticksRemaining = 0;
    private static int ticksToTransition = 1;
    private static int launchSiteCount = 0;
    private static int launchSiteAttempts = 0;
    private static int launchSiteChunkLoads = 0;
    private static int launchSiteChunkLoadsWindow = 0;
    private static long launchSiteChunkLoadsWindowStartMs = 0L;
    private static final java.util.Map<Long, Long> launchSiteChunkLoadsWindowMap = new java.util.HashMap<>();
    private static int launchSiteEvalCandidates = 0;
    private static int launchSiteEvalCacheHits = 0;
    private static int launchSiteHeightCallsThisSecond = 0;
    private static int launchSiteHeightCallsPerSecond = 0;
    private static int launchSiteHeightCallsTicks = 20;
    private static long launchSiteAttemptTotalNanos = 0L;
    private static long launchSiteLastAttemptNanos = 0L;
    private static long launchSiteProcessTotalNanos = 0L;
    private static long launchSiteHeightNanos = 0L;
    private static long launchSiteHeightCenterNanos = 0L;
    private static long launchSiteHeightCornerNanos = 0L;
    private static long launchSitePeakNanos = 0L;
    private static long launchSiteEdgeNanos = 0L;
    private static long launchSiteForwardNanos = 0L;
    private static long launchSiteFlatNanos = 0L;
    private static long launchSiteFluidNanos = 0L;
    private static long launchSitePlaceNanos = 0L;
    private static long launchSiteCacheNanos = 0L;
    private static long launchSiteNearNanos = 0L;
    private static long launchSiteChanceNanos = 0L;
    private static long launchSiteGameStartNanos = 0L;
    private static int launchSiteFailChance = 0;
    private static int launchSiteFailNearPlaced = 0;
    private static int launchSiteFailNearInflight = 0;
    private static int launchSiteFailMinHeight = 0;
    private static int launchSiteFailHeightCenter = 0;
    private static int launchSiteFailHeightCorners = 0;
    private static int launchSiteFailDominance = 0;
    private static int launchSiteFailFlat = 0;
    private static int launchSiteFailFluid = 0;
    private static BlockPos launchSiteLastAttemptPos = null;
    private static LaunchSiteStructure.FailReason launchSiteLastAttemptReason = LaunchSiteStructure.FailReason.NONE;
    private static int launchSiteLastPeakY = 0;
    private static double launchSiteLastEdgeDrop = 0.0;
    private static double launchSiteLastForwardDrop = 0.0;
    private static int launchSiteLastFlatMinY = 0;
    private static int launchSiteLastFlatMaxY = 0;
    private static final Deque<LaunchSiteInfo> recentLaunchSites = new ArrayDeque<>();
    private static final Set<Long> launchSiteChunks = new HashSet<>();
    private static final Deque<LaunchSiteReservation> launchSiteReservations = new ArrayDeque<>();
    private static final Deque<LaunchSiteReservation> launchSitePendingReservations = new ArrayDeque<>();
    private static final Deque<LaunchSiteReservation> launchSiteInflightReservations = new ArrayDeque<>();
    private static final Deque<FenceUpdate> pendingFenceUpdates = new ArrayDeque<>();
    private static final java.util.concurrent.ConcurrentHashMap<Long, LaunchSitePlacement> pendingLaunchSitePlacements =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final int LAUNCH_SITE_RECENT_MAX = 256;
    private static final int LAUNCH_SITE_RESERVATION_MAX = 2048;
    private static final int LAUNCH_SITE_PENDING_TTL_TICKS = 20 * 90;
    private static volatile long launchSiteOverworldTick = 0L;
    private static final int LAUNCH_SITE_REGION_SIZE = 32;
    private static final int LAUNCH_SITE_REGION_BITS = LAUNCH_SITE_REGION_SIZE * LAUNCH_SITE_REGION_SIZE;
    private static final int LAUNCH_SITE_REGION_MAX = 4096;
    private static final java.util.Map<Long, java.util.BitSet> launchSiteEvaluatedRegions =
            new java.util.LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<Long, java.util.BitSet> eldest) {
                    return size() > LAUNCH_SITE_REGION_MAX;
                }
            };

    public static Vec3d getCurrentWindDirection() { return currentWindDirection; }
    public static double getCurrentWindSpeed() { return currentWindSpeed; }
    public static com.tgskiv.skydiving.configuration.SkydivingServerConfig getServerConfig() {
        return state == null ? new com.tgskiv.skydiving.configuration.SkydivingServerConfig() : state.skydivingConfig;
    }

    public static void applyThermalPresets(SkydivingServerConfig.ThermalAmountPreset amount,
                                           SkydivingServerConfig.ThermalIntensityPreset intensity,
                                           SkydivingServerConfig.ThermalSizePreset size,
                                           SkydivingServerConfig.ThermalHeightPreset height) {
        if (state == null) return;
        state.skydivingConfig.thermalAmountPreset = amount;
        state.skydivingConfig.thermalIntensityPreset = intensity;
        state.skydivingConfig.thermalSizePreset = size;
        state.skydivingConfig.thermalHeightPreset = height;
        state.markDirty();
    }

    public static void applyThermalsEnabled(boolean enabled) {
        if (state == null) return;
        state.skydivingConfig.thermalsEnabled = enabled;
        state.markDirty();
    }

    public static void applyThermalGenerationDistancePreset(SkydivingServerConfig.ThermalGenerationDistancePreset preset) {
        if (state == null) return;
        state.skydivingConfig.thermalGenerationDistancePreset = preset;
        state.markDirty();
    }

    public static void applyLaunchGenerationPreset(SkydivingServerConfig.LaunchGenerationPreset preset) {
        if (state == null) return;
        SkydivingServerConfig cfg = state.skydivingConfig;
        cfg.launchGenerationPreset = preset;
        switch (preset) {
            case NONE -> {
                cfg.launchSitesWorldgenEnabled = false;
                cfg.launchAttemptChance = 0f;
                cfg.launchSpacingChunks = 0;
            }
            case LOW -> {
                cfg.launchSitesWorldgenEnabled = true;
                cfg.launchAttemptChance = 0.35f;
                cfg.launchSpacingChunks = 12;
            }
            case STANDARD -> {
                cfg.launchSitesWorldgenEnabled = true;
                cfg.launchAttemptChance = 0.7f;
                cfg.launchSpacingChunks = 6;
            }
            case HIGH -> {
                cfg.launchSitesWorldgenEnabled = true;
                cfg.launchAttemptChance = 1.0f;
                cfg.launchSpacingChunks = 0;
            }
        }
        state.markDirty();
    }
    // End Codigo nuestro

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            state = StateSaverAndLoader.getServerState(server);
            windForecast = new WindForecast(state.skydivingConfig);
            launchSiteCount = 0;
            launchSiteAttempts = 0;
            launchSiteChunkLoads = 0;
            launchSiteChunkLoadsWindow = 0;
            launchSiteChunkLoadsWindowStartMs = 0L;
            launchSiteChunkLoadsWindowMap.clear();
            launchSiteEvalCandidates = 0;
            launchSiteEvalCacheHits = 0;
            launchSiteHeightCallsThisSecond = 0;
            launchSiteHeightCallsPerSecond = 0;
            launchSiteHeightCallsTicks = 20;
            launchSiteAttemptTotalNanos = 0L;
            launchSiteLastAttemptNanos = 0L;
            launchSiteProcessTotalNanos = 0L;
            launchSiteHeightNanos = 0L;
            launchSiteHeightCenterNanos = 0L;
            launchSiteHeightCornerNanos = 0L;
            launchSitePeakNanos = 0L;
            launchSiteEdgeNanos = 0L;
            launchSiteForwardNanos = 0L;
            launchSiteFlatNanos = 0L;
            launchSiteFluidNanos = 0L;
            launchSitePlaceNanos = 0L;
            launchSiteCacheNanos = 0L;
            launchSiteNearNanos = 0L;
            launchSiteChanceNanos = 0L;
            launchSiteGameStartNanos = 0L;
            launchSiteFailChance = 0;
            launchSiteFailNearPlaced = 0;
            launchSiteFailNearInflight = 0;
            launchSiteFailMinHeight = 0;
            launchSiteFailHeightCenter = 0;
            launchSiteFailHeightCorners = 0;
            launchSiteFailDominance = 0;
            launchSiteFailFlat = 0;
            launchSiteFailFluid = 0;
            launchSiteLastAttemptPos = null;
            launchSiteLastAttemptReason = LaunchSiteStructure.FailReason.NONE;
            launchSiteLastPeakY = 0;
            launchSiteLastEdgeDrop = 0.0;
            launchSiteLastForwardDrop = 0.0;
            launchSiteLastFlatMinY = 0;
            launchSiteLastFlatMaxY = 0;
            recentLaunchSites.clear();
            launchSiteChunks.clear();
            launchSiteReservations.clear();
            launchSitePendingReservations.clear();
            launchSiteInflightReservations.clear();
            launchSiteOverworldTick = 0L;
            synchronized (launchSiteEvaluatedRegions) {
                launchSiteEvaluatedRegions.clear();
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(SkydivingHandler::onWorldTick);
        CommandRegistrationCallback.EVENT.register(SkydivingHandler::registerCommands);
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (!world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) return;
            SkydivingServerConfig cfg = getServerConfig();
            launchSiteChunkLoads++;
            long nowMs = System.currentTimeMillis();
            if (launchSiteChunkLoadsWindowStartMs == 0L) {
                launchSiteChunkLoadsWindowStartMs = nowMs;
            } else if (nowMs - launchSiteChunkLoadsWindowStartMs >= 60_000L) {
                launchSiteChunkLoadsWindowStartMs = nowMs;
                launchSiteChunkLoadsWindow = 0;
                launchSiteChunkLoadsWindowMap.clear();
            }
            long packed = chunk.getPos().toLong();
            Long lastSeen = launchSiteChunkLoadsWindowMap.get(packed);
            if (lastSeen == null || lastSeen < launchSiteChunkLoadsWindowStartMs) {
                launchSiteChunkLoadsWindowMap.put(packed, nowMs);
                launchSiteChunkLoadsWindow++;
            }
            if (state != null && state.skydivingConfig.launchSitesDebug) {
                broadcastLaunchSiteDebug(world);
            }

        });

        // Client to Server
        // Sends wind configuration when user saves the settings
        PayloadTypeRegistry.playC2S().register(WindConfigSyncPayload.PAYLOAD_ID, WindConfigSyncPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(WindConfigSyncPayload.PAYLOAD_ID, (payload, context) -> {
            state.updateSettingsWithPayload(payload);
            windForecast.repopulateForecast();
            ticksUntilWindChange = 0;

            context.player().sendMessage(
                    Text.translatable("message.paraglidingsimulator.settings_saved")
                            .formatted(Formatting.GREEN)
            );
        });

        // Load the settings when player joins the server
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            WindConfigSyncPayload payload = new WindConfigSyncPayload(
                    state.skydivingConfig.ticksPerWindChange,
                    state.skydivingConfig.windRotationDegrees,
                    state.skydivingConfig.maxSpeedDelta,
                    state.skydivingConfig.maxWindSpeed,
                    state.skydivingConfig.minWindSpeed
            );
            ServerPlayNetworking.send(handler.player, payload);

            // Enviar viento actual al cliente al entrar para evitar desincronizacion en HUD
            WindSyncPayload windPayload = new WindSyncPayload(currentWindDirection, currentWindSpeed);
            ServerPlayNetworking.send(handler.player, windPayload);
            sendLaunchSiteDebug(handler.player);

            if (state != null) {
                ServerPlayerEntity player = handler.player;
                if (!state.hasReceivedTutorial(player.getUuid())) {
                    ItemStack book = createTutorialBook(player);
                    boolean added = player.getInventory().insertStack(book);
                    if (!added) {
                        player.dropItem(book, false);
                    }
                    state.markTutorialReceived(player.getUuid());
                }
            }
        });

        // Start Codigo nuestro
        PayloadTypeRegistry.playC2S().register(ParagliderControlPayload.PAYLOAD_ID, ParagliderControlPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ParagliderControlPayload.PAYLOAD_ID, (payload, context) -> {
            if (context.player().getVehicle() instanceof ParagliderEntity pg) {
                pg.setTurnInput(payload.turn());
                pg.setGroundInput(payload.forward(), payload.sideways());
            }
        });
        // End Codigo nuestro
    }

    public static void onLaunchSitePlaced(BlockPos pos, Direction dir) {
        launchSiteCount++;
        recentLaunchSites.addFirst(new LaunchSiteInfo(pos.toImmutable(), dir));
        launchSiteChunks.add(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4));
        SkydivingServerConfig cfg = getServerConfig();
        if (cfg.launchSpacingChunks > 0) {
            reserveLaunchSiteArea(new ChunkPos(pos), cfg.launchSpacingChunks);
            clearPendingLaunchSiteReservation(new ChunkPos(pos), cfg.launchSpacingChunks);
        }
        while (recentLaunchSites.size() > LAUNCH_SITE_RECENT_MAX) {
            recentLaunchSites.removeLast();
        }
    }

    public static void scheduleLaunchSiteTemplatePlacement(ServerWorld world, BlockPos center, Direction dir) {
        if (world.getServer() == null) return;
        world.getServer().execute(() -> {
            BlockBox bounds = LaunchSitePiece.getTemplateBounds(center, dir);
            long key = launchSitePlacementKey(center, dir);
            pendingLaunchSitePlacements.putIfAbsent(key, new LaunchSitePlacement(center.toImmutable(), dir, bounds));
        });
    }

    public static void scheduleFenceUpdates(ServerWorld world, List<BlockPos> positions, int delayTicks) {
        if (positions == null || positions.isEmpty()) return;
        long executeTime = world.getTime() + Math.max(1, delayTicks);
        pendingFenceUpdates.addLast(new FenceUpdate(world.getRegistryKey(), executeTime, new ArrayList<>(positions)));
    }

    public static void onLaunchSiteGenerated(ServerWorld world, BlockPos pos, Direction dir) {
        onLaunchSitePlaced(pos, dir);
        broadcastLaunchSiteDebug(world);
    }

    private static ItemStack createTutorialBook(ServerPlayerEntity player) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<RawFilteredPair<Text>> pages = List.of(
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page1.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("Paragliding Simulator\n\n").formatted(Formatting.BLACK))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page1.body")
                                .formatted(Formatting.BLACK))),
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("W: ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.w")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("S: ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.s")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("A / D: ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.ad")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("W + A/D: ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.spin")
                                .formatted(Formatting.BLACK))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page2.tip")
                                .formatted(Formatting.DARK_RED))),
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page3.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page3.b1")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page3.b2")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page3.b3")
                                .formatted(Formatting.DARK_RED))),
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page4.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page4.b1")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page4.b2")
                                .formatted(Formatting.BLACK))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page4.b2b")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page4.b3")
                                .formatted(Formatting.DARK_RED))),
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page5.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("Vario: ").formatted(Formatting.DARK_PURPLE))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page5.vario")
                                .formatted(Formatting.BLACK))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page5.vario_levels")
                                .formatted(Formatting.DARK_GRAY))
                        .append(Text.literal("Casco de vuelo: ").formatted(Formatting.DARK_PURPLE))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page5.helmet")
                                .formatted(Formatting.BLACK))),
                RawFilteredPair.of(Text.empty()
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page6.title")
                                .formatted(Formatting.DARK_BLUE, Formatting.BOLD))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page6.b1")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page6.b2")
                                .formatted(Formatting.BLACK))
                        .append(Text.literal("- ").formatted(Formatting.DARK_GREEN))
                        .append(Text.translatable("book.paraglidingsimulator.tutorial.page6.b3")
                                .formatted(Formatting.BLACK)))
        );
        WrittenBookContentComponent content = new WrittenBookContentComponent(
                RawFilteredPair.of(Text.translatable("book.paraglidingsimulator.tutorial.book_title").getString()),
                "Paragliding Simulator",
                0,
                pages,
                false
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    public static void onLaunchSiteAttempt() {
        launchSiteAttempts++;
    }

    public static void onLaunchSiteEvalCandidate() {
        launchSiteEvalCandidates++;
    }

    public static void onLaunchSiteEvalCacheHit() {
        launchSiteEvalCacheHits++;
    }

    public static void onLaunchSiteAttemptTime(long nanos) {
        launchSiteLastAttemptNanos = nanos;
        launchSiteAttemptTotalNanos += nanos;
    }

    public static void onLaunchSiteHeightQuery() {
        launchSiteHeightCallsThisSecond++;
    }

    public static void onLaunchSiteProcessTime(long nanos) {
        if (nanos > 0) {
            launchSiteProcessTotalNanos += nanos;
        }
    }

    public static void onLaunchSiteHeightTime(long nanos) {
        if (nanos > 0) {
            launchSiteHeightNanos += nanos;
        }
    }

    public static void onLaunchSiteHeightCenterTime(long nanos) {
        if (nanos > 0) {
            launchSiteHeightCenterNanos += nanos;
        }
    }

    public static void onLaunchSiteHeightCornerTime(long nanos) {
        if (nanos > 0) {
            launchSiteHeightCornerNanos += nanos;
        }
    }

    public static void onLaunchSitePeakTime(long nanos) {
        if (nanos > 0) {
            launchSitePeakNanos += nanos;
        }
    }

    public static void onLaunchSiteEdgeTime(long nanos) {
        if (nanos > 0) {
            launchSiteEdgeNanos += nanos;
        }
    }

    public static void onLaunchSiteForwardTime(long nanos) {
        if (nanos > 0) {
            launchSiteForwardNanos += nanos;
        }
    }

    public static void onLaunchSiteFlatTime(long nanos) {
        if (nanos > 0) {
            launchSiteFlatNanos += nanos;
        }
    }

    public static void onLaunchSiteFluidTime(long nanos) {
        if (nanos > 0) {
            launchSiteFluidNanos += nanos;
        }
    }

    public static void onLaunchSitePlaceTime(long nanos) {
        if (nanos > 0) {
            launchSitePlaceNanos += nanos;
        }
    }

    public static void onLaunchSiteCacheTime(long nanos) {
        if (nanos > 0) {
            launchSiteCacheNanos += nanos;
        }
    }

    public static void onLaunchSiteNearTime(long nanos) {
        if (nanos > 0) {
            launchSiteNearNanos += nanos;
        }
    }

    public static void onLaunchSiteChanceTime(long nanos) {
        if (nanos > 0) {
            launchSiteChanceNanos += nanos;
        }
    }

    public static void onLaunchSiteFailChance() {
        launchSiteFailChance++;
    }

    public static void onLaunchSiteFailNearPlaced() {
        launchSiteFailNearPlaced++;
    }

    public static void onLaunchSiteFailNearInflight() {
        launchSiteFailNearInflight++;
    }

    public static boolean tryMarkLaunchSiteEvaluated(ChunkPos chunkPos) {
        long start = System.nanoTime();
        int regionX = Math.floorDiv(chunkPos.x, LAUNCH_SITE_REGION_SIZE);
        int regionZ = Math.floorDiv(chunkPos.z, LAUNCH_SITE_REGION_SIZE);
        long key = (((long) regionX) << 32) ^ (regionZ & 0xffffffffL);
        int localX = Math.floorMod(chunkPos.x, LAUNCH_SITE_REGION_SIZE);
        int localZ = Math.floorMod(chunkPos.z, LAUNCH_SITE_REGION_SIZE);
        int index = localZ * LAUNCH_SITE_REGION_SIZE + localX;
        synchronized (launchSiteEvaluatedRegions) {
            java.util.BitSet bits = launchSiteEvaluatedRegions.get(key);
            if (bits == null) {
                bits = new java.util.BitSet(LAUNCH_SITE_REGION_BITS);
                launchSiteEvaluatedRegions.put(key, bits);
            }
            if (bits.get(index)) {
                onLaunchSiteCacheTime(System.nanoTime() - start);
                return false;
            }
            bits.set(index);
            onLaunchSiteCacheTime(System.nanoTime() - start);
            return true;
        }
    }

    public static boolean hasLaunchSiteNear(ChunkPos center, int radiusChunks) {
        if (radiusChunks <= 0 || launchSiteChunks.isEmpty()) {
            return hasReservedLaunchSiteNear(center, radiusChunks);
        }
        int cx = center.x;
        int cz = center.z;
        for (long packed : launchSiteChunks) {
            int lx = ChunkPos.getPackedX(packed);
            int lz = ChunkPos.getPackedZ(packed);
            int dx = Math.abs(cx - lx);
            int dz = Math.abs(cz - lz);
            if (dx <= radiusChunks && dz <= radiusChunks) {
                return true;
            }
        }
        return hasReservedLaunchSiteNear(center, radiusChunks);
    }

    public static void reserveLaunchSitePendingArea(ChunkPos center, int radiusChunks) {
        if (radiusChunks <= 0) {
            return;
        }
        synchronized (launchSiteReservations) {
            prunePendingLaunchSiteReservations(launchSiteOverworldTick);
            launchSitePendingReservations.addLast(new LaunchSiteReservation(
                    center.x, center.z, radiusChunks, launchSiteOverworldTick + LAUNCH_SITE_PENDING_TTL_TICKS));
            while (launchSitePendingReservations.size() > LAUNCH_SITE_RESERVATION_MAX) {
                launchSitePendingReservations.pollFirst();
            }
        }
    }

    public static LaunchSiteReservation tryReserveLaunchSiteInFlight(ChunkPos center, int radiusChunks) {
        if (radiusChunks <= 0) {
            return new LaunchSiteReservation(center.x, center.z, 0, Long.MAX_VALUE);
        }
        synchronized (launchSiteInflightReservations) {
            for (LaunchSiteReservation reservation : launchSiteInflightReservations) {
                int dx = Math.abs(center.x - reservation.centerX);
                int dz = Math.abs(center.z - reservation.centerZ);
                int radius = Math.max(radiusChunks, reservation.radiusChunks);
                if (dx <= radius && dz <= radius) {
                    return null;
                }
            }
            LaunchSiteReservation reservation = new LaunchSiteReservation(center.x, center.z, radiusChunks, Long.MAX_VALUE);
            launchSiteInflightReservations.addLast(reservation);
            return reservation;
        }
    }

    public static void releaseLaunchSiteInFlight(LaunchSiteReservation reservation) {
        if (reservation == null) return;
        synchronized (launchSiteInflightReservations) {
            launchSiteInflightReservations.remove(reservation);
        }
    }

    private static boolean hasReservedLaunchSiteNear(ChunkPos center, int radiusChunks) {
        if (radiusChunks <= 0) {
            return false;
        }
        synchronized (launchSiteReservations) {
            prunePendingLaunchSiteReservations(launchSiteOverworldTick);
            if (launchSiteReservations.isEmpty() && launchSitePendingReservations.isEmpty()) {
                return false;
            }
            int cx = center.x;
            int cz = center.z;
            for (LaunchSiteReservation reservation : launchSiteReservations) {
                int dx = Math.abs(cx - reservation.centerX);
                int dz = Math.abs(cz - reservation.centerZ);
                if (dx <= reservation.radiusChunks && dz <= reservation.radiusChunks) {
                    return true;
                }
            }
            for (LaunchSiteReservation reservation : launchSitePendingReservations) {
                int dx = Math.abs(cx - reservation.centerX);
                int dz = Math.abs(cz - reservation.centerZ);
                if (dx <= reservation.radiusChunks && dz <= reservation.radiusChunks) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void reserveLaunchSiteArea(ChunkPos center, int radiusChunks) {
        if (radiusChunks <= 0) {
            return;
        }
        synchronized (launchSiteReservations) {
            launchSiteReservations.addLast(new LaunchSiteReservation(center.x, center.z, radiusChunks, Long.MAX_VALUE));
            while (launchSiteReservations.size() > LAUNCH_SITE_RESERVATION_MAX) {
                launchSiteReservations.pollFirst();
            }
        }
    }

    private static void clearPendingLaunchSiteReservation(ChunkPos center, int radiusChunks) {
        synchronized (launchSiteReservations) {
            prunePendingLaunchSiteReservations(launchSiteOverworldTick);
            Iterator<LaunchSiteReservation> it = launchSitePendingReservations.iterator();
            while (it.hasNext()) {
                LaunchSiteReservation reservation = it.next();
                int dx = Math.abs(center.x - reservation.centerX);
                int dz = Math.abs(center.z - reservation.centerZ);
                int radius = Math.max(radiusChunks, reservation.radiusChunks);
                if (dx <= radius && dz <= radius) {
                    it.remove();
                }
            }
        }
    }

    private static void prunePendingLaunchSiteReservations(long worldTick) {
        Iterator<LaunchSiteReservation> it = launchSitePendingReservations.iterator();
        while (it.hasNext()) {
            LaunchSiteReservation reservation = it.next();
            if (reservation.expiresAtTick <= worldTick) {
                it.remove();
            }
        }
    }

    public static void onLaunchSiteFailMinHeight() {
        launchSiteFailMinHeight++;
    }

    public static void onLaunchSiteFailHeightCenter() {
        launchSiteFailHeightCenter++;
    }

    public static void onLaunchSiteFailHeightCorners() {
        launchSiteFailHeightCorners++;
    }

    public static void onLaunchSiteFailDominance() {
        launchSiteFailDominance++;
    }

    public static void onLaunchSiteFailFlat() {
        launchSiteFailFlat++;
    }

    public static void onLaunchSiteFailFluid() {
        launchSiteFailFluid++;
    }

    public static void onLaunchSiteManualAttempt(ServerWorld world, BlockPos attemptPos, LaunchSiteStructure.AttemptResult result) {
        BlockPos pos = attemptPos;
        if (result.peak() != null) {
            pos = new BlockPos(result.peak().x(), result.peak().y(), result.peak().z());
        }
        launchSiteLastAttemptPos = pos.toImmutable();
        launchSiteLastAttemptReason = result.reason();
        if (result.peak() != null) {
            launchSiteLastPeakY = result.peak().y();
            launchSiteLastEdgeDrop = result.edgeDrop();
            launchSiteLastForwardDrop = result.forwardDrop();
        }
        if (result.flat() != null) {
            launchSiteLastFlatMinY = result.flat().minY();
            launchSiteLastFlatMaxY = result.flat().maxY();
        }
        broadcastLaunchSiteDebug(world);
    }

    private static void sendLaunchSiteDebug(ServerPlayerEntity player) {
        boolean hasLast = launchSiteLastAttemptPos != null;
        BlockPos lastPos = hasLast ? launchSiteLastAttemptPos : BlockPos.ORIGIN;
        double gameSeconds = launchSiteGameStartNanos == 0L ? 0.0
                : (System.nanoTime() - launchSiteGameStartNanos) / 1_000_000_000.0;
        LaunchSiteDebugPayload payload = new LaunchSiteDebugPayload(
                launchSiteCount,
                launchSiteAttempts,
                launchSiteChunkLoads,
                launchSiteChunkLoadsWindow,
                launchSiteEvalCandidates,
                launchSiteEvalCacheHits,
                launchSiteHeightCallsPerSecond,
                launchSiteLastAttemptNanos / 1_000_000.0,
                launchSiteAttempts == 0 ? 0.0 : (launchSiteAttemptTotalNanos / 1_000_000.0) / launchSiteAttempts,
                launchSiteProcessTotalNanos / 1_000_000_000.0,
                gameSeconds,
                launchSiteHeightNanos / 1_000_000_000.0,
                launchSiteHeightCenterNanos / 1_000_000_000.0,
                launchSiteHeightCornerNanos / 1_000_000_000.0,
                launchSitePeakNanos / 1_000_000_000.0,
                launchSiteEdgeNanos / 1_000_000_000.0,
                launchSiteForwardNanos / 1_000_000_000.0,
                launchSiteFlatNanos / 1_000_000_000.0,
                launchSiteFluidNanos / 1_000_000_000.0,
                launchSitePlaceNanos / 1_000_000_000.0,
                launchSiteCacheNanos / 1_000_000_000.0,
                launchSiteNearNanos / 1_000_000_000.0,
                launchSiteChanceNanos / 1_000_000_000.0,
                launchSiteFailChance,
                launchSiteFailNearPlaced,
                launchSiteFailNearInflight,
                launchSiteFailMinHeight,
                launchSiteFailHeightCenter,
                launchSiteFailHeightCorners,
                launchSiteFailDominance,
                launchSiteFailFlat,
                launchSiteFailFluid,
                recentLaunchSites.stream().map(info -> info.pos).toList(),
                hasLast,
                lastPos,
                launchSiteLastAttemptReason.ordinal(),
                launchSiteLastPeakY,
                launchSiteLastEdgeDrop,
                launchSiteLastForwardDrop,
                launchSiteLastFlatMinY,
                launchSiteLastFlatMaxY
        );
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastLaunchSiteDebug(ServerWorld world) {
        boolean hasLast = launchSiteLastAttemptPos != null;
        BlockPos lastPos = hasLast ? launchSiteLastAttemptPos : BlockPos.ORIGIN;
        double gameSeconds = launchSiteGameStartNanos == 0L ? 0.0
                : (System.nanoTime() - launchSiteGameStartNanos) / 1_000_000_000.0;
        LaunchSiteDebugPayload payload = new LaunchSiteDebugPayload(
                launchSiteCount,
                launchSiteAttempts,
                launchSiteChunkLoads,
                launchSiteChunkLoadsWindow,
                launchSiteEvalCandidates,
                launchSiteEvalCacheHits,
                launchSiteHeightCallsPerSecond,
                launchSiteLastAttemptNanos / 1_000_000.0,
                launchSiteAttempts == 0 ? 0.0 : (launchSiteAttemptTotalNanos / 1_000_000.0) / launchSiteAttempts,
                launchSiteProcessTotalNanos / 1_000_000_000.0,
                gameSeconds,
                launchSiteHeightNanos / 1_000_000_000.0,
                launchSiteHeightCenterNanos / 1_000_000_000.0,
                launchSiteHeightCornerNanos / 1_000_000_000.0,
                launchSitePeakNanos / 1_000_000_000.0,
                launchSiteEdgeNanos / 1_000_000_000.0,
                launchSiteForwardNanos / 1_000_000_000.0,
                launchSiteFlatNanos / 1_000_000_000.0,
                launchSiteFluidNanos / 1_000_000_000.0,
                launchSitePlaceNanos / 1_000_000_000.0,
                launchSiteCacheNanos / 1_000_000_000.0,
                launchSiteNearNanos / 1_000_000_000.0,
                launchSiteChanceNanos / 1_000_000_000.0,
                launchSiteFailChance,
                launchSiteFailNearPlaced,
                launchSiteFailNearInflight,
                launchSiteFailMinHeight,
                launchSiteFailHeightCenter,
                launchSiteFailHeightCorners,
                launchSiteFailDominance,
                launchSiteFailFlat,
                launchSiteFailFluid,
                recentLaunchSites.stream().map(info -> info.pos).toList(),
                hasLast,
                lastPos,
                launchSiteLastAttemptReason.ordinal(),
                launchSiteLastPeakY,
                launchSiteLastEdgeDrop,
                launchSiteLastForwardDrop,
                launchSiteLastFlatMinY,
                launchSiteLastFlatMaxY
        );
        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(CommandManager.literal("wind")
                .then(CommandManager.literal("forecast")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            windForecast.showForecast(source.getPlayer());
                            return 1;
                        })
                )
                .then(CommandManager.literal("again")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            windForecast.repopulateForecast();
                            ticksUntilWindChange = 0;
                            source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.wind_forecast_regenerated"), false);
                            return 1;
                        })
                )
                .then(CommandManager.literal("hud")
                        .then(CommandManager.argument("visible", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    boolean visible = BoolArgumentType.getBool(context, "visible");
                                    ServerPlayerEntity player = source.getPlayer();

                                    ServerPlayNetworking.send(player, new ToggleAirflowDebugPayload(visible));
                                    source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.airflow_hud",
                                            Text.translatable(visible ? "gui.paraglidingsimulator.on" : "gui.paraglidingsimulator.off")), false);
                                    return 1;
                                })
                        )
                )
        );

        dispatcher.register(CommandManager.literal("launchsite")
                .then(CommandManager.literal("try")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            ServerWorld world = source.getWorld();
                            SkydivingServerConfig cfg = getServerConfig();
                            BlockPos attemptPos = player.getBlockPos();
                            ChunkPos chunkPos = new ChunkPos(attemptPos);
                            LaunchSiteStructure.AttemptResult result = LaunchSiteStructure.tryManual(
                                    world, chunkPos, true, cfg.launchSitesDebugMarkers && cfg.launchSitesDebug);
                            onLaunchSiteManualAttempt(world, attemptPos, result);
                            if (result.success()) {
                                source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.launch_try_success",
                                        String.format("%d %d %d", result.peak().x(), result.peak().y(), result.peak().z())), false);
                            } else {
                                source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.launch_try_fail",
                                        Text.translatable(reasonToTranslation(result.reason()))), false);
                            }
                            return 1;
                        })
                )
                .then(CommandManager.literal("force")
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayer();
                            ServerWorld world = source.getWorld();
                            SkydivingServerConfig cfg = getServerConfig();
                            BlockPos pos = player.getBlockPos();
                            Direction dir = Direction.fromRotation(player.getYaw());
                            boolean placed = LaunchSitePiece.placeTemplate(world, pos, dir);
                            if (!placed) {
                                LaunchSitePiece.buildPlatform(world, pos, cfg.launchFlatRadius);
                                LaunchSitePiece.placeWindsock(world, pos, cfg.launchFlatRadius);
                            }
                            onLaunchSiteGenerated(world, pos, dir);
                            source.sendFeedback(() -> Text.literal("Despegue forzado en " +
                                    pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
                            return 1;
                        })
                )
                .then(CommandManager.literal("worldgen")
                        .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    boolean enabled = BoolArgumentType.getBool(context, "enabled");
                                    state.skydivingConfig.launchSitesWorldgenEnabled = enabled;
                                    source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.launch_worldgen",
                                            Text.translatable(enabled ? "gui.paraglidingsimulator.on" : "gui.paraglidingsimulator.off")), false);
                                    return 1;
                                })
                        )
                )
        );

        dispatcher.register(CommandManager.literal("launchsites")
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            state.skydivingConfig.launchSitesEnabled = enabled;
                            state.skydivingConfig.launchSitesWorldgenEnabled = enabled;
                            source.sendFeedback(() -> Text.translatable("message.paraglidingsimulator.launch_sites",
                                    Text.translatable(enabled ? "gui.paraglidingsimulator.on" : "gui.paraglidingsimulator.off")), false);
                            return 1;
                        })
                )
        );

        dispatcher.register(CommandManager.literal("thermal")
                .then(CommandManager.literal("factors")
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("height", DoubleArgumentType.doubleArg(0.1))
                                        .then(CommandManager.argument("size", DoubleArgumentType.doubleArg(0.1))
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    double height = DoubleArgumentType.getDouble(context, "height");
                                                    double size = DoubleArgumentType.getDouble(context, "size");
                                                    ParagliderForces.setDailyThermalOverride(height, size);
                                                    source.sendFeedback(() -> Text.translatable(
                                                            "message.paraglidingsimulator.thermal_factors_set",
                                                            String.format("%.2f", height),
                                                            String.format("%.2f", size)), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(CommandManager.literal("clear")
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ParagliderForces.clearDailyThermalOverride();
                                    source.sendFeedback(() -> Text.translatable(
                                            "message.paraglidingsimulator.thermal_factors_cleared"), false);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static String reasonToTranslation(LaunchSiteStructure.FailReason reason) {
        return switch (reason) {
            case SPACING -> "message.paraglidingsimulator.launch_fail_spacing";
            case MIN_HEIGHT -> "message.paraglidingsimulator.launch_fail_height";
            case EDGE -> "message.paraglidingsimulator.launch_fail_edge";
            case FORWARD -> "message.paraglidingsimulator.launch_fail_forward";
            case FLAT -> "message.paraglidingsimulator.launch_fail_flat";
            case FLUID -> "message.paraglidingsimulator.launch_fail_fluid";
            default -> "message.paraglidingsimulator.launch_fail_unknown";
        };
    }

    private static void onWorldTick(ServerWorld world) {
        // Solo usar el overworld para evitar desincronizacion entre dimensiones
        if (!world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) return;
        launchSiteOverworldTick = world.getTime();
        synchronized (launchSiteReservations) {
            prunePendingLaunchSiteReservations(launchSiteOverworldTick);
        }

        if (launchSiteGameStartNanos == 0L) {
            launchSiteGameStartNanos = System.nanoTime();
        }

        windForecast.populateForecast();
        tickWindInterpolation();
        ParagliderForces.tickThermals(world, currentWindDirection, currentWindSpeed);

        thermalSyncTicks--;
        if (thermalSyncTicks <= 0) {
            thermalSyncTicks = 10;
            List<ParagliderForces.ThermalRenderData> renderData = ParagliderForces.getThermalsForSync(world.getTime());
            List<ThermalSyncPayload.ThermalSnapshot> snapshots = new ArrayList<>(renderData.size());
            for (ParagliderForces.ThermalRenderData thermal : renderData) {
                snapshots.add(new ThermalSyncPayload.ThermalSnapshot(
                        thermal.centerX(),
                        thermal.centerZ(),
                        thermal.cloudY(),
                        thermal.strengthFactor(),
                        thermal.sizeFactor()
                ));
            }
            ThermalSyncPayload payload = new ThermalSyncPayload(snapshots);
            for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }
        }

        launchSiteHeightCallsTicks--;
        if (launchSiteHeightCallsTicks <= 0) {
            launchSiteHeightCallsPerSecond = launchSiteHeightCallsThisSecond;
            launchSiteHeightCallsThisSecond = 0;
            launchSiteHeightCallsTicks = 20;
        }

        if (ticksUntilWindChange <= 0) {
            applyNextWindChange(world);
            ticksUntilWindChange = state.skydivingConfig.ticksPerWindChange;
        } else {
            ticksUntilWindChange--;
        }

        if (!pendingFenceUpdates.isEmpty()) {
            Iterator<FenceUpdate> it = pendingFenceUpdates.iterator();
            long now = world.getTime();
            while (it.hasNext()) {
                FenceUpdate update = it.next();
                if (!update.worldKey.equals(world.getRegistryKey())) {
                    continue;
                }
                if (now < update.executeTime) {
                    continue;
                }
                for (BlockPos pos : update.positions) {
                    if (!world.isChunkLoaded(pos)) {
                        continue;
                    }
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.OAK_FENCE)) {
                        world.updateNeighbors(pos, state.getBlock());
                    }
                }
                it.remove();
            }
        }

        if (!pendingLaunchSitePlacements.isEmpty()) {
            for (java.util.Map.Entry<Long, LaunchSitePlacement> entry : pendingLaunchSitePlacements.entrySet()) {
                LaunchSitePlacement placement = entry.getValue();
                if (!areAllChunksLoaded(world, placement.bounds)) {
                    continue;
                }
                LaunchSitePiece.placeTemplate(world, placement.center, placement.dir, null);
                pendingLaunchSitePlacements.remove(entry.getKey());
            }
        }
    }

    private static void applyNextWindChange(ServerWorld world) {
        WindChange change = windForecast.poll();
        if (change == null) return;

        // Start Codigo nuestro
        // Guardar viento actual para que lo use el parapente-entidad
        fromWindDirection = currentWindDirection;
        pastWindSpeed = currentWindSpeed;
        targetWindDirection = change.direction.normalize();
        targetWindSpeed = change.speed;
        ticksToTransition = Math.max(state.skydivingConfig.ticksPerWindChange / 3, 1);
        ticksRemaining = ticksToTransition;

        if (targetWindDirection.lengthSquared() < 0.0001) {
            targetWindDirection = new Vec3d(0.0001, 0, 0.0001);
        }
        // End Codigo nuestro

        WindSyncPayload payload = new WindSyncPayload(change.direction, change.speed);

        for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
            player.sendMessage(
                    Text.translatable("message.paraglidingsimulator.wind_updated",
                            windToString(change.direction, change.speed, state.skydivingConfig))
                            .formatted(Formatting.AQUA),
                    false
            );
        }
    }

    private static void tickWindInterpolation() {
        if (ticksRemaining <= 0) {
            currentWindDirection = targetWindDirection;
            currentWindSpeed = targetWindSpeed;
            return;
        }

        double t = 1.0 - (ticksRemaining / (double) ticksToTransition);
        currentWindDirection = slerp(fromWindDirection, targetWindDirection, t);
        currentWindSpeed = lerp(pastWindSpeed, targetWindSpeed, t);
        ticksRemaining--;
    }

    private static Vec3d slerp(Vec3d from, Vec3d to, double fraction) {
        double dot = from.dotProduct(to);
        dot = Math.min(Math.max(dot, -1.0), 1.0);

        double theta = Math.acos(dot) * fraction;
        Vec3d relativeVec = to.subtract(from.multiply(dot));
        if (relativeVec.lengthSquared() == 0) return from;
        relativeVec = relativeVec.normalize();
        return from.multiply(Math.cos(theta)).add(relativeVec.multiply(Math.sin(theta)));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private record LaunchSiteInfo(BlockPos pos, Direction dir) {}
    private record FenceUpdate(net.minecraft.registry.RegistryKey<World> worldKey, long executeTime, List<BlockPos> positions) {}
    private record LaunchSitePlacement(BlockPos center, Direction dir, BlockBox bounds) {}

    private static long launchSitePlacementKey(BlockPos center, Direction dir) {
        long base = center.asLong();
        return base ^ ((long) dir.getId() << 48);
    }

    private static boolean areAllChunksLoaded(ServerWorld world, BlockBox bounds) {
        int minChunkX = bounds.getMinX() >> 4;
        int maxChunkX = bounds.getMaxX() >> 4;
        int minChunkZ = bounds.getMinZ() >> 4;
        int maxChunkZ = bounds.getMaxZ() >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static final class LaunchSiteReservation {
        private final int centerX;
        private final int centerZ;
        private final int radiusChunks;
        private final long expiresAtTick;

        private LaunchSiteReservation(int centerX, int centerZ, int radiusChunks, long expiresAtTick) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radiusChunks = radiusChunks;
            this.expiresAtTick = expiresAtTick;
        }
    }
}

