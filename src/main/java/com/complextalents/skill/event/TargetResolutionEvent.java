package com.complextalents.skill.event;

import com.complextalents.targeting.TargetType;
import com.complextalents.targeting.TargetingSnapshot;
import com.complextalents.util.EntityValidationHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Fired to normalize targeting data from client-provided snapshot.
 *
 * <p><b>Event Phase:</b> Second in the pipeline (after SkillCastRequestEvent)</p>
 * <p><b>Cancelable:</b> No - this event always produces valid target data</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Convert TargetingSnapshot to ResolvedTargetData</li>
 *   <li>Validate and look up entities from client-provided IDs</li>
 *   <li>Client is authoritative for fallback behavior</li>
 * </ul>
 *
 * <p><b>Resolution Rules:</b></p>
 * <ul>
 *   <li>NONE → target is caster</li>
 *   <li>DIRECTION → aimDirection + position at max range</li>
 *   <li>POSITION → exact hit position</li>
 *   <li>ENTITY → entity from snapshot (client handles fallback)</li>
 * </ul>
 *
 * <p><b>Note:</b> The client decides whether to fall back to self based on skill
 * configuration. The server trusts the client's targeting snapshot and simply
 * resolves entity IDs to actual Entity objects.</p>
 */
public class TargetResolutionEvent extends SkillEvent {

    private final TargetType targetingType;
    private final TargetingSnapshot snapshot;
    private final ResolvedTargetData resolvedTarget;

    /**
     * Create a new target resolution event.
     *
     * @param player The player casting the skill
     * @param skillId The skill being cast
     * @param targetingType The skill's targeting type
     * @param snapshot The client-provided targeting snapshot (client authoritative)
     */
    public TargetResolutionEvent(ServerPlayer player, ResourceLocation skillId,
                                  TargetType targetingType, TargetingSnapshot snapshot) {
        super(player, skillId);
        this.targetingType = targetingType;
        this.snapshot = snapshot;
        this.resolvedTarget = resolveTarget();
    }

    /**
     * @return The skill's targeting type
     */
    public TargetType getTargetingType() {
        return targetingType;
    }

    /**
     * @return The original client-provided targeting snapshot
     */
    public TargetingSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * @return The resolved target data
     */
    public ResolvedTargetData getResolvedTarget() {
        return resolvedTarget;
    }

    /**
     * Resolve the target based on targeting type rules.
     * Client is authoritative for fallback behavior.
     *
     * @return ResolvedTargetData from client snapshot
     */
    private ResolvedTargetData resolveTarget() {
        Vec3 aimDirection = snapshot.getAimDirection();
        Vec3 targetPosition = snapshot.getTargetPosition();

        // DEBUG: Log resolution start
        player.sendSystemMessage(Component.literal("§7[DEBUG]   §eResolving: " + targetingType));

        return switch (targetingType) {
            case NONE -> {
                // Targetless skills target the caster
                player.sendSystemMessage(Component.literal("§7[DEBUG]   §7Rule: NONE → Target caster"));
                yield new ResolvedTargetData(
                        player,
                        player,
                        player.position(),
                        aimDirection,
                        true,
                        true,
                        0.0
                );
            }

            case DIRECTION, POSITION, ENTITY -> {
                Entity targetEntity = null;
                boolean isAlly = false;
                boolean isSelfTarget = false;

                if (snapshot.hasEntity()) {
                    Entity hitEntity = EntityValidationHelper.validateEntity(player.level(), snapshot.getTargetEntityId());
                    if (hitEntity != null) {
                        targetEntity = hitEntity;
                        isAlly = snapshot.isAlly();
                        isSelfTarget = (hitEntity == player);
                        player.sendSystemMessage(Component.literal("§7[DEBUG]   §aRule: " + targetingType + " → " +
                                (isSelfTarget ? "SELF" : hitEntity.getName().getString())));
                    } else {
                        player.sendSystemMessage(Component.literal("§7[DEBUG]   §cRule: " + targetingType + " → Entity from snapshot no longer valid"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§7[DEBUG]   §7Rule: " + targetingType + " → No entity"));
                }

                yield new ResolvedTargetData(
                        player,
                        targetEntity,
                        targetPosition,
                        aimDirection,
                        isSelfTarget,
                        isAlly,
                        snapshot.getDistance()
                );
            }
        };
    }
}
