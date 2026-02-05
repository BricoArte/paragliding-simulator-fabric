package com.tgskiv.skydiving.render;

import com.tgskiv.skydiving.flight.ParagliderForces;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class ThermalCloudRenderer {

    private static final Identifier CLOUD_TEXTURE = Identifier.of("minecraft", "textures/block/snow.png");
    private static final float CLOUD_ALPHA = 0.8f;
    private static final float CLOUD_TINT_R = 1.0f;
    private static final float CLOUD_TINT_G = 0.98f;
    private static final float CLOUD_TINT_B = 0.95f;
    private static final double CLOUD_RADIUS = 7.0;
    private static final float CLOUD_CUBE_SIZE = 7.0f;
    private static final double CLOUD_TALL_THRESHOLD = 1.675;
    private static final double CLOUD_LOW_THRESHOLD = 0.96;
    private static int lastRenderCount = 0;
    private static long lastRenderNanos = 0;

    private ThermalCloudRenderer() {}

    public static void render(WorldRenderContext context) {
        if (context.world() == null) return;

        Vec3d camPos = context.camera().getPos();
        double cy;
        VertexConsumerProvider consumers = context.consumers();
        long worldTime = context.world().getTime();
        long start = System.nanoTime();
        int count = 0;

        for (ParagliderForces.ThermalRenderData thermal : ParagliderForces.getThermalsForRender(worldTime)) {
            double cx = thermal.centerX();
            double cz = thermal.centerZ();
            cy = thermal.cloudY();
            Vec3d camRel = new Vec3d(camPos.x - cx, camPos.y - cy, camPos.z - cz);

            MatrixStack matrices = context.matrixStack();
            matrices.push();
            matrices.translate(cx - camPos.x, cy - camPos.y, cz - camPos.z);
            renderCloudCubes(matrices, consumers, camRel, thermal.strengthFactor(), thermal.sizeFactor());
            matrices.pop();
            count++;
        }
        lastRenderCount = count;
        lastRenderNanos = System.nanoTime() - start;
    }

    public static int getLastRenderCount() {
        return lastRenderCount;
    }

    public static long getLastRenderNanos() {
        return lastRenderNanos;
    }

    private static void renderCloudCubes(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camRel, double factor, double sizeFactor) {
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getEntityTranslucent(CLOUD_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        float r = CLOUD_TINT_R;
        float g = CLOUD_TINT_G;
        float b = CLOUD_TINT_B;
        float a = (float) (CLOUD_ALPHA * factor);
        if (a <= 0.01f) return;

        float half = CLOUD_CUBE_SIZE * 0.5f;
        float eased = (float) (factor * factor);
        float scale = (float) ((0.2f + (0.8f * eased)) * sizeFactor);

        renderBox(vc, entry, camRel, 0.0f, 0.0f, 0.0f, half * 1.5f * scale, half * 0.25f * scale, half * 1.2f * scale, r, g, b, a);
        renderBox(vc, entry, camRel, 0.0f, half * 0.60f * scale, 0.0f, half * 1.0f * scale, half * 0.25f * scale, half * 0.85f * scale, r, g, b, a);
        if (sizeFactor > CLOUD_LOW_THRESHOLD) {
            renderBox(vc, entry, camRel, 0.0f, half * 1.20f * scale, 0.0f, half * 0.6f * scale, half * 0.25f * scale, half * 0.55f * scale, r, g, b, a);
            if (sizeFactor >= CLOUD_TALL_THRESHOLD) {
                renderBox(vc, entry, camRel, 0.0f, half * 1.80f * scale, 0.0f, half * 0.4f * scale, half * 0.20f * scale, half * 0.4f * scale, r, g, b, a);
            }
        }
    }

    private static void renderBox(VertexConsumer vc, MatrixStack.Entry entry, Vec3d camRel,
                                  float ox, float oy, float oz,
                                  float halfX, float halfY, float halfZ,
                                  float r, float g, float b, float a) {
        float x0 = ox - halfX;
        float x1 = ox + halfX;
        float y0 = oy - halfY;
        float y1 = oy + halfY;
        float z0 = oz - halfZ;
        float z1 = oz + halfZ;

        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 1.0f;
        float v1 = 1.0f;

        addQuad(vc, entry, camRel, ox, y1, oz, 0.0f, 1.0f, 0.0f,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a, u0, v0, u1, v1);
        addQuad(vc, entry, camRel, ox, y0, oz, 0.0f, -1.0f, 0.0f,
                x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, r, g, b, a, u0, v0, u1, v1);

        addQuad(vc, entry, camRel, ox, oy, z0, 0.0f, 0.0f, -1.0f,
                x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a, u0, v0, u1, v1);
        addQuad(vc, entry, camRel, ox, oy, z1, 0.0f, 0.0f, 1.0f,
                x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, r, g, b, a, u0, v0, u1, v1);

        addQuad(vc, entry, camRel, x0, oy, oz, -1.0f, 0.0f, 0.0f,
                x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, r, g, b, a, u0, v0, u1, v1);
        addQuad(vc, entry, camRel, x1, oy, oz, 1.0f, 0.0f, 0.0f,
                x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a, u0, v0, u1, v1);
    }

    private static void addQuad(VertexConsumer vc, MatrixStack.Entry entry, Vec3d camRel,
                                float cx, float cy, float cz,
                                float nx, float ny, float nz,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float r, float g, float b, float a,
                                float u0, float v0, float u1, float v1) {
        double vx = camRel.x - cx;
        double vy = camRel.y - cy;
        double vz = camRel.z - cz;
        double dot = (vx * nx) + (vy * ny) + (vz * nz);
        if (dot <= 0) return;
        vc.vertex(entry.getPositionMatrix(), x0, y0, z0).color(r, g, b, a).texture(u0, v0)
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, nx, ny, nz);
        vc.vertex(entry.getPositionMatrix(), x1, y1, z1).color(r, g, b, a).texture(u1, v0)
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, nx, ny, nz);
        vc.vertex(entry.getPositionMatrix(), x2, y2, z2).color(r, g, b, a).texture(u1, v1)
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, nx, ny, nz);
        vc.vertex(entry.getPositionMatrix(), x3, y3, z3).color(r, g, b, a).texture(u0, v1)
                .overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(entry, nx, ny, nz);
    }
}
