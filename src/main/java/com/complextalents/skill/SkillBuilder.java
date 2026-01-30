package com.complextalents.skill;

import com.complextalents.passive.PassiveStackDef;
import com.complextalents.passive.PassiveStackRegistry;
import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Builder for creating skill definitions.
 * Provides fluent API for skill registration.
 */
public class SkillBuilder {
    private ResourceLocation id;
    private SkillNature nature = SkillNature.ACTIVE;
    private TargetType targetingType = TargetType.NONE;
    private double maxRange = 32.0;
    private double activeCooldown = 0.0;
    private double passiveCooldown = 0.0;
    private double resourceCost = 0.0;
    private ResourceLocation resourceType;
    private boolean toggleable = false;
    private double toggleCostPerTick = 0.0;
    private boolean allowSelfTarget = false;
    private boolean targetAllyOnly = false;
    private boolean targetPlayerOnly = false;
    private int maxLevel = 1;
    private final java.util.Map<String, double[]> scaledStats = new java.util.HashMap<>();
    private final java.util.Map<String, PassiveStackDef> passiveStacks = new java.util.HashMap<>();

    // Execution handlers
    private BiConsumer<Skill.ExecutionContext, Object> activeHandler;
    private Consumer<Object> passiveHandler;
    private ChanneledHandler channeledHandler;

    // Validation handler (runs before cast to check if skill can be used)
    private BiFunction<Skill.ExecutionContext, Object, Boolean> validationHandler;

    // Channeling properties
    private double minChannelTime = 0.0;
    private double maxChannelTime = 0.0;

    /**
     * Handler for channeled skills that receives channel time.
     */
    @FunctionalInterface
    public interface ChanneledHandler {
        void handle(Skill.ExecutionContext context, Object player, double channelTime);
    }

    /**
     * Create a new skill builder.
     *
     * @param modId    The mod ID (namespace)
     * @param skillName The skill name (path)
     * @return A new SkillBuilder instance
     */
    public static SkillBuilder create(String modId, String skillName) {
        return new SkillBuilder(ResourceLocation.fromNamespaceAndPath(modId, skillName));
    }

    /**
     * Create a new skill builder with a ResourceLocation.
     *
     * @param id The skill ID
     * @return A new SkillBuilder instance
     */
    public static SkillBuilder create(ResourceLocation id) {
        return new SkillBuilder(id);
    }

    private SkillBuilder(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Set the skill nature (PASSIVE, ACTIVE, BOTH, TOGGLE).
     */
    public SkillBuilder nature(SkillNature nature) {
        this.nature = nature;
        return this;
    }

    /**
     * Set the targeting type for the active component.
     */
    public SkillBuilder targeting(TargetType type) {
        this.targetingType = type;
        return this;
    }

    /**
     * Set the maximum range for targeting (in blocks).
     */
    public SkillBuilder maxRange(double range) {
        this.maxRange = range;
        return this;
    }

    /**
     * Set the cooldown in seconds for the active component.
     */
    public SkillBuilder activeCooldown(double seconds) {
        this.activeCooldown = seconds;
        return this;
    }

    /**
     * Set the cooldown in seconds for the passive trigger (for hybrid skills).
     */
    public SkillBuilder passiveCooldown(double seconds) {
        this.passiveCooldown = seconds;
        return this;
    }

    /**
     * Set the resource cost for active cast.
     *
     * @param cost        The amount of resource to consume
     * @param resourceType The resource ID (e.g., "irons_spellbooks:mana")
     */
    public SkillBuilder resourceCost(double cost, String resourceType) {
        this.resourceCost = cost;
        this.resourceType = parseResourceLocation(resourceType);
        return this;
    }

    /**
     * Set whether this skill can be toggled.
     */
    public SkillBuilder toggleable(boolean toggle) {
        this.toggleable = toggle;
        return this;
    }

    /**
     * Set the resource cost per tick while toggled on.
     */
    public SkillBuilder toggleCost(double costPerTick) {
        this.toggleCostPerTick = costPerTick;
        return this;
    }

    /**
     * Register the active execution handler.
     * This is called when SkillExecuteEvent fires for this skill.
     * The handler receives a Skill.ExecutionContext containing the player and target data.
     * The second parameter is the raw ServerPlayer for convenience.
     */
    public SkillBuilder onActive(BiConsumer<Skill.ExecutionContext, Object> handler) {
        this.activeHandler = handler;
        return this;
    }

    /**
     * Register a passive event handler.
     * For passives that respond to specific Forge events.
     * The handler receives the Forge event object.
     */
    public SkillBuilder onPassive(Consumer<Object> handler) {
        this.passiveHandler = handler;
        return this;
    }

    /**
     * Set the minimum channel time in seconds.
     */
    public SkillBuilder minChannelTime(double seconds) {
        this.minChannelTime = seconds;
        return this;
    }

    /**
     * Set the maximum channel time in seconds.
     */
    public SkillBuilder maxChannelTime(double seconds) {
        this.maxChannelTime = seconds;
        return this;
    }

    /**
     * Set whether this skill allows targeting the caster.
     * When true, the player can target themselves with ENTITY targeting.
     * When false, the player cannot target themselves.
     *
     * @param allow true if self-targeting is allowed
     * @return this builder
     */
    public SkillBuilder allowSelfTarget(boolean allow) {
        this.allowSelfTarget = allow;
        return this;
    }

    /**
     * Set whether this skill can only target allies.
     * When true, non-allies will be filtered out during targeting.
     *
     * @param allyOnly true if only allies can be targeted
     * @return this builder
     */
    public SkillBuilder targetAllyOnly(boolean allyOnly) {
        this.targetAllyOnly = allyOnly;
        return this;
    }

    /**
     * Set whether this skill can only target players.
     * When true, mobs and other non-player entities will be filtered out.
     *
     * @param playerOnly true if only players can be targeted
     * @return this builder
     */
    public SkillBuilder targetPlayerOnly(boolean playerOnly) {
        this.targetPlayerOnly = playerOnly;
        return this;
    }

    /**
     * Set the maximum level for this skill.
     * Default is 1, meaning the skill cannot be leveled up.
     *
     * @param maxLevel The maximum level (must be >= 1)
     * @return this builder
     * @throws IllegalArgumentException if maxLevel < 1
     */
    public SkillBuilder setMaxLevel(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Max level must be at least 1, got: " + maxLevel);
        }
        this.maxLevel = maxLevel;
        return this;
    }

    /**
     * Add a scaled stat that varies by skill level.
     * Values are indexed by level: index 0 = level 1, index 1 = level 2, etc.
     * If the skill level exceeds the array length, the last value is used.
     *
     * @param name The stat name (e.g., "damage", "duration", "range")
     * @param values Array of values per level (must have at least one value)
     * @return this builder
     * @throws IllegalArgumentException if values is null or empty
     */
    public SkillBuilder scaledStat(String name, double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Scaled stat values must have at least one element");
        }
        this.scaledStats.put(name, values);
        return this;
    }

    /**
     * Register the channeled execution handler.
     * This is called for channeling skills with the channel time as a parameter.
     * The handler receives the Skill.ExecutionContext, the raw ServerPlayer, and the channel time in seconds.
     */
    public SkillBuilder onChannel(ChanneledHandler handler) {
        this.channeledHandler = handler;
        return this;
    }

    /**
     * Register a validation handler that runs before the skill is cast.
     * This allows custom conditions to be checked before skill execution.
     * <p>
     * The validation handler receives the Skill.ExecutionContext and the raw ServerPlayer.
     * It should return {@code true} if the skill can be cast, or {@code false} to cancel the cast.
     * <p>
     * Example use cases:
     * <ul>
     *   <li>Checking if player is in a specific biome</li>
     *   <li>Checking if player is holding a required item</li>
     *   <li>Checking time of day or weather conditions</li>
     *   <li>Checking player health or other status</li>
     * </ul>
     *
     * <pre>{@code
     * SkillBuilder.create("modid", "fireball")
     *     .validate((context, player) -> {
     *         // Cannot cast underwater
     *         if (player.isInWater()) return false;
     *         // Must have item in hand
     *         return !player.getMainHandItem().isEmpty();
     *     })
     *     .onActive((context, player) -> { ... })
     *     .register();
     * }</pre>
     *
     * @param handler The validation handler that returns true if casting is allowed
     * @return this builder
     */
    public SkillBuilder validate(BiFunction<Skill.ExecutionContext, Object, Boolean> handler) {
        this.validationHandler = handler;
        return this;
    }

    /**
     * Add a passive stack type to this skill.
     * <p>
     * Passive stacks are tracked per-player and can be used for mechanics like:
     * - Stacks that build up over time
     * - Stacks gained/lost on events
     * - Conditional effects based on stack count
     * </p>
     * <p>
     * The skill's event handlers are responsible for all stack logic
     * (generation, decay, triggers). This only defines the stack type.
     * </p>
     *
     * @param stackName The unique name for this stack type (e.g., "heat", "charge")
     * @param def       The stack definition with max stacks and display info
     * @return this builder
     */
    public SkillBuilder passiveStack(String stackName, PassiveStackDef def) {
        this.passiveStacks.put(stackName, def);
        return this;
    }

    /**
     * Build the skill and return a BuiltSkill instance.
     * Call SkillRegistry.register() with the result to register it.
     */
    public BuiltSkill build() {
        return new BuiltSkill(this);
    }

    /**
     * Build and register the skill in one step.
     * Also registers passive stack definitions with the shared PassiveStackRegistry.
     *
     * @return The registered BuiltSkill instance
     */
    public BuiltSkill register() {
        BuiltSkill skill = build();
        SkillRegistry.getInstance().register(skill);

        // Register passive stack definitions with shared registry
        for (java.util.Map.Entry<String, PassiveStackDef> entry : passiveStacks.entrySet()) {
            PassiveStackRegistry.register(skill, entry.getKey(), entry.getValue());
        }

        return skill;
    }

    // Package-private getters for BuiltSkill
    ResourceLocation getId() { return id; }
    SkillNature getNature() { return nature; }
    TargetType getTargetingType() { return targetingType; }
    double getMaxRange() { return maxRange; }
    double getActiveCooldown() { return activeCooldown; }
    double getPassiveCooldown() { return passiveCooldown; }
    double getResourceCost() { return resourceCost; }
    ResourceLocation getResourceType() { return resourceType; }
    boolean isToggleable() { return toggleable; }
    double getToggleCostPerTick() { return toggleCostPerTick; }
    boolean isAllowSelfTarget() { return allowSelfTarget; }
    boolean isTargetAllyOnly() { return targetAllyOnly; }
    boolean isTargetPlayerOnly() { return targetPlayerOnly; }
    double getMinChannelTime() { return minChannelTime; }
    double getMaxChannelTime() { return maxChannelTime; }
    BiConsumer<Skill.ExecutionContext, Object> getActiveHandler() { return activeHandler; }
    Consumer<Object> getPassiveHandler() { return passiveHandler; }
    ChanneledHandler getChanneledHandler() { return channeledHandler; }
    BiFunction<Skill.ExecutionContext, Object, Boolean> getValidationHandler() { return validationHandler; }
    int getMaxLevel() { return maxLevel; }
    java.util.Map<String, double[]> getScaledStats() { return new java.util.HashMap<>(scaledStats); }
    java.util.Map<String, PassiveStackDef> getPassiveStacks() { return new java.util.HashMap<>(passiveStacks); }

    private ResourceLocation parseResourceLocation(String resourceType) {
        if (resourceType == null || resourceType.isEmpty()) {
            return null;
        }
        if (resourceType.contains(":")) {
            return ResourceLocation.tryParse(resourceType);
        }
        // Default to irons_spellbooks namespace if none provided
        return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", resourceType);
    }
}
