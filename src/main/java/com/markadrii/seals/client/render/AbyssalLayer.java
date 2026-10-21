package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import com.markadrii.seals.seal.SealVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Forma abisal del Elder Guardian: escamas sólidas con degradado de sus colores sobre la skin,
 * manchas de esos colores en la propia piel,
 * pinchos en la cabeza, la espalda y los brazos, y una cola que se mueve.
 */
public class AbyssalLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation ELDER = new ResourceLocation("minecraft", "textures/entity/guardian_elder.png");
    private static final ResourceLocation SCALES = Seals.id("textures/entity/abyssal_scales.png");
    private static final ResourceLocation PATCHES = Seals.id("textures/entity/abyssal_patches.png");

    // Copias del modelo del jugador, algo infladas, para pintar encima de la skin (como una armadura)
    private final PlayerModel<AbstractClientPlayer> scalesWide = shell(0.30F, false);
    private final PlayerModel<AbstractClientPlayer> scalesSlim = shell(0.30F, true);
    private final PlayerModel<AbstractClientPlayer> patchesWide = shell(0.02F, false);
    private final PlayerModel<AbstractClientPlayer> patchesSlim = shell(0.02F, true);

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart tail0;
    private final ModelPart tail1;
    private final ModelPart tail2;

    public AbyssalLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
        ModelPart root = createLayer().bakeRoot();
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.tail0 = body.getChild("tail0");
        this.tail1 = tail0.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
    }

    /** Pinchos y cola. Las coordenadas de textura son las del propio guardián (64x64). */
    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation d = CubeDeformation.NONE;
        CubeListBuilder spike = CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -3.0F, -0.5F, 1, 3, 1, d);

        // Espalda: tres pinchos en la columna
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 3; i++) {
            body.addOrReplaceChild("back_spike" + i, spike,
                    PartPose.offsetAndRotation(0.0F, 2.0F + i * 3.5F, 2.2F, -1.0F, 0.0F, 0.0F));
        }
        // Hombros
        body.addOrReplaceChild("shoulder_r", spike, PartPose.offsetAndRotation(-3.0F, 0.5F, 1.0F, -0.5F, 0.0F, -0.4F));
        body.addOrReplaceChild("shoulder_l", spike, PartPose.offsetAndRotation(3.0F, 0.5F, 1.0F, -0.5F, 0.0F, 0.4F));

        // Cola en tres trozos con aleta al final (como la del guardián)
        PartDefinition t0 = body.addOrReplaceChild("tail0", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-1.5F, -1.5F, 0.0F, 3, 3, 5, d), PartPose.offsetAndRotation(0.0F, 10.5F, 2.0F, 0.5F, 0.0F, 0.0F));
        PartDefinition t1 = t0.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(0, 54).addBox(-1.0F, -1.0F, 0.0F, 2, 2, 5, d), PartPose.offset(0.0F, 0.0F, 5.0F));
        t1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(41, 32).addBox(-0.5F, -0.5F, 0.0F, 1, 1, 4, d)
                .texOffs(25, 19).addBox(0.0F, -3.0F, 2.0F, 0, 6, 5, d), PartPose.offset(0.0F, 0.0F, 5.0F));

        // Cabeza: dos pinchos arriba y uno detrás
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("head_spike_r", spike, PartPose.offsetAndRotation(-2.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.35F));
        head.addOrReplaceChild("head_spike_l", spike, PartPose.offsetAndRotation(2.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.35F));
        head.addOrReplaceChild("head_spike_b", spike, PartPose.offsetAndRotation(0.0F, -6.0F, 4.0F, -1.2F, 0.0F, 0.0F));

        // Brazos: un pincho hacia fuera en cada antebrazo
        PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        ra.addOrReplaceChild("arm_spike", spike, PartPose.offsetAndRotation(-3.0F, 6.0F, 0.0F, 0.0F, 0.0F, -1.4F));
        PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        la.addOrReplaceChild("arm_spike", spike, PartPose.offsetAndRotation(3.0F, 6.0F, 0.0F, 0.0F, 0.0F, 1.4F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !SealVisuals.abyssal(player)) return;
        int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
        PlayerModel<AbstractClientPlayer> model = getParentModel();

        // 1) Manchas con los colores del Elder Guardian pegadas a la skin (en los huecos de las escamas)
        // 2) Escamas sólidas con degradado, un poco por encima de la skin, como una armadura
        boolean slim = "slim".equals(player.getModelName());
        renderShell(poseStack, buffer, packedLight, overlay, model, slim ? patchesSlim : patchesWide, PATCHES);
        renderShell(poseStack, buffer, packedLight, overlay, model, slim ? scalesSlim : scalesWide, SCALES);

        // Cola que se mueve
        float swing = Mth.sin(ageInTicks * 0.12F) * 0.3F + Mth.cos(limbSwing * 0.6662F) * 0.35F * limbSwingAmount;
        tail0.yRot = swing * 0.6F;
        tail1.yRot = swing * 0.9F;
        tail2.yRot = swing * 1.2F;
        tail0.xRot = 0.5F + limbSwingAmount * 0.3F;

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(ELDER));
        renderOn(poseStack, model.body, body, vc, packedLight, overlay);
        renderOn(poseStack, model.head, head, vc, packedLight, overlay);
        renderOn(poseStack, model.rightArm, rightArm, vc, packedLight, overlay);
        renderOn(poseStack, model.leftArm, leftArm, vc, packedLight, overlay);
    }

    private static PlayerModel<AbstractClientPlayer> shell(float inflate, boolean slim) {
        MeshDefinition mesh = PlayerModel.createMesh(new CubeDeformation(inflate), slim);
        PlayerModel<AbstractClientPlayer> m = new PlayerModel<>(LayerDefinition.create(mesh, 64, 64).bakeRoot(), slim);
        // Solo la capa base: la capa exterior de la skin (gorro, chaqueta...) no se usa
        m.hat.visible = false;
        m.jacket.visible = false;
        m.leftSleeve.visible = false;
        m.rightSleeve.visible = false;
        m.leftPants.visible = false;
        m.rightPants.visible = false;
        return m;
    }

    /** Pinta una capa sólida (sin transparencia parcial) que se mueve igual que el jugador. */
    private static void renderShell(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay,
                                    PlayerModel<AbstractClientPlayer> parent, PlayerModel<AbstractClientPlayer> shell,
                                    ResourceLocation texture) {
        parent.copyPropertiesTo(shell);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texture));
        shell.renderToBuffer(poseStack, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Dibuja {@code extra} pegado a la pieza del jugador (se mueve con ella). */
    private static void renderOn(PoseStack poseStack, ModelPart playerPart, ModelPart extra, VertexConsumer vc, int light, int overlay) {
        poseStack.pushPose();
        playerPart.translateAndRotate(poseStack);
        extra.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }
}
