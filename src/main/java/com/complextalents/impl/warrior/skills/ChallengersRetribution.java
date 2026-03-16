package com.complextalents.impl.warrior.skills;

import com.complextalents.impl.warrior.WarriorOriginHandler;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.skill.Skill;
import com.complextalents.util.UUIDHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.S2CSpawnAAAParticlePacket;
import org.joml.Vector3f;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import net.minecraft.world.InteractionHand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Charge skill: Challenger's Retribution.
 * Holds a shield, taunts enemies, and reflects absorbed damage.
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ChallengersRetribution {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents",
            "challengers_retribution");
    private static final UUID SLOWNESS_UUID = UUIDHelper.generateAttributeModifierUUID("warrior",
            "retribution_slowness");
    private static final UUID KB_RESIST_UUID = UUIDHelper.generateAttributeModifierUUID("warrior",
            "retribution_kb_resist");

    private static final Map<UUID, ShieldData> ACTIVE_SHIELDS = new HashMap<>();

    public static class ShieldData {
        public double health;
        public double maxHealth;
        public double absorbedDamage;
        public final long startTime;

        public ShieldData(double health) {
            this.health = health;
            this.maxHealth = health;
            this.absorbedDamage = 0;
            this.startTime = System.currentTimeMillis();
        }
    }

    public static void register() {
        SkillBuilder.create(ID)
                .displayName("Challenger's Retribution")
                .description("Adopt a defensive stance, taunting nearby enemies into a magical shield. Release to unleash up to 160% of all absorbed damage. Shield health scales immensely with Style Rank.")
                .nature(SkillNature.CHARGE)
                .maxChannelTime(5.0) // Maximum 5 second charge
                .scaledStat("tauntRange", new double[] { 5, 6, 7, 8, 10 })
                .scaledStat("baseHp", new double[] { 5, 8, 12, 16, 20 })
                .scaledStat("reflectPercent", new double[] { 0.5, 0.75, 1.0, 1.25, 1.6 })
                .onActive(ChallengersRetribution::startCharge)
                .onRelease(ChallengersRetribution::releaseCharge)
                .register();
    }

    private static void startCharge(Skill.ExecutionContext context, Object playerObj) {
        if (!(playerObj instanceof ServerPlayer player))
            return;

        // Calculate Shield HP
        double points = OriginManager.getResource(player);
        WarriorOriginHandler.StyleRank rank = WarriorOriginHandler.StyleRank.getRank(points);

        double baseHp = context.getStat("baseHp");
        double rankMult = 1.0;
        double rankBonusMult = 1.0;

        // Rank Multipliers: D (0.5x), C (0.7x), B (1.0x), A (1.5x), S (2.0x), SS
        // (3.0x), SSS (4.0x)
        // Bonus HP Multipliers: D (0.1x), C (0.2x), B (0.4x), A (0.6x), S (0.8x), SS
        // (0.9x), SSS (1.0x)
        switch (rank) {
            case D -> {
                rankMult = 0.5;
                rankBonusMult = 0.1;
            }
            case C -> {
                rankMult = 0.7;
                rankBonusMult = 0.2;
            }
            case B -> {
                rankMult = 1.0;
                rankBonusMult = 0.4;
            }
            case A -> {
                rankMult = 1.5;
                rankBonusMult = 0.6;
            }
            case S -> {
                rankMult = 2.0;
                rankBonusMult = 0.8;
            }
            case SS -> {
                rankMult = 3.0;
                rankBonusMult = 0.9;
            }
            case SSS -> {
                rankMult = 4.0;
                rankBonusMult = 1.0;
            }
        }

        double bonusMaxHp = Math.max(0, player.getMaxHealth() - 20.0);
        double maxBonusContrib = bonusMaxHp * 0.5;
        double shieldHp = (baseHp * rankMult) + (maxBonusContrib * rankBonusMult);

        ACTIVE_SHIELDS.put(player.getUUID(), new ShieldData(shieldHp));

        // Sync to HUD
        OriginManager.getCapability(player).ifPresent(data -> {
            data.setShieldMax(shieldHp);
            data.setShieldValue(shieldHp);
        });

        // Apply Immobilization and Knockback Resistance
        var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SLOWNESS_UUID);
            speedAttr.addTransientModifier(new AttributeModifier(SLOWNESS_UUID, "Retribution Slowness", -0.9,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        var kbAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            kbAttr.removeModifier(KB_RESIST_UUID);
            kbAttr.addTransientModifier(new AttributeModifier(KB_RESIST_UUID, "Retribution KB Resist", 1.0,
                    AttributeModifier.Operation.ADDITION));
        }

        // Taunt nearby enemies
        double tauntRange = context.getStat("tauntRange");
        AABB area = player.getBoundingBox().inflate(tauntRange);
        player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player).forEach(entity -> {
            entity.setLastHurtByMob(player);
            if (entity instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(player);
            }
        });

        player.sendSystemMessage(Component.literal("\u00A76[CHARGING] \u00A7fShield: " + String.format("%.1f", shieldHp)
                + " HP | Taunting nearby enemies!"));

        // FX: Roar, Shield Block, and Taunt Particle
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.PLAYERS, 0.7f, 1.2f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS, 1.0f, 0.8f);

        PacketHandler.sendToNearby(new S2CSpawnAAAParticlePacket(
                ResourceLocation.fromNamespaceAndPath("complextalents", "taunt"),
                player.position().add(0, 1.0, 0)), (net.minecraft.server.level.ServerLevel) player.level(),
                player.position());

        // Epic Fight Animation: Play GUARD animation
        ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
        if (playerPatch != null) {
            CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
            AnimationAccessor<? extends StaticAnimation> anim = itemCap.getGuardMotion(null, yesman.epicfight.skill.guard.GuardSkill.BlockType.GUARD, playerPatch);
            if (anim != null) {
                playerPatch.playAnimationSynchronized(anim, 0.0F);
            }
        }
    }

    private static void releaseCharge(Skill.ExecutionContext context, Object playerObj, double chargeTime) {
        if (!(playerObj instanceof ServerPlayer player))
            return;

        ShieldData data = ACTIVE_SHIELDS.remove(player.getUUID());

        // Clear HUD
        OriginManager.getCapability(player).ifPresent(cap -> {
            cap.setShieldValue(0);
            cap.setShieldMax(0);
        });

        if (data != null) {
            // Remove Attributes
            var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null)
                speedAttr.removeModifier(SLOWNESS_UUID);
            var kbAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbAttr != null)
                kbAttr.removeModifier(KB_RESIST_UUID);

            if (data.health > 0) {
                // Reflect Damage
                double reflectMult = context.getStat("reflectPercent");
                double damage = data.absorbedDamage * reflectMult;

                // Release AoE burst
                double burstRange = 6.0;
                AABB area = player.getBoundingBox().inflate(burstRange);
                player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player).forEach(entity -> {
                    entity.hurt(player.damageSources().mobAttack(player), (float) damage);

                    // Knockback
                    double kbMagnitude = 1.5 + (data.absorbedDamage * 0.1);
                    Vec3 dir = entity.position().subtract(player.position()).normalize();
                    entity.knockback(kbMagnitude, -dir.x, -dir.z);
                });

                player.sendSystemMessage(Component.literal(
                        "\u00A7a[RELEASE] \u00A7fUnleashed " + String.format("%.1f", damage) + " reflect damage!"));

                // Epic Fight Animation: Play current weapon attack animation
                ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
                if (playerPatch != null) {
                    CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                    // BlockType.GUARD is used here as a reference pattern from GuardSkill
                    List<AnimationAccessor<? extends AttackAnimation>> anim = itemCap.getAutoAttackMotion(playerPatch);
                    if (anim != null) {
                        playerPatch.playAnimationSynchronized(anim.get(0), 0.0F);
                    }
                }

                // FX: Reflect Sounds (Clang + Explode) and Particle
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvent.createVariableRangeEvent(
                                ResourceLocation.fromNamespaceAndPath("complextalents", "grandmaster.gate_hit")),
                        SoundSource.PLAYERS, 1.0f, 1.0f);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);

                Vector3f rotation = new Vector3f(0, (float) Math.toRadians(-player.getYRot() + 180), 0);
                PacketHandler.sendToNearby(new S2CSpawnAAAParticlePacket(
                        ResourceLocation.fromNamespaceAndPath("complextalents", "reflect"),
                        player.position().add(0, 1.2, 0),
                        rotation,
                        0.4f), (net.minecraft.server.level.ServerLevel) player.level(), player.position());
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShieldData data = ACTIVE_SHIELDS.get(player.getUUID());
            if (data != null) {
                double amount = event.getAmount();
                data.absorbedDamage += amount;
                data.health -= amount;

                if (data.health <= 0) {
                    // Shield BROKEN
                    ACTIVE_SHIELDS.remove(player.getUUID());

                    // Clear HUD
                    OriginManager.getCapability(player).ifPresent(cap -> {
                        cap.setShieldValue(0);
                        cap.setShieldMax(0);
                    });

                    // Remove Attributes
                    var speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speedAttr != null)
                        speedAttr.removeModifier(SLOWNESS_UUID);
                    var kbAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                    if (kbAttr != null)
                        kbAttr.removeModifier(KB_RESIST_UUID);

                    player.sendSystemMessage(Component.literal("\u00A7c[SHIELD BROKEN] \u00A77Skill canceled!"));

                    // Epic Fight Animation: Play GUARD_BREAK animation
                    ServerPlayerPatch playerPatch = EpicFightCapabilities.getEntityPatch(player, ServerPlayerPatch.class);
                    if (playerPatch != null) {
                        CapabilityItem itemCap = playerPatch.getHoldingItemCapability(InteractionHand.MAIN_HAND);
                        AnimationAccessor<? extends StaticAnimation> anim = itemCap.getGuardMotion(null, yesman.epicfight.skill.guard.GuardSkill.BlockType.GUARD_BREAK, playerPatch);
                        if (anim != null) {
                            playerPatch.playAnimationSynchronized(anim, 0.0F);
                        }
                    }

                    // Cancellations for Skill system (requires some way to tell the client/handler
                    // to stop)
                    // Currently, the server just stops release from happening if data is removed
                } else {
                    // Update HUD
                    OriginManager.getCapability(player).ifPresent(cap -> {
                        cap.setShieldValue(data.health);
                    });
                }

                // Absorb damage
                event.setAmount(0);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (ACTIVE_SHIELDS.containsKey(player.getUUID())) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0, player.getDeltaMovement().z);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (ACTIVE_SHIELDS.containsKey(player.getUUID())) {
                if (player.tickCount % 2 == 0) {
                    ((net.minecraft.server.level.ServerLevel) player.level()).sendParticles(
                            ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            2, 0.3, 0.5, 0.3, 0.02);
                }
            }
        }
    }
}
