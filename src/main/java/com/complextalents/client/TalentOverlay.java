package com.complextalents.client;

import com.complextalents.TalentsMod;
import com.complextalents.config.TalentClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class TalentOverlay {
    // Cache expensive operations - update every 100ms instead of every frame
    private static int cachedMaxTalents = 5;
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL_MS = 100;

    // Disabled HUD overlay rendering
    // @SubscribeEvent
    // public static void renderOverlay(RenderGuiOverlayEvent.Post event) {
    //     if (!TalentClientConfig.showTalentOverlay.get()) return;

    //     Minecraft minecraft = Minecraft.getInstance();
    //     Player player = minecraft.player;
    //     if (player == null) return;

    //     // Update cached data periodically (not every frame)
    //     long currentTime = System.currentTimeMillis();
    //     if (currentTime - lastCacheUpdate > CACHE_UPDATE_INTERVAL_MS) {
    //         // Cache expensive config reads and capability lookups here
    //         cachedMaxTalents = TalentClientConfig.overlayScale.get();
    //         // TODO: When implementing real talent display, cache capability data here:
    //         // player.getCapability(TALENTS).ifPresent(talents -> cachedTalentData = talents.getUnlockedTalents());
    //         lastCacheUpdate = currentTime;
    //     }

    //     // Render every frame using cached data (this is fast and smooth)
    //     GuiGraphics guiGraphics = event.getGuiGraphics();
    //     renderTalentOverlay(guiGraphics, minecraft, player);
    // }

    private static void renderTalentOverlay(GuiGraphics guiGraphics, Minecraft minecraft, Player player) {
        int x = 10;
        int y = 10;

        // Render title
        guiGraphics.drawString(minecraft.font, "§6Complex Talents", x, y, 0xFFFFFF);
        y += 12;

        // Render placeholder talents using cached data
        for (int i = 0; i < cachedMaxTalents; i++) {
            // Draw talent box
            guiGraphics.fill(x, y, x + 16, y + 16, 0xFF404040);

            // Draw talent number
            String text = String.valueOf(i + 1);
            guiGraphics.drawString(minecraft.font, text, x + 4, y + 4, 0xFFFFFF);

            y += 20;
        }
    }
}
