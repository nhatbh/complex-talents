package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;


public class IceOPStrategy implements IOPStrategy {
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
        int slowness = (int) (15 * scale); // This would be the amplifier
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60,
                Math.min(4, slowness / 10)));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions ice = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("ice");
        if (ice != null) {
            level.sendParticles(ice, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private void applyT2(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        int duration = (int) (20 * Math.sqrt(scale)); // 1s base
        // Using a long slow or something to simulate root if actual root not available
        target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, duration, 10));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions snowflake = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("snowflake");
        if (snowflake != null) {
            level.sendParticles(snowflake, target.getX(), target.getY() + 0.5, target.getZ(), 15, 0.4, 0.2, 0.4, 0.02);
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        int duration = (int) (100 * Math.sqrt(scale));
        target.addEffect(new MobEffectInstance(OPEffects.BRITTLE.get(), duration, 0));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions ice = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("ice");
        if (ice != null) {
            level.sendParticles(ice, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.5, 0.3, 0.1);
        }
    }

    private void applyT4(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
        double radius = 4.0 * Math.sqrt(scale);
        
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius));
        for (LivingEntity entity : nearby) {
            if (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity)) {
                entity.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
            }
        }
        
        net.minecraft.core.particles.ParticleOptions snowflake = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("snowflake");
        if (snowflake != null) {
            level.sendParticles(snowflake, target.getX(), target.getY() + 1, target.getZ(), 40, radius/2, 1.0, radius/2, 0.05);
        }
    }

    private void applyT5(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
        int duration = (int) (200 * Math.sqrt(scale));
        target.addEffect(new MobEffectInstance(OPEffects.ABSOLUTE_ZERO.get(), duration, 0));
        // Note: Absolute Zero AI stop is handled in OPTickHandler
        
        net.minecraft.core.particles.ParticleOptions ice = com.complextalents.elemental.handlers.OPTickHandler.getIronParticle("ice");
        if (ice != null) {
            level.sendParticles(ice, target.getX(), target.getY() + 1, target.getZ(), 50, 1.0, 1.0, 1.0, 0.1);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLASH, target.getX(), target.getY() + 1, target.getZ(), 10, 0.1, 0.1, 0.1, 0);
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier >= 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("Applied Chilled: Slowness %d for 3s (T1 Chilled)", (int)Math.min(4, (15 * scale) / 10)));
        }
        if (tier >= 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Deep Freeze: Movement Rooted for %.1fs (T2 Deep Freeze)", 20f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Applied Brittle for %.1fs (T3 Brittle)", 100f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 4) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
            breakdown.add(String.format("Glacial Aura: %.1fm AoE Freeze (T4 Glacial Aura)", 4.0 * Math.sqrt(scale)));
        }
        if (tier >= 5) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
            breakdown.add(String.format("Absolute Zero: 200%% Damage Taken for %.1fs (T5 Absolute Zero)", 200f * Math.sqrt(scale) / 20f));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.ICE;
    }
}
