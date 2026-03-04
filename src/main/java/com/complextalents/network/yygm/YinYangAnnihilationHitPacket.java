package com.complextalents.network.yygm;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Network packet for triggering Yin Yang Annihilation hit effect on client.
 * <p>
 * Sent every time the Annihilation target is hit.
 * Triggers expanding ring animation that stacks with other hits.
 * </p>
 */
public class YinYangAnnihilationHitPacket {
    private final int entityId;
    private final long hitTick;

    public YinYangAnnihilationHitPacket(int entityId, long hitTick) {
        this.entityId = entityId;
        this.hitTick = hitTick;
    }

    // Decode constructor
    public YinYangAnnihilationHitPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.hitTick = buffer.readLong();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeLong(hitTick);
    }

    public static YinYangAnnihilationHitPacket decode(FriendlyByteBuf buffer) {
        return new YinYangAnnihilationHitPacket(buffer);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleClient());
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        // Add new hit animation for this entity (stacks with existing animations)
        ClientHitData.addHit(entityId, hitTick);
    }

    // Getters
    public int getEntityId() {
        return entityId;
    }

    public long getHitTick() {
        return hitTick;
    }

    /**
     * Client-side storage for active hit animations.
     * Supports multiple simultaneous animations per entity (stacking).
     */
    public static class ClientHitData {
        private static final java.util.Map<Integer, java.util.List<HitAnimationData>> ACTIVE_HITS = new java.util.HashMap<>();

        public static void addHit(int entityId, long hitTick) {
            ACTIVE_HITS.computeIfAbsent(entityId, k -> new java.util.ArrayList<>())
                .add(new HitAnimationData(hitTick));
        }

        public static java.util.List<HitAnimationData> getHits(int entityId) {
            java.util.List<HitAnimationData> hits = ACTIVE_HITS.get(entityId);
            return hits != null ? hits : java.util.Collections.emptyList();
        }

        /**
         * Clean up finished animations.
         * Should be called every render tick.
         */
        public static void cleanupFinished(long currentTime) {
            ACTIVE_HITS.entrySet().removeIf(entityEntry -> {
                entityEntry.getValue().removeIf(hitData -> hitData.shouldRemove(currentTime));
                return entityEntry.getValue().isEmpty();
            });
        }

        /**
         * Get all entities with active hits for iteration.
         */
        public static java.util.Set<Integer> getEntitiesWithHits() {
            return ACTIVE_HITS.keySet();
        }
    }

    /**
     * Data class for a single hit animation state.
     * Each hit creates its own animation that plays independently.
     */
    public static class HitAnimationData {
        private final long hitTick;
        private static final float MAX_SCALE = 6.0f;  // 6-block radius (wider)
        private static final int DURATION_TICKS = 10;  // 0.5 seconds (faster)

        public HitAnimationData(long hitTick) {
            this.hitTick = hitTick;
        }

        public long getHitTick() {
            return hitTick;
        }

        /**
         * Get the current scale of the expanding circle.
         * Grows from 0 to 6 blocks over 0.5 seconds with fast easing.
         */
        public float getScale(long currentTime) {
            long elapsed = currentTime - hitTick;
            if (elapsed >= DURATION_TICKS) return MAX_SCALE;
            // Ease-out cubic: 1 - (1-t)^3 for smooth deceleration
            float progress = elapsed / (float) DURATION_TICKS;
            float easedProgress = 1.0f - (float) Math.pow(1.0f - progress, 3.0f);
            return easedProgress * MAX_SCALE;
        }

        /**
         * Get the current alpha (opacity) of the expanding circle.
         * Fades from 0.8 to 0.0 over 0.5 seconds.
         */
        public float getAlpha(long currentTime) {
            long elapsed = currentTime - hitTick;
            if (elapsed >= DURATION_TICKS) return 0.0f;
            // Linear fade
            float progress = elapsed / (float) DURATION_TICKS;
            return 0.8f * (1.0f - progress);
        }

        /**
         * Get the current rotation angle.
         * Slow rotation for visual effect.
         */
        public float getRotation(long currentTime) {
            long elapsed = currentTime - hitTick;
            return elapsed * 2.0f;  // 2 degrees per tick
        }

        /**
         * Check if this animation data should be removed.
         * Returns true after 0.5 seconds (animation complete).
         */
        public boolean shouldRemove(long currentTime) {
            long elapsed = currentTime - hitTick;
            return elapsed > DURATION_TICKS;
        }
    }
}
