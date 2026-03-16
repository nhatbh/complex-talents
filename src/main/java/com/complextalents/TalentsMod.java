package com.complextalents;

import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.effects.ElementalEffects;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.elemental.integration.ModIntegrationHandler;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import com.complextalents.elemental.registry.ReactionRegistry;
import com.complextalents.impl.yygm.effect.YinYangEffects;
import com.complextalents.impl.yygm.entity.YygmEntities;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import com.complextalents.impl.darkmage.DarkMageRegistrar;
import com.complextalents.impl.elementalmage.origin.ElementalMageOrigin;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.impl.highpriest.entity.HighPriestEntities;
import com.complextalents.impl.highpriest.item.HighPriestItems;
import com.complextalents.item.ModItems;
import com.complextalents.item.ModCreativeTabs;
import com.complextalents.impl.highpriest.origin.HighPriestOrigin;
import com.complextalents.impl.assassin.effect.AssassinEffects;
import com.complextalents.impl.assassin.AssassinRegistrar;
import com.complextalents.network.PacketHandler;
import com.complextalents.origin.OriginRegistry;
import com.complextalents.origin.command.OriginCommand;
import com.complextalents.origin.integration.OriginModIntegrationHandler;
import com.complextalents.registry.SoundRegistry;
import com.complextalents.origin.integration.SpellCritAttributes;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.command.SkillCommand;
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

        // Register custom status effects
        ElementalEffects.register(modEventBus);
        OPEffects.register(modEventBus);
        HighPriestEffects.register(modEventBus);
        YinYangEffects.register(modEventBus);
        AssassinEffects.register(modEventBus);

        // Register spell crit attributes
        SpellCritAttributes.register(modEventBus);

        // Register custom entities
        ModEntities.register(modEventBus);
        HighPriestEntities.register(modEventBus);
        YygmEntities.register(modEventBus);

        // Register items
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        HighPriestItems.register(modEventBus);

        // Register network packets
        PacketHandler.register();

        // Register custom sounds
        SoundRegistry.register(modEventBus);

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

        // Initialize origin module mod integration
        OriginModIntegrationHandler.init();
        LOGGER.info("Origin mod integration initialized");

        // Initialize skill registry
        SkillRegistry.getInstance().initialize();
        LOGGER.info("Skill registry initialized");

        // Initialize origin registry
        OriginRegistry.getInstance().initialize();
        LOGGER.info("Origin registry initialized");

        // Register example origins
        HighPriestOrigin.register();
        HighPriestOrigin.initIntegration();
        YinYangGrandmasterOrigin.register();
        DarkMageRegistrar.register();
        AssassinRegistrar.register();
        ElementalMageOrigin.register();
        com.complextalents.impl.warrior.WarriorRegistrar.register();
        LOGGER.info("Example origins registered");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Complex Talents server starting");

        // Initialize reaction registry with all registered reactions
        ReactionRegistry.getInstance().initialize();
        OverwhelmingPowerRegistry.getInstance().initialize();
        LOGGER.info("Reaction and OP registries initialized");

        // Register skill commands
        SkillCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Skill commands registered");

        // Register origin commands
        OriginCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Origin commands registered");
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