package com.complextalents.impl.yygm.client;

import com.complextalents.network.yygm.SwordDanceDashPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side renderer for Sword Dance visual effects.
 * Processes tick-based dash rendering for particle trails.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class SwordDanceRenderer {

    /**
     * Client tick handler for dash particle rendering.
     * Delegates to the packet's static tick method.
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        SwordDanceDashPacket.clientTick();
    }
}
