package com.complextalents.elemental;

import com.complextalents.TalentsMod;
import com.complextalents.config.ElementalReactionConfig;
import com.complextalents.elemental.registry.ReactionRegistry;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SpawnParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ElementalStackManager {
    private static final Map<UUID, Map<ElementType, ElementStack>> entityElements = new HashMap<>();


    private static class Tracker {
        private static final Map<UUID, Set<UUID>> playerToEntitiesMap = new HashMap<>();
        private static final Map<UUID, UUID> entityToPlayerMap = new HashMap<>();

        static void add(UUID player, UUID entity) {
            playerToEntitiesMap.computeIfAbsent(player, k -> new HashSet<>()).add(entity);
            entityToPlayerMap.put(entity, player);
        }

        static void removeEntity(UUID entity) {
            UUID player = entityToPlayerMap.remove(entity);
            if (player != null) {
                Set<UUID> entities = playerToEntitiesMap.get(player);
                if (entities != null) {
                    entities.remove(entity);
                    if (entities.isEmpty()) {
                        playerToEntitiesMap.remove(player);
                    }
                }
            }
        }

        static Set<UUID> getTrackedEntities(UUID player) {
            return playerToEntitiesMap.getOrDefault(player, Collections.emptySet());
        }
    }
    /**
     * Applies an element stack to a target entity.
     * This is called from all magic damage sources (vanilla and modded).
     *
     * @param target The entity to apply the element to
     * @param element The element type to apply
     * @param source The entity causing the damage (for talent checking)
     */
    public static void applyElementStack(LivingEntity target, ElementType element, LivingEntity source) {
        if (!ElementalReactionConfig.enableElementalSystem.get()) return;

        UUID targetId = target.getUUID();
        Map<ElementType, ElementStack> elements = entityElements.computeIfAbsent(targetId, k -> new HashMap<>());

        // Check for reactions BEFORE adding the new element
        if (source instanceof ServerPlayer player) {
            Tracker.add(source.getUUID(), targetId);
            checkAndTriggerReactions(target, elements, element, player, 1);
        }

        // Get or create element stack
        ElementStack stack = elements.get(element);
        if (stack != null) {
            stack.addStack();
        } else {
            stack = new ElementStack(element, target, source);
            elements.put(element, stack);
        }

        // Cap the stack count and refresh timer
        int maxStacks = ElementalReactionConfig.maxStackCount.get();
        if (stack.getStackCount() > maxStacks) {
            stack.setStackCount(maxStacks);
            stack.refreshTimer();
        }

        // Spawn particle effects for stack application
        if (target.level() instanceof ServerLevel) {
            Vec3 particlePos = target.position().add(0, target.getBbHeight() / 2, 0);
            SpawnParticlesPacket packet = new SpawnParticlesPacket(particlePos, element, stack.getStackCount());
            PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> target), packet);
        }
    }

    private static void checkAndTriggerReactions(LivingEntity target, Map<ElementType, ElementStack> elements, ElementType newElement, ServerPlayer player, float damageMultiplier) {
        for (Map.Entry<ElementType, ElementStack> entry : elements.entrySet()) {
            ElementType existingElement = entry.getKey();
            if (existingElement != newElement && existingElement.canReactWith(newElement)) {
                ElementalReaction reaction = existingElement.getReactionWith(newElement);
                if (reaction != null) {
                    ReactionRegistry.getInstance().executeReaction(
                        target, reaction, newElement, existingElement, player, damageMultiplier
                    );
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Level level = event.level;
        if (level.isClientSide) return;

        // Early return if no stacks to process
        if (entityElements.isEmpty()) return;

        long decayMillis = ElementalReactionConfig.stackDecayTicks.get() * 50L; // Convert ticks to milliseconds
        long gameTime = level.getGameTime();

        // Process all entities with stacks
        entityElements.entrySet().removeIf(entry -> {
            Map<ElementType, ElementStack> stacks = entry.getValue();

            // Get entity reference from the stack itself (more reliable than UUID lookup)
            ElementStack anyStack = stacks.values().iterator().next();
            LivingEntity entity = anyStack.getEntity();

            if (entity == null || !entity.isAlive()) {
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
                    ElementStack stack = stackEntry.getValue();

                    // Spawn continuous particle effect
                    SpawnParticlesPacket packet = new SpawnParticlesPacket(particlePos, element, stack.getStackCount());
                    PacketHandler.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), packet);
                }
            }

            // Remove expired stacks every second (20 ticks)
            if (gameTime % 20 == 0) {
                stacks.entrySet().removeIf(stackEntry -> stackEntry.getValue().isExpired(decayMillis));
            }

            return stacks.isEmpty(); // Remove entity entry if no stacks remain
        });
    }

    public static void clearEntityStacks(UUID entityId) {
        entityElements.remove(entityId);
    }

    public static boolean hasAnyStack(LivingEntity entity) {
        Map<ElementType, ElementStack> stacks = entityElements.get(entity.getUUID());
        return stacks != null && !stacks.isEmpty();
    }

    public static Map<ElementType, Integer> getEntityStacks(LivingEntity entity) {
        Map<ElementType, Integer> result = new HashMap<>();
        Map<ElementType, ElementStack> stacks = entityElements.get(entity.getUUID());
        if (stacks != null) {
            for (Map.Entry<ElementType, ElementStack> entry : stacks.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getStackCount());
            }
        }
        return result;
    }

    /**
     * Set the stack count for a specific element on an entity
     */
    public static void setStacks(LivingEntity target, ElementType element, int count) {
        if (count <= 0) {
            // Remove the element stack if count is 0 or less
            Map<ElementType, ElementStack> stacks = entityElements.get(target.getUUID());
            if (stacks != null) {
                stacks.remove(element);
                if (stacks.isEmpty()) {
                    entityElements.remove(target.getUUID());
                    Tracker.removeEntity(target.getUUID());
                }
            }
        } else {
            Map<ElementType, ElementStack> stacks = entityElements.computeIfAbsent(target.getUUID(), k -> new HashMap<>());
            ElementStack stack = stacks.get(element);
            if (stack != null) {
                stack.setStackCount(count);
            } else {
                // Create new stack with specified count
                stack = new ElementStack(element, target, null);
                stack.setStackCount(count);
                stacks.put(element, stack);
            }
        }
    }

    private static LivingEntity getEntityFromGlobalUUID(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }

    private static ElementStack getLatestStack(Collection<ElementStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return null;
        }
        Optional<ElementStack> latest = stacks.stream().max(Comparator.comparingLong(ElementStack::getLastAppliedTime));

        return latest.orElse(null);
    }

    public static void detonateAllPossibleReactionFor(ServerPlayer player, float damageMultiplier) {
        Set<UUID> entityIds = Tracker.getTrackedEntities(player.getUUID());
        entityIds.forEach(targetId -> {
            LivingEntity target = getEntityFromGlobalUUID(player.getServer(), targetId);
            if (target != null) {
                Map<ElementType, ElementStack> elements = entityElements.computeIfAbsent(targetId, k -> new HashMap<>());
                ElementType latestElement = getLatestStack(elements.values()).getElement();
                if (elements.size() >= 2) {
                    // Only trigger standard reactions for now
                    checkAndTriggerReactions(target, elements, latestElement, player, damageMultiplier);
                }
            }
        });
    }
}
