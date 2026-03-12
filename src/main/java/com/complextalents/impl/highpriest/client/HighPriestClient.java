package com.complextalents.impl.highpriest.client;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.client.renderer.DivinePunisherRenderer;
import com.complextalents.impl.highpriest.client.renderer.SeraphsEdgeRenderer;
import com.complextalents.impl.highpriest.entity.HighPriestEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side registration for High Priest origin.
 * Registers entity renderers and other client-only components.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class HighPriestClient {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                HighPriestEntities.DIVINE_PUNISHER.get(),
                DivinePunisherRenderer::new);
        event.registerEntityRenderer(
                HighPriestEntities.SERAPHS_EDGE.get(),
                SeraphsEdgeRenderer::new);
        TalentsMod.LOGGER.info("Registered High Priest entity renderers");
    }

    /**
     * Client-side event handler for game tick events.
     */
    @Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
    }
}
