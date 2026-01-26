package com.complextalents.elemental.api;

import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalReaction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Context object that encapsulates all data needed for reaction execution.
 * This follows the Parameter Object pattern to reduce method parameters
 * and make the API more flexible for future extensions.
 */
public class ReactionContext {

    private final LivingEntity target;
    private final ServerPlayer attacker;
    private final ElementalReaction reaction;
    private final ElementType triggeringElement;
    private final ElementType existingElement;
    private final float damageMultiplier;
    private final float elementalMastery;
    private final ServerLevel level;

    // Additional context data that strategies might need
    private final Map<String, Object> additionalData;

    // Cached calculations to avoid recalculation
    private Float cachedDamage;
    private Boolean canTrigger;

    private ReactionContext(Builder builder) {
        this.target = builder.target;
        this.attacker = builder.attacker;
        this.reaction = builder.reaction;
        this.triggeringElement = builder.triggeringElement;
        this.existingElement = builder.existingElement;
        this.damageMultiplier = builder.damageMultiplier;
        this.elementalMastery = builder.elementalMastery;
        this.level = builder.level;
        this.additionalData = new HashMap<>(builder.additionalData);
    }

    // Getters
    public LivingEntity getTarget() {
        return target;
    }

    public ServerPlayer getAttacker() {
        return attacker;
    }

    public ElementalReaction getReaction() {
        return reaction;
    }

    public ElementType getTriggeringElement() {
        return triggeringElement;
    }

    public ElementType getExistingElement() {
        return existingElement;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public float getElementalMastery() {
        return elementalMastery;
    }

    /**
     * Gets additional data stored in the context.
     *
     * @param key The data key
     * @param type The expected type
     * @param <T> The type parameter
     * @return The data value, or null if not found or wrong type
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getData(String key, Class<T> type) {
        Object value = additionalData.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Stores additional data in the context.
     *
     * @param key The data key
     * @param value The data value
     */
    public void setData(String key, Object value) {
        additionalData.put(key, value);
    }

    /**
     * Checks if additional data exists.
     *
     * @param key The data key
     * @return true if data exists for the key
     */
    public boolean hasData(String key) {
        return additionalData.containsKey(key);
    }

    /**
     * Caches the calculated damage to avoid recalculation.
     *
     * @param damage The calculated damage
     */
    public void setCachedDamage(float damage) {
        this.cachedDamage = damage;
    }

    /**
     * Gets the cached damage if available.
     *
     * @return The cached damage, or null if not calculated yet
     */
    @Nullable
    public Float getCachedDamage() {
        return cachedDamage;
    }

    /**
     * Caches whether the reaction can trigger.
     *
     * @param canTrigger Whether the reaction can trigger
     */
    public void setCanTrigger(boolean canTrigger) {
        this.canTrigger = canTrigger;
    }

    /**
     * Gets the cached trigger check result if available.
     *
     * @return The cached result, or null if not checked yet
     */
    @Nullable
    public Boolean getCanTrigger() {
        return canTrigger;
    }

    /**
     * Creates a new builder for ReactionContext.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for ReactionContext.
     * Uses the Builder pattern for flexible object construction.
     */
    public static class Builder {
        private LivingEntity target;
        private ServerPlayer attacker;
        private ElementalReaction reaction;
        private ElementType triggeringElement;
        private ElementType existingElement;
        private float damageMultiplier;
        private float elementalMastery;
        private ServerLevel level;
        private final Map<String, Object> additionalData = new HashMap<>();

        public Builder target(LivingEntity target) {
            this.target = target;
            return this;
        }

        public Builder attacker(ServerPlayer attacker) {
            this.attacker = attacker;
            return this;
        }

        public Builder reaction(ElementalReaction reaction) {
            this.reaction = reaction;
            return this;
        }

        public Builder triggeringElement(ElementType element) {
            this.triggeringElement = element;
            return this;
        }

        public Builder existingElement(ElementType element) {
            this.existingElement = element;
            return this;
        }

        public Builder damageMultiplier(float multiplier) {
            this.damageMultiplier = multiplier;
            return this;
        }

        public Builder level(ServerLevel level) {
            this.level = level;
            return this;
        }

        public Builder elementalMastery(float mastery) {
            this.elementalMastery = mastery;
            return this;
        }

        public Builder withData(String key, Object value) {
            this.additionalData.put(key, value);
            return this;
        }

        /**
         * Builds the ReactionContext.
         * Validates that all required fields are set.
         *
         * @return The built ReactionContext
         * @throws IllegalStateException if required fields are missing
         */
        public ReactionContext build() {
            if (target == null) {
                throw new IllegalStateException("Target is required for ReactionContext");
            }
            if (attacker == null) {
                throw new IllegalStateException("Attacker is required for ReactionContext");
            }
            if (reaction == null) {
                throw new IllegalStateException("Reaction type is required for ReactionContext");
            }
            if (triggeringElement == null) {
                throw new IllegalStateException("Triggering element is required for ReactionContext");
            }
            if (level == null && target != null) {
                // Try to get level from target
                if (target.level() instanceof ServerLevel serverLevel) {
                    this.level = serverLevel;
                } else {
                    throw new IllegalStateException("ServerLevel is required for ReactionContext");
                }
            }

            return new ReactionContext(this);
        }
    }
}