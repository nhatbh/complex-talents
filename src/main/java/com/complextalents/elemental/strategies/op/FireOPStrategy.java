package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.handlers.OPTickHandler;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import com.complextalents.network.Messages;
import org.joml.Vector3f;

import java.util.List;

public class FireOPStrategy implements IOPStrategy {

    @Override
    public void execute(OPContext context, int tier) {
        float damage = context.getRawDamage();
        LivingEntity target = context.getTarget();
        ServerLevel level = context.getLevel();

        switch (tier) {
            case 3:
                applyT3(context.getAttacker(), target, level, damage);
                break;
            case 2:
                applyT2(context.getAttacker(), target, level, damage);
                break;
            case 1:
                applyT1(context.getAttacker(), target, damage);
                break;
        }
    }

    private void applyT1(LivingEntity attacker, LivingEntity target, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
        float bonusDamage = 5f * scale;
        target.hurt(target.level().damageSources().indirectMagic(attacker, target), bonusDamage); // True damage

        if (target.level() instanceof ServerLevel serverLevel) {
            net.minecraft.core.particles.ParticleOptions fire = OPTickHandler.getIronParticle("fire");
            if (fire != null) {
                serverLevel.sendParticles(fire, target.getX(), target.getY() + 1, target.getZ(), 15, 0.2, 0.2, 0.2,
                        0.1);
            }
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float aoeDamage = 15f * scale;
        double radius = 4.0;

        // Visuals: Larger flame explosion
        net.minecraft.core.particles.ParticleOptions fire = OPTickHandler.getIronParticle("fire");
        net.minecraft.core.particles.ParticleOptions dragonFire = OPTickHandler.getIronParticle("dragon_fire");

        if (dragonFire != null)
            level.sendParticles(dragonFire, target.getX(), target.getY() + 1, target.getZ(), 30, 0.8, 0.8, 0.8, 0.2);
        if (fire != null)
            level.sendParticles(fire, target.getX(), target.getY() + 1, target.getZ(), 40, 1.2, 1.2, 1.2, 0.1);

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.5,
                target.getZ(), 2, 1.0, 1.0,
                1.0, 0.1);

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius));
        for (LivingEntity entity : nearby) {
            if (entity != target && !com.complextalents.util.TeamHelper.isAlly(target, entity)) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), aoeDamage); // True damage
                entity.setSecondsOnFire(3);
            }
        }

        // T2 Scorch Zone
        OPTickHandler.spawnScorchedZone(level, target.position(), 4.0f, 100, 15f * scale, attacker);
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        // AAA Particle: Inferno
        Messages.spawnAAAParticle(level, target.position().add(0, 1.0, 0), "inferno", new Vector3f(0), 1f);

        com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, 35, () -> {
            // T3 Miniature Sun
            OPTickHandler.spawnMiniatureSun(level, target.position(), 2.0f * scale, 120, 40f * scale, attacker);
        });
    }

    @Override
    public List<String> getEffectBreakdown(int tier, float damage) {
        List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier == 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("+ %.1f DPS Ramping Miniature Sun (T3 Supernova)", 40f * scale));
        } else if (tier == 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("+ %.1f AoE Damage & Scorched Zone (T2 Scorch)", 15f * scale));
        } else if (tier == 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Bonus True Damage (T1 Ignite)", 5f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.FIRE;
    }
}
