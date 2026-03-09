package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Selects the highest health target from multiple hits in a single tick/instance.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class OPTargetSelector {
    private static final Map<UUID, Map<OPElementType, List<OPContext>>> tickBuffers = new HashMap<>();

    public static void buffer(OPContext context) {
        UUID attackerId = context.getAttacker().getUUID();
        tickBuffers.computeIfAbsent(attackerId, k -> new HashMap<>())
                   .computeIfAbsent(context.getElement(), k -> new ArrayList<>())
                   .add(context);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processBuffers();
            tickBuffers.clear();
        }
    }

    private static void processBuffers() {
        for (Map<OPElementType, List<OPContext>> playerMap : tickBuffers.values()) {
            for (List<OPContext> contexts : playerMap.values()) {
                if (contexts.isEmpty()) continue;

                // Find highest health target
                OPContext winner = contexts.get(0);
                float maxHealth = winner.getTarget().getHealth();

                for (int i = 1; i < contexts.size(); i++) {
                    OPContext current = contexts.get(i);
                    float currentHealth = current.getTarget().getHealth();
                    if (currentHealth > maxHealth) {
                        maxHealth = currentHealth;
                        winner = current;
                    }
                }

                // Trigger for the winner
                triggerWinner(winner);
            }
        }
    }

    private static void triggerWinner(OPContext context) {
        // Debug message for detection
        context.getAttacker().sendSystemMessage(net.minecraft.network.chat.Component.literal(
            String.format("\u00A78[OP Debug] Winner Selected: %.1f on %s", 
            context.getRawDamage(), context.getTarget().getName().getString())
        ));

        OverwhelmingPowerRegistry.getInstance().trigger(context);
    }
}
