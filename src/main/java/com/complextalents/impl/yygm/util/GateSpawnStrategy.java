package com.complextalents.impl.yygm.util;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for gate spawning logic in the YYGM system.
 * Consolidates duplicate gate spawning code from HarmonizedEffect and ExposedEffect.
 */
public final class GateSpawnStrategy {

    private GateSpawnStrategy() {
        // Utility class - prevent instantiation
    }

    /** Yang (gold) gate type */
    public static final int GATE_YANG = 0;

    /** Yin (silver) gate type */
    public static final int GATE_YIN = 1;

    /** No gate at this direction */
    public static final int GATE_NONE = -1;

    /** Bitmap value when all 8 slots have been used */
    public static final int ALL_SLOTS_USED = 0xFF;

    /** Number of compass directions */
    public static final int NUM_DIRECTIONS = 8;

    /**
     * Result of spawning dual gates for Harmonized state.
     *
     * @param yangDirection The compass direction for the Yang gate (0-7)
     * @param yinDirection The compass direction for the Yin gate (0-7)
     * @param newUsedSlotsBitmap The updated bitmap of used slots
     */
    public record GateSpawnResult(int yangDirection, int yinDirection, int newUsedSlotsBitmap) {

        /**
         * Check if this result is valid (both gates assigned).
         */
        public boolean isValid() {
            return yangDirection >= 0 && yangDirection < NUM_DIRECTIONS
                && yinDirection >= 0 && yinDirection < NUM_DIRECTIONS
                && yangDirection != yinDirection;
        }
    }

    /**
     * Spawn two gates (Yang and Yin) at different compass directions for Harmonized state.
     * Only spawns in slots that haven't been used yet. Resets when all 8 slots are used.
     *
     * @param entity The target entity (for logging)
     * @param playerUuid The player's UUID (for logging)
     * @param random Random source for gate placement
     * @param usedSlotsBitmap Current bitmap of used slots
     * @return Result containing gate positions and updated bitmap
     */
    public static GateSpawnResult spawnDualGates(LivingEntity entity, UUID playerUuid,
                                                  RandomSource random, int usedSlotsBitmap) {
        // Reset if all slots have been used
        if (usedSlotsBitmap == ALL_SLOTS_USED) {
            usedSlotsBitmap = 0;
        }

        // Get list of available (unused) slots
        List<Integer> availableSlots = getAvailableSlots(usedSlotsBitmap);

        // Need at least 2 available slots
        if (availableSlots.size() < 2) {
            // Edge case: reset and try again
            usedSlotsBitmap = 0;
            availableSlots = getAvailableSlots(0);
        }

        // Pick two different random slots from available list
        int yangIndex = random.nextInt(availableSlots.size());
        int yangDir = availableSlots.get(yangIndex);

        int yinIndex;
        do {
            yinIndex = random.nextInt(availableSlots.size());
        } while (yinIndex == yangIndex);
        int yinDir = availableSlots.get(yinIndex);

        // Mark both slots as used and return result
        int newBitmap = markSlotsAsUsed(usedSlotsBitmap, yangDir, yinDir);

        return new GateSpawnResult(yangDir, yinDir, newBitmap);
    }

    /**
     * Generate a random gate pattern for Exposed state (8 gates).
     * Creates exactly 4 Yang and 4 Yin gates, randomly distributed.
     * Returns an 8-bit bitmap where bit position = compass direction (0-7).
     *
     * @param random Random source for pattern generation
     * @return 8-bit bitmap (bit set = Yang, bit clear = Yin)
     */
    public static int generateEightGatePattern(RandomSource random) {
        // Create array with 4 Yangs and 4 Yins
        int[] gates = {GATE_YANG, GATE_YANG, GATE_YANG, GATE_YANG,
                       GATE_YIN, GATE_YIN, GATE_YIN, GATE_YIN};

        // Shuffle using Fisher-Yates algorithm
        for (int i = gates.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = gates[i];
            gates[i] = gates[j];
            gates[j] = temp;
        }

        // Convert to bitmap (1 = Yang, 0 = Yin)
        int pattern = 0;
        for (int i = 0; i < 8; i++) {
            if (gates[i] == GATE_YANG) {
                pattern |= (1 << i);  // Set bit for Yang
            }
        }
        return pattern;
    }

    /**
     * Get a list of available (unused) slot indices.
     *
     * @param usedBitmap Bitmap of used slots
     * @return List of available slot indices (0-7)
     */
    public static List<Integer> getAvailableSlots(int usedBitmap) {
        List<Integer> available = new ArrayList<>();
        for (int slot = 0; slot < NUM_DIRECTIONS; slot++) {
            if ((usedBitmap & (1 << slot)) == 0) {
                available.add(slot);
            }
        }
        return available;
    }

    /**
     * Count the number of used slots in a bitmap.
     *
     * @param usedBitmap Bitmap of used slots
     * @return Number of used slots (0-8)
     */
    public static int countUsedSlots(int usedBitmap) {
        return Integer.bitCount(usedBitmap);
    }

    /**
     * Mark specific slots as used in a bitmap.
     *
     * @param usedBitmap Current bitmap of used slots
     * @param slots Slots to mark as used
     * @return Updated bitmap
     */
    public static int markSlotsAsUsed(int usedBitmap, int... slots) {
        int bitmap = usedBitmap;
        for (int slot : slots) {
            bitmap |= (1 << slot);
        }
        return bitmap;
    }

    /**
     * Get a random available slot that is not the same as the excluded slot.
     * Resets bitmap if all slots are used.
     *
     * @param entity The target entity
     * @param playerUuid The player's UUID
     * @param excludeSlot Slot to exclude from selection
     * @param random Random source for selection
     * @param usedSlotsBitmap Current bitmap of used slots
     * @param nbtRoot NBT root key for saving updated bitmap
     * @return A random available slot different from excludeSlot
     */
    public static int getRandomAvailableSlot(LivingEntity entity, UUID playerUuid, int excludeSlot,
                                            RandomSource random, int usedSlotsBitmap, String nbtRoot) {
        // Reset if all slots have been used
        if (usedSlotsBitmap == ALL_SLOTS_USED) {
            usedSlotsBitmap = 0;
        }

        List<Integer> availableSlots = getAvailableSlots(usedSlotsBitmap);

        // Remove the excluded slot from available list
        availableSlots.remove(Integer.valueOf(excludeSlot));

        // If no available slots (edge case), reset
        if (availableSlots.isEmpty()) {
            usedSlotsBitmap = 0;
            availableSlots = getAvailableSlots(0);
            availableSlots.remove(Integer.valueOf(excludeSlot));
        }

        // Pick a random available slot
        int chosenSlot = availableSlots.get(random.nextInt(availableSlots.size()));

        return chosenSlot;
    }

    /**
     * Get the gate type at a specific direction from a gate pattern bitmap.
     *
     * @param pattern The gate pattern bitmap (from generateEightGatePattern)
     * @param direction The compass direction (0-7)
     * @return GATE_YANG if bit is set, GATE_YIN otherwise
     */
    public static int getGateTypeFromPattern(int pattern, int direction) {
        return (pattern & (1 << direction)) != 0 ? GATE_YANG : GATE_YIN;
    }
}
