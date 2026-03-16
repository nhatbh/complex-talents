package com.complextalents.leveling.events.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player's current XP is reset.
 * This typically occurs when a player dies (death penalty).
 * This event is NOT cancelable - the XP has already been reset.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>Send notifications to the player</li>
 *   <li>Sync level data to client</li>
 *   <li>Track statistics</li>
 *   <li>Apply additional penalties/rewards</li>
 * </ul>
 *
 * <p>This event is immutable - all fields are final and read-only.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 */
public class PlayerXPResetEvent extends Event {
    private final ServerPlayer player;
    private final double lostXP;
    private final ResetReason reason;

    /**
     * Creates a new PlayerXPResetEvent.
     *
     * @param player The player whose XP was reset
     * @param lostXP The amount of XP that was lost
     * @param reason The reason for the reset
     */
    public PlayerXPResetEvent(ServerPlayer player, double lostXP, ResetReason reason) {
        this.player = player;
        this.lostXP = lostXP;
        this.reason = reason;
    }

    // Immutable getters
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the amount of XP that was lost in the reset.
     *
     * @return The lost XP amount
     */
    public double getLostXP() {
        return lostXP;
    }

    /**
     * Gets the reason for the XP reset.
     *
     * @return The reason
     */
    public ResetReason getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return String.format("PlayerXPResetEvent{player=%s, lostXP=%.1f, reason=%s}",
                player.getName().getString(), lostXP, reason);
    }

    /**
     * Enumeration of reasons why XP might be reset.
     */
    public enum ResetReason {
        /**
         * XP was reset due to player death (death penalty).
         */
        DEATH("Death"),

        /**
         * XP was reset by admin command.
         */
        ADMIN_COMMAND("Admin Command");

        private final String displayName;

        ResetReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
