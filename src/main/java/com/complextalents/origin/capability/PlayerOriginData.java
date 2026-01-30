package com.complextalents.origin.capability;

import com.complextalents.origin.Origin;
import com.complextalents.origin.OriginRegistry;
import com.complextalents.origin.ResourceType;
import com.complextalents.origin.network.OriginDataSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of player origin data capability.
 * Stores active origin, origin level, and resource value.
 * Mirrors PlayerSkillData pattern.
 * <p>
 * Passive stacks are now managed by the shared passive capability.
 * </p>
 */
public class PlayerOriginData implements IPlayerOriginData {

    private final ServerPlayer player;

    // Active origin
    private ResourceLocation activeOrigin = null;

    // Origin level (1-maxLevel)
    private int originLevel = 1;

    // Current resource value
    private double resourceValue = 0;

    public PlayerOriginData(ServerPlayer player) {
        this.player = player;
    }

    @Override
    @Nullable
    public ResourceLocation getActiveOrigin() {
        return activeOrigin;
    }

    @Override
    public void setActiveOrigin(@Nullable ResourceLocation originId) {
        this.activeOrigin = originId;

        // Reset to default values when origin changes
        this.originLevel = 1;
        ResourceType resourceType = getResourceType();
        if (resourceType != null) {
            this.resourceValue = resourceType.getMin();
        } else {
            this.resourceValue = 0;
        }

        // Reset passive stacks when origin changes (handled by shared capability)
        player.getCapability(com.complextalents.passive.capability.PassiveStackDataProvider.PASSIVE_STACK_DATA)
                .ifPresent(data -> data.resetPassiveStacks());
    }

    @Override
    public int getOriginLevel() {
        return originLevel;
    }

    @Override
    public void setOriginLevel(int level) {
        Origin origin = getOrigin();
        int maxLevel = origin != null ? origin.getMaxLevel() : 1;
        this.originLevel = Math.max(1, Math.min(level, maxLevel));
    }

    @Override
    @Nullable
    public ResourceType getResourceType() {
        Origin origin = getOrigin();
        return origin != null ? origin.getResourceType() : null;
    }

    @Override
    public double getResource() {
        return resourceValue;
    }

    @Override
    public void setResource(double value) {
        ResourceType resourceType = getResourceType();
        if (resourceType != null) {
            this.resourceValue = resourceType.clamp(value);
        } else {
            this.resourceValue = value;
        }
    }

    @Override
    public void modifyResource(double delta) {
        double oldValue = resourceValue;
        setResource(resourceValue + delta);
        // Only sync if value actually changed
        if (oldValue != resourceValue) {
            sync();
        }
    }

    @Override
    public void sync() {
        // Send sync packet to client
        ResourceType resourceType = getResourceType();
        ResourceLocation resourceTypeId = resourceType != null ? resourceType.getId() : null;
        double resourceMax = resourceType != null ? resourceType.getMax() : 0;

        OriginDataSyncPacket.send(
                player,
                activeOrigin,
                originLevel,
                resourceValue,
                resourceMax,
                resourceTypeId
        );

        // Passive stacks are synced separately by the shared capability
    }

    @Override
    public void clear() {
        activeOrigin = null;
        originLevel = 1;
        resourceValue = 0;
        sync();
    }

    @Override
    public void tick() {
        // Placeholder for passive effects
        // Origins can implement tick-based behaviors here
        // For now, this is empty - tick behaviors are handled by event handlers
    }

    /**
     * Helper to get the Origin object for the active origin.
     */
    @Nullable
    private Origin getOrigin() {
        if (activeOrigin == null) {
            return null;
        }
        return OriginRegistry.getInstance().getOrigin(activeOrigin);
    }

    // NBT serialization for persistence
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Serialize active origin
        if (activeOrigin != null) {
            tag.putString("activeOrigin", activeOrigin.toString());
        }

        // Serialize origin level (persists through death)
        tag.putInt("originLevel", originLevel);

        // Serialize resource value (resets on death, but saved for logout/login)
        tag.putDouble("resourceValue", resourceValue);

        // Passive stacks are now serialized by the shared capability

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Deserialize active origin
        if (tag.contains("activeOrigin")) {
            String originStr = tag.getString("activeOrigin");
            activeOrigin = ResourceLocation.tryParse(originStr);
        } else {
            activeOrigin = null;
        }

        // Deserialize origin level
        if (tag.contains("originLevel")) {
            originLevel = tag.getInt("originLevel");
            // Clamp to valid range
            Origin origin = getOrigin();
            int maxLevel = origin != null ? origin.getMaxLevel() : 1;
            originLevel = Math.max(1, Math.min(originLevel, maxLevel));
        } else {
            originLevel = 1;
        }

        // Deserialize resource value
        if (tag.contains("resourceValue")) {
            resourceValue = tag.getDouble("resourceValue");
            // Clamp to resource type range
            ResourceType resourceType = getResourceType();
            if (resourceType != null) {
                resourceValue = resourceType.clamp(resourceValue);
            }
        } else {
            ResourceType resourceType = getResourceType();
            resourceValue = resourceType != null ? resourceType.getMin() : 0;
        }

        // Passive stacks are now deserialized by the shared capability
    }
}
