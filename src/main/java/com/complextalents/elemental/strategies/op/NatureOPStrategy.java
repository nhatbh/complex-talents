package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.handlers.OPTickHandler;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import com.complextalents.network.Messages;
import org.joml.Vector3f;

import java.util.List;

public class NatureOPStrategy implements IOPStrategy {
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
        int duration = (int) (100 * Math.sqrt(scale)); // 5s
        target.addEffect(new MobEffectInstance(OPEffects.PARASITIC_SEED.get(), duration, 0));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }

        net.minecraft.core.particles.ParticleOptions nature = com.complextalents.elemental.handlers.OPTickHandler
                .getIronParticle("nature");
        if (nature != null) {
            level.sendParticles(nature, target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        int duration = (int) (100 * Math.sqrt(scale));

        // AoE Seed Application
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0));
        for (LivingEntity entity : nearby) {
            if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.addEffect(new MobEffectInstance(OPEffects.PARASITIC_SEED.get(), duration, 0));
                if (attacker != null) {
                    entity.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
                }
            }
        }

        net.minecraft.core.particles.ParticleOptions nature = com.complextalents.elemental.handlers.OPTickHandler
                .getIronParticle("nature");
        if (nature != null) {
            level.sendParticles(nature, target.getX(), target.getY() + 1, target.getZ(), 25, 1.0, 1.0, 1.0, 0.1);
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        float radius = 5.0f * (float) Math.sqrt(scale);

        // AAA Particle: Sandstorm
        Messages.spawnAAAParticle(level, target.position().add(0, 1.0, 0), "sandstorm", new Vector3f(0), 2.5f);
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }

        com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, 30, () -> {
            OPTickHandler.spawnSandTornado(level, target.position(), radius, 160, 30f * scale, attacker);
        });
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier == 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Sandstorm: %.1f DPS Sand Tornado (T3 Sandstorm)", 30f * scale));
            breakdown
                    .add(String.format("   Blindness, Slowness & Vortex Pull (%.1fm radius)", 5.0f * Math.sqrt(scale)));
        } else if (tier == 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Spore Burst: AoE Parasitic Seed Application (T2 Spore Burst)"));
        } else if (tier == 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("Applied Parasitic Seed for %.1fs (T1 Parasitic Seed)",
                    100f * Math.sqrt(scale) / 20f));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.NATURE;
    }
}
