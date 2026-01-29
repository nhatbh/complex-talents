package com.complextalents.skill.client;

import com.complextalents.skill.Skill;
import com.complextalents.targeting.TargetRelation;
import com.complextalents.targeting.TargetingRequest;
import com.complextalents.targeting.TargetingSnapshot;
import com.complextalents.targeting.TargetType;
import com.complextalents.targeting.client.ClientTargetingResolver;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side skill casting coordinator.
 *
 * <p>Delegates channeling state management to {@link ChannelManager}
 * and focuses on targeting resolution for skill casting.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Resolve targeting based on skill's TargetingType</li>
 *   <li>Provide current channeling skill for HUD rendering</li>
 *   <li>Coordinate between channeling and targeting systems</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class SkillCastingClient {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final ClientTargetingResolver RESOLVER = ClientTargetingResolver.getInstance();

    private SkillCastingClient() {}

    // ==================== Channel State Delegates ====================
    // All channeling operations are delegated to ChannelManager

    public static void startChanneling(int slotIndex, double maxTime) {
        ChannelManager.startChanneling(slotIndex, maxTime);
    }

    public static void cancelChanneling() {
        ChannelManager.cancelChanneling();
    }

    public static long endChanneling() {
        return ChannelManager.endChanneling();
    }

    public static long getCurrentChannelTime() {
        return ChannelManager.getCurrentChannelTime();
    }

    public static boolean isChanneling() {
        return ChannelManager.isChanneling();
    }

    public static int getCurrentSlot() {
        return ChannelManager.getCurrentSlot();
    }

    public static double getMaxChannelTime() {
        return ChannelManager.getMaxChannelTime();
    }

    public static double getChannelProgress() {
        return ChannelManager.getChannelProgress();
    }

    public static double validateChannelTime(Skill skill, long channelTimeMs, double maxChannelTime) {
        return ChannelManager.validateChannelTime(skill, channelTimeMs, maxChannelTime);
    }

    public static void displayCastFeedback(double channelTimeSeconds) {
        ChannelManager.displayCastFeedback(channelTimeSeconds);
    }

    public static void reset() {
        ChannelManager.reset();
    }

    // ==================== Skill Information ====================

    /**
     * Get the skill currently being channeled.
     *
     * @return The skill, or null if not channeling or skill not found
     */
    public static Skill getCurrentChannelingSkill() {
        return ChannelManager.getCurrentChannelingSkill();
    }

    // ==================== Targeting Resolution ====================

    /**
     * Resolve targeting for a skill, using the skill's configuration.
     *
     * @param skill The skill to resolve targeting for
     * @return A snapshot of the resolved targeting
     */
    public static TargetingSnapshot resolveTargeting(Skill skill) {
        if (MC.player == null || skill == null) {
            return TargetingSnapshot.createEmpty();
        }

        TargetType targetingType = skill.getTargetingType();
        double maxRange = skill.getMaxRange();

        return switch (targetingType) {
            case NONE -> TargetingSnapshot.createMinimal(
                    MC.player.getEyePosition(),
                    MC.player.getLookAngle(),
                    MC.player.position()
            );
            case DIRECTION -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(maxRange)
                    .allowedTypes(TargetType.DIRECTION, TargetType.POSITION)
                    .build());
            case POSITION -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(maxRange)
                    .allowedTypes(TargetType.POSITION)
                    .build());
            case ENTITY -> RESOLVER.resolve(TargetingRequest.builder(MC.player)
                    .maxRange(maxRange)
                    .allowedTypes(TargetType.ENTITY, TargetType.POSITION)
                    .relationFilter(TargetRelation.ANY)
                    .allowTargetSelf(skill.allowsSelfTarget())
                    .targetAllyOnly(skill.targetsAllyOnly())
                    .targetPlayerOnly(skill.targetsPlayerOnly())
                    .build());
        };
    }
}
