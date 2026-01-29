package com.complextalents.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Centralized utility for validating entities in various contexts.
 *
 * <p>This provides common validation logic used by both the targeting system
 * and the skill system for entity validation.</p>
 */
public final class EntityValidationHelper {

    private EntityValidationHelper() {}

    /**
     * Validate that an entity exists and is alive.
     *
     * @param level The level to look up the entity in
     * @param entityId The entity ID to validate
     * @return The validated entity, or null if invalid
     */
    public static Entity validateEntity(Level level, int entityId) {
        if (entityId == -1) {
            return null;
        }

        Entity entity = level.getEntity(entityId);

        if (entity == null || !entity.isAlive()) {
            return null;
        }

        return entity;
    }

    /**
     * Validate that an entity exists and is alive, with a result object.
     *
     * @param level The level to look up the entity in
     * @param entityId The entity ID to validate
     * @return A validation result
     */
    public static ValidationResult validateEntityWithResult(Level level, int entityId) {
        if (entityId == -1) {
            return ValidationResult.invalid("Entity ID not provided");
        }

        Entity entity = level.getEntity(entityId);

        if (entity == null) {
            return ValidationResult.invalid("Target entity not found");
        }

        if (!entity.isAlive()) {
            return ValidationResult.invalid("Target entity is dead");
        }

        return ValidationResult.valid(entity);
    }

    /**
     * Check if an entity is valid for targeting purposes.
     *
     * @param entity The entity to check
     * @return true if the entity is valid for targeting
     */
    public static boolean isValidTarget(Entity entity) {
        return entity != null && entity.isAlive();
    }

    /**
     * Simple validation result holder.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final Entity entity;

        private ValidationResult(boolean valid, String reason, Entity entity) {
            this.valid = valid;
            this.reason = reason;
            this.entity = entity;
        }

        public static ValidationResult valid(Entity entity) {
            return new ValidationResult(true, null, entity);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
