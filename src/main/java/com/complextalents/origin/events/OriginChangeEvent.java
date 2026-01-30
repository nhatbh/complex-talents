package com.complextalents.origin.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player's origin changes.
 * <p>
 * This event fires when:
 * <ul>
 *   <li>A player's origin is set ({@link ChangeType#SET})</li>
 *   <li>A player's origin is cleared ({@link ChangeType#CLEAR})</li>
 *   <li>A player's origin level changes ({@link ChangeType#LEVEL_CHANGE})</li>
 * </ul>
 * </p>
 * <p>
 * Origins can listen to this event to handle level-dependent stat updates,
 * such as refreshing attribute modifiers when scaled stats change.
 * </p>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 */
public class OriginChangeEvent extends Event {

    private final ServerPlayer player;
    private final ResourceLocation originId;
    private final int oldLevel;
    private final int newLevel;
    private final ChangeType changeType;

    /**
     * Creates a new OriginChangeEvent.
     *
     * @param player     The player whose origin changed
     * @param originId   The origin ID (null if cleared)
     * @param oldLevel   The old origin level
     * @param newLevel   The new origin level
     * @param changeType The type of change
     */
    public OriginChangeEvent(ServerPlayer player, ResourceLocation originId, int oldLevel, int newLevel, ChangeType changeType) {
        this.player = player;
        this.originId = originId;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.changeType = changeType;
    }

    /**
     * Gets the player whose origin changed.
     *
     * @return The player
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the origin ID.
     *
     * @return The origin ID, or null if the origin was cleared
     */
    public ResourceLocation getOriginId() {
        return originId;
    }

    /**
     * Gets the old origin level.
     *
     * @return The old level
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * Gets the new origin level.
     *
     * @return The new level
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * Gets the type of change that occurred.
     *
     * @return The change type
     */
    public ChangeType getChangeType() {
        return changeType;
    }

    /**
     * The type of origin change.
     */
    public enum ChangeType {
        /**
         * A new origin was set for the player.
         */
        SET,
        /**
         * The player's origin was cleared.
         */
        CLEAR,
        /**
         * The player's origin level changed.
         */
        LEVEL_CHANGE
    }
}
