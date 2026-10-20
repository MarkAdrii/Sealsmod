package com.markadrii.seals.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/**
 * Modelo del hombre lobo: cabeza de lobo con hocico y orejas, brazos largos con garras y cola.
 * Usa la misma estructura que el jugador, así que anda, ataca y se agacha igual.
 * Textura: textures/entity/werewolf.png (64x64).
 */
public class WerewolfModel extends HumanoidModel<AbstractClientPlayer> {
    private final ModelPart tail;

    public WerewolfModel(ModelPart root) {
        super(root);
        this.tail = root.getChild("body").getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation none = CubeDeformation.NONE;

        // Cabeza con hocico y orejas
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, none)
                .texOffs(32, 0).addBox(-2.0F, -4.0F, -8.0F, 4, 4, 4, none), PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                .texOffs(48, 0).addBox(-4.0F, -11.0F, -1.0F, 2, 3, 1, none), PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                .texOffs(48, 0).mirror().addBox(2.0F, -11.0F, -1.0F, 2, 3, 1, none), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        // Cuerpo con cola
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, none), PartPose.ZERO);
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-1.0F, 0.0F, 0.0F, 2, 9, 2, none),
                PartPose.offsetAndRotation(0.0F, 10.0F, 2.0F, 0.7F, 0.0F, 0.0F));

        // Brazos largos con garras
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(48, 16).addBox(-3.0F, -2.0F, -2.0F, 4, 14, 4, none), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(48, 36).addBox(-1.0F, -2.0F, -2.0F, 4, 14, 4, none), PartPose.offset(5.0F, 2.0F, 0.0F));
        for (int i = 0; i < 3; i++) {
            rightArm.addOrReplaceChild("claw_" + i, CubeListBuilder.create()
                    .texOffs(56, 0).addBox(-2.6F + i * 1.3F, 12.0F, -2.6F, 1, 3, 1, none), PartPose.ZERO);
            leftArm.addOrReplaceChild("claw_" + i, CubeListBuilder.create()
                    .texOffs(56, 0).addBox(-0.6F + i * 1.3F, 12.0F, -2.6F, 1, 3, 1, none), PartPose.ZERO);
        }

        // Piernas
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, none), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, none), PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // La cola se mueve sola y más rápido al correr
        tail.yRot = Mth.sin(ageInTicks * 0.15F) * 0.25F + Mth.cos(limbSwing * 0.6662F) * 0.4F * limbSwingAmount;
        tail.xRot = 0.7F + limbSwingAmount * 0.4F;
        // Brazos algo más abiertos, como garras listas
        rightArm.zRot += 0.08F;
        leftArm.zRot -= 0.08F;
    }
}
