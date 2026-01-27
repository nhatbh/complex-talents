package com.complextalents.client.renderer;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.client.renderers.entities.NatureCoreRenderer;
import com.complextalents.elemental.entity.ModEntities;
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
        event.registerEntityRenderer(ModEntities.NATURE_CORE.get(), NatureCoreRenderer::new);
        TalentsMod.LOGGER.info("Registering entity renderers for elemental entities");
    }
}