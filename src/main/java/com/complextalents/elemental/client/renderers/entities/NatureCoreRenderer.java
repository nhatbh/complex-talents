package com.complextalents.elemental.client.renderers.entities;

import com.complextalents.elemental.entity.NatureCoreEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Custom renderer for Nature Core Entity
 * Renders the entity as a tiny glowing slime block item
 */
public class NatureCoreRenderer extends EntityRenderer<NatureCoreEntity> {

    public NatureCoreRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(NatureCoreEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int light) {

        poseStack.pushPose();

        // Position the entity
        float scale = 1F; // Half size for a tiny appearance
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.2, 0); // Lift up slightly

        // Rotate to make it look like it's floating
        float rotation = (entity.level().getGameTime() % 360) + partialTicks;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation * 0.5f));

        // Render the slime block item
        Minecraft.getInstance().getItemRenderer().renderStatic(
            new ItemStack(Items.SLIME_BLOCK),
            ItemDisplayContext.GROUND,
            light,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );

        poseStack.popPose();

        super.render(entity, yaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(NatureCoreEntity entity) {
        // Use the block atlas for item rendering
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
