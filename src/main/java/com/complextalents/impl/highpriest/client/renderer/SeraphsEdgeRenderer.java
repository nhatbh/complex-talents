package com.complextalents.impl.highpriest.client.renderer;

import com.complextalents.elemental.client.renderers.entities.CustomRenderTypes;
import com.complextalents.impl.highpriest.entity.SeraphsEdgeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renderer for the Seraph's Edge entity.
 * Renders as a yellow-white glowing orb.
 */
public class SeraphsEdgeRenderer extends EntityRenderer<SeraphsEdgeEntity> {

    private static final ResourceLocation BLOCK_ATLAS = InventoryMenu.BLOCK_ATLAS;

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
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        float time = entity.tickCount + partialTicks;
        float bobOffset = (float) Math.sin(time * 0.1f) * 0.05f;
        poseStack.translate(0, 0.5 + bobOffset, 0); // Shift up + bobbing
        poseStack.scale(0.6f, 0.6f, 0.6f);

        // Render core sphere (solid, bright yellow-white)
        VertexConsumer coreConsumer = buffer.getBuffer(CustomRenderTypes.sphereNoCull());
        renderSphere(poseStack, coreConsumer, 0.4f, 20, 20, 240, 255, 255, 200, 255);

        // Render outer glow (larger, transparent)
        VertexConsumer glowConsumer = buffer.getBuffer(CustomRenderTypes.sphereGlow());
        renderSphere(poseStack, glowConsumer, 0.55f, 16, 16, 240, 255, 255, 150, 80);

        poseStack.popPose();
    }

    private void renderSphere(PoseStack poseStack, VertexConsumer consumer,
                              float radius, int stacks, int slices, int packedLight,
                              int r, int g, int b, int a) {
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();

        // Create sphere using latitude and longitude
        for (int i = 0; i < stacks; i++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) i / stacks);
            float z0 = (float) Math.sin(lat0);
            float zr0 = (float) Math.cos(lat0);

            float lat1 = (float) Math.PI * (-0.5f + (float) (i + 1) / stacks);
            float z1 = (float) Math.sin(lat1);
            float zr1 = (float) Math.cos(lat1);

            for (int j = 0; j < slices; j++) {
                float lng0 = 2 * (float) Math.PI * (float) j / slices;
                float x0 = (float) Math.cos(lng0);
                float y0 = (float) Math.sin(lng0);

                float lng1 = 2 * (float) Math.PI * (float) (j + 1) / slices;
                float x1 = (float) Math.cos(lng1);
                float y1 = (float) Math.sin(lng1);

                // First triangle
                addVertex(consumer, matrix4f, matrix3f,
                        x0 * zr0 * radius, y0 * zr0 * radius, z0 * radius,
                        x0 * zr0, y0 * zr0, z0, packedLight, r, g, b, a);
                addVertex(consumer, matrix4f, matrix3f,
                        x1 * zr0 * radius, y1 * zr0 * radius, z0 * radius,
                        x1 * zr0, y1 * zr0, z0, packedLight, r, g, b, a);
                addVertex(consumer, matrix4f, matrix3f,
                        x1 * zr1 * radius, y1 * zr1 * radius, z1 * radius,
                        x1 * zr1, y1 * zr1, z1, packedLight, r, g, b, a);

                // Second triangle
                addVertex(consumer, matrix4f, matrix3f,
                        x0 * zr0 * radius, y0 * zr0 * radius, z0 * radius,
                        x0 * zr0, y0 * zr0, z0, packedLight, r, g, b, a);
                addVertex(consumer, matrix4f, matrix3f,
                        x1 * zr1 * radius, y1 * zr1 * radius, z1 * radius,
                        x1 * zr1, y1 * zr1, z1, packedLight, r, g, b, a);
                addVertex(consumer, matrix4f, matrix3f,
                        x0 * zr1 * radius, y0 * zr1 * radius, z1 * radius,
                        x0 * zr1, y0 * zr1, z1, packedLight, r, g, b, a);
            }
        }
    }

    private void addVertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                           float x, float y, float z, float nx, float ny, float nz, int packedLight,
                           int r, int g, int b, int a) {
        consumer.vertex(matrix4f, x, y, z)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(0)
                .uv2(packedLight)
                .normal(matrix3f, nx, ny, nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SeraphsEdgeEntity entity) {
        return BLOCK_ATLAS;
    }

    @Override
    public boolean shouldRender(SeraphsEdgeEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }
}
