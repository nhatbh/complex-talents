package com.complextalents.leveling.events.xp;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired BEFORE XP is awarded to a player.
 * This event is cancelable, allowing handlers to prevent or modify XP awards.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>Modify the XP amount (e.g., apply fatigue multipliers)</li>
 *   <li>Cancel the XP award entirely</li>
 *   <li>Validate or log XP awards</li>
 * </ul>
 *
 * <p>High priority handlers (like FatigueHandler) should modify the amount.
 * Low priority handlers should perform validation or logging.</p>
 *
 * <p>If canceled, the XP is NOT awarded and no XPAwardedEvent is fired.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 *
 * @see XPAwardedEvent
 */
@Cancelable
public class XPPreAwardEvent extends Event {
    private final ServerPlayer player;
    private final XPSource source;
    private double amount; // Mutable to allow modification by handlers
    private final XPContext context;

    /**
     * Creates a new XPPreAwardEvent.
     *
     * @param player The player receiving XP
     * @param source The source of the XP award
     * @param amount The initial XP amount
     * @param context The context object with metadata
     */
    public XPPreAwardEvent(ServerPlayer player, XPSource source, double amount, XPContext context) {
        this.player = player;
        this.source = source;
        this.amount = amount;
        this.context = context;
    }

    // Getters
    public ServerPlayer getPlayer() {
        return player;
    }

    public XPSource getSource() {
        return source;
    }

    /**
     * Gets the current XP amount (may have been modified by previous handlers).
     *
     * @return The XP amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Sets the XP amount. This allows handlers to modify the XP before it is awarded.
     * For example, fatigue handlers can reduce the amount based on chunk fatigue.
     *
     * @param amount The new XP amount (should be non-negative)
     */
    public void setAmount(double amount) {
        this.amount = Math.max(0, amount);
    }

    /**
     * Multiplies the current XP amount by a factor.
     * Convenient method for applying multipliers like fatigue.
     *
     * @param multiplier The multiplier to apply
     */
    public void multiplyAmount(double multiplier) {
        this.amount *= Math.max(0, multiplier);
    }

    public XPContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("XPPreAwardEvent{player=%s, source=%s, amount=%.1f}",
                player.getName().getString(), source.getDisplayName(), amount);
    }
}
