package com.complextalents.client.renderer;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.client.renderers.entities.BlackHoleRenderer;
import com.complextalents.elemental.client.renderers.entities.NatureCoreRenderer;
import com.complextalents.elemental.client.renderers.entities.SpringPotionRenderer;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.impl.highpriest.client.renderer.DivinePunisherRenderer;
import com.complextalents.impl.highpriest.client.renderer.SeraphsEdgeRenderer;
import com.complextalents.impl.highpriest.entity.HighPriestEntities;
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
        // Elemental entities
        event.registerEntityRenderer(ModEntities.NATURE_CORE.get(), NatureCoreRenderer::new);
        event.registerEntityRenderer(ModEntities.SPRING_POTION.get(), SpringPotionRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);

        // High Priest entities
        event.registerEntityRenderer(HighPriestEntities.DIVINE_PUNISHER.get(), DivinePunisherRenderer::new);
        event.registerEntityRenderer(HighPriestEntities.SERAPHS_EDGE.get(), SeraphsEdgeRenderer::new);

        TalentsMod.LOGGER.info("Registering entity renderers");
    }
}
