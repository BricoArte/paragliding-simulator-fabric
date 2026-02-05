package com.tgskiv.skydiving.ui;

import com.tgskiv.skydiving.entity.ParagliderEntity;
import com.tgskiv.skydiving.flight.ParagliderForces;
import com.tgskiv.skydiving.flight.WindInterpolator;
import com.tgskiv.skydiving.menu.SkydivingClientConfig;
import com.tgskiv.skydiving.render.ThermalCloudRenderer;
import com.tgskiv.skydiving.registry.ModItems;
import com.tgskiv.skydiving.ui.ChunkLoadDebugState;
import com.tgskiv.skydiving.ui.LaunchSiteDebugState;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;

/**
 * Muestra la velocidad vertical (m/s) cuando el jugador va montado en el paraglider.
 */
public class VariometerOverlay implements HudRenderCallback {
    private static boolean wasRiding = false;
    private static long flightStartTick = 0L;
    private static final int DEPLOY_PHASE1_TICKS = 60;
    private static final int DEPLOY_PHASE2_TICKS = 30;
    private static final long CONTROL_HINT_IDLE_MS = 2000L;
    private static final float CONTROL_HINT_LERP = 0.12f;
    private static long lastControlInputMs = System.currentTimeMillis();
    private static float controlHintAlpha = 0.0f;
    private static final Identifier WIND_ARROW_TEXTURE = Identifier.of("paraglidingsimulator", "textures/gui/wind_arrow.png");
    private static final int WIND_ARROW_TEX_W = 256;
    private static final int WIND_ARROW_TEX_H = 252;
    private static final Identifier COMPASS_TEXTURE = Identifier.of("paraglidingsimulator", "textures/gui/brujula1.png");
    private static final int COMPASS_TEX_W = 256;
    private static final int COMPASS_TEX_H = 256;

    private static float getWindArrowAngleDeg(Vec3d windDir, Vec3d forwardDir) {
        double windX = windDir.x;
        double windZ = windDir.z;
        double windLen = Math.hypot(windX, windZ);
        if (windLen < 1.0e-6) {
            return 0.0f;
        }
        double fx = forwardDir.x;
        double fz = forwardDir.z;
        double fLen = Math.hypot(fx, fz);
        if (fLen < 1.0e-6) {
            return 0.0f;
        }
        windX /= windLen;
        windZ /= windLen;
        fx /= fLen;
        fz /= fLen;
        double rightX = fz;
        double rightZ = -fx;
        double forwardDot = fx * windX + fz * windZ;
        double rightDot = rightX * windX + rightZ * windZ;
        return (float)Math.toDegrees(Math.atan2(rightDot, forwardDot));
    }

    private static void drawWindArrowTexture(DrawContext ctx, int centerX, int centerY, int size, float angleDeg) {
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-angleDeg));
        float scale = size / (float)Math.max(WIND_ARROW_TEX_W, WIND_ARROW_TEX_H);
        matrices.scale(scale, scale, 1.0f);
        int x = -WIND_ARROW_TEX_W / 2;
        int y = -WIND_ARROW_TEX_H / 2;
        ctx.drawTexture(RenderLayer::getGuiTextured, WIND_ARROW_TEXTURE, x, y, 0.0f, 0.0f,
                WIND_ARROW_TEX_W, WIND_ARROW_TEX_H, WIND_ARROW_TEX_W, WIND_ARROW_TEX_H);
        matrices.pop();
    }

    private static void drawCompassTexture(DrawContext ctx, int centerX, int centerY, int size, float angleDeg) {
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(centerX, centerY, 0);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-angleDeg));
        float scale = size / (float)Math.max(COMPASS_TEX_W, COMPASS_TEX_H);
        matrices.scale(scale, scale, 1.0f);
        int x = -COMPASS_TEX_W / 2;
        int y = -COMPASS_TEX_H / 2;
        ctx.drawTexture(RenderLayer::getGuiTextured, COMPASS_TEXTURE, x, y, 0.0f, 0.0f,
                COMPASS_TEX_W, COMPASS_TEX_H, COMPASS_TEX_W, COMPASS_TEX_H);
        matrices.pop();
    }

    private static String formatFlightTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static int colorByThreshold(int value, int warn, int danger) {
        if (value <= danger) return 0xFFCC3333;
        if (value <= warn) return 0xFFCCAA33;
        return 0xFF55FF55;
    }

    private static int colorByThreshold(double value, double warn, double danger) {
        if (value >= danger) return 0xFFCC3333;
        if (value >= warn) return 0xFFCCAA33;
        return 0xFF55FF55;
    }

    private static boolean isControlInputActive(MinecraftClient mc) {
        return mc.options.forwardKey.isPressed()
                || mc.options.backKey.isPressed()
                || mc.options.leftKey.isPressed()
                || mc.options.rightKey.isPressed();
    }

    private static void drawControlHints(DrawContext ctx, TextRenderer tr, MinecraftClient mc, int startX, int startY, float alpha) {
        int keyBg = (MathHelper.clamp((int)(alpha * 180), 0, 255) << 24) | 0x202020;
        int keyFg = (MathHelper.clamp((int)(alpha * 255), 0, 255) << 24) | 0xFFFFFF;
        int textFg = (MathHelper.clamp((int)(alpha * 255), 0, 255) << 24) | 0xE6E6E6;
        int padX = 3;
        int padY = 2;
        int lineH = 14;
        int y = startY;

        String keyForward = mc.options.forwardKey.getBoundKeyLocalizedText().getString();
        String keyBack = mc.options.backKey.getBoundKeyLocalizedText().getString();
        String keyLeft = mc.options.leftKey.getBoundKeyLocalizedText().getString();
        String keyRight = mc.options.rightKey.getBoundKeyLocalizedText().getString();

        String[][] lines = new String[][]{
                {keyForward, Text.translatable("hud.paraglidingsimulator.hint_accelerate").getString()},
                {keyBack, Text.translatable("hud.paraglidingsimulator.hint_brake").getString()},
                {keyLeft, Text.translatable("hud.paraglidingsimulator.hint_left").getString()},
                {keyRight, Text.translatable("hud.paraglidingsimulator.hint_right").getString()},
                {keyForward + " + " + keyLeft + "/" + keyRight, Text.translatable("hud.paraglidingsimulator.hint_spin").getString()}
        };

        for (String[] line : lines) {
            String key = line[0];
            String label = line[1];
            int keyW = tr.getWidth(key);
            int boxW = keyW + padX * 2;
            int boxH = tr.fontHeight + padY * 2;
            ctx.fill(startX, y, startX + boxW, y + boxH, keyBg);
            ctx.drawTextWithShadow(tr, key, startX + padX, y + padY, keyFg);
            ctx.drawTextWithShadow(tr, label, startX + boxW + 6, y + padY, textFg);
            y += lineH;
        }
    }

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        drawFoldDeployBar(drawContext, mc);
        boolean isRiding = mc.player.getVehicle() instanceof ParagliderEntity;
        ParagliderEntity paraglider = isRiding ? (ParagliderEntity) mc.player.getVehicle() : null;
        if (isRiding && !wasRiding && mc.world != null) {
            flightStartTick = mc.world.getTime();
        }
        wasRiding = isRiding;
        // Tomar la velocidad del paraglider si va montado; si no, la del propio jugador
        Vec3d baseVel = isRiding ? ((ParagliderEntity) mc.player.getVehicle()).getVelocity() : mc.player.getVelocity();

        TextRenderer tr = mc.textRenderer;
        boolean collapsed = paraglider != null && paraglider.isGrounded() && baseVel.lengthSquared() < 0.0001f;
        long nowMs = System.currentTimeMillis();
        if (collapsed && isControlInputActive(mc)) {
            lastControlInputMs = nowMs;
        } else if (!collapsed) {
            lastControlInputMs = nowMs;
        }
        boolean showHints = collapsed && (nowMs - lastControlInputMs) >= CONTROL_HINT_IDLE_MS;
        float targetAlpha = showHints ? 1.0f : 0.0f;
        controlHintAlpha = MathHelper.lerp(CONTROL_HINT_LERP, controlHintAlpha, targetAlpha);

        if (controlHintAlpha > 0.01f) {
            drawControlHints(drawContext, tr, mc, 10, 10, controlHintAlpha);
        }

        boolean hasHelmet = mc.player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.VARIO_HELMET);
        if (!hasHelmet) return;
        boolean allowHud = SkydivingClientConfig.hudShowWind
                || SkydivingClientConfig.hudShowHeading
                || SkydivingClientConfig.hudShowAltitude
                || SkydivingClientConfig.hudShowSpeed
                || SkydivingClientConfig.hudShowVario
                || SkydivingClientConfig.hudShowThermal
                || SkydivingClientConfig.hudShowThermalDebug;
        if (!allowHud) return;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float hudScale = (float)Math.max(0.5, Math.min(1.5, SkydivingClientConfig.hudScale));
        int lineHeight = 12;

        // Velocidad vertical (m/s) y horizontal (km/h)
        double verticalSpeed = baseVel.y * 20.0; // bloques/tick -> m/s
        // Si no vas en parapente y estÃ¡s en el suelo, el cliente suele tener un delta Y negativo por gravedad -> mostrar 0
        if (!(mc.player.getVehicle() instanceof ParagliderEntity) && mc.player.isOnGround()) {
            verticalSpeed = 0.0;
        }
        double horizontalSpeed = new Vec3d(baseVel.x, 0, baseVel.z).length() * 20.0 * 3.6; // km/h

        String varioText = Text.translatable("hud.paraglidingsimulator.vario",
                String.format("%.2f", verticalSpeed)).getString();
        // Mostrar velocidad inflada para HUD: real ~12 km/h -> mostrado ~40 km/h => factor ~3.33
        String speedText = Text.translatable("hud.paraglidingsimulator.speed",
                String.format("%.1f", horizontalSpeed * 3.33)).getString();

        // Viento
        boolean inOverworld = mc.world != null && mc.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD);
        Vec3d windDir = inOverworld ? WindInterpolator.getWindDirection() : Vec3d.ZERO;
        double windSpeedKmh = 0.0;
        if (inOverworld) {
            double windSpeed = WindInterpolator.getWindSpeed();
            double minWind = SkydivingClientConfig.minWindSpeed;
            double maxWind = SkydivingClientConfig.maxWindSpeed;
            double range = Math.max(1.0e-6, maxWind - minWind);
            double t = MathHelper.clamp((windSpeed - minWind) / range, 0.0, 1.0);
            double displayScale = 1.0 - (0.5 * t);
            windSpeedKmh = windSpeed * displayScale * 200.0 * 3.6;
        }
        String windText = Text.translatable("hud.paraglidingsimulator.wind_value",
                String.format("%.1f", windSpeedKmh)).getString();

        // Rumbo: usar la mirada del jugador (casco) incluso si va en paraglider
        float yaw = mc.player.getYaw();
        double yawRad = Math.toRadians(yaw);
        double hx = -Math.sin(yawRad);
        double hz = Math.cos(yawRad);
        String headingText = "";

        // Altitud
        double altitude = mc.player.getY();
        String altText = Text.translatable("hud.paraglidingsimulator.altitude",
                String.format("%.2f", altitude)).getString();
        double thermalUpdraft = ParagliderForces.getThermalUpdraftForEntity(mc.player) * 20.0;
        String thermalText = Text.translatable("hud.paraglidingsimulator.thermal",
                String.format("%.3f", thermalUpdraft)).getString();
        String flightTimeText = Text.translatable("hud.paraglidingsimulator.flight_time", "0:00").getString();
        if (mc.world != null && isRiding) {
            long elapsedSeconds = (mc.world.getTime() - flightStartTick) / 20;
            flightTimeText = Text.translatable("hud.paraglidingsimulator.flight_time",
                    formatFlightTime(elapsedSeconds)).getString();
        }
        long dayTime = mc.world == null ? 0 : (mc.world.getTimeOfDay() % 24000);
        boolean spawnTime = ParagliderForces.isThermalSpawnTime(dayTime);
        String thermalDebug = null;
        String thermalDebug2 = null;
        String thermalDebug3 = null;
        String thermalDebug4 = null;
        String thermalDebug5 = null;
        String thermalDebug6 = null;
        String thermalDebug7 = null;
        String thermalDebug8 = null;
        String thermalDebug9 = null;
        String launchDebug1 = null;
        String launchDebug2 = null;
        String launchDebug3 = null;
        String launchDebug4 = null;
        String launchDebug5 = null;
        String launchDebug6 = null;
        String launchDebug6b = null;
        String launchDebug7 = null;
        String launchDebug8 = null;
        String launchDebug9 = null;
        String launchDebug10 = null;
        String launchDebug11 = null;
        String launchDebug12 = null;
        String launchDebug13 = null;
        String launchDebug14 = null;
        String launchDebug15 = null;
        if (SkydivingClientConfig.hudShowThermalDebug) {
            int activeThermals = ParagliderForces.getActiveThermalCount();
            int nonLavaThermals = ParagliderForces.getActiveNonLavaThermalCount();
            int lavaThermals = Math.max(0, activeThermals - nonLavaThermals);
            String spawnState = Text.translatable(spawnTime ? "gui.paraglidingsimulator.on" : "gui.paraglidingsimulator.off").getString();
            thermalDebug = Text.translatable("hud.paraglidingsimulator.thermals_status",
                    spawnState,
                    dayTime,
                    activeThermals,
                    lavaThermals).getString();
            thermalDebug2 = Text.translatable("hud.paraglidingsimulator.thermals_candidates",
                    ParagliderForces.getLastSpawnCandidatesNormal(),
                    ParagliderForces.getLastSpawnCandidatesLava(),
                    ParagliderForces.getLastSpawnAdded()).getString();
            thermalDebug3 = Text.translatable("hud.paraglidingsimulator.thermals_scan",
                    String.format("%.2f", ParagliderForces.getLastThermalTickNanos() / 1_000_000.0),
                    ParagliderForces.getLastLavaScans()).getString();
            thermalDebug4 = Text.translatable("hud.paraglidingsimulator.thermals_render",
                    ThermalCloudRenderer.getLastRenderCount(),
                    String.format("%.2f", ThermalCloudRenderer.getLastRenderNanos() / 1_000_000.0)).getString();
            thermalDebug5 = Text.translatable("hud.paraglidingsimulator.thermals_factors",
                    String.format("%.2f", ParagliderForces.getDailyThermalHeightFactor()),
                    String.format("%.2f", ParagliderForces.getDailyThermalSizeFactor())).getString();
            thermalDebug6 = Text.translatable("hud.paraglidingsimulator.thermals_override",
                    Text.translatable(ParagliderForces.isDailyThermalOverrideActive()
                            ? "gui.paraglidingsimulator.on"
                            : "gui.paraglidingsimulator.off").getString()).getString();

            if (mc.player != null && mc.player.getVehicle() instanceof com.tgskiv.skydiving.entity.ParagliderEntity dbgParaglider) {
                float roll = com.tgskiv.skydiving.render.ParagliderRenderer.getRollTargetDegrees(dbgParaglider);
                thermalDebug7 = Text.translatable("hud.paraglidingsimulator.roll_angle",
                        String.format("%.1f", roll)).getString();
                float rollReal = com.tgskiv.skydiving.render.ParagliderRenderer.getLastRenderedRoll();
                float pitchReal = com.tgskiv.skydiving.render.ParagliderRenderer.getLastRenderedPitch();
                thermalDebug8 = Text.translatable("hud.paraglidingsimulator.attitude",
                        String.format("%.1f", rollReal),
                        String.format("%.1f", pitchReal)).getString();
                long msSince = paraglider.getMsSinceLastTrackedUpdate();
                if (msSince >= 0) {
                    thermalDebug9 = Text.translatable("hud.paraglidingsimulator.net_update_delay",
                            Long.toString(msSince),
                            Integer.toString(paraglider.getLastInterpolationSteps())).getString();
                }
            }

            int launchCount = LaunchSiteDebugState.getTotalCount();
            launchDebug1 = Text.translatable("hud.paraglidingsimulator.launch_sites_count", launchCount).getString();
            launchDebug3 = Text.translatable("hud.paraglidingsimulator.launch_sites_fail",
                    LaunchSiteDebugState.getAttempts(),
                    LaunchSiteDebugState.getChunkLoads(),
                    LaunchSiteDebugState.getFailNearPlaced(),
                    LaunchSiteDebugState.getFailNearInflight(),
                    LaunchSiteDebugState.getFailChance(),
                    LaunchSiteDebugState.getFailMinHeight(),
                    LaunchSiteDebugState.getFailDominance(),
                    LaunchSiteDebugState.getFailFlat(),
                    LaunchSiteDebugState.getFailFluid()).getString();
            int launchChunks = LaunchSiteDebugState.getChunkLoads();
            if (launchChunks > 0) {
                double evalPct = (LaunchSiteDebugState.getAttempts() * 100.0) / launchChunks;
                launchDebug4 = Text.translatable("hud.paraglidingsimulator.launch_sites_eval_pct",
                        String.format("%.1f", evalPct)).getString();
            }
            double totalSeconds = LaunchSiteDebugState.getProcessTotalSeconds();
            double gameSeconds = LaunchSiteDebugState.getGameTotalSeconds();
            if (totalSeconds > 0.0 && gameSeconds > 0.0) {
                double gamePct = (totalSeconds * 100.0) / gameSeconds;
                launchDebug5 = Text.translatable("hud.paraglidingsimulator.launch_sites_total_time",
                        String.format("%.2f", totalSeconds),
                        String.format("%.2f", gamePct)).getString();
            } else if (totalSeconds > 0.0) {
                launchDebug5 = Text.translatable("hud.paraglidingsimulator.launch_sites_total_time",
                        String.format("%.2f", totalSeconds),
                        "0.00").getString();
            }
            if (totalSeconds > 0.0) {
                double heightPct = (LaunchSiteDebugState.getHeightSeconds() * 100.0) / totalSeconds;
                double heightCenterPct = (LaunchSiteDebugState.getHeightCenterSeconds() * 100.0) / totalSeconds;
                double heightCornerPct = (LaunchSiteDebugState.getHeightCornerSeconds() * 100.0) / totalSeconds;
                double peakPct = (LaunchSiteDebugState.getPeakSeconds() * 100.0) / totalSeconds;
                double edgePct = (LaunchSiteDebugState.getEdgeSeconds() * 100.0) / totalSeconds;
                double forwardPct = (LaunchSiteDebugState.getForwardSeconds() * 100.0) / totalSeconds;
                double flatPct = (LaunchSiteDebugState.getFlatSeconds() * 100.0) / totalSeconds;
                double fluidPct = (LaunchSiteDebugState.getFluidSeconds() * 100.0) / totalSeconds;
                double placePct = (LaunchSiteDebugState.getPlaceSeconds() * 100.0) / totalSeconds;
                double cachePct = (LaunchSiteDebugState.getCacheSeconds() * 100.0) / totalSeconds;
                double nearPct = (LaunchSiteDebugState.getNearSeconds() * 100.0) / totalSeconds;
                double chancePct = (LaunchSiteDebugState.getChanceSeconds() * 100.0) / totalSeconds;
                launchDebug6 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_height",
                        String.format("%.2f", LaunchSiteDebugState.getHeightSeconds()),
                        String.format("%.1f", heightPct)).getString();
                launchDebug6b = Text.translatable("hud.paraglidingsimulator.launch_sites_time_height_split",
                        String.format("%.2f", LaunchSiteDebugState.getHeightCenterSeconds()),
                        LaunchSiteDebugState.getFailHeightCenter(),
                        String.format("%.1f", heightCenterPct),
                        String.format("%.2f", LaunchSiteDebugState.getHeightCornerSeconds()),
                        LaunchSiteDebugState.getFailHeightCorners(),
                        String.format("%.1f", heightCornerPct)).getString();
                launchDebug13 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_peak",
                        String.format("%.2f", LaunchSiteDebugState.getPeakSeconds()),
                        String.format("%.1f", peakPct)).getString();
                launchDebug7 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_edge",
                        String.format("%.2f", LaunchSiteDebugState.getEdgeSeconds()),
                        String.format("%.1f", edgePct)).getString();
                launchDebug8 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_forward",
                        String.format("%.2f", LaunchSiteDebugState.getForwardSeconds()),
                        String.format("%.1f", forwardPct)).getString();
                launchDebug9 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_flat",
                        String.format("%.2f", LaunchSiteDebugState.getFlatSeconds()),
                        String.format("%.1f", flatPct)).getString();
                launchDebug10 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_fluid",
                        String.format("%.2f", LaunchSiteDebugState.getFluidSeconds()),
                        String.format("%.1f", fluidPct)).getString();
                launchDebug11 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_place",
                        String.format("%.2f", LaunchSiteDebugState.getPlaceSeconds()),
                        String.format("%.1f", placePct)).getString();
                launchDebug12 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_cache",
                        String.format("%.2f", LaunchSiteDebugState.getCacheSeconds()),
                        String.format("%.1f", cachePct)).getString();
                launchDebug14 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_near",
                        String.format("%.2f", LaunchSiteDebugState.getNearSeconds()),
                        String.format("%.1f", nearPct)).getString();
                launchDebug15 = Text.translatable("hud.paraglidingsimulator.launch_sites_time_chance",
                        String.format("%.2f", LaunchSiteDebugState.getChanceSeconds()),
                        String.format("%.1f", chancePct)).getString();
            }
        }

        // Color: verde si sube, rojo si baja, blanco si ~0
        int color = 0xFFFFFF;
        if (verticalSpeed > 0.01) color = 0x55FF55;
        else if (verticalSpeed < -0.01) color = 0xFF5555;

        // Calcular ancho mÃ¡ximo para centrar
        int windArrowSize = 32;
        int compassSize = 32;
        int leftBlockWidth = 0;
        int rightBlockWidth = 0;
        int windArrowGap = 0;
        int windArrowOffset = 0;

        String windLabel = Text.translatable("hud.paraglidingsimulator.wind_label").getString();
        if (SkydivingClientConfig.hudShowWind) {
            int windLabelWidth = tr.getWidth(windLabel);
            leftBlockWidth = Math.max(leftBlockWidth, windLabelWidth);
            leftBlockWidth = Math.max(leftBlockWidth, tr.getWidth(windText));
            windArrowGap = windArrowSize + 10;
            windArrowOffset = windArrowSize / 2 + 10;
        }

        if (SkydivingClientConfig.hudShowAltitude) {
            rightBlockWidth = Math.max(rightBlockWidth, tr.getWidth(altText));
        }
        if (SkydivingClientConfig.hudShowSpeed) {
            rightBlockWidth = Math.max(rightBlockWidth, tr.getWidth(speedText));
        }
        if (SkydivingClientConfig.hudShowVario) {
            rightBlockWidth = Math.max(rightBlockWidth, tr.getWidth(varioText));
        }
        if (SkydivingClientConfig.hudShowThermal) {
            rightBlockWidth = Math.max(rightBlockWidth, tr.getWidth(flightTimeText));
        }

        int leftCount = 0;
        if (SkydivingClientConfig.hudShowWind) leftCount += 2; // label + value
        int rightCount = 0;
        if (SkydivingClientConfig.hudShowAltitude) rightCount++;
        if (SkydivingClientConfig.hudShowSpeed) rightCount++;
        if (SkydivingClientConfig.hudShowVario) rightCount++;
        if (SkydivingClientConfig.hudShowThermal) rightCount++;
        int leftBlockHeight = leftCount * lineHeight + windArrowGap;
        int rightBlockHeight = rightCount * lineHeight;
        int totalHeight = Math.max(leftBlockHeight, rightBlockHeight);
        float yStart = (screenH - totalHeight * hudScale) / 2f;

        int lime = 0xFF77FF44;

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(hudScale, hudScale, 1f);

        float leftX = 20f;
        float rightX = (screenW / hudScale) - rightBlockWidth - 20f;

        int yLeft = (int)(yStart / hudScale);
        int yRight = (int)(yStart / hudScale);

        if (SkydivingClientConfig.hudShowHeading) {
            int compassX = (int)((screenW / hudScale) / 2f);
            int compassY = 16 + compassSize / 2;
            float compassAngle = yaw;
            drawCompassTexture(drawContext, compassX, compassY, compassSize, compassAngle);
        }

        if (SkydivingClientConfig.hudShowWind) {
            drawContext.drawTextWithShadow(tr, windLabel, (int)leftX, yLeft, lime);
            yLeft += lineHeight;
            drawContext.drawTextWithShadow(tr, windText, (int)leftX, yLeft, lime);
            Vec3d forwardDir = new Vec3d(hx, 0, hz);
            float arrowAngle = getWindArrowAngleDeg(windDir, forwardDir);
            int arrowX = (int)leftX + tr.getWidth(windText) / 2;
            int arrowY = yLeft + windArrowOffset;
            drawWindArrowTexture(drawContext, arrowX, arrowY, windArrowSize, arrowAngle);
            float norm = MathHelper.wrapDegrees(arrowAngle);
            String windState = null;
            if (Math.abs(norm) <= 30.0f) {
                windState = Text.translatable("hud.paraglidingsimulator.wind_tail").getString();
            } else if (Math.abs(Math.abs(norm) - 180.0f) <= 30.0f) {
                windState = Text.translatable("hud.paraglidingsimulator.wind_head").getString();
            }
            if (windState != null) {
                int wx = arrowX - tr.getWidth(windState) / 2;
                int wy = arrowY + (windArrowSize / 2) + 2;
                drawContext.drawTextWithShadow(tr, windState, wx, wy, lime);
            }
            yLeft += windArrowGap;
        }

        if (SkydivingClientConfig.hudShowAltitude) {
            drawContext.drawTextWithShadow(tr, altText, (int)rightX, yRight, lime);
            yRight += lineHeight;
        }
        if (SkydivingClientConfig.hudShowVario) {
            drawContext.drawTextWithShadow(tr, varioText, (int)rightX, yRight, color);
            yRight += lineHeight;
        }
        if (SkydivingClientConfig.hudShowSpeed) {
            drawContext.drawTextWithShadow(tr, speedText, (int)rightX, yRight, lime);
            yRight += lineHeight;
        }
        if (SkydivingClientConfig.hudShowThermal) {
            drawContext.drawTextWithShadow(tr, flightTimeText, (int)rightX, yRight, lime);
            yRight += lineHeight;
        }
        if (SkydivingClientConfig.hudShowThermalDebug) {
            drawContext.drawTextWithShadow(tr, thermalText, (int)rightX, yRight, 0xFF4444); yRight += lineHeight;
        }

        matrices.pop();

        if (SkydivingClientConfig.hudShowThermalDebug) {
            int debugX = 10;
            int debugY = 10;
            int debugLine = 10;
            drawContext.drawTextWithShadow(tr, thermalDebug, debugX, debugY, 0xFFCC00); debugY += debugLine;
            drawContext.drawTextWithShadow(tr, thermalDebug2, debugX, debugY, 0xFFCC00); debugY += debugLine;
            drawContext.drawTextWithShadow(tr, thermalDebug3, debugX, debugY, 0xFFCC00); debugY += debugLine;
            drawContext.drawTextWithShadow(tr, thermalDebug4, debugX, debugY, 0xFFCC00);
            debugY += debugLine;
            drawContext.drawTextWithShadow(tr, thermalDebug5, debugX, debugY, 0xFFCC00); debugY += debugLine;
            drawContext.drawTextWithShadow(tr, thermalDebug6, debugX, debugY, 0xFFCC00);
            debugY += debugLine;
            if (thermalDebug7 != null) {
                drawContext.drawTextWithShadow(tr, thermalDebug7, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (thermalDebug8 != null) {
                drawContext.drawTextWithShadow(tr, thermalDebug8, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (thermalDebug9 != null) {
                drawContext.drawTextWithShadow(tr, thermalDebug9, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            debugY += 2;
            drawContext.drawTextWithShadow(tr, launchDebug1, debugX, debugY, 0xFFCC00); debugY += debugLine;
            drawContext.drawTextWithShadow(tr, launchDebug3, debugX, debugY, 0xFFCC00);
            debugY += debugLine;
            drawContext.drawTextWithShadow(tr, launchDebug4, debugX, debugY, 0xFFCC00);
            debugY += debugLine;
            if (launchDebug5 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug5, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug6 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug6, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug6b != null) {
                drawContext.drawTextWithShadow(tr, launchDebug6b, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug13 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug13, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug7 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug7, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug8 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug8, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug9 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug9, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug10 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug10, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug11 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug11, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug12 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug12, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug14 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug14, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            if (launchDebug15 != null) {
                drawContext.drawTextWithShadow(tr, launchDebug15, debugX, debugY, 0xFFCC00);
                debugY += debugLine;
            }
            debugY += 2;

            int fps = mc.getCurrentFps();
            int fpsColor = colorByThreshold(fps, 60, 45);
            drawContext.drawTextWithShadow(tr,
                    Text.translatable("hud.paraglidingsimulator.fps", fps).getString(),
                    debugX, debugY, fpsColor);
            debugY += debugLine;

            double scanMs = ParagliderForces.getLastThermalTickNanos() / 1_000_000.0;
            int scanColor = colorByThreshold(scanMs, 10.0, 20.0);
            drawContext.drawTextWithShadow(tr,
                    Text.translatable("hud.paraglidingsimulator.scan_ms", String.format("%.2f", scanMs)).getString(),
                    debugX, debugY, scanColor);
            debugY += debugLine;

            double renderMs = ThermalCloudRenderer.getLastRenderNanos() / 1_000_000.0;
            int renderColor = colorByThreshold(renderMs, 2.0, 5.0);
            drawContext.drawTextWithShadow(tr,
                    Text.translatable("hud.paraglidingsimulator.render_ms", String.format("%.2f", renderMs)).getString(),
                    debugX, debugY, renderColor);
            debugY += debugLine;

            int chunkSamples = ChunkLoadDebugState.getSampleCount();
            String chunkAvgText = chunkSamples > 0 ? String.format("%.1f", ChunkLoadDebugState.getAvgMs()) : "-";
            String chunkLastText = chunkSamples > 0 ? String.format("%.1f", ChunkLoadDebugState.getLastMs()) : "-";
            String chunkMaxText = chunkSamples > 0 ? String.format("%.1f", ChunkLoadDebugState.getMaxMs()) : "-";
            int chunkColor = chunkSamples > 0 ? colorByThreshold(ChunkLoadDebugState.getAvgMs(), 40.0, 80.0) : 0xFFCCCCCC;
            drawContext.drawTextWithShadow(tr,
                    Text.translatable("hud.paraglidingsimulator.chunk_ms", chunkAvgText, chunkLastText, chunkMaxText, chunkSamples).getString(),
                    debugX, debugY, chunkColor);
            debugY += debugLine;

            int pingMs = -1;
            if (mc.getNetworkHandler() != null && mc.player != null) {
                var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (entry != null) pingMs = entry.getLatency();
            }
            if (pingMs >= 0) {
                int pingColor = colorByThreshold(pingMs, 100, 200);
                drawContext.drawTextWithShadow(tr,
                        Text.translatable("hud.paraglidingsimulator.ping", pingMs).getString(),
                        debugX, debugY, pingColor);
            }
        }

        // debug arrow removed; keep only next to wind line
    }

    private static String launchReasonKey(int code) {
        return switch (code) {
            case 1 -> "message.paraglidingsimulator.launch_fail_spacing";
            case 2 -> "message.paraglidingsimulator.launch_fail_height";
            case 3 -> "message.paraglidingsimulator.launch_fail_edge";
            case 4 -> "message.paraglidingsimulator.launch_fail_forward";
            case 5 -> "message.paraglidingsimulator.launch_fail_flat";
            case 6 -> "message.paraglidingsimulator.launch_fail_fluid";
            default -> "message.paraglidingsimulator.launch_fail_unknown";
        };
    }

    private static void drawFoldDeployBar(DrawContext drawContext, MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;

        boolean deployActive = mc.player.isUsingItem()
                && mc.player.getActiveItem().isOf(ModItems.PARAGLIDER_ITEM);
        float deployProgress = 0.0f;
        String deployLabel = Text.translatable("hud.paraglidingsimulator.deploying").getString();
        if (deployActive) {
            int maxUse = mc.player.getActiveItem().getItem().getMaxUseTime(mc.player.getActiveItem(), mc.player);
            int remaining = mc.player.getItemUseTimeLeft();
            if (maxUse > 0) {
                int elapsed = maxUse - remaining;
                if (elapsed < DEPLOY_PHASE1_TICKS) {
                    deployLabel = Text.translatable("hud.paraglidingsimulator.deploying").getString();
                    deployProgress = Math.min(1.0f, Math.max(0.0f, elapsed / (float)DEPLOY_PHASE1_TICKS));
                } else {
                    deployLabel = Text.translatable("hud.paraglidingsimulator.verifying").getString();
                    int phaseElapsed = elapsed - DEPLOY_PHASE1_TICKS;
                    deployProgress = Math.min(1.0f, Math.max(0.0f, phaseElapsed / (float)DEPLOY_PHASE2_TICKS));
                }
            }
        }

        boolean foldActive = false;
        float foldProgress = 0.0f;
        if (!deployActive && mc.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr) {
            if (ehr.getEntity() instanceof ParagliderEntity target) {
                int hits = target.getFoldHitCount();
                int age = target.getFoldHitAge();
                if (hits > 0 && age <= ParagliderEntity.getFoldHitTimeoutTicks()) {
                    foldActive = true;
                    foldProgress = Math.min(1.0f, Math.max(0.0f, hits / (float)ParagliderEntity.getFoldHitRequired()));
                }
            }
        }

        if (!deployActive && !foldActive) return;

        String label = deployActive ? deployLabel : Text.translatable("hud.paraglidingsimulator.folding").getString();
        float progress = deployActive ? deployProgress : foldProgress;

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        float hudScale = (float)Math.max(0.5, Math.min(1.5, SkydivingClientConfig.hudScale));
        int barWidth = 140;
        int barHeight = 10;
        int x = (int)((screenW / hudScale - barWidth) / 2);
        int y = (int)((screenH / hudScale) * 0.8f);

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(hudScale, hudScale, 1f);

        int bg = 0xAA000000;
        int fill = 0xFF55FF55;
        int outline = 0xFFFFFFFF;
        drawContext.fill(x, y, x + barWidth, y + barHeight, bg);
        int filled = (int)(barWidth * progress);
        if (filled > 0) {
            drawContext.fill(x, y, x + filled, y + barHeight, fill);
        }
        drawContext.fill(x, y, x + barWidth, y + 1, outline);
        drawContext.fill(x, y + barHeight - 1, x + barWidth, y + barHeight, outline);
        drawContext.fill(x, y, x + 1, y + barHeight, outline);
        drawContext.fill(x + barWidth - 1, y, x + barWidth, y + barHeight, outline);

        int labelWidth = mc.textRenderer.getWidth(label);
        drawContext.drawTextWithShadow(mc.textRenderer, label, x + (barWidth - labelWidth) / 2, y - 12, 0xFFFFFF);

        matrices.pop();
    }
}
