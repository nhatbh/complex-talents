package com.complextalents.elemental.talents.mage;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.talent.PassiveTalent;
import com.complextalents.talent.ResourceBarConfig;
import com.complextalents.talent.ResourceBarType;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The base Definition talent for the Elemental Mage class.
 * This talent enables the Focus resource system and serves as the foundation
 * for all Elemental Mage abilities.
 */
public class ElementalMageDefinition extends PassiveTalent {

    // Focus system constants
    public static final float BASE_MAX_FOCUS = 150f;
    public static final float FOCUS_DECAY_RATE = 5f; // per second
    public static final int FOCUS_DECAY_DELAY = 600; // 30 seconds in ticks
    public static final float BASE_FOCUS_PER_REACTION = 10f;

    // Track Focus decay for each player
    private static final java.util.Map<java.util.UUID, Integer> lastFocusGainTick = new java.util.HashMap<>();

    public ElementalMageDefinition() {
        super(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition"),
            Component.translatable("talent.complextalents.elemental_mage_definition"),
            Component.translatable("talent.complextalents.elemental_mage_definition.desc"),
            1, // Max level 1 for Definition talents
            TalentSlotType.DEFINITION,
            createFocusResourceConfig()
        );

        // Register event handler for Focus decay
        MinecraftForge.EVENT_BUS.register(this);
    }

    private static ResourceBarConfig createFocusResourceConfig() {
        return ResourceBarConfig.builder(
            ResourceBarType.FOCUS,
            Component.translatable("resource.complextalents.focus")
        )
            .maxValue(BASE_MAX_FOCUS)
            .startingValue(0f)
            .regenRate(0f) // We'll handle decay manually
            .color(0x6699FF) // Ethereal blue
            .showInUI(true)
            .build();
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Initialize Focus tracking for this player
        lastFocusGainTick.put(player.getUUID(), player.server.getTickCount());
        TalentsMod.LOGGER.info("Elemental Mage Definition unlocked for player: " + player.getName().getString());
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Clean up tracking data
        lastFocusGainTick.remove(player.getUUID());
        TalentsMod.LOGGER.info("Elemental Mage Definition removed for player: " + player.getName().getString());
    }

    /**
     * Called when the player gains Focus from any source.
     * Resets the decay timer.
     */
    public static void onFocusGained(ServerPlayer player) {
        if (player != null) {
            lastFocusGainTick.put(player.getUUID(), player.server.getTickCount());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Check if this player has the Elemental Mage Definition talent
        serverPlayer.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            if (!talents.hasTalent(this.getId())) {
                return;
            }

            // Handle Focus decay
            Integer lastGainTick = lastFocusGainTick.get(serverPlayer.getUUID());
            if (lastGainTick != null) {
                int currentTick = serverPlayer.server.getTickCount();
                int ticksSinceGain = currentTick - lastGainTick;

                // Start decay after FOCUS_DECAY_DELAY ticks of no Focus generation
                if (ticksSinceGain > FOCUS_DECAY_DELAY) {
                    float currentFocus = talents.getResource();
                    if (currentFocus > 0) {
                        // Decay Focus by FOCUS_DECAY_RATE per second (convert to per tick)
                        float decayPerTick = FOCUS_DECAY_RATE / 20f;
                        float newFocus = Math.max(0, currentFocus - decayPerTick);
                        talents.setResource(newFocus);
                    }
                }
            }
        });
    }

    /**
     * Calculate Focus generation from a reaction, accounting for any bonuses.
     * This will be enhanced by the Elemental Attunement talent.
     */
    public static float calculateFocusGeneration(ServerPlayer player, boolean isSuperReaction) {
        float baseFocus = isSuperReaction ? BASE_FOCUS_PER_REACTION * 5 : BASE_FOCUS_PER_REACTION;

        // This will be modified by Elemental Attunement talent bonuses
        // For now, return base amount
        return baseFocus;
    }

}