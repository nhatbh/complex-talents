package com.complextalents.elemental.strategies.op;

import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.effects.OPEffects;
import com.complextalents.elemental.registry.OverwhelmingPowerRegistry;
import com.complextalents.elemental.handlers.OPTickHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public class AquaOPStrategy implements IOPStrategy {
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
                applyT3(context.getAttacker(), target, damage, level);
            case 2:
                applyT2(context.getAttacker(), damage, target, level);
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
            if (entity != target && (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity))) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), splashDamage);
                break; // Only +1 target for T1
            }
        }
        
        net.minecraft.core.particles.ParticleOptions bubble = OPTickHandler.getIronParticle("bubble");
        if (bubble != null) {
            level.sendParticles(bubble, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        } else {
            level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private void applyT2(LivingEntity attacker, float damage, LivingEntity target, ServerLevel level) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
        int duration = (int) (60 * Math.sqrt(scale)); // 3s base
        attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
        
        net.minecraft.core.particles.ParticleOptions mist = OPTickHandler.getIronParticle("mist");
        if (mist != null) {
            level.sendParticles(mist, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    private void applyT3(LivingEntity attacker, LivingEntity target, float damage, ServerLevel level) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
        int duration = (int) (120 * Math.sqrt(scale)); // 6s base
        target.addEffect(new MobEffectInstance(OPEffects.DRENCHED.get(), duration, 0));
        if (attacker != null) {
            target.getPersistentData().putString("OP_Attacker", attacker.getName().getString());
        }
        
        net.minecraft.core.particles.ParticleOptions acidBubble = OPTickHandler.getIronParticle("acid_bubble");
        if (acidBubble != null) {
            level.sendParticles(acidBubble, target.getX(), target.getY() + 1, target.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
        }
    }

    private void applyT4(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
        float splashDamage = 100f * scale;
        
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(5.0));
        int count = 0;
        for (LivingEntity entity : nearby) {
            if (entity != target && (attacker == null || !com.complextalents.util.TeamHelper.isAlly(attacker, entity))) {
                entity.hurt(level.damageSources().indirectMagic(attacker, entity), splashDamage);
                count++;
                if (count >= 3) break;
            }
        }
        
        net.minecraft.core.particles.ParticleOptions shockwave = OPTickHandler.getIronParticle("shockwave");
        if (shockwave != null) {
            level.sendParticles(shockwave, target.getX(), target.getY() + 0.1, target.getZ(), 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1, target.getZ(), 2, 0.5, 0.5, 0.5, 0.1);
    }

    private void applyT5(LivingEntity attacker, LivingEntity target, ServerLevel level, float damage) {
        float scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
        float dps = 20f * scale;
        float radius = 5.0f * (float)Math.sqrt(scale);
        
        OPTickHandler.spawnWhirlpool(level, target.position(), radius, 160, dps, attacker);
    }

    @Override
    public java.util.List<String> getEffectBreakdown(int tier, float damage) {
        java.util.List<String> breakdown = new java.util.ArrayList<>();
        float scale;
        if (tier >= 1) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(1);
            breakdown.add(String.format("+ %.1f Splash Damage (T1 Splash)", 10f * scale));
        }
        if (tier >= 2) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(2);
            breakdown.add(String.format("Moisture: Speed Boost for %.1fs (T2 Moisture)", 60f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 3) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(3);
            breakdown.add(String.format("Applied Drenched: +15%% Vulnerability for %.1fs (T3 Drenched)", 120f * Math.sqrt(scale) / 20f));
        }
        if (tier >= 4) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(4);
            breakdown.add(String.format("+ %.1f Multi-Splash Damage (T4 Violent Splash)", 100f * scale));
        }
        if (tier >= 5) {
            scale = damage / (float) OverwhelmingPowerRegistry.getThreshold(5);
            breakdown.add(String.format("The Deluge: %.1f DPS Whirlpool (T5 Deluge)", 20f * scale));
            breakdown.add(String.format("   Pulling enemies into vortex (%.1fm radius)", 5.0f * Math.sqrt(scale)));
        }
        return breakdown;
    }

    @Override
    public OPElementType getElementType() {
        return OPElementType.AQUA;
    }
}
