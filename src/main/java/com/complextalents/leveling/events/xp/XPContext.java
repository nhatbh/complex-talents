package com.complextalents.leveling.events.xp;

import net.minecraft.world.level.ChunkPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context object containing metadata about XP awards.
 * Uses builder pattern for flexible construction with optional metadata.
 */
public class XPContext {
    private final XPSource source;
    private final ChunkPos chunkPos;
    private final double rawAmount;
    private final Map<String, Object> metadata;

    private XPContext(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "XPSource cannot be null");
        this.chunkPos = Objects.requireNonNull(builder.chunkPos, "ChunkPos cannot be null");
        this.rawAmount = builder.rawAmount;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public XPSource getSource() {
        return source;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public double getRawAmount() {
        return rawAmount;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public Object getMetadata(String key, Object defaultValue) {
        return metadata.getOrDefault(key, defaultValue);
    }

    @Override
    public String toString() {
        return String.format("XPContext{source=%s, amount=%.1f, chunk=%d,%d, metadata=%s}",
                source.getDisplayName(), rawAmount, chunkPos.x, chunkPos.z, metadata.keySet());
    }

    /**
     * Builder for XPContext with fluent API.
     */
    public static class Builder {
        private XPSource source;
        private ChunkPos chunkPos;
        private double rawAmount;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder source(XPSource source) {
            this.source = source;
            return this;
        }

        public Builder chunkPos(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
            return this;
        }

        public Builder rawAmount(double rawAmount) {
            this.rawAmount = rawAmount;
            return this;
        }

        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata.putAll(metadata);
            return this;
        }

        public XPContext build() {
            return new XPContext(this);
        }
    }
}
