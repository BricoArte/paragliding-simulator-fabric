package com.tgskiv.skydiving.blocks;

import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class WindsockModel extends Model {
    private final ModelPart root;
    public final ModelPart stick;
    public final ModelPart cone1;
    public final ModelPart cone2;
    public final ModelPart cone3;
    public final ModelPart cone4;

    public static final Identifier STICK_TEXTURE = Identifier.of("paraglidingsimulator", "textures/entity/brown_stick_texture.png");
    public static final Identifier RED_TEXTURE = Identifier.of("paraglidingsimulator", "textures/block/red_cone_texture.png");
    public static final Identifier WHITE_TEXTURE = Identifier.of("paraglidingsimulator", "textures/block/white_cone_texture.png");


    public WindsockModel(ModelPart root) {
        super(root, RenderLayer::getEntityCutout);
        this.root = root;
        this.stick = root.getChild("stick");
        this.cone1 = root.getChild("cone1");
        this.cone2 = root.getChild("cone2");
        this.cone3 = root.getChild("cone3");
        this.cone4 = root.getChild("cone4");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        int higher = 8;

        root.addChild("stick",
                ModelPartBuilder
                        .create()
                        .uv(0, 0)
                        .cuboid(0, 0, 0, 1, 32+higher, 1),
                ModelTransform.pivot(8, 0, 8)
        );

        root.addChild("cone1",
                ModelPartBuilder
                        .create()
                        .uv(0, 0)
                        .cuboid(0, 0, 0, 6, 9, 9),
                ModelTransform.pivot(1, 23+higher, 4)
        );

        ModelPartData cone2 = root.addChild("cone2",
                ModelPartBuilder
                        .create()
                        .uv(0, 0)
                        .cuboid(0, 0, 0, 9, 7, 7),
                ModelTransform.pivot(-4, 24+higher, 5)
        );

        ModelPartData cone3 = root.addChild("cone3",
                ModelPartBuilder
                        .create()
                        .uv(0, 0)
                        .cuboid(0, 0, 0, 10, 5, 5),
                ModelTransform.pivot(-9, 25+higher, 6)
        );

        ModelPartData cone4 = root.addChild("cone4",
                ModelPartBuilder
                        .create()
                        .uv(0, 0)
                        .cuboid(0, 0, 0, 10, 3, 3),
                ModelTransform.pivot(-14, 26+higher, 7)
        );

        return TexturedModelData.of(modelData, 32, 32);
    }

//    @Override
//    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
//        root.render(matrices, vertices, light, overlay, color);
//    }

}
