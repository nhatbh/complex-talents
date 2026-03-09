package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;


public class NatureOPStrategy implements IOPStrategy {
    @Override
    public void execute(OPContext context, int tier) {
        float damage = context.getRawDamage();
        LivingEntity target = context.getTarget();
        ServerLevel level = context.getLevel();

        switch (tier) {
            case 5:
                applyT5(context.getAttacker(), target, level, damage);
            case 4:
                applyT4(context.getAttacker(), target, level, damage);
            case 3:
                applyT3(context.getAttacker(), target, level, damage);
            case 2:
                applyT2(context.getAttacker(), target, level, damage);
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
        
        net.minecraft.core.particles.ParticleOptions nature = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("nature");
        if (nature != null) {
            level.sendParticles(nature, target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        float heal = 10f * scale;
        attacker.heal(heal);
        
        net.minecraft.core.particles.ParticleOptions firefly = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("firefly");
        if (firefly != null) {
            level.sendParticles(firefly, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 15, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        // Scaling Rot: Increment amp based on scale
        target.addEffect(new MobEffectInstance(OPEffects.PARASITIC_SEED.get(), (int)(100 * Math.sqrt(scale)), 1));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions nature = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("nature");
        if (nature != null) {
            level.sendParticles(nature, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
        }
    }

    private void applyT4(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
        // Apply a brief marker effect that scales with damage
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, (int)(100 * scale), 0));
        
        net.minecraft.core.particles.ParticleOptions blood = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("blood");
        if (blood != null) {
            level.sendParticles(blood, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void applyT5(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
        target.addEffect(new MobEffectInstance(OPEffects.OVERGROWTH.get(), 1200, (int)(scale * 10))); // 60s base
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions nature = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("nature");
        if (nature != null) {
            level.sendParticles(nature, target.getX(), target.getY() + 0.5, target.getZ(), 50, 1.5, 0.5, 1.5, 0.1);
        }
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier >= 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("Applied Parasitic Seed for %.1fs (T1 Parasitic Seed)", 100f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Healed %.1f HP (T2 Leech)", 10f * scale));
        }
        if (tier >= 3) breakdown.add("Aggressive Seed Proliferation (T3 Aggressive Scaling)");
        if (tier >= 4) breakdown.add("Verdant Burst Marker Applied (T4 Verdant Burst)");
        if (tier >= 5) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
            breakdown.add(String.format("Verdant Decay: %.1f DPS Nature Pulses (T5 Verdant Decay)", 100f * scale));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.NATURE;
    }
}
