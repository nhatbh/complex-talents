package com.complextalents.leveling.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Immutable value object representing a player's leveling statistics.
 * Contains level, current XP, total XP, and skill points.
 */
public class LevelStats {
    private final int level;
    private final double currentXP;
    private final double totalXP;
    private final int totalSkillPoints;
    private final int consumedSkillPoints;

    /**
     * Creates a new LevelStats with default values.
     */
    public LevelStats() {
        this(1, 0.0, 0.0, 0, 0);
    }

    /**
     * Creates a new LevelStats with specified values.
     *
     * @param level The player's level
     * @param currentXP The current XP progress towards next level
     * @param totalXP The total XP accumulated
     * @param totalSkillPoints The total number of skill points earned
     * @param consumedSkillPoints The number of skill points already used
     */
    public LevelStats(int level, double currentXP, double totalXP, int totalSkillPoints, int consumedSkillPoints) {
        this.level = Math.max(1, level);
        this.currentXP = Math.max(0, currentXP);
        this.totalXP = Math.max(0, totalXP);
        this.totalSkillPoints = Math.max(0, totalSkillPoints);
        this.consumedSkillPoints = Math.max(0, consumedSkillPoints);
    }

    // Getters (immutable)
    public int getLevel() {
        return level;
    }

    public double getCurrentXP() {
        return currentXP;
    }

    public double getTotalXP() {
        return totalXP;
    }

    public int getTotalSkillPoints() {
        return totalSkillPoints;
    }

    public int getConsumedSkillPoints() {
        return consumedSkillPoints;
    }

    public int getAvailableSkillPoints() {
        return Math.max(0, totalSkillPoints - consumedSkillPoints);
    }

    /**
     * Creates a new LevelStats with added XP.
     * Useful for functional-style operations without modifying this instance.
     *
     * @param amount The XP amount to add
     * @return A new LevelStats with updated values
     */
    public LevelStats withAddedXP(double amount) {
        return new LevelStats(
                level,
                currentXP + amount,
                totalXP + amount,
                totalSkillPoints,
                consumedSkillPoints
        );
    }

    /**
     * Creates a new LevelStats with current XP reset to 0.
     *
     * @return A new LevelStats with current XP reset
     */
    public LevelStats withResetCurrentXP() {
        return new LevelStats(level, 0.0, totalXP, totalSkillPoints, consumedSkillPoints);
    }

    /**
     * Creates a new LevelStats with increased level and skill points.
     *
     * @param levelIncrease The number of levels to increase
     * @param skillPointsPerLevel The skill points awarded per level
     * @return A new LevelStats with updated level and skill points
     */
    public LevelStats withLevelUp(int levelIncrease, int skillPointsPerLevel) {
        return new LevelStats(
                level + levelIncrease,
                currentXP,
                totalXP,
                totalSkillPoints + (levelIncrease * skillPointsPerLevel),
                consumedSkillPoints
        );
    }

    /**
     * Creates a new LevelStats with updated consumed points.
     *
     * @param consumedPoints The new consumed points value
     * @return A new LevelStats with updated consumed points
     */
    public LevelStats withConsumedPoints(int consumedPoints) {
        return new LevelStats(level, currentXP, totalXP, totalSkillPoints, consumedPoints);
    }

    /**
     * Serializes this LevelStats to NBT format.
     *
     * @return A CompoundTag containing all stats
     */
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putDouble("currentXP", currentXP);
        tag.putDouble("totalXP", totalXP);
        tag.putInt("totalSkillPoints", totalSkillPoints);
        tag.putInt("consumedSkillPoints", consumedSkillPoints);
        return tag;
    }

    /**
     * Deserializes LevelStats from NBT format.
     *
     * @param tag The CompoundTag to deserialize
     * @return A new LevelStats loaded from the tag
     */
    public static LevelStats deserializeNBT(CompoundTag tag) {
        // Migration: Check for old "skillPoints" key
        int totalSP = tag.contains("totalSkillPoints") ? tag.getInt("totalSkillPoints") : tag.getInt("skillPoints");
        return new LevelStats(
                tag.getInt("level"),
                tag.getDouble("currentXP"),
                tag.getDouble("totalXP"),
                totalSP,
                tag.getInt("consumedSkillPoints")
        );
    }

    @Override
    public String toString() {
        return String.format("LevelStats{level=%d, currentXP=%.1f, totalXP=%.1f, totalSP=%d, consumedSP=%d}",
                level, currentXP, totalXP, totalSkillPoints, consumedSkillPoints);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LevelStats)) return false;

        LevelStats that = (LevelStats) o;

        if (level != that.level) return false;
        if (Double.compare(that.currentXP, currentXP) != 0) return false;
        if (Double.compare(that.totalXP, totalXP) != 0) return false;
        if (totalSkillPoints != that.totalSkillPoints) return false;
        if (consumedSkillPoints != that.consumedSkillPoints) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = level;
        temp = Double.doubleToLongBits(currentXP);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(totalXP);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + totalSkillPoints;
        result = 31 * result + consumedSkillPoints;
        return result;
    }
}
