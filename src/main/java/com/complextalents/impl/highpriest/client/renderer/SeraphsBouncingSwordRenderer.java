package com.complextalents.impl.highpriest.client.renderer;

import com.complextalents.impl.highpriest.entity.SeraphsBouncingSwordEntity;
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
 * Renderer for the Seraph's Bouncing Sword entity.
 * Renders the sword item model and rotates it based on movement direction.
 * Supports homing turning with roll/banking.
 */
public class SeraphsBouncingSwordRenderer extends EntityRenderer<SeraphsBouncingSwordEntity> {

    public SeraphsBouncingSwordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.15f;
    }

    @Override
    public void render(
            SeraphsBouncingSwordEntity entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        // 1. Snapshot values to local variables immediately
        float pYaw = entity.prevYawRender;
        float cYaw = entity.yawRender;
        float pPitch = entity.prevPitchRender;
        float cPitch = entity.pitchRender;
        float pRoll = entity.prevRollRender;
        float cRoll = entity.rollRender;

        // 2. EMERGENCY SANITIZATION: If state is corrupted, reset to current or zero
        // This prevents Mth.lerp from returning NaN
        if (!Float.isFinite(pYaw)) pYaw = Float.isFinite(cYaw) ? cYaw : 0;
        if (!Float.isFinite(cYaw)) cYaw = pYaw;

        if (!Float.isFinite(pPitch)) pPitch = Float.isFinite(cPitch) ? cPitch : 0;
        if (!Float.isFinite(cPitch)) cPitch = pPitch;

        if (!Float.isFinite(pRoll)) pRoll = Float.isFinite(cRoll) ? cRoll : 0;
        if (!Float.isFinite(cRoll)) cRoll = pRoll;

        poseStack.pushPose();

        // 3. Use rotLerp for Yaw (prevents 360-degree spins when crossing 0/360 boundary)
        float yaw = Mth.rotLerp(partialTicks, pYaw, cYaw);
        float pitch = Mth.lerp(partialTicks, pPitch, cPitch);
        float roll = Mth.lerp(partialTicks, pRoll, cRoll);

        // 4. Transform - Apply rotations in a consistent, stable order (Y -> X -> Z)
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        // Center the sword model
        poseStack.translate(0, 0.25, 0);

        ItemStack stack = new ItemStack(HighPriestItems.DIVINE_PUNISHER.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                15728880, // Full bright
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId() // Pass entity ID for consistent random seeds
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SeraphsBouncingSwordEntity entity) {
        // Item renderer uses atlas; this value is not actually used
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public boolean shouldRender(SeraphsBouncingSwordEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true; // disable frustum culling for testing
    }

}
