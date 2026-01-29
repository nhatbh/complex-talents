package com.complextalents.targeting;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Authoritative targeting payload produced by the client and sent to the server.
 *
 * <p>This is the ONLY targeting data sent from client to server. The server
 * trusts this data and uses it for skill execution without re-calculating
 * raycasts or aim direction.</p>
 *
 * <p><b>Invariants:</b></p>
 * <ul>
 *   <li>{@code aimDirection} is ALWAYS present</li>
 *   <li>{@code targetPosition} is ALWAYS present</li>
 *   <li>Entity hits also produce a position</li>
 *   <li>Direction-only skills still get a position</li>
 * </ul>
 *
 * <p>This guarantees uniform consumption by all skills regardless of their
 * targeting requirements.</p>
 */
public class TargetingSnapshot {
    private final Vec3 origin;
    private final Vec3 aimDirection;
    private final Vec3 targetPosition;

    private final int targetEntityId;
    private final boolean hasEntity;

    private final boolean isAlly;
    private final double distance;

    private final EnumSet<TargetType> resolvedTypes;

    /**
     * Create a new targeting snapshot.
     *
     * @param origin The origin point of the raycast (typically player's eyes)
     * @param aimDirection The normalized look direction vector
     * @param targetPosition The resolved target position (block hit, entity position, or max range point)
     * @param targetEntityId The entity ID of the targeted entity, or -1 if none
     * @param hasEntity Whether an entity is being targeted
     * @param isAlly Whether the target (if any) is considered an ally
     * @param distance The distance from origin to target
     * @param resolvedTypes The set of target types that were successfully resolved
     */
    public TargetingSnapshot(
            Vec3 origin,
            Vec3 aimDirection,
            Vec3 targetPosition,
            int targetEntityId,
            boolean hasEntity,
            boolean isAlly,
            double distance,
            EnumSet<TargetType> resolvedTypes
    ) {
        this.origin = origin;
        this.aimDirection = aimDirection;
        this.targetPosition = targetPosition;
        this.targetEntityId = targetEntityId;
        this.hasEntity = hasEntity;
        this.isAlly = isAlly;
        this.distance = distance;
        this.resolvedTypes = resolvedTypes;
    }

    /**
     * Create a snapshot from a packet buffer.
     *
     * @param buffer The buffer to read from
     * @return A new TargetingSnapshot
     */
    public static TargetingSnapshot fromNetwork(FriendlyByteBuf buffer) {
        Vec3 origin = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 aimDirection = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        Vec3 targetPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());

        int targetEntityId = buffer.readVarInt();
        boolean hasEntity = buffer.readBoolean();
        boolean isAlly = buffer.readBoolean();
        double distance = buffer.readDouble();

        int typeBits = buffer.readVarInt();
        EnumSet<TargetType> resolvedTypes = EnumSet.noneOf(TargetType.class);
        for (TargetType type : TargetType.values()) {
            if ((typeBits & (1 << type.ordinal())) != 0) {
                resolvedTypes.add(type);
            }
        }

        return new TargetingSnapshot(origin, aimDirection, targetPosition, targetEntityId, hasEntity, isAlly, distance, resolvedTypes);
    }

    /**
     * Write this snapshot to a packet buffer.
     *
     * @param buffer The buffer to write to
     */
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeDouble(origin.x);
        buffer.writeDouble(origin.y);
        buffer.writeDouble(origin.z);

        buffer.writeDouble(aimDirection.x);
        buffer.writeDouble(aimDirection.y);
        buffer.writeDouble(aimDirection.z);

        buffer.writeDouble(targetPosition.x);
        buffer.writeDouble(targetPosition.y);
        buffer.writeDouble(targetPosition.z);

        buffer.writeVarInt(targetEntityId);
        buffer.writeBoolean(hasEntity);
        buffer.writeBoolean(isAlly);
        buffer.writeDouble(distance);

        int typeBits = 0;
        for (TargetType type : resolvedTypes) {
            typeBits |= (1 << type.ordinal());
        }
        buffer.writeVarInt(typeBits);
    }

    // Getters

    /**
     * @return The origin point of the raycast (typically player's eye position)
     */
    public Vec3 getOrigin() {
        return origin;
    }

    /**
     * @return The normalized look direction vector (ALWAYS present)
     */
    public Vec3 getAimDirection() {
        return aimDirection;
    }

    /**
     * @return The resolved target position (ALWAYS present)
     */
    public Vec3 getTargetPosition() {
        return targetPosition;
    }

    /**
     * @return The entity ID of the targeted entity, or -1 if no entity is targeted
     */
    public int getTargetEntityId() {
        return targetEntityId;
    }

    /**
     * @return Whether an entity is being targeted
     */
    public boolean hasEntity() {
        return hasEntity;
    }

    /**
     * @return Whether the target (if any) is considered an ally
     */
    public boolean isAlly() {
        return isAlly;
    }

    /**
     * @return The distance from origin to target
     */
    public double getDistance() {
        return distance;
    }

    /**
     * @return The set of target types that were successfully resolved
     */
    public EnumSet<TargetType> getResolvedTypes() {
        return EnumSet.copyOf(resolvedTypes);
    }

    /**
     * Check if a specific target type was resolved.
     *
     * @param type The target type to check
     * @return true if this type was resolved
     */
    public boolean hasType(TargetType type) {
        return resolvedTypes.contains(type);
    }

    /**
     * Check if this snapshot has at least one of the specified target types.
     *
     * @param types The target types to check for
     * @return true if at least one of the specified types is present
     */
    public boolean hasAnyOf(TargetType... types) {
        for (TargetType type : types) {
            if (resolvedTypes.contains(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this snapshot has all of the specified target types.
     *
     * @param types The target types to check for
     * @return true if all specified types are present
     */
    public boolean hasAllOf(TargetType... types) {
        for (TargetType type : types) {
            if (!resolvedTypes.contains(type)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "TargetingSnapshot{" +
                "origin=" + origin +
                ", aimDirection=" + aimDirection +
                ", targetPosition=" + targetPosition +
                ", targetEntityId=" + targetEntityId +
                ", hasEntity=" + hasEntity +
                ", isAlly=" + isAlly +
                ", distance=" + distance +
                ", resolvedTypes=" + resolvedTypes +
                '}';
    }
}
