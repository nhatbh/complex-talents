package com.complextalents.passive.capability;

import com.complextalents.passive.PassiveStackDef;
import com.complextalents.passive.PassiveStackRegistry;
import com.complextalents.passive.network.PassiveStackSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of player passive stack data capability.
 * Stores passive stack counts that can be used by both origins and skills.
 */
public class PassiveStackData implements IPassiveStackData {

    private final ServerPlayer player;

    // Passive stacks storage - maps stack type name to current count
    private final Map<String, Integer> passiveStacks = new ConcurrentHashMap<>();

    public PassiveStackData(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public Map<String, Integer> getPassiveStacks() {
        return new ConcurrentHashMap<>(passiveStacks);
    }

    @Override
    public int getPassiveStackCount(String stackTypeName) {
        return passiveStacks.getOrDefault(stackTypeName, 0);
    }

    @Override
    public void setPassiveStacks(String stackTypeName, int count) {
        int oldValue = getPassiveStackCount(stackTypeName);

        // Clamp to max stacks from definition
        int maxStacks = getMaxStacksForType(stackTypeName);
        int clampedCount = Math.max(0, Math.min(count, maxStacks));

        if (clampedCount == 0) {
            passiveStacks.remove(stackTypeName);
        } else {
            passiveStacks.put(stackTypeName, clampedCount);
        }

        // Only sync if value changed
        if (oldValue != clampedCount) {
            sync();
        }
    }

    @Override
    public void modifyPassiveStacks(String stackTypeName, int delta) {
        int current = getPassiveStackCount(stackTypeName);
        setPassiveStacks(stackTypeName, current + delta);
    }

    @Override
    public void resetPassiveStacks() {
        if (!passiveStacks.isEmpty()) {
            passiveStacks.clear();
            sync();
        }
    }

    @Override
    public void sync() {
        // Send sync packet to client
        PassiveStackSyncPacket.send(player, getPassiveStacks());
    }

    /**
     * Get the max stacks for a specific stack type from the registry.
     *
     * @param stackTypeName The stack type name
     * @return The max stacks, or Integer.MAX_VALUE if not defined
     */
    private int getMaxStacksForType(String stackTypeName) {
        // Try to find the definition in the registry
        // Since we don't know the owner here, we'll search all owners
        // This is a bit inefficient but keeps the API simple
        for (String ownerId : getAllOwnerIds()) {
            PassiveStackDef def = PassiveStackRegistry.getDefRaw(ownerId, stackTypeName);
            if (def != null) {
                return def.getMaxStacks();
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Helper to get all possible owner IDs from the player.
     * Checks both origin and skills.
     */
    private String[] getAllOwnerIds() {
        return new String[] {
            getOriginId(),
            getSkillIds()
        };
    }

    /**
     * Get the player's origin ID as a string.
     */
    private String getOriginId() {
        return player.getCapability(com.complextalents.origin.capability.OriginDataProvider.ORIGIN_DATA)
                .map(data -> {
                    var originId = data.getActiveOrigin();
                    return originId != null ? originId.toString() : null;
                })
                .orElse(null);
    }

    /**
     * Get the player's skill IDs as comma-separated string.
     * This is a simple approach - for checking all skill passive definitions.
     */
    private String getSkillIds() {
        // Return empty for now - skills will be checked differently
        // In practice, the registry lookup should work by stack name alone
        return "";
    }

    // NBT serialization for persistence
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Serialize passive stacks
        CompoundTag stacksTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : passiveStacks.entrySet()) {
            stacksTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("passiveStacks", stacksTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Deserialize passive stacks
        passiveStacks.clear();
        if (tag.contains("passiveStacks")) {
            CompoundTag stacksTag = tag.getCompound("passiveStacks");
            for (String key : stacksTag.getAllKeys()) {
                int count = stacksTag.getInt(key);
                // Clamp to max stacks from definition
                int maxStacks = getMaxStacksForType(key);
                int clampedCount = Math.max(0, Math.min(count, maxStacks));
                if (clampedCount > 0) {
                    passiveStacks.put(key, clampedCount);
                }
            }
        }
    }
}
