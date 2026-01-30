package com.complextalents.passive.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player's passive stacks change.
 * <p>
 * This event fires when:
 * <ul>
 *   <li>A player's passive stacks are set to a new value ({@link ChangeType#SET})</li>
 *   <li>A player's passive stacks are modified ({@link ChangeType#MODIFY})</li>
 *   <li>All passive stacks are reset ({@link ChangeType#RESET})</li>
 * </ul>
 * </p>
 * <p>
 * Origins and skills can listen to this event to handle passive stack changes,
 * such as updating attribute modifiers or applying effects based on stack count.
 * </p>
 *
 * <p>This event is {@linkplain Cancelable cancelable}</p>
 */
public class PassiveStackChangeEvent extends Event {

    private final ServerPlayer player;
    private final String stackTypeName;
    private final int oldValue;
    private final int newValue;
    private final ChangeType changeType;

    /**
     * Creates a new PassiveStackChangeEvent for a specific stack type.
     *
     * @param player       The player whose stacks changed
     * @param stackTypeName The stack type name
     * @param oldValue     The old stack count
     * @param newValue     The new stack count
     * @param changeType   The type of change
     */
    public PassiveStackChangeEvent(ServerPlayer player, String stackTypeName, int oldValue, int newValue, ChangeType changeType) {
        this.player = player;
        this.stackTypeName = stackTypeName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    /**
     * Creates a new PassiveStackChangeEvent for a full reset.
     *
     * @param player     The player whose stacks were reset
     * @param changeType The change type (must be RESET)
     */
    public PassiveStackChangeEvent(ServerPlayer player, ChangeType changeType) {
        this.player = player;
        this.stackTypeName = null;
        this.oldValue = -1;
        this.newValue = 0;
        this.changeType = changeType;
    }

    /**
     * Gets the player whose stacks changed.
     *
     * @return The player
     */
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the stack type name that changed.
     *
     * @return The stack type name, or null if all stacks were reset
     */
    public String getStackTypeName() {
        return stackTypeName;
    }

    /**
     * Gets the old stack count.
     *
     * @return The old stack count, or -1 if reset
     */
    public int getOldValue() {
        return oldValue;
    }

    /**
     * Gets the new stack count.
     *
     * @return The new stack count
     */
    public int getNewValue() {
        return newValue;
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
     * The type of passive stack change.
     */
    public enum ChangeType {
        /**
         * Passive stacks were set to a specific value.
         */
        SET,
        /**
         * Passive stacks were modified by a delta.
         */
        MODIFY,
        /**
         * All passive stacks were reset.
         */
        RESET
    }
}
