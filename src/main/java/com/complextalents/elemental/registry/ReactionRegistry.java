package com.complextalents.elemental.registry;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.events.ElementalReactionTriggeredEvent;
import com.complextalents.elemental.strategies.reactions.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

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

        final float mastery = calculateElementalMastery(attacker);

        // Build the reaction context
        ReactionContext context = ReactionContext.builder()
            .target(target)
            .attacker(attacker)
            .reaction(reaction)
            .triggeringElement(triggeringElement)
            .existingElement(existingElement)
            .damageMultiplier(damageMultiplier)
            .elementalMastery(mastery)
            .level((ServerLevel) target.level())
            .build();

        // Check if the reaction can trigger
        if (!strategy.canTrigger(context)) {
            TalentsMod.LOGGER.debug("Reaction {} cannot trigger for target {}",
                reaction, target.getName().getString());
            return false;
        }

        // Calculate final damage before execution
        float finalDamage = strategy.calculateDamage(context);

        // Fire the reaction triggered event
        ElementalReactionTriggeredEvent reactionEvent = new ElementalReactionTriggeredEvent(
            target, attacker, reaction, triggeringElement, existingElement,
            finalDamage, mastery, damageMultiplier
        );
        MinecraftForge.EVENT_BUS.post(reactionEvent);

        // Check if event was canceled
        if (reactionEvent.isCanceled()) {
            TalentsMod.LOGGER.debug("Reaction {} canceled by event handler for target {}",
                reaction, target.getName().getString());
            return false;
        }

        // Update damage from event (in case handler modified it)
        finalDamage = reactionEvent.getDamage();

        // Execute the reaction
        strategy.execute(context);

        // Send chat message with damage breakdown
        sendDamageChatMessage(attacker, reaction, mastery, damageMultiplier, finalDamage, target);

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
        // Register original five reaction implementations
        register(ElementalReaction.MELT, new MeltReaction());
        register(ElementalReaction.VAPORIZE, new VaporizeReaction());
        register(ElementalReaction.OVERLOADED, new OverloadReaction());
        register(ElementalReaction.BURNING, new BurningReaction());
        register(ElementalReaction.VOIDFIRE, new VoidfireReaction());

        // Register new Ice reactions
        register(ElementalReaction.FREEZE, new FreezeReaction());
        register(ElementalReaction.SUPERCONDUCT, new SuperconductReaction());
        register(ElementalReaction.PERMAFROST, new PermafrostReaction());
        register(ElementalReaction.FRACTURE, new FractureReaction());

        // Register Aqua reactions
        register(ElementalReaction.ELECTRO_CHARGED, new ElectroChargedReaction());

        // Register Nature reactions
        register(ElementalReaction.BLOOM, new BloomReaction());

        TalentsMod.LOGGER.info("Registered 11 default elemental reaction strategies");
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

    /**
     * Calculates elemental mastery using the geometric mean of spell power attributes.
     * This provides a balanced overall power metric that considers all elements equally.
     *
     * Geometric mean is calculated as: (product of all values)^(1/n)
     * This is better than arithmetic mean for this use case because:
     * - It penalizes having very low values in any element
     * - It prevents extremely high single-element stats from skewing the result
     * - It better represents overall elemental proficiency
     *
     * @param player The player to calculate mastery for
     * @return The geometric mean of all spell power attributes, or 1.0 if no attributes found
     */
    public float calculateElementalMastery(ServerPlayer player) {
        // List of all spell power attributes to include in calculation
        List<ResourceLocation> spellPowerAttributes = List.of(
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "fire_spell_power"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ice_spell_power"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "lightning_spell_power"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "nature_spell_power"),
            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "ender_spell_power"),
            ResourceLocation.fromNamespaceAndPath("traveloptics", "aqua_spell_power")
        );

        float product = 1.0f;
        int validAttributes = 0;

        // Collect attribute values and calculate product
        for (ResourceLocation attrId : spellPowerAttributes) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);

            if (attribute != null) {
                double value = player.getAttributeValue(attribute);
                product *= value;
                validAttributes++;

                TalentsMod.LOGGER.debug("Attribute {} = {}", attrId, value);
            } else {
                TalentsMod.LOGGER.warn("Attribute not found: {}", attrId);
            }
        }

        // If no valid attributes found, return default
        if (validAttributes == 0) {
            TalentsMod.LOGGER.warn("No valid spell power attributes found for player {}", player.getName().getString());
            return 0;
        }

        // Calculate geometric mean: nth root of the product
        double geometricMean = Math.pow(product, 1.0 / validAttributes);

        TalentsMod.LOGGER.debug("Elemental mastery for {}: {} (based on {} attributes)",
            player.getName().getString(), geometricMean, validAttributes);

        return (float)geometricMean;
    }

    /**
     * Calculates elemental mastery with a custom attribute list.
     * Useful for testing or specialized calculations.
     *
     * @param player The player to calculate mastery for
     * @param attributes Custom list of attribute IDs to include
     * @return The geometric mean of the specified attributes
     */
    public double calculateElementalMastery(ServerPlayer player, List<ResourceLocation> attributes) {
        double product = 1.0;
        int validAttributes = 0;

        for (ResourceLocation attrId : attributes) {
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attrId);

            if (attribute != null) {
                double value = player.getAttributeValue(attribute);
                double safeValue = Math.max(1.0, value);
                product *= safeValue;
                validAttributes++;
            }
        }

        return validAttributes > 0 ? Math.pow(product, 1.0 / validAttributes) : 1.0;
    }

    /**
     * Sends a chat message to the attacker with damage calculation details.
     *
     * @param attacker The player who triggered the reaction
     * @param reaction The reaction type
     * @param mastery The elemental mastery value
     * @param multiplier The damage multiplier
     * @param finalDamage The final calculated damage
     * @param target The target entity
     */
    private void sendDamageChatMessage(ServerPlayer attacker, ElementalReaction reaction,
                                       float mastery, float multiplier, float finalDamage,
                                       LivingEntity target) {
        Component message = Component.literal("")
            .append(Component.literal("Elemental Reaction: ")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF6B6B"))))
            .append(Component.literal(reaction.name())
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FFD93D")).withBold(true)))
            .append(Component.literal("\n"))

            .append(Component.literal("Mastery: ")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#6BCB77"))))
            .append(Component.literal(String.format("%.2f", mastery))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#4D96FF"))))
            .append(Component.literal(" | "))

            .append(Component.literal("Multiplier: ")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#6BCB77"))))
            .append(Component.literal(String.format("%.2f", multiplier))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#4D96FF"))))
            .append(Component.literal("\n"))

            .append(Component.literal("Final Damage: ")
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF6B6B"))))
            .append(Component.literal(String.format("%.2f", finalDamage))
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#FF0000")).withBold(true)))
            .append(Component.literal(" to "))

            .append(Component.literal(target.getName().getString())
                .setStyle(Style.EMPTY.withColor(TextColor.parseColor("#AAAAAA"))));

        attacker.sendSystemMessage(message);
    }
}