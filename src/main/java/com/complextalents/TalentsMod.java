package com.complextalents;

import com.complextalents.api.TalentAPI;
import com.complextalents.capability.PlayerTalentsProvider;
import com.complextalents.client.ClientHandler;
import com.complextalents.command.GrantTalentCommand;
import com.complextalents.command.ListTalentsCommand;
import com.complextalents.command.RevokeTalentCommand;
import com.complextalents.command.SelectBranchCommand;
import com.complextalents.command.TalentCommand;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.config.TalentClientConfig;
import com.complextalents.config.TalentConfig;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.ElementalTalents;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.effects.ModEffects;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.elemental.integration.IronSpellbooksIntegration;
import com.complextalents.elemental.integration.ModIntegrationHandler;
import com.complextalents.network.PacketHandler;
import com.complextalents.talent.TalentRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(TalentsMod.MODID)
public class TalentsMod {
    public static final String MODID = "complextalents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TalentsMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register common setup
        modEventBus.addListener(this::commonSetup);

        // Register capability
        PlayerTalentsProvider.register(modEventBus);

        // Register mastery attributes
        MasteryAttributes.register(modEventBus);

        // Register custom status effects
        ModEffects.register(modEventBus);

        // Register custom entities
        ModEntities.register(modEventBus);

        // Register network packets
        PacketHandler.register();

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register configurations
        context.registerConfig(ModConfig.Type.COMMON, TalentConfig.SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ElementalReactionConfig.SPEC, "complextalents-reactions.toml");
        context.registerConfig(ModConfig.Type.CLIENT, TalentClientConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Complex Talents mod initializing...");

        // Register elemental talents
        ElementalTalents.register(null);
        LOGGER.info("Elemental talents registered");

        LOGGER.info("Talent commands will be registered on server start");

        // Initialize mod integration (Iron's Spellbooks only - addons use main mod)
        ModIntegrationHandler.init();
        LOGGER.info("Mod integration initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Complex Talents server starting");

        // Register commands
        TalentCommand.register(event.getServer().getCommands().getDispatcher());
        GrantTalentCommand.register(event.getServer().getCommands().getDispatcher());
        RevokeTalentCommand.register(event.getServer().getCommands().getDispatcher());
        ListTalentsCommand.register(event.getServer().getCommands().getDispatcher());
        SelectBranchCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Talent commands registered");

        // Initialize elemental stack manager on server start
        ElementalStackManager.init();
        LOGGER.info("Elemental stack manager initialized");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientHandler.init();
            LOGGER.info("Complex Talents client setup complete");
        }
    }
}
