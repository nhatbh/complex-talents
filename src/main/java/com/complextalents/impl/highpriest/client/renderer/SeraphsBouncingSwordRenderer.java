package com.complextalents.impl.highpriest.client.renderer;

import com.complextalents.impl.highpriest.entity.SeraphsBouncingSwordEntity;
import com.complextalents.impl.highpriest.item.HighPriestItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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
        poseStack.pushPose();

        // Interpolated rotations
        float yaw = Mth.lerp(partialTicks, entity.prevYawRender, entity.yawRender);
        float pitch = Mth.lerp(partialTicks, entity.prevPitchRender, entity.pitchRender);
        float roll = Mth.lerp(partialTicks, entity.prevRollRender, entity.rollRender);

        // Apply in correct order
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

        ItemStack stack = new ItemStack(HighPriestItems.DIVINE_PUNISHER.get());
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                15728880, // full bright; replace with packedLight if you want world lighting
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                0
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SeraphsBouncingSwordEntity entity) {
        // Item renderer uses atlas; this value is not actually used
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
