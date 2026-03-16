package com.complextalents.leveling.events.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player levels up.
 * This event is NOT cancelable - the level-up has already occurred.
 *
 * <p>This event allows systems to:
 * <ul>
 *   <li>Send notifications to the player</li>
 *   <li>Sync level data to client</li>
 *   <li>Play sounds/effects</li>
 *   <li>Track statistics</li>
 * </ul>
 *
 * <p>This event is immutable - all fields are final and read-only.</p>
 *
 * <p>This event is fired on the Forge Event Bus.</p>
 */
public class PlayerLevelUpEvent extends Event {
    private final ServerPlayer player;
    private final int oldLevel;
    private final int newLevel;
    private final int skillPointsAwarded;

    /**
     * Creates a new PlayerLevelUpEvent.
     *
     * @param player The player who leveled up
     * @param oldLevel The previous level
     * @param newLevel The new level
     * @param skillPointsAwarded The number of skill points awarded for this level-up
     */
    public PlayerLevelUpEvent(ServerPlayer player, int oldLevel, int newLevel, int skillPointsAwarded) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.skillPointsAwarded = skillPointsAwarded;
    }

    // Immutable getters
    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the level the player was at before leveling up.
     *
     * @return The old level
     */
    public int getOldLevel() {
        return oldLevel;
    }

    /**
     * Gets the new level the player reached.
     *
     * @return The new level
     */
    public int getNewLevel() {
        return newLevel;
    }

    /**
     * Gets the number of levels gained in this event.
     * Usually 1, but could be multiple if a large amount of XP was awarded at once.
     *
     * @return The number of levels gained
     */
    public int getLevelsGained() {
        return newLevel - oldLevel;
    }

    /**
     * Gets the number of skill points awarded for this level-up.
     *
     * @return The skill points awarded
     */
    public int getSkillPointsAwarded() {
        return skillPointsAwarded;
    }

    @Override
    public String toString() {
        return String.format("PlayerLevelUpEvent{player=%s, %d->%d, skillPoints=%d}",
                player.getName().getString(), oldLevel, newLevel, skillPointsAwarded);
    }
}
