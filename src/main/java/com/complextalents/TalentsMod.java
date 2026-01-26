package com.complextalents;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.elemental.integration.ModIntegrationHandler;
import com.complextalents.elemental.registry.ReactionRegistry;
import com.complextalents.network.PacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TalentsMod.MODID)
public class TalentsMod {
    public static final String MODID = "complextalents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TalentsMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register common setup
        modEventBus.addListener(this::commonSetup);

        // Register mastery attributes
        MasteryAttributes.register(modEventBus);

        // Register custom status effects
        ElementalEffects.register(modEventBus);

        // Register custom entities
        ModEntities.register(modEventBus);

        // Register network packets
        PacketHandler.register();

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register configurations
        context.registerConfig(ModConfig.Type.COMMON, ElementalReactionConfig.SPEC, "complextalents-reactions.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Complex Talents mod initializing...");

        // Initialize mod integration (Iron's Spellbooks only - addons use main mod)
        ModIntegrationHandler.init();
        LOGGER.info("Mod integration initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Complex Talents server starting");

        // Initialize reaction registry with all registered reactions
        ReactionRegistry.getInstance().initialize();
        LOGGER.info("Reaction registry initialized");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Client setup can be added here if needed
            LOGGER.info("Complex Talents client setup complete");
        }
    }
}