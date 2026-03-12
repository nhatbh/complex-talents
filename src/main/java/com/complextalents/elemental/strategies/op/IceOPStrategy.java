package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import com.complextalents.network.Messages;
import org.joml.Vector3f;

import java.util.List;

public class IceOPStrategy implements IOPStrategy {
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
        float iceDamage = 5f * scale; // Reduced from 25f
        target.hurt(level.damageSources().indirectMagic(attacker, target), iceDamage);

        // Heavy Slowdown (Slowness IV for 4s scaled)
        int duration = (int) (80 * Math.sqrt(scale));
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 3));

        net.minecraft.core.particles.ParticleOptions ice = com.complextalents.elemental.handlers.OPTickHandler
                .getIronParticle("ice");
        if (ice != null) {
            level.sendParticles(ice, target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float aoeDamage = 15f * scale; // Reduced from 60f
        double radius = 6.0; // Increased radius for better control

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius));
        for (LivingEntity entity : nearby) {
            if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), aoeDamage);

                // Frostbind: Effectiveness "Root" (Slowness 10 for 3s scaled)
                int duration = (int) (60 * Math.sqrt(scale));
                entity.addEffect(
                        new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 9));
            }
        }

        net.minecraft.core.particles.ParticleOptions snowflake = com.complextalents.elemental.handlers.OPTickHandler
                .getIronParticle("snowflake");
        if (snowflake != null) {
            level.sendParticles(snowflake, target.getX(), target.getY() + 1, target.getZ(), 50, 2.5, 1.0, 2.5, 0.05);
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        double radius = 6.0;
        int duration = (int) (300 * Math.sqrt(scale));

        // AAA Particle: Nifthelm
        Messages.spawnAAAParticle(level, target.position().add(0, 1.0, 0), "nifthelm", new Vector3f(0), 5.0f);

        com.complextalents.elemental.handlers.DelayedActionHandler.queueAction(level, 30, () -> {
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(radius));
            for (LivingEntity entity : nearby) {
                if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                    entity.addEffect(new MobEffectInstance(OPEffects.ABSOLUTE_ZERO.get(), duration, 0));
                    entity.hurt(level.damageSources().indirectMagic(attacker, entity), 30f * scale);
                }
            }
        });
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier == 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format(
                    "Absolute Zero: 200%% DMG Vulnerability + immobilization for %.1fs (T3 Absolute Zero)",
                    300f * Math.sqrt(scale) / 20f));
        } else if (tier == 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Glacial Aura: %.1f AoE Damage & Rooting (T2 AoE Frostbind)", 15f * scale));
        } else if (tier == 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Ice Damage & Slowness IV for %.1fs (T1 Frozen Touch)", 5f * scale,
                    80f * Math.sqrt(scale) / 20f));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.ICE;
    }
}
