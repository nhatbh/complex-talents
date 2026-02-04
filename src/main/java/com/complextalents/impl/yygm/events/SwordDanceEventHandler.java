package com.complextalents.impl.yygm.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.skill.SwordDanceSkill;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.SwordDanceGateActivatePacket;
import com.complextalents.skill.capability.SkillDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event handler for Sword Dance dash processing.
 * Handles tick-based dash movement and post-dash gate activation logic.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class SwordDanceEventHandler {

    /**
     * Server tick handler for dash processing and target immobilization.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Process dashing players
            for (ServerPlayer player : level.players()) {
                if (SwordDanceSkill.isDashing(player)) {
                    processDashTick(player);
                }
            }
        }
    }

    /**
     * Process a single tick of dash movement for a player.
     * Server only tracks progress - client handles interpolation.
     * Final position is enforced when dash completes.
     */
    private static void processDashTick(ServerPlayer player) {
        int tick = SwordDanceSkill.incrementDashTick(player);

        if (tick >= SwordDanceSkill.DASH_DURATION_TICKS) {
            // Dash complete - enforce final position and process post-dash logic
            Vec3 endPos = SwordDanceSkill.getDashEndPos(player);
            player.teleportTo(endPos.x, endPos.y, endPos.z);
            finalizeDash(player);
        }
        // Client handles per-frame interpolation for smooth visuals
    }

    /**
     * Finalize the dash and process gate activation logic.
     */
    private static void finalizeDash(ServerPlayer player) {
        Vec3 startPos = SwordDanceSkill.getDashStartPos(player);
        Vec3 endPos = SwordDanceSkill.getDashEndPos(player);
        LivingEntity target = SwordDanceSkill.getDashTarget(player);

        if (target != null && target.isAlive()) {
            // Determine dash case (through, near, or miss)
            DashResult result = calculateDashResult(startPos, endPos, target);

            switch (result.caseType) {
                case THROUGH -> handleThroughTarget(player, target, startPos, endPos);
                case NEAR -> handleNearTarget(player, target, endPos);
                case MISS -> {
                    // No gate activation, just cleanup
                }
            }
        }

        // Cleanup dash data
        SwordDanceSkill.clearDashData(player);

        TalentsMod.LOGGER.debug("Sword Dance: Dash finalized for {}", player.getName().getString());
    }

    /**
     * Calculate the result of the dash relative to the target.
     */
    private static DashResult calculateDashResult(Vec3 startPos, Vec3 endPos, LivingEntity target) {
        AABB targetHitbox = target.getBoundingBox();
        Vec3 targetCenter = target.position();

        // Check if dash line segment intersects target hitbox
        Vec3 direction = endPos.subtract(startPos);
        double maxLength = direction.length();
        direction = direction.normalize();

        // Inflate hitbox slightly for detection
        AABB hitbox = targetHitbox.inflate(0.1);

        // Simple line segment intersection check
        // Sample points along the dash path
        int samples = 10;
        for (int i = 0; i <= samples; i++) {
            double t = (double) i / samples;
            Vec3 samplePos = startPos.add(endPos.subtract(startPos).scale(t));

            if (hitbox.contains(samplePos)) {
                // Dash goes through target
                return new DashResult(DashCase.THROUGH, samplePos);
            }
        }

        // Check if end position is near target
        double distanceToCenter = endPos.distanceTo(targetCenter);
        if (distanceToCenter <= SwordDanceSkill.NEAR_DISTANCE + target.getBbWidth() / 2.0) {
            return new DashResult(DashCase.NEAR, endPos);
        }

        return new DashResult(DashCase.MISS, endPos);
    }

    /**
     * Handle the case where dash goes through the target.
     * Activates two gates (start and end angles).
     * Simplified: count correct gates, gain 1 Equilibrium after 2, then reset.
     * Note: Position clamping is already done during dash initialization.
     */
    private static void handleThroughTarget(ServerPlayer player, LivingEntity target,
                                            Vec3 startPos, Vec3 endPos) {
        // Calculate two angles: from target to start, and from target to end
        double startAngle = calculateAngleFromTarget(target, startPos);
        double endAngle = calculateAngleFromTarget(target, endPos);

        // Activate gates in sequence
        int correctGates = 0;

        // Start position gate activation
        YYGMGateHitEvent startEvent = activateGateAtAngle(player, target, startAngle);
        if (startEvent.getHitResult() == YYGMGateHitEvent.HitResult.TRUE_GATE) {
            correctGates++;
            // Deal YYGM true damage for correct gate hit
            dealYYGMTrueDamage(player, target);
        }

        // End position gate activation
        YYGMGateHitEvent endEvent = activateGateAtAngle(player, target, endAngle);
        if (endEvent.getHitResult() == YYGMGateHitEvent.HitResult.TRUE_GATE) {
            correctGates++;
            // Deal YYGM true damage for correct gate hit
            dealYYGMTrueDamage(player, target);
        }

        // Equilibrium is handled by the centralized YYGMGateHitEvent handler
        // No need to duplicate logic here

        // Apply cooldown refund
        ResourceLocation skillId = SwordDanceSkill.ID;
        if (correctGates == 2) {
            // Both gates correct - full refund
            int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                    .map(data -> data.getSkillLevel(skillId))
                    .orElse(1);
            double maxCooldown = SwordDanceSkill.getCooldown(skillLevel);
            applyCooldownRefund(player, skillId, maxCooldown);
            TalentsMod.LOGGER.debug("Sword Dance: Full refund (both gates correct)");
        } else if (correctGates == 1) {
            // One gate correct - 25% refund
            int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                    .map(data -> data.getSkillLevel(skillId))
                    .orElse(1);
            double maxCooldown = SwordDanceSkill.getCooldown(skillLevel);
            applyCooldownRefund(player, skillId, maxCooldown * SwordDanceSkill.REFUND_ONE_GATE);
            TalentsMod.LOGGER.debug("Sword Dance: 25% refund (one gate correct)");
        }
    }

    /**
     * Handle the case where dash ends near the target.
     */
    private static void handleNearTarget(ServerPlayer player, LivingEntity target, Vec3 endPos) {
        // Calculate angle from target to dash end position
        double angle = calculateAngleFromTarget(target, endPos);

        // Activate gate
        YYGMGateHitEvent event = activateGateAtAngle(player, target, angle);

        // Deal YYGM true damage if correct gate hit
        if (event.getHitResult() == YYGMGateHitEvent.HitResult.TRUE_GATE) {
            dealYYGMTrueDamage(player, target);

            // Apply cooldown refund
            int skillLevel = player.getCapability(SkillDataProvider.SKILL_DATA)
                    .map(data -> data.getSkillLevel(SwordDanceSkill.ID))
                    .orElse(1);
            double maxCooldown = SwordDanceSkill.getCooldown(skillLevel);
            applyCooldownRefund(player, SwordDanceSkill.ID, maxCooldown * SwordDanceSkill.REFUND_ONE_GATE);
            TalentsMod.LOGGER.debug("Sword Dance: 25% refund (near target, correct gate)");
        }
    }

    /**
     * Deal YYGM true damage to target using the gate damage formula.
     * Formula: baseAttackDamage * (1 + (trueDamageMultiplier - 1) + equilibrium * equilibriumBonusPercent)
     * This makes trueDamageMultiplier and equilibriumBonus additive.
     */
    private static void dealYYGMTrueDamage(ServerPlayer player, LivingEntity target) {
        // Get base attack damage from player's attributes
        double baseAttackDamage = player.getAttributes().getValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        // Get current Equilibrium from player-global data
        int equilibrium = com.complextalents.impl.yygm.EquilibriumData.getEquilibrium(player.getUUID());

        // Get YYGM stats
        double trueDamageMult = com.complextalents.origin.OriginManager.getOriginStat(player, "trueDamageMultiplier");
        double equilibriumBonusPercent = com.complextalents.origin.OriginManager.getOriginStat(player, "equilibriumTrueDamagePercent");

        // Calculate final damage additively
        // Total multiplier = 1 (base) + (trueDamageMultiplier - 1) + (equilibrium * equilibriumBonusPercent)
        float totalMultiplier = 1.0f + (float) (trueDamageMult - 1.0) + (float) (equilibrium * equilibriumBonusPercent);
        float trueDamage = (float) baseAttackDamage * totalMultiplier;

        // Apply damage
        target.hurt(target.level().damageSources().indirectMagic(player, target), trueDamage);

        TalentsMod.LOGGER.debug("Sword Dance: Dealt {} true damage (base: {}, mult: {}, eq: {}, totalMult: {})",
                trueDamage, baseAttackDamage, trueDamageMult, equilibrium, totalMultiplier);
    }

    /**
     * Calculate the angle from target to a position.
     * Uses the same formula as HarmonizedEffect.calculateAttackAngle.
     */
    private static double calculateAngleFromTarget(LivingEntity target, Vec3 pos) {
        double dx = pos.x - target.getX();
        double dz = pos.z - target.getZ();

        double angle = Math.atan2(dx, -dz);

        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }

    /**
     * Activate a gate at a specific angle and return the event result.
     */
    private static YYGMGateHitEvent activateGateAtAngle(ServerPlayer player, LivingEntity target, double angle) {
        int compassDirection = com.complextalents.impl.yygm.effect.HarmonizedEffect.angleToCompassDirection(angle);

        // Get gate data
        int yangGate = com.complextalents.impl.yygm.effect.HarmonizedEffect.getYangGateDirection(target, player.getUUID());
        int yinGate = com.complextalents.impl.yygm.effect.HarmonizedEffect.getYinGateDirection(target, player.getUUID());
        int nextRequired = com.complextalents.impl.yygm.effect.HarmonizedEffect.getNextRequiredGate(target, player.getUUID());
        long currentTime = target.level().getGameTime();

        // Check cooldown
        long cooldownEnd = com.complextalents.impl.yygm.effect.HarmonizedEffect.getGateCooldown(target, player.getUUID());
        YYGMGateHitEvent.HitResult hitResult;
        int gateType = YYGMGateHitEvent.GATE_NONE;

        if (currentTime < cooldownEnd) {
            hitResult = YYGMGateHitEvent.HitResult.EMPTY_GATE;
        } else {
            // Determine which gate was hit
            if (compassDirection == yangGate && yangGate != com.complextalents.impl.yygm.effect.HarmonizedEffect.GATE_NONE) {
                gateType = YYGMGateHitEvent.GATE_YANG;
            } else if (compassDirection == yinGate && yinGate != com.complextalents.impl.yygm.effect.HarmonizedEffect.GATE_NONE) {
                gateType = YYGMGateHitEvent.GATE_YIN;
            }

            // Determine hit result
            if (gateType == YYGMGateHitEvent.GATE_NONE) {
                hitResult = YYGMGateHitEvent.HitResult.EMPTY_GATE;
            } else if (gateType == nextRequired) {
                hitResult = YYGMGateHitEvent.HitResult.TRUE_GATE;
            } else {
                hitResult = YYGMGateHitEvent.HitResult.FALSE_GATE;
            }
        }

        // Create event with Sword Dance flag
        YYGMGateHitEvent event = new YYGMGateHitEvent(player, target, hitResult, gateType,
                compassDirection, angle, nextRequired, true);

        // Fire event
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

        // Send visual packet to client
        if (player.level() instanceof ServerLevel level) {
            int hitResultCode = switch (hitResult) {
                case TRUE_GATE -> SwordDanceGateActivatePacket.HIT_TRUE_GATE;
                case FALSE_GATE -> SwordDanceGateActivatePacket.HIT_FALSE_GATE;
                case EMPTY_GATE -> SwordDanceGateActivatePacket.HIT_EMPTY_GATE;
            };

            PacketHandler.sendToNearby(new SwordDanceGateActivatePacket(
                    target.getId(), angle, gateType, hitResultCode
            ), level, target.position());
        }

        return event;
    }

    /**
     * Apply cooldown refund to a skill.
     */
    private static void applyCooldownRefund(ServerPlayer player, ResourceLocation skillId, double refundAmount) {
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            double remaining = data.getCooldown(skillId);
            if (remaining > 0) {
                double newCooldown = Math.max(0, remaining - refundAmount);
                data.setCooldown(skillId, newCooldown);
                data.sync();
                TalentsMod.LOGGER.debug("Sword Dance: Refunded {} seconds of cooldown, remaining: {}",
                        refundAmount, newCooldown);
            }
        });
    }

    // Dash result types
    private enum DashCase {
        THROUGH,  // Dash goes through target's hitbox
        NEAR,     // Dash ends near target (within 1 block)
        MISS      // Dash doesn't interact with target
    }

    private record DashResult(DashCase caseType, Vec3 position) {}
}
