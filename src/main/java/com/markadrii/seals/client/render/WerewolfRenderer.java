package com.markadrii.seals.client.render;

import com.markadrii.seals.Seals;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;

/**
 * Dibuja al jugador transformado en hombre lobo en lugar de su modelo normal.
 * Lleva los objetos en las garras y los ojos le brillan en rojo.
 */
public class WerewolfRenderer extends LivingEntityRenderer<AbstractClientPlayer, WerewolfModel> {
    public static final ResourceLocation TEXTURE = Seals.id("textures/entity/werewolf.png");
    public static final ResourceLocation EYES = Seals.id("textures/entity/werewolf_eyes.png");
    private static final RenderType EYES_TYPE = RenderType.eyes(EYES);
    public static final float SCALE = 1.1F;

    @Nullable private static WerewolfRenderer instance;

    public WerewolfRenderer(EntityRendererProvider.Context context) {
        super(context, new WerewolfModel(WerewolfModel.createLayer().bakeRoot()), 0.5F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES_TYPE;
            }
        });
    }

    /** Se crea la primera vez que hace falta (con las piezas del propio juego). */
    public static WerewolfRenderer get() {
        if (instance == null) {
            Minecraft mc = Minecraft.getInstance();
            var dispatcher = mc.getEntityRenderDispatcher();
            var context = new EntityRendererProvider.Context(dispatcher, mc.getItemRenderer(), mc.getBlockRenderer(),
                    dispatcher.getItemInHandRenderer(), mc.getResourceManager(), mc.getEntityModels(), mc.font);
            instance = new WerewolfRenderer(context);
        }
        return instance;
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        return TEXTURE;
    }

    @Override
    public void render(AbstractClientPlayer player, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        WerewolfModel m = getModel();
        m.crouching = player.isCrouching();
        m.rightArmPose = player.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        m.leftArmPose = player.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        super.render(player, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    protected void scale(AbstractClientPlayer player, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE * 0.9375F, SCALE * 0.9375F, SCALE * 0.9375F);
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player) {
        return player != Minecraft.getInstance().getCameraEntity() && super.shouldShowName(player);
    }

    /** Brazo en primera persona: la zarpa del hombre lobo. */
    public void renderArm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, HumanoidArm side) {
        WerewolfModel m = getModel();
        m.attackTime = 0.0F;
        m.crouching = false;
        m.swimAmount = 0.0F;
        m.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        var arm = side == HumanoidArm.RIGHT ? m.rightArm : m.leftArm;
        arm.xRot = 0.0F;
        arm.zRot = 0.0F;
        arm.render(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, OverlayTexture.NO_OVERLAY);
    }
}
