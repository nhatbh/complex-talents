package com.complextalents.elemental.client.renderers.entities;

import com.complextalents.elemental.entity.SpringPotionEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
 * Custom renderer for Spring Potion Entity
 * Renders the entity as a glowing potion item
 */
public class SpringPotionRenderer extends EntityRenderer<SpringPotionEntity> {

    public SpringPotionRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SpringPotionEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int light) {

        poseStack.pushPose();

        // Position the entity
        float scale = 0.8f;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0, 0.3, 0); // Lift up slightly

        // Bobbing animation
        float bobOffset = (float) Math.sin((entity.level().getGameTime() + partialTicks) * 0.1) * 0.1f;
        poseStack.translate(0, bobOffset, 0);

        // Rotate to make it look like it's floating and spinning
        float rotation = (entity.level().getGameTime() % 360) + partialTicks;
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 0.5f));

        // Render the potion item
        ItemStack potionStack = new ItemStack(Items.POTION);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            potionStack,
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
    public ResourceLocation getTextureLocation(SpringPotionEntity entity) {
        // Use the block atlas for item rendering
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
