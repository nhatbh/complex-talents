package com.complextalents.talent;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

/**
 * Configuration for a resource bar defined by a Definition talent
 */
public class ResourceBarConfig {
    private final ResourceBarType type;
    private final Component displayName;
    private final float maxValue;
    private final float startingValue;
    private final float regenRate; // Per second
    private final int color;
    private final boolean showInUI;
    private final Supplier<ResourceBarRenderer> rendererSupplier;

    private ResourceBarConfig(Builder builder) {
        this.type = builder.type;
        this.displayName = builder.displayName;
        this.maxValue = builder.maxValue;
        this.startingValue = builder.startingValue;
        this.regenRate = builder.regenRate;
        this.color = builder.color;
        this.showInUI = builder.showInUI;
        this.rendererSupplier = builder.rendererSupplier;
    }

    public ResourceBarType getType() {
        return type;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public float getStartingValue() {
        return startingValue;
    }

    public float getRegenRate() {
        return regenRate;
    }

    public int getColor() {
        return color;
    }

    public boolean shouldShowInUI() {
        return showInUI;
    }

    /**
     * Get the custom renderer for this resource bar
     * @return The renderer supplier, or null to use the default renderer
     */
    @OnlyIn(Dist.CLIENT)
    public Supplier<ResourceBarRenderer> getRendererSupplier() {
        return rendererSupplier;
    }

    /**
     * Check if this config has a custom renderer
     * @return true if a custom renderer is provided
     */
    public boolean hasCustomRenderer() {
        return rendererSupplier != null;
    }

    public static Builder builder(ResourceBarType type, Component displayName) {
        return new Builder(type, displayName);
    }

    public static class Builder {
        private final ResourceBarType type;
        private final Component displayName;
        private float maxValue = 100.0f;
        private float startingValue = 0.0f;
        private float regenRate;
        private int color;
        private boolean showInUI = true;
        private Supplier<ResourceBarRenderer> rendererSupplier = null;

        private Builder(ResourceBarType type, Component displayName) {
            this.type = type;
            this.displayName = displayName;
            this.regenRate = type.getDefaultRegenRate();
            this.color = type.getDefaultColor();
        }

        /**
         * Set the maximum value of the resource bar
         */
        public Builder maxValue(float maxValue) {
            this.maxValue = maxValue;
            return this;
        }

        /**
         * Set the starting value when the talent is equipped
         */
        public Builder startingValue(float startingValue) {
            this.startingValue = startingValue;
            return this;
        }

        /**
         * Set the regeneration rate (positive = regen, negative = decay) in units per second
         */
        public Builder regenRate(float regenRate) {
            this.regenRate = regenRate;
            return this;
        }

        /**
         * Set the color of the resource bar (RGB hex)
         */
        public Builder color(int color) {
            this.color = color;
            return this;
        }

        /**
         * Set whether this resource bar should be shown in the UI
         */
        public Builder showInUI(boolean showInUI) {
            this.showInUI = showInUI;
            return this;
        }

        /**
         * Set a custom renderer for the resource bar HUD
         * The supplier allows lazy initialization on the client side
         * @param rendererSupplier A supplier that creates the renderer instance (client-side only)
         */
        @OnlyIn(Dist.CLIENT)
        public Builder renderer(Supplier<ResourceBarRenderer> rendererSupplier) {
            this.rendererSupplier = rendererSupplier;
            return this;
        }

        public ResourceBarConfig build() {
            return new ResourceBarConfig(this);
        }
    }
}
