package com.complextalents.skill;

import com.complextalents.skill.capability.SkillDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central API for skill operations.
 * Mirrors OriginManager pattern.
 */
public class SkillManager {

    /**
     * Get a player's level for a specific skill.
     *
     * @param player  The player
     * @param skillId The skill ID
     * @return The skill level, or 1 if not set or not assigned
     */
    public static int getSkillLevel(ServerPlayer player, ResourceLocation skillId) {
        return player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> data.getSkillLevel(skillId))
                .orElse(1);
    }

    /**
     * Get a scaled stat value for a player's skill.
     *
     * @param player   The player
     * @param skillId  The skill ID
     * @param statName The stat name
     * @return The scaled stat value, or 0 if not found
     */
    public static double getSkillStat(ServerPlayer player, ResourceLocation skillId, String statName) {
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (!(skill instanceof BuiltSkill builtSkill)) {
            return 0.0;
        }
        int level = getSkillLevel(player, skillId);
        return builtSkill.getScaledStat(statName, level);
    }

    /**
     * Check if a player has a skill assigned to any slot.
     *
     * @param player  The player
     * @param skillId The skill ID
     * @return true if the skill is assigned
     */
    public static boolean hasSkill(ServerPlayer player, ResourceLocation skillId) {
        return player.getCapability(SkillDataProvider.SKILL_DATA)
                .map(data -> {
                    for (ResourceLocation slot : data.getAssignedSlots()) {
                        if (skillId.equals(slot)) return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Set a player's level for a specific skill.
     *
     * @param player  The player
     * @param skillId The skill ID
     * @param level   The new level
     */
    public static void setSkillLevel(ServerPlayer player, ResourceLocation skillId, int level) {
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            data.setSkillLevel(skillId, level);
            data.sync();
        });
    }
}
