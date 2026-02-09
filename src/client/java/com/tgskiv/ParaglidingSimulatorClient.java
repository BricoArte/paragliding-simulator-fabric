package com.tgskiv;


import com.tgskiv.skydiving.blocks.ModModelLayers;
import com.tgskiv.skydiving.blocks.WindsockModel;
import com.tgskiv.skydiving.flight.FlightUtils;
import com.tgskiv.skydiving.flight.ParagliderForces;
import com.tgskiv.skydiving.flight.WindInterpolator;
import com.tgskiv.skydiving.menu.SkydivingClientConfig;
import com.tgskiv.skydiving.menu.ModScreenHandlers;
import com.tgskiv.skydiving.menu.VarioConfigHandledScreen;
import com.tgskiv.skydiving.network.LaunchSiteDebugPayload;
import com.tgskiv.skydiving.network.ToggleAirflowDebugPayload;
import com.tgskiv.skydiving.network.ThermalSyncPayload;
import com.tgskiv.skydiving.network.WindConfigSyncPayload;
import com.tgskiv.skydiving.network.WindSyncPayload;
import com.tgskiv.skydiving.registry.ModBlockEntities;
import com.tgskiv.skydiving.registry.ModItems;
import com.tgskiv.skydiving.ui.AirflowDebugOverlay;
import com.tgskiv.skydiving.ui.ChunkLoadDebugState;
import com.tgskiv.skydiving.ui.DebugMetricsClipboard;
import com.tgskiv.skydiving.ui.LaunchSiteDebugState;
import com.tgskiv.skydiving.ui.VariometerOverlay;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.text.Text;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Vec3d;
import com.tgskiv.skydiving.blocks.WindsockBlockEntityRenderer;

// Start Codigo nuestro
import com.tgskiv.skydiving.entity.ParagliderEntity;
import com.tgskiv.skydiving.network.ParagliderControlPayload;

import com.tgskiv.skydiving.registry.ModEntities;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.tgskiv.skydiving.render.ParagliderRenderer;
import com.tgskiv.skydiving.model.ParagliderModel;
import com.tgskiv.skydiving.render.ThermalCloudRenderer;
// End Codigo nuestro

import net.minecraft.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
public class ParaglidingSimulatorClient implements ClientModInitializer {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static int dismountWarnCooldown = 0;
    private static int varioCooldown = 0;
    private static int spinTicks = 0;
    private static int windSoundCooldown = 0;
    private static final int SPIN_WIND_SOUND_TICKS = ParagliderEntity.SPIN_TRIGGER_TICKS + 20;
    private static boolean windSoundPlaying = false;
    private static boolean pendingDismountWarn = false;

	@Override
	public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ParagliderForces.clearClientThermalSnapshot()
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("psmetrics")
                        .then(ClientCommandManager.literal("copy")
                                .executes(context -> {
                                    String text = DebugMetricsClipboard.buildMetricsText(mc);
                                    mc.keyboard.setClipboard(text);
                                    if (mc.player != null) {
                                        mc.player.sendMessage(
                                                Text.translatable("message.paraglidingsimulator.metrics_copied"),
                                                false
                                        );
                                    }
                                    return 1;
                                })
                        )));

        // Subscribe to receiving the wind updates from the server
        ClientPlayNetworking.registerGlobalReceiver(
                WindSyncPayload.PAYLOAD_ID,
                (payload, context) ->
                        context.client().execute(() -> {
                            WindInterpolator.updateTarget(payload.direction(), payload.speed());
                        })

        );

		// Subscribe to receiving the wind configuration on joining the world
		ClientPlayNetworking.registerGlobalReceiver(WindConfigSyncPayload.PAYLOAD_ID, (payload, context) ->
				context.client().execute(() -> {
					SkydivingClientConfig.ticksPerWindChange = payload.ticksPerWindChange();
					SkydivingClientConfig.windRotationDegrees = payload.windRotationDegrees();
					SkydivingClientConfig.maxSpeedDelta = payload.maxSpeedDelta();
					SkydivingClientConfig.maxWindSpeed = payload.maxWindSpeed();
					SkydivingClientConfig.minWindSpeed = payload.minWindSpeed();
				})
		);

        ClientPlayNetworking.registerGlobalReceiver(ThermalSyncPayload.PAYLOAD_ID, (payload, context) ->
                context.client().execute(() -> {
                    List<ParagliderForces.ThermalRenderData> renderData = new ArrayList<>(payload.thermals().size());
                    for (ThermalSyncPayload.ThermalSnapshot thermal : payload.thermals()) {
                        renderData.add(new ParagliderForces.ThermalRenderData(
                                thermal.centerX(),
                                thermal.centerZ(),
                                thermal.cloudY(),
                                thermal.factor(),
                                thermal.sizeFactor()
                        ));
                    }
                    ParagliderForces.setClientThermalSnapshot(renderData);
                })
        );

		// Subscribe to Hud display command
		ClientPlayNetworking.registerGlobalReceiver(ToggleAirflowDebugPayload.PAYLOAD_ID, (payload, context) ->
				context.client().execute(() ->
					AirflowDebugOverlay.visible = payload.visible()
				)
		);

        ClientPlayNetworking.registerGlobalReceiver(LaunchSiteDebugPayload.PAYLOAD_ID, (payload, context) ->
                context.client().execute(() ->
                LaunchSiteDebugState.update(
                        payload.totalCount(),
                        payload.attempts(),
                        payload.chunkLoads(),
                        payload.chunkLoadsWindow(),
                        payload.evalCandidates(),
                        payload.evalCacheHits(),
                        payload.heightCallsPerSecond(),
                                payload.lastAttemptMs(),
                                payload.avgAttemptMs(),
                                payload.processTotalSeconds(),
                                payload.gameTotalSeconds(),
                                payload.heightSeconds(),
                                payload.heightCenterSeconds(),
                                payload.heightCornerSeconds(),
                                payload.peakSeconds(),
                                payload.edgeSeconds(),
                                payload.forwardSeconds(),
                                payload.flatSeconds(),
                                payload.fluidSeconds(),
                                payload.placeSeconds(),
                                payload.cacheSeconds(),
                                payload.nearSeconds(),
                                payload.chanceSeconds(),
                                payload.failChance(),
                                payload.failNearPlaced(),
                                payload.failNearInflight(),
                                payload.failMinHeight(),
                                payload.failHeightCenter(),
                                payload.failHeightCorners(),
                                payload.failDominance(),
                                payload.failFlat(),
                                payload.failFluid(),
                                payload.recent(),
                                payload.hasLastAttempt(),
                                payload.lastAttemptPos(),
                                payload.lastAttemptReason(),
                                payload.lastPeakY(),
                                payload.lastEdgeDrop(),
                                payload.lastForwardDrop(),
                                payload.lastFlatMinY(),
                                payload.lastFlatMaxY()
                        )
                )
        );

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> ChunkLoadDebugState.recordChunkLoad());


		// Apply wind on tick
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (mc.player == null) { return; }
			ChunkLoadDebugState.tick();

			if (mc.isPaused()) {
				mc.getSoundManager().stopSounds(Registries.SOUND_EVENT.getId(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value()), SoundCategory.PLAYERS);
				mc.getSoundManager().stopSounds(Registries.SOUND_EVENT.getId(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()), SoundCategory.PLAYERS);
				varioCooldown = 0;
				return;
			}

            WindInterpolator.tick();

            boolean inOverworld = mc.world != null && mc.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD);
            boolean inFlight = (mc.player.isGliding() && !(mc.player.isTouchingWater() || mc.player.isSubmergedInWater()));

            if (inFlight && inOverworld) FlightUtils.applyWindToPlayer(mc.player, WindInterpolator.getWindDirection(), WindInterpolator.getWindSpeed());

            if (inOverworld) {
                FlightUtils.getUpdraftEffect(mc.player, WindInterpolator.getWindDirection(), WindInterpolator.getWindSpeed());
            } else {
                FlightUtils.updraftStrength = 0;
            }
            if (inFlight && inOverworld) FlightUtils.applyUpdraftEffect(mc.player);

            // Start Codigo nuestro
            if (mc.player != null && mc.player.getVehicle() instanceof ParagliderEntity pg) {
                boolean left = mc.options.leftKey.isPressed();
                boolean right = mc.options.rightKey.isPressed();
                boolean forward = mc.options.forwardKey.isPressed();
                boolean back = mc.options.backKey.isPressed();

                int turn = (left == right) ? 0 : (right ? 1 : -1);
                float fwd = (forward == back) ? 0f : (forward ? 1f : -1f);
                float side = (left == right) ? 0f : (right ? 1f : -1f);

                ClientPlayNetworking.send(new ParagliderControlPayload(turn, fwd, side));

                // Limitar solo la cabeza/cuerpo del jugador en cliente (sin tocar la camara).
                float relativeHead = net.minecraft.util.math.MathHelper.wrapDegrees(mc.player.getHeadYaw() - pg.getYaw());
                if (relativeHead < -100f || relativeHead > 100f) {
                    float clamped = pg.getYaw() + net.minecraft.util.math.MathHelper.clamp(relativeHead, -100f, 100f);
                    mc.player.setHeadYaw(clamped);
                    mc.player.setBodyYaw(clamped);
                }

                // Bloquear desmontar en vuelo para supervivencia; permitir en suelo o creativo
                if (pendingDismountWarn && dismountWarnCooldown == 0) {
                    mc.player.sendMessage(Text.translatable("message.paraglidingsimulator.dismount_ground_or_creative"), true);
                    dismountWarnCooldown = 40; // 2s a 20 tps
                }
                pendingDismountWarn = false;

                // Pitido de vario basado en velocidad vertical del paraglider (m/s) y solo si llevas el vario en el inventario
                boolean hasVarioItem = mc.player.getInventory().contains(new net.minecraft.item.ItemStack(ModItems.VARIO_ITEM));
                boolean hasVarioHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.VARIO_HELMET);
                boolean hasVario = hasVarioItem || hasVarioHelmet;
                double verticalSpeed = pg.getVelocity().y * 20.0;
                double descentThreshold = hasVarioHelmet ? SkydivingClientConfig.helmetDescentThreshold : SkydivingClientConfig.varioDescentThreshold;
                double strongDescentThreshold = hasVarioHelmet ? SkydivingClientConfig.helmetStrongDescentThreshold : SkydivingClientConfig.varioStrongDescentThreshold;
                float varioVolume = hasVarioHelmet ? SkydivingClientConfig.helmetVolume : SkydivingClientConfig.varioVolume;
                if (hasVario && verticalSpeed <= strongDescentThreshold) { // descenso fuerte
                    if (varioCooldown <= 0) {
                        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), varioVolume, 0.5f);
                        varioCooldown = 8; // pitido cada ~0.4s
                    }
                } else if (hasVario && verticalSpeed < descentThreshold) { // descenso pronunciado
                    if (varioCooldown <= 0) {
                        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), varioVolume, 0.65f);
                        varioCooldown = 8; // pitido cada ~0.4s
                    }
                } else if (hasVario && verticalSpeed > 0.1) { // ascenso
                    double climb = Math.min(verticalSpeed, 5.0);
                    float pitch = (float)(1.2 + climb * 0.15); // mÃ¡s agudo con mÃ¡s subida
                    int interval = (int)Math.max(2, 8 - climb * 1.2); // mÃ¡s rÃ¡pido al subir mÃ¡s
                    if (varioCooldown <= 0) {
                        mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), varioVolume, pitch);
                        varioCooldown = interval;
                    }
                } else {
                    varioCooldown = 0;
                }

                boolean spinInput = turn != 0 && fwd > 0.01f;
                if (spinInput) {
                    spinTicks = Math.min(spinTicks + 1, ParagliderEntity.SPIN_TRIGGER_TICKS + ParagliderEntity.SPIN_RAMP_TICKS);
                } else {
                    spinTicks = 0;
                }

                if (spinTicks >= SPIN_WIND_SOUND_TICKS) {
                    if (windSoundCooldown <= 0) {
                        mc.player.playSound(SoundEvents.ITEM_ELYTRA_FLYING, 0.8f, 1.0f);
                        windSoundCooldown = 20;
                        windSoundPlaying = true;
                    }
                } else {
                    windSoundCooldown = 0;
                    if (windSoundPlaying) {
                        mc.getSoundManager().stopSounds(SoundEvents.ITEM_ELYTRA_FLYING.id(), SoundCategory.PLAYERS);
                        windSoundPlaying = false;
                    }
                }
            }

            if (!(mc.player.getVehicle() instanceof ParagliderEntity)) {
                spinTicks = 0;
                windSoundCooldown = 0;
                if (windSoundPlaying) {
                    mc.getSoundManager().stopSounds(SoundEvents.ITEM_ELYTRA_FLYING.id(), SoundCategory.PLAYERS);
                    windSoundPlaying = false;
                }
            }

            if (dismountWarnCooldown > 0) dismountWarnCooldown--;
            if (varioCooldown > 0) varioCooldown--;
            if (windSoundCooldown > 0) windSoundCooldown--;
            // End Codigo nuestro

        });

		// En START tick, anular el sneaking para evitar que el cliente se desmonte visualmente en vuelo (survival)
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (mc.player == null) return;
			if (mc.player.getVehicle() instanceof ParagliderEntity pg) {
				if (!mc.player.getAbilities().creativeMode && !pg.isGrounded()) {
					if (mc.options.sneakKey.isPressed()) {
						pendingDismountWarn = true;
					}
					mc.options.sneakKey.setPressed(false);
					mc.player.setSneaking(false);
				}
			}
		});

		// Register the Windsock Block Entity renderer
		BlockEntityRendererFactories.register(
				ModBlockEntities.WINDSOCK_BLOCK_ENTITY,
				// WindsockBlockEntityRenderer::new

				// This all needed to render cone from a far, as by default
				// it renders 64 blocks far max
				ctx -> new WindsockBlockEntityRenderer(ctx) {
					@Override
					public int getRenderDistance() {
						return 256;
					}
				}
		);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.WINDSOCK_LAYER, WindsockModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PARAGLIDER_LAYER, ParagliderModel::getTexturedModelData);
        HudRenderCallback.EVENT.register(new AirflowDebugOverlay());
        HudRenderCallback.EVENT.register(new VariometerOverlay());
        WorldRenderEvents.LAST.register(ThermalCloudRenderer::render);

        // Start Codigo nuestro
        EntityRendererRegistry.register(ModEntities.PARAGLIDER, ParagliderRenderer::new);
        // End Codigo nuestro
        HandledScreens.register(ModScreenHandlers.VARIO, VarioConfigHandledScreen::new);

    }

}
