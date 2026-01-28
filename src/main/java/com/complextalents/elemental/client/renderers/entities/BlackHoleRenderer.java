package com.complextalents.elemental.client.renderers.entities;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.complextalents.elemental.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Custom renderer for Black Hole Entity
 * Renders a procedural sphere using black concrete texture
 * - Fast horizontal spin (Y axis)
 * - Slower vertical spin (X axis)
 * - Shrinks during implosion
 */
public class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {

    private static final ResourceLocation BLOCK_ATLAS = InventoryMenu.BLOCK_ATLAS;

    public BlackHoleRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

@Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTicks, 
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        
        poseStack.pushPose();
        
        // Get the vertex consumer for rendering
        VertexConsumer vertexConsumer = buffer.getBuffer(CustomRenderTypes.sphereNoCull());
        
        // Render the sphere
        renderSphere(poseStack, vertexConsumer, 0.2f, 20, 20, packedLight);
        
        poseStack.popPose();
    }
    
    private void renderSphere(PoseStack poseStack, VertexConsumer consumer, 
                             float radius, int stacks, int slices, int packedLight) {
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
                         x0 * zr0, y0 * zr0, z0, packedLight);
                addVertex(consumer, matrix4f, matrix3f,
                         x1 * zr0 * radius, y1 * zr0 * radius, z0 * radius,
                         x1 * zr0, y1 * zr0, z0, packedLight);
                addVertex(consumer, matrix4f, matrix3f,
                         x1 * zr1 * radius, y1 * zr1 * radius, z1 * radius,
                         x1 * zr1, y1 * zr1, z1, packedLight);
                
                // Second triangle
                addVertex(consumer, matrix4f, matrix3f,
                         x0 * zr0 * radius, y0 * zr0 * radius, z0 * radius,
                         x0 * zr0, y0 * zr0, z0, packedLight);
                addVertex(consumer, matrix4f, matrix3f,
                         x1 * zr1 * radius, y1 * zr1 * radius, z1 * radius,
                         x1 * zr1, y1 * zr1, z1, packedLight);
                addVertex(consumer, matrix4f, matrix3f,
                         x0 * zr1 * radius, y0 * zr1 * radius, z1 * radius,
                         x0 * zr1, y0 * zr1, z1, packedLight);
            }
        }
    }
    
    private void addVertex(VertexConsumer consumer, Matrix4f matrix4f, Matrix3f matrix3f,
                          float x, float y, float z, float nx, float ny, float nz, int packedLight) {
        consumer.vertex(matrix4f, x, y, z)
                .color(0, 0, 0, 255)  // Black color (R, G, B, A)
                .uv(0, 0)
                .overlayCoords(0)
                .uv2(packedLight)
                .normal(matrix3f, nx, ny, nz)
                .endVertex();
    }
    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return BLOCK_ATLAS;
    }
}
