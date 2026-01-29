package com.complextalents.targeting;

import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Set;

/**
 * Client-side only request for targeting resolution.
 * Created by a skill to describe what it wants from the targeting system.
 *
 * <p>This class is NEVER sent to the server. It is used only on the client
 * to configure the targeting resolver before producing a TargetingSnapshot.</p>
 *
 * <p>Use {@link #builder()} to construct instances.</p>
 */
public class TargetingRequest {
    private final Player player;
    private final double maxRange;
    private final EnumSet<TargetType> allowedTypes;
    private final TargetRelation relationFilter;
    private final boolean requireLineOfSight;
    private final double entitySearchRadius;
    private final boolean targetSelfAllowed;
    private final boolean fallbackToSelf;

    private TargetingRequest(Builder builder) {
        this.player = builder.player;
        this.maxRange = builder.maxRange;
        this.allowedTypes = EnumSet.copyOf(builder.allowedTypes);
        this.relationFilter = builder.relationFilter;
        this.requireLineOfSight = builder.requireLineOfSight;
        this.entitySearchRadius = builder.entitySearchRadius;
        this.targetSelfAllowed = builder.targetSelfAllowed;
        this.fallbackToSelf = builder.fallbackToSelf;
    }

    /**
     * Create a new builder for constructing TargetingRequest instances.
     *
     * @param player The player initiating the targeting request
     * @return A new Builder instance
     */
    public static Builder builder(Player player) {
        return new Builder(player);
    }

    // Getters

    /**
     * @return The player initiating this targeting request
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return Maximum range for targeting in blocks
     */
    public double getMaxRange() {
        return maxRange;
    }

    /**
     * @return Set of allowed target types (ENTITY, POSITION, DIRECTION)
     */
    public Set<TargetType> getAllowedTypes() {
        return EnumSet.copyOf(allowedTypes);
    }

    /**
     * @return The relationship filter for targeting (ANY, ALLY, ENEMY)
     */
    public TargetRelation getRelationFilter() {
        return relationFilter;
    }

    /**
     * @return Whether line of sight is required for entity targeting
     */
    public boolean isRequireLineOfSight() {
        return requireLineOfSight;
    }

    /**
     * @return Radius around the aim point to search for entities (in blocks)
     */
    public double getEntitySearchRadius() {
        return entitySearchRadius;
    }

    /**
     * @return Whether the player can target themselves
     */
    public boolean isTargetSelfAllowed() {
        return targetSelfAllowed;
    }

    /**
     * @return Whether entity targeting should fall back to self when no valid target is found
     */
    public boolean isFallbackToSelf() {
        return fallbackToSelf;
    }

    /**
     * Builder for constructing TargetingRequest instances.
     */
    public static class Builder {
        private final Player player;
        private double maxRange = 32.0;
        private final EnumSet<TargetType> allowedTypes = EnumSet.allOf(TargetType.class);
        private TargetRelation relationFilter = TargetRelation.ANY;
        private boolean requireLineOfSight = true;
        private double entitySearchRadius = 2.0;
        private boolean targetSelfAllowed = false;
        private boolean fallbackToSelf = true;

        private Builder(Player player) {
            this.player = player;
        }

        /**
         * Set the maximum range for targeting.
         *
         * @param range Maximum range in blocks
         * @return this builder
         */
        public Builder maxRange(double range) {
            this.maxRange = range;
            return this;
        }

        /**
         * Set the allowed target types. Use {@code EnumSet.of(...)} to specify.
         *
         * @param types Set of allowed TargetType values
         * @return this builder
         */
        public Builder allowedTypes(EnumSet<TargetType> types) {
            this.allowedTypes.clear();
            this.allowedTypes.addAll(types);
            return this;
        }

        /**
         * Set the allowed target types using varargs.
         *
         * @param types TargetType values to allow
         * @return this builder
         */
        public Builder allowedTypes(TargetType... types) {
            this.allowedTypes.clear();
            for (TargetType type : types) {
                this.allowedTypes.add(type);
            }
            return this;
        }

        /**
         * Set the relationship filter for targeting.
         *
         * @param filter The TargetRelation filter (ANY, ALLY, ENEMY)
         * @return this builder
         */
        public Builder relationFilter(TargetRelation filter) {
            this.relationFilter = filter;
            return this;
        }

        /**
         * Set whether line of sight is required for entity targeting.
         *
         * @param require true if line of sight is required
         * @return this builder
         */
        public Builder requireLineOfSight(boolean require) {
            this.requireLineOfSight = require;
            return this;
        }

        /**
         * Set the search radius for finding entities near the aim point.
         * A larger radius makes it easier to target entities.
         *
         * @param radius Search radius in blocks
         * @return this builder
         */
        public Builder entitySearchRadius(double radius) {
            this.entitySearchRadius = radius;
            return this;
        }

        /**
         * Set whether the player can target themselves.
         *
         * @param allow true if self-targeting is allowed
         * @return this builder
         */
        public Builder allowTargetSelf(boolean allow) {
            this.targetSelfAllowed = allow;
            return this;
        }

        /**
         * Set whether entity targeting should fall back to self when no valid target is found.
         * When true and no entity is hit, the targeting will fall back to the player.
         * When false and no entity is hit, the snapshot will have no entity.
         *
         * @param fallback true if fallback to self is enabled
         * @return this builder
         */
        public Builder fallbackToSelf(boolean fallback) {
            this.fallbackToSelf = fallback;
            return this;
        }

        /**
         * Build the TargetingRequest instance.
         *
         * @return A new TargetingRequest with the configured parameters
         */
        public TargetingRequest build() {
            return new TargetingRequest(this);
        }
    }
}
