package com.complextalents.elemental.registry;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.strategies.reactions.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for elemental reaction strategies.
 * Manages registration, lookup, execution, and initialization of reaction implementations.
 * Implemented as a singleton for global access.
 * Thread-safe for concurrent access.
 */
public class ReactionRegistry {

    private static ReactionRegistry INSTANCE;

    private final Map<ElementalReaction, IReactionStrategy> strategies;
    private final Map<String, ElementalReaction> nameToReactionMap;
    private final List<IReactionStrategy> sortedStrategies;

    // Lock for thread safety during modifications
    private final Object registryLock = new Object();
    private volatile boolean initialized = false;

    /**
     * Private constructor for singleton pattern.
     * Reactions can be registered dynamically as needed.
     */
    private ReactionRegistry() {
        this.strategies = new ConcurrentHashMap<>();
        this.nameToReactionMap = new ConcurrentHashMap<>();
        this.sortedStrategies = new ArrayList<>();
    }

    /**
     * Gets the singleton instance of the ReactionRegistry.
     *
     * @return The ReactionRegistry instance
     */
    public static ReactionRegistry getInstance() {
        if (INSTANCE == null) {
            synchronized (ReactionRegistry.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ReactionRegistry();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Initializes the registry with default reaction strategies.
     * Should be called during mod initialization.
     */
    public void initialize() {
        synchronized (registryLock) {
            if (initialized) {
                TalentsMod.LOGGER.warn("ReactionRegistry already initialized");
                return;
            }

            TalentsMod.LOGGER.info("Initializing Elemental Reaction Registry");

            // Register all default reactions
            registerDefaultReactions();

            // Sort strategies by priority
            updateSortedStrategies();

            initialized = true;
            TalentsMod.LOGGER.info("Registered {} elemental reactions", strategies.size());
        }
    }

    /**
     * Executes a reaction using the registered strategy.
     *
     * @param target The target entity
     * @param reaction The reaction type
     * @param triggeringElement The element that triggered the reaction
     * @param existingElement The element already on the target
     * @param attacker The attacking player
     * @param damageMultiplier Damage multiplier added by skills
     * @return true if the reaction was executed, false otherwise
     */
    public boolean executeReaction(LivingEntity target, ElementalReaction reaction,
                                  ElementType triggeringElement, ElementType existingElement,
                                  ServerPlayer attacker, float damageMultiplier) {

        // Get the strategy for this reaction
        IReactionStrategy strategy = getStrategy(reaction);
        if (strategy == null) {
            TalentsMod.LOGGER.debug("No strategy registered for reaction: {}", reaction);
            return false;
        }

        // Build the reaction context
        ReactionContext context = ReactionContext.builder()
            .target(target)
            .attacker(attacker)
            .reaction(reaction)
            .triggeringElement(triggeringElement)
            .existingElement(existingElement)
            .damageMultiplier(damageMultiplier)
            .level((ServerLevel) target.level())
            .build();

        // Check if the reaction can trigger
        if (!strategy.canTrigger(context)) {
            TalentsMod.LOGGER.debug("Reaction {} cannot trigger for target {}",
                reaction, target.getName().getString());
            return false;
        }

        // Execute the reaction
        strategy.execute(context);

        TalentsMod.LOGGER.info("Executed {} reaction on {} by {}",
            reaction, target.getName().getString(), attacker.getName().getString());

        return true;
    }

    /**
     * Registers a reaction strategy.
     *
     * @param reaction The reaction type
     * @param strategy The strategy implementation
     * @throws IllegalArgumentException if reaction is already registered
     */
    public void register(ElementalReaction reaction, IReactionStrategy strategy) {
        Objects.requireNonNull(reaction, "Reaction type cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        synchronized (registryLock) {
            if (strategies.containsKey(reaction)) {
                throw new IllegalArgumentException(
                    "Reaction " + reaction + " is already registered");
            }

            strategies.put(reaction, strategy);
            nameToReactionMap.put(reaction.name().toLowerCase(), reaction);

            // Update sorted list
            updateSortedStrategies();

            TalentsMod.LOGGER.debug("Registered reaction strategy: {}", reaction);
        }
    }

    /**
     * Registers a reaction strategy, replacing any existing registration.
     *
     * @param reaction The reaction type
     * @param strategy The strategy implementation
     */
    public void registerOrReplace(ElementalReaction reaction, IReactionStrategy strategy) {
        Objects.requireNonNull(reaction, "Reaction type cannot be null");
        Objects.requireNonNull(strategy, "Strategy cannot be null");

        synchronized (registryLock) {
            strategies.put(reaction, strategy);
            nameToReactionMap.put(reaction.name().toLowerCase(), reaction);

            // Update sorted list
            updateSortedStrategies();

            TalentsMod.LOGGER.debug("Registered/replaced reaction strategy: {}", reaction);
        }
    }

    /**
     * Unregisters a reaction strategy.
     *
     * @param reaction The reaction type to unregister
     * @return The unregistered strategy, or null if not found
     */
    @Nullable
    public IReactionStrategy unregister(ElementalReaction reaction) {
        synchronized (registryLock) {
            IReactionStrategy removed = strategies.remove(reaction);
            if (removed != null) {
                nameToReactionMap.remove(reaction.name().toLowerCase());
                updateSortedStrategies();
                TalentsMod.LOGGER.debug("Unregistered reaction strategy: {}", reaction);
            }
            return removed;
        }
    }

    /**
     * Gets the strategy for a specific reaction type.
     *
     * @param reaction The reaction type
     * @return The strategy, or null if not registered
     */
    @Nullable
    public IReactionStrategy getStrategy(ElementalReaction reaction) {
        return strategies.get(reaction);
    }

    /**
     * Gets a reaction by name (case-insensitive).
     *
     * @param name The reaction name
     * @return The reaction type, or null if not found
     */
    @Nullable
    public ElementalReaction getReactionByName(String name) {
        return nameToReactionMap.get(name.toLowerCase());
    }

    /**
     * Gets all registered reaction types.
     *
     * @return Unmodifiable set of registered reactions
     */
    public Set<ElementalReaction> getRegisteredReactions() {
        return Collections.unmodifiableSet(strategies.keySet());
    }

    /**
     * Gets all strategies sorted by priority (highest first).
     *
     * @return Unmodifiable list of sorted strategies
     */
    public List<IReactionStrategy> getSortedStrategies() {
        return Collections.unmodifiableList(sortedStrategies);
    }

    /**
     * Checks if a reaction is registered.
     *
     * @param reaction The reaction type
     * @return true if registered
     */
    public boolean isRegistered(ElementalReaction reaction) {
        return strategies.containsKey(reaction);
    }

    /**
     * Clears all registered strategies.
     * Useful for testing or reload scenarios.
     */
    public void clear() {
        synchronized (registryLock) {
            strategies.clear();
            nameToReactionMap.clear();
            sortedStrategies.clear();
            initialized = false;
            TalentsMod.LOGGER.info("Cleared reaction registry");
        }
    }

    /**
     * Reloads the registry, re-registering all default reactions.
     */
    public void reload() {
        synchronized (registryLock) {
            clear();
            initialize();
            TalentsMod.LOGGER.info("Reloaded reaction registry");
        }
    }

    /**
     * Updates the sorted strategies list based on priority.
     */
    private void updateSortedStrategies() {
        sortedStrategies.clear();
        sortedStrategies.addAll(strategies.values());
        sortedStrategies.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    /**
     * Registers all default reaction strategies.
     */
    private void registerDefaultReactions() {
        // Register our five reaction implementations
        register(ElementalReaction.MELT, new MeltReaction());
        register(ElementalReaction.VAPORIZE, new VaporizeReaction());
        register(ElementalReaction.OVERLOADED, new OverloadReaction());
        register(ElementalReaction.BURNING, new BurningReaction());
        register(ElementalReaction.VOIDFIRE, new VoidfireReaction());

        TalentsMod.LOGGER.info("Registered 5 default elemental reaction strategies");
    }

    /**
     * Gets statistics about the registry.
     *
     * @return Map of statistic names to values
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_reactions", strategies.size());
        stats.put("initialized", initialized);

        // Count by priority
        Map<Integer, Long> priorityCounts = new HashMap<>();
        for (IReactionStrategy strategy : strategies.values()) {
            priorityCounts.merge(strategy.getPriority(), 1L, Long::sum);
        }
        stats.put("priority_distribution", priorityCounts);

        return stats;
    }
}