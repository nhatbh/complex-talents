package com.complextalents.skill.client;

import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.TargetingType;
import com.complextalents.targeting.TargetRelation;
import com.complextalents.targeting.TargetingRequest;
import com.complextalents.targeting.TargetingSnapshot;
import com.complextalents.targeting.TargetType;
import com.complextalents.targeting.client.ClientTargetingResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.EnumSet;

/**
 * Client-side skill casting state and logic.
 * Handles channeling tracking and targeting resolution for skill casting.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Track channeling state (start, cancel, end)</li>
 *   <li>Calculate channel progress (0.0 to 1.0)</li>
 *   <li>Resolve targeting based on skill's TargetingType</li>
 *   <li>Validate channel time against skill requirements</li>
 *   <li>Provide current channeling skill for HUD rendering</li>
 * </ul>
 *
 * <p><b>Channeling Behavior:</b></p>
 * <ul>
 *   <li>All skills use channeling - instant skills have maxChannelTime = 0</li>
 *   <li>Channel state is tracked client-side to avoid network latency</li>
 *   <li>Skills have minChannelTime validation (if > 0)</li>
 *   <li>Channel time is capped at maxChannelTime (if > 0)</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SkillCastingClient {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final ClientTargetingResolver RESOLVER = ClientTargetingResolver.getInstance();

    // ==================== Channel State ====================

    private static long channelStartTime = 0;
    private static boolean isChanneling = false;
    private static int currentSlot = -1;
    private static double maxChannelTime = 0;

    /**
     * Start channeling a skill in the given slot.
     *
     * @param slotIndex The hotbar slot (0-3)
     * @param maxTime   The maximum channel time in seconds (0 for instant)
     */
    public static void startChanneling(int slotIndex, double maxTime) {
        if (!isChanneling) {
            channelStartTime = System.currentTimeMillis();
            isChanneling = true;
            currentSlot = slotIndex;
            maxChannelTime = maxTime;
        }
    }

    /**
     * Cancel the current channeling operation.
     */
    public static void cancelChanneling() {
        if (isChanneling) {
            isChanneling = false;
            currentSlot = -1;
        }
    }

    /**
     * End channeling and return the duration.
     *
     * @return The channel duration in milliseconds, or 0 if not channeling
     */
    public static long endChanneling() {
        if (isChanneling) {
            long duration = System.currentTimeMillis() - channelStartTime;
            isChanneling = false;
            currentSlot = -1;
            maxChannelTime = 0;
            return duration;
        }
        return 0;
    }

    /**
     * Get the current channel time in milliseconds.
     *
     * @return Current channel time, or 0 if not channeling
     */
    public static long getCurrentChannelTime() {
        return isChanneling ? System.currentTimeMillis() - channelStartTime : 0;
    }

    /**
     * Check if currently channeling a skill.
     *
     * @return true if channeling
     */
    public static boolean isChanneling() {
        return isChanneling;
    }

    /**
     * Get the slot being channeled.
     *
     * @return The slot index, or -1 if not channeling
     */
    public static int getCurrentSlot() {
        return currentSlot;
    }

    /**
     * Get the maximum channel time for the current channeling operation.
     *
     * @return Max channel time in seconds, or 0 if not channeling
     */
    public static double getMaxChannelTime() {
        return maxChannelTime;
    }

    // ==================== Channel Progress ====================

    /**
     * Get the current channel progress as a ratio (0.0 to 1.0).
     *
     * @return Progress ratio, or 0.0 if not channeling
     */
    public static double getChannelProgress() {
        if (!isChanneling) {
            return 0.0;
        }
        double maxTime = maxChannelTime;
        if (maxTime <= 0) {
            return 0.0;
        }
        long elapsedMs = getCurrentChannelTime();
        double elapsedSeconds = elapsedMs / 1000.0;
        return Math.min(1.0, elapsedSeconds / maxTime);
    }

    /**
     * Get the skill currently being channeled.
     *
     * @return The skill, or null if not channeling or skill not found
     */
    public static Skill getCurrentChannelingSkill() {
        if (!isChanneling) {
            return null;
        }
        ResourceLocation skillId = ClientSkillData.getSkillInSlot(currentSlot);
        return skillId != null ? SkillRegistry.getInstance().getSkill(skillId) : null;
    }

    // ==================== Targeting Resolution ====================

    /**
     * Resolve targeting based on the skill's targeting type.
     *
     * @param targetingType The type of targeting to resolve
     * @return A snapshot of the resolved targeting
     */
    public static TargetingSnapshot resolveTargeting(TargetingType targetingType) {
        if (MC.player == null) {
            return createMinimalSnapshot();
        }

        return switch (targetingType) {
            case NONE -> createMinimalSnapshot();
            case DIRECTION -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(32.0)
                    .allowedTypes(TargetType.DIRECTION, TargetType.POSITION)
                    .build());
            case POSITION -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(32.0)
                    .allowedTypes(TargetType.POSITION)
                    .build());
            case ENTITY -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(32.0)
                    .allowedTypes(TargetType.ENTITY, TargetType.POSITION)
                    .relationFilter(TargetRelation.ANY)
                    .allowTargetSelf(true)
                    .build());
        };
    }

    /**
     * Create a minimal targeting snapshot for skills with no targeting.
     *
     * @return A snapshot with player's position and look direction
     */
    private static TargetingSnapshot createMinimalSnapshot() {
        return new TargetingSnapshot(
                MC.player.getEyePosition(),
                MC.player.getLookAngle(),
                MC.player.position(),
                -1, false, true, 0.0,
                EnumSet.of(TargetType.DIRECTION)
        );
    }

    // ==================== Skill Casting Validation ====================

    /**
     * Validate channel time against skill requirements.
     *
     * @param skill           The skill being cast
     * @param channelTimeMs   The channel time in milliseconds
     * @param maxChannelTime  The maximum channel time from the skill
     * @return The validated channel time in seconds, or -1 if validation failed
     */
    public static double validateChannelTime(Skill skill, long channelTimeMs, double maxChannelTime) {
        double channelTimeSeconds = channelTimeMs / 1000.0;

        // Client-side validation (all logic here)
        if (skill.getMinChannelTime() > 0 && channelTimeSeconds < skill.getMinChannelTime()) {
            MC.player.displayClientMessage(Component.literal("§cChannel time too short! Need " +
                    String.format("%.1f", skill.getMinChannelTime()) + " seconds"), true);
            return -1;
        }

        // Cap at max (client enforces this)
        if (maxChannelTime > 0 && channelTimeSeconds > maxChannelTime) {
            channelTimeSeconds = maxChannelTime;
        }

        return channelTimeSeconds;
    }

    /**
     * Display a cast feedback message to the player.
     *
     * @param channelTimeSeconds The validated channel time in seconds
     */
    public static void displayCastFeedback(double channelTimeSeconds) {
        if (channelTimeSeconds > 0) {
            MC.player.displayClientMessage(Component.literal("§aChannel time: " +
                    String.format("%.2f", channelTimeSeconds) + "s"), true);
        }
    }

    /**
     * Reset all casting state (called on disconnect/world change).
     */
    public static void reset() {
        isChanneling = false;
        currentSlot = -1;
        maxChannelTime = 0;
        channelStartTime = 0;
    }
}
