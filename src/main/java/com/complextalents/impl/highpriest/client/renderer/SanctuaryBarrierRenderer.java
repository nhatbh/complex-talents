package com.complextalents.impl.highpriest.client.renderer;

import com.complextalents.impl.highpriest.entity.SanctuaryBarrierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Hex-grid sphere renderer for Sanctuary Barrier.
 * Renders a Geodesic Polyhedron with filled faces and glowing edges.
 */
public class SanctuaryBarrierRenderer extends EntityRenderer<SanctuaryBarrierEntity> {

    private static final float EDGE_THICKNESS = 0.08f;
    private static final int SUBDIVISION_LEVEL = 2;

    // Use a blank white texture so color modulation works purely via vertex color
    private static final ResourceLocation BLANK_TEXTURE = new ResourceLocation("textures/misc/white.png");

    // Cached mesh data
    private static final Map<Integer, List<Tile>> CACHED_TILES = new HashMap<>();

    public SanctuaryBarrierRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0;
    }

    @Override
    public void render(
            SanctuaryBarrierEntity entity,
            float yaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        float radius = Math.max(entity.getRadius(), 4f);
        // Calculate HP percentage for color shifting
        float hpPct = entity.getHp() / entity.getMaxHp();
        hpPct = Mth.clamp(hpPct, 0f, 1f);

        poseStack.pushPose();

        // Use entity_translucent for alpha blending.
        // Point to a valid white texture.
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucentEmissive(BLANK_TEXTURE));

        // Always render at full brightness (ignoring world light)
        int overlayLight = 15728880;

        List<Tile> tiles = getCachedTiles(SUBDIVISION_LEVEL);
        renderHexGrid(poseStack.last().pose(), vc, radius, hpPct, overlayLight, tiles);

        poseStack.popPose();
    }

    private static List<Tile> getCachedTiles(int subdivision) {
        synchronized (CACHED_TILES) {
            return CACHED_TILES.computeIfAbsent(subdivision, SanctuaryBarrierRenderer::generateHexTiles);
        }
    }

    private void renderHexGrid(
            Matrix4f pose,
            VertexConsumer vc,
            float radius,
            float hpPct,
            int light,
            List<Tile> tiles
    ) {
        // Color Logic: Shift from Blue/Green to Red as HP drops
        float r = Mth.lerp(1.0f - hpPct, 0.0f, 1.0f);
        float g = Mth.lerp(hpPct, 0.2f, 0.8f);
        float b = Mth.lerp(hpPct, 1.0f, 0.2f);

        // Opacity settings
        float edgeAlpha = 0.9f; // Bright, solid edges
        float fillAlpha = 0.25f; // Faint, transparent fill

        for (Tile tile : tiles) {
            // --- Pass 1: Fill the hexagon face ---
            // We draw faces first so edges appear cleanly on top if there's slight depth overlap
            fillTile(vc, pose, tile, radius, r, g, b, fillAlpha, light);

            // --- Pass 2: Draw the thick edges ---
            int n = tile.corners.size();
            for (int i = 0; i < n; i++) {
                Vector3f aCorner = tile.corners.get(i);
                Vector3f bCorner = tile.corners.get((i + 1) % n); // Loop back to start

                drawThickEdge(vc, pose,
                        aCorner.x * radius, aCorner.y * radius, aCorner.z * radius,
                        bCorner.x * radius, bCorner.y * radius, bCorner.z * radius,
                        r, g, b, edgeAlpha, light);
            }
        }
    }

    /**
     * Fills a tile by drawing a triangle fan using degenerate quads.
     * RenderType.entityTranslucent expects QUADS, so we must send 4 vertices.
     * We send (Center, Corner A, Corner B, Corner B) to mimic a triangle.
     */
    private void fillTile(
            VertexConsumer vc,
            Matrix4f pose,
            Tile tile,
            float radius,
            float r, float g, float b, float a,
            int light
    ) {
        Vector3f center = tile.center;
        int n = tile.corners.size();

        for (int i = 0; i < n; i++) {
            Vector3f c1 = tile.corners.get(i);
            Vector3f c2 = tile.corners.get((i + 1) % n);

            // Vertex 1: Center
            drawVertex(vc, pose,
                    center.x * radius, center.y * radius, center.z * radius,
                    center.x, center.y, center.z,
                    r, g, b, a, light);

            // Vertex 2: Corner 1
            drawVertex(vc, pose,
                    c1.x * radius, c1.y * radius, c1.z * radius,
                    c1.x, c1.y, c1.z,
                    r, g, b, a, light);

            // Vertex 3: Corner 2
            drawVertex(vc, pose,
                    c2.x * radius, c2.y * radius, c2.z * radius,
                    c2.x, c2.y, c2.z,
                    r, g, b, a, light);

            // Vertex 4: Corner 2 (Repeated)
            // This closes the Quad at the same point as Vertex 3, effectively making a Triangle.
            drawVertex(vc, pose,
                    c2.x * radius, c2.y * radius, c2.z * radius,
                    c2.x, c2.y, c2.z,
                    r, g, b, a, light);
        }
    }

    private void drawThickEdge(
            VertexConsumer vc,
            Matrix4f pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a,
            int light
    ) {
        float halfThick = EDGE_THICKNESS * 0.5f;

        // Vector from p1 to p2
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;

        // Outward normal vector (average position of the edge)
        float mx = (x1 + x2) * 0.5f;
        float my = (y1 + y2) * 0.5f;
        float mz = (z1 + z2) * 0.5f;
        float lenM = Mth.sqrt(mx * mx + my * my + mz * mz);
        if (lenM < 0.0001f) return;
        float nx = mx / lenM;
        float ny = my / lenM;
        float nz = mz / lenM;

        // Calculate "Side" vector to expand thickness along the surface
        // side = direction x normal
        float sx = dy * nz - dz * ny;
        float sy = dz * nx - dx * nz;
        float sz = dx * ny - dy * nx;
        float sLen = Mth.sqrt(sx * sx + sy * sy + sz * sz);

        if (sLen < 0.0001f) return;
        sx = (sx / sLen) * halfThick;
        sy = (sy / sLen) * halfThick;
        sz = (sz / sLen) * halfThick;

        // Draw Quad ribbon. We use the outward normal for lighting.
        drawVertex(vc, pose, x1 - sx, y1 - sy, z1 - sz, nx, ny, nz, r, g, b, a, light);
        drawVertex(vc, pose, x1 + sx, y1 + sy, z1 + sz, nx, ny, nz, r, g, b, a, light);
        drawVertex(vc, pose, x2 + sx, y2 + sy, z2 + sz, nx, ny, nz, r, g, b, a, light);
        drawVertex(vc, pose, x2 - sx, y2 - sy, z2 - sz, nx, ny, nz, r, g, b, a, light);
    }

    private void drawVertex(
            VertexConsumer vc,
            Matrix4f pose,
            float x, float y, float z,
            float nx, float ny, float nz, // Add normals
            float r, float g, float b, float a,
            int light
    ) {
        vc.vertex(pose, x, y, z)
                .color(r, g, b, a)
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                // It's important to provide normals so the lighting engine knows
                // which way the face is pointing, even if fully bright.
                .normal(nx, ny, nz)
                .endVertex();
    }

    // ==========================================
    //      MESH GENERATION (Identical to previous version)
    // ==========================================

    private static List<Tile> generateHexTiles(int subdivision) {
        List<Vector3f> vertices = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();

        createIcosahedron(vertices, faces);

        for (int i = 0; i < subdivision; i++) {
            subdivide(vertices, faces);
        }

        List<List<Integer>> vertexToFaces = new ArrayList<>(vertices.size());
        for (int i = 0; i < vertices.size(); i++) {
            vertexToFaces.add(new ArrayList<>());
        }

        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            vertexToFaces.get(f[0]).add(i);
            vertexToFaces.get(f[1]).add(i);
            vertexToFaces.get(f[2]).add(i);
        }

        List<Vector3f> faceCentroids = new ArrayList<>(faces.size());
        for (int[] f : faces) {
            faceCentroids.add(getCentroid(vertices.get(f[0]), vertices.get(f[1]), vertices.get(f[2])));
        }

        List<Tile> tiles = new ArrayList<>();
        for (int vIdx = 0; vIdx < vertices.size(); vIdx++) {
            Vector3f center = vertices.get(vIdx);
            List<Integer> adjacentFaceIndices = vertexToFaces.get(vIdx);

            if (adjacentFaceIndices.size() < 3) continue;

            List<Vector3f> corners = new ArrayList<>();
            for (int fIdx : adjacentFaceIndices) {
                corners.add(faceCentroids.get(fIdx));
            }

            sortCorners(corners, center);

            Tile tile = new Tile();
            tile.center = center;
            tile.corners = corners;
            tiles.add(tile);
        }

        return tiles;
    }

    private static Vector3f getCentroid(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f sum = new Vector3f(a).add(b).add(c);
        return sum.normalize();
    }

    private static void sortCorners(List<Vector3f> corners, Vector3f centerNormal) {
        Vector3f up = Math.abs(centerNormal.y) < 0.99f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f tangent = new Vector3f(up).cross(centerNormal).normalize();
        Vector3f bitangent = new Vector3f(centerNormal).cross(tangent).normalize();

        corners.sort((c1, c2) -> {
            float dx1 = c1.x - centerNormal.x;
            float dy1 = c1.y - centerNormal.y;
            float dz1 = c1.z - centerNormal.z;

            float dx2 = c2.x - centerNormal.x;
            float dy2 = c2.y - centerNormal.y;
            float dz2 = c2.z - centerNormal.z;

            double x1 = dx1 * tangent.x + dy1 * tangent.y + dz1 * tangent.z;
            double y1 = dx1 * bitangent.x + dy1 * bitangent.y + dz1 * bitangent.z;

            double x2 = dx2 * tangent.x + dy2 * tangent.y + dz2 * tangent.z;
            double y2 = dx2 * bitangent.x + dy2 * bitangent.y + dz2 * bitangent.z;

            return Double.compare(Math.atan2(y1, x1), Math.atan2(y2, x2));
        });
    }

    private static void createIcosahedron(List<Vector3f> vertices, List<int[]> faces) {
        float t = (float) ((1.0 + Math.sqrt(5.0)) / 2.0);
        vertices.add(new Vector3f(-1, t, 0).normalize());
        vertices.add(new Vector3f(1, t, 0).normalize());
        vertices.add(new Vector3f(-1, -t, 0).normalize());
        vertices.add(new Vector3f(1, -t, 0).normalize());
        vertices.add(new Vector3f(0, -1, t).normalize());
        vertices.add(new Vector3f(0, 1, t).normalize());
        vertices.add(new Vector3f(0, -1, -t).normalize());
        vertices.add(new Vector3f(0, 1, -t).normalize());
        vertices.add(new Vector3f(t, 0, -1).normalize());
        vertices.add(new Vector3f(t, 0, 1).normalize());
        vertices.add(new Vector3f(-t, 0, -1).normalize());
        vertices.add(new Vector3f(-t, 0, 1).normalize());

        int[][] f = {
                {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
                {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}
        };
        for (int[] face : f) faces.add(face);
    }

    private static void subdivide(List<Vector3f> vertices, List<int[]> faces) {
        Map<Long, Integer> midPointCache = new HashMap<>();
        List<int[]> newFaces = new ArrayList<>();
        for (int[] face : faces) {
            int a = face[0]; int b = face[1]; int c = face[2];
            int ab = getMidpoint(vertices, midPointCache, a, b);
            int bc = getMidpoint(vertices, midPointCache, b, c);
            int ca = getMidpoint(vertices, midPointCache, c, a);
            newFaces.add(new int[]{a, ab, ca});
            newFaces.add(new int[]{b, bc, ab});
            newFaces.add(new int[]{c, ca, bc});
            newFaces.add(new int[]{ab, bc, ca});
        }
        faces.clear();
        faces.addAll(newFaces);
    }

    private static int getMidpoint(List<Vector3f> vertices, Map<Long, Integer> cache, int a, int b) {
        long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
        return cache.computeIfAbsent(key, k -> {
            Vector3f p1 = vertices.get(a);
            Vector3f p2 = vertices.get(b);
            Vector3f mid = new Vector3f(p1).add(p2).mul(0.5f).normalize();
            vertices.add(mid);
            return vertices.size() - 1;
        });
    }

    private static class Tile {
        Vector3f center;
        List<Vector3f> corners;
    }

    @Override
    public ResourceLocation getTextureLocation(SanctuaryBarrierEntity entity) {
        return BLANK_TEXTURE;
    }
}