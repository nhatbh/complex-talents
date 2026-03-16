package com.complextalents.origin.client;

import com.complextalents.origin.ResourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side cache of player origin data.
 * Used for HUD rendering and UI display.
 * Mirrors ClientSkillData pattern.
 */
@OnlyIn(Dist.CLIENT)
public class ClientOriginData {

    private static ResourceLocation originId = null;
    private static int originLevel = 1;
    private static double resourceValue = 0;
    private static double resourceMax = 0;
    private static ResourceLocation resourceTypeId = null;
    private static double shieldValue = 0;
    private static double shieldMax = 0;

    /**
     * Sync origin data from the server.
     *
     * @param origin           The active origin ID (null if none)
     * @param level            The origin level
     * @param resourceVal      The current resource value
     * @param resMax           The maximum resource value
     * @param resTypeId        The resource type ID
     */
    public static void syncFromServer(@Nullable ResourceLocation origin, int level,
                                       double resourceVal, double resMax, @Nullable ResourceLocation resTypeId,
                                       double sVal, double sMax) {
        originId = origin;
        originLevel = level;
        resourceValue = resourceVal;
        resourceMax = resMax;
        resourceTypeId = resTypeId;
        shieldValue = sVal;
        shieldMax = sMax;
    }


    public static double getShieldValue() {
        return shieldValue;
    }

    public static double getShieldMax() {
        return shieldMax;
    }

    /**
     * Get the active origin ID.
     *
     * @return The origin ID, or null if no origin is active
     */
    @Nullable
    public static ResourceLocation getOriginId() {
        return originId;
    }

    /**
     * Get the origin level.
     *
     * @return The origin level
     */
    public static int getOriginLevel() {
        return originLevel;
    }

    /**
     * Get the current resource value.
     *
     * @return The current resource value
     */
    public static double getResourceValue() {
        return resourceValue;
    }

    /**
     * Get the maximum resource value.
     *
     * @return The maximum resource value
     */
    public static double getResourceMax() {
        return resourceMax;
    }

    /**
     * Get the resource type ID.
     *
     * @return The resource type ID, or null if no resource type
     */
    @Nullable
    public static ResourceLocation getResourceTypeId() {
        return resourceTypeId;
    }

    /**
     * Get the resource type from the registry.
     *
     * @return The resource type, or null if no origin or the origin has no resource
     */
    @Nullable
    public static ResourceType getResourceType() {
        if (resourceTypeId == null) {
            return null;
        }
        return ResourceType.get(resourceTypeId);
    }

    /**
     * Check if the player has an active origin.
     *
     * @return true if an origin is active
     */
    public static boolean hasOrigin() {
        return originId != null;
    }

    /**
     * Clear all origin data.
     */
    public static void clear() {
        originId = null;
        originLevel = 1;
        resourceValue = 0;
        resourceMax = 0;
        resourceTypeId = null;
    }
}
