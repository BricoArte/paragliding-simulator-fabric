// Made with Blockbench 4.12.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports


public class WindCone_Converted<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("modid", "windcone_converted"), "main");
	private final ModelPart group2;
	private final ModelPart group;
	private final ModelPart bone;
	private final ModelPart bb_main;

	public WindCone_Converted(ModelPart root) {
		this.group2 = root.getChild("group2");
		this.group = this.group2.getChild("group");
		this.bone = root.getChild("bone");
		this.bb_main = root.getChild("bb_main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group2 = partdefinition.addOrReplaceChild("group2", CubeListBuilder.create(), PartPose.offset(17.0F, -2.0F, 0.0F));

		PartDefinition group = group2.addOrReplaceChild("group", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition bone = partdefinition.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -32.0F, 8.0F, 1.0F, 32.0F, 1.0F, new CubeDeformation(0.0F))
		.texOffs(8, 35).addBox(-7.0F, -32.0F, 4.0F, 6.0F, 9.0F, 9.0F, new CubeDeformation(0.0F)), PartPose.offset(8.0F, 24.0F, -8.0F));

		PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(8, 20).addBox(3.0F, -31.0F, -3.0F, 9.0F, 7.0F, 7.0F, new CubeDeformation(0.0F))
		.texOffs(8, 8).addBox(7.0F, -30.0F, -2.0F, 10.0F, 5.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(8, 0).addBox(12.0F, -29.0F, -1.0F, 10.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 24.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		group2.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bone.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
