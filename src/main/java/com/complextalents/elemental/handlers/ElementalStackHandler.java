package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.ElementStack;
import com.complextalents.elemental.ElementalStackTracker;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.OPTargetSelector;
import com.complextalents.elemental.events.ElementStackAppliedEvent;
import com.complextalents.elemental.events.ElementStackPreAppliedEvent;
import com.complextalents.elemental.events.ElementalDamageEvent;
import com.complextalents.elemental.events.ElementalStackRemovedEvent;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnElementFXPacket;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

/**
 * Handles elemental stack application and lifecycle management.
 * This is the first stage in the reaction chain.
 *
 * <p>Listens to: {@link ElementalDamageEvent}</p>
 * <p>Fires: {@link ElementStackPreAppliedEvent}, {@link ElementStackAppliedEvent}</p>
 *
 * <p>Also handles stack ticking, cleanup, and entity death/logout events.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalStackHandler {

    /**
     * Listens for elemental damage and applies element stacks.
     * Fires ElementStackAppliedEvent after stack is applied.
     */
    @SubscribeEvent
    public static void onElementalDamage(ElementalDamageEvent event) {
        if (!ElementalReactionConfig.enableElementalSystem.get()) return;

        LivingEntity target = event.getTarget();
        LivingEntity source = event.getSource();
        ElementType element = event.getElement();

        // Server-side only
        if (target.level().isClientSide) return;

        // Validate inputs
        if (target == null || element == null) {
            TalentsMod.LOGGER.warn("Invalid ElementalDamageEvent: target={}, element={}", target, element);
            return;
        }
        // Fire pre-application event (allows cancellation before stack is applied)
        ElementStackPreAppliedEvent preEvent = new ElementStackPreAppliedEvent(target, source, element, 1);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(preEvent);

        // Check if pre-event was canceled
        if (preEvent.isCanceled()) {
            TalentsMod.LOGGER.debug("Element stack application canceled by pre-event handler for entity {}", target.getUUID());
            return;
        }

        try {
            UUID targetId = target.getUUID();
            Map<ElementType, ElementStack> elements = ElementalStackTracker.getOrCreateEntityStacks(targetId);

            // Check if element already exists
            ElementStack existingStack = elements.get(element);
            if (existingStack != null) {
                // Element already applied, just refresh it
                // Replace with new stack to update the applied tick
                ElementStack newStack = new ElementStack(element, target, source);
                elements.put(element, newStack);

                TalentsMod.LOGGER.debug("Refreshed {} stack on {}", element, target.getName().getString());

                // Spawn particle effects for stack refresh
                if (target.level() instanceof ServerLevel) {
                    Vec3 particlePos = target.position().add(0, target.getBbHeight() / 2, 0);
                    SpawnElementFXPacket packet = new SpawnElementFXPacket(particlePos, element, 1);
                    PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
                }
                return;
            }

            // Use the potentially modified stack count from the pre-event
            int stackCount = preEvent.getStackCount();

            // Fire the stack applied event (fires after stack is actually applied)
            ElementStackAppliedEvent stackEvent = new ElementStackAppliedEvent(target, source, element, stackCount);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(stackEvent);

            TalentsMod.LOGGER.info("AFTER_REACTION_CHECK: Element {} applied to {} (UUID: {}). Remaining stacks: {}",
                element, target.getName().getString(), targetId, elements.keySet());

            // Check if event was canceled
            if (stackEvent.isCanceled()) {
                TalentsMod.LOGGER.debug("Element stack application canceled by event handler for entity {}", targetId);
                return;
            }

            // Create and add the new element stack
            ElementStack stack = new ElementStack(element, target, source);
            elements.put(element, stack);

            TalentsMod.LOGGER.info("BEFORE_REACTION_CHECK: Applied {} stack to {} (UUID: {}). Current stacks: {}",
                element, target.getName().getString(), targetId, elements.keySet());

            // Spawn particle effects for stack application
            if (target.level() instanceof ServerLevel) {
                Vec3 particlePos = target.position().add(0, target.getBbHeight() / 2, 0);
                SpawnElementFXPacket packet = new SpawnElementFXPacket(particlePos, element, 1);
                PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
            }

            TalentsMod.LOGGER.debug("Applied {} stack to {}", element, target.getName().getString());

            // Trigger Overwhelming Power logic
            if (source instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    String.format("\u00A78[OP Debug] Elemental Hit Detected: %s on %s, Damage=%.1f", 
                    element, target.getName().getString(), event.getDamage())
                ));
                OPElementType opType = OPElementType.fromBaseType(element);
                if (opType != null) {
                    OPTargetSelector.buffer(new OPContext(target, serverPlayer, opType, event.getDamage()));
                } else {
                    serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00A78[OP Debug] Failed to map element to OPElementType: " + element
                    ));
                }
            }

        } catch (Exception e) {
            TalentsMod.LOGGER.error("Error handling ElementalDamageEvent for entity {}: {}",
                target.getUUID(), e.getMessage(), e);
        }
    }

    /**
     * Gets the element stacks map for a specific entity.
     * Used by ElementalStackManager for tick processing.
     *
     * @param entityId The entity UUID
     * @return The element stacks map, or null if none exist
     */
    public static Map<ElementType, ElementStack> getEntityStacksMap(UUID entityId) {
        return ElementalStackTracker.getEntityStacks(entityId);
    }

    /**
     * Gets all entity elements.
     * Used by ElementalStackManager for tick processing.
     *
     * @return The entity elements map
     */
    public static Map<UUID, Map<ElementType, ElementStack>> getAllEntityElements() {
        return ElementalStackTracker.getAllEntityElements();
    }

    /**
     * Removes all element stacks for an entity.
     *
     * @param entityId The entity UUID
     */
    public static void clearEntityStacks(UUID entityId) {
        ElementalStackTracker.removeEntityStacks(entityId);
    }

    /**
     * Clean up stacks and tracker when an entity dies
     */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // Server-side only
        if (entity.level().isClientSide) return;

        UUID entityId = entity.getUUID();

        // Fire removal events for all stacks before removing them
        var stacks = ElementalStackTracker.getEntityStacks(entityId);
        if (stacks != null) {
            stacks.forEach((element, stack) -> {
                ElementalStackRemovedEvent removedEvent = new ElementalStackRemovedEvent(
                    entity, element, ElementalStackRemovedEvent.RemovalReason.ENTITY_DEATH
                );
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(removedEvent);
            });
        }

        // Remove from tracker
        ElementalStackTracker.removeEntityTracking(entityId);

        // Notify reaction handler
        ElementalReactionHandler.onEntityDeath(entityId);

        // Stacks will be cleaned up by the tick handler when it detects the entity is dead
        // But we can optionally remove them immediately here for faster cleanup
        ElementalStackTracker.removeEntityStacks(entityId);

        TalentsMod.LOGGER.debug("Cleaned up elemental stacks for dead entity: {}", entity.getName().getString());
    }

    /**
     * Clean up tracker when a player disconnects to prevent memory leaks
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerId = player.getUUID();

            // Get all entities this player was tracking
            var trackedEntities = ElementalStackTracker.getTrackedEntities(playerId);

            // Remove all tracked entities for this player
            trackedEntities.forEach(ElementalStackTracker::removeEntityTracking);

            // Notify reaction handler
            ElementalReactionHandler.onPlayerLogout(playerId);

            TalentsMod.LOGGER.debug("Cleaned up tracker for disconnected player: {}", player.getName().getString());
        }
    }

    /**
     * World tick handler for stack maintenance:
     * - Removes expired stacks
     * - Spawns visual particles
     * - Cleans up dead/invalid entities
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = event.level;
        if (level.isClientSide) return;

        // Get the shared entity elements map from ElementalStackTracker
        var entityElements = ElementalStackTracker.getAllEntityElements();

        // Early return if no stacks to process
        if (entityElements.isEmpty()) return;

        long decayTicks = ElementalReactionConfig.stackDecayTicks.get();
        long gameTime = level.getGameTime();

        // Process all entities with stacks
        entityElements.entrySet().removeIf(entry -> {
            Map<ElementType, ElementStack> stacks = entry.getValue();

            // Validate and clean corrupted stacks
            if (stacks == null || stacks.isEmpty()) {
                return true; // Remove empty or null stack maps
            }

            // Remove corrupted stacks (null elements or stacks with invalid data)
            stacks.entrySet().removeIf(stackEntry -> {
                if (stackEntry == null || stackEntry.getKey() == null || stackEntry.getValue() == null) {
                    TalentsMod.LOGGER.warn("Removing corrupted stack entry for entity {}", entry.getKey());
                    return true;
                }
                ElementStack stack = stackEntry.getValue();
                // Validate stack data integrity
                if (stack.getEntity() == null) {
                    TalentsMod.LOGGER.warn("Removing invalid stack for entity {} - element: {}",
                        entry.getKey(), stack.getElement());
                    return true;
                }
                return false;
            });

            // If all stacks were corrupted, remove the entity entry
            if (stacks.isEmpty()) {
                return true;
            }

            // Get entity reference from the stack itself (more reliable than UUID lookup)
            try {
                ElementStack anyStack = stacks.values().iterator().next();
                LivingEntity entity = anyStack.getEntity();

                if (entity == null || !entity.isAlive()) {
                    ElementalStackTracker.removeEntityTracking(entry.getKey()); // Clean up tracker
                    return true; // Remove stacks for dead/non-existent entities
                }

                // Make sure entity is in the correct dimension
                if (!entity.level().equals(level)) {
                    return false; // Skip processing this entity in this dimension
                }

                // Spawn particles every 10 ticks (0.5 seconds) for visual feedback
                if (gameTime % 10 == 0) {
                    Vec3 particlePos = entity.position().add(0, entity.getBbHeight() / 2, 0);
                    for (Map.Entry<ElementType, ElementStack> stackEntry : stacks.entrySet()) {
                        ElementType element = stackEntry.getKey();

                        // Spawn continuous particle effect (always count of 1)
                        SpawnElementFXPacket packet = new SpawnElementFXPacket(particlePos, element, 1);
                        PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
                    }
                }

                // Remove expired stacks every second (20 ticks)
                if (gameTime % 20 == 0) {
                    stacks.entrySet().removeIf(stackEntry -> {
                        try {
                            ElementStack stack = stackEntry.getValue();
                            if (stack.isExpired(decayTicks)) {
                                // Fire the stack removed event for expired stacks
                                ElementalStackRemovedEvent removedEvent = new ElementalStackRemovedEvent(
                                    entity, stackEntry.getKey(), ElementalStackRemovedEvent.RemovalReason.EXPIRED
                                );
                                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(removedEvent);
                                return true;
                            }
                            return false;
                        } catch (Exception e) {
                            TalentsMod.LOGGER.error("Error checking stack expiration for entity {}: {}",
                                entry.getKey(), e.getMessage());
                            return true; // Remove problematic stack
                        }
                    });
                }

                return stacks.isEmpty(); // Remove entity entry if no stacks remain
            } catch (Exception e) {
                TalentsMod.LOGGER.error("Error processing stacks for entity {}: {}",
                    entry.getKey(), e.getMessage());
                ElementalStackTracker.removeEntityTracking(entry.getKey());
                return true; // Remove entity entry with error
            }
        });
    }
}
