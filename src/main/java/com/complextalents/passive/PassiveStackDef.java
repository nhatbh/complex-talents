package com.complextalents.passive;

import net.minecraft.network.chat.Component;

/**
 * Definition for a passive stack type.
 * The owner's event handlers handle all logic (generation, decay, triggers).
 * This only stores the definition for max stacks and optional display info.
 */
public class PassiveStackDef {

    private final String stackName;
    private final int maxStacks;
    private final Component displayName;
    private final int color;

    private PassiveStackDef(Builder builder) {
        this.stackName = builder.stackName;
        this.maxStacks = builder.maxStacks;
        this.displayName = builder.displayName;
        this.color = builder.color;
    }

    /**
     * Create a new builder for a passive stack definition.
     *
     * @param stackName The unique name for this stack type (e.g., "grace")
     * @return A new builder
     */
    public static Builder create(String stackName) {
        return new Builder(stackName);
    }

    public String getStackName() {
        return stackName;
    }

    public int getMaxStacks() {
        return maxStacks;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    /**
     * Builder for creating passive stack definitions.
     */
    public static class Builder {
        private final String stackName;
        private int maxStacks = 10;
        private Component displayName;
        private int color = 0xFFFFFFFF; // Default white

        private Builder(String stackName) {
            this.stackName = stackName;
            this.displayName = Component.literal(stackName);
        }

        /**
         * Set the maximum stacks for this type.
         *
         * @param max Maximum stacks (must be >= 1)
         * @return this builder
         */
        public Builder maxStacks(int max) {
            if (max < 1) {
                throw new IllegalArgumentException("Max stacks must be at least 1, got: " + max);
            }
            this.maxStacks = max;
            return this;
        }

        /**
         * Set the display name for this stack type (shown in HUD).
         *
         * @param name The display name
         * @return this builder
         */
        public Builder displayName(String name) {
            this.displayName = Component.literal(name);
            return this;
        }

        /**
         * Set the display name for this stack type (shown in HUD).
         *
         * @param name The display name component
         * @return this builder
         */
        public Builder displayName(Component name) {
            this.displayName = name;
            return this;
        }

        /**
         * Set the color for this stack type (for UI rendering).
         *
         * @param color ARGB color integer
         * @return this builder
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
         * Build the passive stack definition.
         *
         * @return The PassiveStackDef
         */
        public PassiveStackDef build() {
            return new PassiveStackDef(this);
        }
    }
}
