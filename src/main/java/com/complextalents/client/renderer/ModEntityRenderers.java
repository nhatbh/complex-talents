package com.complextalents.client.renderer;

import com.complextalents.TalentsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Registers entity renderers for custom entities.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        TalentsMod.LOGGER.info("Registering entity renderers for elemental entities");
    }
}