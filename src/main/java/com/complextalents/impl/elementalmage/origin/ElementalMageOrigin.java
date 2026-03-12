package com.complextalents.impl.elementalmage.origin;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.events.ElementalDamageEvent;
import com.complextalents.impl.elementalmage.ElementalMageData;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Elemental Mage Origin - Masters of raw Evocation magic.
 * Scales attributes based on a mathematical framework utilizing the Balance
 * Metric and diminishing returns.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalMageOrigin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents",
            "elemental_mage");
    
    // Level scaling arrays (index corresponds to level 1, 2, 3, 4, 5)
    private static final double[] BASE_RES = {40.0, 50.0, 60.0, 70.0, 100.0};
    private static final double[] MULT_RES = {60.0, 70.0, 80.0, 95.0, 120.0};
    private static final double[] BASE_REGEN = {1.0, 1.2, 1.4, 1.6, 2.5}; // Per second
    private static final double[] MULT_REGEN = {1.0, 1.1, 1.2, 1.4, 2.0}; // Per second per mastery point

    /**
     * Register the Elemental Mage origin.
     * Call this during mod initialization.
     */
    public static void register() {
        // Register the Elemental Resonance resource
        com.complextalents.origin.ResourceType resonanceType = com.complextalents.origin.ResourceType.register(
                ResourceLocation.fromNamespaceAndPath("complextalents", "elemental_resonance"),
                "Elemental Resonance",
                0.0,
                100.0, // Default max, overridden dynamically
                0xFF4D96FF // Bright blue color for UI
        );

        OriginBuilder.create("complextalents", "elemental_mage")
                .displayName("Elemental Mage")
                .description(Component
                        .literal("Masters of raw elemental magic. Deals elemental damage to fuel their mastery. Triggers potent elemental reactions."))
                .resourceType(resonanceType)
                .maxLevel(5) // Max level is now 5
                .dynamicMaxResource((level, player) -> {
                    int idx = Math.min(Math.max(level - 1, 0), 4);
                    // Get latest elemental mastery value
                    double mastery = com.complextalents.elemental.registry.ReactionRegistry.getInstance().calculateElementalMastery(player);
                    return BASE_RES[idx] + (MULT_RES[idx] * mastery);
                })
                .renderer(new com.complextalents.impl.elementalmage.client.ElementalMageRenderer())
                .register();

        // Register Harmonic Convergence Skill
        com.complextalents.impl.elementalmage.skill.HarmonicConvergenceSkill.register();

        TalentsMod.LOGGER.info("Elemental Mage origin registered");
    }

    /**
     * Get the Elemental Mage origin ID.
     */
    public static ResourceLocation getId() {
        return ID;
    }

    /**
     * Check if a player is an Elemental Mage.
     */
    public static boolean isElementalMage(net.minecraft.server.level.ServerPlayer player) {
        return ID.equals(OriginManager.getOriginId(player));
    }

    /**
     * Regenerate Elemental Resonance over time.
     * Called every tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        // Only run on server, and only once per tick (Phase START or END, pick one)
        if (event.side.isClient() || event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        
        if (!(event.player instanceof ServerPlayer serverPlayer)) return;

        // Apply regeneration once per second (every 20 ticks)
        if (serverPlayer.level().getGameTime() % 20L == 0L) {
            if (isElementalMage(serverPlayer)) {
                // Get player origin data capability
                serverPlayer.getCapability(com.complextalents.origin.capability.OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
                    int level = data.getOriginLevel();
                    int idx = Math.min(Math.max(level - 1, 0), 4);
                    
                    double mastery = com.complextalents.elemental.registry.ReactionRegistry.getInstance().calculateElementalMastery(serverPlayer);
                    double regenAmount = BASE_REGEN[idx] + (MULT_REGEN[idx] * mastery);
                    
                    data.modifyResource(regenAmount);
                    
                    // Force a sync to ensure dynamic max resource (based on mastery) is instantly updated 
                    // on the client, even if current resource is already full and didn't change.
                    data.sync();
                });
            }
        }
    }

    /**
     * Listen for ElementalDamageEvents to feed the math framework.
     */
    @SubscribeEvent
    public static void onElementalDamage(ElementalDamageEvent event) {
        // Ensure the event has a valid caster and target
        if (!(event.getSource() instanceof ServerPlayer player))
            return;

        // Ensure the player is an Elemental Mage
        if (!isElementalMage(player))
            return;

        // Pass the damage event details into the stats system
        ElementalMageData.processElementalDamage(player, event.getElement(), event.getDamage());
    }
}
