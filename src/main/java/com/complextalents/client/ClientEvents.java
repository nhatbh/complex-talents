package com.complextalents.client;

import com.complextalents.TalentsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // Register key bindings first
        KeyBindings.register();

        // Then register them with Forge
        for (var keyMapping : KeyBindings.getKeyBindings()) {
            event.register(keyMapping);
        }

        TalentsMod.LOGGER.info("Registered {} key mappings", KeyBindings.getKeyBindings().size());
    }

    @Mod.EventBusSubscriber(modid = TalentsMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeClientEvents {

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                return;
            }

            // Check all keybindings
            while (KeyBindings.ACTIVATE_TALENT_1.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.ACTIVATE_TALENT_1);
            }
            while (KeyBindings.ACTIVATE_TALENT_2.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.ACTIVATE_TALENT_2);
            }
            while (KeyBindings.ACTIVATE_TALENT_3.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.ACTIVATE_TALENT_3);
            }
            while (KeyBindings.ACTIVATE_TALENT_4.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.ACTIVATE_TALENT_4);
            }
            while (KeyBindings.ACTIVATE_TALENT_5.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.ACTIVATE_TALENT_5);
            }
            while (KeyBindings.OPEN_TALENT_SCREEN.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.OPEN_TALENT_SCREEN);
            }
            while (KeyBindings.TOGGLE_COMBAT_MODE.consumeClick()) {
                KeyBindings.onKeyPressed(KeyBindings.TOGGLE_COMBAT_MODE);
            }
        }
    }
}
