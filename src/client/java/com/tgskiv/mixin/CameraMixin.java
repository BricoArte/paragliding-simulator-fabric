package com.tgskiv.mixin;

import com.tgskiv.skydiving.entity.ParagliderEntity;
import com.tgskiv.skydiving.render.ParagliderRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {
    private static final float CAMERA_ROLL_SCALE = 1.0f;
    private static final float CAMERA_ROLL_LERP = 0.03f;
    private static float cameraRoll = 0.0f;
    private static int spinDelayTicks = 0;
    private static final int SPIN_DELAY_TICKS = 10; // ~0.5s at 20 tps

    @Shadow
    @Final
    private Quaternionf rotation;

    @Inject(method = "update", at = @At("TAIL"))
    private void paraglider$applyRoll(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        ParagliderEntity paraglider = null;
        if (focusedEntity instanceof ParagliderEntity direct) {
            paraglider = direct;
        } else if (focusedEntity instanceof PlayerEntity player && player.getVehicle() instanceof ParagliderEntity riding) {
            paraglider = riding;
        }
        if (paraglider == null) {
            cameraRoll = 0.0f;
            spinDelayTicks = 0;
            return;
        }
        float spinProgress = paraglider.getTrackedSpinProgress();
        if (spinProgress > 0.01f) {
            spinDelayTicks = Math.min(spinDelayTicks + 1, SPIN_DELAY_TICKS);
        } else {
            spinDelayTicks = 0;
        }
        float targetRollDeg = spinProgress > 0.01f && spinDelayTicks >= SPIN_DELAY_TICKS
                ? ParagliderRenderer.getRollTargetDegrees(paraglider)
                : 0.0f;
        cameraRoll = MathHelper.lerp(CAMERA_ROLL_LERP, cameraRoll, targetRollDeg);
        if (Math.abs(cameraRoll) < 0.01f) {
            return;
        }
        float radians = (float) Math.toRadians(cameraRoll) * CAMERA_ROLL_SCALE;
        rotation.mul(new Quaternionf().rotateZ(radians));
    }
}
