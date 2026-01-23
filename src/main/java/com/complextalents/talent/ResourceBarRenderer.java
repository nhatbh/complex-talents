package com.complextalents.talent;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Interface for custom resource bar HUD rendering.
 * Definition talents can provide their own rendering logic for their resource bars.
 */
@OnlyIn(Dist.CLIENT)
public interface ResourceBarRenderer {
    /**
     * Render the resource bar on the HUD
     *
     * @param graphics The GuiGraphics context for rendering
     * @param x The X position to render at
     * @param y The Y position to render at
     * @param width The width of the bar
     * @param height The height of the bar
     * @param currentValue The current resource value
     * @param maxValue The maximum resource value
     * @param config The resource bar configuration
     * @param displayName The display name of the resource
     */
    void render(GuiGraphics graphics, int x, int y, int width, int height,
                float currentValue, float maxValue, ResourceBarConfig config, Component displayName);

    /**
     * Get the preferred height for this renderer
     * @return The preferred height in pixels
     */
    default int getPreferredHeight() {
        return 10;
    }

    /**
     * Get the preferred width for this renderer
     * @return The preferred width in pixels
     */
    default int getPreferredWidth() {
        return 100;
    }

    /**
     * Check if this renderer should show the numeric value
     * @return true if numeric value should be displayed
     */
    default boolean shouldShowNumericValue() {
        return true;
    }

    /**
     * Check if this renderer should show the resource name
     * @return true if resource name should be displayed
     */
    default boolean shouldShowResourceName() {
        return true;
    }
}
