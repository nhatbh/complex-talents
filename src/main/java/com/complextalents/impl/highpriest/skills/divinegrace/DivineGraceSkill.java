package com.complextalents.impl.highpriest.skills.divinegrace;

import com.complextalents.impl.highpriest.effect.DivineExaltationEffect;
import com.complextalents.impl.highpriest.effect.HighPriestEffects;
import com.complextalents.impl.highpriest.entity.SanctuaryBarrierEntity;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.highpriest.SpawnBarrierFXPacket;
import com.complextalents.origin.OriginManager;
import com.complextalents.passive.PassiveManager;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.targeting.TargetType;
import com.complextalents.util.TeamHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Divine Grace - A channeled AoE aura skill with three distinct tiers based on channel time.
 * <p>
 * Requires full Grace stacks (10) to activate. The effect depends on how long the skill is channeled:
 * <ul>
 *   <li>Tier 1: Sacred Withdrawal (&lt; 1 second) - Allies gain speed for retreat/reset</li>
 *   <li>Tier 2: Divine Exaltation (1-2 seconds) - Allies gain damage boost and generate piety for caster on hit</li>
 *   <li>Tier 3: The Sanctuary (2+ seconds) - Creates a spherical barrier that blocks enemies and projectiles</li>
 * </ul>
 * <p>
 * <strong>Properties:</strong>
 * <ul>
 *   <li>Resource: Piety (cost varies by tier)</li>
 *   <li>Cooldown: 8 seconds</li>
 *   <li>Max Channel Time: 3 seconds</li>
 *   <li>Max Level: 4</li>
 *   <li>Aura Radius: 8/10/12/15 blocks (by level)</li>
 *   <li>Requires: 10 Grace stacks</li>
 * </ul>
 */
public class DivineGraceSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "divine_grace");

    /**
     * Register this skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "divine_grace")
                .nature(SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/highpriest/divine_grace.png"))
                .maxRange(0.0)  // Self-cast, AoE around player
                .minChannelTime(0.0)  // Can release anytime
                .maxChannelTime(3.0)  // Max 3 seconds for Tier 3
                .activeCooldown(8.0)  // 8 second cooldown
                .setMaxLevel(4)
                // Aura radius: 8/10/12/15 blocks
                .scaledStat("auraRadius", new double[]{8.0, 10.0, 12.0, 15.0})
                // Tier 1 speed duration multiplier: 1/2/3/4
                .scaledStat("speedDurationMult", new double[]{1.0, 2.0, 3.0, 4.0})
                // Tier 2 damage boost: 10/15/20/25%
                .scaledStat("damageBoost", new double[]{0.10, 0.15, 0.20, 0.25})
                // Tier 2 piety per hit: 3/5/7/10
                .scaledStat("pietyPerHit", new double[]{3.0, 5.0, 7.0, 10.0})
                // Tier 3 barrier HP multiplier: 2/3/4/5
                .scaledStat("barrierHpMult", new double[]{2.0, 3.0, 4.0, 5.0})
                // Tier 2 duration: 20 seconds
                .scaledStat("exaltationDuration", new double[]{20.0, 20.0, 20.0, 20.0})
                .validate((context, player) -> {
                    // Must have 10 Grace stacks to activate
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    if (!PassiveManager.hasPassiveStacks(serverPlayer, "grace", 10)) {
                        serverPlayer.sendSystemMessage(Component.literal("§cDivine Grace requires 10 Grace stacks!"));
                        return false;
                    }
                    return true;
                })
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ServerLevel level = (ServerLevel) player.level();

                    // Get channel time and determine tier
                    double channelTime = context.channelTime();

                    // Get scaled stats
                    double auraRadius = context.getStat("auraRadius");

                    if (channelTime < 1.0) {
                        // Tier 1: Sacred Withdrawal
                        applySacredWithdrawal(context, player, level, auraRadius);
                    } else if (channelTime < 2.0) {
                        // Tier 2: Divine Exaltation
                        applyDivineExaltation(context, player, level, auraRadius);
                    } else {
                        // Tier 3: The Sanctuary
                        applySanctuary(context, player, level, auraRadius);
                    }
                })
                .register();
    }

    /**
     * Tier 1: Sacred Withdrawal
     * Consumes 1/3 of current Piety.
     * Allies in range gain speed for duration based on piety consumed.
     */
    private static void applySacredWithdrawal(Skill.ExecutionContext context, ServerPlayer player,
                                               ServerLevel level, double auraRadius) {
        // Calculate piety to consume (1/3 current)
        double currentPiety = OriginManager.getResource(player);
        double pietyConsumed = currentPiety / 3.0;
        OriginManager.modifyResource(player, -pietyConsumed);

        // Get speed duration multiplier
        double durationMult = context.getStat("speedDurationMult");
        int durationTicks = (int) (pietyConsumed * 20.0 * durationMult);

        // Always apply to caster first
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                durationTicks,
                1, // Amplifier for significant speed boost
                false, false, true
        ));

        // Get allies in range (excluding caster since they already got it)
        List<LivingEntity> allies = getNearbyAllies(player, auraRadius).stream()
                .filter(e -> e != player)
                .toList();

        // Apply speed effect to each ally
        for (LivingEntity ally : allies) {
            ally.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SPEED,
                    durationTicks,
                    1, // Amplifier for significant speed boost
                    false, false, true
            ));
        }

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Feedback message
        player.sendSystemMessage(Component.literal(String.format(
                "§eSacred Withdrawal! §7%d allies gain speed for §f%.1f§7 seconds.",
                allies.size() + 1, durationTicks / 20.0
        )));
    }

    /**
     * Tier 2: Divine Exaltation
     * Consumes 2/3 of current Piety.
     * Allies in range gain damage boost and generate piety for caster on hit.
     */
    private static void applyDivineExaltation(Skill.ExecutionContext context, ServerPlayer player,
                                              ServerLevel level, double auraRadius) {
        // Calculate piety to consume (2/3 current)
        double currentPiety = OriginManager.getResource(player);
        double pietyConsumed = (currentPiety * 2.0) / 3.0;
        OriginManager.modifyResource(player, -pietyConsumed);

        // Get effect stats
        double pietyPerHit = context.getStat("pietyPerHit");
        int skillLevel = context.skillLevel();
        double duration = context.getStat("exaltationDuration");
        int durationTicks = (int) (duration * 20);

        // Always apply to caster first
        MobEffectInstance playerEffect = new MobEffectInstance(
                HighPriestEffects.DIVINE_EXALTATION.get(),
                durationTicks,
                skillLevel - 1,
                false, false, true
        );
        player.addEffect(playerEffect);
        DivineExaltationEffect.initializeEffectData(
                player, player.getUUID(), skillLevel, pietyPerHit
        );

        // Get allies in range (excluding caster since they already got it)
        List<LivingEntity> allies = getNearbyAllies(player, auraRadius).stream()
                .filter(e -> e != player)
                .toList();

        // Apply effect to each ally
        for (LivingEntity ally : allies) {
            MobEffectInstance effect = new MobEffectInstance(
                    HighPriestEffects.DIVINE_EXALTATION.get(),
                    durationTicks,
                    skillLevel - 1,
                    false, false, true
            );
            ally.addEffect(effect);

            // Initialize effect data
            DivineExaltationEffect.initializeEffectData(
                    ally, player.getUUID(), skillLevel, pietyPerHit
            );
        }

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.8f);

        // Feedback message
        player.sendSystemMessage(Component.literal(String.format(
                "§6Divine Exaltation! §7%d allies gain §e+%d%%§7 damage. Hits generate piety.",
                allies.size() + 1, (int) (context.getStat("damageBoost") * 100)
        )));
    }

    /**
     * Tier 3: The Sanctuary
     * Consumes ALL Piety.
     * Pushes all enemies outside radius, creates spherical barrier.
     */
    private static void applySanctuary(Skill.ExecutionContext context, ServerPlayer player,
                                       ServerLevel level, double auraRadius) {
        // Consume ALL Piety
        double currentPiety = OriginManager.getResource(player);
        float pietyConsumed = (float) currentPiety;
        OriginManager.setResource(player, 0.0);

        // Get barrier HP multiplier
        double barrierHpMult = context.getStat("barrierHpMult");
        float barrierHP = pietyConsumed * (float) barrierHpMult;

        // First, expel all enemies immediately (one-time scan)
        AABB area = new AABB(player.blockPosition()).inflate(auraRadius);
        List<LivingEntity> enemies = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                e -> e != player && !TeamHelper.isAlly(player, e)
        );

        Vec3 playerPos = player.position();
        int expelledCount = 0;

        for (LivingEntity enemy : enemies) {
            Vec3 push = enemy.position()
                    .subtract(playerPos)
                    .normalize()
                    .scale(2.5);

            enemy.push(push.x, 0.5, push.z);
            enemy.hurtMarked = true;
            expelledCount++;
        }

        // Create barrier entity
        SanctuaryBarrierEntity barrier = new SanctuaryBarrierEntity(
                level, playerPos, (float) auraRadius, barrierHP, player.getUUID()
        );
        level.addFreshEntity(barrier);

        // Set synced data after entity is added to the world
        // Set target radius - barrier will scale up from tiny to this size over 1 second
        barrier.setTargetRadius((float) auraRadius);
        barrier.setHp(barrierHP);
        barrier.setMaxHp(barrierHP);

        // Send creation effect packet
        PacketHandler.sendToNearby(
            new SpawnBarrierFXPacket(
                barrier.getX(), barrier.getY(), barrier.getZ(),
                SpawnBarrierFXPacket.EffectType.CREATED,
                barrier.getTargetRadius(),
                barrier.getId()
            ),
            level, barrier.position()
        );

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.5f, 0.8f);

        // Feedback message
        player.sendSystemMessage(Component.literal(String.format(
                "§eThe Sanctuary! §7Expelled §f%d§7 enemies. Barrier HP: §6%.0f§7. Radius: §f%.0f§7 blocks.",
                expelledCount, barrierHP, auraRadius
        )));
    }

    /**
     * Get nearby allies within the given radius.
     */
    private static List<LivingEntity> getNearbyAllies(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> TeamHelper.isAlly(player, e)
        );
    }
}
