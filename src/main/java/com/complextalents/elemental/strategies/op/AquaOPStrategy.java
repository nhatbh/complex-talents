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

public class AquaOPStrategy implements IOPStrategy {
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
                applyT1(context.getAttacker(), target, level, damage);
                break;
        }
    }

    private void applyT1(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
        float splashDamage = 10f * scale;

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0));
        for (LivingEntity entity : nearby) {
            if (entity != target
                    && (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity))) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), splashDamage);
                break; // Only +1 target for T1
            }
        }

        net.minecraft.core.particles.ParticleOptions bubble = OPTickHandler.getIronParticle("bubble");
        if (bubble != null) {
            level.sendParticles(bubble, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        } else {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, target.getX(), target.getY() + 1,
                    target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float splashDamage = 100f * scale;

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target
                    && (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity))) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), splashDamage);
                count++;
                if (count >= 3)
                    break;
            }
        }

        net.minecraft.core.particles.ParticleOptions shockwave = OPTickHandler.getIronParticle("shockwave");
        if (shockwave != null) {
            level.sendParticles(shockwave, target.getX(), target.getY() + 0.1, target.getZ(), 1, 0, 0, 0, 0);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1,
                target.getZ(), 2, 0.5, 0.5, 0.5, 0.1);
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        float radius = 4.0f * (float) Math.sqrt(scale);

        // AAA Particle: Greatflood
        Messages.spawnAAAParticle(level, target.position().add(0, 1.0, 0), "greatflood", new Vector3f(0), 2.5f);

        com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, 25, () -> {
            OPTickHandler.spawnWaterColumn(level, target.position(), radius, 60, attacker);
        });
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier == 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("The Deluge: Rising Water Column (T3 Deluge)"));
            breakdown.add(String.format("   Launches enemies up (%.1fm radius)", 4.0f * Math.sqrt(scale)));
        } else if (tier == 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("+ %.1f Multi-Splash Damage (T2 Violent Splash)", 100f * scale));
        } else if (tier == 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Splash Damage (T1 Splash)", 10f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.AQUA;
    }
}
