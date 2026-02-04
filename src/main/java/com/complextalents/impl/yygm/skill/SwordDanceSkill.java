package com.complextalents.impl.yygm.skill;

import com.complextalents.TalentsMod;
import com.complextalents.impl.yygm.effect.HarmonizedEffect;
import com.complextalents.impl.yygm.origin.YinYangGrandmasterOrigin;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.yygm.SwordDanceDashPacket;
import com.complextalents.skill.BuiltSkill;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.targeting.TargetType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Sword Dance - A dash skill for Yin Yang Grandmaster.
 *
 * A lightning-fast horizontal dash of 8 blocks that activates gates and deals
 * damage to the harmonized target, with complex cooldown refund mechanics.
 */
public class SwordDanceSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "sword_dance");

    // NBT keys for dash tracking
    private static final String NBT_DASH_TICK = "sword_dance_tick";
    private static final String NBT_DASH_START_X = "sword_dance_start_x";
    private static final String NBT_DASH_START_Y = "sword_dance_start_y";
    private static final String NBT_DASH_START_Z = "sword_dance_start_z";
    private static final String NBT_DASH_END_X = "sword_dance_end_x";
    private static final String NBT_DASH_END_Y = "sword_dance_end_y";
    private static final String NBT_DASH_END_Z = "sword_dance_end_z";
    private static final String NBT_DASH_TARGET_ID = "sword_dance_target_id";
    private static final String NBT_DASH_HAS_REFUND = "sword_dance_has_refund";

    // Constants
    public static final double DASH_DISTANCE = 8.0;
    public static final int DASH_DURATION_TICKS = 10; // 0.5 seconds
    public static final double NEAR_DISTANCE = 1.0;
    public static final double HITBOX_BUFFER = 1.0;

    // Cooldown by level: [30, 25, 20, 15] seconds
    private static final double[] COOLDOWN_BY_LEVEL = {3.0, 25.0, 20.0, 15.0};
    // Refund percentages
    public static final double REFUND_ONE_GATE = 0.25; // 25%
    public static final double REFUND_BOTH_GATES = 1.0; // 100%

    /**
     * Register the Sword Dance skill.
     */
    public static void register() {
        BuiltSkill skill = SkillBuilder.create("complextalents", "sword_dance")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.DIRECTION)
                .maxRange(DASH_DISTANCE)
                .scaledCooldown(COOLDOWN_BY_LEVEL)
                .onActive(SwordDanceSkill::executeDash)
                .build();

        SkillRegistry.getInstance().register(skill);
        TalentsMod.LOGGER.info("Sword Dance skill registered");
    }

    /**
     * Execute the dash when the skill is activated.
     */
    private static void executeDash(Skill.ExecutionContext context, Object playerObj) {
        if (!(playerObj instanceof ServerPlayer player)) {
            return;
        }

        // Validate player is YYGM
        if (!YinYangGrandmasterOrigin.isYinYangGrandmaster(player)) {
            TalentsMod.LOGGER.debug("Sword Dance: Player {} is not YYGM", player.getName().getString());
            return;
        }

        // Calculate dash direction (horizontal only)
        Vec3 startPos = player.position();
        Vec3 lookAngle = player.getLookAngle();
        Vec3 direction = new Vec3(lookAngle.x, 0, lookAngle.z).normalize();
        Vec3 endPos = startPos.add(direction.scale(DASH_DISTANCE));

        // Block collision check
        if (player.level() instanceof ServerLevel level) {
            ClipContext clipContext = new ClipContext(
                    startPos,
                    endPos,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            );
            BlockHitResult blockResult = level.clip(clipContext);

            if (blockResult.getType() != BlockHitResult.Type.MISS) {
                // Cull to collision point
                endPos = blockResult.getLocation();
                TalentsMod.LOGGER.debug("Sword Dance: Dash culled by block at {}", endPos);
            }
        }

        // Get harmonized target (if any)
        LivingEntity harmonizedTarget = null;
        Integer targetId = HarmonizedEffect.getHarmonizedEntityId(player.getUUID());
        if (targetId != null && player.level() instanceof ServerLevel level) {
            net.minecraft.world.entity.Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living && living.isAlive()) {
                harmonizedTarget = living;
            }
        }

        // Store dash data in player's persistent data
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_DASH_TICK, 0);
        data.putDouble(NBT_DASH_START_X, startPos.x);
        data.putDouble(NBT_DASH_START_Y, startPos.y);
        data.putDouble(NBT_DASH_START_Z, startPos.z);
        data.putDouble(NBT_DASH_END_X, endPos.x);
        data.putDouble(NBT_DASH_END_Y, endPos.y);
        data.putDouble(NBT_DASH_END_Z, endPos.z);
        data.putInt(NBT_DASH_TARGET_ID, harmonizedTarget != null ? harmonizedTarget.getId() : -1);

        TalentsMod.LOGGER.info("Sword Dance: Dash data stored for {}, start: {}, end: {}",
                player.getName().getString(), startPos, endPos);

        // Send packet to client for visual effects
        if (player.level() instanceof ServerLevel level) {
            PacketHandler.sendTo(new SwordDanceDashPacket(
                    player.getId(),
                    startPos,
                    endPos,
                    DASH_DURATION_TICKS
            ), player);
            PacketHandler.sendToNearby(new SwordDanceDashPacket(
                    player.getId(),
                    startPos,
                    endPos,
                    DASH_DURATION_TICKS
            ), level, player.position());

            // Play dash start sound
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 0.5f, 1.5f);
        }

        TalentsMod.LOGGER.debug("Sword Dance: Dash started for {}, from {} to {}",
                player.getName().getString(), startPos, endPos);
    }

    /**
     * Check if a player is currently dashing.
     */
    public static boolean isDashing(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(NBT_DASH_TICK) && data.getInt(NBT_DASH_TICK) < DASH_DURATION_TICKS;
    }

    /**
     * Get the current dash tick for a player.
     */
    public static int getDashTick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getInt(NBT_DASH_TICK);
    }

    /**
     * Get the dash start position for a player.
     */
    public static Vec3 getDashStartPos(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return new Vec3(
                data.getDouble(NBT_DASH_START_X),
                data.getDouble(NBT_DASH_START_Y),
                data.getDouble(NBT_DASH_START_Z)
        );
    }

    /**
     * Get the dash end position for a player.
     */
    public static Vec3 getDashEndPos(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return new Vec3(
                data.getDouble(NBT_DASH_END_X),
                data.getDouble(NBT_DASH_END_Y),
                data.getDouble(NBT_DASH_END_Z)
        );
    }

    /**
     * Get the dash target entity for a player.
     */
    public static LivingEntity getDashTarget(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int targetId = data.getInt(NBT_DASH_TARGET_ID);
        if (targetId == -1) {
            return null;
        }
        if (player.level() instanceof ServerLevel level) {
            net.minecraft.world.entity.Entity entity = level.getEntity(targetId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /**
     * Increment the dash tick counter.
     */
    public static int incrementDashTick(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        int tick = data.getInt(NBT_DASH_TICK) + 1;
        data.putInt(NBT_DASH_TICK, tick);
        return tick;
    }

    /**
     * Check if dash has used its refund this cycle.
     */
    public static boolean hasRefund(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getBoolean(NBT_DASH_HAS_REFUND);
    }

    /**
     * Mark that dash has used its refund this cycle.
     */
    public static void markRefundUsed(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(NBT_DASH_HAS_REFUND, true);
    }

    /**
     * Clear dash data from player.
     */
    public static void clearDashData(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.remove(NBT_DASH_TICK);
        data.remove(NBT_DASH_START_X);
        data.remove(NBT_DASH_START_Y);
        data.remove(NBT_DASH_START_Z);
        data.remove(NBT_DASH_END_X);
        data.remove(NBT_DASH_END_Y);
        data.remove(NBT_DASH_END_Z);
        data.remove(NBT_DASH_TARGET_ID);
        data.remove(NBT_DASH_HAS_REFUND);
    }

    /**
     * Update the dash end position (used for clamping).
     */
    public static void updateDashEndPos(ServerPlayer player, Vec3 newEndPos) {
        CompoundTag data = player.getPersistentData();
        data.putDouble(NBT_DASH_END_X, newEndPos.x);
        data.putDouble(NBT_DASH_END_Y, newEndPos.y);
        data.putDouble(NBT_DASH_END_Z, newEndPos.z);
    }

    /**
     * Get the cooldown for a given skill level.
     */
    public static double getCooldown(int level) {
        int index = Math.min(Math.max(level - 1, 0), COOLDOWN_BY_LEVEL.length - 1);
        return COOLDOWN_BY_LEVEL[index];
    }
}
