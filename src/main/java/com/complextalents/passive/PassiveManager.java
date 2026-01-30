package com.complextalents.passive;

import com.complextalents.passive.capability.PassiveStackDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Central API for passive stack operations.
 * Used by both origins and skills.
 */
public class PassiveManager {

    /**
     * Get a player's current stack count for a specific passive stack type.
     *
     * @param player       The player
     * @param stackTypeName The stack type name (e.g., "grace")
     * @return The current stack count, or 0 if not found
     */
    public static int getPassiveStacks(ServerPlayer player, String stackTypeName) {
        return player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA)
                .map(data -> data.getPassiveStackCount(stackTypeName))
                .orElse(0);
    }

    /**
     * Get all passive stacks for a player.
     *
     * @param player The player
     * @return Map of stack type name -> current count (empty if none)
     */
    public static Map<String, Integer> getAllPassiveStacks(ServerPlayer player) {
        return player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA)
                .map(com.complextalents.passive.capability.IPassiveStackData::getPassiveStacks)
                .orElse(Map.of());
    }

    /**
     * Check if a player has at least a certain number of passive stacks.
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param threshold    The minimum stacks required
     * @return true if the player has at least the threshold
     */
    public static boolean hasPassiveStacks(ServerPlayer player, String stackTypeName, int threshold) {
        return getPassiveStacks(player, stackTypeName) >= threshold;
    }

    /**
     * Check if a player is at max stacks for a type.
     *
     * @param player       The player
     * @param owner        The owner (origin or skill) that defines the stack
     * @param stackTypeName The stack type name
     * @return true if at max stacks
     */
    public static boolean isAtMaxPassiveStacks(ServerPlayer player, @Nullable PassiveOwner owner, String stackTypeName) {
        if (owner == null) {
            // Try to find the owner by checking the player's origin
            var originId = player.getCapability(com.complextalents.origin.capability.OriginDataProvider.ORIGIN_DATA)
                    .map(com.complextalents.origin.capability.IPlayerOriginData::getActiveOrigin)
                    .orElse(null);
            if (originId == null) {
                return false;
            }
            // Use legacy registry lookup
            var def = PassiveStackRegistry.getDef(originId, stackTypeName);
            return def != null && getPassiveStacks(player, stackTypeName) >= def.getMaxStacks();
        }

        PassiveStackDef def = PassiveStackRegistry.getDef(owner, stackTypeName);
        if (def == null) {
            return false;
        }
        return getPassiveStacks(player, stackTypeName) >= def.getMaxStacks();
    }

    /**
     * Modify a player's passive stacks.
     * The final value is clamped to the stack type's max.
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param delta        The amount to add (can be negative)
     */
    public static void modifyPassiveStacks(ServerPlayer player, String stackTypeName, int delta) {
        player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
            data.modifyPassiveStacks(stackTypeName, delta);
        });
    }

    /**
     * Set a player's passive stacks to a specific value.
     * The value is clamped to the stack type's max.
     *
     * @param player       The player
     * @param stackTypeName The stack type name
     * @param count        The new stack count
     */
    public static void setPassiveStacks(ServerPlayer player, String stackTypeName, int count) {
        player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
            data.setPassiveStacks(stackTypeName, count);
        });
    }

    /**
     * Reset all passive stacks for a player.
     *
     * @param player The player
     */
    public static void resetPassiveStacks(ServerPlayer player) {
        player.getCapability(PassiveStackDataProvider.PASSIVE_STACK_DATA).ifPresent(data -> {
            data.resetPassiveStacks();
        });
    }

    /**
     * Get a stack definition by owner and stack name.
     *
     * @param owner The owner (origin or skill)
     * @param stackName The stack name
     * @return The stack definition, or null if not found
     */
    @Nullable
    public static PassiveStackDef getStackDef(PassiveOwner owner, String stackName) {
        return PassiveStackRegistry.getDef(owner, stackName);
    }

    /**
     * Get a stack definition by owner ID and stack name (legacy method).
     *
     * @param ownerId The owner ID (origin ID)
     * @param stackName The stack name
     * @return The stack definition, or null if not found
     */
    @Nullable
    public static PassiveStackDef getStackDef(ResourceLocation ownerId, String stackName) {
        return PassiveStackRegistry.getDef(ownerId, stackName);
    }
}
