package com.complextalents.skill.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.HashMap;

/**
 * Client-side cache of player skill data.
 * Used for input handling and UI display.
 */
@OnlyIn(Dist.CLIENT)
public class ClientSkillData {

    private static final ResourceLocation[] skillSlots = new ResourceLocation[1];
    private static final Map<ResourceLocation, Integer> skillLevels = new HashMap<>();

    // Cooldown tracking: skillId -> expiration game time
    private static final Map<ResourceLocation, Long> activeCooldowns = new HashMap<>();
    private static long serverGameTime = 0;
    private static long lastSyncTime = 0;

    /**
     * Sync skill slots from the server.
     *
     * @param slots The slot assignments from server
     * @param levels The skill levels from server
     */
    public static void syncFromServer(ResourceLocation[] slots, Map<ResourceLocation, Integer> levels) {
        if (slots != null && slots.length >= 1) {
            System.arraycopy(slots, 0, skillSlots, 0, 1);
        }
        skillLevels.clear();
        if (levels != null) {
            skillLevels.putAll(levels);
        }
        lastSyncTime = System.currentTimeMillis();
    }

    /**
     * Sync cooldown data from server.
     *
     * @param cooldowns Map of skillId -> expiration game time
     * @param gameTime Current server game time
     */
    public static void syncCooldowns(Map<ResourceLocation, Long> cooldowns, long gameTime) {
        activeCooldowns.clear();
        if (cooldowns != null) {
            activeCooldowns.putAll(cooldowns);
        }
        serverGameTime = gameTime;
        lastSyncTime = System.currentTimeMillis();
    }

    /**
     * Get remaining cooldown for a skill in seconds.
     *
     * @param skillId The skill ID
     * @return Remaining cooldown in seconds, or 0 if not on cooldown
     */
    public static double getCooldownRemaining(ResourceLocation skillId) {
        Long expiration = activeCooldowns.get(skillId);
        if (expiration == null) {
            return 0;
        }
        // Calculate remaining ticks at sync time, then subtract elapsed time
        long remainingTicksAtSync = expiration - serverGameTime;
        if (remainingTicksAtSync <= 0) {
            return 0;
        }
        // Client doesn't have real gameTime, so estimate based on last sync
        long elapsed = System.currentTimeMillis() - lastSyncTime;
        long elapsedTicks = elapsed / 50; // Convert ms to ticks (20 ticks per second)
        long remainingTicks = remainingTicksAtSync - elapsedTicks;
        return Math.max(0, remainingTicks / 20.0); // Convert ticks to seconds
    }

    /**
     * Check if a skill is on cooldown.
     *
     * @param skillId The skill ID
     * @return true if the skill is on cooldown
     */
    public static boolean isOnCooldown(ResourceLocation skillId) {
        return getCooldownRemaining(skillId) > 0;
    }

    /**
     * Set a cooldown from server data (used when server rejects a skill cast due to cooldown).
     *
     * @param skillId The skill ID
     * @param expirationTime The cooldown expiration game time from server
     * @param currentServerTime The current server game time
     */
    public static void setCooldown(ResourceLocation skillId, long expirationTime, long currentServerTime) {
        activeCooldowns.put(skillId, expirationTime);
        serverGameTime = currentServerTime;
        lastSyncTime = System.currentTimeMillis();
    }

    /**
     * Clear all cooldown data (on disconnect).
     */
    public static void clearCooldowns() {
        activeCooldowns.clear();
        serverGameTime = 0;
    }

    /**
     * Get the skill assigned to a slot.
     *
     * @param slotIndex The slot index (0 only)
     * @return The skill ID, or null if empty
     */
    public static ResourceLocation getSkillInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 1) {
            return null;
        }
        return skillSlots[slotIndex];
    }

    /**
     * Get all skill slots.
     *
     * @return Copy of the skill slots array
     */
    public static ResourceLocation[] getAllSlots() {
        return java.util.Arrays.copyOf(skillSlots, 1);
    }

    /**
     * Clear all skill slots.
     */
    public static void clear() {
        java.util.Arrays.fill(skillSlots, null);
        skillLevels.clear();
        clearCooldowns();
    }

    /**
     * Get the skill level for a specific skill.
     *
     * @param skillId The skill ID
     * @return The skill level (default 1 if not set)
     */
    public static int getSkillLevel(ResourceLocation skillId) {
        return skillLevels.getOrDefault(skillId, 1);
    }
}
