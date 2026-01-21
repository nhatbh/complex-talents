package com.complextalents.api;

import com.complextalents.TalentsMod;
import com.complextalents.capability.PlayerTalents;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.ElementType;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Public API for other mods to interact with the Complex Talents system.
 * All methods are safe to call from other mods.
 */
public class TalentAPI {
    /**
     * Grants a talent to a player.
     *
     * @param player The player to grant the talent to
     * @param talentId The ID of the talent to grant
     * @param level The level of the talent (must be >= 1)
     */
    public static void grantTalent(ServerPlayer player, ResourceLocation talentId, int level) {
        if (player == null || talentId == null || level < 1) {
            return;
        }

        // Check if elemental talents are enabled for elemental mastery talents
        if (isElementalTalent(talentId) && !com.complextalents.config.TalentConfig.enableElementalTalents.get()) {
            TalentsMod.LOGGER.debug("API: Elemental talents are disabled, cannot grant {}", talentId);
            return;
        }

        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            talents.unlockTalent(talentId, level);

            Talent talent = TalentRegistry.getTalent(talentId);
            if (talent != null) {
                talent.onUnlock(player, level);
            }

            TalentsMod.LOGGER.debug("API: Granted talent {} at level {} to {}", talentId, level, player.getName().getString());
        });
    }

    /**
     * Removes a talent from a player.
     *
     * @param player The player to remove the talent from
     * @param talentId The ID of the talent to remove
     */
    public static void revokeTalent(ServerPlayer player, ResourceLocation talentId) {
        if (player == null || talentId == null) {
            return;
        }

        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            if (talents.hasTalent(talentId)) {
                Talent talent = TalentRegistry.getTalent(talentId);
                if (talent != null) {
                    talent.onRemove(player);
                }

                talents.removeTalent(talentId);
                TalentsMod.LOGGER.debug("API: Revoked talent {} from {}", talentId, player.getName().getString());
            }
        });
    }

    /**
     * Checks if a player has a specific talent.
     *
     * @param player The player to check
     * @param talentId The ID of the talent to check
     * @return True if the player has the talent, false otherwise
     */
    public static boolean hasTalent(ServerPlayer player, ResourceLocation talentId) {
        if (player == null || talentId == null) {
            return false;
        }

        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.hasTalent(talentId))
                .orElse(false);
    }

    /**
     * Gets the level of a talent for a player.
     *
     * @param player The player to check
     * @param talentId The ID of the talent to check
     * @return The level of the talent, or 0 if the player doesn't have it
     */
    public static int getTalentLevel(ServerPlayer player, ResourceLocation talentId) {
        if (player == null || talentId == null) {
            return 0;
        }

        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.getTalentLevel(talentId))
                .orElse(0);
    }

    /**
     * Gets all unlocked talents for a player.
     *
     * @param player The player to check
     * @return List of unlocked talent IDs
     */
    public static List<ResourceLocation> getUnlockedTalents(ServerPlayer player) {
        if (player == null) {
            return List.of();
        }

        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(PlayerTalents::getUnlockedTalents)
                .orElse(List.of());
    }

    /**
     * Applies an element stack to an entity.
     * This is the main entry point for mods to integrate with the elemental system.
     *
     * @param target The entity to apply the element to
     * @param element The element to apply
     * @param source The source entity (typically the attacker)
     */
    public static void applyElement(LivingEntity target, ElementType element, LivingEntity source) {
        if (target == null || element == null || source == null) {
            return;
        }

        ElementalStackManager.applyElementStack(target, element, source);
        TalentsMod.LOGGER.debug("API: Applied {} element stack to {} from {}", element, target.getName().getString(), source.getName().getString());
    }

    /**
     * Checks if the talent system is enabled.
     *
     * @return True if the talent system is enabled
     */
    public static boolean isTalentSystemEnabled() {
        return com.complextalents.config.TalentConfig.enableTalents.get();
    }

    /**
     * Checks if the elemental system is enabled.
     *
     * @return True if the elemental system is enabled
     */
    public static boolean isElementalSystemEnabled() {
        return com.complextalents.config.TalentConfig.enableElementalSystem.get();
    }

    /**
     * Gets the maximum number of talents a player can have.
     *
     * @return Maximum talent count
     */
    public static int getMaxTalentsPerPlayer() {
        return com.complextalents.config.TalentConfig.maxTalentsPerPlayer.get();
    }

    /**
     * Checks if a talent ID corresponds to an elemental mastery talent.
     *
     * @param talentId The talent ID to check
     * @return True if the talent is an elemental mastery talent
     */
    private static boolean isElementalTalent(ResourceLocation talentId) {
        if (talentId == null || !talentId.getNamespace().equals(TalentsMod.MODID)) {
            return false;
        }

        String path = talentId.getPath();
        return path.equals("elemental_mastery") ||
               path.equals("fire_mastery") ||
               path.equals("aqua_mastery") ||
               path.equals("ice_mastery") ||
               path.equals("lightning_mastery") ||
               path.equals("nature_mastery") ||
               path.equals("ender_mastery");
    }
}
