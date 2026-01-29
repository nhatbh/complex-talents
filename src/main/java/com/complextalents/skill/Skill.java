package com.complextalents.skill;

import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base interface for all skills.
 * All skills must implement this interface to be registered.
 */
public interface Skill {

    /**
     * Unique identifier for this skill (e.g., "complextalents:fireball")
     */
    ResourceLocation getId();

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
     * Cooldown in seconds for the passive trigger
     * Only used for hybrid skills with passive cooldowns
     */
    double getPassiveCooldown();

    /**
     * Resource cost for active cast (mana, energy, etc.)
     * Returns 0 if no resource cost
     */
    double getResourceCost();

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
     * Context object passed to skills during execution.
     * Contains player, target, skill reference, and skill level for stat resolution.
     */
    record ExecutionContext(
            ServerPlayerWrapper player,
            ResolvedTargetWrapper target,
            ResourceLocation skillId,
            int skillLevel
    ) {
        /**
         * Create an execution context.
         *
         * @param player The server player casting the skill
         * @param target The resolved target data
         * @param skillId The ID of the skill being executed
         * @param skillLevel The player's level for this skill
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
