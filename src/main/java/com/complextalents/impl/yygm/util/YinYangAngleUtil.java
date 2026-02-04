package com.complextalents.impl.yygm.util;

import net.minecraft.world.entity.LivingEntity;

/**
 * Utility class for angle and compass direction calculations in the YYGM system.
 * Consolidates duplicate code from HarmonizedEffect and ExposedEffect.
 */
public final class YinYangAngleUtil {

    private YinYangAngleUtil() {
        // Utility class - prevent instantiation
    }

    /** Number of compass directions (8-way) */
    public static final int NUM_DIRECTIONS = 8;

    /** Compass angle values for each direction */
    public static final int[] COMPASS_ANGLES = {0, 45, 90, 135, 180, 225, 270, 315};

    /**
     * Convert an angle in radians to a compass direction (0-7).
     * 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW
     *
     * @param angleRadians Angle in radians
     * @return Compass direction 0-7
     */
    public static int angleToCompassDirection(double angleRadians) {
        double angleDegrees = Math.toDegrees(angleRadians);

        // Normalize to 0-360 range
        while (angleDegrees < 0) {
            angleDegrees += 360;
        }
        while (angleDegrees >= 360) {
            angleDegrees -= 360;
        }

        // Divide into 8 sectors, each 45 degrees wide
        // Add 22.5 to center the sector on the compass point
        int direction = (int) Math.floor((angleDegrees + 22.5) / 45.0);
        return direction % 8;
    }

    /**
     * Calculate the attack angle from target to attacker.
     * Returns angle in radians where 0 = North (positive Z), clockwise positive.
     *
     * @param target The target entity being attacked
     * @param attacker The entity attacking
     * @return Angle in radians [0, 2*PI]
     */
    public static double calculateAttackAngle(LivingEntity target, LivingEntity attacker) {
        double dx = attacker.getX() - target.getX();
        double dz = attacker.getZ() - target.getZ();

        // atan2(dx, -dz) gives angle where 0 = North, clockwise positive
        double angle = Math.atan2(dx, -dz);

        // Normalize to [0, 2*PI]
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }

    /**
     * Convert a compass direction index to its string representation.
     *
     * @param direction Compass direction 0-7
     * @return String representation (N, NE, E, SE, S, SW, W, NW)
     */
    public static String compassDirectionToString(int direction) {
        return switch (direction) {
            case 0 -> "N";
            case 1 -> "NE";
            case 2 -> "E";
            case 3 -> "SE";
            case 4 -> "S";
            case 5 -> "SW";
            case 6 -> "W";
            case 7 -> "NW";
            default -> "UNKNOWN";
        };
    }

    /**
     * Get the opposite compass direction.
     *
     * @param direction Compass direction 0-7
     * @return Opposite direction
     */
    public static int getOppositeDirection(int direction) {
        return (direction + 4) % 8;
    }

    /**
     * Check if two directions are adjacent (45 degrees apart).
     *
     * @param dir1 First direction
     * @param dir2 Second direction
     * @return true if directions are adjacent
     */
    public static boolean areAdjacent(int dir1, int dir2) {
        int diff = Math.abs(dir1 - dir2);
        return diff == 1 || diff == 7; // 7 handles wraparound (0 and 7)
    }
}
