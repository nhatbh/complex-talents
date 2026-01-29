package com.complextalents.skill.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Cancelable;

/**
 * Core skill execution event.
 *
 * <p><b>Event Phase:</b> Fourth in the pipeline (after SkillPreExecuteEvent)</p>
 * <p><b>Cancelable:</b> Yes - prevents the actual skill effect from occurring</p>
 *
 * <p>This is the PRIMARY integration point for skills.</p>
 * <p>Skills should listen to this event and execute when event.skill == this.</p>
 *
 * <p>All skills have a channel time (0 for instant skills).
 * Channeled skills can scale their effects based on getChannelTime().</p>
 *
 * <p><b>Use cases:</b></p>
 * <ul>
 *   <li>Apply skill effects (damage, healing, buffs, etc.)</li>
 *   <li>Spawn entities or projectiles</li>
 *   <li>Trigger animations and sounds</li>
 *   <li>Modify world state</li>
 *   <li>Scale effects based on channel time</li>
 * </ul>
 *
 * <p><b>Important:</b> Skills NEVER directly call other skills.
 * All skill execution flows through this event.</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * // In your FireballSkill class:
 * public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mymod", "fireball");
 *
 * @SubscribeEvent
 * public static void onSkillExecute(SkillExecuteEvent event) {
 *     if (!event.getSkillId().equals(ID)) return;
 *
 *     ResolvedTargetData target = event.getTargetData();
 *     ServerPlayer caster = event.getPlayer();
 *     double channelTime = event.getChannelTime();
 *
 *     // Scale damage by channel time (for channeled skills)
 *     double scale = 1.0 + channelTime; // 1x base + channel bonus
 *
 *     // Spawn fireball at target position
 *     FireballEntity fireball = new FireballEntity(caster.level(), caster,
 *         target.getAimDirection().scale(scale), target.getTargetPosition());
 *     caster.level().addFreshEntity(fireball);
 * }
 * }</pre>
 */
@Cancelable
public class SkillExecuteEvent extends SkillEvent {

    private final ResolvedTargetData targetData;
    private final double channelTime;

    /**
     * Create a new skill execute event.
     *
     * @param player The player casting the skill
     * @param skillId The skill being cast
     * @param targetData The resolved target data
     * @param channelTime The channel time in seconds (0 for instant skills)
     */
    public SkillExecuteEvent(ServerPlayer player, ResourceLocation skillId,
                              ResolvedTargetData targetData, double channelTime) {
        super(player, skillId);
        this.targetData = targetData;
        this.channelTime = channelTime;
    }

    /**
     * @return The resolved target data (guaranteed valid)
     */
    public ResolvedTargetData getTargetData() {
        return targetData;
    }

    /**
     * @return The channel time in seconds (0 for instant skills, >0 for channeled)
     */
    public double getChannelTime() {
        return channelTime;
    }
}
