package com.tgskiv.skydiving.model;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tgskiv.skydiving.render.ParagliderRenderState;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

public class ParagliderModel extends EntityModel<ParagliderRenderState> {
    private final ModelPart root;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart lineLeft;
    private final ModelPart lineRight;
    private final ModelPart harness;

    public ParagliderModel(ModelPart root) {
        super(root, RenderLayer::getEntityCutout);
        ModelPart r = root.getChild("root");
        this.root = r;
        this.wingLeft = r.getChild("wing_left");
        this.wingRight = r.getChild("wing_right");
        this.lineLeft = r.getChild("line_left");
        this.lineRight = r.getChild("line_right");
        this.harness = r.getChild("harness");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot().addChild("root", ModelPartBuilder.create(), ModelTransform.NONE);

        // Wing left
        root.addChild("wing_left",
                ModelPartBuilder.create()
                        .uv(0, 0)
                        .cuboid(-36.0f, -6.0f, -18.0f, 84.0f, 12.0f, 36.0f),
                ModelTransform.of(0.0f, 114.0f, 18.0f, 0.0f, 0.0f, MathHelper.RADIANS_PER_DEGREE * 22.5f));

        // Wing right
        root.addChild("wing_right",
                ModelPartBuilder.create()
                        .uv(0, 50)
                        .cuboid(-48.0f, -6.0f, -18.0f, 84.0f, 12.0f, 36.0f),
                ModelTransform.of(84.0f, 114.0f, 18.0f, 0.0f, 0.0f, MathHelper.RADIANS_PER_DEGREE * -22.5f));

        // Harness block
        root.addChild("harness",
                ModelPartBuilder.create()
                        .uv(0, 100)
                        .cuboid(0.0f, 0.0f, 0.0f, 15.0f, 21.0f, 48.0f),
                ModelTransform.pivot(34.2f, 0.0f, -30.0f));

        // Lines left
        root.addChild("line_left",
                ModelPartBuilder.create()
                        .uv(130, 100)
                        .cuboid(-6.0f, 0.0f, 0.0f, 3.0f, 108.0f, 3.0f),
                ModelTransform.of(12.0f, 48.0f, 0.0f, 0.0f, 0.0f, MathHelper.RADIANS_PER_DEGREE * 22.5f));

        // Lines right
        root.addChild("line_right",
                ModelPartBuilder.create()
                        .uv(150, 100)
                        .cuboid(0.0f, 0.0f, 0.0f, 3.0f, 108.0f, 3.0f),
                ModelTransform.of(54.0f, 54.0f, 0.0f, 0.0f, 0.0f, MathHelper.RADIANS_PER_DEGREE * -22.5f));

        return TexturedModelData.of(modelData, 256, 256);
    }

    public void setAngles(ParagliderRenderState state) {
        // Pivots base (erguido)
        final float baseWingLeftX = 0f, baseWingLeftY = 114f, baseWingLeftZ = 6f;
        final float baseWingRightX = 84f, baseWingRightY = 114f, baseWingRightZ = 6f;
        final float baseLineLeftX = 30f, baseLineLeftY = 12f, baseLineLeftZ = 0f;
        final float baseLineRightX = 54f, baseLineRightY = 12f, baseLineRightZ = 0f;

        // Pivot de colapso (solo alas/lÃ­neas). Ajusta aquÃ­ para cambiar el punto de giro sin mover el arnÃ©s.
        final float collapsePivotX = 42f;
        final float collapsePivotY = 0f;
        final float collapsePivotZ = 60f;

        boolean collapsed = state.collapsed;
        float collapsedPitch = collapsed ? MathHelper.HALF_PI : 0f;

        if (collapsed) {
            wingLeft.setPivot(collapsePivotX + 30f, collapsePivotY, collapsePivotZ + 36f);
            wingRight.setPivot(collapsePivotX - 30f, collapsePivotY, collapsePivotZ + 36f);
            lineLeft.setPivot(collapsePivotX + 30f, collapsePivotY, collapsePivotZ - 60f);
            lineRight.setPivot(collapsePivotX - 30f, collapsePivotY, collapsePivotZ - 60f);
        } else {
            wingLeft.setPivot(baseWingLeftX, baseWingLeftY, baseWingLeftZ);
            wingRight.setPivot(baseWingRightX, baseWingRightY, baseWingRightZ);
            lineLeft.setPivot(baseLineLeftX, baseLineLeftY, baseLineLeftZ);
            lineRight.setPivot(baseLineRightX, baseLineRightY, baseLineRightZ);
        }

        wingLeft.pitch = collapsedPitch;
        wingRight.pitch = collapsedPitch;
        lineLeft.pitch = collapsedPitch;
        lineRight.pitch = collapsedPitch;

        // harness stays as is
        harness.pitch = 0f;
        harness.roll = 0f;
    }

    public void renderModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        matrices.push();
        // Center the model roughly around the entity origin
        matrices.translate(-1.3f, -0f, -0f);
        matrices.scale(0.5f, 0.5f, 0.5f);

        root.render(matrices, vertices, light, overlay);
        matrices.pop();
    }

}
