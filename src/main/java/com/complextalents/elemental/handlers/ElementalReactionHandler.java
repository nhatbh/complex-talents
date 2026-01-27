package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.ElementStack;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementalStackTracker;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.events.ElementStackAppliedEvent;
import com.complextalents.elemental.events.ElementalStackRemovedEvent;
import com.complextalents.elemental.registry.ReactionRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;

/**
 * Handles elemental reaction triggering when ElementStackAppliedEvent is fired.
 * This is the second stage in the reaction chain.
 *
 * <p>Listens to: {@link com.complextalents.elemental.events.ElementStackAppliedEvent}</p>
 * <p>Fires: {@link com.complextalents.elemental.events.ElementalReactionTriggeredEvent} (via ReactionRegistry)</p>
 *
 * <p>Note: This handler responds to the POST-application event. For preventing element
 * application (e.g., for special entities like Nature Cores), use the
 * {@link com.complextalents.elemental.events.ElementStackPreAppliedEvent} in a separate handler.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalReactionHandler {

    /**
     * Listens for element stack application and checks for possible reactions.
     * Triggers reactions if conditions are met.
     */
    @SubscribeEvent
    public static void onStackApplied(ElementStackAppliedEvent event) {
        if (!ElementalReactionConfig.enableElementalSystem.get()) return;

        LivingEntity target = event.getTarget();
        LivingEntity source = event.getSource();
        ElementType newElement = event.getElement();

        // Server-side only
        if (target.level().isClientSide) return;

        // Only trigger reactions if source is a player
        if (!(source instanceof ServerPlayer player)) {
            return;
        }

        UUID targetId = target.getUUID();
        Map<ElementType, ElementStack> elements = ElementalStackTracker.getEntityStacks(targetId);

        TalentsMod.LOGGER.info("REACTION_CHECK_START: Checking reactions for {} (UUID: {}). Applied element: {}. Existing stacks: {}",
            target.getName().getString(), targetId, newElement, elements != null ? elements.keySet() : "null");

        if (elements == null || elements.isEmpty()) {
            return;
        }

        // Track this entity for the player
        ElementalStackTracker.addTracking(player.getUUID(), targetId);

        // Check for reactions with existing elements
        for (Map.Entry<ElementType, ElementStack> entry : elements.entrySet()) {
            ElementType existingElement = entry.getKey();

            // Skip if same element or can't react
            if (existingElement == newElement || !existingElement.canReactWith(newElement)) {
                continue;
            }

            ElementalReaction reaction = existingElement.getReactionWith(newElement);
            if (reaction == null) {
                continue;
            }

            // Trigger the reaction
            boolean executed = ReactionRegistry.getInstance().executeReaction(
                target, reaction, newElement, existingElement, player, 1.0f
            );

            TalentsMod.LOGGER.info("REACTION_EXECUTED: {} reaction on {} (UUID: {}). Reaction executed: {}. Existing element: {}, New element: {}",
                reaction, target.getName().getString(), targetId, executed, existingElement, newElement);

            // If reaction was executed and it consumes stacks, remove the existing element
            if (executed) {
                IReactionStrategy strategy = ReactionRegistry.getInstance().getStrategy(reaction);
                if (strategy != null && strategy.consumesStacks()) {
                    TalentsMod.LOGGER.info("CONSUMING_STACK: Removing {} stack from {} (UUID: {}) after {} reaction. Stacks before removal: {}",
                        existingElement, target.getName().getString(), targetId, reaction, elements.keySet());

                    // Fire the stack removed event before removing
                    ElementalStackRemovedEvent removedEvent = new ElementalStackRemovedEvent(
                        target, existingElement, ElementalStackRemovedEvent.RemovalReason.REACTION_CONSUMED
                    );
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(removedEvent);

                    elements.remove(existingElement);
                    TalentsMod.LOGGER.info("STACK_CONSUMED: Removed {} stack. Remaining stacks: {}",
                        existingElement, elements.keySet());
                } else {
                    TalentsMod.LOGGER.info("STRATEGY_INFO: Reaction {} has strategy: {}, Consumes stacks: {}",
                        reaction, strategy != null ? strategy.getClass().getSimpleName() : "null",
                        strategy != null ? strategy.consumesStacks() : "N/A");
                }
            }

            // Only trigger one reaction per stack application
            break;
        }
    }

    /**
     * Clean up tracker when an entity dies.
     * Called by ElementalStackManager's death event handler.
     *
     * @param entityId The UUID of the entity that died
     */
    public static void onEntityDeath(UUID entityId) {
        ElementalStackTracker.removeEntityTracking(entityId);
    }

    /**
     * Clean up tracker when a player disconnects.
     * Called by ElementalStackManager's logout event handler.
     *
     * @param playerId The UUID of the player that disconnected
     */
    public static void onPlayerLogout(UUID playerId) {
        ElementalStackTracker.removePlayerTracking(playerId);
    }
}
