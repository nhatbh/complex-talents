package com.complextalents.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Centralized utility for determining ally/enemy relationships between entities.
 *
 * <p>This is the single source of truth for ally detection used by both
 * the targeting system and the skill system.</p>
 *
 * <p><b>Ally Detection Rules:</b></p>
 * <ul>
 *   <li>Same team = ally</li>
 *   <li>Tamed animals owned by the player = ally</li>
 *   <li>Players allied to each other via vanilla mechanics = ally</li>
 *   <li>Everything else = not ally</li>
 * </ul>
 */
public final class AllyHelper {

    private AllyHelper() {}

    /**
     * Check if an entity is considered an ally of the player.
     *
     * @param player The player to check alliance against
     * @param entity The entity to check
     * @return true if the entity is considered an ally
     */
    public static boolean isAlly(Player player, Entity entity) {
        // Same team
        if (player.getTeam() != null && entity instanceof LivingEntity living) {
            if (living.getTeam() != null) {
                return player.getTeam() == living.getTeam();
            }
        }

        // Tamed animals owned by this player
        if (entity instanceof TamableAnimal tamable) {
            return tamable.isTame()
                    && tamable.getOwnerUUID() != null
                    && tamable.getOwnerUUID().equals(player.getUUID());
        }

        // Other players allied via vanilla mechanics
        if (entity instanceof Player otherPlayer) {
            return player.isAlliedTo(otherPlayer);
        }

        return false;
    }

    /**
     * Check if an entity is considered an enemy of the player.
     * This is the inverse of isAlly, but can be extended with additional logic.
     *
     * @param player The player to check enmity against
     * @param entity The entity to check
     * @return true if the entity is considered an enemy
     */
    public static boolean isEnemy(Player player, Entity entity) {
        // Player is never their own enemy
        if (player == entity) {
            return false;
        }

        return !isAlly(player, entity);
    }

    /**
     * Check if a player is on the same team as another player.
     *
     * @param player1 The first player
     * @param player2 The second player
     * @return true if both players are on the same team
     */
    public static boolean sameTeam(Player player1, Player player2) {
        if (player1.getTeam() != null && player2.getTeam() != null) {
            return player1.getTeam() == player2.getTeam();
        }
        return false;
    }
}
