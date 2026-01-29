package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.skill.form.SkillFormDefinition;
import com.complextalents.skill.form.SkillFormRegistry;
import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Example FORM skill: Elemental Overload
 * <p>
 * When assigned to slot 4 and activated, this form transforms skills in slots 1-3 into enhanced versions:
 * - Slot 1 (Fireball) -> Mega Fireball: Huge AoE explosion
 * - Slot 2 (Thunder Strike) -> Chain Lightning: Hits up to 5 enemies
 * - Slot 3 (Auto Shield) -> Lightning Shield: Damages nearby enemies
 * <p>
 * Enhanced skills are full BuiltSkills with their own cooldowns, resource costs, and scaled stats.
 * <p>
 * Duration: 15 seconds
 * Cooldown: 60 seconds
 * <p>
 * Usage:
 * - /skill assign 4 complextalents:elemental_overload
 * - Cast the skill to activate the form
 * - Cast again or wait for duration to deactivate
 */
public class ExampleElementalOverloadForm {

    public static final String ID = "compleplexaltents:elemental_overload";

    /**
     * Register this example form skill and its enhanced skills.
     */
    public static void register() {
        // ========================================================================
        // STEP 1: Register the enhanced skills as full BuiltSkills
        // These skills replace the skills in slots 1-3 when the form is active
        // ========================================================================

        // Enhanced Slot 1 (internal slot 0): Mega Fireball
        SkillBuilder.create("complextalents", "elemental_overload/slot0")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.DIRECTION)
                .maxRange(32.0)
                .activeCooldown(4.0)  // Enhanced skill has its own cooldown
                .resourceCost(30.0, "mana")  // Higher cost for enhanced version
                .setMaxLevel(5)
                .scaledStat("damage", new double[]{20.0, 25.0, 30.0, 35.0, 40.0})
                .scaledStat("radius", new double[]{4.0, 4.5, 5.0, 5.5, 6.0})
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);
                    ServerLevel level = player.serverLevel();

                    // Get scaled stats
                    double damage = context.getStat("damage");
                    double radius = context.getStat("radius");

                    Vec3 eyePos = player.getEyePosition(1.0f);
                    Vec3 dir = targetData.getAimDirection();
                    Vec3 targetPos = eyePos.add(dir.scale(16));

                    // Create massive explosion
                    level.explode(null, targetPos.x, targetPos.y, targetPos.z,
                            (float) radius, ServerLevel.ExplosionInteraction.MOB);

                    // Play sounds
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.8f);
                    level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                            SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0f, 0.8f);

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00A6cMEGA FIREBALL! \u00A7e" + String.format("%.0f", damage) + " damage, " +
                                            String.format("%.1f", radius) + " block radius!"
                            ),
                            true
                    );
                })
                .register();

        // Enhanced Slot 2 (internal slot 1): Chain Lightning
        SkillBuilder.create("complextalents", "elemental_overload/slot1")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.ENTITY)
                .maxRange(32.0)
                .activeCooldown(5.0)
                .resourceCost(35.0, "mana")
                .setMaxLevel(5)
                .scaledStat("mainDamage", new double[]{25.0, 30.0, 35.0, 40.0, 50.0})
                .scaledStat("chainDamage", new double[]{15.0, 18.0, 20.0, 22.0, 25.0})
                .scaledStat("chains", new double[]{4, 4, 5, 5, 6})  // Number of chain targets
                .scaledStat("chainRange", new double[]{12.0, 14.0, 16.0, 18.0, 20.0})
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);
                    ServerLevel level = player.serverLevel();

                    // Get scaled stats
                    double mainDamage = context.getStat("mainDamage");
                    double chainDamage = context.getStat("chainDamage");
                    int maxChains = (int) context.getStat("chains");
                    double chainRange = context.getStat("chainRange");

                    Entity target = targetData.getTargetEntity();
                    if (!(target instanceof LivingEntity livingTarget)) {
                        return;
                    }

                    // Hit main target hard
                    livingTarget.hurt(level.damageSources().magic(), (float) mainDamage);
                    EntityType.LIGHTNING_BOLT.spawn(level, livingTarget.blockPosition(), null);

                    level.playSound(null, livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.5f, 1.2f);

                    // Chain to nearby enemies
                    Entity lastTarget = livingTarget;
                    int chains = 0;
                    for (Entity nearby : level.getEntitiesOfClass(LivingEntity.class,
                            lastTarget.getBoundingBox().inflate(chainRange))) {
                        if (nearby != lastTarget && nearby != player && chains < maxChains) {
                            nearby.hurt(level.damageSources().magic(), (float) chainDamage);
                            EntityType.LIGHTNING_BOLT.spawn(level, nearby.blockPosition(), null);

                            level.playSound(null, nearby.getX(), nearby.getY(), nearby.getZ(),
                                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.4f);

                            lastTarget = nearby;
                            chains++;
                        }
                    }

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00A7bCHAIN LIGHTNING! \u00A7fHit " + (chains + 1) + " targets! " +
                                            String.format("%.0f", mainDamage) + " -> " +
                                            String.format("%.0f", chainDamage) + " damage"
                            ),
                            true
                    );
                })
                .register();

        // Enhanced Slot 3 (internal slot 2): Lightning Aura (damaging shield)
        SkillBuilder.create("complextalents", "elemental_overload/slot2")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .activeCooldown(3.0)
                .resourceCost(25.0, "mana")
                .setMaxLevel(5)
                .scaledStat("damage", new double[]{12.0, 15.0, 18.0, 20.0, 25.0})
                .scaledStat("radius", new double[]{6.0, 7.0, 8.0, 9.0, 10.0})
                .scaledStat("knockback", new double[]{1.5, 1.8, 2.0, 2.2, 2.5})
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ServerLevel level = player.serverLevel();

                    // Get scaled stats
                    double damage = context.getStat("damage");
                    double radius = context.getStat("radius");
                    double knockbackStrength = context.getStat("knockback");

                    // Create a damaging aura around the player
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.5f);

                    int hitCount = 0;
                    // Damage all nearby enemies
                    for (Entity nearby : level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(radius))) {
                        if (nearby != player) {
                            nearby.hurt(level.damageSources().magic(), (float) damage);
                            // Visual lightning effect
                            EntityType.LIGHTNING_BOLT.spawn(level, nearby.blockPosition(), null);
                            hitCount++;
                        }
                    }

                    // Knockback all nearby entities
                    for (Entity nearby : level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(radius))) {
                        if (nearby != player && nearby.isAlive()) {
                            Vec3 knockback = nearby.position().subtract(player.position()).normalize().scale(knockbackStrength);
                            nearby.setDeltaMovement(nearby.getDeltaMovement().add(knockback));
                            nearby.hurtMarked = true;
                        }
                    }

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00A7eLIGHTNING AURA! \u00A7fHit " + hitCount + " enemies! " +
                                            String.format("%.0f", damage) + " damage, " +
                                            String.format("%.1f", radius) + " block radius"
                            ),
                            true
                    );
                })
                .register();

        // ========================================================================
        // STEP 2: Register the form skill itself (assigned to slot 4)
        // ========================================================================

        SkillBuilder.create("complextalents", "elemental_overload")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .activeCooldown(60.0)  // 60 second cooldown
                .resourceCost(100.0, "mana")  // High cost to activate
                .toggleable(true)
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(ServerPlayer.class);
                    ServerLevel level = player.serverLevel();

                    // Play activation effects
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 1.0f);

                    // Spawn visual lightning effects
                    for (int i = 0; i < 10; i++) {
                        EntityType.LIGHTNING_BOLT.spawn(level, player.blockPosition(), null);
                    }

                    player.sendSystemMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "\u00A76\u00A7lELEMENTAL OVERLOAD ACTIVATED! \u00A7eYour skills are now empowered!"
                            ),
                            true
                    );
                })
                .register();

        // ========================================================================
        // STEP 3: Register the form definition linking enhanced skills to slots
        // Maps internal slot 0, 1, 2 to the enhanced skills (user-facing slots 1, 2, 3)
        // ========================================================================

        ResourceLocation formId = ResourceLocation.fromNamespaceAndPath("complextalents", "elemental_overload");
        SkillFormDefinition definition = SkillFormDefinition.builder(formId)
                .duration(15.0)  // 15 seconds
                .cooldown(60.0)  // 60 second cooldown after form ends
                // Map internal slot 0 (user slot 1) -> Mega Fireball
                .enhanceSlot0(ResourceLocation.fromNamespaceAndPath("complextalents", "elemental_overload/slot0"))
                // Map internal slot 1 (user slot 2) -> Chain Lightning
                .enhanceSlot1(ResourceLocation.fromNamespaceAndPath("complextalents", "elemental_overload/slot1"))
                // Map internal slot 2 (user slot 3) -> Lightning Aura
                .enhanceSlot2(ResourceLocation.fromNamespaceAndPath("complextalents", "elemental_overload/slot2"))
                .build();

        SkillFormRegistry.getInstance().register(definition);
    }
}
