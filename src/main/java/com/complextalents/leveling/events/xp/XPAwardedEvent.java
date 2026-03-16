package com.complextalents.leveling.events.xp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired AFTER XP is successfully awarded to a player.
 * This event is NOT cancelable - the XP has already been added.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>React to XP awards (e.g., sync to client)</li>
 *   <li>Track statistics</li>
 *   <li>Check for level-ups</li>
 *   <li>Display notifications</li>
 * </ul>
 *
 * <p>This event is immutable - all fields are final and read-only.
 * If you need to prevent or modify XP awards, use XPPreAwardEvent instead.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 *
 * @see XPPreAwardEvent
 */
public class XPAwardedEvent extends Event {
    private final ServerPlayer player;
    private final XPSource source;
    private final double originalAmount; // Amount before modifications
    private final double finalAmount; // Amount actually awarded
    private final XPContext context;

    /**
     * Creates a new XPAwardedEvent.
     *
     * @param player The player who received XP
     * @param source The source of the XP award
     * @param originalAmount The XP amount before any modifications
     * @param finalAmount The XP amount actually awarded
     * @param context The context object with metadata
     */
    public XPAwardedEvent(ServerPlayer player, XPSource source, double originalAmount, double finalAmount, XPContext context) {
        this.player = player;
        this.source = source;
        this.originalAmount = originalAmount;
        this.finalAmount = finalAmount;
        this.context = context;
    }

    // Immutable getters
    public ServerPlayer getPlayer() {
        return player;
    }

    public XPSource getSource() {
        return source;
    }

    /**
     * Gets the original XP amount before any modifications (e.g., before fatigue).
     *
     * @return The original XP amount
     */
    public double getOriginalAmount() {
        return originalAmount;
    }

    /**
     * Gets the final XP amount actually awarded.
     * This may be different from the original amount if modified by handlers.
     *
     * @return The final XP amount
     */
    public double getFinalAmount() {
        return finalAmount;
    }

    /**
     * Calculates the multiplier applied to this XP award.
     * Useful for determining fatigue or other modifiers.
     *
     * @return The multiplier (finalAmount / originalAmount)
     */
    public double getMultiplier() {
        return originalAmount > 0 ? finalAmount / originalAmount : 1.0;
    }

    public XPContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("XPAwardedEvent{player=%s, source=%s, original=%.1f, final=%.1f, multiplier=%.2f}",
                player.getName().getString(), source.getDisplayName(), originalAmount, finalAmount, getMultiplier());
    }
}
