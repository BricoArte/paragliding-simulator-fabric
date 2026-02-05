package com.tgskiv.skydiving.render;

import com.tgskiv.skydiving.blocks.ModModelLayers;
import com.tgskiv.skydiving.entity.ParagliderEntity;
import com.tgskiv.skydiving.model.ParagliderModel;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.WeakHashMap;

public class ParagliderRenderer extends EntityRenderer<ParagliderEntity, ParagliderRenderState> {
    private static final Identifier TEXTURE = Identifier.of("paraglidingsimulator", "textures/entity/paraglider.png");
    private final ParagliderModel model;
    private static final float MAX_ROLL_DEG = -13.0f;
    private static final float ROLL_LERP = 0.05f;
    private static final float MAX_PITCH_DEG = 8.0f;
    private static final float PITCH_LERP = 0.05f;
    private static float lastRenderedRoll = 0.0f;
    private static float lastRenderedPitch = 0.0f;
    private static final Map<ParagliderEntity, Float> ROLL_CACHE = new WeakHashMap<>();
    private static final Map<ParagliderEntity, Float> PITCH_CACHE = new WeakHashMap<>();

    public ParagliderRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new ParagliderModel(ctx.getPart(ModModelLayers.PARAGLIDER_LAYER));
    }

    @Override
    public ParagliderRenderState createRenderState() {
        return new ParagliderRenderState();
    }

    @Override
    public void render(ParagliderRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.yaw));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));
        VertexConsumer vc = vertexConsumers.getBuffer(model.getLayer(TEXTURE));
        model.renderModel(matrices, vc, light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
        lastRenderedRoll = state.roll;
        lastRenderedPitch = state.pitch;
    }

    @Override
    public boolean shouldRender(ParagliderEntity entity, Frustum frustum, double x, double y, double z) {
        // Expand the culling box so the wing doesn't disappear when only the harness is in view.
        Box box = entity.getBoundingBox().expand(4.0);
        return frustum.isVisible(box);
    }

    public void updateRenderState(ParagliderEntity entity, ParagliderRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        if (entity.consumeRenderResetRequest()) {
            ROLL_CACHE.remove(entity);
            PITCH_CACHE.remove(entity);
        }
        state.yaw = entity.getRenderYaw(tickDelta);
        state.speedSq = (float) entity.getVelocity().lengthSquared();
        // Colapsar solo si esta en suelo y practicamente parado; si se mueve en suelo o vuela, mantener erguido
        state.collapsed = entity.isGrounded() && state.speedSq < 0.0001f;
        state.roll = computeRoll(entity, state);
        state.pitch = computePitch(entity, state);

        model.setAngles(state);
    }

    public Identifier getTexture(ParagliderRenderState state) {
        return TEXTURE;
    }

    private static float computeRoll(ParagliderEntity entity, ParagliderRenderState state) {
        if (entity.isGrounded()) {
            ROLL_CACHE.put(entity, 0.0f);
            return 0.0f;
        }
        int turnInput = entity.getTrackedTurnInput();
        float spinProgress = entity.getTrackedSpinProgress();
        float rollScale = 1.0f + spinProgress;
        float base = (float) Math.copySign(Math.abs(MAX_ROLL_DEG) * rollScale, MAX_ROLL_DEG);
        float target = turnInput * base;
        float current = ROLL_CACHE.getOrDefault(entity, 0.0f);
        float next = MathHelper.lerp(ROLL_LERP, current, target);
        ROLL_CACHE.put(entity, next);
        return next;
    }

    private static float computePitch(ParagliderEntity entity, ParagliderRenderState state) {
        float forward = entity.getTrackedForwardInput();
        float target = -forward * MAX_PITCH_DEG;
        float current = PITCH_CACHE.getOrDefault(entity, 0.0f);
        float next = MathHelper.lerp(PITCH_LERP, current, target);
        PITCH_CACHE.put(entity, next);
        return next;
    }

    public static float getRollTargetDegrees(ParagliderEntity entity) {
        int turnInput = entity.getTrackedTurnInput();
        float spinProgress = entity.getTrackedSpinProgress();
        float rollScale = 1.0f + spinProgress;
        float base = (float) Math.copySign(Math.abs(MAX_ROLL_DEG) * rollScale, MAX_ROLL_DEG);
        return turnInput * base;
    }

    public static float getLastRenderedRoll() {
        return lastRenderedRoll;
    }

    public static float getLastRenderedPitch() {
        return lastRenderedPitch;
    }
}
