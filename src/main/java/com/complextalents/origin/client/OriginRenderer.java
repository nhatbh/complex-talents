package com.complextalents.origin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Functional interface for custom origin HUD rendering.
 * Origins can provide their own renderer to display resources and passive stacks
 * in a completely custom way.
 * <p>
 * If an origin returns null from getRenderer(), the default HUD is used.
 * </p>
 */
@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface OriginRenderer {

    /**
     * Render this origin's HUD elements.
     * Called every frame on the client after vanilla overlays are rendered.
     *
     * @param graphics The GUI graphics context for rendering
     * @param screenWidth The screen width in pixels
     * @param screenHeight The screen height in pixels
     */
    void renderHUD(GuiGraphics graphics, int screenWidth, int screenHeight);
}
