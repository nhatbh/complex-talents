package com.complextalents.skill;

import com.complextalents.passive.PassiveOwner;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base interface for all skills.
 * All skills must implement this interface to be registered.
 */
public interface Skill extends PassiveOwner {

    /**
     * Unique identifier for this skill (e.g., "complextalents:fireball")
     */
    ResourceLocation getId();

    /**
     * Get the owner ID for passive stack registration.
     * Implemented as part of PassiveOwner interface.
     *
     * @return The skill ID
     */
    @Override
    default ResourceLocation getOwnerId() {
        return getId();
    }

    /**
     * Get the owner type for passive stack registration.
     * Implemented as part of PassiveOwner interface.
     *
     * @return "skill"
     */
    @Override
    default String getOwnerType() {
        return "skill";
    }

    /**
     * The nature of this skill (passive, active, both, toggle)
     */
    SkillNature getNature();

    /**
     * The targeting type for the active component
     */
    TargetType getTargetingType();

    /**
     * Display name for this skill (translatable)
     * Format: skill.{namespace}.{path}
     */
    Component getDisplayName();

    /**
     * Description for this skill (translatable)
     * Format: skill.{namespace}.{path}.desc
     */
    Component getDescription();

    /**
     * Get the icon texture for this skill.
     * Returns null to use the default icon.
     *
     * @return The icon texture location, or null for default
     */
    @Nullable
    ResourceLocation getIcon();

    /**
     * Maximum range for targeting (blocks)
     */
    double getMaxRange();

    /**
     * Maximum level for this skill.
     * Default is 1, meaning the skill cannot be leveled up.
     */
    int getMaxLevel();

    /**
     * Cooldown in seconds for the active component
     * For hybrid skills, this is the active cooldown only
     */
    double getActiveCooldown();

    /**
     * Get the active cooldown for a specific skill level.
     * Default implementation returns the fixed cooldown.
     * Implementations can override to use scaling arrays.
     *
     * @param level The skill level
     * @return The cooldown in seconds
     */
    default double getActiveCooldown(int level) {
        return getActiveCooldown();
    }

    /**
     * Cooldown in seconds for the passive trigger
     * Only used for hybrid skills with passive cooldowns
     */
    double getPassiveCooldown();

    /**
     * Get the passive cooldown for a specific skill level.
     * Default implementation returns the fixed cooldown.
     * Implementations can override to use scaling arrays.
     *
     * @param level The skill level
     * @return The cooldown in seconds
     */
    default double getPassiveCooldown(int level) {
        return getPassiveCooldown();
    }

    /**
     * Resource cost for active cast (mana, energy, etc.)
     * Returns 0 if no resource cost
     */
    double getResourceCost();

    /**
     * Get the resource cost for a specific skill level.
     * Default implementation returns the fixed cost.
     * Implementations can override to use scaling arrays.
     *
     * @param level The skill level
     * @return The resource cost
     */
    default double getResourceCost(int level) {
        return getResourceCost();
    }

    /**
     * Resource ID for cost (e.g., "irons_spellbooks:mana")
     * Returns null if no resource cost
     */
    @Nullable ResourceLocation getResourceType();

    /**
     * Whether this skill can be toggled
     * Only applies when nature == TOGGLE
     */
    boolean isToggleable();

    /**
     * Whether this skill allows targeting the caster.
     * Only applies when using ENTITY targeting.
     * When true, the player can target themselves.
     * When false, the player cannot target themselves.
     */
    boolean allowsSelfTarget();

    /**
     * Whether this skill can only target allies.
     * Only applies when using ENTITY targeting.
     * When true, non-allies will be filtered out.
     */
    boolean targetsAllyOnly();

    /**
     * Whether this skill can only target players.
     * Only applies when using ENTITY targeting.
     * When true, mobs and other non-player entities will be filtered out.
     */
    boolean targetsPlayerOnly();

    /**
     * Resource cost per tick while toggled on
     */
    double getToggleCostPerTick();

    /**
     * Get the toggle cost per tick for a specific skill level.
     * Default implementation returns the fixed cost.
     * Implementations can override to use scaling arrays.
     *
     * @param level The skill level
     * @return The cost per tick
     */
    default double getToggleCostPerTick(int level) {
        return getToggleCostPerTick();
    }

    /**
     * Maximum duration in seconds that a toggle can stay active.
     * Returns 0 if the toggle has no maximum duration (stays on until manually toggled).
     * When this duration is reached, the toggle will automatically turn off and cooldown will start.
     *
     * @return Maximum toggle duration in seconds, or 0 for unlimited
     */
    double getToggleMaxDuration();

    /**
     * Check if this skill has a toggle-off handler.
     * The toggle-off handler is called when the skill is toggled off (manually or automatically).
     *
     * @return true if a toggle-off handler is present
     */
    boolean hasToggleOffHandler();

    /**
     * Execute the toggle-off handler for this skill.
     * Called when the skill is toggled off (manually or automatically).
     *
     * @param player The player who toggled the skill off
     */
    void executeToggleOff(Object player);

    /**
     * Minimum channel time in seconds.
     * Returns 0 if this skill doesn't require channeling.
     */
    double getMinChannelTime();

    /**
     * Maximum channel time in seconds.
     * Returns 0 if this skill doesn't use channeling.
     */
    double getMaxChannelTime();

    /**
     * Check if this skill uses channeling.
     */
    boolean isChanneling();

    /**
     * Execute the active effect of this skill.
     * Called by SkillExecutionHandler during SkillExecuteEvent.
     *
     * @param context The execution context containing player and target data
     */
    void executeActive(ExecutionContext context);

    /**
     * Execute the channeled effect with given duration.
     * Only called for channeling skills.
     *
     * @param context The execution context containing player and target data
     * @param channelTime The channel time in seconds
     */
    void executeChanneled(ExecutionContext context, double channelTime);

    /**
     * Check if this skill has an active execution handler.
     */
    boolean hasActiveHandler();

    /**
     * Check if this skill can be cast by the player in the current context.
     * This runs any custom validation registered via SkillBuilder.validate().
     * <p>
     * Returns true by default for skills without custom validation.
     *
     * @param context The execution context containing player and target data
     * @return true if the skill can be cast, false otherwise
     */
    default boolean canCast(ExecutionContext context) {
        return true;
    }

    /**
     * Get passive stack definitions for this skill.
     * Returns map of stack name -> definition.
     *
     * @return Map of passive stack definitions (empty by default)
     */
    default java.util.Map<String, com.complextalents.passive.PassiveStackDef> getPassiveStacks() {
        return java.util.Map.of();
    }

    /**
     * Get a specific passive stack definition.
     *
     * @param stackName The stack type name
     * @return The stack definition, or null if not found
     */
    @org.jetbrains.annotations.Nullable
    default com.complextalents.passive.PassiveStackDef getPassiveStackDef(String stackName) {
        return null;
    }

    /**
     * Context object passed to skills during execution.
     * Contains player, target, skill reference, skill level, and channel time for stat resolution.
     */
    record ExecutionContext(
            ServerPlayerWrapper player,
            ResolvedTargetWrapper target,
            ResourceLocation skillId,
            int skillLevel,
            double channelTime
    ) {
        /**
         * Create an execution context.
         *
         * @param player The server player casting the skill
         * @param target The resolved target data
         * @param skillId The ID of the skill being executed
         * @param skillLevel The player's level for this skill
         * @param channelTime The channel time in seconds (0 for instant skills)
         */
        public ExecutionContext {
            if (player == null) {
                throw new IllegalArgumentException("Player cannot be null");
            }
            if (skillId == null) {
                throw new IllegalArgumentException("Skill ID cannot be null");
            }
            if (skillLevel < 1) {
                throw new IllegalArgumentException("Skill level must be at least 1");
            }
            if (channelTime < 0) {
                throw new IllegalArgumentException("Channel time cannot be negative");
            }
        }

        /**
         * Get the channel time for this skill execution.
         * For instant skills, this returns 0.
         * For channeled skills, this returns the channel duration in seconds.
         *
         * @return The channel time in seconds
         */
        public double channelTime() {
            return channelTime;
        }

        /**
         * Get a scaled stat value for this skill level.
         * Uses the skill's scaled stat configuration to resolve the value.
         *
         * @param statName The name of the stat (e.g., "damage", "duration")
         * @return The resolved stat value, or 0 if not found
         */
        public double getStat(String statName) {
            var skill = com.complextalents.skill.SkillRegistry.getInstance().getSkill(skillId);
            if (skill instanceof BuiltSkill builtSkill) {
                return builtSkill.getScaledStat(statName, skillLevel);
            }
            return 0.0;
        }

        // ========== Origin Integration ==========

        /**
         * Get the player's current passive stack count.
         * Convenience method for skills that check passive stacks from origins or skills.
         *
         * @param stackTypeName The passive stack type name (e.g., "grace")
         * @return Current stack count, or 0 if not found
         */
        public int getPassiveStacks(String stackTypeName) {
            var player = player().getAs(net.minecraft.server.level.ServerPlayer.class);
            return com.complextalents.passive.PassiveManager.getPassiveStacks(player, stackTypeName);
        }

        /**
         * Check if player has at least a certain number of passive stacks.
         *
         * @param stackTypeName The passive stack type name
         * @param threshold Minimum stacks required
         * @return true if player has at least the threshold
         */
        public boolean hasPassiveStacks(String stackTypeName, int threshold) {
            return getPassiveStacks(stackTypeName) >= threshold;
        }

        /**
         * Check if player is at max stacks for a type.
         *
         * @param stackTypeName The passive stack type name
         * @return true if at max stacks
         */
        public boolean isAtMaxPassiveStacks(String stackTypeName) {
            var player = player().getAs(net.minecraft.server.level.ServerPlayer.class);
            return com.complextalents.passive.PassiveManager.isAtMaxPassiveStacks(player, null, stackTypeName);
        }

        /**
         * Get the player's origin resource value.
         * Convenience method for skills that check origin resources.
         *
         * @return Current resource value
         */
        public double getResource() {
            var player = player().getAs(net.minecraft.server.level.ServerPlayer.class);
            return com.complextalents.origin.OriginManager.getResource(player);
        }

        /**
         * Check if player has enough of their origin resource.
         *
         * @param amount Minimum resource required
         * @return true if player has at least the amount
         */
        public boolean hasResource(double amount) {
            return getResource() >= amount;
        }
    }

    /**
     * Wrapper for ServerPlayer to avoid direct dependency in interface.
     * This allows the interface to remain clean while still providing access.
     */
    class ServerPlayerWrapper {
        private final Object player;

        public ServerPlayerWrapper(Object player) {
            this.player = player;
        }

        public Object get() {
            return player;
        }

        @SuppressWarnings("unchecked")
        public <T> T getAs(Class<T> type) {
            return (T) player;
        }
    }

    /**
     * Wrapper for ResolvedTargetData to avoid circular dependencies.
     */
    class ResolvedTargetWrapper {
        private final Object targetData;

        public ResolvedTargetWrapper(Object targetData) {
            this.targetData = targetData;
        }

        public Object get() {
            return targetData;
        }

        @SuppressWarnings("unchecked")
        public <T> T getAs(Class<T> type) {
            return (T) targetData;
        }
    }
}
