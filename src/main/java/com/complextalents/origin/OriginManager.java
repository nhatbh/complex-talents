package com.complextalents.origin;

import com.complextalents.origin.capability.IPlayerOriginData;
import com.complextalents.origin.capability.OriginDataProvider;
import com.complextalents.origin.events.OriginChangeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

/**
 * Central API for origin operations.
 * Mirrors SkillFormManager pattern.
 * <p>
 * All origin operations go through this class.
 * Passive stack operations are now handled by {@link com.complextalents.passive.PassiveManager}.
 * </p>
 */
public class OriginManager {

    /**
     * Set a player's origin.
     *
     * @param player   The player
     * @param originId The origin ID
     * @param level    The origin level (defaults to 1 if less than 1)
     */
    public static void setOrigin(ServerPlayer player, ResourceLocation originId, int level) {
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            ResourceLocation oldOriginId = data.getActiveOrigin();
            int oldLevel = data.getOriginLevel();
            int newLevel = Math.max(1, level);

            data.setActiveOrigin(originId);
            data.setOriginLevel(newLevel);
            data.setResource(data.getResourceType().getMin());
            data.sync();

            // Fire origin change event
            OriginChangeEvent.ChangeType changeType = (oldOriginId == null)
                ? OriginChangeEvent.ChangeType.SET
                : OriginChangeEvent.ChangeType.SET;
            MinecraftForge.EVENT_BUS.post(new OriginChangeEvent(player, originId, oldLevel, newLevel, changeType));
        });
    }

    /**
     * Set a player's origin with default level 1.
     *
     * @param player   The player
     * @param originId The origin ID
     */
    public static void setOrigin(ServerPlayer player, ResourceLocation originId) {
        setOrigin(player, originId, 1);
    }

    /**
     * Clear a player's origin.
     *
     * @param player The player
     */
    public static void clearOrigin(ServerPlayer player) {
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            int oldLevel = data.getOriginLevel();

            data.clear();
            data.sync();

            // Fire origin change event
            MinecraftForge.EVENT_BUS.post(new OriginChangeEvent(
                player, null, oldLevel, 0, OriginChangeEvent.ChangeType.CLEAR));
        });
    }

    /**
     * Get a player's origin.
     *
     * @param player The player
     * @return The origin, or null if the player has no origin
     */
    @Nullable
    public static Origin getOrigin(ServerPlayer player) {
        ResourceLocation originId = getOriginId(player);
        if (originId == null) {
            return null;
        }
        return OriginRegistry.getInstance().getOrigin(originId);
    }

    /**
     * Get a player's origin ID.
     *
     * @param player The player
     * @return The origin ID, or null if the player has no origin
     */
    @Nullable
    public static ResourceLocation getOriginId(ServerPlayer player) {
        return player.getCapability(OriginDataProvider.ORIGIN_DATA)
                .resolve()
                .flatMap(data -> java.util.Optional.ofNullable(data.getActiveOrigin()))
                .orElse(null);
    }

    /**
     * Check if a player has an origin.
     *
     * @param player The player
     * @return true if the player has an origin
     */
    public static boolean hasOrigin(ServerPlayer player) {
        return getOriginId(player) != null;
    }

    /**
     * Get a player's origin level.
     *
     * @param player The player
     * @return The origin level, or 1 if the player has no origin
     */
    public static int getOriginLevel(ServerPlayer player) {
        return player.getCapability(OriginDataProvider.ORIGIN_DATA)
                .map(IPlayerOriginData::getOriginLevel)
                .orElse(1);
    }

    /**
     * Set a player's origin level.
     *
     * @param player The player
     * @param level  The new level (clamped to origin's max level)
     */
    public static void setOriginLevel(ServerPlayer player, int level) {
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            Origin origin = getOrigin(player);
            int maxLevel = origin != null ? origin.getMaxLevel() : 1;
            int oldLevel = data.getOriginLevel();
            int newLevel = Math.max(1, Math.min(level, maxLevel));

            data.setOriginLevel(newLevel);
            data.sync();

            // Fire origin change event if level actually changed
            if (oldLevel != newLevel) {
                ResourceLocation originId = data.getActiveOrigin();
                MinecraftForge.EVENT_BUS.post(new OriginChangeEvent(
                    player, originId, oldLevel, newLevel, OriginChangeEvent.ChangeType.LEVEL_CHANGE));
            }
        });
    }

    /**
     * Add experience to a player's origin level.
     * TODO: Implement XP-to-level progression logic.
     *
     * @param player The player
     * @param xp     The experience to add
     */
    public static void addOriginExperience(ServerPlayer player, double xp) {
        // TODO: Implement XP system for origins
    }

    /**
     * Get the resource type for a player's active origin.
     *
     * @param player The player
     * @return The resource type, or null if the player has no origin or the origin has no resource
     */
    @Nullable
    public static ResourceType getResourceType(ServerPlayer player) {
        return player.getCapability(OriginDataProvider.ORIGIN_DATA)
                .resolve()
                .flatMap(data -> java.util.Optional.ofNullable(data.getResourceType()))
                .orElse(null);
    }

    /**
     * Get a player's current resource value.
     *
     * @param player The player
     * @return The current resource value, or 0 if the player has no origin
     */
    public static double getResource(ServerPlayer player) {
        return player.getCapability(OriginDataProvider.ORIGIN_DATA)
                .map(IPlayerOriginData::getResource)
                .orElse(0.0);
    }

    /**
     * Set a player's resource value.
     * The value is clamped to the resource type's min/max range.
     *
     * @param player The player
     * @param value  The new value
     */
    public static void setResource(ServerPlayer player, double value) {
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            data.setResource(value);
            data.sync();
        });
    }

    /**
     * Modify a player's resource value.
     * The final value is clamped to the resource type's min/max range.
     *
     * @param player The player
     * @param delta  The amount to add (can be negative)
     */
    public static void modifyResource(ServerPlayer player, double delta) {
        player.getCapability(OriginDataProvider.ORIGIN_DATA).ifPresent(data -> {
            data.modifyResource(delta);
            // Sync is handled in modifyResource if value changed
        });
    }

    /**
     * Check if a player has enough of a resource.
     *
     * @param player The player
     * @param amount The amount required
     * @return true if the player has at least the required amount
     */
    public static boolean hasEnoughResource(ServerPlayer player, double amount) {
        return getResource(player) >= amount;
    }

    /**
     * Consume resource from a player.
     * Only succeeds if the player has enough resource.
     *
     * @param player The player
     * @param amount The amount to consume
     * @return true if the resource was consumed, false if not enough
     */
    public static boolean consumeResource(ServerPlayer player, double amount) {
        if (!hasEnoughResource(player, amount)) {
            return false;
        }
        modifyResource(player, -amount);
        return true;
    }

    /**
     * Get a scaled stat value for a player's origin.
     *
     * @param player   The player
     * @param statName The stat name
     * @return The scaled stat value, or 0 if not found
     */
    public static double getOriginStat(ServerPlayer player, String statName) {
        Origin origin = getOrigin(player);
        if (origin == null) {
            return 0.0;
        }
        int level = getOriginLevel(player);
        return origin.getScaledStat(statName, level);
    }

    // ========== Passive Stacks API (removed - use PassiveManager) ==========

    /**
     * Get passive stacks - use {@link com.complextalents.passive.PassiveManager#getPassiveStacks}
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @return The current stack count
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#getPassiveStacks}
     */
    @Deprecated
    public static int getPassiveStacks(ServerPlayer player, String stackTypeName) {
        return com.complextalents.passive.PassiveManager.getPassiveStacks(player, stackTypeName);
    }

    /**
     * Check passive stacks - use {@link com.complextalents.passive.PassiveManager#hasPassiveStacks}
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param threshold    The minimum stacks required
     * @return true if the player has at least the threshold
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#hasPassiveStacks}
     */
    @Deprecated
    public static boolean hasPassiveStacks(ServerPlayer player, String stackTypeName, int threshold) {
        return com.complextalents.passive.PassiveManager.hasPassiveStacks(player, stackTypeName, threshold);
    }

    /**
     * Check max passive stacks - use {@link com.complextalents.passive.PassiveManager#isAtMaxPassiveStacks}
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @return true if at max stacks
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#isAtMaxPassiveStacks}
     */
    @Deprecated
    public static boolean isAtMaxPassiveStacks(ServerPlayer player, String stackTypeName) {
        return com.complextalents.passive.PassiveManager.isAtMaxPassiveStacks(player, getOrigin(player), stackTypeName);
    }

    /**
     * Modify passive stacks - use {@link com.complextalents.passive.PassiveManager#modifyPassiveStacks}
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param delta        The amount to add (can be negative)
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#modifyPassiveStacks}
     */
    @Deprecated
    public static void modifyPassiveStacks(ServerPlayer player, String stackTypeName, int delta) {
        com.complextalents.passive.PassiveManager.modifyPassiveStacks(player, stackTypeName, delta);
    }

    /**
     * Set passive stacks - use {@link com.complextalents.passive.PassiveManager#setPassiveStacks}
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param count        The new stack count
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#setPassiveStacks}
     */
    @Deprecated
    public static void setPassiveStacks(ServerPlayer player, String stackTypeName, int count) {
        com.complextalents.passive.PassiveManager.setPassiveStacks(player, stackTypeName, count);
    }

    /**
     * Reset passive stacks - use {@link com.complextalents.passive.PassiveManager#resetPassiveStacks}
     *
     * @param player The player
     * @deprecated Use {@link com.complextalents.passive.PassiveManager#resetPassiveStacks}
     */
    @Deprecated
    public static void resetPassiveStacks(ServerPlayer player) {
        com.complextalents.passive.PassiveManager.resetPassiveStacks(player);
    }
}
