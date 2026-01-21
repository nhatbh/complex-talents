package com.complextalents.client;

import com.complextalents.TalentsMod;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientHandler {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        TalentsMod.LOGGER.info("Client handler initialized");
    }

    public static void registerKeyBindings(IEventBus modEventBus) {
        // Key bindings are registered here
    }
}
