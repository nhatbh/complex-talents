package com.complextalents.impl.highpriest.client.renderer;

import com.complextalents.impl.highpriest.entity.SeraphsEdgeEntity;
import com.complextalents.impl.highpriest.item.HighPriestItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer for the Seraph's Edge entity.
 */
public class SeraphsEdgeRenderer extends EntityRenderer<SeraphsEdgeEntity> {

    public SeraphsEdgeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(
            SeraphsEdgeEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float pYaw = entity.prevYawRender;
        float cYaw = entity.yawRender;
        float pPitch = entity.prevPitchRender;
        float cPitch = entity.pitchRender;
        float pRoll = entity.prevRollRender;
        float cRoll = entity.rollRender;

        if (!Float.isFinite(pYaw)) pYaw = Float.isFinite(cYaw) ? cYaw : 0;
        if (!Float.isFinite(cYaw)) cYaw = pYaw;
        if (!Float.isFinite(pPitch)) pPitch = Float.isFinite(cPitch) ? cPitch : 0;
        if (!Float.isFinite(cPitch)) cPitch = pPitch;
        if (!Float.isFinite(pRoll)) pRoll = Float.isFinite(cRoll) ? cRoll : 0;
        if (!Float.isFinite(cRoll)) cRoll = pRoll;

        poseStack.pushPose();

        float yaw = Mth.rotLerp(partialTicks, pYaw, cYaw);
        float pitch = Mth.lerp(partialTicks, pPitch, cPitch);
        float roll = Mth.lerp(partialTicks, pRoll, cRoll);

        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        poseStack.translate(0, 0.25, 0);

        ItemStack stack = new ItemStack(HighPriestItems.DIVINE_PUNISHER.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                15728880,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SeraphsEdgeEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public boolean shouldRender(SeraphsEdgeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
