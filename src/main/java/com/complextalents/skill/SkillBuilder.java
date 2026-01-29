package com.complextalents.skill;

import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builder for creating skill definitions.
 * Provides fluent API for skill registration.
 */
public class SkillBuilder {
    private ResourceLocation id;
    private SkillNature nature = SkillNature.ACTIVE;
    private TargetingType targetingType = TargetingType.NONE;
    private double maxRange = 32.0;
    private double activeCooldown = 0.0;
    private double passiveCooldown = 0.0;
    private double resourceCost = 0.0;
    private ResourceLocation resourceType;
    private boolean toggleable = false;
    private double toggleCostPerTick = 0.0;

    // Execution handlers
    private BiConsumer<Skill.ExecutionContext, Object> activeHandler;
    private Consumer<Object> passiveHandler;
    private ChanneledHandler channeledHandler;

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
    public SkillBuilder targeting(TargetingType type) {
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
     * Register the channeled execution handler.
     * This is called for channeling skills with the channel time as a parameter.
     * The handler receives the Skill.ExecutionContext, the raw ServerPlayer, and the channel time in seconds.
     */
    public SkillBuilder onChannel(ChanneledHandler handler) {
        this.channeledHandler = handler;
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
     *
     * @return The registered BuiltSkill instance
     */
    public BuiltSkill register() {
        BuiltSkill skill = build();
        SkillRegistry.getInstance().register(skill);
        return skill;
    }

    // Package-private getters for BuiltSkill
    ResourceLocation getId() { return id; }
    SkillNature getNature() { return nature; }
    TargetingType getTargetingType() { return targetingType; }
    double getMaxRange() { return maxRange; }
    double getActiveCooldown() { return activeCooldown; }
    double getPassiveCooldown() { return passiveCooldown; }
    double getResourceCost() { return resourceCost; }
    ResourceLocation getResourceType() { return resourceType; }
    boolean isToggleable() { return toggleable; }
    double getToggleCostPerTick() { return toggleCostPerTick; }
    double getMinChannelTime() { return minChannelTime; }
    double getMaxChannelTime() { return maxChannelTime; }
    BiConsumer<Skill.ExecutionContext, Object> getActiveHandler() { return activeHandler; }
    Consumer<Object> getPassiveHandler() { return passiveHandler; }
    ChanneledHandler getChanneledHandler() { return channeledHandler; }

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
