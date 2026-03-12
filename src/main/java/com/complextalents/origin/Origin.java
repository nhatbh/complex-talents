package com.complextalents.origin;

import com.complextalents.origin.client.OriginRenderer;
import com.complextalents.passive.PassiveOwner;
import com.complextalents.passive.PassiveStackDef;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Interface for origins.
 * <p>
 * An origin represents a player archetype that provides:
 * - A resource type (shared across origins that use it)
 * - Scaled stats that vary by origin level
 * - Event-driven behaviors (handled by separate event handler classes)
 * - Passive stacks (managed by shared passive system)
 */
public interface Origin extends PassiveOwner {

    /**
     * Unique identifier for this origin (e.g., "complextalents:cleric")
     */
    ResourceLocation getId();

    /**
     * Get the owner ID for passive stack registration.
     * Implemented as part of PassiveOwner interface.
     *
     * @return The origin ID
     */
    @Override
    default ResourceLocation getOwnerId() {
        return getId();
    }

    /**
     * Get the owner type for passive stack registration.
     * Implemented as part of PassiveOwner interface.
     *
     * @return "origin"
     */
    @Override
    default String getOwnerType() {
        return "origin";
    }

    /**
     * Display name for this origin.
     */
    Component getDisplayName();

    /**
     * Description of this origin's mechanics.
     */
    Component getDescription();

    /**
     * The resource type this origin uses.
     * Multiple origins can share the same resource type.
     *
     * @return The resource type, or null if this origin has no resource
     */
    ResourceType getResourceType();

    /**
     * Get the maximum resource value for a specific origin level.
     * Default implementation returns the resource type's fixed max.
     * Implementations can override to use scaling arrays.
     *
     * @param level The origin level
     * @return The maximum resource value
     */
    default double getMaxResource(int level) {
        ResourceType type = getResourceType();
        return type != null ? type.getMax() : 0;
    }

    /**
     * Get the maximum resource value for a specific origin level, taking into account the player's current state.
     *
     * @param level The origin level
     * @param player The player instance
     * @return The maximum resource value
     */
    default double getMaxResource(int level, net.minecraft.server.level.ServerPlayer player) {
        return getMaxResource(level);
    }

    /**
     * Maximum level this origin can be upgraded to.
     * Default is 1 (no leveling).
     */
    int getMaxLevel();

    /**
     * Get a scaled stat value for a specific level.
     * If the level exceeds the defined values, the last value is used.
     *
     * @param statName The stat name (e.g., "pietyOnHit")
     * @param level    The origin level
     * @return The scaled stat value, or 0 if not found
     */
    double getScaledStat(String statName, int level);

    /**
     * Get passive stack definitions for this origin.
     * Returns map of stack name -> definition.
     *
     * @return Map of passive stack definitions (empty by default)
     */
    default Map<String, PassiveStackDef> getPassiveStacks() {
        return Map.of();
    }

    /**
     * Get a specific passive stack definition.
     *
     * @param stackName The stack type name
     * @return The stack definition, or null if not found
     */
    @Nullable
    default PassiveStackDef getPassiveStackDef(String stackName) {
        return null;
    }

    /**
     * Get the custom HUD renderer for this origin.
     * If null, the default HUD renderer is used.
     *
     * @return The custom renderer, or null for default HUD
     */
    @OnlyIn(Dist.CLIENT)
    @Nullable
    default OriginRenderer getRenderer() {
        return null;
    }
}
