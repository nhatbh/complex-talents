package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;

import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import com.complextalents.registry.SoundRegistry;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.complextalents.network.Messages;
import org.joml.Vector3f;
import java.util.List;

public class LightningOPStrategy implements IOPStrategy {
    @Override
    public void execute(OPContext context, int tier) {
        float damage = context.getRawDamage();
        LivingEntity target = context.getTarget();
        ServerLevel level = context.getLevel();
        LivingEntity attacker = context.getAttacker();

        switch (tier) {
            case 3:
                applyT3(attacker, target, level, damage);
                break;
            case 2:
                applyT2(attacker, target, level, damage);
                break;
            case 1:
                applyT1(attacker, target, level, damage);
                break;
        }
    }

    private void applyT1(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
        float arcDamage = 15f * scale;

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.hurt(level.damageSources().lightningBolt(), arcDamage);

                // Visuals: Iron's lightning particles
                net.minecraft.core.particles.ParticleOptions lightning = com.complextalents.elemental.handlers.OPTickHandler
                        .getIronParticle("lightning");
                if (lightning != null) {
                    level.sendParticles(lightning, entity.getX(), entity.getY() + 1, entity.getZ(), 5, 0.1, 0.1, 0.1,
                            0.05);
                }

                count++;
                if (count >= 3)
                    break; // T1: 3 targets
            }
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float arcDamage = 30f * scale; // Higher base damage for T2

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(8.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.hurt(level.damageSources().lightningBolt(), arcDamage);

                net.minecraft.core.particles.ParticleOptions electricity = com.complextalents.elemental.handlers.OPTickHandler
                        .getIronParticle("electricity");
                if (electricity != null) {
                    level.sendParticles(electricity, entity.getX(), entity.getY() + 1, entity.getZ(), 8, 0.2, 0.2, 0.2,
                            0.05);
                }

                count++;
                if (count >= 6)
                    break; // T2: 6 targets
            }
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        float radius = 6.0f * (float) Math.sqrt(scale);

        float initialDamage = 40f * scale;
        float strikeDamage = 15f * scale;

        // Cache target position and bounding box to ensure effects play even if target is dead/removed
        net.minecraft.world.phys.Vec3 targetPos = target.position();
        net.minecraft.world.phys.AABB targetBox = target.getBoundingBox();

        // AAA Particle: Supercell
        Messages.spawnAAAParticle(level, targetPos.add(0, 1.0, 0), "supercell", new Vector3f(0), 2.5f);
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }

        // Ambient sound
        level.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundRegistry.SUPERCELL_AMBIENT.get(),
                SoundSource.PLAYERS, 1.0f, 1.0f);

        // Initial burst at 22 frames = 7 ticks
        com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, 7, () -> {
            if (attacker instanceof net.minecraft.world.entity.player.Player player) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal("Supercell Initial Burst: TICK 7"));
            }
            level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                    net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.PLAYERS, 2.0f, 1.0f);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    targetBox.inflate(radius));
            for (LivingEntity entity : nearby) {
                if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                    entity.hurt(level.damageSources().indirectMagic(attacker, attacker), initialDamage);
                }
            }
        });

        // 10 Chain lightning strikes starting at 331 frames = 110 ticks, every 12
        // frames = 4 ticks
        for (int i = 0; i < 10; i++) {
            int delay = 110 + (i * 4);
            com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, delay, () -> {
                List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                        targetBox.inflate(radius));
                List<LivingEntity> validTargets = new java.util.ArrayList<>();
                for (LivingEntity entity : nearby) {
                    if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                        validTargets.add(entity);
                    }
                }
                
                // Play sound at cached target position
                level.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                        net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0f, 1.0f);
                
                if (!validTargets.isEmpty()) {
                    LivingEntity strikeTarget = validTargets.get(level.random.nextInt(validTargets.size()));
                    strikeTarget.hurt(level.damageSources().indirectMagic(attacker, attacker), strikeDamage);
                }
            });
        }
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier == 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Thundergod's Wrath: Multi-Target Lightning Strikes (T3 Supercell)"));
            breakdown.add(String.format("   Random strikes in %.1fm radius", 6.0f * Math.sqrt(scale)));
        } else if (tier == 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Chain Surge: + %.1f Damage, 6 Targets (T2 Chain Surge)", 30f * scale));
        } else if (tier == 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Chain Lightning Damage (T1 Arcing Bolt)", 15f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.LIGHTNING;
    }
}
